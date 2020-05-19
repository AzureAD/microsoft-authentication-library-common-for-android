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

import androidx.test.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.microsoft.identity.common.exception.ClientException;
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
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.text.ParseException;

@RunWith(AndroidJUnit4.class)
public class DevicePoPManagerTests {

    private Context mContext;
    private IDevicePopManager mDevicePopManager;

    @Before
    public void setUp()
            throws CertificateException, NoSuchAlgorithmException,
            KeyStoreException, IOException {
        mContext = InstrumentationRegistry.getTargetContext();
        mDevicePopManager = new DevicePopManager();
    }

    @After
    public void tearDown() {
        mDevicePopManager.clearAsymmetricKey();
        mDevicePopManager = null;
    }

    @Test
    public void testAsymmetricKeyExists() throws ClientException {
        Assert.assertFalse(mDevicePopManager.asymmetricKeyExists());
        mDevicePopManager.generateAsymmetricKey(mContext);
        Assert.assertTrue(mDevicePopManager.asymmetricKeyExists());
    }

    @Test
    public void testAsymmetricKeyExistsById() throws ClientException {
        Assert.assertFalse(mDevicePopManager.asymmetricKeyExists());
        mDevicePopManager.generateAsymmetricKey(mContext);
        Assert.assertTrue(mDevicePopManager.asymmetricKeyExists());
        final String kid = mDevicePopManager.getAsymmetricKeyThumbprint();
        Assert.assertTrue(mDevicePopManager.asymmetricKeyExists(kid));
    }

    @Test
    public void testGetAsymmetricKeyThumbprint() throws ClientException {
        Assert.assertFalse(mDevicePopManager.asymmetricKeyExists());
        mDevicePopManager.generateAsymmetricKey(mContext);
        Assert.assertTrue(mDevicePopManager.asymmetricKeyExists());
        final String kid = mDevicePopManager.getAsymmetricKeyThumbprint();
        Assert.assertNotNull(kid);
    }

    @Test
    public void testGenerateAsymmetricKey() throws ClientException {
        Assert.assertFalse(mDevicePopManager.asymmetricKeyExists());
        mDevicePopManager.generateAsymmetricKey(mContext);
        Assert.assertTrue(mDevicePopManager.asymmetricKeyExists());
    }

    @Test
    public void testClearAsymmetricKey() throws ClientException {
        Assert.assertFalse(mDevicePopManager.asymmetricKeyExists());
        mDevicePopManager.generateAsymmetricKey(mContext);
        Assert.assertTrue(mDevicePopManager.asymmetricKeyExists());
        mDevicePopManager.clearAsymmetricKey();
        Assert.assertFalse(mDevicePopManager.asymmetricKeyExists());
    }

    @Test
    public void testGetRequestConfirmation() throws ClientException {
        Assert.assertFalse(mDevicePopManager.asymmetricKeyExists());
        mDevicePopManager.generateAsymmetricKey(mContext);
        final String reqCnf = mDevicePopManager.getRequestConfirmation();
        Assert.assertNotNull(reqCnf);
    }

    @Test
    public void testMintSignedAccessToken() throws ClientException, MalformedURLException {
        Assert.assertFalse(mDevicePopManager.asymmetricKeyExists());
        mDevicePopManager.generateAsymmetricKey(mContext);
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
    public void testMintSignedAccessTokenWithNullHttpMethod()
            throws ClientException, MalformedURLException, ParseException {
        Assert.assertFalse(mDevicePopManager.asymmetricKeyExists());
        mDevicePopManager.generateAsymmetricKey(mContext);
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
        mDevicePopManager.generateAsymmetricKey(mContext);
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
        mDevicePopManager.generateAsymmetricKey(mContext);
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
        mDevicePopManager.generateAsymmetricKey(mContext);
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
        mDevicePopManager.generateAsymmetricKey(mContext);
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
        mDevicePopManager.generateAsymmetricKey(mContext);
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
        mDevicePopManager.generateAsymmetricKey(mContext);
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
}
