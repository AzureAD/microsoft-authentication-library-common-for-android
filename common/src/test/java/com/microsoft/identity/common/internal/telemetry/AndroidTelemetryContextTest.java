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
package com.microsoft.identity.common.internal.telemetry;

import androidx.test.core.app.ApplicationProvider;

import com.microsoft.identity.common.java.telemetry.TelemetryEventStrings;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.concurrent.ConcurrentHashMap;

@RunWith(RobolectricTestRunner.class)
public class AndroidTelemetryContextTest {

    private AndroidTelemetryContext androidTelemetryContext;

    @Before
    public void setup() {
        androidTelemetryContext = new AndroidTelemetryContext(ApplicationProvider.getApplicationContext());
    }

    @Test
    public void testAndroidTelemetryContextApplicationProperties() {
        final ConcurrentHashMap<String, String> properties = androidTelemetryContext.getProperties();

        Assert.assertEquals("org.robolectric.default", properties.get(TelemetryEventStrings.App.NAME));
        Assert.assertEquals("org.robolectric.default", properties.get(TelemetryEventStrings.App.PACKAGE));
        Assert.assertEquals("0", properties.get(TelemetryEventStrings.App.BUILD));
    }
}
