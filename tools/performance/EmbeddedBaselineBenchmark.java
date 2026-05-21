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
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class EmbeddedBaselineBenchmark {
    private static final String WORKLOAD_PROPERTY = "datacore.benchmark.workload";
    private static final String ROWS_PROPERTY = "datacore.benchmark.rows";
    private static final String READS_PROPERTY = "datacore.benchmark.reads";
    private static final String RANGES_PROPERTY = "datacore.benchmark.ranges";
    private static final String WARMUP_PROPERTY = "datacore.benchmark.warmup";
    private static final String ITERATIONS_PROPERTY =
            "datacore.benchmark.iterations";
    private static final String DB_PROPERTY = "datacore.benchmark.db";
    private static final String RESULTS_PROPERTY = "datacore.benchmark.results";
    private static final String GIT_COMMIT_PROPERTY =
            "datacore.benchmark.gitCommit";
    private static final String CSV_HEADER =
            "timestamp,workload,run_type,iteration,rows,read_operations," +
            "insert_ms,lookup_ms,update_ms,scan_ms,total_ms,java_version," +
            "os_name,os_arch,git_commit\n";

    private EmbeddedBaselineBenchmark() {
    }

    public static void main(String[] args) throws Exception {
        String workload = System.getProperty(WORKLOAD_PROPERTY, "mixed");
        int rows = Integer.getInteger(ROWS_PROPERTY, 5000);
        int readOperations = Integer.getInteger(READS_PROPERTY, rows * 10);
        int rangeOperations = Integer.getInteger(RANGES_PROPERTY, rows);
        int warmup = Integer.getInteger(WARMUP_PROPERTY, 1);
        int iterations = Integer.getInteger(ITERATIONS_PROPERTY, 3);
        String gitCommit = System.getProperty(GIT_COMMIT_PROPERTY, "unknown");
        Path dbPath = Paths.get(System.getProperty(DB_PROPERTY,
                "generated/performance/embedded-baseline-db"));
        Path resultsPath = Paths.get(System.getProperty(RESULTS_PROPERTY,
                "generated/performance/results.csv"));

        Files.createDirectories(parentOf(dbPath));
        Files.createDirectories(parentOf(resultsPath));

        System.out.println("Innervex DataCore embedded baseline");
        System.out.println("workload=" + workload);
        System.out.println("rows=" + rows);
        if ("read-heavy".equals(workload)) {
            System.out.println("read_operations=" + readOperations);
        }
        if ("range-scan".equals(workload)) {
            System.out.println("range_operations=" + rangeOperations);
        }
        System.out.println("warmup=" + warmup);
        System.out.println("iterations=" + iterations);
        System.out.println("git_commit=" + gitCommit);

        for (int iteration = 1; iteration <= warmup; iteration++) {
            runOnce(dbPath, workload, rows, readOperations, "warmup",
                    iteration, rangeOperations);
        }

        List<BenchmarkResult> measuredResults = new ArrayList<>();
        for (int iteration = 1; iteration <= iterations; iteration++) {
            BenchmarkResult result = runOnce(dbPath, workload, rows,
                    readOperations, "measured", iteration, rangeOperations);
            result.gitCommit = gitCommit;
            printResults(result);
            appendResults(resultsPath, result);
            measuredResults.add(result);
        }

        printSummary(measuredResults);
        System.out.println("results_csv=" + resultsPath.toAbsolutePath());
    }

    private static BenchmarkResult runOnce(Path dbPath, String workload,
            int rows, int readOperations, String runType, int iteration)
            throws Exception {
        return runOnce(dbPath, workload, rows, readOperations, runType,
                iteration, rows);
    }

    private static BenchmarkResult runOnce(Path dbPath, String workload,
            int rows, int readOperations, String runType, int iteration,
            int rangeOperations)
            throws Exception {
        if ("mixed".equals(workload)) {
            return runMixed(dbPath, workload, rows, readOperations, runType,
                    iteration);
        }

        if ("read-heavy".equals(workload)) {
            return runReadHeavy(dbPath, workload, rows, readOperations,
                    runType, iteration);
        }

        if ("insert-heavy".equals(workload)) {
            return runInsertHeavy(dbPath, workload, rows, runType, iteration);
        }

        if ("range-scan".equals(workload)) {
            return runRangeScan(dbPath, workload, rows, rangeOperations,
                    runType, iteration);
        }

        throw new IllegalArgumentException("Unknown workload: " + workload);
    }

    private static BenchmarkResult runMixed(Path dbPath, String workload,
            int rows, int readOperations, String runType, int iteration)
            throws Exception {
        deleteIfExists(dbPath);

        String url = "jdbc:derby:" + dbPath.toAbsolutePath() + ";create=true";
        long startNanos = System.nanoTime();
        try (Connection connection = DriverManager.getConnection(url)) {
            connection.setAutoCommit(false);
            createSchema(connection);

            BenchmarkResult result = new BenchmarkResult(workload, runType,
                    iteration, rows, 0);
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

    private static BenchmarkResult runReadHeavy(Path dbPath, String workload,
            int rows, int readOperations, String runType, int iteration)
            throws Exception {
        deleteIfExists(dbPath);

        String url = "jdbc:derby:" + dbPath.toAbsolutePath() + ";create=true";
        long startNanos = System.nanoTime();
        try (Connection connection = DriverManager.getConnection(url)) {
            connection.setAutoCommit(false);
            createSchema(connection);

            BenchmarkResult result = new BenchmarkResult(workload, runType,
                    iteration, rows, readOperations);
            result.insertNanos = time(() -> insertRows(connection, rows));
            connection.commit();

            result.lookupNanos = time(() -> readHeavyLookups(connection, rows,
                    readOperations));
            connection.commit();
            result.totalNanos = System.nanoTime() - startNanos;
            return result;
        } finally {
            shutdown(dbPath);
        }
    }

    private static BenchmarkResult runInsertHeavy(Path dbPath, String workload,
            int rows, String runType, int iteration)
            throws Exception {
        deleteIfExists(dbPath);

        String url = "jdbc:derby:" + dbPath.toAbsolutePath() + ";create=true";
        long startNanos = System.nanoTime();
        try (Connection connection = DriverManager.getConnection(url)) {
            connection.setAutoCommit(false);
            createSchema(connection);

            BenchmarkResult result = new BenchmarkResult(workload, runType,
                    iteration, rows, 0);
            result.insertNanos = time(() -> insertRows(connection, rows));
            connection.commit();
            result.totalNanos = System.nanoTime() - startNanos;
            return result;
        } finally {
            shutdown(dbPath);
        }
    }

    private static BenchmarkResult runRangeScan(Path dbPath, String workload,
            int rows, int rangeOperations, String runType, int iteration)
            throws Exception {
        deleteIfExists(dbPath);

        String url = "jdbc:derby:" + dbPath.toAbsolutePath() + ";create=true";
        long startNanos = System.nanoTime();
        try (Connection connection = DriverManager.getConnection(url)) {
            connection.setAutoCommit(false);
            createSchema(connection);

            BenchmarkResult result = new BenchmarkResult(workload, runType,
                    iteration, rows, rangeOperations);
            result.insertNanos = time(() -> insertRows(connection, rows));
            connection.commit();

            result.scanNanos = time(() -> indexedRangeScans(connection,
                    rangeOperations));
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

    private static void readHeavyLookups(Connection connection, int rows,
            int readOperations) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "select name, amount from baseline_item where id = ?")) {
            for (int index = 1; index <= readOperations; index++) {
                int id = ((index * 31) % rows) + 1;
                statement.setInt(1, id);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (!resultSet.next()) {
                        throw new SQLException("Missing row " + id);
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

    private static void indexedRangeScans(Connection connection,
            int rangeOperations) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "select id, name, amount from baseline_item " +
                "where amount between ? and ? order by amount, id")) {
            for (int index = 0; index < rangeOperations; index++) {
                int start = index % 90;
                statement.setInt(1, start);
                statement.setInt(2, start + 9);
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        resultSet.getInt(1);
                        resultSet.getString(2);
                        resultSet.getInt(3);
                    }
                }
            }
        }
    }

    private static long time(SqlRunnable runnable) throws SQLException {
        long startNanos = System.nanoTime();
        runnable.run();
        return System.nanoTime() - startNanos;
    }

    private static void printResults(BenchmarkResult result) {
        System.out.println("workload=" + result.workload);
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

    private static void printSummary(List<BenchmarkResult> results) {
        if (results.isEmpty()) {
            return;
        }

        System.out.println("summary=measured_iterations");
        printStats("insert_ms", collect(results, Metric.INSERT));
        printStats("lookup_ms", collect(results, Metric.LOOKUP));
        printStats("update_ms", collect(results, Metric.UPDATE));
        printStats("scan_ms", collect(results, Metric.SCAN));
        printStats("total_ms", collect(results, Metric.TOTAL));
    }

    private static void printStats(String name, List<Long> values) {
        long min = Long.MAX_VALUE;
        long max = Long.MIN_VALUE;
        long total = 0L;
        for (Long value : values) {
            min = Math.min(min, value.longValue());
            max = Math.max(max, value.longValue());
            total += value.longValue();
        }

        double average = total / (double) values.size();
        System.out.printf("%s_avg=%.3f%n", name, average / 1_000_000.0d);
        System.out.printf("%s_min=%.3f%n", name, min / 1_000_000.0d);
        System.out.printf("%s_max=%.3f%n", name, max / 1_000_000.0d);
    }

    private static List<Long> collect(List<BenchmarkResult> results,
            Metric metric) {
        List<Long> values = new ArrayList<>();
        for (BenchmarkResult result : results) {
            values.add(Long.valueOf(metric.value(result)));
        }
        return values;
    }

    private static void appendResults(Path resultsPath, BenchmarkResult result)
            throws IOException {
        ensureResultsHeader(resultsPath);
        StringBuilder line = new StringBuilder();
        line.append(Instant.now()).append(',')
                .append(result.workload).append(',')
                .append(result.runType).append(',')
                .append(result.iteration).append(',')
                .append(result.rows).append(',')
                .append(result.readOperations).append(',')
                .append(toMillis(result.insertNanos)).append(',')
                .append(toMillis(result.lookupNanos)).append(',')
                .append(toMillis(result.updateNanos)).append(',')
                .append(toMillis(result.scanNanos)).append(',')
                .append(toMillis(result.totalNanos)).append(',')
                .append(csv(System.getProperty("java.version"))).append(',')
                .append(csv(System.getProperty("os.name"))).append(',')
                .append(csv(System.getProperty("os.arch"))).append(',')
                .append(csv(result.gitCommit)).append('\n');

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
        private final String workload;
        private final String runType;
        private final int iteration;
        private final int rows;
        private final int readOperations;
        private long insertNanos;
        private long lookupNanos;
        private long updateNanos;
        private long scanNanos;
        private long totalNanos;
        private String gitCommit = "unknown";

        private BenchmarkResult(String workload, String runType, int iteration,
                int rows, int readOperations) {
            this.workload = workload;
            this.runType = runType;
            this.iteration = iteration;
            this.rows = rows;
            this.readOperations = readOperations;
        }
    }

    private enum Metric {
        INSERT {
            @Override
            long value(BenchmarkResult result) {
                return result.insertNanos;
            }
        },
        LOOKUP {
            @Override
            long value(BenchmarkResult result) {
                return result.lookupNanos;
            }
        },
        UPDATE {
            @Override
            long value(BenchmarkResult result) {
                return result.updateNanos;
            }
        },
        SCAN {
            @Override
            long value(BenchmarkResult result) {
                return result.scanNanos;
            }
        },
        TOTAL {
            @Override
            long value(BenchmarkResult result) {
                return result.totalNanos;
            }
        };

        abstract long value(BenchmarkResult result);
    }

    @FunctionalInterface
    private interface SqlRunnable {
        void run() throws SQLException;
    }
}
