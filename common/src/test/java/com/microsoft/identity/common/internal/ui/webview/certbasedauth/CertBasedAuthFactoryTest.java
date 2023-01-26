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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import androidx.annotation.Nullable;

import com.microsoft.identity.common.shadows.ShadowCertBasedAuthTelemetryHelper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;

@RunWith(RobolectricTestRunner.class)
@Config(shadows={ShadowCertBasedAuthTelemetryHelper.class})
public class CertBasedAuthFactoryTest extends AbstractCertBasedAuthTest {
    private CertBasedAuthFactory mFactory;
    protected TestUsbSmartcardCertBasedAuthManager mUsbManager;
    protected TestNfcSmartcardCertBasedAuthManager mNfcManager;

    @Before
    public void factorySetUp() {
        mUsbManager = new TestUsbSmartcardCertBasedAuthManager(new ArrayList<>());
        mNfcManager = new TestNfcSmartcardCertBasedAuthManager(new ArrayList<>());
        mFactory = new CertBasedAuthFactory(mActivity, mUsbManager, mNfcManager, mDialogHolder);
    }

    @Test
    public void testInitiallyUsbConnected() {
        mUsbManager.mockConnect();
        mFactory.createCertBasedAuthChallengeHandler(new CertBasedAuthFactory.CertBasedAuthChallengeHandlerCallback() {
            @Override
            public void onReceived(@Nullable ICertBasedAuthChallengeHandler challengeHandler) {
                assertTrue(challengeHandler instanceof UsbSmartcardCertBasedAuthChallengeHandler);
            }
        });
    }

    @Test
    public void testCancelAtUserChoiceDialog() {
        mFactory.createCertBasedAuthChallengeHandler(new CertBasedAuthFactory.CertBasedAuthChallengeHandlerCallback() {
            @Override
            public void onReceived(@Nullable ICertBasedAuthChallengeHandler challengeHandler) {
                //nothing needed
            }
        });
        final UserChoiceDialog.CancelCbaCallback callback = mDialogHolder.getMUserChoiceCancelCbaCallback();
        assertNotNull(callback);
        callback.onCancel();
        checkIfCorrectDialogIsShowing(null);
    }

    @Test
    public void testCancelAtPromptDialog() {

    }

    @Test
    public void testChooseSmartcardAndProceedWithUsb() {

    }

    @Test
    public void testChooseSmartcardAndProceedWithNfc() {

    }
}
