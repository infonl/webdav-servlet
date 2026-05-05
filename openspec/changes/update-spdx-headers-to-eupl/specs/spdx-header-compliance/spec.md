## ADDED Requirements

### Requirement: Every Java source file carries an SPDX license identifier
Every `.java` file in `src/` SHALL contain an `SPDX-License-Identifier` comment line.

#### Scenario: Original project file without SPDX header
- **WHEN** a `.java` file has no `SPDX-License-Identifier` and is not derived from Apache Catalina source
- **THEN** the file SHALL have `// SPDX-FileCopyrightText: 2026 INFO.nl` and `// SPDX-License-Identifier: EUPL-1.2+` prepended before the `package` statement

#### Scenario: Apache-derived file in fromcatalina/ without SPDX header
- **WHEN** a `.java` file in `fromcatalina/` has no `SPDX-License-Identifier` and retains its Apache License 2.0 prose header
- **THEN** the file SHALL have `SPDX-FileCopyrightText: 2026 INFO.nl` and `SPDX-License-Identifier: Apache-2.0` added inside its existing comment block

#### Scenario: File already has EUPL-1.2+ SPDX header
- **WHEN** a `.java` file already contains `SPDX-License-Identifier: EUPL-1.2+`
- **THEN** the file SHALL NOT be modified

### Requirement: SPDX copyright line references 2026 INFO.nl
Every SPDX block added by this change SHALL use `SPDX-FileCopyrightText: 2026 INFO.nl` as the copyright line.

#### Scenario: New SPDX header copyright year and owner
- **WHEN** an SPDX header is added to any file
- **THEN** the `SPDX-FileCopyrightText` value SHALL be `2026 INFO.nl`
