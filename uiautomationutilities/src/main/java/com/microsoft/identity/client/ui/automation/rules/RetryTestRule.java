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
package com.microsoft.identity.client.ui.automation.rules;

import com.microsoft.identity.client.ui.automation.BuildConfig;
import com.microsoft.identity.client.ui.automation.annotations.RetryOnFailure;
import com.microsoft.identity.client.ui.automation.annotations.StressTest;
import com.microsoft.identity.client.ui.automation.broker.ITestBroker;
import com.microsoft.identity.client.ui.automation.logging.Logger;
import com.microsoft.identity.client.ui.automation.powerlift.IPowerLiftIntegratedApp;

import org.junit.AssumptionViolatedException;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * A Test Rule to retry the test n number of times where n could be any number as denoted by the
 * {@link RetryOnFailure} annotation.
 */
public class RetryTestRule implements TestRule {

    private final static String TAG = RetryTestRule.class.getSimpleName();
    private final static int MINIMUM_NUMBER_OF_ATTEMPTS = BuildConfig.MINIMUM_TEST_ATTEMPTS;

    private ITestBroker mBroker;

    public RetryTestRule(final ITestBroker broker) {
        this.mBroker = broker;
    }

    @Override
    public Statement apply(final Statement base, final Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                Logger.i(TAG, "Applying rule....");
                Throwable caughtThrowable = null;
                int numAttempts = 1;

                // Stress tests must run exactly once: retrying them hides the very
                // failures (deadlocks, contention bugs) they exist to surface and
                // multiplies an already long runtime. Skip retries (and the minimum
                // attempts floor) when the test or its class is a @StressTest.
                final boolean isStressTest =
                        description.getAnnotation(StressTest.class) != null
                                || description.getTestClass().getAnnotation(StressTest.class) != null;

                if (isStressTest) {
                    Logger.i(TAG, "StressTest detected - running a single attempt with no retries");
                    base.evaluate();
                    return;
                }

                RetryOnFailure retryOnFailure = description.getAnnotation(RetryOnFailure.class);

                if (retryOnFailure == null) {
                    Logger.i(TAG, "Does not Received any retryOnFailure annotation..");
                    // if the test didn't have the RetryOnFailure annotation, then we see if the
                    // class had that annotation and we try to honor that
                    retryOnFailure = description.getTestClass().getAnnotation(RetryOnFailure.class);
                }

                if (retryOnFailure != null) {
                    final int retryCount = retryOnFailure.retryCount();
                    Logger.i(TAG, "Received retry count annotation with value: " + retryCount);
                    numAttempts += retryCount;
                }

                // If after evaluating annotation, we still have number of attempts less than minimum, increase number of attempts
                if (numAttempts < MINIMUM_NUMBER_OF_ATTEMPTS) {
                    numAttempts = MINIMUM_NUMBER_OF_ATTEMPTS;
                }

                for (int i = 0; i < numAttempts; i++) {
                    try {
                        Logger.i(TAG, "Executing attempt #" + (i + 1) + " of " + numAttempts);
                        base.evaluate();
                        Logger.i(TAG, "Attempt #" + (i + 1) + " has succeeded!!");
                        return;
                    } catch (final AssumptionViolatedException assumptionException) {
                        // Test assumptions not met - skip the test without retrying
                        Logger.i(TAG, "Test " + description.getMethodName() + " skipped due to assumption violation: " + assumptionException.getMessage());
                        throw assumptionException;
                    } catch (final Throwable throwable) {
                        caughtThrowable = throwable;
                        Logger.e(TAG, description.getMethodName() + ": Attempt " + (i + 1) +
                                " failed with " + throwable.getClass().getSimpleName(), throwable);
                    }
                }

                Logger.e(TAG, "Test " + description.getMethodName() +
                        " - Giving up after " + numAttempts + " attempts as all attempts have failed :(");

                assert caughtThrowable != null;

                // Create powerlift incident at the end of retry attempts
                if (mBroker != null && mBroker instanceof IPowerLiftIntegratedApp && BuildConfig.SEND_POWERLIFT_LOGS) {
                    PowerLiftIncidentRule.attemptPowerLiftIncident(caughtThrowable, (IPowerLiftIntegratedApp) mBroker);
                } else {
                    throw caughtThrowable;
                }
            }
        };
    }
}
