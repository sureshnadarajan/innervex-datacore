# Security Policy

Innervex DataCore is an independent open-source project based on Apache Derby.
It is not an official Apache Derby release and is not maintained by the Apache
Software Foundation.

The project is developed and maintained by Innervex Technologies Private
Limited.

## Reporting a Vulnerability

Please do not open public GitHub issues for suspected security vulnerabilities.

Instead, report privately to:

```text
admin@innervex.net
```

Include as much detail as you can safely share:

- Affected version or commit
- Operating system and Java version
- Steps to reproduce
- Expected behavior and observed behavior
- Logs, stack traces, or proof-of-concept details
- Whether the issue affects embedded mode, network server mode, tools, or
  optional modules

Please avoid sharing exploit code publicly before the project has had a chance
to investigate and prepare a fix.

## Scope

Security-sensitive areas include:

- SQL execution and authorization behavior
- Authentication and user management
- Network server access
- File system access through database features
- Import/export and tool execution paths
- Optional modules such as Lucene, JSON, and raw database inspection utilities
- Build, release, and dependency supply-chain integrity

## Supported Versions

This project is currently in early public setup. Until formal releases are
published, security fixes are expected to target the `main` branch.

## Upstream Derby Issues

If a vulnerability is inherited from Apache Derby, Innervex DataCore may track
the upstream fix and apply a compatible patch where appropriate. Apache Derby
security reports should also follow the official Apache project process.
