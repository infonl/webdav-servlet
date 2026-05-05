## Context

`XMLWriter` (lifted from Apache Catalina) writes WebDAV XML responses by appending raw strings to a `StringBuffer`. `writeText()` and `writeProperty(String, String)` do not escape their input. User-controlled values — most notably resource paths in `AbstractMethod.sendReport()` and property values in `DoPropfind` — flow into these methods, allowing XML injection in multi-status responses.

The GitHub code-scanning alert references the data flow:
- `AbstractMethod.java:389` — `generatedXML.writeText(errorPath)` with `errorPath` from user request
- `XMLWriter.java` — `writeText` appends raw to buffer; buffer written to `Writer` at line 207

## Goals / Non-Goals

**Goals:**
- XML-escape `&`, `<`, `>` in `writeText()` and the value argument of `writeProperty(String, String)`.
- Cover the fix with unit tests for `XMLWriter` and `AbstractMethod.sendReport()`.

**Non-Goals:**
- Escaping element/namespace names in `writeElement()` — these are always programmer-supplied constants.
- Escaping attribute values in namespace declarations inside `writeElement()` — namespace URIs are programmer-supplied constants.
- Changing `writeData()` — it already uses CDATA, no escaping needed.
- Fixing the `_writer.write()` call itself — the issue is unescaped input entering the buffer, not the write path.

## Decisions

### Decision: Private static helper in `XMLWriter`

Add `private static String escapeXml(String text)` that replaces `&` → `&amp;`, `<` → `&lt;`, `>` → `&gt;`. Call it from `writeText()` and `writeProperty(String, String)` before appending.

**Alternatives considered:**
- Apache Commons Text `StringEscapeUtils.escapeXml10()` — adds a dependency not present in the project; not worth it for a four-line escaper.
- `javax.xml.bind.DatatypeConverter` — deprecated/removed in modern JDKs.
- Replacing `writeText()` call sites in `sendReport()` with a `writeData()` (CDATA) call — changes semantics; RFC 4918 does not mandate CDATA for `<href>` or `<status>` content and CDATA is uncommon in WebDAV clients.

### Decision: Do NOT escape `"` and `'` in text nodes

In XML text nodes these characters need no escaping. Escaping them produces unnecessarily verbose output and deviates from what existing WebDAV clients expect. Attribute values are always programmer-controlled in this codebase, so no attribute-value escaping is required.

## Risks / Trade-offs

- Existing resource paths containing literal `&` in their names will now appear XML-escaped (`&amp;`) in multi-status bodies. This is correct XML but clients must XML-parse the response — which they should already be doing. → No mitigation needed; any client that accepted unescaped `&` was relying on broken XML.
- `fromcatalina/XMLWriterTest.java` must be created (no existing test file for this class). → Low risk; straightforward unit tests.

## Migration Plan

No deployment migration needed. This is a library fix; consumers rebuild against the new version.
