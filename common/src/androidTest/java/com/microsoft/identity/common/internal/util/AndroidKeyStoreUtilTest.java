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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.microsoft.identity.common.java.exception.ClientException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;

import javax.crypto.Cipher;
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
    public void testUnwrap_Success() throws Exception {
        // Mock Cipher for successful unwrap
        try (MockedStatic<Cipher> mockedCipher = Mockito.mockStatic(Cipher.class)) {
            Cipher mockCipher = mock(Cipher.class);
            when(mockCipher.unwrap(any(byte[].class), any(String.class), any(int.class)))
                    .thenReturn(mSecretKey);
            mockedCipher.when(() -> Cipher.getInstance(TEST_WRAP_ALGORITHM))
                    .thenReturn(mockCipher);

            // Test successful unwrap
            SecretKey result = AndroidKeyStoreUtil.unwrap(
                    TEST_WRAPPED_KEY_BYTES,
                    TEST_KEY_ALGORITHM,
                    mKeyPair,
                    TEST_WRAP_ALGORITHM
            );

            assertNotNull(result);
            assertEquals(mSecretKey, result);
            verify(mockCipher, times(1)).init(Cipher.UNWRAP_MODE, mKeyPair.getPrivate());
        }
    }

    @Test
    public void testUnwrap_WithRetries() throws Exception {
        // Mock Cipher to fail twice with InvalidKeyException then succeed
        try (MockedStatic<Cipher> mockedCipher = Mockito.mockStatic(Cipher.class)) {
            Cipher mockCipher = mock(Cipher.class);
            when(mockCipher.unwrap(any(byte[].class), any(String.class), any(int.class)))
                    .thenThrow(new InvalidKeyException("Test failure 1"))
                    .thenThrow(new InvalidKeyException("Test failure 2"))
                    .thenReturn(mSecretKey);
            mockedCipher.when(() -> Cipher.getInstance(TEST_WRAP_ALGORITHM))
                    .thenReturn(mockCipher);

            // Test unwrap with retries
            SecretKey result = AndroidKeyStoreUtil.unwrap(
                    TEST_WRAPPED_KEY_BYTES,
                    TEST_KEY_ALGORITHM,
                    mKeyPair,
                    TEST_WRAP_ALGORITHM
            );

            assertNotNull(result);
            assertEquals(mSecretKey, result);
            verify(mockCipher, times(3)).init(Cipher.UNWRAP_MODE, mKeyPair.getPrivate());
        }
    }

    @Test
    public void testUnwrap_MaxRetriesExceeded() throws Exception {
        // Mock Cipher to always fail with InvalidKeyException
        try (MockedStatic<Cipher> mockedCipher = Mockito.mockStatic(Cipher.class)) {
            Cipher mockCipher = mock(Cipher.class);
            when(mockCipher.unwrap(any(byte[].class), any(String.class), any(int.class)))
                    .thenThrow(new InvalidKeyException("Test failure"));
            mockedCipher.when(() -> Cipher.getInstance(TEST_WRAP_ALGORITHM))
                    .thenReturn(mockCipher);

            try {
                AndroidKeyStoreUtil.unwrap(
                        TEST_WRAPPED_KEY_BYTES,
                        TEST_KEY_ALGORITHM,
                        mKeyPair,
                        TEST_WRAP_ALGORITHM
                );
                fail("Should have thrown ClientException");
            } catch (ClientException e) {
                assertEquals("INVALID_KEY", e.getErrorCode());
                verify(mockCipher, times(3)).init(Cipher.UNWRAP_MODE, mKeyPair.getPrivate());
            }
        }
    }

    @Test
    public void testUnwrap_NoSuchAlgorithm() throws Exception {
        try (MockedStatic<Cipher> mockedCipher = Mockito.mockStatic(Cipher.class)) {
            mockedCipher.when(() -> Cipher.getInstance(TEST_WRAP_ALGORITHM))
                    .thenThrow(new NoSuchAlgorithmException("Test failure"));

            try {
                AndroidKeyStoreUtil.unwrap(
                        TEST_WRAPPED_KEY_BYTES,
                        TEST_KEY_ALGORITHM,
                        mKeyPair,
                        TEST_WRAP_ALGORITHM
                );
                fail("Should have thrown ClientException");
            } catch (ClientException e) {
                assertEquals("NO_SUCH_ALGORITHM", e.getErrorCode());
            }
        }
    }
}
