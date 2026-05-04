## Why

The `DocumentBuilderFactory` in `AbstractMethod.getDocumentBuilder()` is created without disabling XML external entity (XXE) processing, allowing an attacker to supply a crafted PROPFIND request body that references external entities and trigger server-side file reads or SSRF. GitHub code-scanning alert #4 flags this as a high-severity vulnerability.

## What Changes

- Configure `DocumentBuilderFactory` in `AbstractMethod.getDocumentBuilder()` to disable DTD processing and external entity resolution before any parsing occurs.
- Refactor `DoProppatch` to use `AbstractMethod.getDocumentBuilder()` instead of its own inline `DocumentBuilderFactory` setup, removing the duplicated XXE-protection code.

## Capabilities

### New Capabilities

- `safe-xml-parsing`: `DocumentBuilderFactory` is hardened against XXE by disabling DTDs and external entity features; this applies to every call site that uses `getDocumentBuilder()` (currently `DoPropfind`, `DoLock`, and `DoProppatch` after refactoring).

### Modified Capabilities

<!-- none -->

## Impact

- `src/main/java/nl/info/webdav/methods/AbstractMethod.java` — `getDocumentBuilder()` method
- `src/main/java/nl/info/webdav/methods/DoProppatch.java` — inline factory replaced with `getDocumentBuilder()` call
- `DoPropfind` and `DoLock` inherit the fix automatically; no changes needed there
- No API or behaviour change visible to compliant WebDAV clients (well-formed requests parse identically)
