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

import static com.microsoft.identity.common.java.crypto.key.AES256SecretKeyGenerator.AES_ALGORITHM;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.microsoft.identity.common.internal.util.AndroidKeyStoreUtil;
import com.microsoft.identity.common.java.crypto.KeyMetadata;
import com.microsoft.identity.common.java.crypto.key.AES256SecretKeyGenerator;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.opentelemetry.OTelUtility;
import com.microsoft.identity.common.java.opentelemetry.SpanExtension;
import com.microsoft.identity.common.java.opentelemetry.SpanName;
import com.microsoft.identity.common.java.util.FileUtil;
import com.microsoft.identity.common.logging.Logger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.security.KeyPair;
import java.security.spec.AlgorithmParameterSpec;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import javax.crypto.SecretKey;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Scope;

/**
 * Manages the lifecycle of multiple symmetric encryption key versions.
 *
 * <p>Key metadata is persisted in {@link SharedPreferences} under the filename
 * {@value #PREFS_FILE_NAME}. The wrapped (RSA-encrypted) AES key material for each version is
 * stored in a separate file named {@code brokerks_<versionId>} (e.g., {@code brokerks_K001}).
 * A single RSA wrapping key pair is held in the AndroidKeyStore under the alias
 * {@value #WRAPPING_KEY_ALIAS}.</p>
 *
 * <p>Old keys are retained for decryption until pruned. A key is eligible for pruning when it is
 * deprecated AND its age exceeds {@link #MAX_KEY_AGE_MILLIS} + {@link #GRACE_PERIOD_MILLIS}.</p>
 */
public class KeyVersionRegistry {

    private static final String TAG = KeyVersionRegistry.class.getSimpleName();

    /** SharedPreferences filename for key metadata storage. */
    @VisibleForTesting
    static final String PREFS_FILE_NAME = "brokerks_metadata";

    /** SharedPreferences key storing the serialized registry state JSON. */
    private static final String PREFS_KEY_REGISTRY = "registry_state";

    /** Alias for the RSA wrapping key pair in AndroidKeyStore. */
    @VisibleForTesting
    static final String WRAPPING_KEY_ALIAS = "brokerks";

    /** File name prefix for per-version wrapped key files. */
    private static final String KEY_FILE_PREFIX = "brokerks_";

    /** Algorithm for RSA key wrapping. */
    private static final String WRAP_ALGORITHM = "RSA/ECB/PKCS1Padding";

    /** Algorithm name for the RSA key pair stored in AndroidKeyStore. */
    private static final String WRAP_KEY_ALGORITHM = "RSA";

    /** Maximum key age (3 years) in milliseconds before a key may be pruned. */
    @VisibleForTesting
    static final long MAX_KEY_AGE_MILLIS = 3L * 365 * 24 * 60 * 60 * 1000;

    /** Grace period (90 days) in milliseconds added on top of {@link #MAX_KEY_AGE_MILLIS} for pruning. */
    @VisibleForTesting
    static final long GRACE_PERIOD_MILLIS = 90L * 24 * 60 * 60 * 1000;

    /** Read buffer size used when loading a wrapped key file. */
    private static final int WRAPPED_KEY_FILE_SIZE = 1024;

    // JSON field names for the registry state object
    private static final String JSON_ACTIVE_VERSION = "activeVersion";
    private static final String JSON_KEYS = "keys";

    private final Context mContext;
    private final SharedPreferences mSharedPreferences;

