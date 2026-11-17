<!-- Copyright (c) Microsoft Corporation. All rights reserved. Licensed under the MIT License. -->

# Common Android: agent entry point

This repository contains shared authentication code, not the public MSAL API.
Changes can affect MSAL, Broker, and legacy ADAL consumers.

## Context and best practices

- Read [Copilot domain and review instructions](.github/copilot-instructions.md)
  for architecture, compatibility, logging, concurrency, and test conventions.
- For PR reviews, use the existing [code-review skill](.github/skills/code-review/SKILL.md)
  and its applicable references. Do not duplicate its rules.
- See the [README](README.md) for contribution and Android Studio guidance.
- `common/` contains Android/platform and IPC code; `common4j/` contains JVM
  code. `testutils/` and `uiautomationutilities/` support Android tests;
  `keyvault/`, `labapi/`, and `LabApiUtilities/` provide JVM/lab utilities.
- Use existing shared abstractions and preserve cache/IPC compatibility.
  Keep changes focused; coordinate changes to shared contracts with consumers.
  Preserve unrelated work and never commit credentials or test-account data.

## Setup and working directories

Use this repository's wrappers, not a globally installed Gradle. The root
[wrapper](gradle/wrapper/gradle-wrapper.properties) uses Gradle 8.11.1;
the JVM-only [wrapper](common-java-root/gradle/wrapper/gradle-wrapper.properties)
uses Gradle 8.1. Do not assume the two builds are interchangeable.

1. Use **JDK 17**, matching PR validation. Set `JAVA_HOME` for the current shell
   and ensure `java -version` and the wrapper's `--version` select the intended
   JDK. Java 8 compilation targets do not mean Gradle should run on Java 8.
2. For Android checks, install Android SDK platform 36 and the build tools
   required by [the version configuration](gradle/versions.gradle). Configure
   `ANDROID_HOME` or an untracked root `local.properties` containing `sdk.dir`.
   Android Studio builds also require the Lombok plugin described in the README.
3. Obtain Packaging Read access to the IdentityDivision feeds configured in
   [root settings](settings.gradle) and [JVM build configuration](common-java-root/build.gradle).
   Provision `ENV_VSTS_MVN_CRED_USERNAME` and `ENV_VSTS_MVN_CRED_ACCESSTOKEN`
   through approved local secret handling before running Gradle. Never put the
   token in a tracked file, command example, log, or PR.
4. Network/lab tests additionally need approved lab credentials and Native Auth
   configuration. CI provisions these through its variable groups and
   [automation-certificate setup](azure-pipelines/templates/steps/automation-cert.yml).
   Request access through the team rather than copying CI secret values.
   Keep local certificates/configuration outside the checkout.

Commands below use PowerShell on Windows. Run root commands from this
repository's root; run JVM-only commands from `common-java-root`.
On macOS/Linux use `./gradlew` in the corresponding directory, with JDK 17
and equivalent SDK/feed setup.

## Fast verification while editing

For Android code, from the repository root:

```powershell
.\gradlew.bat :common:assembleLocalDebug :common:testLocalDebugUnitTest :common:lintLocalDebug :common:spotbugsLocalDebug
```

For JVM-only changes, from `common-java-root`:

```powershell
.\gradlew.bat :common4j:assemble :common4j:test :common4j:spotbugsMain
```

For a focused test, add `--tests` to the test task, replacing this example
class with the class relevant to the change:

```powershell
.\gradlew.bat :common4j:test --tests "com.microsoft.identity.common.java.util.StringUtilTest"
```

Filtered tests are an iteration aid, not full pre-PR verification. JVM test
suites may include network flows; do not assume a JVM task is offline-safe.
SpotBugs requires assembled outputs. Do not silently omit it when assembly fails.

## Before pushing or opening a PR

Validate all applicable layers, not only the changed test class. For documentation
only, check relative links and `git diff --check`; app/device tests are not
required unless the change also affects executable behavior.

