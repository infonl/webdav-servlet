## 1. Implement Path Validation in LocalFileSystemStore

- [x] 1.1 Add `_rootCanonical` field and resolve it in constructor (add `throws IOException` to constructor signature)
- [x] 1.2 Add private `resolveFile(String uri)` helper that calls `getCanonicalPath()`, checks the result starts with `_rootCanonical`, and throws `WebdavException` on traversal
- [x] 1.3 Replace `new File(_root, uri)` at line 86 (`createFolder`) with `resolveFile(uri)`
- [x] 1.4 Replace `new File(_root, uri)` at line 93 (`createResource`) with `resolveFile(uri)`
- [x] 1.5 Replace `new File(_root, uri)` at line 113 (`setResourceContent`) with `resolveFile(uri)`
- [x] 1.6 Replace `new File(_root, uri)` at line 153 (`getChildrenNames`) with `resolveFile(uri)`
- [x] 1.7 Replace `new File(_root, uri)` at line 172 (`removeObject`) with `resolveFile(uri)`
- [x] 1.8 Replace `new File(_root, uri)` at line 184 (`getResourceContent`) with `resolveFile(uri)`
- [x] 1.9 Replace `new File(_root, uri)` at lines 199 and 206 (`getResourceLength`, `getStoredObject`) with `resolveFile(uri)`

## 2. Update Call Sites for Constructor Change

- [x] 2.1 Update any production code that instantiates `LocalFileSystemStore(File)` to handle the new `IOException` (check `WebdavServlet` and any reflective instantiation)
- [x] 2.2 Update test helpers / `MockTest` / existing tests that construct `LocalFileSystemStore` to handle `IOException`

## 3. Tests

- [x] 3.1 Add `LocalFileSystemStoreTest` (JUnit 5) with a temp directory as root
- [x] 3.2 Test: normal URI within root succeeds for each store method (happy path)
- [x] 3.3 Test: URI with `/../` traversal throws `WebdavException` from `resolveFile`
- [x] 3.4 Test: URI equal to root (`/`) is accepted
- [x] 3.5 Test: symlink inside root pointing outside throws `WebdavException` (if OS supports symlinks)

## 4. Code Style and Verification

- [x] 4.1 Run `./gradlew spotlessApply` and fix any style violations
- [x] 4.2 Run `./gradlew build` — all tests pass, zero CodeQL path-injection alerts remain
- [x] 4.3 Update SPDX copyright header in `LocalFileSystemStore.java` (append `, 2026 INFO.nl`)
