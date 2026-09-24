# Telemetry and observability

## Names and attributes

- Add every new span name to `SpanName`; reuse an existing semantic name when possible.
- Add every attribute key to the appropriate `AttributeName`; never inline string keys.
- Before adding an attribute, verify uniqueness, data classification, stable/controlled cardinality, expected value domain, and units.
- Use enum `.name()` values consistently.
- Set optional attributes only when present; avoid empty-string placeholders.
- Use primitive booleans and numeric counts/durations instead of stringified values.
- For an attribute marked `isDateTime`, use the repository's expected instant units, normally epoch milliseconds.
- Add Javadoc for a new attribute's purpose, values, and units.
- A new Common `AttributeName` must also be added to Broker's corresponding enum. Request this as a non-blocking `Follow-up (separate PR):` dependency.
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
- Set `StatusCode.OK` only after successful work.
- On failure, set `StatusCode.ERROR`, record the exception, and rethrow.
- End every span in `finally`; verify all early returns still pass through it.
- Propagate/set the correlation ID early and consistently for major operations.

Create spans for network/cross-process boundaries, major asynchronous boundaries, performance-critical phases, key generation, and token acquisition. Do not create spans for trivial getters, small local helpers, or operations already covered by an existing span.

## Anti-patterns

Flag inline attribute keys, duplicate semantic span names, direct `Span.current()`, direct `span.makeCurrent()`, missing `finally` cleanup, raw sensitive attribute values, stringified numeric/boolean values, and duplicate full stack traces in multiple telemetry/log sinks.

Do not demand tests solely for attaching attributes/values or for the bare `recordException(...)` call. Tests remain appropriate for conditional emission, lifecycle/status logic, and whether an exception is recorded on the correct path.
