# Microsoft Authentication Library Common for Android
[![Build Status](https://github.com/AzureAD/microsoft-authentication-library-common-for-android/actions/workflows/ci.yml/badge.svg?branch=dev)](https://github.com/AzureAD/microsoft-authentication-library-common-for-android/actions/workflows/ci.yml)

This library contains code shared between the [Microsoft Authentication Library (MSAL) for Android](https://github.com/AzureAD/microsoft-authentication-library-for-android) and the Microsoft Authenticator/Company Portal (Broker) apps. This library includes only internal classes and is **NOT** part of the public API. The contents of this library are subject to change without notice.

## Overview

This common library provides cross-repository primitives including:

- **Command architecture** – TokenCommand, BrokerCommand, and controller pipelines.
- **OAuth2/OIDC protocol handling** – Request construction and response parsing.
- **Token cache** – Multi-artifact atomic writes, FOCI (Family of Client IDs) support, and authority normalization.
- **Cryptography utilities** – KeyStore integration, key wrapping, hashing, JWE/JWS support.
- **Telemetry** – OpenTelemetry-based span and attribute enums (SpanName, AttributeName).
- **IPC contracts** – Shared data models and bundle schemas for broker communication.
- **Authority & instance discovery** – Cloud instance validation and regional endpoint support.
- **Error taxonomy** – Structured error mapping and exception hierarchy.
- **Utilities** – Clock abstraction, JSON adapters, correlation IDs, thread pool helpers.

## Requirements

- **Android API level**: 21 (Android 5.0 Lollipop) and above.
- **Java**: Java 8 or higher.
- **Android Studio**: Latest stable release recommended (see [Android Studio Build Requirement](#android-studio-build-requirement) below).

## Project Structure

| Module | Description |
|---|---|
| `common` | Android-specific code (platform utilities, IPC, UI, cryptography). |
| `common4j` | Pure Java/Kotlin code shared across platforms (protocol, cache, telemetry). |
| `testutils` | Shared test utilities and helpers. |
| `uiautomationutilities` | UI automation utilities for end-to-end tests. |
| `LabApiUtilities` | Lab API client and constants for integration testing. |

## Building the Project

Clone the repository and build using the Gradle wrapper:

```bash
./gradlew assembleDebug
```

To run unit tests:

```bash
./gradlew test
```

### Issues
We encourage users of MSAL to file issues against the library that they are using rather than against common. This helps us understand the version of the common library in use based on the version of MSAL against which you report the issue. With that said, if you determine that the issue is indeed with common please go ahead and create it within this repo. Likewise if you have a suggestion, request and/or other feedback relative to common please file it here.

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

### Android Studio Build Requirement
Please note that this project uses [Lombok](https://projectlombok.org/) internally and while using Android Studio you will need to install the [Lombok Plugin](https://plugins.jetbrains.com/plugin/6317-lombok) to get the project to build successfully within Android Studio.
