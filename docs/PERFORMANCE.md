**INNERVEX TECHNOLOGIES PRIVATE LIMITED**

# INNERVEX® DATACORE

### Enterprise Data Platform & Integration Framework

**Version:** 1.0  

**Built on Open Standards. Engineered for Enterprise Operations.**

---

| INNERVEX TECHNOLOGIES PRIVATE LIMITED | INNERVEX® DATACORE<br>Enterprise Data Platform & Integration Framework | Version: 1.0<br>Document Type: Performance Plan & Benchmark Overview |
| --- | --- | --- |

## 1. Technology Notice

Innervex DataCore is an enterprise data platform, deployment framework,
integration architecture, and support ecosystem developed by Innervex
Technologies.

Innervex DataCore may incorporate and utilise open-source technologies and
database engines as part of its implementation architecture. All underlying
open-source software components remain the property of their respective
copyright holders and are governed by their respective licences.

Innervex Technologies does not claim ownership of any third-party open-source
projects included within the Innervex DataCore ecosystem.

# 2. Performance Plan

**INNERVEX® DATACORE**  
Enterprise Data Platform & Integration Framework  
Built on Open Standards. Engineered for Enterprise Operations.

Innervex DataCore provides an enterprise data platform and integration
framework around trusted open technologies. Performance work in this repository
must be guided by repeatable evidence, not by guesswork or broad rewrites.

## Goals

- Establish clear baseline numbers for the current DataCore platform behavior
- Improve performance for real embedded database workloads
- Preserve Derby-compatible SQL, JDBC, transaction, locking, storage, and
  recovery behavior
- Make bottlenecks visible through profiling, diagnostics, and focused tests
- Prefer small, reviewable engine improvements over high-risk rewrites

## Baseline Areas

Initial benchmarking should cover:

- Embedded database startup and shutdown time
- Database creation and open time
- Single-row insert, update, delete, and lookup latency
- Batch insert and indexed lookup throughput
- Read-heavy, write-heavy, and mixed transaction workloads
- Lock contention and concurrent transaction behavior
- Query planning behavior for common joins and indexes
- Network Server client latency and throughput
- Recovery time after unclean shutdown
- Memory usage during sustained embedded operation

## Comparison Databases

H2, PostgreSQL, and Firebird can be useful comparison points, but they should
not be treated as direct drop-in equivalents.

- H2 is useful for embedded Java comparison and MVStore/MVCC behavior
- PostgreSQL is useful as a mature server database performance reference
- Firebird is useful as a compact relational engine reference
- Derby compatibility remains the baseline for Innervex DataCore behavior

The comparison goal is to learn where DataCore needs improvement, not to copy
another database design wholesale.

## Measurement Process

Every meaningful performance change should follow this pattern:

1. Reproduce the workload
2. Measure the current baseline
3. Profile the bottleneck
4. Make the smallest safe change
5. Run correctness tests
6. Re-measure and compare
7. Document the result

If a change improves one workload but harms another, record the tradeoff before
accepting it.

## Compatibility Rule

Performance must not come from silently weakening database guarantees.

Before changing locking, transaction isolation, storage format, recovery,
query execution, or JDBC-visible behavior, add or identify tests that prove the
expected behavior still holds.

## First Practical Work

The first useful performance milestone is not an engine rewrite. It is a
repeatable benchmark baseline that can answer:

- What is DataCore fast at today?
- Where does DataCore fall behind in real embedded database workloads?
- Which bottlenecks are caused by locking, storage, query planning, network
  access, startup, or memory pressure?
- Which improvements are safe to make without breaking compatibility?

Once this baseline exists, optimization work can move with confidence.

## Current Baseline Harness

The repository includes a first embedded benchmark target:

```sh
ant datacore-embedded-benchmark
```

The benchmark creates a local database under `generated/performance/`, runs
warmup and measured iterations of a small insert, lookup, update, and grouped
scan workload, then prints timing results. It also appends measured iterations
to `generated/performance/results.csv` so local baseline history can be
compared over time. CSV rows include the current Git commit id so performance
results can be tied back to the exact source version. The default row count is
intentionally small so contributors can run it quickly during development.

To run a larger local sample:

```sh
ant -Ddatacore.benchmark.rows=50000 datacore-embedded-benchmark
```

To change the number of measured iterations:

```sh
ant -Ddatacore.benchmark.iterations=5 datacore-embedded-benchmark
```

To print the latest result for each workload:

```sh
ant datacore-benchmark-summary
```

To create a local HTML report from the same CSV:

```sh
ant datacore-benchmark-html-report
```

