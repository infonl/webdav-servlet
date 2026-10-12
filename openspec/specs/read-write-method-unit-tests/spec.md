# read-write-method-unit-tests Specification

## Purpose
Characterization tests for GET, HEAD, PUT, MKCOL and OPTIONS status-code branches.
## Requirements
### Requirement: GET/HEAD status branches
The test suite SHALL verify the remaining `DoHead.execute` and `DoGet` branches.

#### Scenario: Default index file for folder
- **WHEN** GET targets a folder and `default-index-file` is configured
- **THEN** the response redirects to / serves the configured index file

#### Scenario: instead-of-404 fallback
- **WHEN** GET targets a missing resource and `instead-of-404` is configured to an existing resource
- **THEN** the fallback resource is served, with status `404` (see findings.md #26)

#### Scenario: Missing resource without fallback
- **WHEN** GET targets a missing resource and no fallback is configured
- **THEN** `404 Not Found` is sent

#### Scenario: If-None-Match matches ETag
- **WHEN** the request `If-None-Match` equals the resource ETag
- **THEN** `304 Not Modified` is returned without a body

#### Scenario: Content length header switch
- **WHEN** `no-content-length-headers` is `1`
- **THEN** `Content-Length` is written; for any other value it is suppressed (inverted semantics; see findings.md #27)

#### Scenario: Access denied
- **WHEN** the store throws `AccessDeniedException`
- **THEN** `403 Forbidden` is sent

#### Scenario: HEAD on folder
- **WHEN** HEAD targets a folder without a default index file
- **THEN** no status, entity headers or body are written; only `If-None-Match` is read

#### Scenario: Directory listing CSS
- **WHEN** GET renders a folder listing
- **THEN** the default CSS is embedded in the HTML output

### Requirement: PUT status branches
The test suite SHALL verify the remaining `DoPut.execute` branches.

#### Scenario: Read-only servlet
- **WHEN** PUT runs with `readOnly=true`
- **THEN** `403 Forbidden` is sent

#### Scenario: Parent missing without lazy creation
- **WHEN** the parent folder does not exist and `lazyFolderCreationOnPut` is false
- **THEN** `404 Not Found` is sent (see findings.md #22)

#### Scenario: Parent missing with lazy creation
- **WHEN** the parent folder does not exist and `lazyFolderCreationOnPut` is true
- **THEN** the parent folder is created and the resource is stored with `201 Created`

#### Scenario: Locked resource
- **WHEN** the target is locked without a matching token
- **THEN** `423 Locked` is returned

#### Scenario: Overwrite existing resource
- **WHEN** PUT targets an existing resource
- **THEN** content is replaced and `201 Created` is returned, or `204 No Content` when the User-Agent contains `Transmit` (see findings.md #23)

#### Scenario: PUT to a folder path
- **WHEN** PUT targets an existing folder
- **THEN** `setResourceContent` is called on the folder path and `201 Created` is returned (see findings.md #24)

### Requirement: MKCOL status branches
The test suite SHALL verify the remaining `DoMkcol.execute` branches.

#### Scenario: Parent missing
- **WHEN** the parent folder does not exist
- **THEN** `409 Conflict` is sent

#### Scenario: Resource exists
- **WHEN** the target already exists
- **THEN** `405 Method Not Allowed` is sent with an `Allow` header

#### Scenario: Null resource replaced
- **WHEN** the target is a locked null resource owned by the requester
- **THEN** the null resource is replaced with a folder and `201 Created` is returned

#### Scenario: Store failure
- **WHEN** `createFolder` throws `AccessDeniedException` or `WebdavException`
- **THEN** `403` or `500` is sent respectively

### Requirement: OPTIONS error branch
The test suite SHALL verify the OPTIONS error branch.

#### Scenario: Store failure
- **WHEN** the store throws `AccessDeniedException` or `WebdavException` while resolving the resource
- **THEN** `403` or `500` is sent respectively

