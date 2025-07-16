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

import static com.microsoft.identity.common.java.exception.ClientException.INVALID_KEY;

import android.content.Context;
import android.security.KeyPairGeneratorSpec;

import androidx.test.core.app.ApplicationProvider;

import com.microsoft.identity.common.adal.internal.AuthenticationSettings;
import com.microsoft.identity.common.internal.util.AndroidKeyStoreUtil;
import com.microsoft.identity.common.java.crypto.key.ISecretKeyProvider;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.util.FileUtil;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.File;
import java.lang.reflect.Constructor;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;

import javax.crypto.SecretKey;
import javax.security.auth.x500.X500Principal;

@RunWith(Parameterized.class)
public class AndroidWrappedKeyProviderTest {


    @Parameterized.Parameter(0)
    public String providerName;

    @Parameterized.Parameter(1)
    public Class<? extends ISecretKeyProvider> providerClass;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                {"KeyStoreBackedSecretKeyProvider", KeyStoreBackedSecretKeyProvider.class},
                {"AndroidWrappedKeyProvider", AndroidWrappedKeyProvider.class}
                // Add other implementations here
        });
    }

    private ISecretKeyProvider createProvider() {
        try {
            Constructor<? extends ISecretKeyProvider> constructor =
                    providerClass.getDeclaredConstructor(String.class, String.class, Context.class);
            return constructor.newInstance(MOCK_KEY_ALIAS, MOCK_KEY_FILE_PATH, context);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create provider instance", e);
        }
    }

    private SecretKey getKeyFromCache(ISecretKeyProvider keyProvider) {
        if (keyProvider instanceof AndroidWrappedKeyProvider) {
            return ((AndroidWrappedKeyProvider) keyProvider).getKeyFromCache();
        } else if (keyProvider instanceof KeyStoreBackedSecretKeyProvider) {
            return ((KeyStoreBackedSecretKeyProvider) keyProvider).getKeyFromCache();
        }
        throw new IllegalArgumentException("Unsupported key provider type: " + keyProvider.getClass().getName());
    }

    private void clearKeyFromCache(ISecretKeyProvider keyProvider) {
        if (keyProvider instanceof AndroidWrappedKeyProvider) {
            ((AndroidWrappedKeyProvider) keyProvider).clearKeyFromCache();
        } else if (keyProvider instanceof KeyStoreBackedSecretKeyProvider) {
            ((KeyStoreBackedSecretKeyProvider) keyProvider).clearKeyFromCache();
        } else {
            throw new IllegalArgumentException("Unsupported key provider type: " + keyProvider.getClass().getName());
        }
    }

    private SecretKey readSecretKeyFromStorage(ISecretKeyProvider keyProvider) throws ClientException {
        if (keyProvider instanceof AndroidWrappedKeyProvider) {
            return ((AndroidWrappedKeyProvider) keyProvider).readSecretKeyFromStorage();
        } else if (keyProvider instanceof KeyStoreBackedSecretKeyProvider) {
            return ((KeyStoreBackedSecretKeyProvider) keyProvider).readSecretKeyFromStorage();
        }
        throw new IllegalArgumentException("Unsupported key provider type: " + keyProvider.getClass().getName());
    }

    private SecretKey generateNewSecretKey(ISecretKeyProvider keyProvider) throws ClientException {
        if (keyProvider instanceof AndroidWrappedKeyProvider) {
            return ((AndroidWrappedKeyProvider) keyProvider).generateRandomKey();
        } else if (keyProvider instanceof KeyStoreBackedSecretKeyProvider) {
            return ((KeyStoreBackedSecretKeyProvider) keyProvider).generateNewSecretKey();
        }
        throw new IllegalArgumentException("Unsupported key provider type: " + keyProvider.getClass().getName());
    }

    final Context context = ApplicationProvider.getApplicationContext();
    final String MOCK_KEY_ALIAS = "MOCK_KEY_ALIAS";
    final String MOCK_KEY_FILE_PATH = "MOCK_KEY_FILE_PATH";
    final int TEST_LOOP = 100;

    final String AES_ALGORITHM = "AES";

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
        final ISecretKeyProvider keyProvider = createProvider();
        final SecretKey secretKey = generateNewSecretKey(keyProvider);

        Assert.assertEquals(AES_ALGORITHM, secretKey.getAlgorithm());
    }

    @Test
    public void testReadKeyDirectly() throws ClientException {
        final ISecretKeyProvider keyProvider = initkeyProviderWithKeyEntry();
        final SecretKey secretKey = keyProvider.getKey();
        final SecretKey storedSecretKey = readSecretKeyFromStorage(keyProvider);

        // They're not the same object!
        Assert.assertNotSame(secretKey, storedSecretKey);

        Assert.assertEquals(AES_ALGORITHM, secretKey.getAlgorithm());
        Assert.assertEquals(AES_ALGORITHM, storedSecretKey.getAlgorithm());

        Assert.assertNotNull(secretKey.getEncoded());
        Assert.assertNotNull(storedSecretKey.getEncoded());
        Assert.assertArrayEquals(secretKey.getEncoded(), storedSecretKey.getEncoded());
        Assert.assertEquals(secretKey.getFormat(), storedSecretKey.getFormat());
    }

    @Test
    public void testLoadKey() throws ClientException {
        // Nothing exists. This load key function should generate a key if the key hasn't exist.
        Assert.assertNull(AndroidKeyStoreUtil.readKey(MOCK_KEY_ALIAS));
        Assert.assertNull(FileUtil.readFromFile(getKeyFile(), KeyStoreBackedSecretKeyProvider.KEY_FILE_SIZE));

        final ISecretKeyProvider keyProvider = createProvider();
        final SecretKey secretKey = keyProvider.getKey();

        final SecretKey key = getKeyFromCache(keyProvider);
        Assert.assertNotNull(key);
        Assert.assertEquals(AES_ALGORITHM, secretKey.getAlgorithm());
        Assert.assertArrayEquals(secretKey.getEncoded(), key.getEncoded());
        Assert.assertEquals(secretKey.getFormat(), key.getFormat());
    }

    @Test
    public void testLoadKeyFromCorruptedFile_TruncatedExisingKey() throws ClientException {
        // Create a new Keystore-wrapped key.
        final ISecretKeyProvider keyProvider = createProvider();
        generateNewSecretKey(keyProvider);

        final byte[] wrappedKey = FileUtil.readFromFile(getKeyFile(), KeyStoreBackedSecretKeyProvider.KEY_FILE_SIZE);
        Assert.assertNotNull(wrappedKey);

        // Overwrite the key file with corrupted data.
        FileUtil.writeDataToFile(Arrays.copyOfRange(wrappedKey, 0, wrappedKey.length/2), getKeyFile());

        // It should fail to read, with an exception, and everything should be wiped.
        try{
            readSecretKeyFromStorage(keyProvider);
            Assert.fail();
        } catch (ClientException e){
            Assert.assertEquals(INVALID_KEY, e.getErrorCode());
        }

        // Everything should be wiped.
        Assert.assertFalse(getKeyFile().exists());

        // the next read should be unblocked.
        Assert.assertNull(readSecretKeyFromStorage(keyProvider));
    }

    @Test
    public void testLoadKeyFromCorruptedFile_InjectGarbage() throws ClientException {
        // Create a new Keystore-wrapped key.
        final ISecretKeyProvider keyProvider = createProvider();
        generateNewSecretKey(keyProvider);

        final byte[] wrappedKey = FileUtil.readFromFile(getKeyFile(), KeyStoreBackedSecretKeyProvider.KEY_FILE_SIZE);
        Assert.assertNotNull(wrappedKey);

        // Overwrite the key file with corrupted data.
        FileUtil.writeDataToFile(new byte[]{10, 20, 30, 40}, getKeyFile());

        // It should fail to read, with an exception, and everything should be wiped.
        try{
            readSecretKeyFromStorage(keyProvider);
            Assert.fail();
        } catch (ClientException e){
            Assert.assertEquals(INVALID_KEY, e.getErrorCode());
        }

        // Everything should be wiped.
        Assert.assertFalse(getKeyFile().exists());

        // the next read should be unblocked.
        Assert.assertNull(readSecretKeyFromStorage(keyProvider));
    }

    // 1s With Google Pixel XL, OS Version 29 (100 loop)
    @Test
    @Ignore
    public void testPerf_WithCachedKey() throws ClientException {
        final ISecretKeyProvider keyProvider = createProvider();

        long timeStartLoop = System.nanoTime();
        for (int i = 0; i < TEST_LOOP; i++) {
            keyProvider.getKey();
        }
        long timeFinishLoop = System.nanoTime();

        System.out.println("Time: " + (timeFinishLoop - timeStartLoop));
    }

    // 23s With Google Pixel XL, OS Version 29 (100 loop)
    @Test
    @Ignore
    public void testPerf_NoCachedKey() throws ClientException {
        final ISecretKeyProvider keyProvider = createProvider();

        long timeStartLoopNotCached = System.nanoTime();
        for (int i = 0; i < 100; i++) {
            clearKeyFromCache(keyProvider);
            keyProvider.getKey();
        }
        long timeFinishLoopNotCached = System.nanoTime();

        System.out.println("Time: " + (timeFinishLoopNotCached - timeStartLoopNotCached));
    }

    /**
     * This test is simulating the drawback of having an in-memory key cache.
     */
    @Test
    public void testLoadDeletedKeyStoreKey() throws ClientException {
        final ISecretKeyProvider keyProvider = initkeyProviderWithKeyEntry();

        AndroidKeyStoreUtil.deleteKey(MOCK_KEY_ALIAS);

        // Cached key also be wiped.
        final SecretKey key = getKeyFromCache(keyProvider);
        Assert.assertNull(key);
    }

    @Test
    public void testLoadDeletedKeyFile() throws ClientException {
        final ISecretKeyProvider keyProvider = initkeyProviderWithKeyEntry();

        FileUtil.deleteFile(getKeyFile());

        // Cached key also be wiped.
        final SecretKey key = getKeyFromCache(keyProvider);
        Assert.assertNull(key);
    }

    private ISecretKeyProvider initkeyProviderWithKeyEntry() throws ClientException {
        final ISecretKeyProvider keyProvider = createProvider();
        final SecretKey key = keyProvider.getKey();
        Assert.assertNotNull(key);
        Assert.assertNotNull(getKeyFromCache(keyProvider));
        return keyProvider;
    }
}
