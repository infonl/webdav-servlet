## 1. Baseline

- [x] 1.1 Run `./gradlew test jacocoTestReport` and record baseline line/branch coverage (expected 65.1% / 52.4%) in the PR description notes
- [x] 1.2 Keep a running list of spec/behavior discrepancies (candidate bugs) found while writing characterization tests

## 2. PROPFIND / PROPPATCH (`propfind-proppatch-unit-tests`)

- [x] 2.1 `DoPropfindTest`: request each live property individually on a file; assert values in 200 propstat
- [x] 2.2 `DoPropfindTest`: same properties on a folder; assert `<collection/>` and file-only props in 404 propstat
- [x] 2.3 `DoPropfindTest`: unknown property reported in 404 propstat
- [x] 2.4 `DoPropfindTest`: `<propname/>` and `<allprop/>` with `Depth: 1` on folder with children
- [x] 2.5 `DoPropfindTest`: `lockdiscovery` for exclusive and shared locks (scope, depth, owner, timeout, token)
- [x] 2.6 `DoPropfindTest`: not found (404), `AccessDeniedException` (403), malformed body (500)
- [x] 2.7 `DoProppatchTest`: combined `<set>`/`<remove>` produces a 207 multistatus with 200 per property and no store mutation (nothing is persisted; findings #3)
- [x] 2.8 `DoProppatchTest`: read-only (403), locked (423), not found (404), malformed body (500)

## 3. LOCK / UNLOCK (`lock-method-unit-tests`)

- [x] 3.1 `DoLockTest`: `Timeout` parsing — missing, `Second-N`, above max, `Infinite`, malformed (parameterized)
- [x] 3.2 `DoLockTest`: null-resource lock success (final status 200, the 201 is overwritten; findings #14) and missing parent (parent folder is created; findings #15)
- [x] 3.3 `DoLockTest`: Transmit user-agent workaround on null-resource lock (204 is also overwritten by 200)
- [x] 3.4 `DoLockTest`: lock refresh with valid `If` token and with unknown token (412)
- [x] 3.5 `DoLockTest`: Darwin user-agent lock success and conflict (plain `sendError(423)`, not a 207; findings #19)
- [x] 3.6 `DoLockTest`: body parsing — shared scope, owner `<href>`, missing locktype (400), malformed XML; read-only (403)
- [x] 3.7 `DoUnlockTest`: missing `Lock-Token` (400), unknown token (400), token from another owner (lock removed, 204; findings #6), null-resource removal on last unlock

## 4. COPY / MOVE / DELETE (`copy-move-delete-unit-tests`)

- [x] 4.1 `DoCopyTest`: `Destination` missing (400), absolute URL stripping, percent-decoding, dot-segment normalization, destination equals source (403)
- [x] 4.2 `DoCopyTest`: `Overwrite: F` with existing destination (412), `Overwrite: T` (204), new destination (201)
- [x] 4.3 `DoCopyTest`: folder copy with `Depth: 0`; per-child exception→status in multistatus (parameterized)
- [x] 4.4 `DoCopyTest`: read-only (403), source not found (404), destination locked (423)
- [x] 4.5 `DoMoveTest`: read-only, missing destination, source/destination locked, store exception→status (parameterized)
- [x] 4.6 `DoDeleteTest`: read-only (403), locked (423), child failure multistatus, root delete exception→status (parameterized)

## 5. Servlet bootstrap (`servlet-bootstrap-unit-tests`)

- [x] 5.1 `WebdavServletTest`: default/empty `ResourceHandlerImplementation` → `LocalFileSystemStore` with `@TempDir` root
- [x] 5.2 `WebdavServletTest`: `constructStore` valid class, unknown class, class without `(File)` constructor
- [x] 5.3 `WebdavServletTest`: missing `rootpath`; `*WAR-FILE-ROOT*` without `/WEB-INF/` in code source
- [x] 5.4 `WebdavServletTest`: optional params (`lazyFolderCreationOnPut`, `no-content-length-headers`, invalid number)
- [x] 5.5 `WebDavServletBeanTest`: `destroy()` with and without store
- [x] 5.6 `WebDavServletBeanTest`: unknown method (501), `UnauthenticatedException` (403), `PathTraversalException` (400), `IOException` (500), runtime exception (rollback + `ServletException`)

## 6. GET / HEAD / PUT / MKCOL / OPTIONS (`read-write-method-unit-tests`)

- [x] 6.1 `DoGetTest`/`DoHeadTest`: default index file, `instead-of-404` fallback, plain 404, `If-None-Match` (304)
- [x] 6.2 `DoGetTest`/`DoHeadTest`: content-length suppression, `AccessDeniedException` (403), HEAD on folder, listing CSS
- [x] 6.3 `DoPutTest`: read-only, missing parent with/without lazy creation, locked, overwrite (204), PUT on folder
- [x] 6.4 `DoMkcolTest`: body present (ignored, 201; no 415 — findings #25), missing parent (409), exists (405 + `Allow`), null resource replaced, store exceptions
- [x] 6.5 `DoOptionsTest`: store exception→status

## 7. Locking (`resource-locks-unit-tests`, `locked-object-unit-tests`)

- [x] 7.1 `ResourceLocksTest`: nested temporary lock cleanup, owned child blocks parent cleanup, multiple sibling cleanup
- [x] 7.2 `ResourceLocksTest`: unlock unknown ID and unlock by non-owner leave state unchanged
- [x] 7.3 `LockedObjectTest`: `checkChildren` — exclusive child, shared child, grandchild, depth 0
- [x] 7.4 `LockedObjectTest`: remove only child and middle child (normal + temporary); remove absent/last owner

## 8. Store and Catalina utilities (`local-filesystem-store-unit-tests`, `catalina-utility-unit-tests`)

- [x] 8.1 `LocalFileSystemStoreTest`: `begin()` creates missing root; uncreatable root (file in parent path) throws
- [x] 8.2 `LocalFileSystemStoreTest`: `createResource`, `setResourceContent`, `getResourceContent` failure paths
- [x] 8.3 Create `fromcatalina/RequestUtilTest`: `URLDecode` overloads — UTF-8, `+`, null, unsupported encoding
- [x] 8.4 Create `fromcatalina/URLEncoderTest`: safe chars unchanged, unsafe/non-ASCII percent-encoded
- [x] 8.5 Create `fromcatalina/XMLHelperTest`: `findSubElement` found/absent/text-only, `getPropertiesFromXML`

## 9. Coverage reporting (`coverage-threshold-reporting`)

- [x] 9.1 Re-run `./gradlew test jacocoTestReport`; record final line/branch coverage
- [x] 9.2 Add `codecov.yml` (with SPDX header) at repo root: `project` target = Codecov-reported coverage rounded down (83%; Codecov counts partially covered lines as misses, so it reads lower than JaCoCo line coverage) with `threshold: 1%`, `patch` target 80%
- [x] 9.3 Validate `codecov.yml` (`curl --data-binary @codecov.yml https://codecov.io/validate`) and confirm `build.gradle.kts` has no coverage verification wired into `check`
- [x] 9.4 After opening the PR, confirm `codecov/project` and `codecov/patch` statuses appear and are not required checks in branch protection
- [x] 9.5 Update spec scenarios that differ from actual behavior; list candidate bugs in the PR description

## 10. Finalize

- [x] 10.1 Add SPDX headers to new test files; append `, 2026 INFO.nl` to modified files' copyright lines
- [x] 10.2 Run `./gradlew spotlessApply` and `./gradlew build`
