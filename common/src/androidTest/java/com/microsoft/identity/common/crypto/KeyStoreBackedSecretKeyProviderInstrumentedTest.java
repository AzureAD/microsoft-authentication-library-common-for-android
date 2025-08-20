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

import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.microsoft.identity.common.adal.internal.AuthenticationSettings;
import com.microsoft.identity.common.internal.util.AndroidKeyStoreUtil;
import com.microsoft.identity.common.java.crypto.key.KeyUtil;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.flighting.CommonFlight;
import com.microsoft.identity.common.java.flighting.CommonFlightsManager;
import com.microsoft.identity.common.java.flighting.IFlightsManager;
import com.microsoft.identity.common.java.flighting.IFlightsProvider;
import com.microsoft.identity.common.java.util.FileUtil;
import com.microsoft.identity.common.logging.Logger;

import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import java.io.File;
import java.util.Arrays;

import javax.crypto.SecretKey;

/**
 * Instrumented tests for KeyStoreBackedSecretKeyProvider.
 * These tests run on real Android devices/emulators with access to the Android KeyStore.
 */
@RunWith(AndroidJUnit4.class)
public class KeyStoreBackedSecretKeyProviderInstrumentedTest {

    private final Context context = ApplicationProvider.getApplicationContext();
    private final String MOCK_KEY_ALIAS = "MOCK_KEY_ALIAS_INSTRUMENTED";
    private final String MOCK_KEY_FILE_PATH = "MOCK_KEY_FILE_PATH_INSTRUMENTED";
    private final String AES_ALGORITHM = "AES";

    private KeyStoreBackedSecretKeyProvider keyProvider;

    @BeforeClass
    public static void classSetUp() {
        Logger.setAndroidLogger();
        Logger.setAllowLogcat(true);
    }

    @Before
    public void setUp() throws Exception {
        // Clean slate for each test
        AuthenticationSettings.INSTANCE.clearSecretKeysForTestCases();
        AndroidKeyStoreUtil.deleteKey(MOCK_KEY_ALIAS);
        FileUtil.deleteFile(getKeyFile());

        final IFlightsProvider mockFlightsProvider = Mockito.mock(IFlightsProvider.class);
        Mockito.when(mockFlightsProvider.isFlightEnabled(CommonFlight.ENABLE_NEW_KEY_GEN_SPEC_FOR_WRAP_WITH_PURPOSE_WRAP_KEY))
                .thenReturn(true);
        Mockito.when(mockFlightsProvider.isFlightEnabled(CommonFlight.ENABLE_NEW_KEY_GEN_SPEC_FOR_WRAP_WITHOUT_PURPOSE_WRAP_KEY))
                .thenReturn(true);
        Mockito.when(mockFlightsProvider.isFlightEnabled(CommonFlight.ENABLE_OAEP_WITH_SHA_AND_MGF1_PADDING))
                .thenReturn(false);

        // Create anonymous IFlightsManager
        IFlightsManager anonymousFlightsManager = new IFlightsManager() {
            @Override
            public @NotNull IFlightsProvider getFlightsProvider(long waitForConfigsWithTimeoutInMs) {
                return mockFlightsProvider;
            }
            @Override
            public @NotNull IFlightsProvider getFlightsProviderForTenant(@NotNull String tenantId, long waitForConfigsWithTimeoutInMs) {
                return mockFlightsProvider;
            }
            @Override
            public @NotNull IFlightsProvider getFlightsProviderForTenant(@NotNull String tenantId) {
                return mockFlightsProvider;
            }
            @NonNull
            @Override
            public IFlightsProvider getFlightsProvider() {
                return mockFlightsProvider;
            }
        };

        // Initialize CommonFlightsManager with the anonymous implementation
        CommonFlightsManager.INSTANCE.initializeCommonFlightsManager(anonymousFlightsManager);
        keyProvider = new KeyStoreBackedSecretKeyProvider(
                context,
                MOCK_KEY_ALIAS,
                MOCK_KEY_FILE_PATH
        );
    }
    
    @After
    public void tearDown() throws Exception {
        // Clean up after each test
        if (keyProvider != null) {
            keyProvider.clearKeyFromCache();
            keyProvider.deleteSecretKeyFromStorage();
        }
        AndroidKeyStoreUtil.deleteKey(MOCK_KEY_ALIAS);
        FileUtil.deleteFile(getKeyFile());
    }

    private File getKeyFile() {
        return new File(
                context.getDir(context.getPackageName(), Context.MODE_PRIVATE),
                MOCK_KEY_FILE_PATH
        );
    }

