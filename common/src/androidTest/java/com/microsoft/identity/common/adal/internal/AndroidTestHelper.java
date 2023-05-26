// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// This code is licensed under the MIT License.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files(the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions :
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.
package com.microsoft.identity.common.adal.internal;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.util.Base64;
import android.util.Log;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.platform.app.InstrumentationRegistry;

import com.microsoft.identity.common.adal.internal.net.HttpUrlConnectionFactory;
import com.microsoft.identity.common.internal.broker.PackageHelper;

import junit.framework.Assert;

import java.security.MessageDigest;
import java.util.Locale;

import static org.junit.Assert.assertTrue;

public class AndroidTestHelper {

    protected static final int REQUEST_TIME_OUT = 40000; // milliseconds

    private static final String TAG = "AndroidTestHelper";

    private byte[] mTestSignature;

    private String mTestTag;

    @SuppressLint("PackageManagerGetSignatures")
    public void setUp() throws Exception {
        System.setProperty(
                "dexmaker.dexcache",
                InstrumentationRegistry
                        .getInstrumentation()
                        .getTargetContext()
                        .getCacheDir()
                        .getPath()
        );

        System.setProperty(
                "org.mockito.android.target",
                ApplicationProvider
                        .getApplicationContext()
                        .getCacheDir()
                        .getPath()
        );

        // ADAL is set to this signature for now
        final Context context = InstrumentationRegistry.getInstrumentation().getContext();
        for (final Signature signature : PackageHelper.getSignatures(context)) {
            mTestSignature = signature.toByteArray();
            MessageDigest md = MessageDigest.getInstance("SHA-512");
            md.update(mTestSignature);
            mTestTag = Base64.encodeToString(md.digest(), Base64.DEFAULT).trim();
            break;
        }

        AuthenticationSettings.INSTANCE.setBrokerSignature(mTestTag);
        AuthenticationSettings.INSTANCE
                .setBrokerPackageName(AuthenticationConstants.Broker.COMPANY_PORTAL_APP_PACKAGE_NAME);
        Log.d(TAG, "mTestSignature is set");
    }

    public void tearDown() throws Exception {
        HttpUrlConnectionFactory.setMockedHttpUrlConnection(null);
    }

    public void assertThrowsException(final Class<? extends Exception> expected, String hasMessage,
                                      final ThrowableRunnable testCode) {
        try {
            testCode.run();
            Assert.fail("This is expecting an exception, but it was not thrown.");
        } catch (final Throwable result) {
            if (!expected.isInstance(result)) {
                Assert.fail("Exception was not correct");
            }

            if (hasMessage != null && !hasMessage.isEmpty()) {
                assertTrue("Message has the text " + result.getMessage(),
                        (result.getMessage().toLowerCase(Locale.US)
                                .contains(hasMessage.toLowerCase(Locale.US))));
            }
        }
    }

    public interface ThrowableRunnable {
        void run() throws Exception;
    }
}


