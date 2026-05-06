## Context

`LocalFileSystemStore` is the reference `IWebdavStore` implementation. Every store method receives a `String uri` sourced from `HttpServletRequest.getPathInfo()` (via `AbstractMethod.getRelativePath()`) or from the `Destination` header (via `DoCopy.parseDestinationHeader()`). Both values are user-controlled. The store concatenates them with `_root` using `new File(_root, uri)` without checking that the resolved canonical path is inside `_root`.

GitHub CodeQL alerts #6–#13 all point to this pattern in `LocalFileSystemStore.java`.

## Goals / Non-Goals

**Goals:**
- Eliminate all 8 `java/path-injection` alerts by validating every `File` constructed from a user-supplied URI.
- Fail closed: any path that escapes `_root` throws `WebdavException`, which the servlet maps to 403 Forbidden.
- Keep the fix local to `LocalFileSystemStore` so the `IWebdavStore` contract and all other classes are unchanged.

**Non-Goals:**
- Fixing path validation in custom `IWebdavStore` implementations (library users' responsibility).
- Changing how URIs are parsed or normalized upstream in `AbstractMethod` or `DoCopy`.

## Decisions

### Validate via canonical path comparison (chosen)

Extract a private helper `resolveFile(String uri)` in `LocalFileSystemStore`:

```java
private File resolveFile(String uri) throws WebdavException {
    try {
        File candidate = new File(_root, uri);
        String rootCanonical = _root.getCanonicalPath();
        String candidateCanonical = candidate.getCanonicalPath();
        if (!candidateCanonical.startsWith(rootCanonical + File.separator)
                && !candidateCanonical.equals(rootCanonical)) {
            throw new WebdavException("Path traversal attempt: " + uri);
        }
        return candidate;
    } catch (IOException e) {
        throw new WebdavException(e);
    }
}
```

Replace every `new File(_root, uri)` call with `resolveFile(uri)`. `getCanonicalPath()` resolves symlinks and `..` segments, so the check is reliable even for encoded or symlinked paths.

**Alternative considered — normalize the URI string first**: Stripping `..` via string manipulation before constructing the File is fragile (OS-specific separators, URL encoding). Canonical path resolution is the JDK-blessed approach.

**Alternative considered — validate in `AbstractMethod.getRelativePath()`**: Centralizing validation upstream would protect all store implementations, but it would require knowing the root path in the method layer (it doesn't), and it wouldn't catch the `Destination` header path. Keeping it in the store is simpler and self-contained.

### Cache `_root.getCanonicalPath()` at construction time

`getCanonicalPath()` does I/O. Calling it on every request is wasteful. Cache it as `_rootCanonical` in the constructor.

```java
private final String _rootCanonical;

public LocalFileSystemStore(File root) throws IOException {
    _root = root;
    _rootCanonical = root.getCanonicalPath();
}
```

**Trade-off**: Constructor now throws `IOException`. The existing call sites in tests pass a temp directory whose canonical path is always resolvable, so this is low risk. If needed, callers can wrap in a `WebdavException`.

## Risks / Trade-offs

- **Symlinks inside root**: If `_root` itself is a symlink, `getCanonicalPath()` resolves it, which is correct — the check uses the resolved root, not the symlink. Resources inside root that are themselves symlinks pointing outside root will be **rejected**. This is the safest default; symlinks that escape the root would be traversal vectors.
- **Windows path separator**: `File.separator` is used in the check, so it handles both Unix `/` and Windows `\` correctly.
- **Constructor signature change**: Callers that `new LocalFileSystemStore(root)` will need to handle `IOException`. In practice the library's servlet (`WebdavServlet`) instantiates stores reflectively and already wraps in a broad exception handler.

## Migration Plan

1. Update `LocalFileSystemStore` constructor and all `new File(_root, uri)` call sites.
2. Run existing tests — no test changes expected for happy paths.
3. Add traversal-attack tests to `LocalFileSystemStoreTest` (or new test class).
4. Run `spotlessApply` and `./gradlew build`.
5. No deployment steps — this is a library; consumers get the fix on next version bump.