    @Test
    public void testBasicProperties() {
        Assert.assertEquals("Alias should match", MOCK_KEY_ALIAS, keyProvider.getAlias());
        Assert.assertEquals("Key type identifier should be A001", "A001", keyProvider.getKeyTypeIdentifier());
        Assert.assertEquals("Cipher transformation should be AES/CBC/PKCS5Padding",
                "AES/CBC/PKCS5Padding", keyProvider.getCipherTransformation());
    }

    @Test
    public void testGetKey_GeneratesWhenNoneExists() throws ClientException {
        // Initially no key should exist
        Assert.assertFalse("Key file should not exist initially", getKeyFile().exists());
        Assert.assertNull("No key should be in KeyStore initially", AndroidKeyStoreUtil.readKey(MOCK_KEY_ALIAS));

        SecretKey secretKey = keyProvider.getKey();

        Assert.assertNotNull("Key should be generated", secretKey);
        Assert.assertEquals("Algorithm should be AES", AES_ALGORITHM, secretKey.getAlgorithm());
        Assert.assertNotNull("Key is cached", keyProvider.getKeyFromCache());
        Assert.assertTrue("Key file should be created", getKeyFile().exists());
        Assert.assertNotNull("KeyPair should be in KeyStore", AndroidKeyStoreUtil.readKey(MOCK_KEY_ALIAS));
    }

    @Test
    public void testGetKey_LoadsExistingKey() throws ClientException {
        // First, generate a key
        SecretKey originalKey = keyProvider.generateNewSecretKey();
        Assert.assertNotNull("Original key should not be null", originalKey);

        // Verify key is not cached
        Assert.assertNull("Cache must be empty", keyProvider.getKeyFromCache());

        // Get key again - should load from storage
        SecretKey loadedKey = keyProvider.getKey();

        Assert.assertNotNull("Loaded key should not be null", loadedKey);
        Assert.assertEquals("Algorithm should be AES", AES_ALGORITHM, loadedKey.getAlgorithm());

        // Keys should have the same thumbprint (same key material)
        String originalThumbprint = KeyUtil.getKeyThumbPrint(originalKey);
        String loadedThumbprint = KeyUtil.getKeyThumbPrint(loadedKey);
        Assert.assertEquals("Thumbprints should match", originalThumbprint, loadedThumbprint);
    }

    @Test
    public void testCaching() throws ClientException {
        // First call should generate and cache the key
        SecretKey key1 = keyProvider.getKey();
        // Second call should return the same cached instance
        SecretKey key2 = keyProvider.getKeyFromCache();
        Assert.assertSame("Keys should be the same cached instance", key1, key2);
    }

    @Test
    public void testClearKeyFromCache() throws ClientException {
        // Generate and cache a key
        SecretKey originalKey = keyProvider.getKey();
        Assert.assertNotNull("Original key should not be null", originalKey);

        // Verify it's cached
        SecretKey cachedKey = keyProvider.getKeyFromCache();
        Assert.assertSame("Should return cached key", originalKey, cachedKey);

        // Clear cache
        keyProvider.clearKeyFromCache();

        // Cache should be empty
        SecretKey afterClearCache = keyProvider.getKeyFromCache();
        Assert.assertNull("Cache should be empty after clearing", afterClearCache);

        // But we should still be able to load from storage
        SecretKey reloadedKey = keyProvider.getKey();
        Assert.assertNotNull("Should be able to reload from storage", reloadedKey);

        // Should have same key material
        String originalThumbprint = KeyUtil.getKeyThumbPrint(originalKey);
        String reloadedThumbprint = KeyUtil.getKeyThumbPrint(reloadedKey);
        Assert.assertEquals("Thumbprints should match", originalThumbprint, reloadedThumbprint);
    }


    @Test
    public void testDeleteSecretKeyFromStorage() throws ClientException {
        // Generate a key first
        keyProvider.generateNewSecretKey();
        Assert.assertTrue("Key file should exist", getKeyFile().exists());
        Assert.assertNotNull("KeyPair should exist in KeyStore", AndroidKeyStoreUtil.readKey(MOCK_KEY_ALIAS));

        // Delete from storage
        keyProvider.deleteSecretKeyFromStorage();

        Assert.assertFalse("Key file should be deleted", getKeyFile().exists());
        Assert.assertNull("KeyPair should be deleted from KeyStore", AndroidKeyStoreUtil.readKey(MOCK_KEY_ALIAS));

        // Cache should also be cleared
        SecretKey cachedKey = keyProvider.getKeyFromCache();
        Assert.assertNull("Cache should be cleared", cachedKey);
    }

