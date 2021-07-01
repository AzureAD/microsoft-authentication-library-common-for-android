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

import android.content.Context;
import android.security.KeyPairGeneratorSpec;

import androidx.test.platform.app.InstrumentationRegistry;

import com.microsoft.identity.common.adal.internal.AuthenticationSettings;
import com.microsoft.identity.common.internal.util.FileUtil;
import com.microsoft.identity.common.internal.util.AndroidKeyStoreUtil;
import com.microsoft.identity.common.java.crypto.key.AES256KeyLoader;
import com.microsoft.identity.common.java.exception.ClientException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Date;

import javax.crypto.SecretKey;
import javax.security.auth.x500.X500Principal;

public class AndroidWrappedKeyLoaderTest {

    final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    final String MOCK_KEY_ALIAS = "MOCK_KEY_ALIAS";

    @Before
    public void setUp() throws Exception {
        // Everything is on clean slate.
        AuthenticationSettings.INSTANCE.clearSecretKeysForTestCases();
        AndroidKeyStoreUtil.deleteKey(MOCK_KEY_ALIAS);
        FileUtil.deleteFile(context, AndroidWrappedKeyLoader.KEY_FILE_PATH);
    }

    @Test
    public void testKeyStoreOperations() throws ClientException {
        final String alias = "SOME_ALIAS";
        final String keyAlgo = "RSA";
        final String cipherAlgo = "RSA/ECB/PKCS1Padding";

        // Write
        final KeyPair generatedKeyPair = AndroidKeyStoreUtil.generateKeyPair(
                keyAlgo,
                getMockKeyPairGeneratorSpec(alias));

        // Read
        final KeyPair keyPairReadFromKeyStore = AndroidKeyStoreUtil.readKey(alias);

        Assert.assertEquals(generatedKeyPair.getPrivate(), keyPairReadFromKeyStore.getPrivate());
        Assert.assertEquals(generatedKeyPair.getPublic(), keyPairReadFromKeyStore.getPublic());

        // Delete
        AndroidKeyStoreUtil.deleteKey(alias);

        // Read again - should be empty.
        Assert.assertNull(AndroidKeyStoreUtil.readKey(alias));
    }

    private AlgorithmParameterSpec getMockKeyPairGeneratorSpec(final String alias){
        final Date startDate = new Date();
        final Date endDate =  new Date(startDate.getTime() + 1000000);

        return new KeyPairGeneratorSpec.Builder(context)
                .setAlias(alias)
                .setSubject(new X500Principal("CN=SOME_CN, OU=SOME_OU"))
                .setSerialNumber(BigInteger.ONE)
                .setStartDate(startDate)
                .setEndDate(endDate)
                .build();
    }

    @Test
    public void testGenerateKey() throws ClientException {
        final AndroidWrappedKeyLoader loader = new AndroidWrappedKeyLoader(MOCK_KEY_ALIAS, context, null);
        final SecretKey secretKey = loader.generateRandomKey();

        Assert.assertEquals(AES256KeyLoader.AES_ALGORITHM, secretKey.getAlgorithm());
    }

    @Test
    public void testReadKeyDirectly() throws ClientException {
        final AndroidWrappedKeyLoader loader = initKeyLoaderWithKeyEntry();
        final SecretKey secretKey = loader.getKey();
        final SecretKey storedSecretKey = loader.readSecretKeyFromStorage();

        // They're not the same object!
        Assert.assertNotSame(secretKey, storedSecretKey);

        Assert.assertEquals(AES256KeyLoader.AES_ALGORITHM, secretKey.getAlgorithm());
        Assert.assertEquals(AES256KeyLoader.AES_ALGORITHM , storedSecretKey.getAlgorithm());

        Assert.assertArrayEquals(secretKey.getEncoded(), storedSecretKey.getEncoded());
        Assert.assertEquals(secretKey.getFormat(), storedSecretKey.getFormat());
    }

    @Test
    public void testLoadKey() throws ClientException {
        // Nothing exists. This load key function should generate a key if the key hasn't exist.
        Assert.assertNull(AndroidKeyStoreUtil.readKey(MOCK_KEY_ALIAS));
        Assert.assertNull(FileUtil.readFromFile(context,
                AndroidWrappedKeyLoader.KEY_FILE_PATH,
                AndroidWrappedKeyLoader.KEY_FILE_SIZE));

        final AndroidWrappedKeyLoader loader = new AndroidWrappedKeyLoader(MOCK_KEY_ALIAS, context, null);
        final SecretKey secretKey = loader.getKey();
        Assert.assertNotNull(loader.mCachedKey);

        Assert.assertEquals(AES256KeyLoader.AES_ALGORITHM, secretKey.getAlgorithm());
        Assert.assertArrayEquals(secretKey.getEncoded(), loader.mCachedKey.getEncoded());
        Assert.assertEquals(secretKey.getFormat(), loader.mCachedKey.getFormat());
    }

    @Test
    public void testLoadDeletedKeyStoreKey() throws ClientException {
        final AndroidWrappedKeyLoader loader = initKeyLoaderWithKeyEntry();

        AndroidKeyStoreUtil.deleteKey(MOCK_KEY_ALIAS);
        // Cached key should not be wiped - yet, since we delete directly in keychain.
        Assert.assertNotNull(loader.mCachedKey);

        final SecretKey storedSecretKey = loader.readSecretKeyFromStorage();
        Assert.assertNull(loader.mCachedKey);
        Assert.assertNull(storedSecretKey);
    }

    @Test
    public void testLoadDeletedKeyFile() throws ClientException {
        final AndroidWrappedKeyLoader loader = initKeyLoaderWithKeyEntry();

        FileUtil.deleteFile(context, AndroidWrappedKeyLoader.KEY_FILE_PATH);
        // Cached key should not be wiped - yet, since we delete the file directly.
        Assert.assertNotNull(loader.mCachedKey);

        final SecretKey storedSecretKey = loader.readSecretKeyFromStorage();
        Assert.assertNull(loader.mCachedKey);
        Assert.assertNull(storedSecretKey);
    }

    private AndroidWrappedKeyLoader initKeyLoaderWithKeyEntry() throws ClientException {
        final AndroidWrappedKeyLoader loader = new AndroidWrappedKeyLoader(MOCK_KEY_ALIAS, context, null);
        final SecretKey key = loader.getKey();
        Assert.assertNotNull(key);
        Assert.assertNotNull(loader.mCachedKey);
        return loader;
    }
}
