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

import com.microsoft.identity.common.java.crypto.key.AES256KeyLoader;
import com.microsoft.identity.common.java.crypto.key.AbstractSecretKeyLoader;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.exception.ErrorStrings;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import static com.microsoft.identity.common.java.AuthenticationConstants.ENCODING_UTF8;
import static com.microsoft.identity.common.java.crypto.MockData.PREDEFINED_KEY;
import static com.microsoft.identity.common.java.crypto.MockData.ANDROID_WRAPPED_KEY;
import static com.microsoft.identity.common.java.crypto.MockData.PREDEFINED_KEY_MALFORMED;
import static com.microsoft.identity.common.java.crypto.MockData.TEXT_ENCRYPTED_BY_PREDEFINED_KEY;
import static com.microsoft.identity.common.java.crypto.MockData.TEXT_ENCRYPTED_BY_ANDROID_WRAPPED_KEY;
import static com.microsoft.identity.common.java.crypto.MockData.EXPECTED_ENCRYPTED_TEXT_1_WITH_MALFORMED_ENCODE_VERSION;
import static com.microsoft.identity.common.java.crypto.MockData.PREDEFINED_KEY_IV;
import static com.microsoft.identity.common.java.crypto.MockData.ANDROID_WRAPPED_KEY_IV;
import static com.microsoft.identity.common.java.crypto.MockData.PREDEFINED_KEY_IDENTIFIER;
import static com.microsoft.identity.common.java.crypto.MockData.ANDROID_WRAPPED_KEY_IDENTIFIER;
import static com.microsoft.identity.common.java.crypto.MockData.TEXT_TO_BE_ENCRYPTED_WITH_PREDEFINED_KEY;
import static com.microsoft.identity.common.java.crypto.MockData.TEXT_TO_BE_ENCRYPTED_WITH_ANDROID_WRAPPED_KEY;
import static com.microsoft.identity.common.java.exception.ClientException.DATA_MALFORMED;
import static com.microsoft.identity.common.java.exception.ClientException.HMAC_MISMATCH;


public class StorageEncryptionManagerTest {

    @Test
    public void testEncrypt() throws ClientException {
        final StorageEncryptionManager manager = new MockStorageEncryptionManager(PREDEFINED_KEY_IV, new MockAES256KeyLoader(PREDEFINED_KEY, PREDEFINED_KEY_IDENTIFIER));
        Assert.assertArrayEquals(TEXT_ENCRYPTED_BY_PREDEFINED_KEY, manager.encrypt(TEXT_TO_BE_ENCRYPTED_WITH_PREDEFINED_KEY));

        final StorageEncryptionManager manager_2 = new MockStorageEncryptionManager(ANDROID_WRAPPED_KEY_IV, new MockAES256KeyLoader(ANDROID_WRAPPED_KEY, ANDROID_WRAPPED_KEY_IDENTIFIER));
        Assert.assertArrayEquals(TEXT_ENCRYPTED_BY_ANDROID_WRAPPED_KEY, manager_2.encrypt(TEXT_TO_BE_ENCRYPTED_WITH_ANDROID_WRAPPED_KEY));
    }

    @Test
    public void testDecrypt() throws ClientException {
        final StorageEncryptionManager manager = new MockStorageEncryptionManager(PREDEFINED_KEY_IV, new MockAES256KeyLoader(PREDEFINED_KEY, PREDEFINED_KEY_IDENTIFIER));
        Assert.assertArrayEquals(TEXT_TO_BE_ENCRYPTED_WITH_PREDEFINED_KEY, manager.decrypt(TEXT_ENCRYPTED_BY_PREDEFINED_KEY));

        final StorageEncryptionManager manager_2 = new MockStorageEncryptionManager(ANDROID_WRAPPED_KEY_IV, new MockAES256KeyLoader(ANDROID_WRAPPED_KEY, ANDROID_WRAPPED_KEY_IDENTIFIER));
        Assert.assertArrayEquals(TEXT_TO_BE_ENCRYPTED_WITH_ANDROID_WRAPPED_KEY, manager_2.decrypt(TEXT_ENCRYPTED_BY_ANDROID_WRAPPED_KEY));
    }

    @Test(expected = IllegalStateException.class)
    public void testEncryptNoKeyLoader() throws ClientException {
        final StorageEncryptionManager manager = new MockStorageEncryptionManager(PREDEFINED_KEY_IV, null);
        manager.encrypt(TEXT_TO_BE_ENCRYPTED_WITH_PREDEFINED_KEY);
        Assert.fail("decrypt() should throw an exception but it succeeds.");
    }

    @Test(expected = IllegalStateException.class)
    public void testDecryptNoKeyLoader() throws ClientException {
        final StorageEncryptionManager manager = new MockStorageEncryptionManager(PREDEFINED_KEY_IV, null, null);
        manager.decrypt(TEXT_ENCRYPTED_BY_PREDEFINED_KEY);
        Assert.fail("decrypt() should throw an exception but it succeeds.");
    }

