# Telemetry and observability

## Names and attributes

- Add every new span name to `SpanName`; reuse an existing semantic name when possible.
- Add every attribute key to the appropriate `AttributeName`; never inline string keys.
- Before adding an attribute, verify uniqueness, data classification, stable/controlled cardinality, expected value domain, and units.
- Follow the existing constant naming style (`snake_case` or lower camel case as established by the enum).
- Use enum `.name()` values consistently.
- Set optional attributes only when present; avoid empty-string placeholders.
- Use primitive booleans and numeric counts/durations instead of stringified values.
- For an attribute marked `isDateTime`, use the repository's expected instant units, normally epoch milliseconds.
- For durations, prefer a separate numeric metric in milliseconds; use `isDateTime=true` only for an instant.
- Add Javadoc for a new attribute's purpose, values, and units.
- A new Common `AttributeName.java` entry must also be added to Broker's `AttributeName.java`. Leave a non-blocking `Follow-up (separate PR):` review comment.
- Note required dashboard or processing-rule updates only when the changed semantic actually requires them.

## Span lifecycle

Use the established pattern:

```java
final Span span = OTelUtility.createSpan(SpanName.MSAL_PerformIpcStrategy.name());
try (final Scope scope = SpanExtension.makeCurrentSpan(span)) {
    span.setAttribute(AttributeName.ipc_strategy.name(), strategy.getType().name());
    span.setAttribute(AttributeName.broker_operation.name(), operation.getMethodName());
    span.setStatus(StatusCode.OK);
    return result;
} catch (final Throwable t) {
    span.setStatus(StatusCode.ERROR);
    span.recordException(t);
    throw t;
} finally {
    span.end();
}
```

- Use `SpanExtension.makeCurrentSpan(span)`, not direct `span.makeCurrent()`.
- Use `SpanExtension.current()`, not direct `Span.current()`, for older-device compatibility.
- Set `StatusCode.OK` near the end of successful work, after all attributes are set and just before returning.
- On failure, set `StatusCode.ERROR`, record the exception, and rethrow.
- End every span in `finally`; verify all early returns still pass through it.
- Propagate/set the correlation ID early and consistently for major operations.

Create spans for network/cross-process boundaries, major asynchronous boundaries, performance-critical phases, key generation, and token acquisition. Do not create spans for trivial getters, small local helpers, or operations already covered by an existing span.

Before adding a span, confirm no existing `SpanName` describes the operation. Choose a concise, action-oriented PascalCase name; reuse names such as `KeyPairGeneration` rather than creating a duplicate. Avoid micro-spans for trivial getters in the token-assembly path.

## Anti-patterns

Flag inline attribute keys, duplicate semantic span names, direct `Span.current()`, direct `span.makeCurrent()`, missing `finally` cleanup, raw sensitive values such as tokens or user principal names, stringified numeric/boolean values, and duplicate full stack traces in multiple telemetry/log sinks.

Allowed:

```java
span.setAttribute(AttributeName.http_status_code.name(), response.getCode());
```

Disallowed:

```java
span.setAttribute("httpStatus", response.getCode());
span.setAttribute(AttributeName.access_token.name(), rawAccessToken);
```

Do not demand tests solely for attaching attributes/values or for the bare `recordException(...)` call. Tests remain appropriate for conditional emission, lifecycle/status logic, and whether an exception is recorded on the correct path.