    @Test
    public void testKeyInvalidation_DeletedKeyStoreKey() throws ClientException {
        // Generate a key and ensure it's cached
        SecretKey originalKey = keyProvider.getKey();
        Assert.assertNotNull("Original key should not be null", originalKey);
        Assert.assertNotNull("Should be cached", keyProvider.getKeyFromCache());

        // Manually delete the KeyStore key (simulating external deletion)
        AndroidKeyStoreUtil.deleteKey(MOCK_KEY_ALIAS);

        // Cache should be invalidated
        SecretKey cachedKey = keyProvider.getKeyFromCache();
        Assert.assertNull("Cache should be invalidated when KeyStore key is deleted", cachedKey);
    }

    @Test
    public void testKeyInvalidation_DeletedKeyFile() throws ClientException {
        // Generate a key and ensure it's cached
        SecretKey originalKey = keyProvider.getKey();
        Assert.assertNotNull("Original key should not be null", originalKey);
        Assert.assertNotNull("Should be cached", keyProvider.getKeyFromCache());

        // Manually delete the key file (simulating external deletion)
        FileUtil.deleteFile(getKeyFile());

        // Cache should be invalidated
        SecretKey cachedKey = keyProvider.getKeyFromCache();
        Assert.assertNull("Cache should be invalidated when key file is deleted", cachedKey);
    }

    @Test
    public void testCorruptedKeyFile_TruncatedData() throws ClientException {
        // Generate a key first
        keyProvider.generateNewSecretKey();

        // Read the wrapped key data
        byte[] originalWrappedKey = FileUtil.readFromFile(getKeyFile(), KeyStoreBackedSecretKeyProvider.KEY_FILE_SIZE);
        Assert.assertNotNull("Original wrapped key should exist", originalWrappedKey);

        // Corrupt the file by truncating it
        byte[] truncatedData = Arrays.copyOfRange(originalWrappedKey, 0, originalWrappedKey.length / 2);
        FileUtil.writeDataToFile(truncatedData, getKeyFile());

        // Clear cache to force reading from corrupted file
        keyProvider.clearKeyFromCache();

        // Should throw exception when trying to read corrupted key
        try {
            keyProvider.readSecretKeyFromStorage();
            Assert.fail("Should throw exception for corrupted key");
        } catch (ClientException e) {
            Assert.assertEquals("Should throw INVALID_KEY exception", INVALID_KEY, e.getErrorCode());
        }

        // File should be cleaned up after failed read
        Assert.assertFalse("Corrupted file should be deleted", getKeyFile().exists());

        // Next read should work (will generate new key)
        SecretKey newKey = keyProvider.readSecretKeyFromStorage();
        Assert.assertNull("Should return null after cleanup, requiring new key generation", newKey);
    }

    @Test
    public void testCorruptedKeyFile_GarbageData() throws ClientException {
        // Generate a key first
        keyProvider.generateNewSecretKey();

        // Corrupt the file with garbage data
        byte[] garbageData = {10, 20, 30, 40, 50};
        FileUtil.writeDataToFile(garbageData, getKeyFile());

        // Clear cache to force reading from corrupted file
        keyProvider.clearKeyFromCache();

        // Should throw exception when trying to read corrupted key
        try {
            keyProvider.readSecretKeyFromStorage();
            Assert.fail("Should throw exception for corrupted key");
        } catch (ClientException e) {
            Assert.assertEquals("Should throw INVALID_KEY exception", INVALID_KEY, e.getErrorCode());
        }

        // File should be cleaned up after failed read
        Assert.assertFalse("Corrupted file should be deleted", getKeyFile().exists());
    }

    @Test
    public void testRealWorldScenario_MultipleOperations() throws ClientException {
        // Simulate real-world usage with multiple operations

        // 1. App starts, generates key
        SecretKey key1 = keyProvider.getKey();
        Assert.assertNotNull("Initial key should not be null", key1);
        String thumbprint1 = KeyUtil.getKeyThumbPrint(key1);

        // 2. App restarts (cache cleared)
        keyProvider.clearKeyFromCache();
        SecretKey key2 = keyProvider.getKey();
        Assert.assertNotNull("Key after restart should not be null", key2);
        String thumbprint2 = KeyUtil.getKeyThumbPrint(key2);
        Assert.assertEquals("Key should be the same after restart", thumbprint1, thumbprint2);

        // 3. Multiple rapid accesses (should use cache)
        for (int i = 0; i < 10; i++) {
            SecretKey rapidKey = keyProvider.getKey();
            Assert.assertSame("Rapid access should return cached key", key2, rapidKey);
        }

        // 4. Manual key regeneration
        SecretKey newKey = keyProvider.generateNewSecretKey();
        Assert.assertNotNull("New key should not be null", newKey);
        String newThumbprint = KeyUtil.getKeyThumbPrint(newKey);
        Assert.assertNotEquals("New key should be different", thumbprint1, newThumbprint);
    }

