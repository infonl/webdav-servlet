## Why

`XMLWriter.writeText()` and `writeProperty()` append raw text into XML buffers without escaping XML special characters (`<`, `>`, `&`, `"`, `'`). User-controlled values such as resource paths flow into these methods via `AbstractMethod.sendReport()` and `DoPropfind`, allowing injection of arbitrary XML markup into WebDAV responses — a stored/reflected XSS vector in any WebDAV client that renders the response as HTML.

## What Changes

- `XMLWriter.writeText(String)` SHALL XML-escape its argument before appending to the buffer.
- `XMLWriter.writeProperty(String, String)` SHALL XML-escape the value before appending to the buffer.
- Unit tests for both `XMLWriter` and `AbstractMethod.sendReport()` SHALL cover paths containing XML special characters.

## Capabilities

### New Capabilities

- `safe-xml-response-output`: XML-escape user-supplied text in `XMLWriter` so WebDAV XML responses cannot contain injected markup.

### Modified Capabilities

<!-- none -->

## Impact

- `src/main/java/nl/info/webdav/fromcatalina/XMLWriter.java` — `writeText()` and `writeProperty(String, String)`
- `src/main/java/nl/info/webdav/methods/AbstractMethod.java` — `sendReport()` calls `writeText()` with user-controlled `errorPath`
- `src/test/java/nl/info/webdav/fromcatalina/XMLWriterTest.java` — new/updated tests
- `src/test/java/nl/info/webdav/methods/AbstractMethodTest.java` — new/updated tests
