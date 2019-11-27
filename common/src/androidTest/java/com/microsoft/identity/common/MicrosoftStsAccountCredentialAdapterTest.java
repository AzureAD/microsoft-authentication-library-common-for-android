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
package com.microsoft.identity.common;

import android.util.Base64;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.microsoft.identity.common.internal.cache.MicrosoftStsAccountCredentialAdapter;
import com.microsoft.identity.common.internal.dto.AccessTokenRecord;
import com.microsoft.identity.common.internal.dto.AccountRecord;
import com.microsoft.identity.common.internal.dto.RefreshTokenRecord;
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsAccount;
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsAuthorizationRequest;
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsOAuth2Strategy;
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsTokenResponse;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.security.SecureRandom;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(AndroidJUnit4.class)
public class MicrosoftStsAccountCredentialAdapterTest {

    public static final String MOCK_ID_TOKEN_WITH_CLAIMS;

    private static final String MOCK_GIVEN_NAME = "John";
    private static final String MOCK_FAMILY_NAME = "Doe";
    private static final String MOCK_NAME = "John Doe";
    private static final String MOCK_MIDDLE_NAME = "Q";
    private static final String MOCK_PREFERRED_USERNAME = "jdoe";
    private static final String MOCK_OID = "1c1db626-0fcb-42bb-b39e-8e983dd92932";
    private static final String MOCK_TID = "7744ecc5-e130-4af1-ba81-749c395efc8c";
    private static final String MOCK_AUTHORITY = "https://sts.windows.net/0287f963-2d72-4363-9e3a-5705c5b0f031/";
    private static final String MOCK_ENVIRONMENT = "sts.windows.net";
    private static final String MOCK_UID = "mock_uid";
    private static final String MOCK_UTID = "mock_utid";
    private static final String MOCK_CLIENT_INFO = createRawClientInfo(MOCK_UID, MOCK_UTID);
    private static final String MOCK_SCOPE = "user.read openid profile offline_access";
    private static final String MOCK_FAMILY_ID = "1";
    private static final long MOCK_EXPIRES_IN = 3600L;
    private static final long MOCK_EXT_EXPIRES_IN = MOCK_EXPIRES_IN * 2;
    private static final Date MOCK_EXPIRES_ON = new GregorianCalendar() {{
        add(Calendar.SECOND, (int) MOCK_EXPIRES_IN);
    }}.getTime();

    static {
        String idTokenWithClaims;
        final SecureRandom random = new SecureRandom();
        final byte[] sharedSecret = new byte[32];
        random.nextBytes(sharedSecret);

        try {
            // Create HMAC signer
            final JWSSigner signer = new MACSigner(sharedSecret);

            // Create/populate claims for the JWT
            final JWTClaimsSet claimsSet =
                    new JWTClaimsSet.Builder()
                            .issuer(MOCK_AUTHORITY)
                            .claim("iat", 1521498950)
                            .claim("exp", 1553035656)
                            .audience("www.contoso.com")
                            .subject("fake.email@contoso.com")
                            .claim("oid", MOCK_OID)
                            .claim("preferred_username", MOCK_PREFERRED_USERNAME)
                            .claim("given_name", MOCK_GIVEN_NAME)
                            .claim("family_name", MOCK_FAMILY_NAME)
                            .claim("name", MOCK_NAME)
                            .claim("middle_name", MOCK_MIDDLE_NAME)
                            .build();

            // Create the JWT
            final SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claimsSet);

            // Sign it
            signedJWT.sign(signer);

            // Stringify it for testing
            idTokenWithClaims = signedJWT.serialize();

        } catch (JOSEException e) {
            e.printStackTrace();
            idTokenWithClaims = null;
        }

