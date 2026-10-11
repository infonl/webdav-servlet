## Context

JaCoCo baseline (2026-10-11, `./gradlew test jacocoTestReport`): **65.1% line, 52.4% branch, 80.6% method**. The archived change `2026-05-04-add-unit-test-coverage` added `ResourceLocksTest`, `LockedObjectTest` and `AbstractMethodTest`; every `Do*` handler already has a test class, but mostly for the happy path.

Largest remaining gaps (missed lines):

| Class | Line | Branch | Main gaps |
|---|---|---|---|
| `fromcatalina/RequestUtil` | 10% | 7% | Mostly dead code; only `URLDecode` used (by `DoCopy`) |
| `DoPropfind` | 48% | 32% | `parseProperties` per-property branches, `writeLockDiscoveryElements` |
| `DoLock` | 70% | 54% | null-resource lock, refresh, timeouts, Darwin/Transmit workarounds |
| `DoCopy` | 74% | 64% | destination parsing/normalize, overwrite, folder errors |
| `WebdavServlet` | 5% | 0% | `init`, `constructStore`, `getFileRoot` |
| `WebDavServletBean` | 64% | 36% | `destroy`, exception mapping in `service`, `debugRequest` |
| `ResourceLocks` | 76% | 60% | `cleanLockedObjects` (0%) |
| `DoProppatch`, `DoHead`, `DoGet`, `DoPut`, `DoMkcol`, `DoMove`, `DoDelete` | 60–80% | 45–75% | error/status branches |

Constraints: JUnit 5 + JMock 2 via `MockTest`; no production code changes; `fromcatalina/` must not be refactored; SPDX header on new files; Spotless formatting.

## Goals / Non-Goals

**Goals:**
- Raise bundle coverage to **≥ 80% line and ≥ 65% branch**.
- Cover error and status-code branches of every method handler, especially those relevant to the recent security fixes (destination path handling, XML parsing, response output).
- Make coverage regressions visible on PRs through a non-blocking Codecov status. The local build never fails on coverage.

**Non-Goals:**
- Testing unused Catalina code in `RequestUtil` (`encodeCookie`, `filter`, `normalize`, `parseCookieHeader`, `parseParameters`, `parseCharacterEncoding`).
- Fixing bugs found while writing tests — they are recorded (see Decisions) and handled in separate changes.
- Trivial exception-class constructors and `debugRequest` logging output (covered incidentally or left out).
- Integration tests with a real servlet container.
- Per-class coverage thresholds.

## Decisions

### 1. Characterization tests: assert current behavior
Specs describe the expected status codes per the WebDAV handler code as read. Where actual behavior differs from a spec scenario, the test asserts **actual** behavior, the scenario is updated, and the discrepancy is listed in the task notes as a candidate bug. This keeps the change free of production edits.
*Alternative:* fix bugs inline — rejected; mixes concerns and complicates review.

### 2. Extend existing test classes; add new classes only for untested types
New tests go into the existing `Do*Test`, `WebDavServletBeanTest`, `WebdavServletTest`, `LocalFileSystemStoreTest`, `ResourceLocksTest`, `LockedObjectTest`. New classes: `RequestUtilTest`, `URLEncoderTest`, `XMLHelperTest` in `src/test/java/nl/info/webdav/fromcatalina/`.
*Alternative:* one new class per capability — rejected; scatters tests for the same class.

### 3. Mocking strategy
- `Do*` handlers: keep JMock `Mockery` from `MockTest`, matching existing tests. Use `TestingOutputStream` / `DelegatingServletInputStream` for bodies and capture XML output to assert on content (`contains` on key elements, not full-string equality, to avoid brittleness on whitespace/namespaces).
- `ResourceLocks` / `LockedObject`: real objects, no mocks (as in existing tests).
- `WebdavServlet`: subclass or JMock `ServletConfig`/`ServletContext` returning init params; `rootpath` points to a JUnit `@TempDir`. `constructStore` is `protected` and can be called directly from a test in the same package.
- `LocalFileSystemStore`: real filesystem under `@TempDir`. Uncreatable root simulated by placing a regular file where a parent directory is expected (portable, no permission tricks).
- Use `@ParameterizedTest` for exception→status mappings to keep tests compact.

### 4. Non-blocking coverage target via Codecov PR status
The local build must never fail on coverage. A PR that misses the target should show a failing check on GitHub, but must still be mergeable. `build-test-publish.yml` already uploads the JaCoCo XML report to Codecov (`codecov/codecov-action`, default `fail_ci_if_error: false`), so only a `codecov.yml` at the repo root is added:
```yaml
coverage:
  status:
    project:
      default:
        target: 83%      # Codecov-reported coverage at merge, rounded down
        threshold: 1%    # tolerate small fluctuations
    patch:
      default:
        target: 80%
```
Codecov then posts `codecov/project` and `codecov/patch` commit statuses. They do not block merging unless branch protection lists them as required, and they must stay non-required.
Codecov measures lines only, so the branch target (≥ 65%) is tracked in the PR description and the JaCoCo report, not enforced.
*Alternatives:*
- `jacocoTestCoverageVerification` wired into `check`: rejected, because it fails the local build and the required `Build and Publish` job.
- A separate GitHub Actions job that runs `jacocoTestCoverageVerification`: this would also fail only on GitHub, but it needs an extra workflow job and Gradle run while Codecov already provides the same signal. Kept as a fallback if Codecov is dropped.

### 5. Order of work by coverage yield
Largest gains first: `DoPropfind` → `DoLock`/`DoUnlock` → `DoCopy`/`DoMove`/`DoDelete` → `WebdavServlet`/`WebDavServletBean` → remaining handlers → locking → store → Catalina utilities → threshold. Re-measure after each group.

## Risks / Trade-offs

- [Brittle tests tied to JMock expectation order / XML formatting] → assert on semantic fragments, use `allowing` for incidental calls, keep `oneOf` for the behavior under test.
- [`*WAR-FILE-ROOT*` branch depends on code source location] → only assert the failure path (test classpath has no `/WEB-INF/`); success path left uncovered.
- [Static state: `AbstractMethod.URL_ENCODER`, shared `ResourceLocks` across tests] → create fresh handler/lock instances per test.
- [80/65 target not reached without testing dead `RequestUtil` code] → if needed, set the Codecov target to the achieved value rather than testing dead code; document final numbers in tasks.
- [Codecov status accidentally made a required check] → verify in branch protection settings that `codecov/*` is not required.
- [Codecov line counting differs slightly from local JaCoCo] → use `threshold: 1%` on the project status.
- [Characterization tests lock in buggy behavior] → discrepancies are listed explicitly so they can be fixed (and tests updated) in follow-up changes.

## Migration Plan

Test-only change plus a `codecov.yml`. Rollback: revert the commit. Codecov picks up `codecov.yml` on the next upload; no workflow or Gradle changes.

## Open Questions

- Should thresholds later move to per-package rules (e.g. stricter for `methods/`)? Out of scope for now.
