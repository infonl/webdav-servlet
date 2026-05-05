## Context

52 Java source files lack SPDX headers. 10 files already have `EUPL-1.2+`. The files split into two groups:

1. **Original project code** (44 files) — methods, interfaces, locking, exceptions, tests. No prior copyright header or a legacy Apache header that pre-dates INFO.nl ownership. These get `EUPL-1.2+`.
2. **`fromcatalina/` files** (4 files: `URLEncoder`, `XMLWriter`, `XMLHelper`, `RequestUtil`) — lifted from Apache Catalina with Apache License 2.0 prose headers intact. These get `Apache-2.0`.

The `fromcatalina/` files still carry their full Apache 2.0 prose block. SPDX lines are added as machine-readable identifiers; the prose is left in place.

## Goals / Non-Goals

**Goals:**
- Every `.java` file carries `SPDX-FileCopyrightText` and `SPDX-License-Identifier`.
- `fromcatalina/` files retain existing Apache 2.0 prose and receive `Apache-2.0` SPDX identifier.
- All other files without SPDX headers receive `EUPL-1.2+` + `2026 INFO.nl` copyright.
- Files already compliant (`EUPL-1.2+`) are untouched.

**Non-Goals:**
- Removing Apache 2.0 prose headers from `fromcatalina/` files.
- Changing any file's logic or formatting beyond the header comment block.
- Running `spotlessApply` as part of this change (headers are above the formatter's concern).

## Decisions

### D1: SPDX placement for files with existing headers

For files that have a `/* ... */` prose block (Apache-derived), insert the SPDX lines at the top of the existing comment block as the first two lines. This groups the machine-readable metadata with the human-readable notice.

For files with no header at all, prepend a new `// SPDX-...` two-line block before the `package` statement.

**Alternative considered**: Add SPDX as a separate new comment above the existing block. Rejected — creates two adjacent comment blocks and looks inconsistent with the existing 10 compliant files (which use inline style).

### D2: Copyright year

Use `2026` (current year) for all added headers. The project is being adopted under INFO.nl in 2026.

### D3: `fromcatalina/` license identifier

Use `Apache-2.0` for `URLEncoder`, `XMLWriter`, `XMLHelper`, `RequestUtil`. These files are verbatim or near-verbatim copies of Apache Catalina source. Relicensing them to EUPL-1.2+ would misrepresent their provenance.

**Alternative considered**: Add dual SPDX (`Apache-2.0 AND EUPL-1.2+`). Rejected — INFO.nl has not added substantial new code to these files; the copyright is still primarily Apache's.

## Risks / Trade-offs

- **Spotless formatter may reorder** the header block if `spotlessApply` is run after. Mitigation: verify with `spotlessCheck` after applying.
- **`fromcatalina/` provenance** — if INFO.nl has made substantive changes beyond the mechanical port, `Apache-2.0` alone may not be sufficient. Mitigation: treat as a known assumption; a future audit can reassess.
