# Innervex DataCore Performance Plan

Innervex DataCore exists to give Innervex an owned, maintained, and measurable
embedded relational database foundation. Performance work in this repository
must be guided by repeatable evidence, not by guesswork or broad rewrites.

## Goals

- Establish clear baseline numbers for the current DataCore engine
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

To run the concurrent mixed transaction workload:

```sh
ant -Ddatacore.benchmark.workload=concurrent-transaction-mixed datacore-embedded-benchmark
```

The concurrent-transaction-mixed workload runs reader threads while one writer
updates rows using one commit per row. This combines locking pressure with
small-transaction commit overhead.

To run the insert-heavy workload:

```sh
ant -Ddatacore.benchmark.workload=insert-heavy datacore-embedded-benchmark
```

The insert-heavy workload creates the table and measures only the batch insert
phase. This gives DataCore a simple write-throughput baseline before adding
deeper storage-engine changes.

To run the update-heavy workload:

```sh
ant -Ddatacore.benchmark.workload=update-heavy datacore-embedded-benchmark
```

The update-heavy workload loads the table, then measures updates across all
rows. This helps track write cost for existing records separately from initial
data loading.

To run the delete-heavy workload:

```sh
ant -Ddatacore.benchmark.workload=delete-heavy datacore-embedded-benchmark
```

The delete-heavy workload loads the table, then measures deletes across all
rows. This gives DataCore a separate baseline for row removal and index cleanup.

To run the transaction-heavy workload:

```sh
ant -Ddatacore.benchmark.workload=transaction-heavy datacore-embedded-benchmark
```

The transaction-heavy workload inserts rows using one commit per row. This
measures transaction commit overhead, which is important for enterprise
applications that perform many small units of work.

To run the indexed range-scan workload:

```sh
ant -Ddatacore.benchmark.workload=range-scan datacore-embedded-benchmark
```

The range-scan workload loads the table, then repeatedly queries ranges over
the indexed `amount` column. The number of range queries can be changed with
`-Ddatacore.benchmark.ranges=10000`.
