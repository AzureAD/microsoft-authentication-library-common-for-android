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
package com.microsoft.identity.common.crypto;

import static com.microsoft.identity.common.crypto.MockData.PREDEFINED_KEY;
import static com.microsoft.identity.common.crypto.MockData.TEXT_ENCRYPTED_BY_ANDROID_WRAPPED_KEY;
import static com.microsoft.identity.common.crypto.MockData.TEXT_ENCRYPTED_BY_PREDEFINED_KEY;
import static com.microsoft.identity.common.java.AuthenticationConstants.ENCODING_UTF8;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import com.microsoft.identity.common.adal.internal.AuthenticationSettings;
import com.microsoft.identity.common.java.crypto.key.ISecretKeyProvider;
import com.microsoft.identity.common.java.crypto.key.KeyUtil;
import com.microsoft.identity.common.java.crypto.key.PredefinedKeyProvider;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.List;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

@RunWith(RobolectricTestRunner.class)
public class AndroidAuthSdkStorageEncryptionManagerTest {

    final SecretKey secretKeyMock = new SecretKeySpec(PREDEFINED_KEY, "AES");
    final Context context = ApplicationProvider.getApplicationContext();

    @Before
    public void setUp() {
        AuthenticationSettings.INSTANCE.clearSecretKeysForTestCases();
    }

    @Test
    public void testGetEncryptionKey() {
        final AndroidAuthSdkStorageEncryptionManager manager = new AndroidAuthSdkStorageEncryptionManager(context);

        final ISecretKeyProvider provider = manager.getKeyProviderForEncryption();
        Assert.assertTrue(provider instanceof AndroidWrappedKeyProvider);
        Assert.assertNotEquals(KeyUtil.getKeyThumbPrint(secretKeyMock), KeyUtil.getKeyThumbPrint(provider));
    }

    @Test
    public void testGetEncryptionKey_PreDefinedKeyProvided() {
        AuthenticationSettings.INSTANCE.setSecretKey(PREDEFINED_KEY);
        final AndroidAuthSdkStorageEncryptionManager manager = new AndroidAuthSdkStorageEncryptionManager(context);

        final ISecretKeyProvider provider = manager.getKeyProviderForEncryption();
        Assert.assertTrue(provider instanceof PredefinedKeyProvider);
        Assert.assertEquals(KeyUtil.getKeyThumbPrint(secretKeyMock), KeyUtil.getKeyThumbPrint(provider));
    }

    /**
     * Given a data encrypted with the wrapped key,
     * try getting a decryption key when a predefined key is NOT provided.
     */
    @Test
    public void testGetDecryptionKey_ForDataEncryptedWithKeyStoreKey() {
        final AndroidAuthSdkStorageEncryptionManager manager = new AndroidAuthSdkStorageEncryptionManager(context);
        final List<ISecretKeyProvider> keyproviderList = manager.getKeyProviderForDecryption(TEXT_ENCRYPTED_BY_ANDROID_WRAPPED_KEY);

        Assert.assertEquals(1, keyproviderList.size());
        Assert.assertTrue(keyproviderList.get(0) instanceof AndroidWrappedKeyProvider);
        Assert.assertNotEquals(KeyUtil.getKeyThumbPrint(secretKeyMock), KeyUtil.getKeyThumbPrint(keyproviderList.get(0)));
    }

    /**
     * Given a data encrypted with the wrapped key,
     * try getting a decryption key when a predefined key is provided.
     */
    @Test
    public void testGetDecryptionKey_ForDataEncryptedWithKeyStoreKey_PreDefinedKeyProvided() {
        AuthenticationSettings.INSTANCE.setSecretKey(PREDEFINED_KEY);
        final AndroidAuthSdkStorageEncryptionManager manager = new AndroidAuthSdkStorageEncryptionManager(context);
        final List<ISecretKeyProvider> keyproviderList = manager.getKeyProviderForDecryption(TEXT_ENCRYPTED_BY_ANDROID_WRAPPED_KEY);

        Assert.assertEquals(1, keyproviderList.size());
        Assert.assertTrue(keyproviderList.get(0) instanceof AndroidWrappedKeyProvider);
        Assert.assertNotEquals(KeyUtil.getKeyThumbPrint(secretKeyMock), KeyUtil.getKeyThumbPrint(keyproviderList.get(0)));
    }

    /**
     * Given a data encrypted with the predefined key,
     * try getting a decryption key when a predefined key is NOT provided.
     */
    @Test
    public void testGetDecryptionKey_ForDataEncryptedWithPreDefinedKey() {
        final AndroidAuthSdkStorageEncryptionManager manager = new AndroidAuthSdkStorageEncryptionManager(context);
        try {
            final List<ISecretKeyProvider> keyproviderList = manager.getKeyProviderForDecryption(TEXT_ENCRYPTED_BY_PREDEFINED_KEY);
        } catch (IllegalStateException ex) {
            Assert.assertEquals(
                    "Cipher Text is encrypted by USER_PROVIDED_KEY_IDENTIFIER, but mPredefinedKeyprovider is null.",
                    ex.getMessage());
        }
    }

    public void testGetDecryptionKey_ForUnencryptedText_returns_empty_keyprovider() {
        AuthenticationSettings.INSTANCE.setIgnoreKeyproviderNotFoundError(false);
        final AndroidAuthSdkStorageEncryptionManager manager = new AndroidAuthSdkStorageEncryptionManager(context);
        final List<ISecretKeyProvider> keyproviderList = manager.getKeyProviderForDecryption("Unencrypted".getBytes(ENCODING_UTF8));
        Assert.assertEquals(0, keyproviderList.size());
    }

    /**
     * Given a data encrypted with the predefined key,
     * try getting a decryption key when a predefined key is provided.
     */
    @Test
    public void testGetDecryptionKey_ForDataEncryptedWithPreDefinedKey_PreDefinedKeyProvided() {
        AuthenticationSettings.INSTANCE.setSecretKey(PREDEFINED_KEY);
        final AndroidAuthSdkStorageEncryptionManager manager = new AndroidAuthSdkStorageEncryptionManager(context);
        final List<ISecretKeyProvider> keyproviderList = manager.getKeyProviderForDecryption(TEXT_ENCRYPTED_BY_PREDEFINED_KEY);

        Assert.assertEquals(1, keyproviderList.size());
        Assert.assertTrue(keyproviderList.get(0) instanceof PredefinedKeyProvider);
        Assert.assertEquals(KeyUtil.getKeyThumbPrint(secretKeyMock), KeyUtil.getKeyThumbPrint(keyproviderList.get(0)));
    }
}
