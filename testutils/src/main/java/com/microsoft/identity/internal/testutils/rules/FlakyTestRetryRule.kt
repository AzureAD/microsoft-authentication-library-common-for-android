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
package com.microsoft.identity.internal.testutils.rules

import android.util.Log
import com.microsoft.identity.internal.testutils.annotations.RetryFlakyTest
import org.junit.AssumptionViolatedException
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * A JUnit4 [TestRule] that retries flaky **instrumented** (androidTest) tests on-device.
 *
 * > **Not to be confused with the UI-automation
 * > `com.microsoft.identity.client.ui.automation.rules.RetryTestRule`.** That rule requires an
 * > `ITestBroker` and is tied to PowerLift / `BuildConfig`, and it honours a *different*
 * > `RetryOnFailure` annotation. This rule is dependency-free and honours [RetryFlakyTest]; the
 * > distinct names make an incorrect import obvious rather than a silent no-op.
 *
 * Unlike the Gradle `org.gradle.test-retry` plugin — which only augments JVM/Robolectric unit test
 * tasks (`tasks.withType(Test)`) and does nothing for `connectedAndroidTest` — this rule runs inside
 * the test process, so it works both for local `connectedAndroidTest` runs and in CI / Firebase Test
 * Lab.
 *
 * Retry is **opt-in**: a test (or its class) must be annotated with [RetryFlakyTest] to be retried.
 * Tests without the annotation run exactly once. A method-level annotation takes precedence over a
 * class-level annotation. Tests skipped via a JUnit assumption ([AssumptionViolatedException], e.g.
 * `Assume.assumeTrue(...)`) are never retried.
 *
 * This rule is intentionally decoupled from the UI-automation framework (it does not depend on
 * `ITestBroker`, PowerLift, or any `BuildConfig` flags), so it can be applied to the plain
 * instrumented test suites in `common`, `msal`, and `broker`.
 *
 * Usage:
 * ```
 * @Rule public FlakyTestRetryRule flakyTestRetryRule = new FlakyTestRetryRule();
 *
 * @Test
 * @RetryFlakyTest(retryCount = 2) // up to 3 total attempts
 * public void someFlakyInstrumentedTest() { ... }
 * ```
 */
class FlakyTestRetryRule : TestRule {

    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            override fun evaluate() {
                // Prefer a method-level annotation; fall back to a class-level annotation.
                val retryFlakyTest = description.getAnnotation(RetryFlakyTest::class.java)
                    ?: description.testClass?.getAnnotation(RetryFlakyTest::class.java)

                if (retryFlakyTest == null) {
                    // Not marked as flaky — run exactly once with no retry.
                    base.evaluate()
                    return
                }

                val totalAttempts = 1 + retryFlakyTest.retryCount.coerceAtLeast(0)
                var lastError: Throwable? = null

                for (attempt in 1..totalAttempts) {
                    try {
                        base.evaluate()
                        if (attempt > 1) {
                            Log.i(
                                TAG,
                                "${description.methodName}: passed on attempt $attempt of $totalAttempts"
                            )
                        }
                        return
                    } catch (assumptionViolated: AssumptionViolatedException) {
                        // Honour JUnit assumptions — a skipped test must not be retried.
                        throw assumptionViolated
                    } catch (error: Throwable) {
                        lastError = error
                        Log.w(
                            TAG,
                            "${description.methodName}: attempt $attempt of $totalAttempts failed " +
                                "with ${error.javaClass.simpleName}",
                            error
                        )
                    }
                }

                Log.e(
                    TAG,
                    "${description.methodName}: giving up after $totalAttempts attempt(s); " +
                        "all attempts failed"
                )
                // Re-throw the last failure so the test is reported as failed.
                throw lastError ?: AssertionError(
                    "${description.methodName} failed after $totalAttempts attempt(s)"
                )
            }
        }
    }

    private companion object {
        private val TAG = FlakyTestRetryRule::class.java.simpleName
    }
}
