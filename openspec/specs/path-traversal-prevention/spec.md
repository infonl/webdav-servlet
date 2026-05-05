## ADDED Requirements

### Requirement: Reject path-traversal sequences in request path
`AbstractMethod.getRelativePath()` SHALL reject any path that contains a `..` path segment (a segment that equals the string `".."` after splitting on `/`) by throwing a `PathTraversalException`. The calling method executor SHALL respond with HTTP 400 Bad Request.

#### Scenario: Source path contains parent-directory traversal
- **WHEN** a WebDAV request arrives with a `pathInfo` value such as `/safe/../../../etc/passwd`
- **THEN** `getRelativePath()` throws `PathTraversalException` and the servlet responds with HTTP 400

#### Scenario: Source path with encoded traversal after URL decoding
- **WHEN** a WebDAV request arrives with a path that resolves to a `..` segment after the container's URL decoding
- **THEN** `getRelativePath()` throws `PathTraversalException` and the servlet responds with HTTP 400

#### Scenario: Normal path passes validation
- **WHEN** a WebDAV request arrives with a path such as `/files/documents/report.pdf`
- **THEN** `getRelativePath()` returns the path unchanged and processing continues normally

### Requirement: Reject path-traversal sequences in Destination header
`DoCopy.parseDestinationHeader()` SHALL reject any `Destination` header value whose path component, after URL-decoding and context-path stripping, contains a `..` segment or causes `normalize()` to return `null`. The method SHALL respond with HTTP 400 Bad Request and return `null`.

#### Scenario: Destination header path escapes root via traversal
- **WHEN** a COPY or MOVE request includes a `Destination` header whose path normalises to `null` (root escape detected by `normalize()`)
- **THEN** the server responds with HTTP 400 and the operation is not performed

#### Scenario: Destination header path contains traversal segment
- **WHEN** a COPY or MOVE request includes a `Destination` header whose decoded path contains a `..` segment after context-path stripping
- **THEN** the server responds with HTTP 400 and the operation is not performed

#### Scenario: Valid Destination header path is accepted
- **WHEN** a COPY or MOVE request includes a well-formed `Destination` header such as `http://example.com/webdav/target/file.txt`
- **THEN** `parseDestinationHeader()` returns the normalised destination path and processing continues

### Requirement: PathTraversalException is a WebdavException
The `PathTraversalException` class SHALL extend `WebdavException` so that existing `catch (WebdavException e)` handlers in method executors automatically catch traversal rejections without requiring new catch blocks.

#### Scenario: Exception caught by existing WebdavException handler
- **WHEN** `assertSafePath()` throws `PathTraversalException` inside a method executor
- **THEN** the surrounding `catch (WebdavException e)` block responds with HTTP 500 unless the handler maps to a more specific status; the traversal attempt does not reach any `IWebdavStore` method

### Requirement: Traversal check covers all method executors
Every method executor that calls `getRelativePath()` (DoGet, DoPut, DoDelete, DoMove, DoCopy, DoPropfind, DoLock, DoHead, DoOptions, DoNotImplemented) SHALL be protected by the traversal check without per-executor changes.

#### Scenario: Traversal attempt on any HTTP method
- **WHEN** a WebDAV request with a traversal path is issued using any supported HTTP method (GET, PUT, DELETE, MOVE, COPY, PROPFIND, LOCK, HEAD)
- **THEN** the server responds with HTTP 400 and no store method is called
