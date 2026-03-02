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

import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;

import com.microsoft.identity.common.java.constants.DeviceRegistrationBroadcastConstants;
import com.microsoft.identity.common.java.flighting.CommonFlight;
import com.microsoft.identity.common.java.flighting.CommonFlightsManager;
import com.microsoft.identity.common.java.flighting.IFlightsManager;
import com.microsoft.identity.common.java.flighting.IFlightsProvider;

import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowLooper;

@RunWith(RobolectricTestRunner.class)
public class DeviceRegistrationStateBroadcastReceiverTests {

    private String actualCallbackReceived;

    private final DeviceRegistrationStateBroadcastReceiver.DeviceRegistrationStateCallback mCallback =
            new DeviceRegistrationStateBroadcastReceiver.DeviceRegistrationStateCallback() {
                @Override
                public void onDeviceRegistered() {
                    actualCallbackReceived = DeviceRegistrationBroadcastConstants.BROADCAST_TYPE_DEVICE_REGISTERED;
                }

                @Override
                public void onDeviceUnregistered() {
                    actualCallbackReceived = DeviceRegistrationBroadcastConstants.BROADCAST_TYPE_DEVICE_UNREGISTERED;
                }

                @Override
                public void onDeviceRegistrationUpgraded() {
                    actualCallbackReceived = DeviceRegistrationBroadcastConstants.BROADCAST_TYPE_DEVICE_REGISTRATION_UPGRADED;
                }

                @Override
                public void onDeviceStateChanged() {
                    actualCallbackReceived = DeviceRegistrationBroadcastConstants.BROADCAST_TYPE_DEVICE_STATE_CHANGED;
                }
            };

    @Before
    public void setUp() {
        actualCallbackReceived = null;
        enableFeatureFlag();
        DeviceRegistrationStateBroadcastReceiver.initialize(
                ApplicationProvider.getApplicationContext(),
                mCallback
        );
    }

    @After
    public void tearDown() {
        DeviceRegistrationStateBroadcastReceiver.unregister(ApplicationProvider.getApplicationContext());
        CommonFlightsManager.INSTANCE.resetFlightsManager();
    }

    @Test
    public void testDeviceRegisteredBroadcast() {
        sendBroadcast(DeviceRegistrationBroadcastConstants.BROADCAST_TYPE_DEVICE_REGISTERED);
        ShadowLooper.idleMainLooper();
        Assert.assertEquals(DeviceRegistrationBroadcastConstants.BROADCAST_TYPE_DEVICE_REGISTERED, actualCallbackReceived);
    }

    @Test
    public void testDeviceUnregisteredBroadcast() {
        sendBroadcast(DeviceRegistrationBroadcastConstants.BROADCAST_TYPE_DEVICE_UNREGISTERED);
        ShadowLooper.idleMainLooper();
        Assert.assertEquals(DeviceRegistrationBroadcastConstants.BROADCAST_TYPE_DEVICE_UNREGISTERED, actualCallbackReceived);
    }

    @Test
    public void testDeviceRegistrationUpgradedBroadcast() {
        sendBroadcast(DeviceRegistrationBroadcastConstants.BROADCAST_TYPE_DEVICE_REGISTRATION_UPGRADED);
        ShadowLooper.idleMainLooper();
        Assert.assertEquals(DeviceRegistrationBroadcastConstants.BROADCAST_TYPE_DEVICE_REGISTRATION_UPGRADED, actualCallbackReceived);
    }

    @Test
    public void testDeviceStateChangedBroadcast() {
        sendBroadcast(DeviceRegistrationBroadcastConstants.BROADCAST_TYPE_DEVICE_STATE_CHANGED);
        ShadowLooper.idleMainLooper();
        Assert.assertEquals(DeviceRegistrationBroadcastConstants.BROADCAST_TYPE_DEVICE_STATE_CHANGED, actualCallbackReceived);
    }

    @Test
    public void testUnknownBroadcastTypeHandledGracefully() {
        sendBroadcast("UNKNOWN_TYPE");
        ShadowLooper.idleMainLooper();
        Assert.assertNull(actualCallbackReceived);
    }

    @Test
    public void testNullBroadcastTypeHandledGracefully() {
        final Intent intent = new Intent();
        intent.setAction(DeviceRegistrationBroadcastConstants.DEVICE_REGISTRATION_STATE_CHANGED_BROADCAST_IDENTIFIER);
        // Do not put a broadcast type extra - results in null
        ApplicationProvider.getApplicationContext().sendBroadcast(intent);
        ShadowLooper.idleMainLooper();
        Assert.assertNull(actualCallbackReceived);
    }

    @Test
    public void testReceiverNotRegisteredWhenFeatureFlagIsOff() {
        // Unregister the receiver set up in setUp() and reset the flight manager to defaults
        DeviceRegistrationStateBroadcastReceiver.unregister(ApplicationProvider.getApplicationContext());
        CommonFlightsManager.INSTANCE.resetFlightsManager();

        // Re-initialize without enabling the flight (default is false)
        DeviceRegistrationStateBroadcastReceiver.initialize(
                ApplicationProvider.getApplicationContext(),
                mCallback
        );

        // Send a broadcast - it should not be received since the receiver was never registered
        sendBroadcast(DeviceRegistrationBroadcastConstants.BROADCAST_TYPE_DEVICE_REGISTERED);
        ShadowLooper.idleMainLooper();
        Assert.assertNull(actualCallbackReceived);
    }

    private void enableFeatureFlag() {
        final IFlightsProvider mockFlightsProvider = Mockito.mock(IFlightsProvider.class);
        Mockito.when(mockFlightsProvider.isFlightEnabled(CommonFlight.ENABLE_DEVICE_REGISTRATION_STATE_BROADCAST))
                .thenReturn(true);

        final IFlightsManager anonymousFlightsManager = new IFlightsManager() {
            @Override
            public @NotNull IFlightsProvider getFlightsProvider(long waitForConfigsWithTimeoutInMs) {
                return mockFlightsProvider;
            }

            @Override
            public @NotNull IFlightsProvider getFlightsProviderForTenant(@NotNull String tenantId, long waitForConfigsWithTimeoutInMs) {
                return mockFlightsProvider;
            }

            @Override
            public @NotNull IFlightsProvider getFlightsProviderForTenant(@NotNull String tenantId) {
                return mockFlightsProvider;
            }

            @NonNull
            @Override
            public IFlightsProvider getFlightsProvider() {
                return mockFlightsProvider;
            }
        };

        CommonFlightsManager.INSTANCE.initializeCommonFlightsManager(anonymousFlightsManager);
    }

    private void sendBroadcast(final String broadcastType) {
        final Intent intent = new Intent();
        intent.setAction(DeviceRegistrationBroadcastConstants.DEVICE_REGISTRATION_STATE_CHANGED_BROADCAST_IDENTIFIER);
        intent.putExtra(DeviceRegistrationBroadcastConstants.BROADCAST_TYPE_KEY, broadcastType);
        ApplicationProvider.getApplicationContext().sendBroadcast(intent);
    }
}
