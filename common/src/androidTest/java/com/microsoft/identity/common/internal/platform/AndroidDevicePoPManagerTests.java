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
package com.microsoft.identity.common.internal.platform;

import android.content.Context;
import android.os.Build;
import android.util.Base64;

import androidx.annotation.RequiresApi;
import androidx.test.InstrumentationRegistry;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.crypto.IDevicePopManager;
import com.google.gson.reflect.TypeToken;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyFactory;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.text.ParseException;
import java.util.Date;
import java.util.Map;

import static com.microsoft.identity.common.java.crypto.IDevicePopManager.PublicKeyFormat.JWK;
import static com.microsoft.identity.common.java.crypto.IDevicePopManager.PublicKeyFormat.X_509_SubjectPublicKeyInfo_ASN_1;

// Note: Test cannot use robolectric due to the following open issue
// https://github.com/robolectric/robolectric/issues/1518
//todo: Investigate if these tests can be migrated to common4j
@RunWith(AndroidJUnit4.class)
public class AndroidDevicePoPManagerTests {

    private Context mContext;
    private IDevicePopManager mDevicePopManager;

    @Before
    public void setUp()
            throws CertificateException, NoSuchAlgorithmException,
            KeyStoreException, IOException {
        mContext = InstrumentationRegistry.getTargetContext();
        mDevicePopManager = new AndroidDevicePopManager(ApplicationProvider.getApplicationContext());
    }

    @After
    public void tearDown() {
        mDevicePopManager.clearAsymmetricKey();
        mDevicePopManager = null;
    }

    @Test
    public void testAsymmetricKeyExists() throws ClientException {
        Assert.assertFalse(mDevicePopManager.asymmetricKeyExists());
        mDevicePopManager.generateAsymmetricKey();
        Assert.assertTrue(mDevicePopManager.asymmetricKeyExists());
    }

    @Test
    public void testAsymmetricKeyExistsById() throws ClientException {
        Assert.assertFalse(mDevicePopManager.asymmetricKeyExists());
        mDevicePopManager.generateAsymmetricKey();
        Assert.assertTrue(mDevicePopManager.asymmetricKeyExists());
        final String kid = mDevicePopManager.getAsymmetricKeyThumbprint();
        Assert.assertTrue(mDevicePopManager.asymmetricKeyExists(kid));
    }

    @Test
    public void testGetAsymmetricKeyThumbprint() throws ClientException {
        Assert.assertFalse(mDevicePopManager.asymmetricKeyExists());
        mDevicePopManager.generateAsymmetricKey();
        Assert.assertTrue(mDevicePopManager.asymmetricKeyExists());
        final String kid = mDevicePopManager.getAsymmetricKeyThumbprint();
        Assert.assertNotNull(kid);
    }

    @Test
    public void testGenerateAsymmetricKey() throws ClientException {
        Assert.assertFalse(mDevicePopManager.asymmetricKeyExists());
        mDevicePopManager.generateAsymmetricKey();
        Assert.assertTrue(mDevicePopManager.asymmetricKeyExists());
    }

    @Test
    public void testClearAsymmetricKey() throws ClientException {
        Assert.assertFalse(mDevicePopManager.asymmetricKeyExists());
        mDevicePopManager.generateAsymmetricKey();
        Assert.assertTrue(mDevicePopManager.asymmetricKeyExists());
        mDevicePopManager.clearAsymmetricKey();
        Assert.assertFalse(mDevicePopManager.asymmetricKeyExists());
    }

    @Test
    public void testGetRequestConfirmation() throws ClientException {
        Assert.assertFalse(mDevicePopManager.asymmetricKeyExists());
        mDevicePopManager.generateAsymmetricKey();
        final String reqCnf = mDevicePopManager.getRequestConfirmation();
        Assert.assertNotNull(reqCnf);
    }

    @Test
    public void testMintSignedAccessToken() throws ClientException, MalformedURLException {
        Assert.assertFalse(mDevicePopManager.asymmetricKeyExists());
        mDevicePopManager.generateAsymmetricKey();
        Assert.assertTrue(mDevicePopManager.asymmetricKeyExists());
        final String shr = mDevicePopManager.mintSignedAccessToken(
                "GET",
                12345,
                new URL("https://www.contoso.com"),
                "a_token_for_you",
                "54321"
        );
        Assert.assertNotNull(shr);
    }

