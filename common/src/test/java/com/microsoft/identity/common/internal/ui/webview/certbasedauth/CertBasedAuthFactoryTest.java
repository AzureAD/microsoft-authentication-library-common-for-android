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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.annotation.NonNull;
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

    private enum ExpectedChallengeHandler {
        USB,
        NFC,
        NULL
    }

    @Before
    public void factorySetUp() {
        mUsbManager = new TestUsbSmartcardCertBasedAuthManager(new ArrayList<>());
        mNfcManager = new TestNfcSmartcardCertBasedAuthManager(new ArrayList<>());
        mFactory = new CertBasedAuthFactory(mActivity, mUsbManager, mNfcManager, mDialogHolder);
    }

    @Test
    public void testInitiallyUsbConnected() {
        mUsbManager.mockConnect();
        challengeHandlerHelper(ExpectedChallengeHandler.USB);
    }

    @Test
    public void testCancelAtUserChoiceDialog() {
        challengeHandlerHelper(ExpectedChallengeHandler.NULL);
        checkIfCorrectDialogIsShowing(TestDialog.user_choice);
        final UserChoiceDialog.CancelCbaCallback callback = mDialogHolder.getUserChoiceCancelCbaCallback();
        assertNotNull(callback);
        callback.onCancel();
        checkIfCorrectDialogIsShowing(null);
    }

    @Test
    public void testPlugInAtUserChoiceDialog() {
        challengeHandlerHelper(ExpectedChallengeHandler.USB);
        checkIfCorrectDialogIsShowing(TestDialog.user_choice);
        mUsbManager.mockConnect();
        final UserChoiceDialog.PositiveButtonListener listener = mDialogHolder.getUserChoicePositiveButtonListener();
        assertNotNull(listener);
        listener.onClick(1);
    }

    @Test
    public void testCancelAtPromptDialog() {
        challengeHandlerHelper(ExpectedChallengeHandler.NULL);
        checkIfCorrectDialogIsShowing(TestDialog.user_choice);
        final UserChoiceDialog.PositiveButtonListener listener = mDialogHolder.getUserChoicePositiveButtonListener();
        assertNotNull(listener);
        listener.onClick(1);
        checkIfCorrectDialogIsShowing(TestDialog.prompt);
        final SmartcardPromptDialog.CancelCbaCallback callback = mDialogHolder.getPromptCancelCbaCallback();
        assertNotNull(callback);
        callback.onCancel();
        checkIfCorrectDialogIsShowing(null);
    }

    @Test
    public void testChooseSmartcardAndProceedWithUsb() {
        challengeHandlerHelper(ExpectedChallengeHandler.USB);
        checkIfCorrectDialogIsShowing(TestDialog.user_choice);
        final UserChoiceDialog.PositiveButtonListener listener = mDialogHolder.getUserChoicePositiveButtonListener();
        assertNotNull(listener);
        listener.onClick(1);
        checkIfCorrectDialogIsShowing(TestDialog.prompt);
        mUsbManager.mockConnect();
    }

    @Test
    public void testChooseSmartcardAndProceedWithNfc() {
        challengeHandlerHelper(ExpectedChallengeHandler.NFC);
        checkIfCorrectDialogIsShowing(TestDialog.user_choice);
        final UserChoiceDialog.PositiveButtonListener listener = mDialogHolder.getUserChoicePositiveButtonListener();
        assertNotNull(listener);
        listener.onClick(1);
        checkIfCorrectDialogIsShowing(TestDialog.prompt);
        mNfcManager.mockConnect(false);
        checkIfCorrectDialogIsShowing(TestDialog.nfc_loading);
    }

    private void challengeHandlerHelper(@NonNull final ExpectedChallengeHandler expectedChallengeHandler) {
        mFactory.createCertBasedAuthChallengeHandler(new CertBasedAuthFactory.CertBasedAuthChallengeHandlerCallback() {
            @Override
            public void onReceived(@Nullable ICertBasedAuthChallengeHandler challengeHandler) {
                switch (expectedChallengeHandler) {
                    case USB:
                        assertTrue(challengeHandler instanceof UsbSmartcardCertBasedAuthChallengeHandler);
                        break;
                    case NFC:
                        assertTrue(challengeHandler instanceof NfcSmartcardCertBasedAuthChallengeHandler);
                        break;
                    default:
                        assertNull(challengeHandler);
                        break;
                }
            }
        });
    }
}
