## Context

CodeQL issue #19 flags user-controlled data from `HttpServletRequest.getPathInfo()` and the `Destination` request header flowing into file-system path operations without a validated boundary check at the servlet method layer. The store-level guard (`LocalFileSystemStore.resolveFile` canonical-path check) protects the reference implementation, but the library contract (`IWebdavStore`) does not require custom implementations to perform the same check. CodeQL cannot see through the interface, so the warning fires at the call sites in each HTTP method handler.

The fix must close the vulnerability at the earliest safe point — the HTTP layer — so every store implementation is protected automatically.

## Goals / Non-Goals

**Goals:**
- Reject paths containing `..` segments before they reach any `IWebdavStore` method.
- Return HTTP 400 for traversal-bearing source paths; return HTTP 400 for traversal-bearing `Destination` header values.
- Cover all method handlers that call `getRelativePath()` (DoGet, DoPut, DoDelete, DoMove, DoCopy, DoPropfind, DoLock, DoHead, DoOptions, DoNotImplemented).
- Keep the fix minimal and in one place (single method in `AbstractMethod`).

**Non-Goals:**
- Removing or weakening the existing `LocalFileSystemStore.resolveFile` check (defence-in-depth, keep it).
- Validating characters beyond path-traversal sequences (e.g., null bytes, encoding attacks — those are separate concerns).
- Changing any other request-header handling.

## Decisions

### Decision 1: validate in `AbstractMethod.getRelativePath`, not in each handler

All method handlers call `getRelativePath(request)` exactly once to obtain the WebDAV path; centralising the check here means every handler gets protection for free with no per-handler changes.

*Alternative considered*: validate inside each handler class. Rejected — error-prone, requires touching ~10 classes, and future handlers could forget the check.

### Decision 2: reject with HTTP 400, not silently sanitise

Stripping `..` segments silently could mask misconfigured clients and would change the semantics of paths. Returning 400 is unambiguous and safe.

### Decision 3: detect traversal by segment inspection, not string-contains

Checking for the literal string `..` in the path would produce false positives on filenames like `readme..txt`. Instead, split on `/` and test whether any segment equals `..` or `.`.

The `normalize()` method in `DoCopy` already resolves `/./` and `/../` — for the `Destination` header path, we keep that normalisation and additionally check whether `normalize()` returns `null` (root escape detected by the existing algorithm) and treat that as a 400.

### Decision 4: one helper method `assertSafePath(String path)` in `AbstractMethod`

Returns `void`, throws a new lightweight `PathTraversalException extends WebdavException` (or alternatively, a `SecurityException`) if the path is unsafe. All call sites already have `WebdavException` in their `catch` chain, so this integrates cleanly.

*Alternative*: boolean return. Rejected — callers would need to check the return value; an exception is harder to silently ignore.

## Risks / Trade-offs

- **Risk**: Legitimate paths that contain `..` as a filename component are rejected.
  → Mitigation: WebDAV paths passed via `getPathInfo()` are URL-decoded and normalised by the servlet container; a literal filename `foo..bar` contains `..` but not as a standalone segment. The segment-equality check (`segment.equals("..")`) avoids this.

- **Risk**: `parseDestinationHeader` in `DoCopy` returns `null` on error today; callers already handle `null`. Adding a `PathTraversalException` path requires callers to also catch it.
  → Mitigation: `DoCopy.copyResource` already catches `WebdavException`; `PathTraversalException extends WebdavException`, so no new catch blocks are needed. We add the validation inside `assertSafePath` called after `normalize()`.

## Migration Plan

No data migration needed. This is a pure code change; no configuration or API surface changes. The change is backwards-compatible for compliant WebDAV clients (they never send `..` segments). Non-compliant clients that relied on traversal paths will receive 400 instead of potentially accessing unintended resources.
