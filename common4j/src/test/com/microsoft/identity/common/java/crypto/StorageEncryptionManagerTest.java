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
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.exception.ErrorStrings;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;

import static com.microsoft.identity.common.java.AuthenticationConstants.ENCODING_UTF8;
import static com.microsoft.identity.common.java.exception.ClientException.DATA_MALFORMED;
import static com.microsoft.identity.common.java.exception.ClientException.HMAC_MISMATCH;

public class StorageEncryptionManagerTest {

    // Value extracted from the legacy StorageHelper.
    // Data Set 1
    final String textToEncrypt = "TEST_TEXT_TO_ENCRYPT";
    final byte[] encryptionKey = new byte[]{22, 78, -69, -66, 84, -65, 119, -9, -34, -80, 60, 67, -12, -117, 86, -47, -84, -24, -18, 121, 70, 32, -110, 51, -93, -10, -93, -110, 124, -68, -42, -119};
    final byte[] encryptionKey_Malformed = new byte[]{22, 78, -75, -66, 84, -65, 119, -9, -34, -80, 60, 67, -12, -117, 86, -47, -84, -24, -18, 121, 70, 32, -110, 51, -93, -10, -93, -110, 124, -68, -42, -119};
    final byte[] iv = new byte[]{15, -63, 107, 116, -73, -68, 101, 37, -1, 21, -27, 53, 106, -106, 10, -78};
    final String expectedEncryptedText = "cE1VTAwMeHz7BCCH/27kWvMYYMsGamVenQk6w+YJ14JnFBi6fJ1D8FrdLe8ZSX/FeU1apYKsj9d1fNoMD4kR62XfPMytA3P2XpXEQtkblP6F6A5R74F";
    final String keyIdentifier_1 = "U001";
    final String expectedEncryptedText_WithMalformedEncodeVersion = "cD1VTAwMeHz7BCCH/27kWvMYYMsGamVenQk6w+YJ14JnFBi6fJ1D8FrdLe8ZSX/FeU1apYKsj9d1fNoMD4kR62XfPMytA3P2XpXEQtkblP6F6A5R74F";

    // Data Set 2
    final String textToEncrypt_2 = "SECOND_TEXT_TO_ENCRYPT";
    final byte[] encryptionKey_2 = new byte[]{122, 75, 49, 112, 36, 126, 5, 35, 46, 45, -61, -61, 55, 105, 9, -123, 115, 27, 35, -54, -49, 14, -16, 49, -74, -88, -29, -15, -33, -13, 100, 118};
    final byte[] iv_2 = new byte[]{63, 54, -115, 111, -46, 66, -40, -9, -53, -56, -8, -65, 112, -101, -116, -1};
    final String expectedEncryptedText_2 = "cE1QTAwMTDvTopC+ds4Wgm7IbhnZl1pEVWU+vt7dp0h098822NjPzaNb9JC2PfLyPi/cJuM/wKGYN9YpvRP+BA+i0DlGdCb7nOQ/fmYfjpNq9aj26Kh";
    final String keyIdentifier_2 = "A001";

    @Test
    public void testEncrypt() throws ClientException {
        final StorageEncryptionManager manager = new MockStorageEncryptionManager(iv, new MockAES256KeyLoader(encryptionKey, keyIdentifier_1));
        Assert.assertEquals(expectedEncryptedText, manager.encrypt(textToEncrypt));

        final StorageEncryptionManager manager_2 = new MockStorageEncryptionManager(iv_2, new MockAES256KeyLoader(encryptionKey_2, keyIdentifier_2));
        Assert.assertEquals(expectedEncryptedText_2, manager_2.encrypt(textToEncrypt_2));
    }

    @Test
    public void testDecrypt() throws ClientException {
        final StorageEncryptionManager manager = new MockStorageEncryptionManager(iv, new MockAES256KeyLoader(encryptionKey, keyIdentifier_1));
        Assert.assertEquals(textToEncrypt, manager.decrypt(expectedEncryptedText));

        final StorageEncryptionManager manager_2 = new MockStorageEncryptionManager(iv_2, new MockAES256KeyLoader(encryptionKey_2, keyIdentifier_2));
        Assert.assertEquals(textToEncrypt_2, manager_2.decrypt(expectedEncryptedText_2));
    }

