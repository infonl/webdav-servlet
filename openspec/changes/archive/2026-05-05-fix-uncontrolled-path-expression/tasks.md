## 1. Exception class

- [x] 1.1 Create `src/main/java/nl/info/webdav/exceptions/PathTraversalException.java` extending `WebdavException` with a single `String message` constructor and SPDX header

## 2. Core validation

- [x] 2.1 Add private static `assertSafePath(String path)` helper in `AbstractMethod` that splits the path on `/`, checks each segment for equality to `".."` or `"."`, and throws `PathTraversalException` if found
- [x] 2.2 Call `assertSafePath(result)` at the end of `AbstractMethod.getRelativePath()` before returning, so all method executors are covered automatically
- [x] 2.3 In `DoCopy.parseDestinationHeader()`, call `assertSafePath(destinationPath)` after `normalize()` runs and after the `null` check; catch `PathTraversalException`, send HTTP 400, and return `null`
- [x] 2.4 In `DoCopy.parseDestinationHeader()`, also add an explicit `null` check after `normalize()` (already present) that sends HTTP 400 — confirm this is already handled and add it if missing

## 3. Exception handling in executors

- [x] 3.1 In each method executor class that has a top-level `try/catch (WebdavException e)` block responding with HTTP 500, verify the existing handler correctly covers `PathTraversalException`; if any executor wraps exceptions differently, add a `catch (PathTraversalException e)` responding with HTTP 400

## 4. Tests

- [x] 4.1 Add `assertSafePath_rejectsTraversalSegment` and `assertSafePath_acceptsNormalPath` unit tests to a new `AbstractMethodTest` (or existing if present) using the public-facing behaviour (test via a concrete subclass or via the executor)
- [x] 4.2 Add traversal-path test case to `DoGetTest`: request with path `/safe/../../etc/passwd` expects HTTP 400
- [x] 4.3 Add traversal-path test case to `DoPutTest`: request with path `/safe/../../etc/cron.d/evil` expects HTTP 400
- [x] 4.4 Add traversal-path test case to `DoDeleteTest`: request with path `/../secret` expects HTTP 400
- [x] 4.5 Add traversal-path test case to `DoMoveTest`: traversal in source path expects HTTP 400
- [x] 4.6 Add traversal-path test case to `DoCopyTest`: traversal in `Destination` header expects HTTP 400
- [x] 4.7 Add traversal-path test case to `DoPropfindTest`: traversal in source path expects HTTP 400
- [x] 4.8 Add traversal-path test case to `DoLockTest`: traversal in source path expects HTTP 400

## 5. Verification

- [x] 5.1 Run `./gradlew spotlessApply` and fix any formatting issues
- [x] 5.2 Run `./gradlew test` and confirm all tests pass
- [x] 5.3 Run `./gradlew build` and confirm the build succeeds