        MOCK_ID_TOKEN_WITH_CLAIMS = idTokenWithClaims;
    }

    @Mock
    MicrosoftStsOAuth2Strategy mockStrategy;

    @Mock
    MicrosoftStsAuthorizationRequest mockRequest;

    @Mock
    MicrosoftStsTokenResponse mockResponse;

    @Mock
    MicrosoftStsAccount mockAccount;

    private MicrosoftStsAccountCredentialAdapter mAccountCredentialAdapter;

    @Before
    public void setUp() throws MalformedURLException {
        MockitoAnnotations.initMocks(this);
        when(mockStrategy.createAccount(any(MicrosoftStsTokenResponse.class))).thenReturn(mockAccount);
        when(mockStrategy.getIssuerCacheIdentifier(mockRequest)).thenReturn(MOCK_ENVIRONMENT);
        when(mockStrategy.getIssuerCacheIdentifierFromTokenEndpoint()).thenReturn(MOCK_ENVIRONMENT);
        when(mockStrategy.getAuthorityFromTokenEndpoint()).thenReturn(MOCK_AUTHORITY);
        when(mockRequest.getAuthority()).thenReturn(new URL(MOCK_AUTHORITY));
        when(mockResponse.getIdToken()).thenReturn(MOCK_ID_TOKEN_WITH_CLAIMS);
        when(mockResponse.getClientInfo()).thenReturn(MOCK_CLIENT_INFO);
        when(mockResponse.getScope()).thenReturn(MOCK_SCOPE);
        when(mockAccount.getRealm()).thenReturn(MOCK_TID);
        when(mockAccount.getHomeAccountId()).thenReturn(MOCK_UID + "." + MOCK_UTID);
        when(mockAccount.getEnvironment()).thenReturn(MOCK_ENVIRONMENT);
        when(mockAccount.getRealm()).thenReturn(MOCK_TID);
        when(mockAccount.getLocalAccountId()).thenReturn(MOCK_OID);
        when(mockAccount.getUsername()).thenReturn(MOCK_PREFERRED_USERNAME);
        when(mockAccount.getAuthorityType()).thenReturn("MSSTS");
        when(mockAccount.getFirstName()).thenReturn(MOCK_GIVEN_NAME);
        when(mockAccount.getName()).thenReturn(MOCK_NAME);
        when(mockAccount.getMiddleName()).thenReturn(MOCK_MIDDLE_NAME);
        when(mockAccount.getFamilyName()).thenReturn(MOCK_FAMILY_NAME);
        when(mockAccount.getClientInfo()).thenReturn(MOCK_CLIENT_INFO);
        when(mockRequest.getScope()).thenReturn(MOCK_SCOPE);
        when(mockResponse.getExpiresIn()).thenReturn(MOCK_EXPIRES_IN);
        when(mockResponse.getExtExpiresIn()).thenReturn(MOCK_EXT_EXPIRES_IN);
        when(mockResponse.getFamilyId()).thenReturn(MOCK_FAMILY_ID);
        mAccountCredentialAdapter = new MicrosoftStsAccountCredentialAdapter();
    }

    @Test
    public void createAccount() {
        // This test is now basically a copy-constructor test
        final AccountRecord account = mAccountCredentialAdapter.createAccount(mockStrategy, mockRequest, mockResponse);
        assertNotNull(account);
        assertEquals(MOCK_UID + "." + MOCK_UTID, account.getHomeAccountId());
        assertEquals(MOCK_ENVIRONMENT, account.getEnvironment());
        assertEquals(MOCK_CLIENT_INFO, account.getClientInfo());
        assertEquals(MOCK_TID, account.getRealm());
        assertEquals(MOCK_OID, account.getLocalAccountId());
        assertEquals(MOCK_PREFERRED_USERNAME, account.getUsername());
        assertEquals("MSSTS", account.getAuthorityType());
        assertEquals(MOCK_GIVEN_NAME, account.getFirstName());
        assertEquals(MOCK_FAMILY_NAME, account.getFamilyName());
        assertEquals(MOCK_MIDDLE_NAME, account.getMiddleName());
        assertEquals(MOCK_NAME, account.getName());
    }

    @Test
    public void createAccessToken() {
        final AccessTokenRecord accessToken = mAccountCredentialAdapter.createAccessToken(mockStrategy, mockRequest, mockResponse);
        assertNotNull(accessToken);
        assertEquals(MOCK_SCOPE, accessToken.getTarget());
        assertNotNull(accessToken.getCachedAt());
        assertNotNull(accessToken.getExpiresOn());
        assertNotNull(accessToken.getExpiresOn());
        assertEquals(MOCK_TID, accessToken.getRealm());
        assertEquals(MOCK_AUTHORITY, accessToken.getAuthority());
        assertEquals(MOCK_ENVIRONMENT, accessToken.getEnvironment());
        assertNotNull(accessToken.getExtendedExpiresOn());
        assertEquals(MOCK_UID + "." + MOCK_UTID, accessToken.getHomeAccountId());
    }

    @Test
    public void createRefreshToken() {
        final RefreshTokenRecord refreshToken = mAccountCredentialAdapter.createRefreshToken(mockStrategy, mockRequest, mockResponse);
        assertNotNull(refreshToken);
        assertEquals(MOCK_SCOPE, refreshToken.getTarget());
        assertNotNull(refreshToken.getCachedAt());
        assertEquals(MOCK_FAMILY_ID, refreshToken.getFamilyId());
    }

    static String createRawClientInfo(final String uid, final String utid) {
        final String claims = "{\"uid\":\"" + uid + "\",\"utid\":\"" + utid + "\"}";

        return new String(Base64.encode(claims.getBytes(
                Charset.forName("UTF-8")), Base64.NO_PADDING | Base64.NO_WRAP | Base64.URL_SAFE));
    }

}
