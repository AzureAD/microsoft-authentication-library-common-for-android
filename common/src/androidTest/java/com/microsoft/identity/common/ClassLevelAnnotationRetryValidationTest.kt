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
package com.microsoft.identity.common

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.microsoft.identity.internal.testutils.annotations.RetryFlakyTest
import com.microsoft.identity.internal.testutils.rules.FlakyTestRetryRule
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test that validates [FlakyTestRetryRule] respects a **class-level** [RetryFlakyTest]
 * annotation when no method-level annotation is present.
 *
 * The class is annotated with `@RetryFlakyTest(retryCount = 2)` (up to 3 total attempts). The test
 * method itself carries **no** `@RetryFlakyTest` annotation, so retry must be driven entirely by the
 * class-level fallback in [FlakyTestRetryRule].
 *
 * Expected logcat when run:
 * ```
 * FlakyTestRetryRule: classLevelAnnotation_triggersRetry: attempt 1 of 3 failed with AssertionError
 * FlakyTestRetryRule: classLevelAnnotation_triggersRetry: attempt 2 of 3 failed with AssertionError
 * FlakyTestRetryRule: classLevelAnnotation_triggersRetry: passed on attempt 3 of 3
 * ```
 */
@RunWith(AndroidJUnit4::class)
@RetryFlakyTest(retryCount = 2)
class ClassLevelAnnotationRetryValidationTest {

    @get:Rule
    val flakyTestRetryRule = FlakyTestRetryRule()

    // Persists across the rule's retry attempts (the rule re-invokes on the same test instance).
    private var attempt = 0

    /**
     * Fails on attempts 1 and 2, then passes on attempt 3. No method-level [RetryFlakyTest]
     * annotation is present; retry must come from the class-level annotation. With `retryCount = 2`
     * (up to 3 attempts) the rule reaches the passing attempt, so the test is reported as PASSED —
     * proving that the class-level annotation fallback works.
     */
    @Test
    fun classLevelAnnotation_triggersRetry() {
        attempt++
        Assert.assertTrue(
            "Simulated flaky failure on attempt $attempt (designed to pass on attempt 3)",
            attempt >= 3
        )
    }
}