    @Test(expected = IllegalStateException.class)
    public void testEncryptNoKeyLoader() throws ClientException {
        final StorageEncryptionManager manager = new MockStorageEncryptionManager(iv, null);
        manager.encrypt(textToEncrypt);
        Assert.fail();
    }

    @Test(expected = IllegalStateException.class)
    public void testDecryptNoKeyLoader() throws ClientException {
        final StorageEncryptionManager manager = new MockStorageEncryptionManager(iv, null, null);
        manager.decrypt(expectedEncryptedText);
        Assert.fail();
    }

    @Test(expected = IllegalStateException.class)
    public void testDecryptNullKeyLoader() throws ClientException {
        final StorageEncryptionManager manager = new MockStorageEncryptionManager(iv, null,
                new ArrayList<AES256KeyLoader>() {{
                    add(null);
                }});
        manager.decrypt(expectedEncryptedText);
        Assert.fail();
    }

    @Test(expected = ClientException.class)
    public void testEncryptFailToLoadKey() throws ClientException {
        final StorageEncryptionManager manager = new MockStorageEncryptionManager(iv, new MockAES256KeyLoaderWithGetKeyError());
        manager.encrypt(textToEncrypt);
        Assert.fail();
    }

    @Test
    public void testDecryptFailToLoadKey() throws ClientException {
        final StorageEncryptionManager manager = new MockStorageEncryptionManager(iv, new MockAES256KeyLoaderWithGetKeyError());
        try {
            manager.decrypt(expectedEncryptedText);
            Assert.fail();
        } catch (ClientException e){
            Assert.assertEquals(e.getErrorCode(), ErrorStrings.DECRYPTION_FAILED);
            Assert.assertEquals(((ClientException)e.getSuppressed()[0]).getErrorCode(),
                    MockAES256KeyLoaderWithGetKeyError.FAIL_TO_LOAD_KEY_ERROR);
        }
    }

    @Test
    public void testDecryptFailToLoadOneOfTheKeys() throws ClientException {
        final AES256KeyLoader failingKeyLoader = new MockAES256KeyLoaderWithGetKeyError();
        final AES256KeyLoader successKeyLoader = new MockAES256KeyLoader(encryptionKey, keyIdentifier_1);

        // Key order doesn't matter.
        final StorageEncryptionManager manager_failFirst = new MockStorageEncryptionManager(iv, null,
                new ArrayList<AES256KeyLoader>(){{
                    add(failingKeyLoader);
                    add(successKeyLoader);
                }});

        Assert.assertEquals(textToEncrypt, manager_failFirst.decrypt(expectedEncryptedText));

        final StorageEncryptionManager manager_failSecond = new MockStorageEncryptionManager(iv, null,
                new ArrayList<AES256KeyLoader>(){{
                    add(successKeyLoader);
                    add(failingKeyLoader);
                }});

        Assert.assertEquals(textToEncrypt, manager_failSecond.decrypt(expectedEncryptedText));
    }

    @Test
    public void testDecryptMatchingKeyNotFound() throws ClientException {
        final AES256KeyLoader decryptKeyLoader = new MockAES256KeyLoader();
        final AES256KeyLoader decryptKeyLoader_2 = new MockAES256KeyLoader();

        final StorageEncryptionManager manager = new MockStorageEncryptionManager(iv, null,
                new ArrayList<AES256KeyLoader>(){{
                    add(decryptKeyLoader);
                    add(decryptKeyLoader_2);
                }});

        try {
            /** This one is encrypted by {@link StorageEncryptionManagerTest#encryptionKey} */
            manager.decrypt(expectedEncryptedText);
            Assert.fail();
        } catch (ClientException e){
            Assert.assertEquals(e.getErrorCode(), ErrorStrings.DECRYPTION_FAILED);
        }
    }

