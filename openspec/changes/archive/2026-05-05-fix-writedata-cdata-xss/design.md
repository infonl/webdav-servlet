## Context

Two security issues in the WebDAV XML response path:

**Issue 1 — `XMLWriter.writeData` (alert #18):**
`XMLWriter.writeData(String)` in `fromcatalina/XMLWriter.java` wraps a caller-supplied string in a CDATA section:
```java
_buffer.append("<![CDATA[").append(data).append("]]>");
```
A CDATA section is terminated by `]]>`. If `data` contains `]]>`, the CDATA block closes early and subsequent characters are interpreted as XML markup, enabling XML injection and XSS.

**Issue 2 — `AbstractMethod.sendReport` (alert #15):**
`sendReport` in `methods/AbstractMethod.java` writes a multi-status XML response but never calls `resp.setContentType(...)`. Every other XML-writing method in the codebase (`DoLock`, `DoPropfind`, `DoProppatch`) sets `"text/xml; charset=UTF-8"`. Without this header a browser may sniff the response as HTML, making XML-escaped user-controlled path values (e.g. `&lt;script&gt;`) potentially interpretable as live markup.

Both `data` in `writeData` and `errorPath` keys in `sendReport` are ultimately derived from user-controlled input (file names, request paths).

## Goals / Non-Goals

**Goals:**
- Prevent premature CDATA termination in `writeData` by escaping `]]>`.
- Ensure `sendReport` declares `text/xml; charset=UTF-8` before writing the response body.
- Add unit tests for both fixes.

**Non-Goals:**
- Changing the escaping behaviour of `writeText` or `writeProperty` (already fixed).
- Replacing CDATA sections with entity-escaped text in `writeData`.

## Decisions

### Decision 1: escape `]]>` in `writeData` by splitting the CDATA section

Replace every occurrence of `]]>` in `data` with `]]]]><![CDATA[>`.

**Rationale:** This is the canonical XML technique: close the current section, emit `]]` as a new CDATA payload, reopen a section, emit `>`. After XML parsing the decoded text equals the original. Single `String.replace` call, no regex. Alternative (entity-escape everything) would change the semantics of `writeData` from "raw data" to "text content".

### Decision 2: add `resp.setContentType("text/xml; charset=UTF-8")` in `sendReport`

Set the content type before writing the XML body, matching the pattern used by `DoLock`, `DoPropfind`, and `DoProppatch`.

**Rationale:** Explicit content type prevents browser MIME sniffing. One-line change; no behavioural impact on well-behaved WebDAV clients.

## Risks / Trade-offs

- [Risk] `writeData` callers that currently pass `]]>` literally will see different byte output (the split form), but decoded value is identical — no functional regression expected.
- [Risk] Adding `Content-Type` in `sendReport` could technically affect clients that relied on missing header and applied their own sniffing — acceptable because `text/xml` is the correct type per RFC 4918.
- [Risk] If `data` is `null`, the existing code would NPE; the fix handles null consistently with `escapeXml` (treat as empty string).

## Migration Plan

No deploy steps needed — pure library change. Callers are unaffected. Tests cover both regression and the fixed behaviour.