The report is written to `docs/performance-report.html`, so it can be opened
from the project welcome page and published with the project documentation.

To run the standard local suite and refresh the report in one command:

```sh
ant datacore-benchmark-suite
```

The suite runs the core mixed/read/write/range/concurrent workloads plus
covering-index twins for the write-heavy paths where the larger range index
can change maintenance cost.

On macOS, the repository also provides helper commands that use the NetBeans
Ant runtime when it is available:

```sh
bin/datacore-benchmark-suite
bin/datacore-benchmark-report
```

To run the read-heavy workload:

```sh
ant -Ddatacore.benchmark.workload=read-heavy datacore-embedded-benchmark
```

The read-heavy workload loads the table, then measures repeated primary-key
lookups. The number of lookups can be changed with
`-Ddatacore.benchmark.reads=100000`.

To run the concurrent-read workload:

```sh
ant -Ddatacore.benchmark.workload=concurrent-read datacore-embedded-benchmark
```

The concurrent-read workload loads the table, then opens multiple embedded
connections and performs primary-key lookups from several reader threads. The
number of lookups and threads can be changed with
`-Ddatacore.benchmark.reads=100000 -Ddatacore.benchmark.threads=8`.

To run the concurrent mixed read/write workload:

```sh
ant -Ddatacore.benchmark.workload=concurrent-mixed datacore-embedded-benchmark
```

The concurrent-mixed workload loads the table, then runs one writer thread
updating rows while the remaining threads perform primary-key reads. This gives
DataCore a first locking and read/write concurrency baseline.

To compare concurrent mixed read/write cost with the larger covering range
index:

```sh
ant -Ddatacore.benchmark.workload=concurrent-mixed-covering-index datacore-embedded-benchmark
```

This workload measures the same concurrent read/write pattern as
`concurrent-mixed`, but creates an `(amount, id, name)` index before loading
and updating rows.

To run the concurrent mixed transaction workload:

```sh
ant -Ddatacore.benchmark.workload=concurrent-transaction-mixed datacore-embedded-benchmark
```

The concurrent-transaction-mixed workload runs reader threads while one writer
updates rows using one commit per row. This combines locking pressure with
small-transaction commit overhead.

To compare concurrent transaction cost with the larger covering range index:

```sh
ant -Ddatacore.benchmark.workload=concurrent-transaction-mixed-covering-index datacore-embedded-benchmark
```

This workload measures the same reader/writer pattern as
`concurrent-transaction-mixed`, but maintains the `(amount, id, name)` index.

To run the insert-heavy workload:

```sh
ant -Ddatacore.benchmark.workload=insert-heavy datacore-embedded-benchmark
```

The insert-heavy workload creates the table and measures only the batch insert
phase. This gives DataCore a simple write-throughput baseline before adding
deeper storage-engine changes.

To compare the insert cost of maintaining the larger covering range index:

```sh
ant -Ddatacore.benchmark.workload=insert-heavy-covering-index datacore-embedded-benchmark
```

This workload measures the same insert phase as `insert-heavy`, but creates an
`(amount, id, name)` index before loading rows.

To compare the full mixed workload with the larger covering range index:

```sh
ant -Ddatacore.benchmark.workload=mixed-covering-index datacore-embedded-benchmark
```

This workload measures the same insert, lookup, update, and grouped scan phases
as `mixed`, but maintains the `(amount, id, name)` index.

To run the update-heavy workload:

```sh
ant -Ddatacore.benchmark.workload=update-heavy datacore-embedded-benchmark
```

The update-heavy workload loads the table, then measures updates across all
rows. This helps track write cost for existing records separately from initial
data loading.

To compare the update cost of maintaining the larger covering range index:

```sh
ant -Ddatacore.benchmark.workload=update-heavy-covering-index datacore-embedded-benchmark
```

This workload measures the same update phase as `update-heavy`, but creates an
`(amount, id, name)` index before loading and updating rows.

To run the delete-heavy workload:

```sh
ant -Ddatacore.benchmark.workload=delete-heavy datacore-embedded-benchmark
```

The delete-heavy workload loads the table, then measures deletes across all
rows. This gives DataCore a separate baseline for row removal and index cleanup.

To compare the delete cost of maintaining the larger covering range index:

```sh
ant -Ddatacore.benchmark.workload=delete-heavy-covering-index datacore-embedded-benchmark
```

This workload measures the same delete phase as `delete-heavy`, but creates an
`(amount, id, name)` index before loading and deleting rows.

To run the transaction-heavy workload:

```sh
ant -Ddatacore.benchmark.workload=transaction-heavy datacore-embedded-benchmark
```

