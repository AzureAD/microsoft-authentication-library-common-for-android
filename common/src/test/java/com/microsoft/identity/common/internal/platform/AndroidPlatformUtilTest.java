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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import com.microsoft.identity.common.java.constants.FidoConstants;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.exception.ErrorStrings;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowPackageManager;

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

    // ---- validateCallingAppForUid (AB#3687466): real getPackagesForUid membership ---------------

    private static final int OWNER_UID = 20001;
    private static final int UNMAPPED_UID = 99999;
    private static final String OWNED_PACKAGE = "com.test.callerapp";
    private static final String COMPANION_PACKAGE = "com.test.callerapp.companion";
    private static final String OTHER_PACKAGE = "com.microsoft.emmx";

    private AndroidPlatformUtil platformUtil() {
        return new AndroidPlatformUtil(ApplicationProvider.getApplicationContext(), null);
    }

    private ShadowPackageManager shadowPackageManager() {
        final Context context = ApplicationProvider.getApplicationContext();
        return Shadows.shadowOf(context.getPackageManager());
    }

    private void assertUnknownCaller(final int callingUid, final String callerPackageName) {
        try {
            platformUtil().validateCallingAppForUid(callingUid, callerPackageName);
            fail("Expected ClientException(UNKNOWN_CALLER) for uid=" + callingUid
                    + " caller=" + callerPackageName);
        } catch (final ClientException e) {
            assertEquals(ErrorStrings.UNKNOWN_CALLER, e.getErrorCode());
        }
    }

    @Test
    @Config(sdk = 28)
    public void validateCallingAppForUid_callerOwnedByUid_passes() throws ClientException {
        shadowPackageManager().setPackagesForUid(OWNER_UID, OWNED_PACKAGE);

        // The self-reported caller package is owned by the attested uid: no exception.
        platformUtil().validateCallingAppForUid(OWNER_UID, OWNED_PACKAGE);
    }

    @Test
    @Config(sdk = 28)
    public void validateCallingAppForUid_sharedUidCallerIsOneOfPackages_passes() throws ClientException {
        shadowPackageManager().setPackagesForUid(OWNER_UID, OWNED_PACKAGE, COMPANION_PACKAGE);

        // A shared-uid caller naming any package the uid owns is accepted.
        platformUtil().validateCallingAppForUid(OWNER_UID, COMPANION_PACKAGE);
    }

    @Test
    @Config(sdk = 28)
    public void validateCallingAppForUid_callerNotOwnedByUid_throwsUnknownCaller() {
        shadowPackageManager().setPackagesForUid(OWNER_UID, OWNED_PACKAGE);

        // The uid owns OWNED_PACKAGE, but the request self-reports a victim package it does not own.
        assertUnknownCaller(OWNER_UID, OTHER_PACKAGE);
    }

    @Test
    @Config(sdk = 28)
    public void validateCallingAppForUid_emptyCallerPackage_throwsUnknownCaller() {
        shadowPackageManager().setPackagesForUid(OWNER_UID, OWNED_PACKAGE);

        // An empty caller package is not owned by the uid: rejected fail-closed (no backfill).
        assertUnknownCaller(OWNER_UID, "");
    }

    @Test
    @Config(sdk = 28)
    public void validateCallingAppForUid_uidResolvesToNoPackage_throwsUnknownCaller() {
        // No packages mapped for the uid: getPackagesForUid returns null -> empty owned set -> fail closed.
        assertUnknownCaller(UNMAPPED_UID, OWNED_PACKAGE);
    }
}