    // Backward/Forward Compatibility Tests AndroidWrappedKeyProvider to KeyStoreBackedSecretKeyProvider using PKCS1

    @Test
    public void testBackwardCompatibility_KeyTypeIdentifierConsistency() {
        AndroidWrappedKeyProvider androidProvider = new AndroidWrappedKeyProvider(
                MOCK_KEY_ALIAS, MOCK_KEY_FILE_PATH, context);

        Assert.assertEquals("Key type identifiers should match",
                androidProvider.getKeyTypeIdentifier(),
                keyProvider.getKeyTypeIdentifier());
        Assert.assertEquals("Key type identifier should be A001", "A001", keyProvider.getKeyTypeIdentifier());
    }

    @Test
    public void testBackwardCompatibility_CipherTransformationConsistency() {
        AndroidWrappedKeyProvider androidProvider = new AndroidWrappedKeyProvider(
                MOCK_KEY_ALIAS, MOCK_KEY_FILE_PATH, context);

        Assert.assertEquals("Cipher transformations should match",
                androidProvider.getCipherTransformation(),
                keyProvider.getCipherTransformation());
        Assert.assertEquals("Cipher transformation should be AES/CBC/PKCS5Padding",
                "AES/CBC/PKCS5Padding", keyProvider.getCipherTransformation());
    }

    @Test
    public void testMigrationScenario_UpgradeFromAndroidWrappedKeyProvider() throws ClientException {
        // Scenario: App was using AndroidWrappedKeyProvider, now upgrading to KeyStoreBackedSecretKeyProvider

        // Step 1: Create data with AndroidWrappedKeyProvider
        AndroidWrappedKeyProvider androidProvider = new AndroidWrappedKeyProvider(
                MOCK_KEY_ALIAS, MOCK_KEY_FILE_PATH, context);
        SecretKey originalKey = androidProvider.generateRandomKey();
        String originalThumbprint = KeyUtil.getKeyThumbPrint(originalKey);

        // Step 2: App upgrades and now uses KeyStoreBackedSecretKeyProvider
        // It should be able to read the existing key
        SecretKey migratedKey = keyProvider.getKey();
        String migratedThumbprint = KeyUtil.getKeyThumbPrint(migratedKey);

        Assert.assertNotNull("Migrated key should not be null", migratedKey);
        Assert.assertEquals("Thumbprints should match after migration", originalThumbprint, migratedThumbprint);

        // Step 3: Key should now be cached in the new provider
        SecretKey cachedKey = keyProvider.getKeyFromCache();
        Assert.assertSame("Key should be cached", migratedKey, cachedKey);

        // Step 4: Subsequent operations should work normally
        SecretKey subsequentKey = keyProvider.getKey();
        Assert.assertSame("Subsequent calls should return cached key", migratedKey, subsequentKey);
    }

    @Test
    public void testMigrationScenario_RollbackToAndroidWrappedKeyProvider() throws ClientException {
        // Scenario: App was using KeyStoreBackedSecretKeyProvider, now rolling back to AndroidWrappedKeyProvider

        // Step 1: Create data with KeyStoreBackedSecretKeyProvider
        SecretKey originalKey = keyProvider.generateNewSecretKey();
        String originalThumbprint = KeyUtil.getKeyThumbPrint(originalKey);

        // Step 2: App rolls back and now uses AndroidWrappedKeyProvider
        // It should be able to read the existing key
        AndroidWrappedKeyProvider androidProvider = new AndroidWrappedKeyProvider(
                MOCK_KEY_ALIAS, MOCK_KEY_FILE_PATH, context);
        SecretKey rolledBackKey = androidProvider.readSecretKeyFromStorage();

        Assert.assertNotNull("Rolled back key should not be null", rolledBackKey);
        String rolledBackThumbprint = KeyUtil.getKeyThumbPrint(rolledBackKey);
        Assert.assertEquals("Thumbprints should match after rollback", originalThumbprint, rolledBackThumbprint);

    }
}