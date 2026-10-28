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

import com.microsoft.identity.common.java.cache.CacheRecord;
import com.microsoft.identity.common.java.cache.ICacheRecord;
import com.microsoft.identity.common.java.dto.AccessTokenRecord;
import com.microsoft.identity.common.java.dto.AccountRecord;
import com.microsoft.identity.common.java.dto.IdTokenRecord;
import com.microsoft.identity.common.java.dto.PrimaryRefreshTokenRecord;
import com.microsoft.identity.common.java.dto.RefreshTokenRecord;
import com.microsoft.identity.common.java.providers.microsoft.MicrosoftRefreshToken;
import com.microsoft.identity.common.java.providers.microsoft.MicrosoftTokenResponse;
import com.microsoft.identity.common.java.providers.oauth2.AccessToken;
import com.microsoft.identity.common.java.providers.oauth2.IDToken;
import com.microsoft.identity.common.java.providers.oauth2.TokenResponse;
import com.microsoft.identity.common.java.providers.oauth2.TokenResult;
import com.microsoft.identity.common.java.request.SdkType;
import com.microsoft.identity.common.java.result.LocalAuthenticationResult;

import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link ISensitiveData#clearSensitiveData()} implementations across
 * token and credential classes. Verifies that sensitive fields are nulled out after
 * clearing and that non-sensitive metadata is preserved where appropriate.
 */
public class ClearSensitiveDataTest {

    private static final String FAKE_ACCESS_TOKEN = "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.fake_access_token_payload";
    private static final String FAKE_REFRESH_TOKEN = "0.ARgA.fake_refresh_token_value";
    // A minimal valid JWT (header.payload.signature) for IDToken parsing tests
    private static final String FAKE_ID_TOKEN =
            "eyJ0eXAiOiJKV1QiLCJhbGciOiJub25lIn0" +
            ".eyJzdWIiOiJ0ZXN0dXNlciIsIm5hbWUiOiJUZXN0IFVzZXIifQ" +
            ".";
    private static final String FAKE_SECRET = "super_secret_credential_value_12345";
    private static final String FAKE_HOME_ACCOUNT_ID = "uid.utid";
    private static final String FAKE_CLIENT_ID = "00000000-0000-0000-0000-000000000001";
    private static final String FAKE_CLIENT_INFO = "eyJ1aWQiOiIxMjM0NSIsInV0aWQiOiI2Nzg5MCJ9";
    private static final String FAKE_SESSION_KEY_JWE = "eyJhbGciOiJSU0EtT0FFUCIsImVuYyI6IkEyNTZHQ00ifQ.fake";
    private static final String FAKE_SESSION_KEY = "encrypted_session_key_value_67890";

    // ==================== Credential Tests ====================

    @Test
    public void testAccessTokenRecord_clearSensitiveData_nullsSecret() {
        final AccessTokenRecord record = new AccessTokenRecord();
        record.setSecret(FAKE_ACCESS_TOKEN);
        record.setHomeAccountId(FAKE_HOME_ACCOUNT_ID);
        record.setClientId(FAKE_CLIENT_ID);
        record.setRealm("contoso.com");
        record.setTarget("user.read openid");

        // Pre-condition: secret is set
        assertEquals(FAKE_ACCESS_TOKEN, record.getSecret());
        assertEquals(FAKE_HOME_ACCOUNT_ID, record.getHomeAccountId());

        // Act
        record.clearSensitiveData();

        // Assert: sensitive fields are nulled
        assertNull("Secret should be null after clearSensitiveData()", record.getSecret());
        assertNull("HomeAccountId should be null after clearSensitiveData()", record.getHomeAccountId());

        // Assert: non-sensitive metadata is preserved
        assertEquals("contoso.com", record.getRealm());
        assertEquals("user.read openid", record.getTarget());
        assertEquals(FAKE_CLIENT_ID, record.getClientId());
    }

    @Test
    public void testRefreshTokenRecord_clearSensitiveData_nullsSecret() {
        final RefreshTokenRecord record = new RefreshTokenRecord();
        record.setSecret(FAKE_REFRESH_TOKEN);
        record.setHomeAccountId(FAKE_HOME_ACCOUNT_ID);
        record.setFamilyId("1");

        // Pre-condition
        assertEquals(FAKE_REFRESH_TOKEN, record.getSecret());

        // Act
        record.clearSensitiveData();

        // Assert
        assertNull("Secret should be null after clearSensitiveData()", record.getSecret());
        assertNull("HomeAccountId should be null after clearSensitiveData()", record.getHomeAccountId());
    }

