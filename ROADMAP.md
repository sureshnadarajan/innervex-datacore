# Innervex DataCore Roadmap

This roadmap is intentionally incremental. The project starts from Apache Derby
10.17.1.0 and keeps compatibility as the baseline.

## Phase 1: Project Foundation

- Keep the Ant build working
- Keep NetBeans project support working
- Preserve Apache license and notices
- Improve public README and contribution docs
- Keep generated build output out of Git

## Phase 2: Developer Experience

- Document source layout
- Add code navigation notes
- Add common build/test commands
- Reduce IDE noise
- Improve Git-based build revision metadata

## Phase 3: Measurement

- Add repeatable benchmarks
- Identify lock contention cases
- Measure embedded and client/server workloads
- Compare Derby behavior against H2, PostgreSQL, and Firebird where useful

## Phase 4: Safe Modernization

- Clean up low-risk code paths
- Improve diagnostics and logging
- Modernize build/test tooling carefully
- Keep JDBC and SQL compatibility intact

## Phase 5: Engine Research

- Study Derby locking and transaction internals
- Explore MVCC concepts as research, not an immediate rewrite
- Prototype only behind clear experimental boundaries