    @Test
    public void testMintSignedAccessTokenWithClientClaims()
            throws ClientException, MalformedURLException, ParseException {
        Assert.assertFalse(mDevicePopManager.asymmetricKeyExists());
        mDevicePopManager.generateAsymmetricKey();
        Assert.assertTrue(mDevicePopManager.asymmetricKeyExists());
        final String httpMethod = "GET";
        final long timestamp = 12345;
        final String scheme = "https://";
        final String path = "/path1/path2";
        final String host = "www.contoso.com:443";
        final String hostWithSchemeAndPath = scheme + host + path;
        final String at = "a_token_for_you";
        final String nonce = "54321";
        final String clientClaims = "some_claims";
        final String shr = mDevicePopManager.mintSignedAccessToken(
                httpMethod,
                timestamp,
                new URL(hostWithSchemeAndPath),
                at,
                nonce,
                clientClaims
        );
        Assert.assertNotNull(shr);

        final SignedJWT jwt = SignedJWT.parse(shr);

        // Verify headers
        final JWSHeader jwsHeader = jwt.getHeader();
        Assert.assertEquals("RS256", jwsHeader.getAlgorithm().getName());
        Assert.assertEquals(jwsHeader.getKeyID(), mDevicePopManager.getAsymmetricKeyThumbprint());

        // Verify body
        final JWTClaimsSet jwtClaimsSet = jwt.getJWTClaimsSet();
        Assert.assertEquals(httpMethod, jwtClaimsSet.getClaim("m"));
        Assert.assertEquals(timestamp, jwtClaimsSet.getClaim("ts"));
        Assert.assertEquals(host, jwtClaimsSet.getClaim("u"));
        Assert.assertEquals(path, jwtClaimsSet.getClaim("p"));
        Assert.assertEquals(nonce, jwtClaimsSet.getClaim("nonce"));
        Assert.assertNotNull(jwtClaimsSet.getClaim("cnf"));
        Assert.assertEquals(clientClaims, jwtClaimsSet.getClaim("client_claims"));
    }

    @Test
    public void testMintSignedAccessTokenWithNullHttpMethod()
            throws ClientException, MalformedURLException, ParseException {
        Assert.assertFalse(mDevicePopManager.asymmetricKeyExists());
        mDevicePopManager.generateAsymmetricKey();
        Assert.assertTrue(mDevicePopManager.asymmetricKeyExists());
        final String shr = mDevicePopManager.mintSignedAccessToken(
                null, // Not supplied
                12345,
                new URL("https://www.contoso.com"),
                "a_token_for_you",
                "54321"
        );
        final SignedJWT jwt = SignedJWT.parse(shr);
        final JWTClaimsSet jwtClaimsSet = jwt.getJWTClaimsSet();
        Assert.assertNull(jwtClaimsSet.getClaim("m"));
        Assert.assertNotNull(jwtClaimsSet.getClaim("ts"));
    }

    @Test
    public void testKidHeaderMatchesThumbprint() throws ClientException, MalformedURLException, ParseException {
        Assert.assertFalse(mDevicePopManager.asymmetricKeyExists());
        mDevicePopManager.generateAsymmetricKey();
        Assert.assertTrue(mDevicePopManager.asymmetricKeyExists());
        final String shr = mDevicePopManager.mintSignedAccessToken(
                null, // Not supplied
                12345,
                new URL("https://www.contoso.com"),
                "a_token_for_you",
                "54321"
        );
        final SignedJWT jwt = SignedJWT.parse(shr);
        final JWSHeader jwsHeader = jwt.getHeader();
        Assert.assertEquals(jwsHeader.getKeyID(), mDevicePopManager.getAsymmetricKeyThumbprint());
    }

    @Test
    public void testHeaderAlgRS256() throws ClientException, MalformedURLException, ParseException {
        Assert.assertFalse(mDevicePopManager.asymmetricKeyExists());
        mDevicePopManager.generateAsymmetricKey();
        Assert.assertTrue(mDevicePopManager.asymmetricKeyExists());
        final String shr = mDevicePopManager.mintSignedAccessToken(
                null, // Not supplied
                12345,
                new URL("https://www.contoso.com"),
                "a_token_for_you",
                "54321"
        );
        final SignedJWT jwt = SignedJWT.parse(shr);
        final JWSHeader jwsHeader = jwt.getHeader();
        Assert.assertEquals("RS256", jwsHeader.getAlgorithm().getName());
    }

    @Test
    public void testMintSignedAccessTokenWithNullPath()
            throws ClientException, MalformedURLException, ParseException {
        final String host = "www.contoso.com";
        final String hostWithScheme = "https://" + host;

        Assert.assertFalse(mDevicePopManager.asymmetricKeyExists());
        mDevicePopManager.generateAsymmetricKey();
        Assert.assertTrue(mDevicePopManager.asymmetricKeyExists());
        final String shr = mDevicePopManager.mintSignedAccessToken(
                "OPTIONS", // Not supplied
                12345,
                new URL(hostWithScheme),
                "a_token_for_you",
                "54321"
        );
        final SignedJWT jwt = SignedJWT.parse(shr);
        final JWTClaimsSet jwtClaimsSet = jwt.getJWTClaimsSet();
        Assert.assertEquals(host, jwtClaimsSet.getClaim("u"));
        Assert.assertNull(jwtClaimsSet.getClaim("p"));
    }

