
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
package com.microsoft.identity.common.adal.internal.net;

import android.content.Context;
import android.net.ConnectivityManager;
import android.os.Build;

import androidx.test.core.app.ApplicationProvider;

import com.microsoft.identity.common.adal.internal.PowerManagerWrapperShadow;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
        PowerManagerWrapperShadow.class
})
public class DefaultConnectionServiceTest {

    protected Context mContext;

    @Before
    public void setup() {
        mContext = ApplicationProvider.getApplicationContext();
    }

    @Test
    public void testConnectionWhenConnected() {
        DefaultConnectionService defaultConnectionService = new DefaultConnectionService(mContext);

        assertTrue(defaultConnectionService.isConnectionAvailable());
    }

    @Test
    public void testConnectionWhenNoActiveNetwork() {
        ConnectivityManager connectivityManager = (ConnectivityManager) mContext
                .getSystemService(Context.CONNECTIVITY_SERVICE);

        shadowOf(connectivityManager).setActiveNetworkInfo(null);
        DefaultConnectionService defaultConnectionService = new DefaultConnectionService(mContext);

        assertFalse(defaultConnectionService.isConnectionAvailable());
    }

    @Test
    public void testNetworkDisabledFromOptimizations() {
        PowerManagerWrapperShadow.ignoringBatteryOptimizations = false;
        DefaultConnectionService defaultConnectionService = new DefaultConnectionService(mContext);
        ReflectionHelpers.setStaticField(Build.VERSION.class, "SDK_INT", Build.VERSION_CODES.M);

        assertTrue(defaultConnectionService.isNetworkDisabledFromOptimizations());
    }

    @Test
    public void testNetworkDisabledFromOptimizationsForAPIsBelow23() {
        ReflectionHelpers.setStaticField(Build.VERSION.class, "SDK_INT", Build.VERSION_CODES.LOLLIPOP_MR1);
        DefaultConnectionService defaultConnectionService = new DefaultConnectionService(mContext);

        assertFalse(defaultConnectionService.isNetworkDisabledFromOptimizations());
    }
}
