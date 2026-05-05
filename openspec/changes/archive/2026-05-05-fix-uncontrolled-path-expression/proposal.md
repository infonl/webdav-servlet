## Why

CodeQL flags `getPathInfo()` and the `Destination` header value as user-controlled data flowing into path expressions without validation at the servlet layer. Although `LocalFileSystemStore.resolveFile` already enforces a canonical-path boundary check, CodeQL cannot see that the store is always called, and future store implementations might not include the same guard. Adding a normalisation and rejection step in `AbstractMethod.getRelativePath` and `DoCopy.parseDestinationHeader` closes the vulnerability at the HTTP boundary before any store method is invoked.

## What Changes

- Sanitise `getRelativePath()` in `AbstractMethod`: after obtaining `pathInfo`, reject paths containing `..` segments (respond 400) so no store method ever receives a traversal path.
- Sanitise the `Destination` header path in `DoCopy.parseDestinationHeader`: the existing `normalize()` call already resolves `/../` but returns `null` only for root-escaping sequences; add an explicit check that rejects the request (400) instead of propagating `null` silently.
- Add path-sanitisation utility method to `AbstractMethod` that both `getRelativePath` and `parseDestinationHeader` (via `DoCopy`) can call.
- Add/extend unit tests covering traversal inputs.

## Capabilities

### New Capabilities
- `path-traversal-prevention`: Validates and rejects user-supplied path values containing traversal sequences at the HTTP method layer before any store operation.

### Modified Capabilities

<!-- none — existing spec-level behaviour (store operations, HTTP status codes) unchanged except new 400 responses for malformed paths -->

## Impact

- `AbstractMethod.java` — new validation helper + call in `getRelativePath`
- `DoCopy.java` — updated `parseDestinationHeader` to reject traversal destinations
- Test classes for `DoGet`, `DoPut`, `DoDelete`, `DoMove`, `DoCopy`, `DoPropfind`, `DoLock` — new traversal-path test cases
