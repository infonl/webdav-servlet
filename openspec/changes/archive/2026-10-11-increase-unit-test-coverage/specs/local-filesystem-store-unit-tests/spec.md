## ADDED Requirements

### Requirement: begin creates missing root
The test suite SHALL verify root handling in `LocalFileSystemStore.begin`.

#### Scenario: Root missing and creatable
- **WHEN** `begin()` is called and the root directory does not exist
- **THEN** the directory is created and no exception is thrown

#### Scenario: Root missing and not creatable
- **WHEN** `begin()` is called and the root cannot be created (e.g. a regular file occupies a parent path)
- **THEN** a `WebdavException` mentioning `"does not exist and could not be created"` is thrown

### Requirement: Resource I/O failures surface as WebdavException
The test suite SHALL verify failure branches in `createResource`, `setResourceContent` and `getResourceContent`.

#### Scenario: createResource fails
- **WHEN** `createResource` targets a path whose parent does not exist
- **THEN** a `WebdavException` is thrown

#### Scenario: setResourceContent fails
- **WHEN** `setResourceContent` cannot open the target file for writing
- **THEN** a `WebdavException` is thrown

#### Scenario: getResourceContent on missing file
- **WHEN** `getResourceContent` targets a non-existing file
- **THEN** a `WebdavException` is thrown
