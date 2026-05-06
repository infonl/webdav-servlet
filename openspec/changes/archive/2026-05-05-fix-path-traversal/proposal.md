## Why

`LocalFileSystemStore` constructs `File` objects directly from user-supplied URI paths without verifying the resolved path stays inside the store root, enabling path traversal attacks (CWE-22). GitHub code scanning flagged 8 high-severity `java/path-injection` alerts across all store methods.

## What Changes

- Add path validation in `LocalFileSystemStore` that canonicalizes each user-supplied URI and asserts the result is a descendant of `_root` before any file operation proceeds.
- Throw a `WebdavException` (mapped to 403 Forbidden) when traversal is detected, so attackers cannot read, write, delete, or stat files outside the WebDAV root.

## Capabilities

### New Capabilities

- `safe-path-resolution`: Centralised path validation in `LocalFileSystemStore` ensuring all file operations are confined to the configured root directory.

### Modified Capabilities

<!-- No existing spec-level requirements change; this is a new security control. -->

## Impact

- **Code**: `LocalFileSystemStore.java` — all methods that call `new File(_root, uri)` (lines 87, 95, 116, 155, 173, 188, 207, 209).
- **API**: No public API changes; `WebdavException` is already the declared exception type for store errors.
- **Tests**: New unit tests for `LocalFileSystemStore` covering traversal attempts and valid paths.
- **Dependencies**: None.
