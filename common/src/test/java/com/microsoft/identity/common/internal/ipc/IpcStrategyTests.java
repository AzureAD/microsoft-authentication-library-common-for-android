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
package com.microsoft.identity.common.internal.ipc;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;

import com.microsoft.identity.common.adal.internal.AuthenticationConstants;
import com.microsoft.identity.common.java.exception.BaseException;
import com.microsoft.identity.common.exception.BrokerCommunicationException;
import com.microsoft.identity.common.internal.broker.ipc.BrokerOperationBundle;
import com.microsoft.identity.common.internal.broker.ipc.IIpcStrategy;
import com.microsoft.identity.common.java.util.ported.ObjectUtils;

import org.junit.Assert;
import org.junit.Test;

import static com.microsoft.identity.common.exception.BrokerCommunicationException.Category.CONNECTION_ERROR;
import static com.microsoft.identity.common.exception.BrokerCommunicationException.Category.OPERATION_NOT_SUPPORTED_ON_CLIENT_SIDE;

/**
 * IMPORTANT: This class must cover EVERY {@link com.microsoft.identity.common.internal.broker.ipc.BrokerOperationBundle.Operation}.
 */
public abstract class IpcStrategyTests {

    protected abstract IIpcStrategy getStrategy();

    @Test
    public abstract void testMsalHello();

    @Test
    public abstract void testMsalGetIntentForInteractiveRequest();

    @Test
    public abstract void testMsalAcquireTokenSilent();

    @Test
    public abstract void testMsalGetAccounts();

    @Test
    public abstract void testMsalRemoveAccounts();

    @Test
    public abstract void testMsalGetDeviceMode();

    @Test
    public abstract void testGetCurrentAccountInSharedDevice();

    @Test
    public abstract void testMsalSignOutFromSharedDevice();

    @Test
    public abstract void testBrokerGetKeyFromInactiveBroker();

    @Test
    public abstract void testIpcFailed();

    protected BrokerOperationBundle getMockRequestBundle(final BrokerOperationBundle.Operation operation) {
        final Bundle bundle = new Bundle();
        bundle.putBoolean("REQUEST_BUNDLE", true);

        return new BrokerOperationBundle(operation, "MOCK_BROKER", bundle);
    }

    public static Bundle getMockIpcResultBundle() {
        final Bundle bundle = new Bundle();
        bundle.putBoolean("MOCK_SUCCESS", true);
        return bundle;
    }

    // Taken from PR: https://github.com/AzureAD/ad-accounts-for-android/pull/1298/files
    public static Intent getMockLegacyInteractiveRequestResultIntent() {
        final Intent intent = new Intent();
        intent.setPackage("MOCK_PACKAGE");
        intent.setClassName("MOCK_PACKAGE", "MOCK_CLASSNAME");
        intent.putExtra(AuthenticationConstants.Broker.BROKER_VERSION, AuthenticationConstants.Broker.BROKER_PROTOCOL_VERSION);
        intent.putExtra(AuthenticationConstants.Broker.CALLER_INFO_UID, 0);
        intent.putExtra(AuthenticationConstants.Broker.BROKER_DEVICE_MODE, false);
        return intent;
    }

    public static Intent getMockInteractiveRequestResultIntent() {
        final Intent intent = new Intent();
        intent.setPackage("MOCK_PACKAGE");
        intent.setClassName("MOCK_PACKAGE", "MOCK_CLASSNAME");
        intent.putExtras(getMockInteractiveRequestResultBundle());
        return intent;
    }

    public static Bundle getMockInteractiveRequestResultBundle() {
        final Bundle bundle = new Bundle();
        bundle.putString(AuthenticationConstants.Broker.BROKER_VERSION, AuthenticationConstants.Broker.BROKER_PROTOCOL_VERSION);
        bundle.putInt(AuthenticationConstants.Broker.CALLER_INFO_UID, 0);
        bundle.putBoolean(AuthenticationConstants.Broker.BROKER_DEVICE_MODE, false);
        bundle.putString(AuthenticationConstants.Broker.BROKER_PACKAGE_NAME, "MOCK_PACKAGE");
        bundle.putString(AuthenticationConstants.Broker.BROKER_ACTIVITY_NAME, "MOCK_CLASSNAME");
        return bundle;
    }

    protected void testOperationSucceeds(@NonNull final BrokerOperationBundle bundle) {
        testOperationSucceeds(bundle, getMockIpcResultBundle());
    }

    protected void testOperationSucceeds(@NonNull final BrokerOperationBundle bundle,
                                         @NonNull final Bundle expectedResultBundle) {
        final IIpcStrategy strategy = getStrategy();
        try {
            final Bundle resultBundle = strategy.communicateToBroker(bundle);
            Assert.assertNotNull(resultBundle);
            Assert.assertTrue(isBundleEqual(resultBundle, expectedResultBundle));
        } catch (BaseException e) {
            Assert.fail("Exception is not expected.");
        }
    }

    protected void testOperationNotSupportedOnClientSide(@NonNull final BrokerOperationBundle bundle) {
        final IIpcStrategy strategy = getStrategy();
        try {
            strategy.communicateToBroker(bundle);
            Assert.fail("Operation should fail.");
        } catch (BaseException e) {
            Assert.assertTrue(e instanceof BrokerCommunicationException);
            Assert.assertSame(((BrokerCommunicationException) e).getCategory(), OPERATION_NOT_SUPPORTED_ON_CLIENT_SIDE);
            Assert.assertSame(((BrokerCommunicationException) e).getStrategyType(), strategy.getType());
        }
    }

    protected void testIpcConnectionFailed(@NonNull final BrokerOperationBundle bundle) {
        final IIpcStrategy strategy = getStrategy();
        try {
            strategy.communicateToBroker(bundle);
            Assert.fail("Operation should fail.");
        } catch (BaseException e) {
            Assert.assertTrue(e instanceof BrokerCommunicationException);
            Assert.assertSame(((BrokerCommunicationException) e).getCategory(), CONNECTION_ERROR);
            Assert.assertSame(((BrokerCommunicationException) e).getStrategyType(), strategy.getType());
        }
    }

    public static boolean isBundleEqual(@NonNull final Bundle resultBundle,
                                        @NonNull final Bundle expectedBundle) {

        if (resultBundle.size() != expectedBundle.size()) {
            return false;
        }

        for (final String key : resultBundle.keySet()) {
            final Object objA = expectedBundle.get(key);
            final Object objB = resultBundle.get(key);

            if (objA instanceof Bundle && objB instanceof Bundle) {
                return isBundleEqual((Bundle) objA, (Bundle) objB);
            } else if (!ObjectUtils.equals(objA, objB)) {
                return false;
            }
        }

        return true;
    }
}
