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

import androidx.test.core.app.ApplicationProvider;

import com.microsoft.identity.common.java.constants.DeviceRegistrationBroadcastConstants;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class DeviceRegistrationStateBroadcastReceiverTests {

    volatile String actualCallbackReceived;

    @Before
    public void setup() {
        DeviceRegistrationStateBroadcastReceiver.Companion.initialize(
                ApplicationProvider.getApplicationContext(),
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
                });
    }

    @After
    public void teardown() {
        DeviceRegistrationStateBroadcastReceiver.Companion.unregister(
                ApplicationProvider.getApplicationContext());
    }

    @Test
    public void testDeviceRegisteredBroadcast() throws InterruptedException {
        sendBroadcast(DeviceRegistrationBroadcastConstants.BROADCAST_TYPE_DEVICE_REGISTERED);
        Thread.sleep(100);
        Assert.assertEquals(DeviceRegistrationBroadcastConstants.BROADCAST_TYPE_DEVICE_REGISTERED, actualCallbackReceived);
    }

    @Test
    public void testDeviceUnregisteredBroadcast() throws InterruptedException {
        sendBroadcast(DeviceRegistrationBroadcastConstants.BROADCAST_TYPE_DEVICE_UNREGISTERED);
        Thread.sleep(100);
        Assert.assertEquals(DeviceRegistrationBroadcastConstants.BROADCAST_TYPE_DEVICE_UNREGISTERED, actualCallbackReceived);
    }

    @Test
    public void testDeviceRegistrationUpgradedBroadcast() throws InterruptedException {
        sendBroadcast(DeviceRegistrationBroadcastConstants.BROADCAST_TYPE_DEVICE_REGISTRATION_UPGRADED);
        Thread.sleep(100);
        Assert.assertEquals(DeviceRegistrationBroadcastConstants.BROADCAST_TYPE_DEVICE_REGISTRATION_UPGRADED, actualCallbackReceived);
    }

    @Test
    public void testDeviceStateChangedBroadcast() throws InterruptedException {
        sendBroadcast(DeviceRegistrationBroadcastConstants.BROADCAST_TYPE_DEVICE_STATE_CHANGED);
        Thread.sleep(100);
        Assert.assertEquals(DeviceRegistrationBroadcastConstants.BROADCAST_TYPE_DEVICE_STATE_CHANGED, actualCallbackReceived);
    }

    @Test
    public void testUnknownBroadcastTypeDoesNotCrash() throws InterruptedException {
        actualCallbackReceived = null;
        sendBroadcast("UNKNOWN_BROADCAST_TYPE");
        Thread.sleep(100);
        Assert.assertNull(actualCallbackReceived);
    }

    private void sendBroadcast(final String broadcastType) {
        final Intent intent = new Intent();
        intent.setAction(DeviceRegistrationBroadcastConstants.DEVICE_REGISTRATION_STATE_CHANGED_BROADCAST_IDENTIFIER);
        intent.putExtra(DeviceRegistrationBroadcastConstants.BROADCAST_TYPE_KEY, broadcastType);
        ApplicationProvider.getApplicationContext().sendBroadcast(intent);
    }
}
