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
package com.microsoft.identity.common.internal.ui.webview.certbasedauth;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.microsoft.identity.common.shadows.ShadowKeyChain;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.security.Principal;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = ShadowKeyChain.class)
public class OnDeviceCertBasedAuthChallengeHandlerTest extends AbstractCertBasedAuthTest  {

    @Before
    public void handlerSetUp() {
        mChallengeHandler = new OnDeviceCertBasedAuthChallengeHandler(mActivity, mTestCertBasedAuthTelemetryHelper);
    }
    private OnDeviceCertBasedAuthChallengeHandler mChallengeHandler;
    private final TestCertBasedAuthTelemetryHelper mTestCertBasedAuthTelemetryHelper = new TestCertBasedAuthTelemetryHelper();

    @Test
    public void processChallenge_noChoice() {
        // This test also checks for nullable keyTypes and principals.
        final TestClientCertRequest request = new TestClientCertRequest(null, null, null);
        mChallengeHandler.processChallenge(request);
        assertTrue(request.isCancelled());
        assertFalse(request.isProceeded());
    }

    @Test
    public void processChallenge_throwKeyChainException() {
        final TestClientCertRequest request = new TestClientCertRequest(new String[0], new Principal[0], ShadowKeyChain.KEY_CHAIN_EXCEPTION);
        mChallengeHandler.processChallenge(request);
        assertTrue(request.isCancelled());
        assertFalse(request.isProceeded());
    }

    @Test
    public void processChallenge_throwInterruptedException() {
        final TestClientCertRequest request = new TestClientCertRequest(new String[0], new Principal[0], ShadowKeyChain.INTERRUPTED_EXCEPTION);
        mChallengeHandler.processChallenge(request);
        assertTrue(request.isCancelled());
        assertFalse(request.isProceeded());
    }

    @Test
    public void processChallenge_proceed() {
        final TestClientCertRequest request = new TestClientCertRequest(new String[0], new Principal[0], ShadowKeyChain.PROCEED);
        mChallengeHandler.processChallenge(request);
        assertFalse(request.isCancelled());
        assertTrue(request.isProceeded());
    }
}
