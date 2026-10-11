# servlet-bootstrap-unit-tests Specification

## Purpose
Unit tests for `WebdavServlet` initialization and store construction, and `WebDavServletBean` lifecycle and exception-to-status mapping.
## Requirements
### Requirement: WebdavServlet defaults to LocalFileSystemStore
The test suite SHALL verify that `WebdavServlet.init()` instantiates `LocalFileSystemStore` when the `ResourceHandlerImplementation` init parameter is absent or empty.

#### Scenario: Missing store class parameter
- **WHEN** `init()` is called with `rootpath` set to a temp directory and no `ResourceHandlerImplementation` parameter
- **THEN** the servlet initializes without error and serves requests through a `LocalFileSystemStore`

#### Scenario: Empty store class parameter
- **WHEN** `init()` is called with `ResourceHandlerImplementation` set to `""`
- **THEN** the servlet falls back to `LocalFileSystemStore`

### Requirement: WebdavServlet constructs configured store reflectively
The test suite SHALL verify that `constructStore` instantiates the named class via its `File` constructor and wraps failures.

#### Scenario: Valid store class
- **WHEN** `constructStore` is called with the name of an `IWebdavStore` implementation that has a public `(File)` constructor
- **THEN** an instance of that class is returned

#### Scenario: Unknown store class
- **WHEN** `constructStore` is called with a class name that does not exist
- **THEN** a `RuntimeException` with message `"Failed to construct store"` is thrown

#### Scenario: Store class without File constructor
- **WHEN** `constructStore` is called with a class lacking a `(File)` constructor
- **THEN** a `RuntimeException` with message `"Failed to construct store"` is thrown

### Requirement: WebdavServlet resolves root path
The test suite SHALL verify root-path resolution from the `rootpath` init parameter.

#### Scenario: Missing rootpath
- **WHEN** `init()` is called without a `rootpath` parameter
- **THEN** a `WebdavException` with message `"missing parameter: rootpath"` is thrown

#### Scenario: WAR-file root outside a WEB-INF layout
- **WHEN** `init()` is called with `rootpath` = `*WAR-FILE-ROOT*` and the code source location contains no `/WEB-INF/`
- **THEN** a `WebdavException` starting with `"Could not determine root of war file"` is thrown

### Requirement: WebdavServlet parses optional init parameters
The test suite SHALL verify handling of `lazyFolderCreationOnPut`, `default-index-file`, `instead-of-404` and `no-content-length-headers`.

#### Scenario: Optional parameters set
- **WHEN** `init()` is called with `lazyFolderCreationOnPut=1` and `no-content-length-headers=0`
- **THEN** initialization succeeds and the values are passed to the method handlers

#### Scenario: Non-numeric content-length parameter
- **WHEN** `init()` is called with `no-content-length-headers=abc`
- **THEN** a `NumberFormatException` is thrown

### Requirement: WebDavServletBean destroy delegates to store
The test suite SHALL verify that `destroy()` calls `IWebdavStore.destroy()` when a store is set and is safe when none is set.

#### Scenario: Destroy with store
- **WHEN** `destroy()` is called on an initialized bean
- **THEN** `store.destroy()` is invoked exactly once

#### Scenario: Destroy without store
- **WHEN** `destroy()` is called on a bean that was never initialized
- **THEN** no exception is thrown

### Requirement: WebDavServletBean maps exceptions to status codes
The test suite SHALL verify that `service()` rolls back the transaction and maps exceptions to HTTP status codes.

#### Scenario: Unknown HTTP method
- **WHEN** `service()` receives a method name not in the method map
- **THEN** the `DoNotImplemented` handler is used and `501 Not Implemented` is sent

#### Scenario: Unauthenticated
- **WHEN** `store.checkAuthentication` throws `UnauthenticatedException`
- **THEN** `403 Forbidden` is sent and the transaction is rolled back

#### Scenario: Path traversal
- **WHEN** a handler throws `PathTraversalException`
- **THEN** `400 Bad Request` is sent and the transaction is rolled back

#### Scenario: IOException from handler
- **WHEN** a handler throws `IOException`
- **THEN** `500 Internal Server Error` is sent and the transaction is rolled back

#### Scenario: Unexpected runtime exception
- **WHEN** a handler throws an unchecked exception
- **THEN** the transaction is rolled back and the exception is wrapped in a `ServletException`

