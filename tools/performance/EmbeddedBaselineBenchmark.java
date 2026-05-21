/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package datacore.performance;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class EmbeddedBaselineBenchmark {
    private static final String ROWS_PROPERTY = "datacore.benchmark.rows";
    private static final String WARMUP_PROPERTY = "datacore.benchmark.warmup";
    private static final String ITERATIONS_PROPERTY =
            "datacore.benchmark.iterations";
    private static final String DB_PROPERTY = "datacore.benchmark.db";
    private static final String RESULTS_PROPERTY = "datacore.benchmark.results";
    private static final String CSV_HEADER =
            "timestamp,run_type,iteration,rows,insert_ms,lookup_ms,update_ms," +
            "scan_ms,total_ms,java_version,os_name,os_arch\n";

    private EmbeddedBaselineBenchmark() {
    }

    public static void main(String[] args) throws Exception {
        int rows = Integer.getInteger(ROWS_PROPERTY, 5000);
        int warmup = Integer.getInteger(WARMUP_PROPERTY, 1);
        int iterations = Integer.getInteger(ITERATIONS_PROPERTY, 3);
        Path dbPath = Paths.get(System.getProperty(DB_PROPERTY,
                "generated/performance/embedded-baseline-db"));
        Path resultsPath = Paths.get(System.getProperty(RESULTS_PROPERTY,
                "generated/performance/results.csv"));

        Files.createDirectories(parentOf(dbPath));
        Files.createDirectories(parentOf(resultsPath));

        System.out.println("Innervex DataCore embedded baseline");
        System.out.println("rows=" + rows);
        System.out.println("warmup=" + warmup);
        System.out.println("iterations=" + iterations);

        for (int iteration = 1; iteration <= warmup; iteration++) {
            runOnce(dbPath, rows, "warmup", iteration);
        }

        for (int iteration = 1; iteration <= iterations; iteration++) {
            BenchmarkResult result = runOnce(dbPath, rows, "measured",
                    iteration);
            printResults(result);
            appendResults(resultsPath, result);
        }

        System.out.println("results_csv=" + resultsPath.toAbsolutePath());
    }

    private static BenchmarkResult runOnce(Path dbPath, int rows,
            String runType, int iteration) throws Exception {
        deleteIfExists(dbPath);

        String url = "jdbc:derby:" + dbPath.toAbsolutePath() + ";create=true";
        long startNanos = System.nanoTime();
        try (Connection connection = DriverManager.getConnection(url)) {
            connection.setAutoCommit(false);
            createSchema(connection);

            BenchmarkResult result = new BenchmarkResult(runType, iteration,
                    rows);
            result.insertNanos = time(() -> insertRows(connection, rows));
            result.lookupNanos = time(() -> lookupRows(connection, rows));
            result.updateNanos = time(() -> updateRows(connection, rows));
            result.scanNanos = time(() -> scanRows(connection));

            connection.commit();
            result.totalNanos = System.nanoTime() - startNanos;
            return result;
        } finally {
            shutdown(dbPath);
        }
    }

    private static void createSchema(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(
                    "create table baseline_item (" +
                    "id int not null primary key, " +
                    "name varchar(80) not null, " +
                    "amount int not null)");
            statement.executeUpdate(
                    "create index baseline_item_amount_idx " +
                    "on baseline_item(amount)");
        }
    }

    private static void insertRows(Connection connection, int rows)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "insert into baseline_item(id, name, amount) values (?, ?, ?)")) {
            for (int index = 1; index <= rows; index++) {
                statement.setInt(1, index);
                statement.setString(2, "item-" + index);
                statement.setInt(3, index % 100);
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private static void lookupRows(Connection connection, int rows)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "select name, amount from baseline_item where id = ?")) {
            for (int index = 1; index <= rows; index++) {
                statement.setInt(1, index);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (!resultSet.next()) {
                        throw new SQLException("Missing row " + index);
                    }
                    resultSet.getString(1);
                    resultSet.getInt(2);
                }
            }
        }
    }

    private static void updateRows(Connection connection, int rows)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "update baseline_item set amount = amount + 1 where id = ?")) {
            for (int index = 1; index <= rows; index++) {
                statement.setInt(1, index);
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private static void scanRows(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "select amount, count(*) from baseline_item " +
                "group by amount order by amount");
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                resultSet.getInt(1);
                resultSet.getInt(2);
            }
        }
    }

    private static long time(SqlRunnable runnable) throws SQLException {
        long startNanos = System.nanoTime();
        runnable.run();
        return System.nanoTime() - startNanos;
    }

    private static void printResults(BenchmarkResult result) {
        System.out.println("measured_iteration=" + result.iteration);
        printMillis("insert_ms", result.insertNanos);
        printMillis("lookup_ms", result.lookupNanos);
        printMillis("update_ms", result.updateNanos);
        printMillis("scan_ms", result.scanNanos);
        printMillis("total_ms", result.totalNanos);
    }

    private static void printMillis(String name, long nanos) {
        System.out.printf("%s=%.3f%n", name, nanos / 1_000_000.0d);
    }

    private static void appendResults(Path resultsPath, BenchmarkResult result)
            throws IOException {
        ensureResultsHeader(resultsPath);
        StringBuilder line = new StringBuilder();
        line.append(Instant.now()).append(',')
                .append(result.runType).append(',')
                .append(result.iteration).append(',')
                .append(result.rows).append(',')
                .append(toMillis(result.insertNanos)).append(',')
                .append(toMillis(result.lookupNanos)).append(',')
                .append(toMillis(result.updateNanos)).append(',')
                .append(toMillis(result.scanNanos)).append(',')
                .append(toMillis(result.totalNanos)).append(',')
                .append(csv(System.getProperty("java.version"))).append(',')
                .append(csv(System.getProperty("os.name"))).append(',')
                .append(csv(System.getProperty("os.arch"))).append('\n');

        Files.write(resultsPath, line.toString().getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    private static void ensureResultsHeader(Path resultsPath)
            throws IOException {
        if (!Files.exists(resultsPath)) {
            writeHeader(resultsPath);
            return;
        }

        List<String> lines = Files.readAllLines(resultsPath,
                StandardCharsets.UTF_8);
        if (lines.isEmpty()) {
            writeHeader(resultsPath);
            return;
        }

        if (!CSV_HEADER.trim().equals(lines.get(0).trim())) {
            Path backupPath = resultsPath.resolveSibling(
                    resultsPath.getFileName() + "." + Instant.now().toEpochMilli()
                    + ".bak");
            Files.move(resultsPath, backupPath);
            writeHeader(resultsPath);
            System.out.println("archived_previous_results="
                    + backupPath.toAbsolutePath());
        }
    }

    private static void writeHeader(Path resultsPath) throws IOException {
        Files.write(resultsPath, CSV_HEADER.getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    private static String toMillis(long nanos) {
        return String.format("%.3f", nanos / 1_000_000.0d);
    }

    private static String csv(String value) {
        if (value == null) {
            return "";
        }
        String escaped = value.replace("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\"")
                || escaped.contains("\n")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }

    private static void shutdown(Path dbPath) {
        try {
            DriverManager.getConnection(
                    "jdbc:derby:" + dbPath.toAbsolutePath() + ";shutdown=true");
        } catch (SQLException expected) {
            if (!"08006".equals(expected.getSQLState())) {
                System.err.println("Unexpected shutdown SQLState: "
                        + expected.getSQLState());
            }
        }
    }

    private static void deleteIfExists(Path path) throws IOException {
        if (!Files.exists(path)) {
            return;
        }

        try (Stream<Path> paths = Files.walk(path)) {
            List<Path> items = paths.sorted((left, right) ->
                    right.getNameCount() - left.getNameCount())
                    .collect(Collectors.toList());
            for (Path item : items) {
                Files.deleteIfExists(item);
            }
        }
    }

    private static Path parentOf(Path path) {
        Path parent = path.getParent();
        if (parent == null) {
            return Paths.get(".");
        }
        return parent;
    }

    private static final class BenchmarkResult {
        private final String runType;
        private final int iteration;
        private final int rows;
        private long insertNanos;
        private long lookupNanos;
        private long updateNanos;
        private long scanNanos;
        private long totalNanos;

        private BenchmarkResult(String runType, int iteration, int rows) {
            this.runType = runType;
            this.iteration = iteration;
            this.rows = rows;
        }
    }

    @FunctionalInterface
    private interface SqlRunnable {
        void run() throws SQLException;
    }
}
