# False-positive suppression

Default to silence when a candidate finding matches these patterns. A missed nit is preferable to a confidently wrong or irrelevant comment.

## Verify behavior, not names

Do not infer inheritance, field meaning, endpoint/source, sensitivity, reachability, or lifecycle from an identifier. Trace the actual type hierarchy and population/control flow.

## Exception inheritance

Before claiming that an exception is unhandled or needs another catch/retry branch, verify whether an existing caught type is its superclass. For example, verify the current hierarchy before commenting on `UiRequiredException` handling by code that already handles `ServiceException`.

## Flights and hypothetical configurations

Do not block on a flight/configuration combination that may not exist in production. Repository defaults are not proof of deployed rollout state.

If a risky combination remains plausible but unverified, post at most one non-blocking comment prefixed `Sanity check:` asking whether it is reachable. Comment normally only when the diff creates an unsafe default or a real reachable path.

## Instrumentation-only tests

Do not request a unit test whose sole purpose is setting span attributes, attaching values, invoking the bare `recordException(...)` attachment call, or populating an instrumentation field.

Tests are still appropriate for branching, parsing/serialization, retries/fallbacks, concurrency, public behavior, conditional telemetry emission, span lifecycle/status, and whether `recordException` runs on the correct error path.

## Cross-PR and cross-repository scope

Do not request unrelated cleanup or downstream work. Raise a cross-repository item only when the changed Common contract requires or breaks it, such as a Broker telemetry-enum mirror, `OneAuthSharedFunctions` contract change, or consumed IPC key.

Label a genuine dependency `Follow-up (separate PR):` and keep it non-blocking for the current PR.

## Unverified domain semantics

Do not assert what a field or data source means without tracing its population logic. If a necessary claim still depends on an unverified domain fact, omit it or state `Assumption: ... If incorrect, disregard.`

## Additional suppressions

- Do not flag unchanged legacy code unless the change directly interacts with it and introduces risk.
- Do not require refactors beyond the PR's scope unless needed to fix a severe security/correctness issue.
- Do not request style changes that conflict with repository conventions or automated formatting.
- Do not request tests solely for low-risk telemetry attachment.
- Do not ask for redundant documentation that accurately restates all non-obvious behavior already.
- Do not suggest `val final`, Java `final` for a Kotlin local/property, or `@NonNull` for Kotlin.
- Do not flag intentional thread confinement, safely published immutable data, or generated code with verified synchronization.
