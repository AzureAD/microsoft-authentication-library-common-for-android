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
import android.content.SharedPreferences;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.microsoft.identity.common.internal.util.AndroidKeyStoreUtil;
import com.microsoft.identity.common.java.crypto.KeyMetadata;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.flighting.CommonFlight;
import com.microsoft.identity.common.java.flighting.CommonFlightsManager;
import com.microsoft.identity.common.java.flighting.IFlightsManager;
import com.microsoft.identity.common.java.util.FileUtil;
import com.microsoft.identity.common.logging.Logger;

import org.json.JSONArray;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import javax.crypto.SecretKey;

/**
 * Instrumented tests for {@link KeyVersionRegistry}.
 * These tests run on a real Android device/emulator because they exercise the AndroidKeyStore
 * (for RSA wrapping), SharedPreferences, and the file system.
 */
@RunWith(AndroidJUnit4.class)
public class KeyVersionRegistryTest {

    /** SharedPreferences key for the JSON array of key metadata (mirrors the private constant). */
    private static final String PREFS_KEY_KEYS = "keys";

    private Context mContext;
    private KeyVersionRegistry mRegistry;

    private final long MAX_KEY_AGE_MILLIS = TimeUnit.DAYS.toMillis(CommonFlightsManager.INSTANCE.getFlightsProvider()
            .getIntValue(CommonFlight.SYMMETRIC_KEY_MAX_AGE_DAYS));

