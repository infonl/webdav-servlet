## Purpose
Direct unit tests for `ResourceLocks`: lock and unlock lifecycle, exclusive versus shared conflicts, timeouts, lookup and cleanup of released locks.
## Requirements
### Requirement: Exclusive lock acquired on unlocked resource
The system SHALL grant an exclusive lock when no prior lock exists on the path.

#### Scenario: Exclusive lock on fresh path
- **WHEN** `lock()` is called with `exclusive=true` on a path that has no existing lock
- **THEN** the method returns `true` and `getLockedObjectByPath` returns a non-null `LockedObject` with the owner set

### Requirement: Exclusive lock blocked by existing exclusive lock
The system SHALL reject a second exclusive lock when the path is already exclusively locked.

#### Scenario: Second exclusive lock rejected
- **WHEN** `lock()` is called with `exclusive=true` on a path that already holds an exclusive lock
- **THEN** the method returns `false`

### Requirement: Shared lock accepted alongside another shared lock
The system SHALL allow multiple shared locks on the same path.

#### Scenario: Two shared locks on same path
- **WHEN** `lock()` is called with `exclusive=false` for owner A, then again for owner B on the same path
- **THEN** both calls return `true` and the `LockedObject` reports two owners

### Requirement: Exclusive lock blocked by existing shared lock
The system SHALL reject an exclusive lock when the path already holds a shared lock.

#### Scenario: Exclusive lock rejected when shared lock present
- **WHEN** a shared lock exists on a path and `lock()` is called with `exclusive=true` on that path
- **THEN** the method returns `false`

### Requirement: Unlock releases an exclusive lock
The system SHALL remove the lock when `unlock()` is called with the correct token and owner.

#### Scenario: Unlock by id removes lock
- **WHEN** an exclusive lock exists and `unlock()` is called with its lock id and original owner
- **THEN** `unlock()` returns `true` and `getLockedObjectByPath` no longer returns a locked object with an owner

### Requirement: Temporary lock lifecycle
The system SHALL create a temporary lock via `lock(..., temporary=true)` and release it via `unlockTemporaryLockedObjects`.

#### Scenario: Temporary lock created and released
- **WHEN** `lock()` is called with `temporary=true` for a path and owner
- **THEN** `getTempLockedObjectByPath` returns a non-null object, and after `unlockTemporaryLockedObjects` the object has no owner

### Requirement: Expired locks removed on timeout check
The system SHALL remove locks whose `_expiresAt` is in the past when `checkTimeouts` is called.

#### Scenario: Expired non-temporary lock removed
- **WHEN** a non-temporary lock exists and its `_expiresAt` is set to a past timestamp before `checkTimeouts(transaction, false)` is called
- **THEN** `getLockedObjectByPath` returns `null` for that path after the check

### Requirement: Lookup by lock ID
The system SHALL return the `LockedObject` by its ID for both permanent and temporary locks.

#### Scenario: getLockedObjectByID returns correct object
- **WHEN** a permanent lock is created and its ID is retrieved
- **THEN** `getLockedObjectByID` with that ID returns the same `LockedObject`

#### Scenario: getTempLockedObjectByID returns correct object
- **WHEN** a temporary lock is created and its ID is retrieved
- **THEN** `getTempLockedObjectByID` with that ID returns the same `LockedObject`

### Requirement: Lock cleanup removes ownerless lock trees
The system SHALL remove locked objects without owners (and whose children have no owners) from the lock tree when cleanup runs after unlock. Note: the real and temporary roots are currently swapped (see findings.md #4); the scenarios below pin current behavior.

#### Scenario: Nested temporary locks cleaned
- **WHEN** temporary locks on `/a/b/c` are released via `unlockTemporaryLockedObjects` and cleanup runs
- **THEN** `/a` is removed but `/a/b` and `/a/b/c` remain in the temporary lock table (current behavior)

#### Scenario: Owned child prevents parent cleanup
- **WHEN** `/a/b` holds a lock with an owner and the lock on `/a/b/c` is released
- **THEN** `/a/b` remains locked and only `/a/b/c` is removed

#### Scenario: Sibling cleanup with array shifting
- **WHEN** a parent has several ownerless children and cleanup runs
- **THEN** all ownerless children are removed (none skipped due to array shifting)

### Requirement: Unlock edge cases
The system SHALL handle unlock requests for unknown IDs and wrong owners without modifying state.

#### Scenario: Unlock unknown ID
- **WHEN** `unlock()` is called with an ID not present
- **THEN** it returns `true`, no exception is thrown and existing locks are unaffected

#### Scenario: Unlock by non-owner
- **WHEN** `unlock()` is called with an owner that does not hold the lock
- **THEN** the lock remains in place

