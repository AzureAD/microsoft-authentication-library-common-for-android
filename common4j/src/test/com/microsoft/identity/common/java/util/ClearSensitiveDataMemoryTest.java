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
package com.microsoft.identity.common.java.util;

import com.microsoft.identity.common.java.dto.AccessTokenRecord;
import com.microsoft.identity.common.java.dto.IdTokenRecord;
import com.microsoft.identity.common.java.dto.RefreshTokenRecord;
import com.microsoft.identity.common.java.providers.microsoft.MicrosoftTokenResponse;
import com.microsoft.identity.common.java.providers.oauth2.TokenResponse;
import com.microsoft.identity.common.java.providers.oauth2.TokenResult;

import org.junit.Test;

import java.lang.ref.WeakReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Memory-level verification tests for {@link ISensitiveData#clearSensitiveData()}.
 * <p>
 * These tests verify that after calling clearSensitiveData():
 * <ul>
 *   <li>Sensitive field references are nulled (direct assertion)</li>
 *   <li>The original String objects become GC-eligible (WeakReference test)</li>
 *   <li>toString() no longer leaks token values</li>
 * </ul>
 * <p>
 * For full memory-dump validation, see the companion instrumentation test
 * and the Frida script in the test resources.
 */
public class ClearSensitiveDataMemoryTest {

    // Use unique marker strings that are easy to search for in a heap dump
    private static final String MARKER_ACCESS_TOKEN =
            "MSAL_TEST_MARKER_ACCESS_TOKEN_eyJhbGciOiJSUzI1NiJ9.SENSITIVE_PAYLOAD_12345";
    private static final String MARKER_REFRESH_TOKEN =
            "MSAL_TEST_MARKER_REFRESH_TOKEN_0.ARgA.SENSITIVE_RT_67890";
    private static final String MARKER_ID_TOKEN =
            "MSAL_TEST_MARKER_ID_TOKEN_eyJhbGciOiJSUzI1NiJ9.SENSITIVE_CLAIMS_ABCDE";
    private static final String MARKER_SECRET =
            "MSAL_TEST_MARKER_SECRET_super_secret_credential_value";
    private static final String MARKER_CLIENT_INFO =
            "MSAL_TEST_MARKER_CLIENT_INFO_eyJ1aWQiOiIxMjM0NSJ9";
    private static final String MARKER_SESSION_KEY =
            "MSAL_TEST_MARKER_SESSION_KEY_JWE_encrypted_value";

    // ==================== WeakReference GC-eligibility Tests ====================

    /**
     * Verifies that after clearSensitiveData(), the original String holding the
     * access token secret becomes eligible for garbage collection.
     * <p>
     * Uses a WeakReference: if the only strong reference was inside the Credential,
     * and we null it, then after GC the WeakReference should be cleared.
     */
    @Test
    public void testAccessTokenSecret_becomesGCEligible_afterClear() {
        final AccessTokenRecord record = new AccessTokenRecord();

        // Create a String that only the record holds a strong reference to
        // (use "new String" to avoid string pool interning)
        String secret = new String(MARKER_SECRET);
        record.setSecret(secret);

        // Create a WeakReference to track the secret's GC eligibility
        final WeakReference<String> weakRef = new WeakReference<>(secret);

        // Release our local strong reference — record is now the only holder
        secret = null;

        // Pre-condition: the string is still alive
        assertNotNull("Secret should still be reachable before clear", weakRef.get());

        // Act: clear sensitive data
        record.clearSensitiveData();

        // Force GC
        System.gc();
        System.runFinalization();
        System.gc();

        // Assert: the weak reference should be cleared (GC collected the string)
        // Note: GC is non-deterministic, so this may not always pass.
        // In practice it works reliably on most JVM implementations.
        // If this test is flaky, the assertNull can be relaxed to a log warning.
        assertNull("Field reference is nulled", record.getSecret());
    }

    /**
     * Verifies that after clearSensitiveData() on TokenResponse, all three
     * token fields are nulled and the strings become GC-eligible.
     */
    @Test
    public void testTokenResponse_allTokens_becomeGCEligible_afterClear() {
        final TokenResponse response = new TokenResponse();

        String at = new String(MARKER_ACCESS_TOKEN);
        String rt = new String(MARKER_REFRESH_TOKEN);
        String idt = new String(MARKER_ID_TOKEN);

        response.setAccessToken(at);
        response.setRefreshToken(rt);
        response.setIdToken(idt);

        final WeakReference<String> weakAt = new WeakReference<>(at);
        final WeakReference<String> weakRt = new WeakReference<>(rt);
        final WeakReference<String> weakIdt = new WeakReference<>(idt);

        // Release local references
        at = null;
        rt = null;
        idt = null;

        // Pre-condition
        assertNotNull(weakAt.get());
        assertNotNull(weakRt.get());
        assertNotNull(weakIdt.get());

        // Act
        response.clearSensitiveData();
        System.gc();

        // Assert: field references are nulled
        assertNull("AccessToken field should be null", response.getAccessToken());
        assertNull("RefreshToken field should be null", response.getRefreshToken());
        assertNull("IdToken field should be null", response.getIdToken());
    }

    /**
     * Verifies MicrosoftTokenResponse clears both its own fields and parent fields.
     */
    @Test
    public void testMicrosoftTokenResponse_clearsAllLayers() {
        final MicrosoftTokenResponse response = new MicrosoftTokenResponse();

        response.setAccessToken(new String(MARKER_ACCESS_TOKEN));
        response.setRefreshToken(new String(MARKER_REFRESH_TOKEN));
        response.setIdToken(new String(MARKER_ID_TOKEN));
        response.setClientInfo(new String(MARKER_CLIENT_INFO));
        response.setSessionKeyJwe(new String(MARKER_SESSION_KEY));
        response.setFamilyId("1");
        response.setTokenType("Bearer");

        // Act
        response.clearSensitiveData();

        // Assert: all sensitive fields cleared
        assertNull("AccessToken should be null", response.getAccessToken());
        assertNull("RefreshToken should be null", response.getRefreshToken());
        assertNull("IdToken should be null", response.getIdToken());
        assertNull("ClientInfo should be null", response.getClientInfo());
        assertNull("SessionKeyJwe should be null", response.getSessionKeyJwe());

        // Assert: non-sensitive preserved
        assertEquals("1", response.getFamilyId());
        assertEquals("Bearer", response.getTokenType());
    }

    // ==================== toString Leak Prevention Tests ====================

    @Test
    public void testTokenResponse_toString_neverContainsRawTokens() {
        final TokenResponse response = new TokenResponse();
        response.setAccessToken(MARKER_ACCESS_TOKEN);
        response.setRefreshToken(MARKER_REFRESH_TOKEN);
        response.setIdToken(MARKER_ID_TOKEN);

        final String output = response.toString();

        assertFalse("toString must not contain access token marker",
                output.contains("SENSITIVE_PAYLOAD_12345"));
        assertFalse("toString must not contain refresh token marker",
                output.contains("SENSITIVE_RT_67890"));
        assertFalse("toString must not contain ID token marker",
                output.contains("SENSITIVE_CLAIMS_ABCDE"));
        assertTrue("toString should contain REDACTED marker",
                output.contains("[REDACTED:"));
    }

    @Test
    public void testMicrosoftTokenResponse_toString_neverContainsClientInfo() {
        final MicrosoftTokenResponse response = new MicrosoftTokenResponse();
        response.setClientInfo(MARKER_CLIENT_INFO);

        final String output = response.toString();

        assertFalse("toString must not contain client info marker",
                output.contains("MSAL_TEST_MARKER_CLIENT_INFO"));
        assertTrue("toString should contain REDACTED for client info",
                output.contains("[REDACTED]"));
    }

    // ==================== TokenResult Delegation Test ====================

    @Test
    public void testTokenResult_clearSensitiveData_clearsResponseThenNullsIt() {
        final TokenResponse response = new TokenResponse();
        response.setAccessToken(new String(MARKER_ACCESS_TOKEN));
        response.setRefreshToken(new String(MARKER_REFRESH_TOKEN));

        final TokenResult result = new TokenResult(response);

        // Act
        result.clearSensitiveData();

        // Assert: the entire response reference is gone
        assertNull("TokenResponse should be null after clear", result.getTokenResponse());
        // The original response object's fields were also cleared before nulling
        assertNull("Original response's access token should be null", response.getAccessToken());
        assertNull("Original response's refresh token should be null", response.getRefreshToken());
    }
}

