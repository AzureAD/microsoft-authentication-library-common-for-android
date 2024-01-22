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
import com.microsoft.identity.labapi.utilities.BuildConfig;
import com.microsoft.identity.labapi.utilities.authentication.LabApiAuthenticationClient;
import com.microsoft.identity.labapi.utilities.client.ILabAccount;
import com.microsoft.identity.labapi.utilities.client.LabClient;
import com.microsoft.identity.labapi.utilities.client.LabQuery;
import com.microsoft.identity.labapi.utilities.constants.TempUserType;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.util.concurrent.TimeUnit;

/**
 * A Test Rule to load lab user for the provided query prior to executing the test case.
 */
public class LoadLabUserTestRule implements TestRule {

    private static final String TAG = LoadLabUserTestRule.class.getSimpleName();

    public static final long TEMP_USER_WAIT_TIME = TimeUnit.SECONDS.toMillis(35);

    private LabQuery query;
    private TempUserType tempUserType;

    protected LabClient mLabClient;
    protected ILabAccount mLabAccount;

    public LoadLabUserTestRule(@NonNull final LabQuery query) {
        this.query = query;
        final LabApiAuthenticationClient authenticationClient = new LabApiAuthenticationClient(
                BuildConfig.LAB_CLIENT_SECRET
        );
        mLabClient = new LabClient(authenticationClient);
    }

    public LoadLabUserTestRule(@NonNull final TempUserType tempUserType) {
        this.tempUserType = tempUserType;
        final LabApiAuthenticationClient authenticationClient = new LabApiAuthenticationClient(
                BuildConfig.LAB_CLIENT_SECRET
        );
        mLabClient = new LabClient(authenticationClient);
    }

    @Override
    public Statement apply(final Statement base, final Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                Logger.i(TAG, "Applying rule....");
                if (query != null) {
                    Logger.i(TAG, "Loading Existing User for Test..");
                    mLabAccount = mLabClient.getLabAccount(query);
                } else if (tempUserType != null) {
                    Logger.i(TAG, "Loading Temp User for Test....");
                    mLabAccount = mLabClient.createTempAccount(tempUserType);
                    try {
                        // temp user takes some time to actually being created even though it may be
                        // returned by the LAB API. Adding a wait here before we proceed with the test.
                        Thread.sleep(LabClient.TEMP_USER_WAIT_TIME);
                    } catch (final InterruptedException e) {
                        throw new AssertionError(e);
                    }
                } else {
                    throw new IllegalArgumentException("Both Lab User query and temp user type were null.");
                }

                base.evaluate();
            }
        };
    }

    public ILabAccount getLabAccount() {
        return mLabAccount;
    }

    public LabClient getLabClient() {
        return mLabClient;
    }

}
