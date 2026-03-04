# Microsoft Authentication Library Common for Android
[![CI](https://github.com/AzureAD/microsoft-authentication-library-common-for-android/actions/workflows/ci.yml/badge.svg?branch=dev)](https://github.com/AzureAD/microsoft-authentication-library-common-for-android/actions/workflows/ci.yml)

This library contains code shared between the [Active Directory Authentication Library (ADAL) for Android](https://github.com/AzureAD/azure-activedirectory-library-for-android) and the [Microsoft Authentication Library (MSAL) for Android](https://github.com/AzureAD/microsoft-authentication-library-for-android). This library includes only internal classes and is **NOT** part of the public API. The contents of this library are subject to change without notice.

### Project Structure

| Module | Description |
|---|---|
| `common` | Android-specific shared code (controllers, cache, crypto, broker IPC, UI). |
| `common4j` | Pure-Java shared code usable outside Android (protocol logic, token parsing, telemetry). |
| `testutils` | Shared test utilities for Android instrumented tests. |
| `uiautomationutilities` | UI automation helpers for end-to-end tests. |
| `LabApiUtilities` | Utilities for communicating with the Microsoft Identity Lab API in tests. |

### Issues
We encourage users of ADAL and MSAL to file issues against the library they are using rather than against common. This helps us understand the version of the common library in use based on the version of ADAL or MSAL against which you report the issue. If you determine that the issue is with this library specifically, please create it in this repo. Likewise, if you have a suggestion, request, or other feedback related to common, please file it here.

### Contributing

This project welcomes contributions and suggestions. Most contributions require you to agree to a
Contributor License Agreement (CLA) declaring that you have the right to, and actually do, grant us
the rights to use your contribution. For details, visit https://cla.microsoft.com.

When you submit a pull request, a CLA-bot will automatically determine whether you need to provide
a CLA and decorate the PR appropriately (e.g., label, comment). Simply follow the instructions
provided by the bot. You will only need to do this once across all repos using our CLA.

This project has adopted the [Microsoft Open Source Code of Conduct](https://opensource.microsoft.com/codeofconduct/).
For more information see the [Code of Conduct FAQ](https://opensource.microsoft.com/codeofconduct/faq/) or
contact [opencode@microsoft.com](mailto:opencode@microsoft.com) with any additional questions or comments.

### Development Setup

**Requirements:**
- Android Studio (latest stable release recommended)
- JDK 11 or higher
- Android SDK with API level 21 or higher

**Build from the command line:**
```
./gradlew assembleDebug
```

**Android Studio:**
This project uses [Lombok](https://projectlombok.org/) internally. To build successfully within Android Studio, install the [Lombok Plugin](https://plugins.jetbrains.com/plugin/6317-lombok) from the plugin marketplace before importing the project.
