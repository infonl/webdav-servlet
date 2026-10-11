## ADDED Requirements

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
