## Why

Two GitHub code-scanning alerts (CWE-79 XSS):

- **Alert #18** — `XMLWriter.writeData(String)` wraps user-controlled input in a CDATA section without escaping `]]>`. An attacker can inject `]]>` to close the block early and insert arbitrary XML markup.
- **Alert #15** — `AbstractMethod.sendReport()` writes an XML multi-status response to the `HttpServletResponse` writer without setting a `Content-Type` header. Without an explicit `text/xml` content type, a browser may sniff the response as HTML and interpret XML-escaped user-controlled values (e.g. error paths) as live markup.

## What Changes

- `XMLWriter.writeData(String)` will sanitize the `data` argument by replacing every occurrence of `]]>` with `]]]]><![CDATA[>` before appending it to the buffer, preventing premature CDATA termination.
- `AbstractMethod.sendReport()` will call `resp.setContentType("text/xml; charset=UTF-8")` before writing the XML body, consistent with `DoLock`, `DoPropfind`, and `DoProppatch`.
- Unit tests will be added covering both fixes.

## Capabilities

### New Capabilities

_(none — this is a security hardening of existing methods)_

### Modified Capabilities

- `safe-xml-response-output`: Add requirement that `writeData` prevents CDATA injection; add requirement that `sendReport` sets `Content-Type: text/xml; charset=UTF-8` before writing the response body.

## Impact

- `src/main/java/nl/info/webdav/fromcatalina/XMLWriter.java` (line 188)
- `src/main/java/nl/info/webdav/methods/AbstractMethod.java` (`sendReport` method)
- `src/test/java/nl/info/webdav/fromcatalina/XMLWriterTest.java`
- `src/test/java/nl/info/webdav/methods/AbstractMethodTest.java` (new or existing)
- No API surface change; callers of `writeData` and `sendReport` are unaffected in the normal case
