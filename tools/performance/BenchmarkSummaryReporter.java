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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class BenchmarkSummaryReporter {
    private static final String RESULTS_PROPERTY = "datacore.benchmark.results";

    private BenchmarkSummaryReporter() {
    }

    public static void main(String[] args) throws Exception {
        Path resultsPath = Paths.get(System.getProperty(RESULTS_PROPERTY,
                "generated/performance/results.csv"));
        List<String> lines = Files.readAllLines(resultsPath,
                StandardCharsets.UTF_8);

        if (lines.size() <= 1) {
            System.out.println("No measured benchmark results found.");
            return;
        }

        Map<String, ResultRow> latestByWorkload = new LinkedHashMap<>();
        String[] header = splitCsv(lines.get(0));
        ColumnIndex columns = new ColumnIndex(header);

        for (int index = 1; index < lines.size(); index++) {
            if (lines.get(index).trim().isEmpty()) {
                continue;
            }

            String[] values = splitCsv(lines.get(index));
            ResultRow row = ResultRow.parse(values, columns);
            if (!"measured".equals(row.runType)) {
                continue;
            }
            latestByWorkload.put(row.workload, row);
        }

        System.out.println("Innervex DataCore latest benchmark results");
        System.out.println("results_csv=" + resultsPath.toAbsolutePath());
        System.out.println("workload, rows, reads, insert_ms, lookup_ms, " +
                "update_ms, delete_ms, scan_ms, total_ms, git_commit");
        for (ResultRow row : latestByWorkload.values()) {
            System.out.println(row.toSummaryLine());
        }
    }

    private static String[] splitCsv(String line) throws IOException {
        // Benchmark output values are simple CSV scalars; this parser still
        // handles quotes so future OS names or values cannot break summaries.
        java.util.ArrayList<String> values = new java.util.ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;

        for (int index = 0; index < line.length(); index++) {
            char character = line.charAt(index);
            if (character == '"') {
                if (quoted && index + 1 < line.length()
                        && line.charAt(index + 1) == '"') {
                    current.append('"');
                    index++;
                } else {
                    quoted = !quoted;
                }
            } else if (character == ',' && !quoted) {
                values.add(current.toString());
                current.setLength(0);
            } else {
                current.append(character);
            }
        }

        if (quoted) {
            throw new IOException("Unclosed CSV quote in line: " + line);
        }

        values.add(current.toString());
        return values.toArray(new String[values.size()]);
    }

    private static final class ColumnIndex {
        private final Map<String, Integer> indexes = new LinkedHashMap<>();

        private ColumnIndex(String[] header) {
            for (int index = 0; index < header.length; index++) {
                indexes.put(header[index], Integer.valueOf(index));
            }
        }

        private String value(String[] values, String name) {
            Integer index = indexes.get(name);
            if (index == null || index.intValue() >= values.length) {
                return "";
            }
            return values[index.intValue()];
        }
    }

    private static final class ResultRow {
        private final String workload;
        private final String runType;
        private final String rows;
        private final String reads;
        private final String insertMs;
        private final String lookupMs;
        private final String updateMs;
        private final String deleteMs;
        private final String scanMs;
        private final String totalMs;
        private final String gitCommit;

        private ResultRow(String workload, String runType, String rows,
                String reads, String insertMs, String lookupMs,
                String updateMs, String deleteMs, String scanMs,
                String totalMs, String gitCommit) {
            this.workload = workload;
            this.runType = runType;
            this.rows = rows;
            this.reads = reads;
            this.insertMs = insertMs;
            this.lookupMs = lookupMs;
            this.updateMs = updateMs;
            this.deleteMs = deleteMs;
            this.scanMs = scanMs;
            this.totalMs = totalMs;
            this.gitCommit = gitCommit;
        }

        private static ResultRow parse(String[] values, ColumnIndex columns) {
            return new ResultRow(
                    columns.value(values, "workload"),
                    columns.value(values, "run_type"),
                    columns.value(values, "rows"),
                    columns.value(values, "read_operations"),
                    columns.value(values, "insert_ms"),
                    columns.value(values, "lookup_ms"),
                    columns.value(values, "update_ms"),
                    columns.value(values, "delete_ms"),
                    columns.value(values, "scan_ms"),
                    columns.value(values, "total_ms"),
                    columns.value(values, "git_commit"));
        }

        private String toSummaryLine() {
            return workload + ", " + rows + ", " + reads + ", " +
                    insertMs + ", " + lookupMs + ", " + updateMs + ", " +
                    deleteMs + ", " + scanMs + ", " + totalMs + ", " +
                    gitCommit;
        }
    }
}
