# Findings: candidate bugs found by characterization tests

Coverage: 65.1% → **87.1% line**, 52.4% → **81.2% branch** (452 tests, 0 failures).

Tests assert the current behavior. Each item below is a candidate for a follow-up change. When it is fixed, the pinned test must be updated too. Paths are relative to `src/main/java/nl/info/webdav/`.

## Security

1. **Path traversal check bypassed with backslashes in `Destination`.** `DoCopy.parseDestinationHeader` runs `assertSafePath` (which splits on `/` only) *before* `normalize`. `normalize` then converts `\` to `/` and leaves a trailing `/..` unresolved (`methods/DoCopy.java:400,452-482`). Example: `Destination: /a%5C..%5C..` passes the check and becomes `/..`. `LocalFileSystemStore.resolveFile` still blocks it with a canonical root check, but custom `IWebdavStore` implementations receive the unsafe path. Pinned at the `normalize()` level only (`DoCopyTest.testNormalizeDoesNotResolveTrailingParentSegment`).

## Data loss / correctness

2. **MOVE with `Depth: 0` deletes uncopied children.** `copyFolder` respects Depth 0, but `DoDelete.deleteFolder` removes the whole source tree (`methods/DoDelete.java:161-181`).
3. **PROPPATCH stores nothing.** Every set/remove property is reported as 200 OK, but nothing is persisted, and `IWebdavStore` has no custom-property API (`methods/DoProppatch.java:80-81`).
4. **`ResourceLocks` roots are swapped.** `_root` is registered as a temporary lock and `_tempRoot` as a real one (`locking/ResourceLocks.java:60-61`). As a result, cleanup works on the wrong tree, nested temporary locks are not fully cleaned, and stale entries remain in `_locks`/`_tempLocks`.
5. **`DoLock` keeps per-request state in instance fields.** `_macLockRequest`, `_userAgent`, etc. live on the instance (`methods/DoLock.java:43-49`), there is one instance per servlet, and `_macLockRequest` is never reset. This is not thread-safe, and after the first Darwin request every request stays in Darwin mode.
6. **UNLOCK ignores the owner.** Any valid lock token removes the lock (`methods/DoUnlock.java:60-76`).
7. **UNLOCK NPE when the stored object is missing**, after the lock has already been removed (`methods/DoUnlock.java:79`).

## Crashes (unchecked exceptions instead of status codes)

8. `Timeout: Second-abc` → `NumberFormatException` (`methods/DoLock.java:428`).
9. LOCK owner given as plain text → NPE (`methods/DoLock.java:386`).
10. PROPPATCH with an `If` header on an unlocked resource → NPE (`methods/DoProppatch.java:110`).
11. PROPFIND on an object with null dates (e.g. lock-null) → NPE (`methods/DoPropfind.java:242-243`).
12. `XMLHelper.findSubElement` NPE on a DOM parsed without namespace awareness (`fromcatalina/XMLHelper.java:19`).
13. HEAD/GET: any `WebdavException` other than `AccessDeniedException` from the initial lookup propagates out of `execute` (`methods/DoHead.java:65-77`).

## Wrong or inconsistent status codes / output

14. Null-resource LOCK returns 200: `setStatus(201)` is overwritten by `generateXMLReport` (`methods/DoLock.java:183,458`).
15. Null-resource LOCK with a missing parent creates the parent instead of returning 409 (`methods/DoLock.java:168-169`).
16. Lock conflict errors are sent twice (423 + 423, and 500 + 400 for a malformed body) (`methods/DoLock.java:154,199,278,284,402`).
17. Darwin and refresh lock responses show `<D:null/>` locktype and `shared` scope; the refresh response has an empty owner (`methods/DoLock.java:468-476`).
18. `Timeout: Infinite` (and other non-numeric values) → `MAX_TIMEOUT` rather than the default.
19. Single-entry error reports are sent as plain `sendError`, not as a 207 multistatus (`methods/AbstractMethod.java:366-372`).
20. MOVE/DELETE map `ObjectNotFoundException` → 500 and `ObjectAlreadyExistsException` → 404; COPY maps the latter to 409.
21. A child-delete failure does not stop the parent folder from being removed (`methods/DoDelete.java:127-128`).
22. PUT with a missing parent (no lazy creation) → 404, not 409 (`methods/DoPut.java:79-82`).
23. PUT overwrite → 201 unless User-Agent contains "Transmit" (204) (`methods/DoPut.java:160-172`).
24. PUT on an existing folder → 201 and `setResourceContent` is called on the folder (`methods/DoPut.java:85-141`).
25. MKCOL never checks the request body (no 415).
26. GET/HEAD on a missing resource: `sendError(404)` then `setStatus(404)`; the `instead-of-404` fallback is served with status 404 (`methods/DoHead.java:172-173`).
27. `no-content-length-headers`: only the value `1` *enables* Content-Length; any other value suppresses it (`methods/DoHead.java:124`).
28. PROPFIND children inherit the parent's mime type (`methods/DoPropfind.java:146-148,206-208`).
29. PROPFIND `lockdiscovery` depth is taken from the request's Depth header, not from the lock (`methods/DoPropfind.java:561-566`).
30. PROPPATCH locked → `setStatus(423)`; PROPFIND uses `sendError` for the same case.
31. `WebDavServletBean.service`: on IOException the `ServletException` is wrapped twice and rollback runs twice (`WebDavServletBean.java:143-157`).
32. `RequestUtil.URLDecode(String, enc)` with an unknown encoding returns `null` (`fromcatalina/RequestUtil.java:294-304`).
33. Depth "infinity" is the number 3 (`methods/AbstractMethod.java:52`), so lock checks stop below 3 levels.

## Unreachable code

- "Neither resource nor folder" 404 branches: `methods/DoDelete.java:129-130`, `methods/DoCopy.java:261-262` (`isResource()` is `!isFolder`).
