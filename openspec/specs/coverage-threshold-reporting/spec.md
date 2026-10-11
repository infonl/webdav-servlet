# coverage-threshold-reporting Specification

## Purpose
Coverage targets reported as non-blocking Codecov PR statuses; the local Gradle build never fails on coverage.
## Requirements
### Requirement: Local build never fails on coverage
The Gradle build SHALL NOT fail because of coverage levels. `./gradlew build` and `./gradlew check` MUST succeed whenever compilation, tests and Spotless pass, regardless of coverage.

#### Scenario: Coverage below target locally
- **WHEN** a developer runs `./gradlew build` and coverage is below the target
- **THEN** the build succeeds and the JaCoCo XML report is still generated

### Requirement: Coverage target reported as non-blocking PR status
The repository SHALL contain a `codecov.yml` defining a project coverage target and a patch coverage target, so that Codecov reports a failing commit status on a pull request whose coverage misses the target.

#### Scenario: PR meets coverage targets
- **WHEN** a PR's uploaded coverage meets the project and patch targets
- **THEN** Codecov reports successful `codecov/project` and `codecov/patch` statuses

#### Scenario: PR misses coverage target
- **WHEN** a PR's uploaded coverage is below the project or patch target
- **THEN** Codecov reports a failing status on the PR, and the PR can still be merged because the status is not a required check

#### Scenario: Codecov upload failure does not fail CI
- **WHEN** the Codecov upload step fails (e.g. service unavailable)
- **THEN** the `Build and Publish` workflow job does not fail because of it

### Requirement: Project target reflects achieved coverage
The project coverage target SHALL be set at or just below the coverage reached by this change (aim: ≥ 80% line), so that later regressions are visible on PRs.

#### Scenario: Target recorded
- **WHEN** the change is complete
- **THEN** `codecov.yml` contains a `project` target the current suite meets

