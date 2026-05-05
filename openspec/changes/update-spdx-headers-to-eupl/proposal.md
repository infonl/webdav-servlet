## Why

The repository is being relicensed to EUPL-1.2+, but 52 of 62 Java source files lack SPDX headers entirely. Adding them completes the license compliance work started by the license-change branch and satisfies the convention in CLAUDE.md that all source files carry SPDX headers.

## What Changes

- Add `SPDX-FileCopyrightText` and `SPDX-License-Identifier` headers to all 52 Java source files that currently have no SPDX identifier.
- Files that are original project code (or modified from Apache originals) receive `SPDX-License-Identifier: EUPL-1.2+` and `SPDX-FileCopyrightText: 2026 INFO.nl`.
- Files in `fromcatalina/` that retain their original Apache License 2.0 text and have not been substantively modified beyond mechanical port receive `SPDX-License-Identifier: Apache-2.0` plus `SPDX-FileCopyrightText: 2026 INFO.nl` for any INFO.nl modifications.
- Files already carrying `SPDX-License-Identifier: EUPL-1.2+` are left unchanged (10 files already compliant).

## Capabilities

### New Capabilities

- `spdx-header-compliance`: All Java source files carry correct SPDX identifiers aligned with the project's EUPL-1.2+ license, distinguishing original project code from Apache-derived code in `fromcatalina/`.

### Modified Capabilities

_(none — no spec-level behavior changes)_

## Impact

- 52 Java source files in `src/main/java/` and `src/test/java/`.
- No runtime behavior change; headers are comments only.
- `fromcatalina/` files: existing Apache 2.0 prose header preserved; SPDX line added above or below it.
- All other files: SPDX block prepended as a comment header.
