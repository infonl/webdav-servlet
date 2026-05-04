## Requirements

### Requirement: Owner added to unowned lock
The system SHALL set a single-element owner array when the first owner is added to a lock with no existing owner.

#### Scenario: First owner added
- **WHEN** `addLockedObjectOwner("alice")` is called on a `LockedObject` with `_owner == null`
- **THEN** the method returns `true` and `getOwner()` returns `["alice"]`

### Requirement: Second owner added to shared lock
The system SHALL append a second owner when the lock already has one owner.

#### Scenario: Second owner added
- **WHEN** `addLockedObjectOwner("alice")` and then `addLockedObjectOwner("bob")` are called
- **THEN** both calls return `true` and `getOwner()` contains both "alice" and "bob"

### Requirement: Duplicate owner rejected
The system SHALL not add an owner that already appears in the owner list.

#### Scenario: Duplicate owner rejected
- **WHEN** `addLockedObjectOwner("alice")` is called twice on the same `LockedObject`
- **THEN** the second call returns `false` and `getOwner()` still contains only one entry

### Requirement: Owner removed from lock
The system SHALL remove the specified owner and shrink the owner array, setting it to null when empty.

#### Scenario: Single owner removed clears array
- **WHEN** `addLockedObjectOwner("alice")` then `removeLockedObjectOwner("alice")` are called
- **THEN** `getOwner()` returns `null`

#### Scenario: One of two owners removed
- **WHEN** both "alice" and "bob" are owners and `removeLockedObjectOwner("alice")` is called
- **THEN** `getOwner()` contains only "bob"

### Requirement: Child lock added
The system SHALL grow the children array when `addChild` is called.

#### Scenario: Child added to childless object
- **WHEN** `addChild(child)` is called on a `LockedObject` with `_children == null`
- **THEN** `_children` is a one-element array containing the child

### Requirement: checkLocks permits non-conflicting lock
The system SHALL return `true` from `checkLocks` when no conflicting lock exists in the hierarchy.

#### Scenario: Shared lock check on unowned path
- **WHEN** `checkLocks(false, 0)` is called on a `LockedObject` with no owner and no children
- **THEN** returns `true`

### Requirement: checkLocks blocks exclusive-on-exclusive conflict
The system SHALL return `false` from `checkLocks` when the target path already holds an exclusive lock.

#### Scenario: Exclusive blocked by existing exclusive owner
- **WHEN** a `LockedObject` already has an exclusive owner and `checkLocks(true, 0)` is called
- **THEN** returns `false`

### Requirement: hasExpired reports correct expiry state
The system SHALL return `true` from `hasExpired()` when the expiry time has passed, and `false` when it has not.

#### Scenario: Lock not yet expired
- **WHEN** `refreshTimeout(3600)` is called and `hasExpired()` is immediately checked
- **THEN** returns `false`

#### Scenario: Lock already expired
- **WHEN** `_expiresAt` is set to `1` (1 ms after epoch) and `hasExpired()` is checked
- **THEN** returns `true`

### Requirement: removeLockedObject cleans up from parent and hashtables
The system SHALL remove a `LockedObject` from its parent's children array and from `ResourceLocks._locks` and `_locksByID`.

#### Scenario: Remove non-root locked object
- **WHEN** a child `LockedObject` is created for a non-root path and `removeLockedObject()` is called
- **THEN** the object is absent from `ResourceLocks._locks` and `ResourceLocks._locksByID`