### Build, JVM/Robolectric tests, coverage, and static analysis

From the repository root:

```powershell
git diff --check
.\gradlew.bat :common:assembleLocal :common:jacocoTestReport -PcodeCoverageEnabled=true
.\gradlew.bat :common:lintLocalDebug :common:spotbugsLocalDebug
```

From `common-java-root`:

```powershell
.\gradlew.bat :common4j:assemble :common4j:jacocoTestReport :common4j:spotbugsMain -PcodeCoverageEnabled=true -Psugar=true
```

These report tasks run their underlying tests; missing coverage execution data
is not a pass. Full CI-equivalent runs require the configured `labSecret`,
`mockApiUrl`, and `nativeAuthConfigString` properties where used by the suites.
The commands above do not provision them. Use approved local configuration or
the existing [Android PR pipeline](azure-pipelines/pull-request-validation/common.yml)
and [JVM PR pipeline](azure-pipelines/pull-request-validation/common4j.yml).
Record unconfigured network/E2E coverage as not run, not verified.

For lab-utility changes, also run this from `common-java-root` after approved
lab setup:

```powershell
.\gradlew.bat :LabApiUtilities:assemble :LabApiUtilities:test :LabApiUtilities:spotbugsMain -Psugar=true
```

See [lab-utility CI](azure-pipelines/pull-request-validation/lab-utilities.yml).
For changes in other modules, discover their tasks and run their applicable
checks rather than treating Common's tasks as covering every module.

### Integration and instrumented/device tests

Android framework, IPC, cache, and lifecycle changes need relevant integration
and instrumented coverage in addition to JVM tests. From the repository root,
with an approved emulator/device visible to `adb devices`:

```powershell
.\gradlew.bat :common:connectedLocalDebugAndroidTest -Psugar=true "-PlabSecret=/data/local/tmp/LabAuth.pfx"
```

The device-path argument presumes an approved lab certificate has already been
provisioned on the test device; it is not a setup command. Use the existing
[instrumented PR pipeline](azure-pipelines/pull-request-validation/pr-common-instrumented.yml)
when local device/lab setup is unavailable. It supplies the emulator and lab
setup through the shared pipeline template. Do not queue credentialed or costly
device runs without approval.

### Consumer and UI E2E validation

Shared-contract, authentication-flow, or dependency changes can require consumer
builds/tests and cross-app UI E2E checks. Use
[consumer validation](azure-pipelines/pull-request-validation/build-consumers.yml),
which builds/tests MSAL and Broker against the selected Common change.
For UI/lab setup and available automation flavors, consult the
[Android Auth testing guide](https://github.com/AzureAD/android-complete/blob/master/docs/Automation/how_to_run_tests.md)
and [test-layer overview](https://github.com/AzureAD/android-complete/blob/master/docs/Automation/testing_overview.md).
Run approved CI/device scenarios for affected flows; a library build or consumer
unit-test pass does not demonstrate cross-app UI behavior.

## Failures and reporting

- Check `$LASTEXITCODE` immediately after each native command; stop on nonzero
  status. Do not let a later successful command hide an earlier failure.
- Use `.\gradlew.bat :common:tasks --all` at the root or
  `.\gradlew.bat :common4j:tasks --all` from `common-java-root` to check task
  availability. These commands still configure the build and need prerequisites.
- If settings fail with `unknown property 'project'` in the credential block,
  check that both feed environment variables were provisioned. The current
  fallback path can fail before dependency resolution; do not work around it
  by committing credentials or changing repository configuration.
- Feed 401/403 errors require access/credential correction, not skipped checks.
  Missing JDK/SDK/device/lab setup is an explicit blocker, not a successful run.
- Report commands, working directories, exit status, and every failed/not-run
  layer with its reason. Do not claim CI, device, or official readiness results
  from documentation or task discovery alone.
