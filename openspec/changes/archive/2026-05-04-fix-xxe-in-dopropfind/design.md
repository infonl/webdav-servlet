## Context

`AbstractMethod.getDocumentBuilder()` (line 206) creates a `DocumentBuilderFactory` with only `setNamespaceAware(true)` set. By default JAXP parsers process DTDs and resolve external entities, which enables XXE attacks. Any caller that parses attacker-controlled XML (PROPFIND body in `DoPropfind`, LOCK body in `DoLock`) is exposed. GitHub code-scanning alert #4 identifies this as a high-severity vulnerability (CWE-611).

## Goals / Non-Goals

**Goals:**
- Disable DTD and external entity resolution in `getDocumentBuilder()` so all parse call sites are protected.
- Pass existing tests; add regression tests that confirm XXE is blocked.

**Non-Goals:**
- Replacing the JAXP/DOM parser with a different XML library.
- Hardening other XML sinks beyond `getDocumentBuilder()` (there are none in the current codebase).

## Decisions

### Harden at `getDocumentBuilder()`, not at each call site

All XML parsing in the library goes through this single factory method. Fixing it once protects every caller, and future callers inherit the protection without extra effort.

`DoProppatch` already has the OWASP feature flags applied inline (lines 128–137), but duplicates the logic outside of `getDocumentBuilder()`. It should be refactored to call `getDocumentBuilder()` so the protection lives in one place.

Alternatives considered:
- **Per-call-site feature flags** — more code changes, risk of missing a call site; `DoProppatch` shows this already diverged from the central method.
- **SchemaFactory / SAXParser** — not applicable; the existing DOM API is correct for the use case.

### Feature flags to disable (OWASP recommended set)

```
http://apache.org/xml/features/disallow-doctype-decl   → true
http://xml.org/sax/features/external-general-entities  → false
http://xml.org/sax/features/external-parameter-entities → false
http://apache.org/xml/features/nonvalidating/load-external-dtd → false
```
Plus `setXIncludeAware(false)` and `setExpandEntityReferences(false)` on the factory.

Disabling DTDs entirely (`disallow-doctype-decl`) is the strongest mitigation. Well-formed WebDAV XML bodies never use DTDs, so this is safe.

### `ParserConfigurationException` handling

Setting these features can throw `ParserConfigurationException` if the JAXP implementation does not support them. The method already throws `ServletException`; wrapping the exception preserves the existing contract and logs the root cause.

## Risks / Trade-offs

- **Incompatible JAXP implementation** → Risk is negligible; all modern JDK-bundled parsers (Xerces-based) support these features. If an exotic parser is used, the `ParserConfigurationException` surfaces at request time, which is far better than silent XXE exposure.
- **Rejecting requests with DTD** → Legitimate WebDAV clients never send DTDs. Any request that triggers a `SAXParseException` due to `disallow-doctype-decl` is either malformed or malicious and should return 500 (current behaviour for parse errors).
