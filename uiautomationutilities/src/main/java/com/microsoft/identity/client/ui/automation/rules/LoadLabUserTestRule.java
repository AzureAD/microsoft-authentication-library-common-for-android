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

import androidx.annotation.NonNull;

import com.microsoft.identity.client.ui.automation.logging.Logger;
import com.microsoft.identity.internal.testutils.labutils.LabUserHelper;
import com.microsoft.identity.internal.testutils.labutils.LabUserQuery;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.util.concurrent.TimeUnit;

/**
 * A Test Rule to load lab user for the provided query prior to executing the test case.
 */
public class LoadLabUserTestRule implements TestRule {

    private static final String TAG = LoadLabUserTestRule.class.getSimpleName();

    public static final long TEMP_USER_WAIT_TIME = TimeUnit.SECONDS.toMillis(20);

    private LabUserQuery query;
    private String tempUserType;

    private String upn;

    public LoadLabUserTestRule(@NonNull final LabUserQuery query) {
        this.query = query;
    }

    public LoadLabUserTestRule(@NonNull final String tempUserType) {
        this.tempUserType = tempUserType;
    }

    @Override
    public Statement apply(final Statement base, final Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                Logger.i(TAG, "Applying rule....");
                if (query != null) {
                    Logger.i(TAG, "Loading Existing User for Test..");
                    upn = LabUserHelper.loadUserForTest(query);
                } else if (tempUserType != null) {
                    Logger.i(TAG, "Loading Temp User for Test....");
                    upn = LabUserHelper.loadTempUser(tempUserType);
                    try {
                        // temp user takes some time to actually being created even though it may be
                        // returned by the LAB API. Adding a wait here before we proceed with the
                        // test.
                        Thread.sleep(TEMP_USER_WAIT_TIME);
                    } catch (final InterruptedException e) {
                        throw new AssertionError(e);
                    }
                } else {
                    throw new IllegalArgumentException(
                            "Both Lab User query and temp user type were null.");
                }

                base.evaluate();
            }
        };
    }

    public String getLabUserUpn() {
        return upn;
    }
}
