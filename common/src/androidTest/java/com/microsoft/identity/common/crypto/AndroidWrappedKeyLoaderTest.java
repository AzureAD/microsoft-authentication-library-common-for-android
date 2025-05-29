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

import androidx.test.core.app.ApplicationProvider;

import com.microsoft.identity.common.adal.internal.AuthenticationSettings;
import com.microsoft.identity.common.internal.util.AndroidKeyStoreUtil;
import com.microsoft.identity.common.java.crypto.key.AES256KeyLoader;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.util.CachedData;
import com.microsoft.identity.common.java.util.FileUtil;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;

import javax.crypto.SecretKey;
import javax.security.auth.x500.X500Principal;

import static com.microsoft.identity.common.java.exception.ClientException.INVALID_KEY;

public class AndroidWrappedKeyLoaderTest {

    final Context context = ApplicationProvider.getApplicationContext();
    final String MOCK_KEY_ALIAS = "MOCK_KEY_ALIAS";
    final String MOCK_KEY_FILE_PATH = "MOCK_KEY_FILE_PATH";
    final int TEST_LOOP = 100;

    @Before
    public void setUp() throws Exception {
        // Everything is on clean slate.
        AuthenticationSettings.INSTANCE.clearSecretKeysForTestCases();
        AndroidKeyStoreUtil.deleteKey(MOCK_KEY_ALIAS);
        FileUtil.deleteFile(getKeyFile());
    }

    private File getKeyFile() {
        return new File(
                context.getDir(context.getPackageName(), Context.MODE_PRIVATE),
                MOCK_KEY_FILE_PATH);
    }

    @Test
    public void testRSAKeyStoreOperations() throws ClientException {
        testKeyStoreOperation("RSA");
    }

    private void testKeyStoreOperation(String keyAlgo) throws ClientException {
        // Write
        final KeyPair generatedKeyPair = AndroidKeyStoreUtil.generateKeyPair(
                keyAlgo,
                getMockKeyPairGeneratorSpec(MOCK_KEY_ALIAS));

        // Read
        final KeyPair keyPairReadFromKeyStore = AndroidKeyStoreUtil.readKey(MOCK_KEY_ALIAS);

        Assert.assertArrayEquals(generatedKeyPair.getPrivate().getEncoded(), keyPairReadFromKeyStore.getPrivate().getEncoded());
        Assert.assertArrayEquals(generatedKeyPair.getPublic().getEncoded(), keyPairReadFromKeyStore.getPublic().getEncoded());

        // Delete
        AndroidKeyStoreUtil.deleteKey(MOCK_KEY_ALIAS);

        // Read again - should be empty.
        Assert.assertNull(AndroidKeyStoreUtil.readKey(MOCK_KEY_ALIAS));
    }

    private AlgorithmParameterSpec getMockKeyPairGeneratorSpec(final String alias) {
        final Date startDate = new Date();
        final Date endDate = new Date(startDate.getTime() + 1000000);

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
        final AndroidWrappedKeyLoader keyLoader = new AndroidWrappedKeyLoader(MOCK_KEY_ALIAS, MOCK_KEY_FILE_PATH, context);
        final SecretKey secretKey = keyLoader.generateRandomKey();

        Assert.assertEquals(AES256KeyLoader.AES_ALGORITHM, secretKey.getAlgorithm());
    }

    @Test
    public void testReadKeyDirectly() throws ClientException {
        final AndroidWrappedKeyLoader keyLoader = initKeyLoaderWithKeyEntry();
        final SecretKey secretKey = keyLoader.getKey();
        final SecretKey storedSecretKey = keyLoader.readSecretKeyFromStorage();

        // They're not the same object!
        Assert.assertNotSame(secretKey, storedSecretKey);

        Assert.assertEquals(AES256KeyLoader.AES_ALGORITHM, secretKey.getAlgorithm());
        Assert.assertEquals(AES256KeyLoader.AES_ALGORITHM, storedSecretKey.getAlgorithm());

        Assert.assertNotNull(secretKey.getEncoded());
        Assert.assertNotNull(storedSecretKey.getEncoded());
        Assert.assertArrayEquals(secretKey.getEncoded(), storedSecretKey.getEncoded());
        Assert.assertEquals(secretKey.getFormat(), storedSecretKey.getFormat());
    }

