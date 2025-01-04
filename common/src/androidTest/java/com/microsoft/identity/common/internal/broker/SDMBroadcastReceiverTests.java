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

import com.microsoft.identity.common.java.constants.SharedDeviceModeConstants;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class SDMBroadcastReceiverTests {

    String actualCallbackReceived;

    @Before
    public void setup() {
        SDMBroadcastReceiver.initialize(ApplicationProvider.getApplicationContext(), new SDMBroadcastReceiver.SharedDeviceModeCallback() {
            @Override
            public void onSharedDeviceModeRegistrationStarted() {
                actualCallbackReceived = SharedDeviceModeConstants.BROADCAST_TYPE_SDM_REGISTRATION_START;
            }

            @Override
            public void onSharedDeviceModeRegistered() {
                actualCallbackReceived = SharedDeviceModeConstants.BROADCAST_TYPE_SDM_REGISTERED;
            }

            @Override
            public void onGlobalSignOut() {
                actualCallbackReceived = SharedDeviceModeConstants.BROADCAST_TYPE_GLOBAL_SIGN_OUT;
            }
        });
    }
    @Test
    public void testSDMBroadcast() throws InterruptedException {
        sendBroadcast(SharedDeviceModeConstants.BROADCAST_TYPE_SDM_REGISTRATION_START);
        Thread.sleep(100);
        Assert.assertEquals(SharedDeviceModeConstants.BROADCAST_TYPE_SDM_REGISTRATION_START, actualCallbackReceived);

        sendBroadcast(SharedDeviceModeConstants.BROADCAST_TYPE_GLOBAL_SIGN_OUT);
        Thread.sleep(100);
        Assert.assertEquals(SharedDeviceModeConstants.BROADCAST_TYPE_GLOBAL_SIGN_OUT, actualCallbackReceived);
    }

    private void sendBroadcast(String broadcastType) {
        final Intent intent = new Intent();
        intent.setAction(SharedDeviceModeConstants.CURRENT_ACCOUNT_CHANGED_BROADCAST_IDENTIFIER);
        intent.putExtra(SharedDeviceModeConstants.BROADCAST_TYPE_KEY, broadcastType);
        ApplicationProvider.getApplicationContext().sendBroadcast(intent);
    }
}
