## 1. ResourceLocks Tests

- [x] 1.1 Create `src/test/java/nl/info/webdav/locking/ResourceLocksTest.java` extending `MockTest` with a shared `ResourceLocks` instance reset before each test
- [x] 1.2 Test: exclusive lock acquired on unlocked resource returns `true` and object is retrievable by path
- [x] 1.3 Test: second exclusive lock on same path returns `false`
- [x] 1.4 Test: two shared locks on same path both succeed and owner array has two entries
- [x] 1.5 Test: exclusive lock rejected when shared lock already present
- [x] 1.6 Test: `unlock()` with correct id and owner returns `true` and object has no owner after unlock
- [x] 1.7 Test: temporary lock created via `lock(..., true)` is retrievable by `getTempLockedObjectByPath` and released by `unlockTemporaryLockedObjects`
- [x] 1.8 Test: `checkTimeouts(transaction, false)` removes a lock whose `_expiresAt` is in the past
- [x] 1.9 Test: `getLockedObjectByID` and `getTempLockedObjectByID` return correct objects

## 2. LockedObject Tests

- [x] 2.1 Create `src/test/java/nl/info/webdav/locking/LockedObjectTest.java` with a `ResourceLocks` instance and a helper to build `LockedObject` instances at specific paths
- [x] 2.2 Test: `addLockedObjectOwner` on null-owner object returns `true` and owner array has one entry
- [x] 2.3 Test: second distinct owner added to shared lock returns `true` and both owners present
- [x] 2.4 Test: duplicate owner rejected by `addLockedObjectOwner`
- [x] 2.5 Test: `removeLockedObjectOwner` on single owner sets `_owner` to `null`
- [x] 2.6 Test: `removeLockedObjectOwner` removes only the target owner when two owners present
- [x] 2.7 Test: `addChild` on childless object creates one-element `_children` array
- [x] 2.8 Test: `checkLocks(false, 0)` returns `true` on unowned, childless object
- [x] 2.9 Test: `checkLocks(true, 0)` returns `false` when object already has an exclusive owner
- [x] 2.10 Test: `hasExpired()` returns `false` immediately after `refreshTimeout(3600)`
- [x] 2.11 Test: `hasExpired()` returns `true` when `_expiresAt` is set to `1`
- [x] 2.12 Test: `removeLockedObject()` removes object from `ResourceLocks._locks` and `_locksByID`

## 3. AbstractMethod Tests

- [x] 3.1 Create `src/test/java/nl/info/webdav/methods/AbstractMethodTest.java` with a minimal anonymous-subclass of `AbstractMethod` and a `Mockery` for HTTP/lock dependencies
- [x] 3.2 Test: `getParentPath("/foo/bar/baz")` returns `"/foo/bar"`
- [x] 3.3 Test: `getParentPath("/foo")` returns `""`
- [x] 3.4 Test: `getParentPath("foo")` returns `null`
- [x] 3.5 Test: `getCleanPath("/foo/bar/")` returns `"/foo/bar"`
- [x] 3.6 Test: `getCleanPath("/")` returns `"/"`
- [x] 3.7 Test: `getDepth` returns `0` for header `"0"`, `1` for `"1"`, `3` for `"infinity"`, `3` for absent header
- [x] 3.8 Test: `getETag` for a resource with known length and `lastModified` produces `W/"<length>-<millis>"`
- [x] 3.9 Test: `getETag(null)` produces `W/"-"`
- [x] 3.10 Test: `getLockIdFromIfHeader` parses single locktoken header into `ids[0]`
- [x] 3.11 Test: `getLockIdFromIfHeader` returns `null` when `If` header is absent
- [x] 3.12 Test: `getLockIdFromLockTokenHeader` extracts token from `<opaquelocktoken:…>` header
- [x] 3.13 Test: `checkLocks` returns `true` when no `LockedObject` at path
- [x] 3.14 Test: `checkLocks` returns `true` when lock at path is shared
- [x] 3.15 Test: `checkLocks` returns `false` when exclusive lock exists and If header token does not match
