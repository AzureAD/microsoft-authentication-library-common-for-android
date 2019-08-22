package com.microsoft.identity.common.internal.testutils;

import android.util.Base64;

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

public class TokenCreator {

    private static final String NAME_CLAIM = "name";
    private static final String OBJECT_ID_CLAIM = "oid";
    private static final String PREFERRED_USERNAME_CLAIM = "preferred_username";
    private static final String TENANT_ID_CLAIM = "tid";
    private static final String VERSION_CLAIM = "ver";

    private static final String AUDIENCE = "audience-for-testing";
    private static final String TENANT_ID = "some-tid";
    private static final String OBJECT_ID = "some-uid";
    private static final String PREFERRED_USERNAME = "test@test.onmicrosoft.com";
    private static final String ISSUER = "https://login.onmicrosoftonline.com/test/v2.0";
    private static final String SUBJECT = "TestSubject";
    private static final String VERSION = "2.0";
    private static final String NAME = "test";
    private static final String UID = "some-uid";
    private static final String UTID = "some-tid";
    private static final String ENCODING_UTF8 = "UTF-8";

    private static String createToken(final String issuer,
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

    private static String createIdToken(final String issuer,
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
        return createToken(
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

    public static String createIdToken() {
        long exp = getExpirationTimeAfterSpecifiedTime(3600);
        return createIdTokenWithExp(exp);
    }

    public static String createIdTokenWithExp(long exp) {
        return createIdToken(
                ISSUER,
                SUBJECT,
                AUDIENCE,
                NAME,
                PREFERRED_USERNAME,
                OBJECT_ID,
                TENANT_ID,
                VERSION,
                new Date(),
                new Date(),
                new Date(exp)
        );
    }

    private static String createRawClientInfo(final String uid, final String utid) {
        final String claims = "{\"uid\":\"" + uid + "\",\"utid\":\"" + utid + "\"}";

        return new String(Base64.encode(claims.getBytes(
                Charset.forName(ENCODING_UTF8)), Base64.NO_PADDING | Base64.NO_WRAP | Base64.URL_SAFE));
    }

    public static String createRawClientInfo() {
        return createRawClientInfo(UID, UTID);
    }

    public static long getExpirationTimeAfterSpecifiedTime(long numberOfSecondsAfterCurrentTime) {
        return System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(numberOfSecondsAfterCurrentTime);
    }
}