    @Test
    public void testDecryptWithMalformedKey() {
        try {
            final StorageEncryptionManager manager = new MockStorageEncryptionManager(iv, new MockAES256KeyLoader(encryptionKey_Malformed, keyIdentifier_1));
            manager.decrypt(expectedEncryptedText);
            Assert.fail();
        } catch (ClientException e){
            Assert.assertEquals(e.getErrorCode(), ErrorStrings.DECRYPTION_FAILED);
            Assert.assertEquals(((ClientException)e.getSuppressed()[0]).getErrorCode(), HMAC_MISMATCH);
        }
    }

    @Test
    public void testDecryptUnencryptedText() throws ClientException {
        final StorageEncryptionManager manager = new MockStorageEncryptionManager(iv, new MockAES256KeyLoader(encryptionKey, keyIdentifier_1));
        Assert.assertEquals(textToEncrypt, manager.decrypt(textToEncrypt));
    }

    @Test
    public void testIsEncryptedByThisKeyIdentifier_True() throws ClientException {
       Assert.assertTrue(
               StorageEncryptionManager.isEncryptedByThisKeyIdentifier(
                       expectedEncryptedText.getBytes(ENCODING_UTF8),
                       keyIdentifier_1));

    }

    @Test
    public void testIsEncryptedByThisKeyIdentifier_False() throws ClientException {
        Assert.assertFalse(
                StorageEncryptionManager.isEncryptedByThisKeyIdentifier(
                        expectedEncryptedText.getBytes(ENCODING_UTF8),
                        keyIdentifier_2));
    }

    @Test
    public void testIsEncryptedByThisKeyIdentifier_StringNotEncrypted() throws ClientException {
        Assert.assertFalse(
                StorageEncryptionManager.isEncryptedByThisKeyIdentifier(
                        textToEncrypt.getBytes(ENCODING_UTF8),
                        keyIdentifier_1));
    }

    @Test
    public void testIsEncryptedByThisKeyIdentifier_StringNotProperlyEncrypted() {
        Assert.assertFalse(
                StorageEncryptionManager.isEncryptedByThisKeyIdentifier(
                        "cE1".getBytes(ENCODING_UTF8),
                        keyIdentifier_1));

        Assert.assertFalse(
                StorageEncryptionManager.isEncryptedByThisKeyIdentifier(
                        "c".getBytes(ENCODING_UTF8),
                        keyIdentifier_1));
    }

    @Test
    public void testIsEncryptedByThisKeyIdentifier_EncodeVersionNotSupported() {
        Assert.assertFalse(
                StorageEncryptionManager.isEncryptedByThisKeyIdentifier(
                        expectedEncryptedText_WithMalformedEncodeVersion.getBytes(ENCODING_UTF8),
                        keyIdentifier_1));
    }

    @Test
    public void testDecryptedTruncatedString() {
        try {
            final StorageEncryptionManager manager = new MockStorageEncryptionManager(iv, new MockAES256KeyLoader(encryptionKey, keyIdentifier_1));
            final byte[] encryptedByteArray = expectedEncryptedText.getBytes(ENCODING_UTF8);
            final byte[] truncatedByteArray = Arrays.copyOf(encryptedByteArray, encryptedByteArray.length / 2);
            manager.decrypt(new String(truncatedByteArray, ENCODING_UTF8));
            Assert.fail();
        } catch (ClientException e){
            Assert.assertEquals(e.getErrorCode(), ErrorStrings.DECRYPTION_FAILED);
            Assert.assertEquals(((ClientException)e.getSuppressed()[0]).getErrorCode(), HMAC_MISMATCH);
        }

        try {
            final StorageEncryptionManager manager = new MockStorageEncryptionManager(iv, new MockAES256KeyLoader(encryptionKey, keyIdentifier_1));
            manager.decrypt(expectedEncryptedText.substring(0, 25));
            Assert.fail();
        } catch (ClientException e){
            Assert.assertEquals(e.getErrorCode(), ErrorStrings.DECRYPTION_FAILED);
            Assert.assertEquals(((ClientException)e.getSuppressed()[0]).getErrorCode(), DATA_MALFORMED);
        }
    }
}