    @Test
    public void testLoadKey() throws ClientException {
        // Nothing exists. This load key function should generate a key if the key hasn't exist.
        Assert.assertNull(AndroidKeyStoreUtil.readKey(MOCK_KEY_ALIAS));
        Assert.assertNull(FileUtil.readFromFile(getKeyFile(), AndroidWrappedKeyLoader.KEY_FILE_SIZE));

        final AndroidWrappedKeyLoader keyLoader = new AndroidWrappedKeyLoader(MOCK_KEY_ALIAS, MOCK_KEY_FILE_PATH, context);
        final SecretKey secretKey = keyLoader.getKey();
        final CachedData<SecretKey> cachedKey = keyLoader.getKeyCache();
        Assert.assertNotNull(cachedKey);
        final SecretKey key = cachedKey.getData();
        Assert.assertNotNull(key);
        Assert.assertEquals(AES256KeyLoader.AES_ALGORITHM, secretKey.getAlgorithm());
        Assert.assertArrayEquals(secretKey.getEncoded(), key.getEncoded());
        Assert.assertEquals(secretKey.getFormat(), key.getFormat());
    }

    @Test
    public void testLoadKeyFromCorruptedFile_TruncatedExistingKey() throws ClientException {
        // Create a new Keystore-wrapped key.
        final AndroidWrappedKeyLoader keyLoader = new AndroidWrappedKeyLoader(MOCK_KEY_ALIAS, MOCK_KEY_FILE_PATH, context);
        keyLoader.generateRandomKey();

        final byte[] wrappedKey = FileUtil.readFromFile(getKeyFile(), AndroidWrappedKeyLoader.KEY_FILE_SIZE);
        Assert.assertNotNull(wrappedKey);

        // Overwrite the key file with corrupted data.
        FileUtil.writeDataToFile(Arrays.copyOfRange(wrappedKey, 0, wrappedKey.length/2), getKeyFile());

        // It should fail to read, with an exception, and everything should be wiped.
        try{
            keyLoader.readSecretKeyFromStorage();
            Assert.fail();
        } catch (final ClientException e){
            Assert.assertEquals(INVALID_KEY, e.getErrorCode());
        }

        // Everything should be wiped.
        Assert.assertFalse(getKeyFile().exists());

        // the next read should be unblocked.
        Assert.assertNull(keyLoader.readSecretKeyFromStorage());
    }

    @Test
    public void testLoadKeyFromCorruptedFile_InjectGarbage() throws ClientException {
        // Create a new Keystore-wrapped key.
        final AndroidWrappedKeyLoader keyLoader = new AndroidWrappedKeyLoader(MOCK_KEY_ALIAS, MOCK_KEY_FILE_PATH, context);
        keyLoader.generateRandomKey();

        final byte[] wrappedKey = FileUtil.readFromFile(getKeyFile(), AndroidWrappedKeyLoader.KEY_FILE_SIZE);
        Assert.assertNotNull(wrappedKey);

        // Overwrite the key file with corrupted data.
        FileUtil.writeDataToFile(new byte[]{10, 20, 30, 40}, getKeyFile());

        // It should fail to read, with an exception, and everything should be wiped.
        try{
            keyLoader.readSecretKeyFromStorage();
            Assert.fail();
        } catch (ClientException e){
            Assert.assertEquals(INVALID_KEY, e.getErrorCode());
        }

        // Everything should be wiped.
        Assert.assertFalse(getKeyFile().exists());

        // the next read should be unblocked.
        Assert.assertNull(keyLoader.readSecretKeyFromStorage());
    }

    // 1s With Google Pixel XL, OS Version 29 (100 loop)
    @Test
    @Ignore
    public void testPerf_WithCachedKey() throws ClientException {
        final AndroidWrappedKeyLoader keyLoader = new AndroidWrappedKeyLoader(MOCK_KEY_ALIAS, MOCK_KEY_FILE_PATH, context);

        long timeStartLoop = System.nanoTime();
        for (int i = 0; i < TEST_LOOP; i++) {
            keyLoader.getKey();
        }
        long timeFinishLoop = System.nanoTime();

        System.out.println("Time: " + (timeFinishLoop - timeStartLoop));
    }

    // 23s With Google Pixel XL, OS Version 29 (100 loop)
    @Test
    @Ignore
    public void testPerf_NoCachedKey() throws ClientException {
        final AndroidWrappedKeyLoader keyLoader = new AndroidWrappedKeyLoader(MOCK_KEY_ALIAS, MOCK_KEY_FILE_PATH, context);

        long timeStartLoopNotCached = System.nanoTime();
        for (int i = 0; i < 100; i++) {
            keyLoader.clearKeyCache();
            keyLoader.getKey();
        }
        long timeFinishLoopNotCached = System.nanoTime();

        System.out.println("Time: " + (timeFinishLoopNotCached - timeStartLoopNotCached));
    }

    /**
     * This test is simulating the drawback of having an in-memory key cache.
     */
    @Test
    public void testLoadDeletedKeyStoreKey() throws ClientException {
        final AndroidWrappedKeyLoader keyLoader = initKeyLoaderWithKeyEntry();

        AndroidKeyStoreUtil.deleteKey(MOCK_KEY_ALIAS);

        // Cached key also be wiped.
        final CachedData<SecretKey> key = keyLoader.getKeyCache();
        Assert.assertNull(key);
    }

