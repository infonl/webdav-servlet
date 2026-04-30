# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Build
./gradlew build

# Run tests
./gradlew test

# Run a single test class
./gradlew test --tests "nl.info.webdav.methods.DoGetTest"

# Lint / code style (Spotless)
./gradlew spotlessCheck
./gradlew spotlessApply   # auto-fix

# Dependency locking
./gradlew dependencies --write-locks
```

## Architecture

This is a Java library (~40 source files) that implements a WebDAV servlet on top of Jakarta Servlet API. It is published to Maven Central as `nl.info.webdav:webdav-servlet`.

**Request flow:**

1. `WebdavServlet` (concrete servlet) reads `web.xml` init params, reflectively instantiates the configured `IWebdavStore` implementation, then delegates to `WebDavServletBean`.
2. `WebDavServletBean` (the reusable core, extends `HttpServlet`) owns a `HashMap<String, IMethodExecutor>` that maps HTTP method names to handler objects. Every request goes through `service()`, which opens a store transaction, looks up the handler, calls `execute()`, then commits or rolls back.
3. Each HTTP method (`GET`, `PUT`, `DELETE`, `LOCK`, `PROPFIND`, etc.) has its own class in `methods/` extending `AbstractMethod implements IMethodExecutor`. `DoMove` and `DoCopy` share `DoDelete` and `DoCopy` as collaborators.
4. `IWebdavStore` is **the integration point** — consumers implement this interface to plug in their own storage backend. `LocalFileSystemStore` is the bundled reference implementation.
5. `locking/ResourceLocks` manages in-memory concurrent-access locking (path-level, not WebDAV protocol locks). `LockedObject` tracks individual lock state.
6. `fromcatalina/` contains utility classes (`URLEncoder`, `XMLWriter`, `XMLHelper`, `RequestUtil`) lifted from Apache Catalina — do not refactor them unnecessarily.

**Key interfaces:**

| Interface | Purpose |
|---|---|
| `IWebdavStore` | Storage backend — implement this to use the library |
| `IMethodExecutor` | Single WebDAV method handler (`execute(transaction, req, resp)`) |
| `ITransaction` | Opaque transaction handle passed through the request lifecycle |
| `IResourceLocks` | In-memory locking contract implemented by `ResourceLocks` |

**Testing:** JUnit 5 + JMock 2. All method tests extend `MockTest` (in `testutil/`), which sets up a shared `Mockery` with threading support and factory helpers for `StoredObject`/`LockedObject`. Tests mock `IWebdavStore`, `ITransaction`, `HttpServletRequest`, and `HttpServletResponse`.

## Conventions

- **SPDX headers** required on all source files. New files get `SPDX-FileCopyrightText: <YEAR> INFO.nl` + `SPDX-License-Identifier: EUPL-1.2+`. When modifying existing files, append `, <YEAR> INFO.nl` to the existing copyright line.
- **Conventional Commits** for all commit messages and PR titles: `<type>[optional scope]: <description>`. PR footer must reference the JIRA issue: `Solves PZ-XXX`.
- **Spotless** enforces code style (Eclipse formatter via `config/webdav-servlet.xml`, import order via `config/importOrder.txt`). Run `spotlessApply` before committing.
- Java 21, Jakarta Servlet API 6.1 (compile-only — the servlet container provides it at runtime).
- Versioning via Axion Release Plugin (git-tag-based); publishing via Gradle Nexus Publish Plugin. Both are CI-only operations.