The transaction-heavy workload inserts rows using one commit per row. This
measures transaction commit overhead, which is important for enterprise
applications that perform many small units of work.

To compare per-row commit insert cost with the larger covering range index:

```sh
ant -Ddatacore.benchmark.workload=transaction-heavy-covering-index datacore-embedded-benchmark
```

This workload measures the same one-commit-per-row insert pattern as
`transaction-heavy`, but maintains the `(amount, id, name)` index.

To run the indexed range-scan workload:

```sh
ant -Ddatacore.benchmark.workload=range-scan datacore-embedded-benchmark
```

The range-scan workload loads the table, then repeatedly queries ranges over
the indexed `amount` column. The number of range queries can be changed with
`-Ddatacore.benchmark.ranges=10000`.

The default range width is 10 distinct `amount` values. Since the benchmark
loads values from 0 through 99, the default range returns about 10 percent of
the table for each range query. Narrower ranges can be tested with:

```sh
ant -Ddatacore.benchmark.workload=range-scan -Ddatacore.benchmark.rangeWidth=1 datacore-embedded-benchmark
```

To isolate index traversal from full row fetching, run the index-only range
scan workload:

```sh
ant -Ddatacore.benchmark.workload=range-scan-index-only datacore-embedded-benchmark
```

This query reads only the indexed `amount` column. Comparing it with
`range-scan` helps show whether range-scan time is dominated by index
traversal or by fetching full rows from the table.

To isolate the cost of explicit ordering from full-row range fetching, run the
unordered full-row range scan workload:

```sh
ant -Ddatacore.benchmark.workload=range-scan-unordered datacore-embedded-benchmark
```

This query fetches the same columns as `range-scan` but does not request
`order by amount, id`.

To test whether an index that matches the ordered range query helps, run the
composite-index range scan workload:

```sh
ant -Ddatacore.benchmark.workload=range-scan-composite-index datacore-embedded-benchmark
```

This query uses the same SQL as `range-scan`, but creates the benchmark table
with an `(amount, id)` index instead of the single-column `amount` index.

To test full-row range scan performance when all returned columns are available
from the range index, run the full-covering-index workload:

```sh
ant -Ddatacore.benchmark.workload=range-scan-full-covering-index datacore-embedded-benchmark
```

This query uses the same SQL as `range-scan-composite-index`, but creates an
`(amount, id, name)` index that covers every returned column in
`baseline_item`.

To verify that the optimizer chooses the covering index when both ordered
range indexes are available, run the dual-index range scan workload:

```sh
ant -Ddatacore.benchmark.workload=range-scan-dual-index datacore-embedded-benchmark
```

This query creates both `(amount, id)` and `(amount, id, name)` indexes, then
runs the same full-row ordered range scan.

To isolate the cost of returning the string payload, run the key-column range
scan workload:

```sh
ant -Ddatacore.benchmark.workload=range-scan-key-columns datacore-embedded-benchmark
```

This query uses the `(amount, id)` index and returns only `id` and `amount`,
leaving out the `name` string column returned by the full-row range scans.

To isolate the cost of returning only the string payload, run the name-column
range scan workload:

```sh
ant -Ddatacore.benchmark.workload=range-scan-name-column datacore-embedded-benchmark
```

This query uses the `(amount, id)` index and returns only `name`, leaving out
the integer columns returned by the full-row range scans.

To test whether returning `name` is slow because Derby must visit the base
table row, run the name-covering-index range scan workload:

```sh
ant -Ddatacore.benchmark.workload=range-scan-name-covering-index datacore-embedded-benchmark
```

This query uses the same SQL as `range-scan-name-column`, but creates an
`(amount, id, name)` index so the returned `name` value is available from the
range index.

To isolate the cost of Java string materialization, run the name-no-read range
scan workload:

```sh
ant -Ddatacore.benchmark.workload=range-scan-name-no-read datacore-embedded-benchmark
```

This query uses the same SQL as `range-scan-name-column`, but advances through
the result rows without calling `getString()` on the returned `name` column.

To isolate the cost of returning many rows without carrying table column data,
run the constant-row range scan workload:

```sh
ant -Ddatacore.benchmark.workload=range-scan-constant-row datacore-embedded-benchmark
```

This query uses the `(amount, id)` index and returns the constant value `1` for
each matching row.

To isolate predicate evaluation from result-row materialization, run the
count-only range scan workload:

```sh
ant -Ddatacore.benchmark.workload=range-scan-count datacore-embedded-benchmark
```

This query returns one aggregate count per range operation, so it is useful
for separating the cost of locating/counting matching rows from the cost of
returning each row to the caller.

---

**Innervex® DataCore Documentation**
