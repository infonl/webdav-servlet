## Why

The locking subsystem (`ResourceLocks`, `LockedObject`) and shared utilities in `AbstractMethod` contain non-trivial business logic — exclusive/shared lock conflict detection, owner tracking, timeout management, path parsing, ETag generation, lock-token header parsing — but have zero direct unit tests. These are the highest-leverage classes to test because bugs there silently affect every WebDAV method.

## What Changes

- Add `ResourceLocksTest` covering lock/unlock lifecycle, exclusive vs. shared conflict, timeout expiry, and cleanup.
- Add `LockedObjectTest` covering owner add/remove, child management, `checkLocks` depth semantics, and `hasExpired`.
- Add `AbstractMethodTest` covering `getParentPath`, `getCleanPath`, `getDepth`, `getETag`, `getLockIdFromIfHeader`, `getLockIdFromLockTokenHeader`, and `checkLocks`.
- No changes to production code.

## Capabilities

### New Capabilities

- `resource-locks-unit-tests`: Direct unit tests for `ResourceLocks` locking lifecycle and conflict detection.
- `locked-object-unit-tests`: Direct unit tests for `LockedObject` state management and lock-check logic.
- `abstract-method-unit-tests`: Unit tests for shared utility methods in `AbstractMethod`.

### Modified Capabilities

## Impact

- New test files under `src/test/java/nl/info/webdav/locking/` and `src/test/java/nl/info/webdav/methods/`.
- No production code changes; no API or dependency changes.
