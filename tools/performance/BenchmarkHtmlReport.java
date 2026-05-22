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
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class BenchmarkHtmlReport {
    private static final String RESULTS_PROPERTY = "datacore.benchmark.results";
    private static final String REPORT_PROPERTY = "datacore.benchmark.report";

    private BenchmarkHtmlReport() {
    }

    public static void main(String[] args) throws Exception {
        Path resultsPath = Paths.get(System.getProperty(RESULTS_PROPERTY,
                "generated/performance/results.csv"));
        Path reportPath = Paths.get(System.getProperty(REPORT_PROPERTY,
                "generated/performance/report.html"));
        Map<String, ResultRow> latestByWorkload =
                latestMeasuredRows(resultsPath);

        Files.createDirectories(parentOf(reportPath));
        Files.write(reportPath, renderReport(resultsPath, latestByWorkload)
                .getBytes(StandardCharsets.UTF_8));

        System.out.println("Innervex DataCore benchmark HTML report");
        System.out.println("results_csv=" + resultsPath.toAbsolutePath());
        System.out.println("report_html=" + reportPath.toAbsolutePath());
        System.out.println("workloads=" + latestByWorkload.size());
    }

    private static Map<String, ResultRow> latestMeasuredRows(Path resultsPath)
            throws IOException {
        List<String> lines = Files.readAllLines(resultsPath,
                StandardCharsets.UTF_8);
        Map<String, ResultRow> latestByWorkload = new LinkedHashMap<>();

        if (lines.size() <= 1) {
            return latestByWorkload;
        }

        String[] header = splitCsv(lines.get(0));
        ColumnIndex columns = new ColumnIndex(header);

        for (int index = 1; index < lines.size(); index++) {
            if (lines.get(index).trim().isEmpty()) {
                continue;
            }

            ResultRow row = ResultRow.parse(splitCsv(lines.get(index)),
                    columns);
            if ("measured".equals(row.runType)) {
                latestByWorkload.put(row.workload, row);
            }
        }

        return latestByWorkload;
    }

    private static String renderReport(Path resultsPath,
            Map<String, ResultRow> latestByWorkload) {
        StringBuilder html = new StringBuilder();
        html.append("<!doctype html>\n");
        html.append("<html lang=\"en\">\n");
        html.append("<head>\n");
        html.append("  <meta charset=\"utf-8\">\n");
        html.append("  <title>Innervex DataCore Benchmark Report</title>\n");
        html.append("  <style>\n");
        html.append("    body { font-family: -apple-system, BlinkMacSystemFont, ");
        html.append("\"Segoe UI\", sans-serif; margin: 0; color: #172033; ");
        html.append("background: #f7f9fc; }\n");
        html.append("    main { max-width: 1160px; margin: 0 auto; ");
        html.append("padding: 40px 24px; }\n");
        html.append("    .brand { width: min(520px, 100%); height: auto; ");
        html.append("display: block; margin: 0 0 24px; }\n");
        html.append("    h1 { margin: 0 0 8px; font-size: 34px; }\n");
        html.append("    p { margin: 0 0 22px; color: #536176; }\n");
        html.append("    .owner { margin: 0 0 10px; color: #008ee8; ");
        html.append("font-size: 13px; font-weight: 800; letter-spacing: 0.08em; ");
        html.append("text-transform: uppercase; }\n");
        html.append("    h2 { margin: 28px 0 12px; font-size: 20px; }\n");
        html.append("    table { width: 100%; border-collapse: collapse; ");
        html.append("background: white; border: 1px solid #d9e1ec; }\n");
        html.append("    th, td { padding: 10px 12px; border-bottom: ");
        html.append("1px solid #e5ebf3; text-align: right; }\n");
        html.append("    th { background: #edf3fb; color: #25314a; ");
        html.append("font-weight: 700; }\n");
        html.append("    th:first-child, td:first-child { text-align: left; }\n");
        html.append("    tr:last-child td { border-bottom: 0; }\n");
        html.append("    code { font-family: ui-monospace, SFMono-Regular, ");
        html.append("Menlo, Consolas, monospace; }\n");
        html.append("    .meta { margin-bottom: 24px; }\n");
        html.append("    .empty { padding: 18px; background: white; ");
        html.append("border: 1px solid #d9e1ec; }\n");
        html.append("    .chart { display: grid; gap: 12px; margin: 0 0 26px; ");
        html.append("padding: 18px; background: white; border: 1px solid #d9e1ec; }\n");
        html.append("    .bar-row { display: grid; grid-template-columns: ");
        html.append("minmax(190px, 260px) 1fr 88px; gap: 12px; align-items: center; }\n");
        html.append("    .bar-label { font-weight: 700; color: #25314a; }\n");
        html.append("    .bar-track { height: 14px; background: #edf3fb; ");
        html.append("border-radius: 999px; overflow: hidden; }\n");
        html.append("    .bar-fill { height: 100%; background: #008ee8; ");
        html.append("border-radius: 999px; }\n");
        html.append("    .bar-value { color: #536176; font-variant-numeric: tabular-nums; ");
        html.append("text-align: right; }\n");
        html.append("    .note { margin: 26px 0 0; padding: 18px; background: white; ");
        html.append("border: 1px solid #d9e1ec; }\n");
        html.append("    .note p { margin-bottom: 12px; }\n");
        html.append("    .command { display: block; overflow-x: auto; padding: 12px 14px; ");
        html.append("background: #071b35; color: #dff6ff; border-radius: 6px; ");
        html.append("white-space: nowrap; }\n");
        html.append("    @media (max-width: 760px) { .bar-row { grid-template-columns: 1fr; ");
        html.append("gap: 6px; } .bar-value { text-align: left; } }\n");
        html.append("  </style>\n");
        html.append("</head>\n");
        html.append("<body>\n");
        html.append("<main>\n");
        html.append("  <img class=\"brand\" src=\"assets/ivx-datacore-logo.png\" ");
        html.append("alt=\"Innervex DataCore logo\">\n");
        html.append("  <p class=\"owner\">Innervex Technologies Private Limited</p>\n");
        html.append("  <h1>Innervex DataCore Benchmark Report</h1>\n");
        html.append("  <p class=\"meta\">Generated ")
                .append(escapeHtml(Instant.now().toString()))
                .append(" from <code>")
                .append(escapeHtml(displayPath(resultsPath)))
                .append("</code>.</p>\n");

        if (latestByWorkload.isEmpty()) {
            html.append("  <div class=\"empty\">No measured benchmark rows ");
            html.append("were found.</div>\n");
        } else {
            appendTotalTimeChart(html, latestByWorkload);
            appendTable(html, latestByWorkload);
            appendRefreshNote(html);
        }

        html.append("</main>\n");
        html.append("</body>\n");
        html.append("</html>\n");
        return html.toString();
    }

    private static void appendRefreshNote(StringBuilder html) {
        html.append("  <section class=\"note\" aria-label=\"Refresh report\">\n");
        html.append("    <h2>Refresh This Report</h2>\n");
        html.append("    <p>Run benchmark workloads first, then regenerate this page ");
        html.append("from the accumulated CSV results.</p>\n");
        html.append("    <code class=\"command\">ant datacore-benchmark-summary</code>\n");
        html.append("    <code class=\"command\">ant datacore-benchmark-html-report</code>\n");
        html.append("  </section>\n");
    }

    private static void appendTotalTimeChart(StringBuilder html,
            Map<String, ResultRow> latestByWorkload) {
        double maxTotalMs = 0.0d;
        for (ResultRow row : latestByWorkload.values()) {
            maxTotalMs = Math.max(maxTotalMs, parseDouble(row.totalMs));
        }

        html.append("  <h2>Total Time by Workload</h2>\n");
        html.append("  <div class=\"chart\" aria-label=\"Total time by workload\">\n");
        for (ResultRow row : latestByWorkload.values()) {
            double totalMs = parseDouble(row.totalMs);
            int width = maxTotalMs <= 0.0d
                    ? 0 : Math.max(2, (int) Math.round(totalMs * 100.0d / maxTotalMs));
            html.append("    <div class=\"bar-row\">");
            html.append("<div class=\"bar-label\">")
                    .append(escapeHtml(row.workload))
                    .append("</div>");
            html.append("<div class=\"bar-track\"><div class=\"bar-fill\" style=\"width: ")
                    .append(width)
                    .append("%\"></div></div>");
            html.append("<div class=\"bar-value\">")
                    .append(escapeHtml(row.totalMs))
                    .append(" ms</div>");
            html.append("</div>\n");
        }
        html.append("  </div>\n");
    }

    private static void appendTable(StringBuilder html,
            Map<String, ResultRow> latestByWorkload) {
        html.append("  <h2>Latest Measured Results</h2>\n");
        html.append("  <table>\n");
        html.append("    <thead><tr>");
        appendHeader(html, "Workload");
        appendHeader(html, "Rows");
        appendHeader(html, "Reads");
        appendHeader(html, "Insert ms");
        appendHeader(html, "Lookup ms");
        appendHeader(html, "Update ms");
        appendHeader(html, "Delete ms");
        appendHeader(html, "Scan ms");
        appendHeader(html, "Total ms");
        appendHeader(html, "Commit");
        html.append("</tr></thead>\n");
        html.append("    <tbody>\n");
        for (ResultRow row : latestByWorkload.values()) {
            html.append("      <tr>");
            appendCell(html, row.workload);
            appendCell(html, row.rows);
            appendCell(html, row.reads);
            appendCell(html, row.insertMs);
            appendCell(html, row.lookupMs);
            appendCell(html, row.updateMs);
            appendCell(html, row.deleteMs);
            appendCell(html, row.scanMs);
            appendCell(html, row.totalMs);
            appendCell(html, shortCommit(row.gitCommit));
            html.append("</tr>\n");
        }
        html.append("    </tbody>\n");
        html.append("  </table>\n");
    }

    private static void appendHeader(StringBuilder html, String value) {
        html.append("<th>").append(escapeHtml(value)).append("</th>");
    }

    private static void appendCell(StringBuilder html, String value) {
        html.append("<td>").append(escapeHtml(value)).append("</td>");
    }

    private static String shortCommit(String gitCommit) {
        if (gitCommit == null || gitCommit.length() <= 12) {
            return gitCommit;
        }
        return gitCommit.substring(0, 12);
    }

    private static String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private static double parseDouble(String value) {
        if (value == null || value.length() == 0) {
            return 0.0d;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException nfe) {
            return 0.0d;
        }
    }

    private static String displayPath(Path path) {
        String normalized = path.toString().replace('\\', '/');
        int generatedIndex = normalized.indexOf("/generated/");
        if (generatedIndex >= 0) {
            return normalized.substring(generatedIndex + 1);
        }
        int docsIndex = normalized.indexOf("/docs/");
        if (docsIndex >= 0) {
            return normalized.substring(docsIndex + 1);
        }
        return normalized;
    }

    private static Path parentOf(Path path) {
        Path parent = path.toAbsolutePath().getParent();
        return parent == null ? Paths.get(".") : parent;
    }

    private static String[] splitCsv(String line) throws IOException {
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
    }
}