    @Test
    public void testIdTokenRecord_clearSensitiveData_nullsSecret() {
        final IdTokenRecord record = new IdTokenRecord();
        record.setSecret(FAKE_ID_TOKEN);
        record.setHomeAccountId(FAKE_HOME_ACCOUNT_ID);
        record.setRealm("contoso.com");

        // Act
        record.clearSensitiveData();

        // Assert
        assertNull("Secret should be null after clearSensitiveData()", record.getSecret());
        assertNull("HomeAccountId should be null after clearSensitiveData()", record.getHomeAccountId());
        // Non-sensitive metadata preserved
        assertEquals("contoso.com", record.getRealm());
    }

    @Test
    public void testCredential_clearSensitiveData_isIdempotent() {
        final AccessTokenRecord record = new AccessTokenRecord();
        record.setSecret(FAKE_ACCESS_TOKEN);

        // Call twice — should not throw
        record.clearSensitiveData();
        record.clearSensitiveData();

        assertNull(record.getSecret());
    }

    @Test
    public void testCredential_clearSensitiveData_handlesNullFields() {
        // Create record without setting any fields
        final AccessTokenRecord record = new AccessTokenRecord();

        // Should not throw NPE
        record.clearSensitiveData();

        assertNull(record.getSecret());
    }

    // ==================== TokenResponse Tests ====================

    @Test
    public void testTokenResponse_clearSensitiveData_nullsAllTokens() {
        final TokenResponse response = new TokenResponse();
        response.setAccessToken(FAKE_ACCESS_TOKEN);
        response.setRefreshToken(FAKE_REFRESH_TOKEN);
        response.setIdToken(FAKE_ID_TOKEN);
        response.setScope("user.read");
        response.setTokenType("Bearer");

        // Pre-condition
        assertEquals(FAKE_ACCESS_TOKEN, response.getAccessToken());
        assertEquals(FAKE_REFRESH_TOKEN, response.getRefreshToken());
        assertEquals(FAKE_ID_TOKEN, response.getIdToken());

        // Act
        response.clearSensitiveData();

        // Assert: tokens nulled
        assertNull("AccessToken should be null after clearSensitiveData()", response.getAccessToken());
        assertNull("RefreshToken should be null after clearSensitiveData()", response.getRefreshToken());
        assertNull("IdToken should be null after clearSensitiveData()", response.getIdToken());

        // Assert: non-sensitive metadata preserved
        assertEquals("user.read", response.getScope());
        assertEquals("Bearer", response.getTokenType());
    }

    @Test
    public void testTokenResponse_toString_redactsTokens() {
        final TokenResponse response = new TokenResponse();
        response.setAccessToken(FAKE_ACCESS_TOKEN);
        response.setRefreshToken(FAKE_REFRESH_TOKEN);
        response.setIdToken(FAKE_ID_TOKEN);

        final String toStringOutput = response.toString();

        // Assert: raw token values are NOT in toString output
        assertFalse("toString() should not contain raw access token",
                toStringOutput.contains(FAKE_ACCESS_TOKEN));
        assertFalse("toString() should not contain raw refresh token",
                toStringOutput.contains(FAKE_REFRESH_TOKEN));
        assertFalse("toString() should not contain raw ID token",
                toStringOutput.contains(FAKE_ID_TOKEN));

        // Assert: redaction markers ARE present
        assertTrue("toString() must contain redaction marker", toStringOutput.contains("[REDACTED:"));
    }

    @Test
    public void testTokenResponse_toString_handlesNullTokens() {
        final TokenResponse response = new TokenResponse();
        // Don't set any tokens — they're null

        final String toStringOutput = response.toString();

        // Should not throw and should contain "null"
        assertNotNull(toStringOutput);
        assertTrue("toString() must contain 'null' for unset tokens", toStringOutput.contains("null"));
    }

    // ==================== MicrosoftTokenResponse Tests ====================

    @Test
    public void testMicrosoftTokenResponse_clearSensitiveData_clearsParentAndSelf() {
        final MicrosoftTokenResponse response = new MicrosoftTokenResponse();
        response.setAccessToken(FAKE_ACCESS_TOKEN);
        response.setRefreshToken(FAKE_REFRESH_TOKEN);
        response.setIdToken(FAKE_ID_TOKEN);
        response.setClientInfo(FAKE_CLIENT_INFO);
        response.setSessionKeyJwe(FAKE_SESSION_KEY_JWE);
        response.setFamilyId("1");

        // Act
        response.clearSensitiveData();

        // Assert: parent fields cleared
        assertNull("AccessToken should be null", response.getAccessToken());
        assertNull("RefreshToken should be null", response.getRefreshToken());
        assertNull("IdToken should be null", response.getIdToken());

        // Assert: MicrosoftTokenResponse-specific sensitive fields cleared
        assertNull("ClientInfo should be null", response.getClientInfo());
        assertNull("SessionKeyJwe should be null", response.getSessionKeyJwe());

        // Assert: non-sensitive fields preserved
        assertEquals("1", response.getFamilyId());
    }

