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
package com.microsoft.identity.common.java.crypto;


import com.microsoft.identity.common.java.exception.ClientException;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import javax.crypto.spec.SecretKeySpec;

/**
 * Tests for {@link SP800108KeyGen}
 */
public class SP800108KeyGenTests {

    @Test
    public void test_generateDerivedKey() throws IOException, NoSuchAlgorithmException, ClientException, InvalidKeyException {
        final SP800108KeyGen keygen = new SP800108KeyGen(new DefaultCryptoFactory());
        final SecretKeySpec keyDerivationKey1 = new SecretKeySpec("keyDerivationKey".getBytes(), "HmacSHA256");
        final byte[] label1 = "label1".getBytes();

        final byte[] ctx1 = "ctx1".getBytes();
        final String expectedDerivedKey1 = "MhPZ41HpPJ4tirU62ciRCaeKLDpDj+KY8Xv+6B6YxeU=";
        final byte[]  derivedKey1 = keygen.generateDerivedKey(keyDerivationKey1, label1, ctx1);
        Assert.assertEquals(expectedDerivedKey1, Base64.getEncoder().encodeToString(derivedKey1));

        final byte[] keyDerivationKey2 = "keyDerivationKey".getBytes();
        final byte[] label2 = "label2".getBytes();
        final byte[] ctx2 = "ctx2".getBytes();
        final String expectedDerivedKey2 = "Vj5vebnHvixm9SZ4YC3AoStaaOFS6uVRCYxQaIcMZFw=";
        final byte[]  derivedKey2 = keygen.generateDerivedKey(keyDerivationKey2, label2, ctx2);
        Assert.assertEquals(expectedDerivedKey2, Base64.getEncoder().encodeToString(derivedKey2));
    }
}
