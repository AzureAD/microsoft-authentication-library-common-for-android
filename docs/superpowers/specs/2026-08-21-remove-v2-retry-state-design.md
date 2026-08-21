# Remove Native Auth V2 Retry State

## Goal

Align Common's Native Auth V2 error results with the MSAL V2 public API contract. Invalid-code
and invalid-password errors report error metadata only; the developer retries through the
existing state object used to make the failed call.

## Design

Remove `retryState` from these internal interaction results:

- `NativeAuthV2InteractionApiResult.InvalidCode`
- `NativeAuthV2InteractionApiResult.InvalidPassword`

Remove `retryState` from these Common command results:

- `NativeAuthV2CommandResult.IncorrectCode`
- `NativeAuthV2CommandResult.PasswordNotAccepted`

The response parser will continue accepting the caller's previous continuation state because
other interaction outcomes use it to resolve successor links. It will no longer copy that state
into invalid-code or invalid-password errors. The flow controller will map those errors without
adding state.

This matches MSAL Android PR #2547: `SubmitCodeErrorV2` and `SubmitNewPasswordErrorV2` expose
error metadata but no continuation state. The existing `CodeRequiredStateV2` or
`NewPasswordRequiredStateV2` remains owned by the developer and is reused for another attempt.

## Error Behavior

Error classification, service error details, correlation IDs, suberrors, and error codes remain
unchanged. This change only removes duplicated state from error objects.

## Tests

Update parser, controller, and command-result tests to construct and assert invalid-code and
invalid-password results without `retryState`. Keep coverage for error classification, suberrors,
error codes, and sanitized string output.
