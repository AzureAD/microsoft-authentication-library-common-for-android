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

import com.microsoft.identity.common.java.crypto.CryptoSuite;
import com.microsoft.identity.common.java.crypto.SigningAlgorithm;

import org.junit.Assert;
import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;

public class RawKeyAccessorTest {
    @Test
    public void testEncryptDecrypt() throws Exception {
        RawKeyAccessor a = getAccessor();

        final byte[] in = "ABCDEFGHABCDEFGHABCDEFGHABCDEFGH".getBytes();
        byte[] out = a.encrypt(in);
        byte[] around = a.decrypt(out);
        Assert.assertArrayEquals(in, around);
    }

    public RawKeyAccessor getAccessor() throws UnsupportedEncodingException {

        return new RawKeyAccessor(new CryptoSuite() {
            @Override
            public SymmetricAlgorithm cipher() {
                return SymmetricAlgorithm.Builder.of("AES/GCM/NoPadding");
            }

            @Override
            public String macName() {
                return "HmacSHA256";
            }

            @Override
            public boolean isAsymmetric() {
                return false;
            }

            @Override
            public Class<? extends KeyStore.Entry> keyClass() {
                return null;
            }

            @Override
            public int keySize() {
                return 256;
            }

            @Override
            public SigningAlgorithm signingAlgorithm() {
                return null;
            }
        }, "12345678123456781234567812345678".getBytes("UTF-8"), null);
    }

    @Test
    public void testSignAndVerify() throws Exception {
        RawKeyAccessor a = getAccessor();

        final byte[] in = "ABCDEFGHABCDEFGHABCDEFGHABCDEFGH".getBytes();
        byte[] out = a.sign(in);
        Assert.assertTrue(a.verify(in, out));
    }

    @Test
    public void testDeriveKey() throws Exception {
        RawKeyAccessor a = getAccessor();
        byte[] label = "testLabel".getBytes(StandardCharsets.UTF_8);
        byte[] ctx = "1234".getBytes(StandardCharsets.UTF_8);
        Assert.assertNotNull(a.generateDerivedKey(label, ctx));
    }
}