    @Test
    public void testLoadDeletedKeyFile() throws ClientException {
        final AndroidWrappedKeyLoader keyLoader = initKeyLoaderWithKeyEntry();

        FileUtil.deleteFile(getKeyFile());

        // Cached key also be wiped.
        final CachedData<SecretKey> key = keyLoader.getKeyCache();
        Assert.assertNull(key);
    }

    @Test
    public void testGetKey_CacheWorks() throws ClientException {
        final AndroidWrappedKeyLoader keyLoader = initKeyLoaderWithKeyEntry();

        // First call should read from storage
        final SecretKey key1 = keyLoader.getKey();
        // Second call should return cached value
        final SecretKey key2 = keyLoader.getKey();

        // Same key should be returned (cached)
        Assert.assertSame(key1, key2);
    }

    @Test
    public void testGetKey_DifferentAliasGetsDifferentCache() throws ClientException {
        final AndroidWrappedKeyLoader keyLoader1 = new AndroidWrappedKeyLoader(MOCK_KEY_ALIAS, MOCK_KEY_FILE_PATH, context);
        final AndroidWrappedKeyLoader keyLoader2 = new AndroidWrappedKeyLoader(MOCK_KEY_ALIAS + "2", MOCK_KEY_FILE_PATH + "2", context);

        // Generate keys for both loaders
        final SecretKey key1 = keyLoader1.getKey();
        final SecretKey key2 = keyLoader2.getKey();

        // Keys should be different since they have different aliases
        Assert.assertNotSame(key1, key2);
        Assert.assertFalse(Arrays.equals(key1.getEncoded(), key2.getEncoded()));
    }

    @Test
    public void testGetKey_SameAliasSharesCache() throws ClientException {
        final AndroidWrappedKeyLoader keyLoader1 = new AndroidWrappedKeyLoader(MOCK_KEY_ALIAS, MOCK_KEY_FILE_PATH, context);
        final AndroidWrappedKeyLoader keyLoader2 = new AndroidWrappedKeyLoader(MOCK_KEY_ALIAS, MOCK_KEY_FILE_PATH + "different", context);

        // Get key from first loader
        final SecretKey key1 = keyLoader1.getKey();
        // Second loader should get same key from cache despite different file path
        final SecretKey key2 = keyLoader2.getKey();

        // Keys should be the same since they share the same alias
        Assert.assertSame(key1, key2);
    }

    @Test
    public void testGetKey_ConcurrentAccess() throws Exception {
        final AndroidWrappedKeyLoader keyLoader = new AndroidWrappedKeyLoader(MOCK_KEY_ALIAS, MOCK_KEY_FILE_PATH, context);
        final int threadCount = 10;
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch finishLatch = new CountDownLatch(threadCount);
        final Set<SecretKey> keys = Collections.synchronizedSet(new HashSet<>());
        final List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());

        // Create multiple threads that will try to get the key simultaneously
        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                try {
                    startLatch.await(); // Wait for all threads to be ready
                    keys.add(keyLoader.getKey());
                } catch (Exception e) {
                    exceptions.add(e);
                } finally {
                    finishLatch.countDown();
                }
            }).start();
        }

        startLatch.countDown(); // Start all threads
        finishLatch.await(2, TimeUnit.SECONDS); // Wait for all threads to finish

        // Check no exceptions occurred
        Assert.assertTrue("Exceptions occurred: " + exceptions, exceptions.isEmpty());
        // All threads should get the same key instance
        Assert.assertEquals("All threads should get the same key", 1, keys.size());
    }

    @Test
    public void testGetKey_DeletedKeyRegenerated() throws ClientException {
        final AndroidWrappedKeyLoader keyLoader = initKeyLoaderWithKeyEntry();

        // Get initial key
        final SecretKey initialKey = keyLoader.getKey();

        // Delete the key
        keyLoader.deleteSecretKeyFromStorage();

        // Get key again - should generate a new one
        final SecretKey newKey = keyLoader.getKey();

        // Keys should be different
        Assert.assertNotSame(initialKey, newKey);
        Assert.assertFalse(Arrays.equals(initialKey.getEncoded(), newKey.getEncoded()));
    }

    private AndroidWrappedKeyLoader initKeyLoaderWithKeyEntry() throws ClientException {
        final AndroidWrappedKeyLoader keyLoader = new AndroidWrappedKeyLoader(MOCK_KEY_ALIAS, MOCK_KEY_FILE_PATH, context);
        final SecretKey key = keyLoader.getKey();
        Assert.assertNotNull(key);
        Assert.assertNotNull(keyLoader.getKeyCache());
        return keyLoader;
    }
}
