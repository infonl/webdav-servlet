## ADDED Requirements

### Requirement: LOCK timeout header parsing
The test suite SHALL verify `Timeout` header parsing in `DoLock`.

#### Scenario: Missing Timeout header
- **WHEN** a LOCK request has no `Timeout` header
- **THEN** the lock is created with the default timeout

#### Scenario: Second-N timeout
- **WHEN** the header is `Second-600`
- **THEN** the lock timeout is 600 seconds

#### Scenario: Timeout above maximum
- **WHEN** the header is `Second-<value greater than MAX_TIMEOUT>`
- **THEN** the lock timeout is capped at `MAX_TIMEOUT`

#### Scenario: Infinite or non-numeric timeout
- **WHEN** the header is `Infinite` or another value not starting with `Second-`
- **THEN** the lock timeout is `MAX_TIMEOUT` (current behavior; see findings.md #18)

#### Scenario: Malformed Second- timeout
- **WHEN** the header is `Second-abc`
- **THEN** a `NumberFormatException` propagates and no lock is created (current behavior; see findings.md #8)

### Requirement: LOCK on non-existing resource creates null resource
The test suite SHALL verify `doNullResourceLock` branches.

#### Scenario: Null-resource lock created
- **WHEN** LOCK targets a non-existing path whose parent exists
- **THEN** an empty resource is created, the lock is granted and a lock XML body is returned with final status `200` (`201` is overwritten; see findings.md #14)

#### Scenario: Parent missing
- **WHEN** LOCK targets a non-existing path whose parent does not exist
- **THEN** the parent folder is created and the lock is granted (current behavior; see findings.md #15)

#### Scenario: Transmit client workaround
- **WHEN** the `User-Agent` contains `Transmit`
- **THEN** the null-resource lock is answered with `204 No Content` instead of `201`

### Requirement: LOCK refresh
The test suite SHALL verify refreshing an existing lock.

#### Scenario: Refresh with valid If header
- **WHEN** LOCK has no body and an `If` header containing the token of an existing lock
- **THEN** the lock timeout is refreshed and lock XML is returned

#### Scenario: Refresh with unknown token
- **WHEN** the `If` header references a token that does not exist
- **THEN** `412 Precondition Failed` is returned

### Requirement: LOCK Mac Finder workaround and failure reporting
The test suite SHALL verify the Darwin user-agent path and lock failure reporting.

#### Scenario: Darwin client lock
- **WHEN** the `User-Agent` contains `Darwin` and the resource is unlocked
- **THEN** an exclusive lock is granted with an owner derived from the user agent

#### Scenario: Darwin client lock conflict
- **WHEN** the `User-Agent` contains `Darwin` and the resource is already exclusively locked
- **THEN** `423 Locked` is sent (as a plain error, since the report has one entry; see findings.md #19)

#### Scenario: Read-only servlet
- **WHEN** LOCK is executed with `readOnly=true`
- **THEN** `403 Forbidden` is sent

### Requirement: LOCK request body parsing
The test suite SHALL verify `getLockInformation` branches.

#### Scenario: Shared lock scope
- **WHEN** the body specifies `<lockscope><shared/></lockscope>`
- **THEN** a shared lock is granted

#### Scenario: Owner given as href
- **WHEN** the owner element contains `<href>`
- **THEN** the href text is used as lock owner

#### Scenario: Missing lock type
- **WHEN** the body lacks a `<locktype>` element
- **THEN** the request is rejected with `400 Bad Request`

#### Scenario: Malformed body
- **WHEN** the body is not well-formed XML
- **THEN** the request is rejected and no lock is created

### Requirement: UNLOCK error branches
The test suite SHALL verify `DoUnlock` error branches.

#### Scenario: Missing Lock-Token header
- **WHEN** UNLOCK has no `Lock-Token` header
- **THEN** `400 Bad Request` is sent

#### Scenario: Unknown lock token
- **WHEN** the `Lock-Token` does not match any lock
- **THEN** `400 Bad Request` is sent

#### Scenario: Token from another owner
- **WHEN** the `Lock-Token` matches a lock owned by a different owner
- **THEN** the lock is removed and `204 No Content` is returned (owner is not checked; see findings.md #6)

#### Scenario: Unlock of null resource
- **WHEN** the last lock on a null resource (zero-length, created by LOCK) is removed
- **THEN** the null resource is removed from the store
