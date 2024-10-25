//  Copyright (c) Microsoft Corporation.
//  All rights reserved.
//
//  This code is licensed under the MIT License.
//
//  Permission is hereby granted, free of charge, to any person obtaining a copy
//  of this software and associated documentation files(the "Software"), to deal
//  in the Software without restriction, including without limitation the rights
//  to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
//  copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions :
//
//  The above copyright notice and this permission notice shall be included in
//  all copies or substantial portions of the Software.
//
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//  THE SOFTWARE.
package com.microsoft.identity.internal.testutils.mocks;

import android.util.Base64;

import com.microsoft.identity.internal.testutils.TestConstants;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import java.nio.charset.Charset;
import java.security.SecureRandom;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class MockTokenCreator {

    private static final String NAME_CLAIM = "name";
    private static final String OBJECT_ID_CLAIM = "oid";
    private static final String PREFERRED_USERNAME_CLAIM = "preferred_username";
    private static final String TENANT_ID_CLAIM = "tid";
    private static final String VERSION_CLAIM = "ver";

    // mock token constants values
    public static final String MOCK_AUDIENCE_VALUE = "audience-for-testing";
    public static final String MOCK_TENANT_ID_VALUE = TestConstants.Authorities.AAD_MOCK_HTTP_RESPONSE_AUTHORITY_TENANT;
    public static final String MOCK_OBJECT_ID_VALUE = "99a1340e-0f35-4ac1-94ac-0837718f0b1f";
    public static final String MOCK_PREFERRED_USERNAME_VALUE = "test@test.onmicrosoft.com";
    public static final String MOCK_SUBJECT_VALUE = "TestSubject";
    public static final String MOCK_VERSION_VALUE = "2.0";
    public static final String MOCK_NAME_VALUE = "test";
    public static final String MOCK_UID_VALUE = "99a1340e-0f35-4ac1-94ac-0837718f0b1f";
    public static final String MOCK_UTID_VALUE = MOCK_TENANT_ID_VALUE;
    public static final String MOCK_ENCODING_UTF8_VALUE = "UTF-8";
    public static final String MOCK_ISSUER_PREFIX_VALUE = "https://test.authority/";
    public static final String MOCK_ISSUER_SUFFIX_VALUE = "/v2.0";
    public static final Pattern CLOUD_DISCOVERY_ENDPOINT_REGEX = Pattern.compile("^https:\\/\\/login.microsoftonline.com\\/common\\/discovery\\/instance\\?api-version=1.1\\&authorization_endpoint=https%3A%2F%2Flogin.microsoftonline.com%2Fcommon%2Foauth2%2Fv2.0%2Fauthorize$");
    public static final Pattern MOCK_TOKEN_URL_REGEX = Pattern.compile("https:\\/\\/login.microsoftonline.com\\/.*");
    public static final Pattern DEVICE_CODE_FLOW_AUTHORIZATION_REGEX = Pattern.compile("https:\\/\\/login.microsoftonline.com\\/common\\/oAuth2\\/v2.0\\/devicecode");

    private static String createMockToken(final String issuer,
                                          final String subject,
                                          final String audience,
                                          final Date issuedAt,
                                          final Date notBefore,
                                          final Date expiration,
                                          final Map<String, Object> extraClaims) {
        final SecureRandom random = new SecureRandom();
        final byte[] secret = new byte[32];
        random.nextBytes(secret);

        try {
            final JWSSigner signer = new MACSigner(secret);
            JWTClaimsSet.Builder claimsBuilder =
                    new JWTClaimsSet.Builder()
                            .issuer(issuer)
                            .subject(subject)
                            .audience(audience)
                            .issueTime(issuedAt)
                            .notBeforeTime(notBefore)
                            .expirationTime(expiration);

            if (null != extraClaims) {
                for (final Map.Entry<String, Object> claim : extraClaims.entrySet()) {
                    claimsBuilder = claimsBuilder.claim(claim.getKey(), claim.getValue());
                }
            }

            JWTClaimsSet claims = claimsBuilder.build();

            // Create the JWT
            final SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);

            // Sign it
            signedJWT.sign(signer);

            // Stringify it for testing
            return signedJWT.serialize();
        } catch (JOSEException e) {
            return null;
        }
    }

    private static String createMockIdToken(final String issuer,
                                            final String subject,
                                            final String audience,
                                            final String name,
                                            final String preferredName,
                                            final String objectId,
                                            final String tenantId,
                                            final String version,
                                            final Date issuedAt,
                                            final Date notBefore,
                                            final Date expiration) {
        return createMockToken(
                issuer,
                subject,
                audience,
                issuedAt,
                notBefore,
                expiration,
                new HashMap<String, Object>() {{
                    put(NAME_CLAIM, name);
                    put(PREFERRED_USERNAME_CLAIM, preferredName);
                    put(OBJECT_ID_CLAIM, objectId);
                    put(TENANT_ID_CLAIM, tenantId);
                    put(VERSION_CLAIM, version);
                }});
    }

    public static String createMockIdToken() {
        long exp = getExpirationTimeAfterSpecifiedTime(3600);
        return createMockIdTokenWithExpAndTenantId(exp, MOCK_TENANT_ID_VALUE);
    }

    public static String createMockIdTokenWithTenantId(final String tenantId) {
        long exp = getExpirationTimeAfterSpecifiedTime(3600);
        return createMockIdTokenWithExpAndTenantId(exp, tenantId);
    }

    public static String createMockIdTokenWithExpAndTenantId(long exp, final String tenantId) {
        final String issuer = MOCK_ISSUER_PREFIX_VALUE + tenantId + MOCK_ISSUER_SUFFIX_VALUE;
        return createMockIdToken(
                issuer,
                MOCK_SUBJECT_VALUE,
                MOCK_AUDIENCE_VALUE,
                MOCK_NAME_VALUE,
                MOCK_PREFERRED_USERNAME_VALUE,
                MOCK_OBJECT_ID_VALUE,
                tenantId,
                MOCK_VERSION_VALUE,
                new Date(),
                new Date(),
                new Date(exp)
        );
    }

    public static String createMockIdTokenWithObjectIdTenantIdAndIssuer(final String objectId, final String tenantId, final String issuer) {
        long exp = getExpirationTimeAfterSpecifiedTime(3600);
        return createMockIdToken(
                issuer,
                MOCK_SUBJECT_VALUE,
                MOCK_AUDIENCE_VALUE,
                MOCK_NAME_VALUE,
                MOCK_PREFERRED_USERNAME_VALUE,
                objectId,
                tenantId,
                MOCK_VERSION_VALUE,
                new Date(),
                new Date(),
                new Date(exp)
        );
    }

    public static String createMockRawClientInfo(final String uid, final String utid) {
        final String claims = "{\"uid\":\"" + uid + "\",\"utid\":\"" + utid + "\"}";

        return new String(Base64.encode(claims.getBytes(
                Charset.forName(MOCK_ENCODING_UTF8_VALUE)), Base64.NO_PADDING | Base64.NO_WRAP | Base64.URL_SAFE));
    }

    public static String createMockRawClientInfo() {
        return createMockRawClientInfo(MOCK_UID_VALUE, MOCK_UTID_VALUE);
    }

    public static long getExpirationTimeAfterSpecifiedTime(long numberOfSecondsAfterCurrentTime) {
        return System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(numberOfSecondsAfterCurrentTime);
    }
}
