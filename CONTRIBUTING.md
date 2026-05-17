# Contributing to Innervex DataCore

Thank you for considering a contribution.

Innervex DataCore is an independent open-source project based on Apache Derby.
The goal is to preserve Derby compatibility while improving maintainability,
developer experience, and long-term platform readiness.

The project is developed and maintained by Innervex Technologies Private
Limited.

## Contribution Guidelines

- Keep Apache license headers intact.
- Keep changes small and focused.
- Prefer compatibility-preserving improvements.
- Add tests when behavior changes.
- Avoid large rewrites without prior design discussion.
- Do not rename `org.apache.derby` packages without a migration plan.

## Good First Contributions

- Documentation cleanup
- NetBeans or Ant build improvements
- Small warnings and compatibility fixes
- Test stabilization
- Benchmark setup
- Code navigation notes for engine, storage, locking, and SQL layers

## Sensitive Areas

Please be extra careful with:

- Transaction handling
- Locking and isolation
- Raw store and recovery
- B-tree and heap storage
- SQL compilation and execution
- JDBC compatibility

These areas are central to database correctness and should be changed only with
tests and a clear explanation.