    @Test
    public void testMicrosoftTokenResponse_toString_redactsClientInfo() {
        final MicrosoftTokenResponse response = new MicrosoftTokenResponse();
        response.setClientInfo(FAKE_CLIENT_INFO);

        final String toStringOutput = response.toString();

        assertFalse("toString() should not contain raw client info",
                toStringOutput.contains(FAKE_CLIENT_INFO));
    }

    // ==================== TokenResult Tests ====================

    @Test
    public void testTokenResult_clearSensitiveData_clearsContainedResponse() {
        final TokenResponse response = new TokenResponse();
        response.setAccessToken(FAKE_ACCESS_TOKEN);
        response.setRefreshToken(FAKE_REFRESH_TOKEN);
        response.setIdToken(FAKE_ID_TOKEN);

        final TokenResult result = new TokenResult(response);

        // Pre-condition
        assertNotNull(result.getTokenResponse());
        assertEquals(FAKE_ACCESS_TOKEN, result.getTokenResponse().getAccessToken());

        // Act
        result.clearSensitiveData();

        // Assert: response reference is nulled
        assertNull("TokenResponse should be null after clearSensitiveData()",
                result.getTokenResponse());
        // Assert: delegated cleanup on the response object
        assertNull("Contained response accessToken should be null", response.getAccessToken());
        assertNull("Contained response refreshToken should be null", response.getRefreshToken());
        assertNull("Contained response idToken should be null", response.getIdToken());
    }

    @Test
    public void testTokenResult_clearSensitiveData_handlesNullResponse() {
        final TokenResult result = new TokenResult();

        // Should not throw NPE
        result.clearSensitiveData();

        assertNull(result.getTokenResponse());
    }

    @Test
    public void testTokenResult_clearSensitiveData_preservesErrorResponse() {
        final TokenResult result = new TokenResult();
        result.setSuccess(false);

        // Should not throw
        result.clearSensitiveData();

        // Error response is not sensitive — verify it's still accessible
        assertFalse(result.getSuccess());
    }

    // ==================== PrimaryRefreshTokenRecord Tests ====================

    @Test
    public void testPrimaryRefreshTokenRecord_clearSensitiveData_nullsSessionKey() {
        final PrimaryRefreshTokenRecord record = new PrimaryRefreshTokenRecord();
        record.setSecret(FAKE_SECRET);
        record.setHomeAccountId(FAKE_HOME_ACCOUNT_ID);
        record.setSessionKey(FAKE_SESSION_KEY);
        record.setSessionKeyRollingDate("1700000000");
        record.setFamilyId("1");

        record.clearSensitiveData();

        assertNull("Secret should be null after clearSensitiveData()",
                record.getSecret());
        assertNull("HomeAccountId should be null after clearSensitiveData()",
                record.getHomeAccountId());
        assertNull("SessionKey should be null after clearSensitiveData()",
                record.getSessionKey());
        assertNull("SessionKeyRollingDate should be null after clearSensitiveData()",
                record.getSessionKeyRollingDate());
        // Non-sensitive metadata should be preserved
        assertEquals("FamilyId should be preserved", "1", record.getFamilyId());
    }

    @Test
    public void testPrimaryRefreshTokenRecord_toString_redactsSessionKey() {
        final PrimaryRefreshTokenRecord record = new PrimaryRefreshTokenRecord();
        record.setSessionKey(FAKE_SESSION_KEY);

        final String str = record.toString();
        assertFalse("toString() should not contain the session key value",
                str.contains(FAKE_SESSION_KEY));
        assertTrue("toString() must contain redaction marker", str.contains("[REDACTED:"));
    }

    // ==================== LocalAuthenticationResult Tests ====================