    @Test(expected = IllegalStateException.class)
    public void testDecryptNullKeyLoader() throws ClientException {
        final StorageEncryptionManager manager = new MockStorageEncryptionManager(PREDEFINED_KEY_IV, null,
                new ArrayList<AbstractSecretKeyLoader>() {{
                    add(null);
                }});
        manager.decrypt(TEXT_ENCRYPTED_BY_PREDEFINED_KEY);
        Assert.fail("decrypt() should throw an exception but it succeeds.");
    }

    @Test(expected = IllegalStateException.class)
    public void testDecrypt_empty_KeyLoader_throws() throws ClientException {
        final StorageEncryptionManager manager = new MockStorageEncryptionManager(PREDEFINED_KEY_IV, null, Collections.<AbstractSecretKeyLoader>emptyList());
        manager.decrypt(TEXT_ENCRYPTED_BY_PREDEFINED_KEY);
        Assert.fail("decrypt() should throw an exception but it succeeds.");
    }

    @Test(expected = IllegalStateException.class)
    public void testDecrypt_null_keyloader_throws() throws ClientException {
        final StorageEncryptionManager manager = new MockStorageEncryptionManager(PREDEFINED_KEY_IV, null, null);
        final byte[] plainBytes = manager.decrypt(TEXT_ENCRYPTED_BY_PREDEFINED_KEY);
        Assert.fail("decrypt() should throw an exception but it succeeds.");
    }

    @Test(expected = ClientException.class)
    public void testEncryptFailToLoadKey() throws ClientException {
        final StorageEncryptionManager manager = new MockStorageEncryptionManager(PREDEFINED_KEY_IV, new MockAES256KeyLoaderWithGetKeyError());
        manager.encrypt(TEXT_TO_BE_ENCRYPTED_WITH_PREDEFINED_KEY);
        Assert.fail();
    }

    @Test
    public void testDecryptFailToLoadKey() throws ClientException {
        final StorageEncryptionManager manager = new MockStorageEncryptionManager(PREDEFINED_KEY_IV, new MockAES256KeyLoaderWithGetKeyError());
        try {
            manager.decrypt(TEXT_ENCRYPTED_BY_PREDEFINED_KEY);
            Assert.fail("decrypt() should throw an exception but it succeeds.");
        } catch (ClientException e){
            Assert.assertEquals(e.getErrorCode(), ErrorStrings.DECRYPTION_FAILED);
            Assert.assertEquals(((ClientException)e.getSuppressedException().get(0)).getErrorCode(),
                    MockAES256KeyLoaderWithGetKeyError.FAIL_TO_LOAD_KEY_ERROR);
        }
    }

    @Test
    public void testDecryptFailToLoadOneOfTheKeys() throws ClientException {
        final AES256KeyLoader failingKeyLoader = new MockAES256KeyLoaderWithGetKeyError();
        final AES256KeyLoader successKeyLoader = new MockAES256KeyLoader(PREDEFINED_KEY, PREDEFINED_KEY_IDENTIFIER);

        // Key order doesn't matter.
        final StorageEncryptionManager manager_failFirst = new MockStorageEncryptionManager(PREDEFINED_KEY_IV, null,
                new ArrayList<AbstractSecretKeyLoader>(){{
                    add(failingKeyLoader);
                    add(successKeyLoader);
                }});

        Assert.assertArrayEquals(TEXT_TO_BE_ENCRYPTED_WITH_PREDEFINED_KEY, manager_failFirst.decrypt(TEXT_ENCRYPTED_BY_PREDEFINED_KEY));

        final StorageEncryptionManager manager_failSecond = new MockStorageEncryptionManager(PREDEFINED_KEY_IV, null,
                new ArrayList<AbstractSecretKeyLoader>(){{
                    add(successKeyLoader);
                    add(failingKeyLoader);
                }});

        Assert.assertArrayEquals(TEXT_TO_BE_ENCRYPTED_WITH_PREDEFINED_KEY, manager_failSecond.decrypt(TEXT_ENCRYPTED_BY_PREDEFINED_KEY));
    }

    @Test
    public void testDecryptMatchingKeyNotFound() throws ClientException {
        final AES256KeyLoader decryptKeyLoader = new MockAES256KeyLoader();
        final AES256KeyLoader decryptKeyLoader_2 = new MockAES256KeyLoader();

        final StorageEncryptionManager manager = new MockStorageEncryptionManager(PREDEFINED_KEY_IV, null,
                new ArrayList<AbstractSecretKeyLoader>(){{
                    add(decryptKeyLoader);
                    add(decryptKeyLoader_2);
                }});

        try {
            /* This one is encrypted by {@link StorageEncryptionManagerTest#encryptionKey} */
            manager.decrypt(TEXT_ENCRYPTED_BY_PREDEFINED_KEY);
            Assert.fail("decrypt() should throw an exception but it succeeds.");
        } catch (final ClientException e){
            Assert.assertEquals(e.getErrorCode(), ErrorStrings.DECRYPTION_FAILED);
        }
    }

