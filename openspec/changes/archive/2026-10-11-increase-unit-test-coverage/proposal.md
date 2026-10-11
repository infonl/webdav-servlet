## Why

Overall unit test coverage is 65.1% line / 52.4% branch (JaCoCo, 2026-10-11). The earlier `add-unit-test-coverage` change covered the locking classes and `AbstractMethod` utilities, but large parts of the request-handling code are still untested: servlet bootstrap (`WebdavServlet` at 5% line), XML-heavy method handlers (`DoPropfind` at 48%, `DoLock` at 70%), copy/move destination handling, and most error/edge branches in every `Do*` handler. These are exactly the paths touched by the recent security fixes (path traversal, XSS, XXE), so regressions there go unnoticed. Coverage is also not tracked against a target, so drops go unnoticed on PRs.

## What Changes

- Add `WebdavServlet` tests for `init()` parameter handling, store construction via reflection, and root-path resolution (including missing parameter and `*WAR-FILE-ROOT*` failure).
- Extend `WebDavServletBeanTest` to cover `destroy()`, unknown-method handling, and exception-to-status mapping in `service()`.
- Extend `DoPropfindTest` / `DoProppatchTest` for the uncovered `parseProperties` branches (individual live properties, `allprop`/`propname`, lock discovery output, not-found properties) and PROPPATCH set/remove error paths.
- Extend `DoLockTest` / `DoUnlockTest` for null-resource locks, lock refresh, timeout parsing, Mac Finder workaround, and lock-failure reporting.
- Extend `DoCopyTest`, `DoMoveTest`, `DoDeleteTest` for destination header parsing/normalization, overwrite semantics, folder recursion and error collection.
- Extend `DoGetTest`, `DoHeadTest`, `DoPutTest`, `DoMkcolTest`, `DoOptionsTest` for remaining status-code branches (read-only, locked, not found, default index file, `instead-of-404`, lazy folder creation).
- Extend `LocalFileSystemStoreTest` for `begin()` root validation and I/O failure paths.
- Add tests for the Catalina utilities actually used in production: `RequestUtil.URLDecode`, `URLEncoder.encode`, `XMLHelper`.
- Extend `ResourceLocksTest` / `LockedObjectTest` for `cleanLockedObjects`, `checkChildren`, and child-removal branches.
- Add a `codecov.yml` with project and patch coverage targets so Codecov reports a (non-blocking) failing PR status when coverage misses the target. The local Gradle build never fails on coverage.
- No production code changes.

## Capabilities

### New Capabilities

- `servlet-bootstrap-unit-tests`: Tests for `WebdavServlet` init/store construction/root resolution and `WebDavServletBean` lifecycle and dispatch error handling.
- `propfind-proppatch-unit-tests`: Tests for PROPFIND property rendering and PROPPATCH request handling branches.
- `lock-method-unit-tests`: Tests for LOCK/UNLOCK method handler branches.
- `copy-move-delete-unit-tests`: Tests for COPY/MOVE/DELETE destination parsing, overwrite and recursion branches.
- `read-write-method-unit-tests`: Tests for GET/HEAD/PUT/MKCOL/OPTIONS status-code branches.
- `local-filesystem-store-unit-tests`: Tests for `LocalFileSystemStore` initialization and failure paths.
- `catalina-utility-unit-tests`: Tests for the production-used Catalina helpers (`URLDecode`, `URLEncoder`, `XMLHelper`).
- `coverage-threshold-reporting`: Non-blocking Codecov PR status for coverage targets; local build unaffected.

### Modified Capabilities

- `resource-locks-unit-tests`: Adds requirements for lock cleanup (`cleanLockedObjects`) and unlock edge cases.
- `locked-object-unit-tests`: Adds requirements for `checkChildren` and child/owner removal branches.

## Impact

- New/extended test classes under `src/test/java/nl/info/webdav/**`.
- New `codecov.yml` at repo root. No changes to `build.gradle.kts`; the existing Codecov upload in `build-test-publish.yml` is reused.
- Codecov statuses must not be marked as required checks in branch protection, so PRs below target can still be merged.
- No production code, public API, or runtime dependency changes.
