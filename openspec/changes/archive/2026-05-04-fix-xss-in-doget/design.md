## Context

`DoGet.folderBody` builds an HTML directory listing by string-concatenating path segments and child resource names directly into a `StringBuilder`. Neither the `path` parameter nor the child names from `IWebdavStore.getChildrenNames()` are HTML-encoded before insertion. Any value containing `<`, `>`, `"`, `&`, or `'` can inject arbitrary HTML, including `<script>` tags.

Affected locations in `DoGet.java`:
- `folderBody`: `path` in `<title>` (line ~119), child name in `href` attribute (line ~138), child name as link text (line ~150)
- `getHeader`: `path` in `<h1>` (line ~271)

The library has zero runtime dependencies (only `jakarta.servlet.api` compile-only). This constraint rules out pulling in Apache Commons Text or OWASP Java Encoder.

## Goals / Non-Goals

**Goals:**
- Eliminate XSS by HTML-encoding all user-supplied values written into HTML output
- Keep zero new runtime dependencies
- Leave the public API (`IWebdavStore`, `IMethodExecutor`, `ITransaction`, `DoGet` overridable methods) unchanged

**Non-Goals:**
- Sanitizing or validating resource names at the store level
- Encoding URLs in `href` attributes beyond what already occurs (URL encoding is a separate concern; this fix targets HTML context encoding only)
- Changing the visual appearance of the directory listing

## Decisions

### Decision: Private static `escapeHtml` helper in `DoGet`

Add a `private static String escapeHtml(String input)` method to `DoGet` that replaces `&`, `<`, `>`, `"`, and `'` with their named HTML entities. Call it on every user-controlled value before appending to `childrenTemp`.

**Alternatives considered:**
- *Apache Commons Text `StringEscapeUtils.escapeHtml4`*: Correct, but adds a transitive dependency to a library. Unacceptable for a zero-dep library published to Maven Central.
- *Jakarta EE `HtmlUtils` or servlet API helper*: No such utility exists in Jakarta Servlet API 6.1.
- *`org.owasp.encoder.Encode.forHtml`*: Same objection as Commons Text — new runtime dependency.

A five-character entity map is stable, well-understood, and requires no ongoing maintenance.

### Decision: Encode `path` in both `folderBody` and `getHeader`

`getHeader` is `protected` and overridable, but the default implementation interpolates `path` unsafely. Fix the default; subclasses that override it are responsible for their own output.

## Risks / Trade-offs

- [Visual regression if path/names contain `&` or `<`] → After fix these render as literal characters (correct). Before fix they could corrupt HTML structure. This is the intended behaviour.
- [Subclass `getHeader`/`getFooter` overrides remain unsafe] → Out of scope; those are consumer code. Javadoc note can warn implementors.

## Migration Plan

Drop-in fix, no API changes, no migration required. Library consumers see no behavioural change for well-formed resource names.
