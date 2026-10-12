## ADDED Requirements

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
