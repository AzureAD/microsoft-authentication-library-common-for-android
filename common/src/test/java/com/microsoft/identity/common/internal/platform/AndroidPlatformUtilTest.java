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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.microsoft.identity.common.java.constants.FidoConstants;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

@RunWith(RobolectricTestRunner.class)
public class AndroidPlatformUtilTest {

    final Map.Entry<String, String> webauthnParam = new AbstractMap.SimpleEntry<>(
    FidoConstants.WEBAUTHN_QUERY_PARAMETER_FIELD,
    FidoConstants.WEBAUTHN_QUERY_PARAMETER_VALUE
    );
    final ArrayList<Map.Entry<String, String>> emptyList = new ArrayList<>();
    final ArrayList<Map.Entry<String, String>> singleList = new ArrayList<>(Collections.singletonList(new AbstractMap.SimpleEntry<>("foo", "1")));
    final ArrayList<Map.Entry<String, String>> alreadyInList = new ArrayList<>(Collections.singletonList((webauthnParam)));

    @Test
    @Config(sdk = 28)
    public void testUpdateWithOrDeleteWebAuthnParam_emptyListWebAuthnCapable() {
        final ArrayList<Map.Entry<String, String>> list = AndroidPlatformUtil.updateWithOrDeleteWebAuthnParam(emptyList, true);
        assertTrue(list.contains(webauthnParam));
    }

    @Test
    @Config(sdk = 28)
    public void testUpdateWithOrDeleteWebAuthnParam_emptyListNotWebAuthnCapable() {
        final ArrayList<Map.Entry<String, String>> list = AndroidPlatformUtil.updateWithOrDeleteWebAuthnParam(emptyList, false);
        assertFalse(list.contains(webauthnParam));
    }

    @Test
    @Config(sdk = 28)
    public void testUpdateWithOrDeleteWebAuthnParam_singleListWebAuthnCapable() {
        final ArrayList<Map.Entry<String, String>> list = AndroidPlatformUtil.updateWithOrDeleteWebAuthnParam(singleList, true);
        assertTrue(list.contains(webauthnParam));
    }

    @Test
    @Config(sdk = 28)
    public void testUpdateWithOrDeleteWebAuthnParam_singleListNotWebAuthnCapable() {
        final ArrayList<Map.Entry<String, String>> list = AndroidPlatformUtil.updateWithOrDeleteWebAuthnParam(singleList, false);
        assertFalse(list.contains(webauthnParam));
    }

    @Test
    @Config(sdk = 28)
    public void testUpdateWithOrDeleteWebAuthnParam_alreadyInListWebAuthnCapable() {
        final ArrayList<Map.Entry<String, String>> list = AndroidPlatformUtil.updateWithOrDeleteWebAuthnParam(alreadyInList, true);
        assertTrue(list.contains(webauthnParam));
    }

    @Test
    @Config(sdk = 28)
    public void testUpdateWithOrDeleteWebAuthnParam_alreadyListNotWebAuthnCapable() {
        final ArrayList<Map.Entry<String, String>> list = AndroidPlatformUtil.updateWithOrDeleteWebAuthnParam(alreadyInList, false);
        // We don't remove, since the app could be doing the per-request option and manually adding the param.
        assertTrue(list.contains(webauthnParam));
    }

    @Test
    @Config(sdk = 26)
    public void testUpdateWithOrDeleteWebAuthnParam_emptyListWebAuthnCapableLowOs() {
        final ArrayList<Map.Entry<String, String>> list = AndroidPlatformUtil.updateWithOrDeleteWebAuthnParam(emptyList, true);
        assertFalse(list.contains(webauthnParam));
    }

    @Test
    @Config(sdk = 26)
    public void testUpdateWithOrDeleteWebAuthnParam_singleListWebAuthnCapableLowOs() {
        final ArrayList<Map.Entry<String, String>> list = AndroidPlatformUtil.updateWithOrDeleteWebAuthnParam(singleList, true);
        assertFalse(list.contains(webauthnParam));
    }

    @Test
    @Config(sdk = 26)
    public void testUpdateWithOrDeleteWebAuthnParam_alreadyInListWebAuthnCapableLowOs() {
        final ArrayList<Map.Entry<String, String>> list = AndroidPlatformUtil.updateWithOrDeleteWebAuthnParam(alreadyInList, true);
        assertFalse(list.contains(webauthnParam));
    }
}
