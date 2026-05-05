## 1. Add SPDX headers to fromcatalina/ files (Apache-2.0)

- [x] 1.1 Add SPDX headers to `src/main/java/nl/info/webdav/fromcatalina/URLEncoder.java`
- [x] 1.2 Add SPDX headers to `src/main/java/nl/info/webdav/fromcatalina/XMLWriter.java`
- [x] 1.3 Add SPDX headers to `src/main/java/nl/info/webdav/fromcatalina/XMLHelper.java`
- [x] 1.4 Add SPDX headers to `src/main/java/nl/info/webdav/fromcatalina/RequestUtil.java`

## 2. Add SPDX headers to main source files (EUPL-1.2+)

- [x] 2.1 Add SPDX headers to top-level interfaces and classes: `IMethodExecutor`, `IWebdavStore`, `ITransaction`, `IMimeTyper`, `WebdavStatus`, `StoredObject`, `WebdavServlet`
- [x] 2.2 Add SPDX headers to `methods/` main sources: `AbstractMethod`, `DeterminableMethod`, `DoCopy`, `DoDelete`, `DoGet`, `DoHead`, `DoLock`, `DoMkcol`, `DoMove`, `DoNotImplemented`, `DoOptions`, `DoPropfind`, `DoProppatch`, `DoPut`, `DoUnlock`
- [x] 2.3 Add SPDX headers to `locking/` main sources: `IResourceLocks`, `LockedObject`, `ResourceLocks`
- [x] 2.4 Add SPDX headers to `exceptions/` main sources: `AccessDeniedException`, `LockFailedException`, `ObjectAlreadyExistsException`, `ObjectNotFoundException`, `UnauthenticatedException`, `WebdavException`

## 3. Add SPDX headers to test source files (EUPL-1.2+)

- [x] 3.1 Add SPDX headers to top-level test: `WebdavServletTest`
- [x] 3.2 Add SPDX headers to `methods/` test sources: `DoDeleteTest`, `DoGetTest`, `DoHeadTest`, `DoLockTest`, `DoMkcolTest`, `DoMoveTest`, `DoNotImplementedTest`, `DoOptionsTest`, `DoPropfindTest`, `DoProppatchTest`, `DoPutTest`, `DoUnlockTest`, `TestingOutputStream`
- [x] 3.3 Add SPDX headers to `testutil/` sources: `DelegatingServletInputStream`, `MockPrincipal`, `MockTest`

## 4. Verify

- [x] 4.1 Confirm no `.java` file in `src/` is missing `SPDX-License-Identifier` (`find src -name "*.java" | xargs grep -L "SPDX-License-Identifier"` should return nothing)
- [x] 4.2 Run `./gradlew spotlessCheck` to confirm formatter is happy with the changes
- [x] 4.3 Run `./gradlew test` to confirm no test breakage