    @Test
    public void testMintSignedAccessTokenWithPath()
            throws ClientException, MalformedURLException, ParseException {
        final String scheme = "https://";
        final String path = "/path1/path2";
        final String host = "www.contoso.com:443";
        final String hostWithSchemeAndPath = scheme + host + path;

        Assert.assertFalse(mDevicePopManager.asymmetricKeyExists());
        mDevicePopManager.generateAsymmetricKey();
        Assert.assertTrue(mDevicePopManager.asymmetricKeyExists());
        final String shr = mDevicePopManager.mintSignedAccessToken(
                null, // Not supplied
                12345,
                new URL(hostWithSchemeAndPath),
                "a_token_for_you",
                "54321"
        );
        final SignedJWT jwt = SignedJWT.parse(shr);
        final JWTClaimsSet jwtClaimsSet = jwt.getJWTClaimsSet();
        Assert.assertEquals(host, jwtClaimsSet.getClaim("u"));
        Assert.assertEquals(path, jwtClaimsSet.getClaim("p"));
    }

    @Test
    public void testMintSignedAccessTokenWithPortNumber()
            throws ClientException, MalformedURLException, ParseException {
        final String host = "www.contoso.com:443";
        final String hostWithScheme = "https://" + host;

        Assert.assertFalse(mDevicePopManager.asymmetricKeyExists());
        mDevicePopManager.generateAsymmetricKey();
        Assert.assertTrue(mDevicePopManager.asymmetricKeyExists());
        final String shr = mDevicePopManager.mintSignedAccessToken(
                null, // Not supplied
                12345,
                new URL(hostWithScheme),
                "a_token_for_you",
                "54321"
        );
        final SignedJWT jwt = SignedJWT.parse(shr);
        final JWTClaimsSet jwtClaimsSet = jwt.getJWTClaimsSet();
        Assert.assertEquals(host, jwtClaimsSet.getClaim("u"));
    }

    @Test
    public void testMintSignedAccessTokenContainsRequisiteClaims()
            throws ClientException, MalformedURLException, ParseException {
        final String httpMethod = "TRACE";
        final String path = "/path1/path2";
        final String host = "www.contoso.com:443";
        final String hostWithScheme = "https://" + host;
        final String hostWithPath = hostWithScheme + path;
        final long timestamp = 12345;
        final String nonce = "a_nonce_value";
        final String accessToken = "a_token_for_you";

        Assert.assertFalse(mDevicePopManager.asymmetricKeyExists());
        mDevicePopManager.generateAsymmetricKey();
        Assert.assertTrue(mDevicePopManager.asymmetricKeyExists());
        final String shr = mDevicePopManager.mintSignedAccessToken(
                httpMethod,
                timestamp,
                new URL(hostWithPath),
                accessToken,
                nonce
        );
        final SignedJWT jwt = SignedJWT.parse(shr);

        // Verify headers
        final JWSHeader jwsHeader = jwt.getHeader();
        Assert.assertEquals("RS256", jwsHeader.getAlgorithm().getName());
        Assert.assertEquals(jwsHeader.getKeyID(), mDevicePopManager.getAsymmetricKeyThumbprint());

        // Verify body
        final JWTClaimsSet jwtClaimsSet = jwt.getJWTClaimsSet();
        Assert.assertEquals(httpMethod, jwtClaimsSet.getClaim("m"));
        Assert.assertEquals(timestamp, jwtClaimsSet.getClaim("ts"));
        Assert.assertEquals(host, jwtClaimsSet.getClaim("u"));
        Assert.assertEquals(path, jwtClaimsSet.getClaim("p"));
        Assert.assertEquals(accessToken, jwtClaimsSet.getClaim("at"));
        Assert.assertEquals(nonce, jwtClaimsSet.getClaim("nonce"));
        Assert.assertNotNull(jwtClaimsSet.getClaim("cnf"));
    }

    @Test
    public void testAsymmetricKeyCreationDateNullWhenUninitialized() throws ClientException {
        final Date createdDate = mDevicePopManager.getAsymmetricKeyCreationDate();
        Assert.assertNull(createdDate);
    }

    @Test
    public void testAsymmetricKeyHasCreationDate() throws ClientException {
        final Date createdDate = mDevicePopManager.getAsymmetricKeyCreationDate();
        Assert.assertNull(createdDate);

        // Generate it
        mDevicePopManager.generateAsymmetricKey();

        // Assert the Date exists
        Assert.assertNotNull(mDevicePopManager.getAsymmetricKeyCreationDate());
    }

