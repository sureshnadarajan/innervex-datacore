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

The benchmark creates a local database under `generated/performance/`, runs a
small insert, lookup, update, and grouped scan workload, then prints timing
results. The default row count is intentionally small so contributors can run it
quickly during development.

To run a larger local sample:

```sh
ant -Ddatacore.benchmark.rows=50000 datacore-embedded-benchmark
```