    @Test
    public void testLocalAuthenticationResult_clearSensitiveData_releasesAllTokens() {
        final AccessTokenRecord accessTokenRecord = new AccessTokenRecord();
        accessTokenRecord.setSecret(FAKE_ACCESS_TOKEN);
        accessTokenRecord.setExpiresOn("1700000000");
        accessTokenRecord.setRealm("tenant1");
        accessTokenRecord.setTarget("user.read");
        accessTokenRecord.setHomeAccountId(FAKE_HOME_ACCOUNT_ID);

        final RefreshTokenRecord refreshTokenRecord = new RefreshTokenRecord();
        refreshTokenRecord.setSecret(FAKE_REFRESH_TOKEN);

        final IdTokenRecord idTokenRecord = new IdTokenRecord();
        idTokenRecord.setSecret(FAKE_ID_TOKEN);

        final AccountRecord accountRecord = new AccountRecord();
        accountRecord.setHomeAccountId(FAKE_HOME_ACCOUNT_ID);

        final ICacheRecord cacheRecord = CacheRecord.builder()
                .account(accountRecord)
                .accessToken(accessTokenRecord)
                .refreshToken(refreshTokenRecord)
                .idToken(idTokenRecord)
                .build();

        final List<ICacheRecord> cacheRecordList = Collections.singletonList(cacheRecord);

        final LocalAuthenticationResult result = new LocalAuthenticationResult(
                cacheRecord,
                cacheRecordList,
                SdkType.MSAL,
                false
        );

        // Pre-conditions
        assertEquals(FAKE_ACCESS_TOKEN, result.getAccessToken());
        assertEquals(FAKE_ID_TOKEN, result.getIdToken());
        assertEquals(FAKE_REFRESH_TOKEN, result.getRefreshToken());
        assertNotNull(result.getAccessTokenRecord());
        assertNotNull(result.getCacheRecordWithTenantProfileData());

        // Act: clear sensitive data
        result.clearSensitiveData();

        // Assert: all token fields and references are released
        assertNull("AccessToken should be null", result.getAccessToken());
        assertNull("IdToken should be null", result.getIdToken());
        assertNull("RefreshToken should be null", result.getRefreshToken());
        assertNull("AccessTokenRecord reference should be null", result.getAccessTokenRecord());
        assertNull("CacheRecord list reference should be null", result.getCacheRecordWithTenantProfileData());

        // Assert: idempotent — second clear does not throw
        result.clearSensitiveData();

        // Assert: external shared credential was NOT mutated
        assertEquals("External AccessTokenRecord should not be mutated",
                FAKE_ACCESS_TOKEN, accessTokenRecord.getSecret());
        assertEquals("External RefreshTokenRecord should not be mutated",
                FAKE_REFRESH_TOKEN, refreshTokenRecord.getSecret());
        assertEquals("External IdTokenRecord should not be mutated",
                FAKE_ID_TOKEN, idTokenRecord.getSecret());
    }

    // ==================== AccessToken (provider) Tests ====================

    @Test
    public void testAccessToken_clearSensitiveData_nullsRawToken() {
        final TokenResponse response = new TokenResponse();
        response.setAccessToken(FAKE_ACCESS_TOKEN);
        response.setTokenType("Bearer");
        response.setExpiresIn(3600L);

        final AccessToken token = new AccessToken(response);
        assertEquals(FAKE_ACCESS_TOKEN, token.getRawAccessToken());

        token.clearSensitiveData();

        assertNull("RawAccessToken should be null after clearSensitiveData()", token.getRawAccessToken());
        assertEquals("Bearer", token.getTokenType());
    }

    // ==================== RefreshToken (provider) Tests ====================

    @Test
    public void testMicrosoftRefreshToken_clearSensitiveData_nullsRawToken() {
        final MicrosoftTokenResponse tokenResponse = new MicrosoftTokenResponse();
        tokenResponse.setRefreshToken(FAKE_REFRESH_TOKEN);
        tokenResponse.setClientInfo(FAKE_CLIENT_INFO);
        tokenResponse.setScope("user.read");
        tokenResponse.setClientId(FAKE_CLIENT_ID);

        final MicrosoftRefreshToken rt = new MicrosoftRefreshToken(tokenResponse);
        // Pre-conditions
        assertEquals(FAKE_REFRESH_TOKEN, rt.getRefreshToken());
        assertEquals(FAKE_REFRESH_TOKEN, rt.getSecret());
        assertNotNull(rt.getClientInfo());
        assertEquals("12345.67890", rt.getHomeAccountId());

        // Act
        rt.clearSensitiveData();

        // Assert: sensitive fields nulled
        assertNull("RawRefreshToken should be null after clearSensitiveData()", rt.getRefreshToken());
        assertNull("Secret should be null after clearSensitiveData()", rt.getSecret());
        assertNull("ClientInfo should be null after clearSensitiveData()", rt.getClientInfo());
        assertNull("HomeAccountId should be null after clearSensitiveData()", rt.getHomeAccountId());

        // Assert: non-sensitive metadata preserved
        assertEquals("user.read", rt.getTarget());
        assertEquals(FAKE_CLIENT_ID, rt.getClientId());
    }

    // ==================== IDToken (provider) Tests ====================

    @Test
    public void testIDToken_clearSensitiveData_nullsRawToken() throws Exception {
        final IDToken idToken = new IDToken(FAKE_ID_TOKEN);
        assertEquals(FAKE_ID_TOKEN, idToken.getRawIDToken());

        idToken.clearSensitiveData();

        assertNull("RawIdToken should be null after clearSensitiveData()", idToken.getRawIDToken());
        assertNull("TokenClaims should be null after clearSensitiveData()", idToken.getStringClaim("sub"));
    }
}

