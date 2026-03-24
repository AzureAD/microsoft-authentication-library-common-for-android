// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// This code is licensed under the MIT License.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
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
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;

import com.microsoft.identity.common.internal.util.AndroidKeyStoreUtil;
import com.microsoft.identity.common.java.crypto.KeyMetadata;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.flighting.CommonFlight;
import com.microsoft.identity.common.java.flighting.CommonFlightsManager;
import com.microsoft.identity.common.java.flighting.IFlightConfig;
import com.microsoft.identity.common.java.flighting.IFlightsManager;
import com.microsoft.identity.common.java.flighting.IFlightsProvider;
import com.microsoft.identity.common.java.util.FileUtil;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.File;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Locale;

import static org.junit.Assert.*;

/**
 * Robolectric unit tests for {@link KeyVersionRegistry}.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = {Build.VERSION_CODES.P})
public class KeyVersionRegistryTest {

    private static final String PREFS_KEY_KEYS = "keys";
    private Context context;
    private KeyVersionRegistry registry;
    private static KeyPair fakeWrappingKeyPair;
    private MockedStatic<AndroidKeyStoreUtil> androidKeyStoreUtilMock;

    static final long MAX_KEY_AGE_MILLIS = 3L * 365 * 24 * 60 * 60 * 1000;

    @BeforeClass
    public static void beforeClass() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        fakeWrappingKeyPair = kpg.generateKeyPair();
    }

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        androidKeyStoreUtilMock = Mockito.mockStatic(AndroidKeyStoreUtil.class);
        androidKeyStoreUtilMock.when(() -> AndroidKeyStoreUtil.readKey(Mockito.anyString()))
                .thenReturn(fakeWrappingKeyPair);
        androidKeyStoreUtilMock.when(() -> AndroidKeyStoreUtil.deleteKey(Mockito.anyString()))
                .thenAnswer(invocation -> null);
        androidKeyStoreUtilMock.when(() -> AndroidKeyStoreUtil.wrap(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.isNull()))
                .thenCallRealMethod();
        androidKeyStoreUtilMock.when(() -> AndroidKeyStoreUtil.unwrap(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.isNull()))
                .thenCallRealMethod();

        registry = new KeyVersionRegistry(context);
        cleanUp();
    }

    @After
    public void tearDown() {
        cleanUp();
        androidKeyStoreUtilMock.close();
        CommonFlightsManager.INSTANCE.resetFlightsManager();
    }

    @Test
    public void generateNewKey_createsMetadataWithExpectedFields() throws ClientException {
        KeyMetadata metadata = registry.generateNewKey();
        assertNotNull("Metadata should not be null", metadata);
        assertEquals("First version ID should be K001", "K001", metadata.getVersionId());
        assertEquals("Algorithm should be default", KeyMetadata.DEFAULT_ALGORITHM, metadata.getAlgorithm());
        assertEquals("Key size should be 256", KeyMetadata.DEFAULT_KEY_SIZE, metadata.getKeySize());
        assertFalse("New key should not be deprecated", metadata.isDeprecated());
        assertTrue("createdAtMillis should be at or before now", metadata.getCreatedAtMillis() <= System.currentTimeMillis());
    }

    @Test
    public void generateNewKey_createsWrappedKeyFile() throws ClientException {
        KeyMetadata metadata = registry.generateNewKey();
        assertTrue("Key file should exist after generateNewKey", getKeyFile(metadata.getVersionId()).exists());
    }

    @Test
    public void generateNewKey_assignsIncrementingVersionIds() throws ClientException {
        KeyMetadata first = registry.generateNewKey();
        KeyMetadata second = registry.generateNewKey();
        assertEquals("First version ID should be K001", "K001", first.getVersionId());
        assertEquals("Second version ID should be K002", "K002", second.getVersionId());
    }

    @Test
    public void generateNewKey_doesNotAutoPromoteToActive() throws ClientException {
        registry.generateNewKey();
        assertNull("Newly generated key should not be auto-promoted to active", registry.getActiveKey());
    }

    @Test
    public void getActiveKey_returnsNullInitially() throws ClientException {
        assertNull("Active key should be null when none is set", registry.getActiveKey());
    }

    @Test
    public void setActiveKey_promotesKey() throws ClientException {
        KeyMetadata generated = registry.generateNewKey();
        registry.setActiveKey(generated.getVersionId());
        KeyMetadata active = registry.getActiveKey();
        assertNotNull("Active key should be set after setActiveKey", active);
        assertEquals("Active version ID should match", generated.getVersionId(), active.getVersionId());
    }

    @Test(expected = IllegalStateException.class)
    public void setActiveKey_throwsForUnknownVersion() throws ClientException {
        registry.setActiveKey("K999");
    }

    @Test
    public void getKeyByVersion_returnsCorrectMetadata() throws ClientException {
        KeyMetadata generated = registry.generateNewKey();
        KeyMetadata found = registry.getKeyByVersion(generated.getVersionId());
        assertNotNull("Key should be found by version", found);
        assertEquals("Version IDs should match", generated.getVersionId(), found.getVersionId());
    }

    @Test
    public void getKeyByVersion_returnsNullForUnknownVersion() throws ClientException {
        assertNull("Should return null for unknown version", registry.getKeyByVersion("K999"));
    }

    @Test
    public void deprecateKey_marksKeyAsDeprecated() throws ClientException {
        KeyMetadata generated = registry.generateNewKey();
        registry.deprecateKey(generated.getVersionId());
        KeyMetadata updated = registry.getKeyByVersion(generated.getVersionId());
        assertNotNull("Key should still exist after deprecation", updated);
        assertTrue("Key should be marked deprecated", updated.isDeprecated());
    }

    @Test
    public void deprecateKey_doesNotAffectOtherKeys() throws ClientException {
        KeyMetadata first = registry.generateNewKey();
        KeyMetadata second = registry.generateNewKey();
        registry.deprecateKey(first.getVersionId());
        KeyMetadata secondUpdated = registry.getKeyByVersion(second.getVersionId());
        assertNotNull("Second key should still exist", secondUpdated);
        assertFalse("Second key should not be deprecated", secondUpdated.isDeprecated());
    }

    @Test(expected = IllegalStateException.class)
    public void deprecateKey_throwsForUnknownVersion() throws ClientException {
        registry.deprecateKey("K999");
    }

    @Test
    public void deprecateKey_isIdempotent() throws ClientException {
        KeyMetadata generated = registry.generateNewKey();
        registry.deprecateKey(generated.getVersionId());
        registry.deprecateKey(generated.getVersionId());
        KeyMetadata updated = registry.getKeyByVersion(generated.getVersionId());
        assertNotNull("Key should still exist", updated);
        assertTrue("Key should still be deprecated", updated.isDeprecated());
    }

    @Test
    public void getDeprecatedKeys_returnsOnlyDeprecatedKeys() throws ClientException {
        KeyMetadata first = registry.generateNewKey();
        registry.generateNewKey();
        registry.deprecateKey(first.getVersionId());
        assertEquals("Only one key should be deprecated", 1, registry.getDeprecatedKeys().size());
        assertEquals("Deprecated key should be K001", first.getVersionId(), registry.getDeprecatedKeys().get(0).getVersionId());
    }

    @Test
    public void getDeprecatedKeys_returnsEmptyListWhenNoneDeprecated() throws ClientException {
        registry.generateNewKey();
        assertNotNull("Deprecated list should not be null", registry.getDeprecatedKeys());
        assertTrue("No keys should be deprecated when none have been deprecated", registry.getDeprecatedKeys().isEmpty());
    }

    @Test
    public void loadSecretKey_returnsAesSecretKeyForValidVersion() throws ClientException {
        KeyMetadata generated = registry.generateNewKey();
        javax.crypto.SecretKey secretKey = registry.loadSecretKey(generated.getVersionId());
        assertNotNull("Secret key should not be null", secretKey);
        assertEquals("Algorithm should be AES", "AES", secretKey.getAlgorithm());
    }

    @Test(expected = IllegalStateException.class)
    public void loadSecretKey_throwsForUnknownVersion() throws ClientException {
        registry.loadSecretKey("K999");
    }

    @Test
    public void loadSecretKey_returnsSameKeyMaterialOnReload() throws ClientException {
        KeyMetadata generated = registry.generateNewKey();
        javax.crypto.SecretKey key1 = registry.loadSecretKey(generated.getVersionId());
        javax.crypto.SecretKey key2 = registry.loadSecretKey(generated.getVersionId());
        assertNotNull("First load should return a key", key1);
        assertNotNull("Second load should return a key", key2);
        assertArrayEquals("Key material should be identical on both loads", key1.getEncoded(), key2.getEncoded());
    }

    @Test
    public void pruneExpiredKeys_doesNotPruneRecentlyCreatedKey() throws ClientException {
        registry.generateNewKey();
        registry.pruneExpiredKeys();
        assertNotNull("Recently created key should not be pruned", registry.getKeyByVersion("K001"));
    }

    @Test
    public void pruneExpiredKeys_removesOldNonActiveKey() throws ClientException {
        registry.generateNewKey();
        overrideKeyCreationTimestamp("K001", expiredTimestamp());
        registry.pruneExpiredKeys();
        assertNull("Old non-active key should be pruned", registry.getKeyByVersion("K001"));
    }

    @Test
    public void pruneExpiredKeys_deletesKeyFileForPrunedKey() throws ClientException {
        registry.generateNewKey();
        overrideKeyCreationTimestamp("K001", expiredTimestamp());
        registry.pruneExpiredKeys();
        assertFalse("Key file should be deleted when key is pruned", getKeyFile("K001").exists());
    }

    @Test
    public void pruneExpiredKeys_doesNotRemoveActiveKeyRegardlessOfAge() throws ClientException {
        registry.generateNewKey();
        registry.setActiveKey("K001");
        overrideKeyCreationTimestamp("K001", expiredTimestamp());
        registry.pruneExpiredKeys();
        KeyMetadata active = registry.getActiveKey();
        assertNotNull("Active key should never be pruned", active);
        assertEquals("Active key version should still be K001", "K001", active.getVersionId());
    }

    @Test
    public void pruneExpiredKeys_doesNotRemoveNonDeprecatedKeyWithinRetentionPeriod() throws ClientException {
        registry.generateNewKey();
        registry.pruneExpiredKeys();
        assertNotNull("Non-deprecated key within retention period should not be pruned", registry.getKeyByVersion("K001"));
    }

    @Test
    public void pruneExpiredKeys_removesExpiredKeyRegardlessOfDeprecationStatus() throws ClientException {
        registry.generateNewKey();
        overrideKeyCreationTimestamp("K001", expiredTimestamp());
        registry.pruneExpiredKeys();
        assertNull("Expired key should be pruned even if not explicitly deprecated", registry.getKeyByVersion("K001"));
    }

    @Test
    public void pruneExpiredKeys_keepsKeyJustUnderThreshold() throws ClientException {
        registry.generateNewKey();
        // Use a 1-second margin so the test is immune to the few ms that elapse between
        // computing the timestamp here and pruneExpiredKeys() sampling System.currentTimeMillis().
        long justUnderThreshold = System.currentTimeMillis() -
                (MAX_KEY_AGE_MILLIS + KeyVersionRegistry.GRACE_PERIOD_MILLIS) + 1_000;
        overrideKeyCreationTimestamp("K001", justUnderThreshold);
        registry.pruneExpiredKeys();
        assertNotNull("Key just under pruning threshold should not be pruned", registry.getKeyByVersion("K001"));
    }

    @Test
    public void multipleKeyLifecycle_generateDeprecateGenerateVerifyBothAccessible() throws ClientException {
        KeyMetadata k001 = registry.generateNewKey();
        registry.setActiveKey(k001.getVersionId());
        registry.deprecateKey(k001.getVersionId());
        KeyMetadata k002 = registry.generateNewKey();
        registry.setActiveKey(k002.getVersionId());
        assertNotNull("K001 should still be accessible after deprecation", registry.getKeyByVersion("K001"));
        assertNotNull("K002 should be accessible", registry.getKeyByVersion("K002"));
        assertEquals("Exactly one key should be deprecated", 1, registry.getDeprecatedKeys().size());
        assertEquals("K001 should be deprecated", "K001", registry.getDeprecatedKeys().get(0).getVersionId());
        KeyMetadata active = registry.getActiveKey();
        assertNotNull("Active key should be set", active);
        assertEquals("K002 should be the active key", "K002", active.getVersionId());
    }

    @Test
    public void pruneExpiredKeys_usesDefaultWhenFlightReturnsZero() throws ClientException {
        // Generate a recently-created key; with the default 1095-day max age it should survive pruning.
        registry.generateNewKey();
        // Inject a flights provider that returns 0 for SYMMETRIC_KEY_MAX_AGE_DAYS.
        CommonFlightsManager.INSTANCE.initializeCommonFlightsManager(buildFlightsManagerWithMaxAgeDays(0));
        registry.pruneExpiredKeys();
        // The key must still exist – the fallback to default (1095 days) should prevent it from being pruned.
        assertNotNull("Key should not be pruned when flight returns 0; default should be used",
                registry.getKeyByVersion("K001"));
    }

    @Test
    public void pruneExpiredKeys_usesDefaultWhenFlightReturnsNegative() throws ClientException {
        // Generate a recently-created key; with the default 1095-day max age it should survive pruning.
        registry.generateNewKey();
        // Inject a flights provider that returns -1 for SYMMETRIC_KEY_MAX_AGE_DAYS.
        CommonFlightsManager.INSTANCE.initializeCommonFlightsManager(buildFlightsManagerWithMaxAgeDays(-1));
        registry.pruneExpiredKeys();
        // The key must still exist – the fallback to default (1095 days) should prevent it from being pruned.
        assertNotNull("Key should not be pruned when flight returns -1; default should be used",
                registry.getKeyByVersion("K001"));
    }

    // Helpers

    private long expiredTimestamp() {
        return System.currentTimeMillis() -
                (MAX_KEY_AGE_MILLIS + KeyVersionRegistry.GRACE_PERIOD_MILLIS + 1000);
    }

    private File getKeyFile(String versionId) {
        return new File(
                context.getDir(context.getPackageName(), Context.MODE_PRIVATE),
                KeyVersionRegistry.KEY_FILE_PREFIX + versionId
        );
    }

    private void overrideKeyCreationTimestamp(String versionId, long newCreatedAtMillis) {
        android.content.SharedPreferences prefs = context.getSharedPreferences(
                KeyVersionRegistry.METADATA_PREFS_NAME,
                Context.MODE_PRIVATE
        );
        String json = prefs.getString(PREFS_KEY_KEYS, null);
        assertNotNull("Keys JSON should exist in SharedPreferences after key generation; ensure generateNewKey() was called before overrideKeyCreationTimestamp", json);

        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                KeyMetadata km = KeyMetadata.fromJson(array.getString(i));
                if (versionId.equals(km.getVersionId())) {
                    KeyMetadata updated = KeyMetadata.builder()
                            .versionId(km.getVersionId())
                            .createdAtMillis(newCreatedAtMillis)
                            .algorithm(km.getAlgorithm())
                            .keySize(km.getKeySize())
                            .deprecated(km.isDeprecated())
                            .build();
                    array.put(i, updated.toJson());
                    break;
                }
            }
            prefs.edit().putString(PREFS_KEY_KEYS, array.toString()).commit();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void cleanUp() {
        context.getSharedPreferences(KeyVersionRegistry.METADATA_PREFS_NAME, Context.MODE_PRIVATE)
                .edit().clear().commit();
        try {
            AndroidKeyStoreUtil.deleteKey(KeyVersionRegistry.WRAPPING_KEY_ALIAS);
        } catch (Exception ignored) {
        }
        for (int i = 1; i <= 10; i++) {
            FileUtil.deleteFile(getKeyFile(String.format(Locale.ROOT, "K%03d", i)));
        }
    }

    /**
     * Creates an {@link IFlightsManager} whose provider returns {@code maxAgeDays} for
     * {@link CommonFlight#SYMMETRIC_KEY_MAX_AGE_DAYS} and the flight's own default for everything else.
     */
    private IFlightsManager buildFlightsManagerWithMaxAgeDays(final int maxAgeDays) {
        final IFlightsProvider provider = new IFlightsProvider() {
            @Override
            public boolean isFlightEnabled(IFlightConfig flightConfig) {
                return (boolean) flightConfig.getDefaultValue();
            }

            @Override
            public boolean getBooleanValue(IFlightConfig flightConfig) {
                return (boolean) flightConfig.getDefaultValue();
            }

            @Override
            public int getIntValue(IFlightConfig flightConfig) {
                if (flightConfig == CommonFlight.SYMMETRIC_KEY_MAX_AGE_DAYS) {
                    return maxAgeDays;
                }
                return (int) flightConfig.getDefaultValue();
            }

            @Override
            public double getDoubleValue(IFlightConfig flightConfig) {
                return (double) flightConfig.getDefaultValue();
            }

            @Override
            public String getStringValue(IFlightConfig flightConfig) {
                return (String) flightConfig.getDefaultValue();
            }

            @Override
            public JSONObject getJsonValue(IFlightConfig flightConfig) {
                return (JSONObject) flightConfig.getDefaultValue();
            }
        };

        return new IFlightsManager() {
            @NonNull
            @Override
            public IFlightsProvider getFlightsProvider(long waitForConfigsWithTimeoutInMs) {
                return provider;
            }

            @NonNull
            @Override
            public IFlightsProvider getFlightsProviderForTenant(String tenantId, long waitForConfigsWithTimeoutInMs) {
                return provider;
            }


            @Override
            public @NotNull IFlightsProvider getFlightsProviderForTenant(@NotNull String tenantId) {
                return provider;
            }

            @NonNull
            @Override
            public IFlightsProvider getFlightsProvider() {
                return provider;
            }
        };
    }
}