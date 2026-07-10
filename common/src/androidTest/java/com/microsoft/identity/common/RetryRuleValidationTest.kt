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
 * Instrumented test that validates [FlakyTestRetryRule] + [RetryFlakyTest] actually re-run a failing
 * test and report success once a later attempt passes.
 *
 * This is a self-contained sanity check for the retry mechanism (it does not exercise any Common
 * production code). Expected logcat when run:
 * ```
 * FlakyTestRetryRule: passesOnThirdAttempt: attempt 1 of 4 failed with AssertionError
 * FlakyTestRetryRule: passesOnThirdAttempt: attempt 2 of 4 failed with AssertionError
 * FlakyTestRetryRule: passesOnThirdAttempt: passed on attempt 3 of 4
 * ```
 */
@RunWith(AndroidJUnit4::class)
class RetryRuleValidationTest {

    @get:Rule
    val flakyTestRetryRule = FlakyTestRetryRule()

    // Persists across the rule's retry attempts (the rule re-invokes on the same test instance).
    private var attempt = 0

    /**
     * Fails on attempts 1 and 2, then passes on attempt 3. With retryCount = 3 (up to 4 attempts)
     * the rule reaches the passing attempt, so the test is reported as PASSED — proving retry works.
     */
    @Test
    @RetryFlakyTest(retryCount = 3)
    fun passesOnThirdAttempt() {
        attempt++
        Assert.assertTrue(
            "Simulated flaky failure on attempt $attempt (designed to pass on attempt 3)",
            attempt >= 3
        )
    }
}
