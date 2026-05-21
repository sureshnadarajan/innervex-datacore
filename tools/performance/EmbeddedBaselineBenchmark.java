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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class EmbeddedBaselineBenchmark {
    private static final String ROWS_PROPERTY = "datacore.benchmark.rows";
    private static final String DB_PROPERTY = "datacore.benchmark.db";

    private EmbeddedBaselineBenchmark() {
    }

    public static void main(String[] args) throws Exception {
        int rows = Integer.getInteger(ROWS_PROPERTY, 5000);
        Path dbPath = Paths.get(System.getProperty(DB_PROPERTY,
                "generated/performance/embedded-baseline-db"));

        deleteIfExists(dbPath);
        Files.createDirectories(dbPath.getParent());

        String url = "jdbc:derby:" + dbPath.toAbsolutePath() + ";create=true";

        long startNanos = System.nanoTime();
        try (Connection connection = DriverManager.getConnection(url)) {
            connection.setAutoCommit(false);

            createSchema(connection);

            long insertNanos = time(() -> insertRows(connection, rows));
            long lookupNanos = time(() -> lookupRows(connection, rows));
            long updateNanos = time(() -> updateRows(connection, rows));
            long scanNanos = time(() -> scanRows(connection));

            connection.commit();

            long totalNanos = System.nanoTime() - startNanos;
            printResults(rows, insertNanos, lookupNanos, updateNanos,
                    scanNanos, totalNanos);
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

    private static void printResults(int rows, long insertNanos,
            long lookupNanos, long updateNanos, long scanNanos,
            long totalNanos) {
        System.out.println("Innervex DataCore embedded baseline");
        System.out.println("rows=" + rows);
        printMillis("insert_ms", insertNanos);
        printMillis("lookup_ms", lookupNanos);
        printMillis("update_ms", updateNanos);
        printMillis("scan_ms", scanNanos);
        printMillis("total_ms", totalNanos);
    }

    private static void printMillis(String name, long nanos) {
        System.out.printf("%s=%.3f%n", name, nanos / 1_000_000.0d);
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

    @FunctionalInterface
    private interface SqlRunnable {
        void run() throws SQLException;
    }
}