    /**
     * Constructs a {@link KeyVersionRegistry}.
     *
     * @param context Android context used for SharedPreferences and file storage access.
     */
    public KeyVersionRegistry(@NonNull final Context context) {
        mContext = context.getApplicationContext();
        mSharedPreferences = mContext.getSharedPreferences(PREFS_FILE_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Returns the {@link KeyMetadata} for the currently active key.
     *
     * @return the active key metadata, or {@code null} if no active key has been set.
     */
    @Nullable
    public synchronized KeyMetadata getActiveKey() {
        final String activeVersion = readActiveVersion();
        if (activeVersion == null) {
            return null;
        }
        return getKeyByVersion(activeVersion);
    }

    /**
     * Returns the {@link KeyMetadata} for the key with the given version ID.
     *
     * @param versionId the version identifier (e.g., {@code "K001"}).
     * @return matching {@link KeyMetadata}, or {@code null} if not found.
     */
    @Nullable
    public synchronized KeyMetadata getKeyByVersion(@NonNull final String versionId) {
        for (final KeyMetadata key : readAllKeys()) {
            if (versionId.equals(key.getVersionId())) {
                return key;
            }
        }
        return null;
    }

    /**
     * Returns an unmodifiable list of all deprecated keys in the registry.
     *
     * @return list of deprecated {@link KeyMetadata} entries; never {@code null}.
     */
    @NonNull
    public synchronized List<KeyMetadata> getDeprecatedKeys() {
        final List<KeyMetadata> deprecated = new ArrayList<>();
        for (final KeyMetadata key : readAllKeys()) {
            if (key.isDeprecated()) {
                deprecated.add(key);
            }
        }
        return Collections.unmodifiableList(deprecated);
    }

    /**
     * Generates a new AES-256 key, wraps it with the AndroidKeyStore RSA key, stores the wrapped
     * bytes in a file, and persists the metadata in SharedPreferences.
     *
     * <p>The new key is <em>not</em> automatically promoted to the active key; call
     * {@link #setActiveKey(String)} to make it active.</p>
     *
     * <p>If metadata persistence fails after the key file has been written, the orphaned key file
     * is deleted to keep storage consistent.</p>
     *
     * @return {@link KeyMetadata} for the newly created key.
     * @throws ClientException if key generation, wrapping, file I/O, or metadata persistence fails.
     */
    @NonNull
    public synchronized KeyMetadata generateNewKey() throws ClientException {
        final String methodTag = TAG + ":generateNewKey";
        final Span span = OTelUtility.createSpanFromParent(
                SpanName.KeyVersionRegistryGenerateKey.name(),
                SpanExtension.current().getSpanContext());
        try (final Scope ignored = SpanExtension.makeCurrentSpan(span)) {
            // 1. Generate a new AES-256 secret key
            final SecretKey secretKey = AES256SecretKeyGenerator.INSTANCE.generateRandomKey();

            // 2. Load or create the RSA wrapping key pair from AndroidKeyStore
            final KeyPair keyPair = getOrCreateWrappingKeyPair();

            // 3. Wrap (encrypt) the AES secret key with the RSA key pair
            final byte[] wrappedKey = AndroidKeyStoreUtil.wrap(secretKey, keyPair, WRAP_ALGORITHM, null);

            // 4. Compute the next auto-incremented version ID
            final String newVersionId = computeNextVersionId();

            // 5. Persist the wrapped key bytes to a per-version file
            final File keyFile = getKeyFile(newVersionId);
            FileUtil.writeDataToFile(wrappedKey, keyFile);

            // 6. Build and persist the key metadata; clean up the key file on failure
            final KeyMetadata metadata = KeyMetadata.builder()
                    .versionId(newVersionId)
                    .createdAtMillis(System.currentTimeMillis())
                    .algorithm(KeyMetadata.DEFAULT_ALGORITHM)
                    .keySize(KeyMetadata.DEFAULT_KEY_SIZE)
                    .build();
            try {
                appendKeyMetadata(metadata);
            } catch (final ClientException e) {
                // Avoid orphaned key files that can never be discovered or pruned
                Logger.warn(methodTag, "Metadata persistence failed; removing orphaned key file for: " + newVersionId);
                FileUtil.deleteFile(keyFile);
                throw e;
            }

            Logger.info(methodTag, "Generated new key version: " + newVersionId);
            span.setStatus(StatusCode.OK);
            return metadata;
        } catch (final ClientException e) {
            span.setStatus(StatusCode.ERROR);
            span.recordException(e);
            throw e;
        } finally {
            span.end();
        }
    }

    /**
     * Marks the key with the given version ID as deprecated.
     *
     * <p>A deprecated key may no longer be used for encryption but remains available for
     * decryption until it is pruned by {@link #pruneExpiredKeys()}.</p>
     *
     * @param versionId the version ID to deprecate.
     * @throws IllegalArgumentException if the version ID is not found in the registry.
     * @throws ClientException          if the updated state cannot be persisted.
     */
    public synchronized void deprecateKey(@NonNull final String versionId) throws ClientException {
        final List<KeyMetadata> allKeys = readAllKeys();
        final List<KeyMetadata> updated = new ArrayList<>(allKeys.size());
        boolean found = false;

        for (final KeyMetadata key : allKeys) {
            if (versionId.equals(key.getVersionId())) {
                found = true;
                updated.add(KeyMetadata.builder()
                        .versionId(key.getVersionId())
                        .createdAtMillis(key.getCreatedAtMillis())
                        .algorithm(key.getAlgorithm())
                        .keySize(key.getKeySize())
                        .deprecated(true)
                        .build());
            } else {
                updated.add(key);
            }
        }

        if (!found) {
            throw new IllegalArgumentException("Key version not found: " + versionId);
        }

        persistRegistryState(readActiveVersion(), updated);
    }

    /**
     * Updates the active version pointer to the key with the given version ID.
     *
     * @param versionId the version ID to set as active.
     * @throws IllegalArgumentException if the version ID is not found in the registry.
     * @throws ClientException          if the updated state cannot be persisted.
     */
    public synchronized void setActiveKey(@NonNull final String versionId) throws ClientException {
        if (getKeyByVersion(versionId) == null) {
            throw new IllegalArgumentException("Key version not found: " + versionId);
        }
        persistRegistryState(versionId, readAllKeys());
    }

    /**
     * Loads and unwraps the {@link SecretKey} for the given version ID.
     *
     * @param versionId the version ID of the key to load.
     * @return the unwrapped {@link SecretKey}.
     * @throws ClientException if the wrapped key file cannot be found, read, or unwrapped.
     */
    @NonNull
    public synchronized SecretKey loadSecretKey(@NonNull final String versionId) throws ClientException {
        final String methodTag = TAG + ":loadSecretKey";

        final File keyFile = getKeyFile(versionId);
        if (!keyFile.exists()) {
            throw new ClientException(ClientException.IO_ERROR,
                    "Key file not found for version: " + versionId);
        }

        final byte[] wrappedKey = FileUtil.readFromFile(keyFile, WRAPPED_KEY_FILE_SIZE);
        if (wrappedKey == null) {
            throw new ClientException(ClientException.IO_ERROR,
                    "Key file is empty for version: " + versionId);
        }

        final KeyPair keyPair = AndroidKeyStoreUtil.readKey(WRAPPING_KEY_ALIAS);
        if (keyPair == null) {
            throw new ClientException(ClientException.INVALID_KEY_MISSING,
                    "RSA wrapping key pair not found in AndroidKeyStore");
        }

        final SecretKey secretKey = AndroidKeyStoreUtil.unwrap(
                wrappedKey, AES_ALGORITHM, keyPair, WRAP_ALGORITHM, null);

        Logger.info(methodTag, "Loaded secret key for version: " + versionId);
        return secretKey;
    }

    /**
     * Removes keys that are deprecated and whose age exceeds
     * {@link #MAX_KEY_AGE_MILLIS} + {@link #GRACE_PERIOD_MILLIS}.
     *
     * <p>The wrapped key files for pruned entries are deleted from disk.</p>
     *
     * @throws ClientException if the updated registry state cannot be persisted after pruning.
     */
    public synchronized void pruneExpiredKeys() throws ClientException {
        final String methodTag = TAG + ":pruneExpiredKeys";
        final long now = System.currentTimeMillis();
        final long pruneThreshold = MAX_KEY_AGE_MILLIS + GRACE_PERIOD_MILLIS;

        final List<KeyMetadata> allKeys = readAllKeys();
        final List<KeyMetadata> remaining = new ArrayList<>(allKeys.size());

        for (final KeyMetadata key : allKeys) {
            final boolean expired = key.isDeprecated()
                    && (now - key.getCreatedAtMillis()) > pruneThreshold;
            if (expired) {
                Logger.info(methodTag, "Pruning expired key: " + key.getVersionId());
                FileUtil.deleteFile(getKeyFile(key.getVersionId()));
            } else {
                remaining.add(key);
            }
        }

        if (remaining.size() < allKeys.size()) {
            persistRegistryState(readActiveVersion(), remaining);
        }
    }

    // --- Private helpers ---

    /**
     * Reads all key metadata entries from SharedPreferences.
     *
     * @return mutable list of all stored {@link KeyMetadata} entries; empty if none or on parse error.
     */
    @NonNull
    private List<KeyMetadata> readAllKeys() {
        final String json = mSharedPreferences.getString(PREFS_KEY_REGISTRY, null);
        if (json == null) {
            return new ArrayList<>();
        }
        try {
            final JSONObject root = new JSONObject(json);
            final JSONArray keysArray = root.optJSONArray(JSON_KEYS);
            if (keysArray == null) {
                return new ArrayList<>();
            }
            final List<KeyMetadata> keys = new ArrayList<>(keysArray.length());
            for (int i = 0; i < keysArray.length(); i++) {
                keys.add(KeyMetadata.fromJson(keysArray.getString(i)));
            }
            return keys;
        } catch (final JSONException e) {
            Logger.error(TAG + ":readAllKeys", "Failed to parse registry JSON", e);
            return new ArrayList<>();
        }
    }

    /**
     * Reads the active version identifier from SharedPreferences.
     *
     * @return the active version string (e.g., {@code "K002"}), or {@code null} if absent.
     */
    @Nullable
    private String readActiveVersion() {
        final String json = mSharedPreferences.getString(PREFS_KEY_REGISTRY, null);
        if (json == null) {
            return null;
        }
        try {
            final JSONObject root = new JSONObject(json);
            final String value = root.optString(JSON_ACTIVE_VERSION, null);
            return (value != null && !value.isEmpty()) ? value : null;
        } catch (final JSONException e) {
            Logger.error(TAG + ":readActiveVersion", "Failed to parse registry JSON", e);
            return null;
        }
    }

    /**
     * Serializes and persists the complete registry state to SharedPreferences.
     *
     * <p>Uses {@link SharedPreferences.Editor#commit()} for a synchronous, reliable write that
     * surfaces I/O failures to the caller.</p>
     *
     * @param activeVersion the current active version ID, or {@code null} if unset.
     * @param keys          the full list of key metadata entries to persist.
     * @throws ClientException if JSON serialization or the SharedPreferences commit fails.
     */
    private void persistRegistryState(@Nullable final String activeVersion,
                                      @NonNull final List<KeyMetadata> keys) throws ClientException {
        try {
            final JSONObject root = new JSONObject();
            if (activeVersion != null && !activeVersion.isEmpty()) {
                root.put(JSON_ACTIVE_VERSION, activeVersion);
            }
            final JSONArray keysArray = new JSONArray();
            for (final KeyMetadata key : keys) {
                keysArray.put(new JSONObject(key.toJson()));
            }
            root.put(JSON_KEYS, keysArray);
            final boolean committed = mSharedPreferences.edit()
                    .putString(PREFS_KEY_REGISTRY, root.toString())
                    .commit();
            if (!committed) {
                throw new ClientException(ClientException.IO_ERROR,
                        "SharedPreferences commit returned false while persisting registry state");
            }
        } catch (final JSONException e) {
            throw new ClientException(ClientException.IO_ERROR,
                    "Failed to serialize key registry state", e);
        }
    }

    /**
     * Appends a new {@link KeyMetadata} entry to the existing registry state and persists it.
     *
     * @param metadata the new key metadata to append.
     * @throws ClientException if persistence fails.
     */
    private void appendKeyMetadata(@NonNull final KeyMetadata metadata) throws ClientException {
        final List<KeyMetadata> keys = readAllKeys();
        keys.add(metadata);
        persistRegistryState(readActiveVersion(), keys);
    }

    /**
     * Computes the next auto-incremented version ID based on existing entries.
     *
     * <p>Existing version IDs are expected to follow the format {@code K<NNN>} (e.g., K001, K002).
     * The returned ID has the format {@code K%03d} (zero-padded to three digits).</p>
     *
     * @return the next version ID string (e.g., {@code "K003"}).
     */
    @NonNull
    private String computeNextVersionId() {
        int maxNum = 0;
        for (final KeyMetadata key : readAllKeys()) {
            final String versionId = key.getVersionId();
            if (versionId != null && versionId.length() > 1 && versionId.charAt(0) == 'K') {
                try {
                    final int num = Integer.parseInt(versionId.substring(1));
                    if (num > maxNum) {
                        maxNum = num;
                    }
                } catch (final NumberFormatException ignored) {
                    // Non-numeric suffix; skip this entry
                }
            }
        }
        return String.format(Locale.ROOT, "K%03d", maxNum + 1);
    }

    /**
     * Returns the {@link File} used to store the wrapped key bytes for the given version ID.
     *
     * @param versionId the key version ID (e.g., {@code "K001"}).
     * @return the {@link File} for the given version's wrapped key.
     */
    @NonNull
    private File getKeyFile(@NonNull final String versionId) {
        return new File(
                mContext.getDir(mContext.getPackageName(), Context.MODE_PRIVATE),
                KEY_FILE_PREFIX + versionId);
    }

    /**
     * Loads the RSA wrapping key pair from AndroidKeyStore; generates a new one if absent.
     *
     * @return the existing or newly generated {@link KeyPair}.
     * @throws ClientException if key pair generation fails.
     */
    @NonNull
    private KeyPair getOrCreateWrappingKeyPair() throws ClientException {
        final String methodTag = TAG + ":getOrCreateWrappingKeyPair";
        KeyPair keyPair = AndroidKeyStoreUtil.readKey(WRAPPING_KEY_ALIAS);
        if (keyPair == null) {
            Logger.info(methodTag, "No existing wrapping key pair found. Generating a new one.");
            keyPair = generateWrappingKeyPair();
        }
        return keyPair;
    }

    /**
     * Generates a new RSA-2048 key pair in the AndroidKeyStore for wrapping symmetric keys.
     *
     * @return the newly generated {@link KeyPair}.
     * @throws ClientException if key pair generation fails.
     */
    @NonNull
    private KeyPair generateWrappingKeyPair() throws ClientException {
        final AlgorithmParameterSpec spec = new KeyGenParameterSpec.Builder(
                WRAPPING_KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setKeySize(2048)
                .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1)
                .build();
        return AndroidKeyStoreUtil.generateKeyPair(WRAP_KEY_ALGORITHM, spec);
    }
}
