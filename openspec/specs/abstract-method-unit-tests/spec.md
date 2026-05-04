## Requirements

### Requirement: getParentPath extracts parent from multi-segment path
The system SHALL return the path up to (but not including) the last slash.

#### Scenario: Standard nested path
- **WHEN** `getParentPath("/foo/bar/baz")` is called
- **THEN** returns `"/foo/bar"`

#### Scenario: Single-segment path returns empty string
- **WHEN** `getParentPath("/foo")` is called
- **THEN** returns `""`

#### Scenario: Path without slash returns null
- **WHEN** `getParentPath("foo")` is called
- **THEN** returns `null`

### Requirement: getCleanPath strips trailing slash
The system SHALL remove a trailing `/` from any path longer than one character.

#### Scenario: Trailing slash removed
- **WHEN** `getCleanPath("/foo/bar/")` is called
- **THEN** returns `"/foo/bar"`

#### Scenario: Root path unchanged
- **WHEN** `getCleanPath("/")` is called
- **THEN** returns `"/"`

### Requirement: getDepth parses the Depth header
The system SHALL return `0` for `"0"`, `1` for `"1"`, and `3` (INFINITY) for any other or absent value.

#### Scenario: Depth header "0"
- **WHEN** request has header `Depth: 0`
- **THEN** `getDepth(request)` returns `0`

#### Scenario: Depth header "1"
- **WHEN** request has header `Depth: 1`
- **THEN** `getDepth(request)` returns `1`

#### Scenario: Depth header "infinity"
- **WHEN** request has header `Depth: infinity`
- **THEN** `getDepth(request)` returns `3` (INFINITY)

#### Scenario: No Depth header
- **WHEN** request has no `Depth` header
- **THEN** `getDepth(request)` returns `3` (INFINITY)

### Requirement: getETag produces weak ETag for resource
The system SHALL produce a weak ETag of the form `W/"<length>-<lastModifiedMillis>"` for a resource `StoredObject`.

#### Scenario: ETag from valid resource
- **WHEN** `getETag(storedObject)` is called with a non-null resource `StoredObject` with known length and modification time
- **THEN** returns a string matching `W/"<length>-<millis>"`

#### Scenario: ETag from null or folder
- **WHEN** `getETag(null)` or `getETag(folderStoredObject)` is called
- **THEN** returns `W/"-"`  (empty length and empty lastModified)

### Requirement: getLockIdFromIfHeader parses single lock token
The system SHALL extract the lock ID from a single-token `If` header.

#### Scenario: Single locktoken in If header
- **WHEN** request has `If: (<urn:uuid:abc-123>)` or `If: (<locktoken:abc-123>)`
- **THEN** `getLockIdFromIfHeader` returns an array with `ids[0]` containing `"abc-123"` and `ids[1]` as `null`

#### Scenario: No If header returns null
- **WHEN** request has no `If` header
- **THEN** `getLockIdFromIfHeader` returns `null`

### Requirement: getLockIdFromLockTokenHeader parses Lock-Token header
The system SHALL extract the token value from the `Lock-Token` header.

#### Scenario: Lock-Token header parsed
- **WHEN** request has `Lock-Token: <opaquelocktoken:abc-123>`
- **THEN** `getLockIdFromLockTokenHeader` returns `"abc-123"`

### Requirement: checkLocks permits access when no lock exists
The system SHALL return `true` when no `LockedObject` is registered at the given path.

#### Scenario: No lock at path
- **WHEN** `getLockedObjectByPath` returns `null` for the path
- **THEN** `checkLocks` returns `true`

### Requirement: checkLocks permits access for shared lock without If header match needed
The system SHALL return `true` when the lock at the path is shared, regardless of the If header.

#### Scenario: Shared lock always permits
- **WHEN** a shared `LockedObject` exists at the path
- **THEN** `checkLocks` returns `true`

### Requirement: checkLocks rejects access when exclusive lock token mismatches
The system SHALL return `false` when an exclusive lock exists and the If header's lock token does not match.

#### Scenario: Exclusive lock with wrong token
- **WHEN** an exclusive `LockedObject` exists at the path and the If header contains a different lock ID
- **THEN** `checkLocks` returns `false`
