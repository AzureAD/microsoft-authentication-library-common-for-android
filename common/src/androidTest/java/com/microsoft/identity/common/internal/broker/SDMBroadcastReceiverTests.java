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
package com.microsoft.identity.common.internal.broker;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.os.ParcelFileDescriptor;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.platform.app.InstrumentationRegistry;

import com.microsoft.identity.common.internal.activebrokerdiscovery.IBrokerDiscoveryClient;
import com.microsoft.identity.common.internal.activebrokerdiscovery.IBrokerDiscoveryClientTelemetryCallback;
import com.microsoft.identity.common.java.constants.SharedDeviceModeConstants;
import com.microsoft.identity.common.java.exception.ClientException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(JUnit4.class)
public class SDMBroadcastReceiverTests {
    private static final String[] BROADCAST_TYPES = {
            SharedDeviceModeConstants.BROADCAST_TYPE_SDM_REGISTRATION_START,
            SharedDeviceModeConstants.BROADCAST_TYPE_SDM_REGISTERED,
            SharedDeviceModeConstants.BROADCAST_TYPE_GLOBAL_SIGN_OUT
    };

    private Context mContext;
    private BrokerData mActiveBroker;
    private IBrokerDiscoveryClient mBrokerDiscoveryClient;

    @Before
    public void setup() {
        mContext = ApplicationProvider.getApplicationContext();
        mActiveBroker = new BrokerData(mContext.getPackageName(), "test-signature");
        mBrokerDiscoveryClient = new IBrokerDiscoveryClient() {
            @Override
            public BrokerData getActiveBroker(final boolean shouldSkipCache) {
                return mActiveBroker;
            }

            @Override
            public BrokerData getActiveBroker(
                    final boolean shouldSkipCache,
                    final IBrokerDiscoveryClientTelemetryCallback telemetryCallback) {
                return mActiveBroker;
            }

            @Override
            public BrokerData forceBrokerRediscovery(final BrokerData brokerCandidate)
                    throws ClientException {
                return mActiveBroker;
            }

            @Override
            public BrokerData getActiveBrokerWithInMemoryCache(
                    final IBrokerDiscoveryClientTelemetryCallback telemetryCallback) {
                return mActiveBroker;
            }
        };
    }

    @After
    public void tearDown() {
        SDMBroadcastReceiver.resetForTest();
    }

    @Test
    public void compatibilityMode_unpermittedSender_deliversAllBroadcastTypes() throws Exception {
        for (final String broadcastType : BROADCAST_TYPES) {
            final CallbackResult callbackResult = initialize(false);

            sendShellBroadcast(broadcastType);

            assertTrue(callbackResult.await(broadcastType));
        }
    }

    @Test
    public void compatibilityMode_permittedSender_deliversAllBroadcastTypes() throws Exception {
        for (final String broadcastType : BROADCAST_TYPES) {
            final CallbackResult callbackResult = initialize(false);

            sendAppBroadcast(broadcastType);

            assertTrue(callbackResult.await(broadcastType));
        }
    }

    @Test
    public void protectedMode_unpermittedSenderRejectsAllBroadcastTypes() throws Exception {
        for (final String broadcastType : BROADCAST_TYPES) {
            final CallbackResult callbackResult = initialize(true);

            sendShellBroadcast(broadcastType);

            assertFalse(callbackResult.awaitAny());
        }
    }

    @Test
    public void protectedMode_permittedSender_deliversAllBroadcastTypes() throws Exception {
        for (final String broadcastType : BROADCAST_TYPES) {
            final CallbackResult callbackResult = initialize(true);

            sendAppBroadcast(broadcastType);

            assertTrue(callbackResult.await(broadcastType));
        }
    }

    private CallbackResult initialize(final boolean protectedMode) {
        final CallbackResult callbackResult = new CallbackResult();
        SDMBroadcastReceiver.initialize(
                mContext,
                callbackResult,
                mBrokerDiscoveryClient,
                activeBroker -> protectedMode,
                new PackageHelper(mContext),
                (context, platformComponents) -> true
        );
        return callbackResult;
    }

    private void sendAppBroadcast(final String broadcastType) {
        final Intent intent = new Intent();
        intent.setAction(SharedDeviceModeConstants.CURRENT_ACCOUNT_CHANGED_BROADCAST_IDENTIFIER);
        intent.putExtra(SharedDeviceModeConstants.BROADCAST_TYPE_KEY, broadcastType);
        mContext.sendBroadcast(intent);
    }

    private void sendShellBroadcast(final String broadcastType) throws IOException {
        final Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        final String command = "am broadcast -a "
                + SharedDeviceModeConstants.CURRENT_ACCOUNT_CHANGED_BROADCAST_IDENTIFIER
                + " --es "
                + SharedDeviceModeConstants.BROADCAST_TYPE_KEY
                + " "
                + broadcastType;
        try (ParcelFileDescriptor descriptor =
                     instrumentation.getUiAutomation().executeShellCommand(command);
             FileInputStream inputStream =
                     new FileInputStream(descriptor.getFileDescriptor())) {
            while (inputStream.read() != -1) {
                // Wait for the shell command to complete.
            }
        }
    }

    private static final class CallbackResult
            implements SDMBroadcastReceiver.SharedDeviceModeCallback {
        private final AtomicReference<String> mReceivedType = new AtomicReference<>();
        private final CountDownLatch mLatch = new CountDownLatch(1);

        @Override
        public void onSharedDeviceModeRegistrationStarted() {
            record(SharedDeviceModeConstants.BROADCAST_TYPE_SDM_REGISTRATION_START);
        }

        @Override
        public void onSharedDeviceModeRegistered() {
            record(SharedDeviceModeConstants.BROADCAST_TYPE_SDM_REGISTERED);
        }

        @Override
        public void onGlobalSignOut() {
            record(SharedDeviceModeConstants.BROADCAST_TYPE_GLOBAL_SIGN_OUT);
        }

        private void record(final String broadcastType) {
            mReceivedType.set(broadcastType);
            mLatch.countDown();
        }

        private boolean await(final String expectedType) throws InterruptedException {
            return mLatch.await(5, TimeUnit.SECONDS)
                    && expectedType.equals(mReceivedType.get());
        }

        private boolean awaitAny() throws InterruptedException {
            return mLatch.await(2, TimeUnit.SECONDS);
        }
    }
}
