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
package com.microsoft.identity.common.internal.broker.ipc;

import android.os.Build;

import androidx.test.core.app.ApplicationProvider;

import com.microsoft.identity.common.internal.ipc.IpcStrategyTests;
import com.microsoft.identity.common.internal.ipc.mock.ShadowContentResolverConnectionFailed;
import com.microsoft.identity.common.internal.ipc.mock.ShadowContentResolverWithSuccessResult;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static com.microsoft.identity.common.internal.broker.ipc.BrokerOperationBundle.Operation.BROKER_GET_KEY_FROM_INACTIVE_BROKER;
import static com.microsoft.identity.common.internal.broker.ipc.BrokerOperationBundle.Operation.MSAL_ACQUIRE_TOKEN_SILENT;
import static com.microsoft.identity.common.internal.broker.ipc.BrokerOperationBundle.Operation.MSAL_GET_ACCOUNTS;
import static com.microsoft.identity.common.internal.broker.ipc.BrokerOperationBundle.Operation.MSAL_GET_CURRENT_ACCOUNT_IN_SHARED_DEVICE;
import static com.microsoft.identity.common.internal.broker.ipc.BrokerOperationBundle.Operation.MSAL_GET_DEVICE_MODE;
import static com.microsoft.identity.common.internal.broker.ipc.BrokerOperationBundle.Operation.MSAL_GET_INTENT_FOR_INTERACTIVE_REQUEST;
import static com.microsoft.identity.common.internal.broker.ipc.BrokerOperationBundle.Operation.MSAL_HELLO;
import static com.microsoft.identity.common.internal.broker.ipc.BrokerOperationBundle.Operation.MSAL_REMOVE_ACCOUNT;
import static com.microsoft.identity.common.internal.broker.ipc.BrokerOperationBundle.Operation.MSAL_SIGN_OUT_FROM_SHARED_DEVICE;

import lombok.NonNull;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {Build.VERSION_CODES.N}, shadows = {ShadowContentResolverWithSuccessResult.class})
public class ContentProviderStrategyTests extends IpcStrategyTests {

    static class MockContentProviderStatusLoader implements IContentProviderStatusLoader {
        @Override
        public boolean getStatus(@NonNull String packageName) {
            return true;
        }
    }

    @Override
    protected IIpcStrategy getStrategy() {
        return new ContentProviderStrategy(ApplicationProvider.getApplicationContext(),
                new MockContentProviderStatusLoader(),
                true);
    }

    @Test
    @Override
    public void testMsalHello() {
        testOperationSucceeds(getMockRequestBundle(MSAL_HELLO));
    }

    @Test
    @Override
    public void testMsalGetIntentForInteractiveRequest() {
        testOperationSucceeds(getMockRequestBundle(MSAL_GET_INTENT_FOR_INTERACTIVE_REQUEST));
    }

    @Test
    @Override
    public void testMsalAcquireTokenSilent() {
        testOperationSucceeds(getMockRequestBundle(MSAL_ACQUIRE_TOKEN_SILENT));
    }

    @Test
    @Override
    public void testMsalGetAccounts() {
        testOperationSucceeds(getMockRequestBundle(MSAL_GET_ACCOUNTS));
    }

    @Test
    @Override
    public void testMsalRemoveAccounts() {
        testOperationSucceeds(getMockRequestBundle(MSAL_REMOVE_ACCOUNT));
    }

    @Test
    @Override
    public void testMsalGetDeviceMode() {
        testOperationSucceeds(getMockRequestBundle(MSAL_GET_DEVICE_MODE));
    }

    @Test
    @Override
    public void testGetCurrentAccountInSharedDevice() {
        testOperationSucceeds(getMockRequestBundle(MSAL_GET_CURRENT_ACCOUNT_IN_SHARED_DEVICE));
    }

    @Test
    @Override
    public void testMsalSignOutFromSharedDevice() {
        testOperationSucceeds(getMockRequestBundle(MSAL_SIGN_OUT_FROM_SHARED_DEVICE));
    }

    @Test
    @Override
    public void testBrokerGetKeyFromInactiveBroker() {
        testOperationNotSupportedOnClientSide(getMockRequestBundle(BROKER_GET_KEY_FROM_INACTIVE_BROKER));
    }

    @Test
    @Override
    @Config(shadows = {ShadowContentResolverConnectionFailed.class})
    public void testIpcFailed() {
        testIpcConnectionFailed(getMockRequestBundle(MSAL_HELLO));
    }
}
