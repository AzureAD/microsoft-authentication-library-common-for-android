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
package com.microsoft.identity.common.internal.platform;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.Activity;

import androidx.test.core.app.ApplicationProvider;

import com.microsoft.identity.common.java.constants.FidoConstants;
import com.microsoft.identity.common.java.util.IPlatformUtil;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RunWith(RobolectricTestRunner.class)
public class AndroidPlatformUtilTest {

    private final IPlatformUtil mPlatformUtil = new AndroidPlatformUtil(ApplicationProvider.getApplicationContext(), new Activity());
    private final Map.Entry<String, String> webauthnEntry = new AbstractMap.SimpleEntry<>(FidoConstants.WEBAUTHN_QUERY_PARAMETER_FIELD, FidoConstants.WEBAUTHN_QUERY_PARAMETER_VALUE);

    @Test
    @Config(sdk=28)
    public void testSetPlatformSpecificExtraQueryParameters_list() {
        final List <Map.Entry<String, String>> list = new ArrayList<>();
        list.add(new AbstractMap.SimpleEntry<>("foo", "1"));
        assertTrue(mPlatformUtil.updateWithAndGetPlatformSpecificExtraQueryParametersForBroker(list).contains(webauthnEntry));
    }

    @Test
    @Config(sdk=28)
    public void testSetPlatformSpecificExtraQueryParameters_emptyList() {
        assertTrue(mPlatformUtil.updateWithAndGetPlatformSpecificExtraQueryParametersForBroker(null).contains(webauthnEntry));
    }

    @Test
    @Config(sdk=28)
    public void testSetPlatformSpecificExtraQueryParameters_alreadyHasParameter() {
        final List <Map.Entry<String, String>> list = new ArrayList<>();
        list.add(new AbstractMap.SimpleEntry<>("foo", "1"));
        list.add(webauthnEntry);
        final List <Map.Entry<String, String>> result = mPlatformUtil.updateWithAndGetPlatformSpecificExtraQueryParametersForBroker(list);
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.contains(webauthnEntry));
        assertTrue(result.contains(new AbstractMap.SimpleEntry<>("foo", "1")));
    }

    @Test
    @Config(sdk=26)
    public void testSetPlatformSpecificExtraQueryParameters_OldOsVersion() {
        assertFalse(mPlatformUtil.updateWithAndGetPlatformSpecificExtraQueryParametersForBroker(null).contains(webauthnEntry));
    }
}