    @Test
    public void testDecryptWithMalformedKey() {
        try {
            final StorageEncryptionManager manager = new MockStorageEncryptionManager(PREDEFINED_KEY_IV, new MockAES256KeyLoader(PREDEFINED_KEY_MALFORMED, PREDEFINED_KEY_IDENTIFIER));
            manager.decrypt(TEXT_ENCRYPTED_BY_PREDEFINED_KEY);
            Assert.fail("decrypt() should throw an exception but it succeeds.");
        } catch (final ClientException e){
            Assert.assertEquals(e.getErrorCode(), ErrorStrings.DECRYPTION_FAILED);
            Assert.assertEquals(((ClientException)e.getSuppressedException().get(0)).getErrorCode(), HMAC_MISMATCH);
        }
    }

    @Test
    public void testDecryptUnencryptedText() throws ClientException {
        final StorageEncryptionManager manager = new MockStorageEncryptionManager(PREDEFINED_KEY_IV, new MockAES256KeyLoader(PREDEFINED_KEY, PREDEFINED_KEY_IDENTIFIER));
        Assert.assertArrayEquals(TEXT_TO_BE_ENCRYPTED_WITH_PREDEFINED_KEY, manager.decrypt(TEXT_TO_BE_ENCRYPTED_WITH_PREDEFINED_KEY));
    }

    @Test
    public void testIsEncryptedByThisKeyIdentifier_True() throws ClientException {
       Assert.assertTrue(
               StorageEncryptionManager.isEncryptedByThisKeyIdentifier(
                       TEXT_ENCRYPTED_BY_PREDEFINED_KEY,
                       PREDEFINED_KEY_IDENTIFIER));

    }

    @Test
    public void testIsEncryptedByThisKeyIdentifier_False() throws ClientException {
        Assert.assertFalse(
                StorageEncryptionManager.isEncryptedByThisKeyIdentifier(
                        TEXT_ENCRYPTED_BY_PREDEFINED_KEY,
                        ANDROID_WRAPPED_KEY_IDENTIFIER));
    }

    @Test
    public void testIsEncryptedByThisKeyIdentifier_StringNotEncrypted() throws ClientException {
        Assert.assertFalse(
                StorageEncryptionManager.isEncryptedByThisKeyIdentifier(
                        TEXT_TO_BE_ENCRYPTED_WITH_PREDEFINED_KEY,
                        PREDEFINED_KEY_IDENTIFIER));
    }

    @Test
    public void testIsEncryptedByThisKeyIdentifier_StringNotProperlyEncrypted() {
        Assert.assertFalse(
                StorageEncryptionManager.isEncryptedByThisKeyIdentifier(
                        "cE1".getBytes(ENCODING_UTF8),
                        PREDEFINED_KEY_IDENTIFIER));

        Assert.assertFalse(
                StorageEncryptionManager.isEncryptedByThisKeyIdentifier(
                        "c".getBytes(ENCODING_UTF8),
                        PREDEFINED_KEY_IDENTIFIER));
    }

    @Test
    public void testIsEncryptedByThisKeyIdentifier_EncodeVersionNotSupported() {
        Assert.assertFalse(
                StorageEncryptionManager.isEncryptedByThisKeyIdentifier(
                        EXPECTED_ENCRYPTED_TEXT_1_WITH_MALFORMED_ENCODE_VERSION,
                        PREDEFINED_KEY_IDENTIFIER));
    }

    @Test
    public void testDecryptedTruncatedString() {
        try {
            final StorageEncryptionManager manager = new MockStorageEncryptionManager(PREDEFINED_KEY_IV, new MockAES256KeyLoader(PREDEFINED_KEY, PREDEFINED_KEY_IDENTIFIER));
            final byte[] encryptedByteArray = TEXT_ENCRYPTED_BY_PREDEFINED_KEY;
            final byte[] truncatedByteArray = Arrays.copyOf(encryptedByteArray, encryptedByteArray.length / 2);
            manager.decrypt(truncatedByteArray);
            Assert.fail("decrypt() should throw an exception but it succeeds.");
        } catch (final ClientException e){
            Assert.assertEquals(e.getErrorCode(), ErrorStrings.DECRYPTION_FAILED);
            Assert.assertEquals(((ClientException)e.getSuppressedException().get(0)).getErrorCode(), HMAC_MISMATCH);
        }

        try {
            final StorageEncryptionManager manager = new MockStorageEncryptionManager(PREDEFINED_KEY_IV, new MockAES256KeyLoader(PREDEFINED_KEY, PREDEFINED_KEY_IDENTIFIER));
            manager.decrypt(new String(TEXT_ENCRYPTED_BY_PREDEFINED_KEY, ENCODING_UTF8).substring(0, 25).getBytes(ENCODING_UTF8));
            Assert.fail("decrypt() should throw an exception but it succeeds.");
        } catch (final ClientException e){
            Assert.assertEquals(e.getErrorCode(), ErrorStrings.DECRYPTION_FAILED);
            Assert.assertEquals(((ClientException)e.getSuppressedException().get(0)).getErrorCode(), DATA_MALFORMED);
        }
    }
}