    @Test
    public void testAsymmetricKeyHasPublicKeyX509() throws ClientException, NoSuchAlgorithmException, InvalidKeySpecException {
        // Generate keys
        mDevicePopManager.generateAsymmetricKey();

        // Get the public key
        final String publicKey = mDevicePopManager.getPublicKey(X_509_SubjectPublicKeyInfo_ASN_1);

        // Rehydrate the certificate
        final byte[] bytes = Base64.decode(publicKey, Base64.DEFAULT);
        final KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        final PublicKey pubKeyRestored = keyFactory.generatePublic(new X509EncodedKeySpec(bytes));
        Assert.assertEquals("X.509", pubKeyRestored.getFormat());
    }

    @Test
    public void testAsymmetricKeyHasPublicKeyJwk() throws ClientException {
        // Generate keys
        mDevicePopManager.generateAsymmetricKey();

        // Get the public key
        final String publicKey = mDevicePopManager.getPublicKey(JWK);

        // Convert it to JSON, parse to verify fields
        final Map<String, String> jwkObj = new Gson().fromJson(publicKey, TypeToken.getParameterized(Map.class, String.class, String.class).getType());

        // We should expect the following claims...
        // 'kty' - Key Type - Identifies the cryptographic alg used with this key (ex: RSA, EC)
        // 'e' - Public Exponent - The exponent used on signed/encoded data to decode the orig value
        // 'n' - Modulus - The product of two prime numbers used to generate the key pair
        final String kty = jwkObj.get("kty");
        Assert.assertNotNull(kty);
        Assert.assertFalse(kty.isEmpty());

        final String e = jwkObj.get("e");
        Assert.assertNotNull(e);
        Assert.assertFalse(e.isEmpty());

        final String n = jwkObj.get("n");
        Assert.assertNotNull(n);
        Assert.assertFalse(n.isEmpty());
    }

    @Test
    public void testGenerateShr() throws ClientException, MalformedURLException, ParseException {
        final String httpMethod = "TRACE";
        final String path = "/path1/path2";
        final String host = "www.contoso.com:443";
        final String hostWithScheme = "https://" + host;
        final String hostWithPath = hostWithScheme + path;
        final long timestamp = 12345;
        final String nonce = "a_nonce_value";
        final String clientClaims = "test claims!";

        Assert.assertFalse(mDevicePopManager.asymmetricKeyExists());
        mDevicePopManager.generateAsymmetricKey();
        Assert.assertTrue(mDevicePopManager.asymmetricKeyExists());
        final String shr = mDevicePopManager.mintSignedHttpRequest(
                httpMethod,
                timestamp,
                new URL(hostWithPath),
                nonce,
                clientClaims
        );
        final SignedJWT jwt = SignedJWT.parse(shr);

        // Verify headers
        final JWSHeader jwsHeader = jwt.getHeader();
        Assert.assertEquals("RS256", jwsHeader.getAlgorithm().getName());
        Assert.assertEquals(jwsHeader.getKeyID(), mDevicePopManager.getAsymmetricKeyThumbprint());

        // Verify body
        final JWTClaimsSet jwtClaimsSet = jwt.getJWTClaimsSet();
        Assert.assertEquals(httpMethod, jwtClaimsSet.getClaim("m"));
        Assert.assertEquals(timestamp, jwtClaimsSet.getClaim("ts"));
        Assert.assertEquals(host, jwtClaimsSet.getClaim("u"));
        Assert.assertEquals(path, jwtClaimsSet.getClaim("p"));
        Assert.assertEquals(nonce, jwtClaimsSet.getClaim("nonce"));
        Assert.assertNotNull(jwtClaimsSet.getClaim("cnf"));
        Assert.assertEquals(clientClaims, jwtClaimsSet.getClaim("client_claims"));
    }

    @Test
    @RequiresApi(Build.VERSION_CODES.N)
    public void testHasCertificateChain24() throws ClientException {
        Assert.assertFalse(mDevicePopManager.asymmetricKeyExists());
        mDevicePopManager.generateAsymmetricKey();
        Assert.assertTrue(mDevicePopManager.asymmetricKeyExists());

        // At least 1 certificate should exist, though likely there is an additional
        // endorsement key (EK) root if testing on a real, Play Services compatible device.
        final Certificate[] chain = mDevicePopManager.getCertificateChain();
        Assert.assertNotEquals(0, chain.length);
        Assert.assertEquals(
                "X.509",
                mDevicePopManager.getCertificateChain()[0].getType()
        );
    }

    @Test
    @RequiresApi(Build.VERSION_CODES.N)
    public void testNullWhenQueryingNonexistentChain24() throws ClientException {
        Assert.assertFalse(mDevicePopManager.asymmetricKeyExists());
        // Returns null for nonexistent key
        Assert.assertNull(mDevicePopManager.getCertificateChain());
    }
}
