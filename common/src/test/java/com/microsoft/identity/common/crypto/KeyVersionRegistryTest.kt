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
package com.microsoft.identity.common.crypto

import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import com.microsoft.identity.common.internal.util.AndroidKeyStoreUtil
import com.microsoft.identity.common.java.crypto.KeyMetadata
import com.microsoft.identity.common.java.exception.ClientException
import com.microsoft.identity.common.java.util.FileUtil
import org.json.JSONArray
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.Locale

/**
 * Robolectric unit tests for [KeyVersionRegistry].
 *
 * Covers key generation, version ID assignment, active key management, deprecation,
 * secret key loading, metadata persistence, pruning, and edge cases.
 * Uses Robolectric to provide an Android context without a physical device or emulator.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P])
class KeyVersionRegistryTest {

    /** SharedPreferences key for the JSON array of key metadata (mirrors the private constant). */
    private val PREFS_KEY_KEYS = "keys"

    private lateinit var context: Context
    private lateinit var registry: KeyVersionRegistry

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        registry = KeyVersionRegistry(context)
        cleanUp()
    }

    @After
    fun tearDown() {
        cleanUp()
    }

    // -------------------------------------------------------------------------
    // generateNewKey
    // -------------------------------------------------------------------------

    @Test
    fun generateNewKey_createsMetadataWithExpectedFields() {
        val metadata = registry.generateNewKey()

        Assert.assertNotNull("Metadata should not be null", metadata)
        Assert.assertEquals("First version ID should be K001", "K001", metadata.versionId)
        Assert.assertEquals(
            "Algorithm should be default",
            KeyMetadata.DEFAULT_ALGORITHM,
            metadata.algorithm
        )
        Assert.assertEquals("Key size should be 256", KeyMetadata.DEFAULT_KEY_SIZE, metadata.keySize)
        Assert.assertFalse("New key should not be deprecated", metadata.isDeprecated)
        Assert.assertTrue(
            "createdAtMillis should be at or before now",
            metadata.createdAtMillis <= System.currentTimeMillis()
        )
    }

    @Test
    fun generateNewKey_createsWrappedKeyFile() {
        val metadata = registry.generateNewKey()

        Assert.assertTrue(
            "Key file should exist after generateNewKey",
            getKeyFile(metadata.versionId).exists()
        )
    }

    @Test
    fun generateNewKey_assignsIncrementingVersionIds() {
        val first = registry.generateNewKey()
        val second = registry.generateNewKey()

        Assert.assertEquals("First version ID should be K001", "K001", first.versionId)
        Assert.assertEquals("Second version ID should be K002", "K002", second.versionId)
    }

    @Test
    fun generateNewKey_doesNotAutoPromoteToActive() {
        registry.generateNewKey()

        val active = registry.getActiveKey()
        Assert.assertNull("Newly generated key should not be auto-promoted to active", active)
    }

    // -------------------------------------------------------------------------
    // getActiveKey / setActiveKey
    // -------------------------------------------------------------------------

    @Test
    fun getActiveKey_returnsNullInitially() {
        Assert.assertNull("Active key should be null when none is set", registry.getActiveKey())
    }

    @Test
    fun setActiveKey_promotesKey() {
        val generated = registry.generateNewKey()
        registry.setActiveKey(generated.versionId)

        val active = registry.getActiveKey()
        Assert.assertNotNull("Active key should be set after setActiveKey", active)
        Assert.assertEquals(
            "Active version ID should match",
            generated.versionId,
            active!!.versionId
        )
    }

    @Test(expected = IllegalStateException::class)
    fun setActiveKey_throwsForUnknownVersion() {
        registry.setActiveKey("K999")
    }

    // -------------------------------------------------------------------------
    // getKeyByVersion
    // -------------------------------------------------------------------------

    @Test
    fun getKeyByVersion_returnsCorrectMetadata() {
        val generated = registry.generateNewKey()

        val found = registry.getKeyByVersion(generated.versionId)
        Assert.assertNotNull("Key should be found by version", found)
        Assert.assertEquals(
            "Version IDs should match",
            generated.versionId,
            found!!.versionId
        )
    }

    @Test
    fun getKeyByVersion_returnsNullForUnknownVersion() {
        Assert.assertNull(
            "Should return null for unknown version",
            registry.getKeyByVersion("K999")
        )
    }

    // -------------------------------------------------------------------------
    // deprecateKey / getDeprecatedKeys
    // -------------------------------------------------------------------------

    @Test
    fun deprecateKey_marksKeyAsDeprecated() {
        val generated = registry.generateNewKey()
        registry.deprecateKey(generated.versionId)

        val updated = registry.getKeyByVersion(generated.versionId)
        Assert.assertNotNull("Key should still exist after deprecation", updated)
        Assert.assertTrue("Key should be marked deprecated", updated!!.isDeprecated)
    }

    @Test
    fun deprecateKey_doesNotAffectOtherKeys() {
        val first = registry.generateNewKey()
        val second = registry.generateNewKey()
        registry.deprecateKey(first.versionId)

        val secondUpdated = registry.getKeyByVersion(second.versionId)
        Assert.assertNotNull("Second key should still exist", secondUpdated)
        Assert.assertFalse("Second key should not be deprecated", secondUpdated!!.isDeprecated)
    }

    @Test(expected = IllegalStateException::class)
    fun deprecateKey_throwsForUnknownVersion() {
        registry.deprecateKey("K999")
    }

    @Test
    fun deprecateKey_isIdempotent() {
        val generated = registry.generateNewKey()
        registry.deprecateKey(generated.versionId)
        registry.deprecateKey(generated.versionId) // second call must not throw

        val updated = registry.getKeyByVersion(generated.versionId)
        Assert.assertNotNull("Key should still exist", updated)
        Assert.assertTrue("Key should still be deprecated", updated!!.isDeprecated)
    }

    @Test
    fun getDeprecatedKeys_returnsOnlyDeprecatedKeys() {
        val first = registry.generateNewKey()
        registry.generateNewKey() // K002 — not deprecated
        registry.deprecateKey(first.versionId)

        val deprecated = registry.getDeprecatedKeys()
        Assert.assertEquals("Only one key should be deprecated", 1, deprecated.size)
        Assert.assertEquals(
            "Deprecated key should be K001",
            first.versionId,
            deprecated[0].versionId
        )
    }

    @Test
    fun getDeprecatedKeys_returnsEmptyListWhenNoneDeprecated() {
        registry.generateNewKey()

        val deprecated = registry.getDeprecatedKeys()
        Assert.assertNotNull("Deprecated list should not be null", deprecated)
        Assert.assertTrue(
            "No keys should be deprecated when none have been deprecated",
            deprecated.isEmpty()
        )
    }

    // -------------------------------------------------------------------------
    // loadSecretKey
    // -------------------------------------------------------------------------

    @Test
    fun loadSecretKey_returnsAesSecretKeyForValidVersion() {
        val generated = registry.generateNewKey()

        val secretKey = registry.loadSecretKey(generated.versionId)
        Assert.assertNotNull("Secret key should not be null", secretKey)
        Assert.assertEquals("Algorithm should be AES", "AES", secretKey.algorithm)
    }

    @Test(expected = IllegalStateException::class)
    fun loadSecretKey_throwsForUnknownVersion() {
        registry.loadSecretKey("K999")
    }

    @Test
    fun loadSecretKey_returnsSameKeyMaterialOnReload() {
        val generated = registry.generateNewKey()

        val key1 = registry.loadSecretKey(generated.versionId)
        val key2 = registry.loadSecretKey(generated.versionId)

        Assert.assertNotNull("First load should return a key", key1)
        Assert.assertNotNull("Second load should return a key", key2)
        Assert.assertArrayEquals(
            "Key material should be identical on both loads",
            key1.encoded,
            key2.encoded
        )
    }

    // -------------------------------------------------------------------------
    // pruneExpiredKeys
    // -------------------------------------------------------------------------

    @Test
    fun pruneExpiredKeys_doesNotPruneRecentlyCreatedKey() {
        registry.generateNewKey()
        registry.pruneExpiredKeys()

        Assert.assertNotNull(
            "Recently created key should not be pruned",
            registry.getKeyByVersion("K001")
        )
    }

    @Test
    fun pruneExpiredKeys_removesOldNonActiveKey() {
        registry.generateNewKey()
        overrideKeyCreationTimestamp("K001", expiredTimestamp())

        registry.pruneExpiredKeys()

        Assert.assertNull(
            "Old non-active key should be pruned",
            registry.getKeyByVersion("K001")
        )
    }

    @Test
    fun pruneExpiredKeys_deletesKeyFileForPrunedKey() {
        registry.generateNewKey()
        overrideKeyCreationTimestamp("K001", expiredTimestamp())

        registry.pruneExpiredKeys()

        Assert.assertFalse(
            "Key file should be deleted when key is pruned",
            getKeyFile("K001").exists()
        )
    }

    @Test
    fun pruneExpiredKeys_doesNotRemoveActiveKeyRegardlessOfAge() {
        registry.generateNewKey()
        registry.setActiveKey("K001")
        overrideKeyCreationTimestamp("K001", expiredTimestamp())

        registry.pruneExpiredKeys()

        val active = registry.getActiveKey()
        Assert.assertNotNull("Active key should never be pruned", active)
        Assert.assertEquals("Active key version should still be K001", "K001", active!!.versionId)
    }

    @Test
    fun pruneExpiredKeys_doesNotRemoveNonDeprecatedKeyWithinRetentionPeriod() {
        registry.generateNewKey()
        // Key is recent, so it should NOT be pruned even though it's not deprecated
        registry.pruneExpiredKeys()

        Assert.assertNotNull(
            "Non-deprecated key within retention period should not be pruned",
            registry.getKeyByVersion("K001")
        )
    }

    @Test
    fun pruneExpiredKeys_removesExpiredKeyRegardlessOfDeprecationStatus() {
        registry.generateNewKey() // NOT deprecated
        overrideKeyCreationTimestamp("K001", expiredTimestamp())

        registry.pruneExpiredKeys()

        Assert.assertNull(
            "Expired key should be pruned even if not explicitly deprecated",
            registry.getKeyByVersion("K001")
        )
    }

    @Test
    fun pruneExpiredKeys_keepsKeyJustUnderThreshold() {
        registry.generateNewKey()
        // 1 ms younger than the exact pruning threshold — key must be kept
        val justUnderThreshold = System.currentTimeMillis() -
                (KeyVersionRegistry.MAX_KEY_AGE_MILLIS + KeyVersionRegistry.GRACE_PERIOD_MILLIS) + 1
        overrideKeyCreationTimestamp("K001", justUnderThreshold)

        registry.pruneExpiredKeys()

        Assert.assertNotNull(
            "Key just under pruning threshold should not be pruned",
            registry.getKeyByVersion("K001")
        )
    }

    // -------------------------------------------------------------------------
    // Multiple key lifecycle
    // -------------------------------------------------------------------------

    @Test
    fun multipleKeyLifecycle_generateDeprecateGenerateVerifyBothAccessible() {
        // Generate K001, promote to active
        val k001 = registry.generateNewKey()
        registry.setActiveKey(k001.versionId)

        // Deprecate K001
        registry.deprecateKey(k001.versionId)

        // Generate K002, promote to active
        val k002 = registry.generateNewKey()
        registry.setActiveKey(k002.versionId)

        // Both keys should still be accessible
        Assert.assertNotNull("K001 should still be accessible after deprecation", registry.getKeyByVersion("K001"))
        Assert.assertNotNull("K002 should be accessible", registry.getKeyByVersion("K002"))

        // Only K001 should be deprecated
        val deprecated = registry.getDeprecatedKeys()
        Assert.assertEquals("Exactly one key should be deprecated", 1, deprecated.size)
        Assert.assertEquals("K001 should be deprecated", "K001", deprecated[0].versionId)

        // Active key should be K002
        val active = registry.getActiveKey()
        Assert.assertNotNull("Active key should be set", active)
        Assert.assertEquals("K002 should be the active key", "K002", active!!.versionId)
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Returns a timestamp old enough to make a key eligible for pruning.
     */
    private fun expiredTimestamp(): Long {
        return System.currentTimeMillis() -
                (KeyVersionRegistry.MAX_KEY_AGE_MILLIS + KeyVersionRegistry.GRACE_PERIOD_MILLIS + 1_000)
    }

    /**
     * Returns the wrapped key file for the given version ID.
     */
    private fun getKeyFile(versionId: String): File {
        return File(
            context.getDir(context.packageName, Context.MODE_PRIVATE),
            KeyVersionRegistry.KEY_FILE_PREFIX + versionId
        )
    }

    /**
     * Directly overwrites the `createdAtMillis` field for the key with [versionId] in
     * SharedPreferences, simulating a key that was created in the past.
     */
    private fun overrideKeyCreationTimestamp(versionId: String, newCreatedAtMillis: Long) {
        val prefs = context.getSharedPreferences(
            KeyVersionRegistry.METADATA_PREFS_NAME,
            Context.MODE_PRIVATE
        )
        val json = prefs.getString(PREFS_KEY_KEYS, null)
        Assert.assertNotNull(
            "Keys JSON should exist in SharedPreferences after key generation; " +
                "ensure generateNewKey() was called before overrideKeyCreationTimestamp",
            json
        )

        val array = JSONArray(json)
        for (i in 0 until array.length()) {
            val km = KeyMetadata.fromJson(array.getString(i))
            if (versionId == km.versionId) {
                val updated = KeyMetadata.builder()
                    .versionId(km.versionId)
                    .createdAtMillis(newCreatedAtMillis)
                    .algorithm(km.algorithm)
                    .keySize(km.keySize)
                    .deprecated(km.isDeprecated)
                    .build()
                array.put(i, updated.toJson())
                break
            }
        }
        // Use commit() for synchronous write; subsequent test operations depend on immediate persistence.
        prefs.edit().putString(PREFS_KEY_KEYS, array.toString()).commit()
    }

    /**
     * Removes all state created by the registry: SharedPreferences, wrapping key pair from
     * AndroidKeyStore, and wrapped key files.
     */
    private fun cleanUp() {
        context.getSharedPreferences(KeyVersionRegistry.METADATA_PREFS_NAME, Context.MODE_PRIVATE)
            .edit().clear().commit()

        try {
            AndroidKeyStoreUtil.deleteKey(KeyVersionRegistry.WRAPPING_KEY_ALIAS)
        } catch (ignored: Exception) {
            // Best-effort cleanup. If deletion fails (e.g., key was never created), it is safe
            // to continue because the registry will reuse an existing wrapping key pair if it
            // finds one under the same alias, ensuring test isolation is maintained.
        }

        // Remove any key files that could have been written.
        // K001–K010 covers all keys generated across the test suite; expand if new tests exceed 10.
        for (i in 1..10) {
            FileUtil.deleteFile(getKeyFile(String.format(Locale.ROOT, "K%03d", i)))
        }
    }
}
