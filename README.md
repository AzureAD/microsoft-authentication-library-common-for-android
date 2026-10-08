# Microsoft Authentication Library Common for Android
[![Build Status](https://travis-ci.com/AzureAD/microsoft-authentication-library-common-for-android.svg?token=h2nbumGCE3DdxpFdJZ6S&branch=dev)](https://travis-ci.com/AzureAD/microsoft-authentication-library-common-for-android)

This library contains code shared between the [Active Directory Authentication Library (ADAL) for Android](https://github.com/AzureAD/azure-activedirectory-library-for-android) and the [Microsoft Authentication Library (MSAL) for Android](https://github.com/AzureAD/microsoft-authentication-library-for-android).  This library includes only internal classes and is **NOT** part of the public API.  The contents of this library are subject to change without notice.

### Issues
We encourage users of ADAL and MSAL to file issues against the library that they are using rather than against common.  This helps us understand the version of the common library in use based on the version of ADAL or MSAL against which you report the issue.  With that said, if you determine that the issue is indeed with common please go ahead and create it within this repo.  Likewise if you have a suggestion, request and/or other feedback relative to common please file it here.

### Contributing

This project welcomes contributions and suggestions.  Most contributions require you to agree to a
Contributor License Agreement (CLA) declaring that you have the right to, and actually do, grant us
the rights to use your contribution. For details, visit https://cla.microsoft.com.

When you submit a pull request, a CLA-bot will automatically determine whether you need to provide
a CLA and decorate the PR appropriately (e.g., label, comment). Simply follow the instructions
provided by the bot. You will only need to do this once across all repos using our CLA.

This project has adopted the [Microsoft Open Source Code of Conduct](https://opensource.microsoft.com/codeofconduct/).
For more information see the [Code of Conduct FAQ](https://opensource.microsoft.com/codeofconduct/faq/) or
contact [opencode@microsoft.com](mailto:opencode@microsoft.com) with any additional questions or comments.

### Android Studio Build Requirement
Please note that this project uses [Lombok](https://projectlombok.org/) internally and while using Android Studio you will need to install [Lombok Plugin](https://plugins.jetbrains.com/plugin/6317-lombok) to get the project to build successfully within Android Studio.

### Onboarding telemetry integration

Common accepts optional accumulated onboarding JSON through `InteractiveTokenCommandParameters.onboardingSeedJson` (or `AuthorizationActivityParameters.onboardingSeedJson` for direct authorization launches). The latter also accepts `onboardingClientId` and `onboardingTarget` (sorted, space-joined scopes). The authorization Fragment retains its local `OnboardingTelemetryRecorder` across view/configuration recreation, attaches it to each WebView client before navigation, and exports a full snapshot on every delivered `RawAuthorizationResult`. This state is not persisted across process death; absent or unavailable telemetry does not affect authorization.

The snapshot is optional metadata (`RawAuthorizationResult.getOnboardingTelemetryJson()`, also exposed on the converted `AuthorizationResult` and `AcquireTokenResult.getOnboardingBlob()` for local interactive requests). It is **not** a token issuance signal: `COMPLETED` only indicates an authorization redirect. Consumers must incorporate the returned full snapshot **once**, not append it to the original seed, then record later native/token events and finalize at token-request completion. This assumes the request owner records native events before passing the seed and after consuming the snapshot; simultaneous independent native recording cannot be merged losslessly without an ordered event-identity contract.

**Coordinated consumer work remains required outside this repository:** OneAuth `EmbeddedBrowser` must read the returned authorization snapshot and use its existing write-back into the original `AuthParametersInternal`; the request owner must transport the seed through the authorization launch contract. For brokered calls, the broker command/IPC must pass its seed into the authorization launch and carry the accumulated snapshot through its existing `BrokerResult` onboarding blob response, incorporating any broker-native events before finalizing. Common does not mutate OneAuth's `AuthParametersInternal` or emit MATS.
