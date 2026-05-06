## ADDED Requirements

### Requirement: Store operations confined to root directory
`LocalFileSystemStore` SHALL reject any URI that resolves to a path outside the configured root directory before performing any file system operation (read, write, delete, stat, list).

#### Scenario: Normal request within root
- **WHEN** a WebDAV method supplies a URI such as `/docs/file.txt` that resolves inside `_root`
- **THEN** the store proceeds normally and returns the expected result

#### Scenario: Path traversal via dot-dot segments
- **WHEN** a URI contains `..` segments (e.g., `/../etc/passwd`) that would resolve outside `_root`
- **THEN** the store throws `WebdavException` and no file system access occurs outside `_root`

#### Scenario: Path traversal via encoded sequences
- **WHEN** a URI contains encoded traversal sequences (e.g., `%2F..%2F`) that after decoding resolve outside `_root`
- **THEN** the store throws `WebdavException` and no file system access occurs outside `_root`

#### Scenario: Root URI itself
- **WHEN** a URI resolves exactly to `_root` (i.e., `/`)
- **THEN** the store accepts the path and operates on the root directory normally

### Requirement: Traversal detection uses canonical path resolution
The path confinement check SHALL use `File.getCanonicalPath()` to resolve symlinks and OS-specific path normalizations before comparing against the canonical root path.

#### Scenario: Symlink inside root pointing outside
- **WHEN** a URI resolves via symlink to a path outside `_root`
- **THEN** the store throws `WebdavException`, refusing access to the symlink target

#### Scenario: Root canonical path cached at construction
- **WHEN** `LocalFileSystemStore` is constructed
- **THEN** the canonical path of the root directory is resolved once and reused for all subsequent checks, avoiding redundant I/O per request
