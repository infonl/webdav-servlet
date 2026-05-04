## Context

The locking subsystem (`ResourceLocks`, `LockedObject`) and the shared `AbstractMethod` base class contain the most complex business logic in the library — lock conflict detection, owner tracking, timeout lifecycle, path utilities, ETag computation, and lock-token header parsing — yet none of these classes have direct unit tests. Existing tests exercise them only indirectly through the HTTP method tests (JUnit 5 + JMock 2, all extending `MockTest`).

## Goals / Non-Goals

**Goals:**
- Direct unit tests for `ResourceLocks`, `LockedObject`, and the utility methods in `AbstractMethod`.
- Tests use the same JUnit 5 + JMock 2 conventions already established in the project.
- Each test covers a specific scenario, isolated, deterministic, and fast.

**Non-Goals:**
- Integration tests with a real store or servlet container.
- Coverage of the HTTP method classes (`DoGet`, `DoPut`, etc.) — they already have tests.
- Mutation testing or coverage targets.

## Decisions

### Direct instantiation for `ResourceLocks` and `LockedObject`

Neither class requires external dependencies beyond each other; both are directly instantiable. No mocking needed in the locking tests. This keeps tests simple and avoids false coverage from mock returns.

*Alternative considered*: mocking `ITransaction` — rejected because `ResourceLocks` only passes the transaction parameter through to recursive calls; the actual logic never reads from it.

### Minimal concrete subclass for `AbstractMethod`

`AbstractMethod` is abstract and `IMethodExecutor` requires an `execute()` method. Tests instantiate an anonymous inner-class stub with an empty `execute()`. All methods under test are `protected`, so the subclass also exposes them as package-visible delegating methods.

*Alternative considered*: reflection to call protected methods — rejected as brittle and harder to read.

### JMock for `AbstractMethod.checkLocks` only

`checkLocks` calls into `IResourceLocks` and reads `HttpServletRequest` headers, so these two dependencies need mocking. All other `AbstractMethod` methods under test are pure functions (no I/O, no collaborators), so no mocks are needed there.

### No changes to production code

All three test targets are testable as-is. No visibility or refactoring changes are needed.

## Risks / Trade-offs

- `hasExpired()` and timeout-related assertions depend on `System.currentTimeMillis()`. Tests avoid sleeping by constructing objects with an already-expired `_expiresAt` (set directly via the package-accessible field or via `refreshTimeout(0)`). → Mitigation: set `_expiresAt = 1` (epoch + 1 ms) directly on the `LockedObject` for expired-state tests.
- `cleanLockedObjects` is private; it is exercised indirectly through `unlock` after setting `_cleanupCounter` above the limit. → Mitigation: set `_cleanupCounter` to `_cleanupLimit + 1` before the triggering operation in the test.