    @BeforeClass
    public static void classSetUp() {
        Logger.setAndroidLogger();
        Logger.setAllowLogcat(true);
    }

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        mRegistry = new KeyVersionRegistry(mContext);
        cleanUp();
    }

    @After
    public void tearDown() {
        cleanUp();
    }

    // -------------------------------------------------------------------------
    // generateNewKey
    // -------------------------------------------------------------------------

    @Test
    public void generateNewKey_createsMetadataWithExpectedFields() throws ClientException {
        final KeyMetadata metadata = mRegistry.generateNewKey();

        Assert.assertNotNull("Metadata should not be null", metadata);
        Assert.assertEquals("First version ID should be K001", "K001", metadata.getVersionId());
        Assert.assertEquals("Algorithm should be default",
                KeyMetadata.DEFAULT_ALGORITHM, metadata.getAlgorithm());
        Assert.assertEquals("Key size should be 256", KeyMetadata.DEFAULT_KEY_SIZE, metadata.getKeySize());
        Assert.assertFalse("New key should not be deprecated", metadata.isDeprecated());
        Assert.assertTrue("createdAtMillis should be at or before now",
                metadata.getCreatedAtMillis() <= System.currentTimeMillis());
    }

    @Test
    public void generateNewKey_createsWrappedKeyFile() throws ClientException {
        final KeyMetadata metadata = mRegistry.generateNewKey();

        Assert.assertTrue("Key file should exist after generateNewKey",
                getKeyFile(metadata.getVersionId()).exists());
    }

    @Test
    public void generateNewKey_incrementsVersionId() throws ClientException {
        final KeyMetadata first = mRegistry.generateNewKey();
        final KeyMetadata second = mRegistry.generateNewKey();

        Assert.assertEquals("First version ID should be K001", "K001", first.getVersionId());
        Assert.assertEquals("Second version ID should be K002", "K002", second.getVersionId());
    }

    @Test
    public void generateNewKey_doesNotAutoPromoteToActive() throws ClientException {
        mRegistry.generateNewKey();

        final KeyMetadata active = mRegistry.getActiveKey();
        Assert.assertNull("Newly generated key should not be auto-promoted to active", active);
    }

    // -------------------------------------------------------------------------
    // getActiveKey / setActiveKey
    // -------------------------------------------------------------------------

    @Test
    public void getActiveKey_returnsNullInitially() throws ClientException {
        Assert.assertNull("Active key should be null when none is set", mRegistry.getActiveKey());
    }

    @Test
    public void setActiveKey_promotesKey() throws ClientException {
        final KeyMetadata generated = mRegistry.generateNewKey();
        mRegistry.setActiveKey(generated.getVersionId());

        final KeyMetadata active = mRegistry.getActiveKey();
        Assert.assertNotNull("Active key should be set after setActiveKey", active);
        Assert.assertEquals("Active version ID should match",
                generated.getVersionId(), active.getVersionId());
    }

    @Test(expected = IllegalStateException.class)
    public void setActiveKey_throwsForUnknownVersion() throws ClientException {
        mRegistry.setActiveKey("K999");
    }

    // -------------------------------------------------------------------------
    // getKeyByVersion
    // -------------------------------------------------------------------------

    @Test
    public void getKeyByVersion_returnsCorrectMetadata() throws ClientException {
        final KeyMetadata generated = mRegistry.generateNewKey();

        final KeyMetadata found = mRegistry.getKeyByVersion(generated.getVersionId());
        Assert.assertNotNull("Key should be found by version", found);
        Assert.assertEquals("Version IDs should match",
                generated.getVersionId(), found.getVersionId());
    }

    @Test
    public void getKeyByVersion_returnsNullForUnknownVersion() throws ClientException {
        Assert.assertNull("Should return null for unknown version",
                mRegistry.getKeyByVersion("K999"));
    }

    // -------------------------------------------------------------------------
    // deprecateKey / getDeprecatedKeys
    // -------------------------------------------------------------------------

    @Test
    public void deprecateKey_marksKeyAsDeprecated() throws ClientException {
        final KeyMetadata generated = mRegistry.generateNewKey();
        mRegistry.deprecateKey(generated.getVersionId());

        final KeyMetadata updated = mRegistry.getKeyByVersion(generated.getVersionId());
        Assert.assertNotNull("Key should still exist after deprecation", updated);
        Assert.assertTrue("Key should be marked deprecated", updated.isDeprecated());
    }

    @Test(expected = IllegalStateException.class)
    public void deprecateKey_throwsForUnknownVersion() throws ClientException {
        mRegistry.deprecateKey("K999");
    }

    @Test
    public void deprecateKey_isIdempotent() throws ClientException {
        final KeyMetadata generated = mRegistry.generateNewKey();
        mRegistry.deprecateKey(generated.getVersionId());
        mRegistry.deprecateKey(generated.getVersionId()); // second call must not throw

        final KeyMetadata updated = mRegistry.getKeyByVersion(generated.getVersionId());
        Assert.assertNotNull("Key should still exist", updated);
        Assert.assertTrue("Key should still be deprecated", updated.isDeprecated());
    }

    @Test
    public void getDeprecatedKeys_returnsOnlyDeprecatedKeys() throws ClientException {
        final KeyMetadata first = mRegistry.generateNewKey();
        mRegistry.generateNewKey(); // K002 — not deprecated
        mRegistry.deprecateKey(first.getVersionId());

        final List<KeyMetadata> deprecated = mRegistry.getDeprecatedKeys();
        Assert.assertEquals("Only one key should be deprecated", 1, deprecated.size());
        Assert.assertEquals("Deprecated key should be K001",
                first.getVersionId(), deprecated.get(0).getVersionId());
    }

    @Test
    public void getDeprecatedKeys_returnsEmptyListWhenNoneDeprecated() throws ClientException {
        mRegistry.generateNewKey();

        final List<KeyMetadata> deprecated = mRegistry.getDeprecatedKeys();
        Assert.assertNotNull("Deprecated list should not be null", deprecated);
        Assert.assertTrue("No keys should be deprecated when none have been deprecated",
                deprecated.isEmpty());
    }

    // -------------------------------------------------------------------------
    // loadSecretKey
    // -------------------------------------------------------------------------

    @Test
    public void loadSecretKey_returnsUnwrappedAesKey() throws ClientException {
        final KeyMetadata generated = mRegistry.generateNewKey();

        final SecretKey secretKey = mRegistry.loadSecretKey(generated.getVersionId());
        Assert.assertNotNull("Secret key should not be null", secretKey);
        Assert.assertEquals("Algorithm should be AES", "AES", secretKey.getAlgorithm());
    }

    @Test(expected = IllegalStateException.class)
    public void loadSecretKey_throwsForUnknownVersion() throws ClientException {
        mRegistry.loadSecretKey("K999");
    }

    @Test
    public void loadSecretKey_returnsSameKeyMaterialOnReload() throws ClientException {
        final KeyMetadata generated = mRegistry.generateNewKey();

        final SecretKey key1 = mRegistry.loadSecretKey(generated.getVersionId());
        final SecretKey key2 = mRegistry.loadSecretKey(generated.getVersionId());

        Assert.assertNotNull("First load should return a key", key1);
        Assert.assertNotNull("Second load should return a key", key2);
        Assert.assertArrayEquals("Key material should be identical on both loads",
                key1.getEncoded(), key2.getEncoded());
    }

    // -------------------------------------------------------------------------
    // pruneExpiredKeys
    // -------------------------------------------------------------------------

    @Test
    public void pruneExpiredKeys_doesNotPruneRecentKeys() throws ClientException {
        mRegistry.generateNewKey();
        mRegistry.pruneExpiredKeys();

        Assert.assertNotNull("Recently created key should not be pruned",
                mRegistry.getKeyByVersion("K001"));
    }

    @Test
    public void pruneExpiredKeys_prunesOldNonActiveKey() throws Exception {
        mRegistry.generateNewKey();
        overrideKeyCreationTimestamp("K001", expiredTimestamp());

        mRegistry.pruneExpiredKeys();

        Assert.assertNull("Old non-active key should be pruned",
                mRegistry.getKeyByVersion("K001"));
    }

    @Test
    public void pruneExpiredKeys_deletesKeyFileForPrunedKey() throws Exception {
        mRegistry.generateNewKey();
        overrideKeyCreationTimestamp("K001", expiredTimestamp());

        mRegistry.pruneExpiredKeys();

        Assert.assertFalse("Key file should be deleted when key is pruned",
                getKeyFile("K001").exists());
    }

    @Test
    public void pruneExpiredKeys_doesNotPruneActiveKeyEvenIfOld() throws Exception {
        mRegistry.generateNewKey();
        mRegistry.setActiveKey("K001");
        overrideKeyCreationTimestamp("K001", expiredTimestamp());

        mRegistry.pruneExpiredKeys();

        final KeyMetadata active = mRegistry.getActiveKey();
        Assert.assertNotNull("Active key should never be pruned", active);
        Assert.assertEquals("Active key version should still be K001",
                "K001", active.getVersionId());
    }

    @Test
    public void pruneExpiredKeys_prunesExpiredKeyRegardlessOfDeprecationStatus() throws Exception {
        mRegistry.generateNewKey(); // NOT deprecated
        overrideKeyCreationTimestamp("K001", expiredTimestamp());

        mRegistry.pruneExpiredKeys();

        Assert.assertNull("Expired key should be pruned even if not explicitly deprecated",
                mRegistry.getKeyByVersion("K001"));
    }

    @Test
    public void pruneExpiredKeys_keepsKeyJustUnderThreshold() throws Exception {
        mRegistry.generateNewKey();
        // 1s younger than the exact pruning threshold — key age is (MAX + GRACE - 1), which is
        // NOT strictly greater than (MAX + GRACE), so the key must be kept.
        final long justUnderThreshold = System.currentTimeMillis()
                - (MAX_KEY_AGE_MILLIS) + 1_000;
        overrideKeyCreationTimestamp("K001", justUnderThreshold);

        mRegistry.pruneExpiredKeys();

        Assert.assertNotNull("Key just under pruning threshold should not be pruned",
                mRegistry.getKeyByVersion("K001"));
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Returns a timestamp old enough to make a key eligible for pruning.
     */
    private long expiredTimestamp() {
        return System.currentTimeMillis()
                - (MAX_KEY_AGE_MILLIS + 1_000);
    }

    /**
     * Returns the wrapped key file for the given version ID.
     */
    private File getKeyFile(final String versionId) {
        return new File(
                mContext.getDir(mContext.getPackageName(), Context.MODE_PRIVATE),
                KeyVersionRegistry.KEY_FILE_PREFIX + versionId);
    }

    /**
     * Directly overwrites the {@code createdAtMillis} field for the key with {@code versionId}
     * in SharedPreferences, simulating a key that was created in the past.
     */
    private void overrideKeyCreationTimestamp(
            final String versionId, final long newCreatedAtMillis) throws Exception {
        final SharedPreferences prefs = mContext.getSharedPreferences(
                KeyVersionRegistry.METADATA_PREFS_NAME, Context.MODE_PRIVATE);
        final String json = prefs.getString(PREFS_KEY_KEYS, null);
        Assert.assertNotNull("Keys JSON must not be null before overriding timestamp", json);

        final JSONArray array = new JSONArray(json);
        for (int i = 0; i < array.length(); i++) {
            final KeyMetadata km;
            try {
                km = KeyMetadata.fromJson(array.getString(i));
            } catch (final Exception e) {
                throw new AssertionError("Failed to parse KeyMetadata at index " + i, e);
            }
            if (versionId.equals(km.getVersionId())) {
                final KeyMetadata updated = KeyMetadata.builder()
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
        // Use commit() for synchronous write; subsequent test operations depend on immediate persistence.
        prefs.edit().putString(PREFS_KEY_KEYS, array.toString()).commit();
    }

    /**
     * Removes all state created by the registry: SharedPreferences, wrapping key pair from
     * AndroidKeyStore, and wrapped key files.
     */
    private void cleanUp() {
        mContext.getSharedPreferences(KeyVersionRegistry.METADATA_PREFS_NAME, Context.MODE_PRIVATE)
                .edit().clear().commit();

        try {
            AndroidKeyStoreUtil.deleteKey(KeyVersionRegistry.WRAPPING_KEY_ALIAS);
        } catch (final Exception ignored) {
            // Best-effort cleanup; failure here must not affect test isolation.
        }

        // Remove any key files that could have been written.
        // K001–K010 covers all keys generated across the test suite; expand if new tests exceed 10.
        for (int i = 1; i <= 10; i++) {
            FileUtil.deleteFile(getKeyFile(String.format(Locale.ROOT, "K%03d", i)));
        }
    }
}
