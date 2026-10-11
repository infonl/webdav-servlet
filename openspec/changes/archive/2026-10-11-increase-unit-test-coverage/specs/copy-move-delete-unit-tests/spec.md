## ADDED Requirements

### Requirement: Destination header parsing
The test suite SHALL verify `DoCopy.parseDestinationHeader` and `normalize`.

#### Scenario: Missing Destination header
- **WHEN** COPY or MOVE has no `Destination` header
- **THEN** `400 Bad Request` is sent

#### Scenario: Absolute URL destination
- **WHEN** `Destination` is `http://host:8080/context/servlet/target`
- **THEN** the scheme, host, context path and servlet path are stripped and `/target` is used

#### Scenario: URL-encoded destination
- **WHEN** `Destination` contains percent-encoded characters
- **THEN** the decoded path is used

#### Scenario: Dot segments rejected
- **WHEN** `Destination` contains `.` or `..` segments
- **THEN** `400 Bad Request` is sent by the path traversal check before normalization

#### Scenario: normalize resolves dot segments
- **WHEN** `normalize` is called directly with `.`/`..` segments and backslashes
- **THEN** segments are resolved, `\` becomes `/`, paths escaping the root return `null`, and a trailing `/..` is left unresolved (see findings.md #1)

#### Scenario: Destination equals source
- **WHEN** the normalized destination equals the source path
- **THEN** `403 Forbidden` is sent

### Requirement: COPY overwrite semantics
The test suite SHALL verify `Overwrite` header handling.

#### Scenario: Overwrite F with existing destination
- **WHEN** `Overwrite: F` and the destination exists
- **THEN** `412 Precondition Failed` is sent and the destination is untouched

#### Scenario: Overwrite T with existing destination
- **WHEN** `Overwrite: T` (or absent) and the destination exists
- **THEN** the destination is deleted, the copy is made and `204 No Content` is returned

#### Scenario: Copy to new destination
- **WHEN** the destination does not exist
- **THEN** `201 Created` is returned

### Requirement: COPY folder recursion
The test suite SHALL verify folder copy with depth and per-child error collection.

#### Scenario: Depth 0 folder copy
- **WHEN** a folder is copied with `Depth: 0`
- **THEN** only the folder is created, not its children

#### Scenario: Child copy failure
- **WHEN** copying two or more children throws `AccessDeniedException`, `ObjectNotFoundException`, `ObjectAlreadyExistsException` or `WebdavException`
- **THEN** the child paths are reported in a 207 multistatus with `403`, `404`, `409` or `500` respectively

#### Scenario: Single child copy failure
- **WHEN** exactly one child fails
- **THEN** its status is sent as a plain error, not a multistatus (see findings.md #19)

### Requirement: COPY and MOVE error mapping
The test suite SHALL verify exception-to-status mapping in `DoCopy.execute` and `DoMove.execute`.

#### Scenario: Read-only servlet
- **WHEN** COPY or MOVE runs with `readOnly=true`
- **THEN** `403 Forbidden` is sent

#### Scenario: Source not found
- **WHEN** the source does not exist
- **THEN** `404 Not Found` is sent

#### Scenario: Source or destination locked
- **WHEN** source (MOVE) or destination (COPY/MOVE) is locked without a matching token
- **THEN** `423 Locked` is returned

#### Scenario: Store failure during move
- **WHEN** the store throws `AccessDeniedException`, `ObjectAlreadyExistsException` or `WebdavException` during MOVE
- **THEN** `403`, `404` or `500` is sent respectively

### Requirement: DELETE error branches
The test suite SHALL verify `DoDelete` error and recursion branches.

#### Scenario: Read-only servlet
- **WHEN** DELETE runs with `readOnly=true`
- **THEN** `403 Forbidden` is sent

#### Scenario: Locked resource
- **WHEN** the target is locked without a matching token
- **THEN** `423 Locked` is returned

#### Scenario: Child delete failure
- **WHEN** deleting two or more children of a folder throws `AccessDeniedException` or `WebdavException`
- **THEN** the child paths are reported in a 207 multistatus with `403` or `500`, and the folder itself is still removed (see findings.md #21)

#### Scenario: Store failure on root delete
- **WHEN** `removeObject` on the target throws `AccessDeniedException`, `ObjectAlreadyExistsException` or `WebdavException`
- **THEN** `403`, `404` or `500` is sent respectively
