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

package com.microsoft.identity.common.internal.util;

import static com.microsoft.identity.common.java.exception.ClientException.INVALID_KEY;
import static com.microsoft.identity.common.java.exception.ClientException.NO_SUCH_ALGORITHM;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.microsoft.identity.common.java.exception.ClientException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.security.KeyPair;
import java.security.KeyPairGenerator;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

@RunWith(AndroidJUnit4.class)
public class AndroidKeyStoreUtilTest {
    private static final String TEST_WRAP_ALGORITHM = "RSA/ECB/PKCS1Padding";
    private static final String TEST_KEY_ALGORITHM = "AES";
    private static final byte[] TEST_KEY_BYTES = new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16};
    private static final byte[] TEST_WRAPPED_KEY_BYTES = new byte[]{16, 15, 14, 13, 12, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1};

    private KeyPair mKeyPair;
    private SecretKey mSecretKey;

    @Before
    public void setUp() throws Exception {
        // Create a real KeyPair for testing
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        mKeyPair = keyGen.generateKeyPair();

        // Create a test SecretKey
        mSecretKey = new SecretKeySpec(TEST_KEY_BYTES, TEST_KEY_ALGORITHM);
    }

    @Test
    public void testUnwrap_Success() throws ClientException {
        // Test successful wrapping
        final byte[] wrapped = AndroidKeyStoreUtil.wrap(
                mSecretKey,
                mKeyPair,
                TEST_WRAP_ALGORITHM,
                null
        );

        // Test successful unwrap
        final SecretKey unwrapped = AndroidKeyStoreUtil.unwrap(
                wrapped,
                TEST_KEY_ALGORITHM,
                mKeyPair,
                TEST_WRAP_ALGORITHM,
                null
        );

        assertNotNull(unwrapped);
        assertEquals(mSecretKey, unwrapped);
    }

    @Test
    public void testUnwrap_NoSuchAlgorithm() {
        try {
            final byte[] wrapped = AndroidKeyStoreUtil.wrap(
                    mSecretKey,
                    mKeyPair,
                    TEST_WRAP_ALGORITHM,
                    null
            );

            final SecretKey secretKey = AndroidKeyStoreUtil.unwrap(
                    wrapped,
                    TEST_KEY_ALGORITHM,
                    mKeyPair,
                    "NoAlg",
                    null
            );
            fail("Should have thrown ClientException");
        } catch (final ClientException e) {
            assertEquals(NO_SUCH_ALGORITHM, e.getErrorCode());
        }
    }

    @Test
    public void testUnwrap_InvalidKey() {
        try {
            AndroidKeyStoreUtil.unwrap(
                    TEST_WRAPPED_KEY_BYTES,
                    TEST_KEY_ALGORITHM,
                    mKeyPair,
                    TEST_WRAP_ALGORITHM,
                    null
            );
            fail("Should have thrown ClientException");
        } catch (final ClientException e) {
            assertEquals(INVALID_KEY, e.getErrorCode());
        }
    }
}
