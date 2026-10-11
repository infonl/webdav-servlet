## Purpose
Direct unit tests for `LockedObject`: owner and child management, lock-conflict checks (`checkLocks`, `checkChildren`) and expiry.
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

### Requirement: checkChildren detects conflicting child locks
The system SHALL report a conflict for a deep lock request when any descendant holds a conflicting lock.

#### Scenario: Exclusive child blocks deep lock
- **WHEN** `/a/b` holds an exclusive lock and `checkLocks(exclusive, depth=infinity)` is called on `/a`
- **THEN** returns `false`

#### Scenario: Shared child permits shared deep lock
- **WHEN** `/a/b` holds a shared lock and `checkLocks(shared, depth=infinity)` is called on `/a`
- **THEN** returns `true`

#### Scenario: Grandchild conflict found recursively
- **WHEN** only `/a/b/c` holds an exclusive lock and a deep exclusive lock is checked on `/a`
- **THEN** returns `false`

#### Scenario: Depth 0 ignores children
- **WHEN** a child holds an exclusive lock and `checkLocks` is called on the parent with depth 0
- **THEN** returns `true`

### Requirement: Removal handles single and multiple children
The system SHALL correctly shrink the parent's children array on removal for both normal and temporary locks.

#### Scenario: Remove only child
- **WHEN** the single child of a parent is removed via `removeLockedObject`
- **THEN** the parent's `_children` becomes `null`

#### Scenario: Remove middle child of several
- **WHEN** the middle of three children is removed via `removeTempLockedObject`
- **THEN** the remaining two children stay in the parent in order

### Requirement: Owner removal edge cases
The system SHALL handle removal of non-existing owners and the last owner.

#### Scenario: Remove owner not present
- **WHEN** `removeLockedObjectOwner` is called with an owner not in the list
- **THEN** the owner list is unchanged

#### Scenario: Remove last owner
- **WHEN** the only owner is removed
- **THEN** `_owner` becomes `null`

