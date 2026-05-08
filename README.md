# Innervex DataCore

Enterprise-grade embedded relational database technology for Java platforms,
based on Apache Derby.

Innervex DataCore is an independent open-source continuation and modernization
workspace derived from Apache Derby 10.17.1.0. The project keeps Derby's proven
embedded relational database foundation while creating room for focused
enterprise maintenance, usability improvements, and Java platform evolution.

## Project Status

This repository begins from the Apache Derby source distribution. The original
Apache licensing, notices, and source attribution are preserved in `LICENSE`,
`NOTICE`, and `README`.

## Build

The primary build remains Ant-based:

```sh
ant -quiet clobber buildsource buildjars
```

The main generated product jars are created under `jars/sane/`.

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
