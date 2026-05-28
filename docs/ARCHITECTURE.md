**INNERVEX TECHNOLOGIES PRIVATE LIMITED**

# INNERVEX® DATACORE

### Enterprise Data Platform & Integration Framework

**Version:** 1.0  

**Built on Open Standards. Engineered for Enterprise Operations.**

---

| INNERVEX TECHNOLOGIES PRIVATE LIMITED | INNERVEX® DATACORE<br>Enterprise Data Platform & Integration Framework | Version: 1.0<br>Document Type: Product Architecture & Technical Overview |
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

# 2. Architecture Overview

**INNERVEX® DATACORE**  
Enterprise Data Platform & Integration Framework  
Built on Open Standards. Engineered for Enterprise Operations.

Innervex DataCore begins from Apache Derby 10.17.1.0. The current codebase is
best understood as a modular Java relational database with an embedded engine,
network access layer, command-line tools, optional integrations, and a broad
compatibility test suite.

This document is a practical map for contributors. It describes where the major
parts live and how to approach modernization without breaking Derby-compatible
behavior.

The long-term purpose is to make DataCore an Innervex-supported enterprise data
platform and integration framework. Performance improvements should be driven
by repeatable measurements, targeted technical work, and careful protection of
database correctness.

## High-Level Shape

```text
Application / Tooling
        |
        v
JDBC APIs and Derby Tools
        |
        v
SQL compiler, optimizer, execution engine
        |
        v
Transaction, locking, storage, logging, recovery
        |
        v
Database files
```

The embedded database engine is the core. Network server, client drivers, tools,
and optional extensions build around that core.

## Main Source Modules

- `java/org.apache.derby.engine` - core database engine, SQL compilation,
  execution, catalog, storage, transactions, locking, logging, recovery, and
  embedded JDBC behavior.
- `java/org.apache.derby.commons` - shared APIs and common support used by
  engine, client, server, tools, tests, and optional modules.
- `java/org.apache.derby.client` - network JDBC client implementation.
- `java/org.apache.derby.server` - Derby network server and DRDA handling.
- `java/org.apache.derby.tools` - command-line and developer tools such as
  `ij`, `dblook`, import/export helpers, and related utilities.
- `java/org.apache.derby.optionaltools` - optional integrations and utilities,
  including Lucene support, JSON helpers, and raw database inspection tools.
- `java/org.apache.derby.runner` - launcher support for running common Derby
  tools from a single jar.
- `java/org.apache.derby.tests` - Derby compatibility and regression tests.

## Supporting Folders

- `java/build` - build-time helper code used by Ant.
- `java/demo` - sample applications and demo databases.
- `java/locales` - localized messages and generated locale module metadata.
- `java/storeless` - storeless service support.
- `java/stubs` - compile-time stubs for optional external APIs.
- `java/pptesting` - package-private testing support.
- `tools` - Ant support, bundled build dependencies, release utilities, and IDE
  metadata.
- `plugins` - legacy Eclipse plugin assets.
- `maven2` - Maven publishing metadata inherited from Derby.

## Build Model

The primary build remains Ant-based.

```sh
ant -quiet clobber buildsource buildjars
```

Generated development jars are placed under:

```text
jars/sane/
```

`sane` means the build includes Derby sanity checks. This is useful during
engine development because internal consistency problems are easier to detect.

## Compatibility Baseline

The current Derby locking and storage behavior is the compatibility baseline.
That means modernization should preserve observable database behavior unless the
change is intentionally documented and tested.

Important areas to protect:

- SQL behavior and JDBC compatibility
- Transaction isolation and rollback behavior
- Lock acquisition, lock release, and deadlock handling
- Crash recovery and log replay
- On-disk storage compatibility
- System catalog behavior
- Existing Derby tests and documented public APIs

## Modernization Direction

Good modernization work should be incremental.

- Improve build and developer ergonomics without changing database behavior.
- Add documentation before changing deep engine contracts.
- Add focused tests around locking, transactions, storage, and recovery before
  modifying those systems.
- Benchmark before and after performance-sensitive changes.
- Keep optional features optional.

Future research areas:

- Cleaner module boundaries
- Clearer public versus internal APIs
- Better diagnostics around locks, transactions, and query plans
- Clearer indexing guidance for common query shapes
- Modern packaging and publishing workflow
- MVCC research as a long-term investigation, not an immediate replacement for
  Derby locking semantics

## Related Notes

- [Indexing notes](INDEXING.md) - practical covering-index guidance proven by
  the local benchmark suite and Derby regression tests.

## Contributor Rule of Thumb

Small, well-tested improvements are preferred over broad rewrites. The database
engine is mature code; correctness, compatibility, and recoverability matter
more than cosmetic modernization.

---

**Innervex® DataCore Documentation**
