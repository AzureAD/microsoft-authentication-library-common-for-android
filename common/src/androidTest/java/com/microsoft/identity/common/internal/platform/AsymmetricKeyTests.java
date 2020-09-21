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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.microsoft.identity.common.exception.ClientException;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Locale;

@RunWith(AndroidJUnit4.class)
public class AsymmetricKeyTests {

    private static final String testKeyName = "testkey_alias";

    private AsymmetricKeyFactory mKeyFactory;
    private AsymmetricKey mAsymmetricKey;

    @Before
    public void setUp() throws ClientException {
        final Context context = InstrumentationRegistry.getTargetContext();
        mKeyFactory = new AndroidKeystoreAsymmetricKeyFactory(context);
        mAsymmetricKey = mKeyFactory.generateAsymmetricKey(testKeyName);
    }

    @After
    public void tearDown() throws ClientException {
        mKeyFactory.clearAsymmetricKey(testKeyName);
    }

    @Test
    public void testHasCreatedOn() throws ClientException {
        Assert.assertNotNull(mAsymmetricKey.getCreatedOn());
    }

    @Test
    public void testHasThumbprint() throws ClientException {
        Assert.assertNotNull(mAsymmetricKey.getThumbprint());
    }

    @Test
    public void testHasPublicKey() throws ClientException {
        final String publicKey = mAsymmetricKey.getPublicKey();

        Assert.assertNotNull(publicKey);

        // Convert it to JSON, parse to verify fields
        final JsonElement jwkElement = new JsonParser().parse(publicKey);

        // Convert to JsonObject to extract claims
        final JsonObject jwkObj = jwkElement.getAsJsonObject();

        // We should expect the following claims...
        // 'kty' - Key Type - Identifies the cryptographic alg used with this key (ex: RSA, EC)
        // 'e' - Public Exponent - The exponent used on signed/encoded data to decode the orig value
        // 'n' - Modulus - The product of two prime numbers used to generate the key pair
        final JsonElement kty = jwkObj.get("kty");
        Assert.assertNotNull(kty);
        Assert.assertFalse(kty.getAsString().isEmpty());

        final JsonElement e = jwkObj.get("e");
        Assert.assertNotNull(e);
        Assert.assertFalse(e.getAsString().isEmpty());

        final JsonElement n = jwkObj.get("n");
        Assert.assertNotNull(n);
        Assert.assertFalse(n.getAsString().isEmpty());
    }

    @Test
    public void testCanSignAndVerify() throws ClientException {
        final String validationText = "The quick brown fox jumped over the lazy dog.";
        final String signature = mAsymmetricKey.sign(validationText);
        Assert.assertNotNull(signature);
        Assert.assertTrue(mAsymmetricKey.verify(validationText, signature));
    }

    @Test
    public void testVerificationFailsForModifiedString() throws ClientException {
        final String validationText = "The quick brown fox jumped over the lazy dog.";
        final String signature = mAsymmetricKey.sign(validationText);
        Assert.assertNotNull(signature);
        Assert.assertFalse(mAsymmetricKey.verify(validationText.toLowerCase(Locale.ROOT), signature));
    }

}
