![Innervex DataCore logo](docs/assets/ivx-datacore-logo.png)

# Innervex® DataCore

Enterprise Data Platform & Integration Framework.

Built on Open Standards. Engineered for Enterprise Operations.

Innervex DataCore is an enterprise data platform, deployment framework,
integration architecture, and support ecosystem developed by Innervex
Technologies Private Limited.

Innervex DataCore may incorporate and utilise open-source technologies and
database engines as part of its implementation architecture. All underlying
open-source software components remain the property of their respective
copyright holders and are governed by their respective licences.

Innervex Technologies does not claim ownership of any third-party open-source
projects included within the Innervex DataCore ecosystem.

## Project Mission

The purpose of Innervex DataCore is to provide an enterprise data platform and
integration framework around trusted open technologies. The project starts from
Apache Derby's mature codebase and aims to improve performance, developer
experience, diagnostics, and long-term maintainability while preserving
Derby-compatible behavior.

Performance work will be benchmark-led and compatibility-safe. The goal is to
improve measurable behavior without casually breaking Derby-compatible SQL,
JDBC, transaction, storage, or recovery semantics.

See [`docs/PERFORMANCE.md`](docs/PERFORMANCE.md) for the project performance
plan and [`docs/INDEXING.md`](docs/INDEXING.md) for covering-index guidance.

## Public Project Notice

Innervex DataCore includes work based on Apache Derby. It is not an official
Apache Derby release and is not affiliated with, endorsed by, or maintained by
the Apache Software Foundation.

Innervex and Innervex DataCore branding belong to Innervex Technologies Private
Limited. Third-party open-source projects retain their own ownership, notices,
and licence terms.

Apache Derby source attribution, copyright notices, license terms, and NOTICE
content are preserved. New Innervex DataCore changes are developed in this
repository under the same Apache License, Version 2.0.

## Project Status

This repository begins from the Apache Derby source distribution. The original
Apache licensing, notices, and source attribution are preserved in `LICENSE`,
`NOTICE`, and `README`.

For a contributor-oriented map of the codebase, see
[`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md).

For the performance direction, see
[`docs/PERFORMANCE.md`](docs/PERFORMANCE.md).

For indexing guidance, see [`docs/INDEXING.md`](docs/INDEXING.md).

## Build

The primary build remains Ant-based:

```sh
ant -quiet clobber buildsource buildjars
```

The main generated product jars are created under `jars/sane/`.

`sane` is Derby's development build flavor. It keeps Derby sanity checks
enabled so internal problems are easier to catch while working on the engine.

Run the first embedded performance baseline with:

```sh
ant datacore-embedded-benchmark
```

Run the standard local benchmark suite and refresh the HTML report with:

```sh
bin/datacore-benchmark-suite
```

Refresh only the generated report with:

```sh
bin/datacore-benchmark-report
```

The report is published at
[`docs/performance-report.html`](docs/performance-report.html).

## NetBeans

Open the repository root directly in NetBeans:

```text
/Users/sureshn/Projects/db-derby-10/innervex-datacore
```

The root `nbproject` metadata exposes the main source modules and delegates
build actions to the top-level Ant `build.xml`.

Recommended local setup:

- JDK 21 or newer
- Ant 1.10.14 or newer
- Trust the project build script when NetBeans prompts

Build outputs such as `classes/`, `generated/`, `jars/`, and
`changenumber.properties` are local artifacts and are ignored by Git.

## Contributing

Contributions are welcome. Please keep changes small, focused, and compatible
with the existing Derby behavior unless a change is clearly documented as a
planned modernization.

For private vulnerability reporting, see [`SECURITY.md`](SECURITY.md).

Good first areas:

- Build and IDE cleanup
- Documentation improvements
- Tests and small bug fixes
- Benchmarking and performance measurement
- Locking, storage, and query-planning analysis

Before changing transaction, locking, storage, or SQL execution behavior, add or
identify tests that prove compatibility.

## Source Layout

- `java/org.apache.derby.engine` - core database engine
- `java/org.apache.derby.client` - network JDBC client
- `java/org.apache.derby.server` - network server
- `java/org.apache.derby.tools` - command-line and utility tools
- `java/org.apache.derby.commons` - shared support code
- `java/org.apache.derby.optionaltools` - optional Lucene, JSON, and dump tools
- `java/org.apache.derby.runner` - launcher support
- `java/org.apache.derby.tests` - Derby test suite

## License

Innervex DataCore is based on Apache Derby and is distributed under the Apache
License, Version 2.0. See `LICENSE` and `NOTICE` for details.

Copyright for new Innervex DataCore modifications, project branding,
project documentation, and project-specific assets:

```text
Copyright 2026 Innervex Technologies Private Limited.
```

See `NOTICE-INNERVEX` for project-specific attribution. Original Apache Derby
source attribution, copyright notices, license terms, and NOTICE content are
preserved.
