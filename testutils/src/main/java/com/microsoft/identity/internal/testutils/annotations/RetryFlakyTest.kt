//  Copyright (c) Microsoft Corporation.
//  All rights reserved.
//
//  This code is licensed under the MIT License.
//
//  Permission is hereby granted, free of charge, to any person obtaining a copy
//  of this software and associated documentation files(the "Software"), to deal
//  in the Software without restriction, including without limitation the rights
//  to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
//  copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions :
//
//  The above copyright notice and this permission notice shall be included in
//  all copies or substantial portions of the Software.
//
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//  THE SOFTWARE.
package com.microsoft.identity.internal.testutils.annotations

/**
 * Marks a flaky instrumented (androidTest) test method — or an entire test class — so that
 * [com.microsoft.identity.internal.testutils.rules.FlakyTestRetryRule] re-runs it up to [retryCount]
 * additional times before reporting a failure.
 *
 * > **Do not confuse this with the UI-automation
 * > `com.microsoft.identity.client.ui.automation.annotations.RetryOnFailure`.** That annotation is
 * > only honoured by the UI-automation `RetryTestRule` (which requires an `ITestBroker` and PowerLift).
 * > For the plain instrumented suites in `common`, `msal`, and `broker`, use **this** annotation
 * > together with [com.microsoft.identity.internal.testutils.rules.FlakyTestRetryRule]. The two were
 * > deliberately given distinct names so an incorrect import is obvious and never silently no-ops.
 *
 * Retry is **opt-in**: tests without this annotation run exactly once. Apply this annotation only to
 * tests that are known to be flaky, and continue tracking and fixing the underlying flakiness —
 * retrying is a mitigation, not a fix.
 *
 * The Gradle `org.gradle.test-retry` plugin only retries JVM/Robolectric unit tests
 * (`tasks.withType(Test)`); it does not apply to instrumented `connectedAndroidTest` tasks. This
 * annotation, together with [com.microsoft.identity.internal.testutils.rules.FlakyTestRetryRule],
 * provides the equivalent behaviour on-device (works both locally and in CI/Firebase Test Lab).
 *
 * > **Retries reuse the same test instance.** Unlike the Gradle `org.gradle.test-retry` plugin —
 * > which constructs a fresh test instance for each attempt — this rule re-invokes the test body on
 * > the *same* instance. `@Before`/`@After` still run on every attempt, but any field initialised at
 * > its declaration (rather than in `@Before`) is **not** reset between attempts and its mutated
 * > state carries over. Keep retried tests self-contained: reset mutable state in `@Before`, not at
 * > field declaration, so each attempt starts from a clean slate.
 *
 * A method-level annotation takes precedence over a class-level annotation.
 *
 * Usage (from a Java or Kotlin instrumented test):
 * ```
 * @Rule public FlakyTestRetryRule flakyTestRetryRule = new FlakyTestRetryRule();
 *
 * @Test
 * @RetryFlakyTest(retryCount = 2) // up to 3 total attempts
 * public void someFlakyInstrumentedTest() { ... }
 * ```
 *
 * @property retryCount the number of **additional** attempts to make after the first failure. The
 * total number of attempts is `1 + retryCount`. Defaults to `1` (i.e. two attempts in total). Values
 * less than `0` are treated as `0` (no retry).
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
annotation class RetryFlakyTest(val retryCount: Int = 1)
