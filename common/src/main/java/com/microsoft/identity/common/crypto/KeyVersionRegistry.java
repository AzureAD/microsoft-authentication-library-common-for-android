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
import android.os.Build;
import android.security.KeyPairGeneratorSpec;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;

import androidx.annotation.RequiresApi;
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

import java.io.File;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.spec.AlgorithmParameterSpec;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import javax.crypto.SecretKey;
import javax.security.auth.x500.X500Principal;

import edu.umd.cs.findbugs.annotations.Nullable;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Scope;
import lombok.NonNull;

/**
 * Registry that manages the lifecycle of versioned symmetric encryption keys.
 *
 * <p>Key metadata is persisted in SharedPreferences under the filename
 * {@value #METADATA_PREFS_NAME}. Wrapped key material is stored in files
 * named {@code brokerks_<versionId>} (e.g. {@code brokerks_K001}).
 *
 * <p>Keys are wrapped with an RSA key pair held in the AndroidKeyStore.
 * The RSA key pair alias is {@value #WRAPPING_KEY_ALIAS}.
 *
 * <p>Old keys are retained for decryption until they become pruneable:
 * a deprecated key may be removed after it has been deprecated for longer
 * than {@link #GRACE_PERIOD_MILLIS} <em>and</em> the key itself is older
 * than {@link #MAX_KEY_AGE_MILLIS}.
 */
public class KeyVersionRegistry {

    private static final String TAG = KeyVersionRegistry.class.getSimpleName();

    /** SharedPreferences file name for key metadata. */
    @VisibleForTesting
    /* package */ static final String METADATA_PREFS_NAME = "brokerks_metadata";

    /** Alias for the RSA wrapping key pair in the AndroidKeyStore. */
    @VisibleForTesting
    /* package */ static final String WRAPPING_KEY_ALIAS = "brokerks_wrapping_key";

    /** Prefix for wrapped key files. */
    @VisibleForTesting
    /* package */ static final String KEY_FILE_PREFIX = "brokerks_";

    /** Prefix for auto-generated version IDs (e.g. K001, K002). */
    private static final String VERSION_ID_PREFIX = "K";

    /** SharedPreferences key holding the active version ID. */
    private static final String PREFS_KEY_ACTIVE_VERSION = "activeVersion";

    /** SharedPreferences key holding the JSON array of key metadata. */
    private static final String PREFS_KEY_KEYS = "keys";

    /** Algorithm used to wrap (encrypt) symmetric keys. */
    private static final String WRAP_ALGORITHM = "RSA/ECB/PKCS1Padding";

    /** Algorithm for the RSA wrapping key itself. */
    private static final String WRAP_KEY_ALGORITHM = "RSA";

    /**
     * Maximum age of a key before it is eligible for pruning (3 years in milliseconds).
     */
    @VisibleForTesting
    /* package */ static final long MAX_KEY_AGE_MILLIS = 3L * 365 * 24 * 60 * 60 * 1000;

    /**
     * Grace period after deprecation before a key may be pruned (90 days in milliseconds).
     */
    @VisibleForTesting
    /* package */ static final long GRACE_PERIOD_MILLIS = 90L * 24 * 60 * 60 * 1000;

    /** Expected max size (bytes) of a wrapped key blob when reading from file.
     * A 2048-bit RSA-wrapped AES-256 key produces ~256 bytes; 1024 bytes provides ample headroom. */
    private static final int MAX_KEY_FILE_SIZE = 1024;

    private final Context mContext;

    /**
     * Constructs a new {@code KeyVersionRegistry}.
     *
     * @param context Android context used for SharedPreferences and file storage access.
     */
    public KeyVersionRegistry(@NonNull final Context context) {
        mContext = context;
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Returns the metadata for the currently active key.
     *
     * @return the active {@link KeyMetadata}, or {@code null} if no active key is set.
     * @throws ClientException if metadata storage cannot be read.
     */
    @Nullable
    public synchronized KeyMetadata getActiveKey() throws ClientException {
        final RegistryState state = loadState();
        if (state.activeVersion == null) {
            return null;
        }
        return findKeyInState(state, state.activeVersion);
    }

    /**
     * Returns the metadata for the key identified by {@code versionId}.
     *
     * @param versionId the version identifier (e.g. {@code "K001"}).
     * @return the matching {@link KeyMetadata}, or {@code null} if not found.
     * @throws ClientException if metadata storage cannot be read.
     */
    @Nullable
    public synchronized KeyMetadata getKeyByVersion(@NonNull final String versionId)
            throws ClientException {
        return findKeyInState(loadState(), versionId);
    }

    /**
     * Returns an unmodifiable list of all deprecated key metadata entries.
     *
     * @return list of deprecated {@link KeyMetadata} instances; never {@code null}.
     * @throws ClientException if metadata storage cannot be read.
     */
    @NonNull
    public synchronized List<KeyMetadata> getDeprecatedKeys() throws ClientException {
        final RegistryState state = loadState();
        final List<KeyMetadata> deprecated = new ArrayList<>();
        for (final KeyMetadata km : state.keys) {
            if (km.isDeprecated()) {
                deprecated.add(km);
            }
        }
        return Collections.unmodifiableList(deprecated);
    }

    /**
     * Generates a new AES-256 symmetric key, wraps it with the AndroidKeyStore RSA key pair,
     * persists the wrapped key material to a file, and stores the associated
     * {@link KeyMetadata} in SharedPreferences.
     *
     * <p>The version ID is auto-incremented (K001, K002, …).  The newly generated key
     * is <em>not</em> automatically made active; call {@link #setActiveKey(String)} to
     * promote it.
     *
     * @return the {@link KeyMetadata} for the newly created key.
     * @throws ClientException if key generation or storage fails.
     */
    @NonNull
    public synchronized KeyMetadata generateNewKey() throws ClientException {
        final String methodTag = TAG + ":generateNewKey";

        final Span span = OTelUtility.createSpanFromParent(
                SpanName.KeyVersionRegistryGenerateKey.name(),
                SpanExtension.current().getSpanContext());

        try (final Scope ignored = SpanExtension.makeCurrentSpan(span)) {
            final RegistryState state = loadState();
            final String newVersionId = nextVersionId(state);

            // Generate AES-256 key.
            final SecretKey secretKey = AES256SecretKeyGenerator.INSTANCE.generateRandomKey();

            // Wrap and persist key material.
            // RSA does not require an IV/AlgorithmParameters, so null is correct here.
            final KeyPair keyPair = getOrCreateWrappingKeyPair();
            final byte[] wrappedKey = AndroidKeyStoreUtil.wrap(secretKey, keyPair, WRAP_ALGORITHM, null);
            FileUtil.writeDataToFile(wrappedKey, getKeyFile(newVersionId));

            // Build and persist metadata. Explicitly set algorithm and keySize to match
            // the AES-256 key generated above.
            final KeyMetadata metadata = KeyMetadata.builder()
                    .versionId(newVersionId)
                    .createdAtMillis(System.currentTimeMillis())
                    .algorithm(KeyMetadata.DEFAULT_ALGORITHM)
                    .keySize(KeyMetadata.DEFAULT_KEY_SIZE)
                    .build();

            state.keys.add(metadata);
            saveState(state);

            Logger.info(methodTag, "Generated new key with versionId: " + newVersionId);
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
     * Marks the key identified by {@code versionId} as deprecated.
     * Deprecated keys may no longer be used for new encryptions, but are retained
     * to allow decryption of existing ciphertexts.
     *
     * @param versionId the version identifier of the key to deprecate.
     * @throws ClientException      if metadata storage cannot be read or written.
     * @throws IllegalStateException if no key with {@code versionId} exists.
     */
    public synchronized void deprecateKey(@NonNull final String versionId) throws ClientException {
        final RegistryState state = loadState();
        final int index = indexOfKey(state, versionId);
        if (index < 0) {
            throw new IllegalStateException("Key not found: " + versionId);
        }
        final KeyMetadata existing = state.keys.get(index);
        if (!existing.isDeprecated()) {
            state.keys.set(index, KeyMetadata.builder()
                    .versionId(existing.getVersionId())
                    .createdAtMillis(existing.getCreatedAtMillis())
                    .algorithm(existing.getAlgorithm())
                    .keySize(existing.getKeySize())
                    .deprecated(true)
                    .build());
            saveState(state);
        }
    }

    /**
     * Updates the active version pointer to the key identified by {@code versionId}.
     *
     * @param versionId the version identifier of the key to make active.
     * @throws ClientException      if metadata storage cannot be read or written.
     * @throws IllegalStateException if no key with {@code versionId} exists.
     */
    public synchronized void setActiveKey(@NonNull final String versionId) throws ClientException {
        final RegistryState state = loadState();
        if (indexOfKey(state, versionId) < 0) {
            throw new IllegalStateException("Key not found: " + versionId);
        }
        state.activeVersion = versionId;
        saveState(state);
    }

    /**
     * Loads and unwraps the {@link SecretKey} associated with {@code versionId}.
     *
     * @param versionId the version identifier of the key to load.
     * @return the unwrapped {@link SecretKey}.
     * @throws ClientException      if the key cannot be read or unwrapped.
     * @throws IllegalStateException if no metadata exists for {@code versionId}.
     */
    @NonNull
    public synchronized SecretKey loadSecretKey(@NonNull final String versionId)
            throws ClientException {
        final String methodTag = TAG + ":loadSecretKey";

        // Ensure metadata entry exists.
        final KeyMetadata metadata = getKeyByVersion(versionId);
        if (metadata == null) {
            throw new IllegalStateException("Key not found in registry: " + versionId);
        }

        final KeyPair keyPair = AndroidKeyStoreUtil.readKey(WRAPPING_KEY_ALIAS);
        if (keyPair == null) {
            throw new ClientException(
                    ClientException.KEYSTORE_NOT_INITIALIZED,
                    "Wrapping key pair not found in AndroidKeyStore for alias: " + WRAPPING_KEY_ALIAS);
        }

        final byte[] wrappedKey = FileUtil.readFromFile(getKeyFile(versionId), MAX_KEY_FILE_SIZE);
        if (wrappedKey == null) {
            // readFromFile returns null when the file does not exist.
            throw new ClientException(
                    ClientException.IO_ERROR,
                    "Wrapped key file does not exist for versionId: " + versionId);
        }

        Logger.info(methodTag, "Unwrapping key for versionId: " + versionId);
        return AndroidKeyStoreUtil.unwrap(wrappedKey, AES_ALGORITHM, keyPair, WRAP_ALGORITHM, null);
    }

    /**
     * Removes key entries (metadata and wrapped key files) for keys that are no longer needed.
     *
     * <p>A key is eligible for pruning when its total age from creation exceeds
     * {@link #MAX_KEY_AGE_MILLIS} + {@link #GRACE_PERIOD_MILLIS}, regardless of whether it
     * has been explicitly deprecated. This ensures that stale keys are cleaned up even if
     * deprecation was never called on them.
     *
     * <p>The active key is never pruned.
     *
     * @throws ClientException if metadata storage cannot be read or written.
     */
    public synchronized void pruneExpiredKeys() throws ClientException {
        final String methodTag = TAG + ":pruneExpiredKeys";
        final RegistryState state = loadState();
        final long now = System.currentTimeMillis();

        final List<KeyMetadata> toKeep = new ArrayList<>();
        for (final KeyMetadata km : state.keys) {
            final boolean isActive = km.getVersionId().equals(state.activeVersion);
            // Any non-active key whose total age exceeds MAX_KEY_AGE_MILLIS + GRACE_PERIOD_MILLIS is prunable.
            final boolean isPrunable = (now - km.getCreatedAtMillis()) > (MAX_KEY_AGE_MILLIS + GRACE_PERIOD_MILLIS);

            if (!isActive && isPrunable) {
                Logger.info(methodTag, "Pruning expired key: " + km.getVersionId());
                deleteKeyFile(km.getVersionId());
            } else {
                toKeep.add(km);
            }
        }

        if (toKeep.size() != state.keys.size()) {
            state.keys = toKeep;
            saveState(state);
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Loads registry state from SharedPreferences.
     *
     * @return the current {@link RegistryState}.
     * @throws ClientException if JSON parsing fails.
     */
    @NonNull
    private RegistryState loadState() throws ClientException {
        final String methodTag = TAG + ":loadState";
        final SharedPreferences prefs = getSharedPreferences();
        final String json = prefs.getString(PREFS_KEY_KEYS, null);
        final String activeVersion = prefs.getString(PREFS_KEY_ACTIVE_VERSION, null);

        final RegistryState state = new RegistryState();
        state.activeVersion = activeVersion;

        if (json == null) {
            return state;
        }

        try {
            final JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                state.keys.add(KeyMetadata.fromJson(array.getString(i)));
            }
        } catch (final JSONException e) {
            Logger.warn(methodTag, "Failed to parse key metadata JSON: " + e.getMessage());
            throw new ClientException(ClientException.JSON_PARSE_FAILURE,
                    "Failed to parse key registry metadata", e);
        }

        return state;
    }

    /**
     * Persists the given {@link RegistryState} to SharedPreferences.
     *
     * @param state the state to save.
     * @throws ClientException if JSON serialization fails.
     */
    private void saveState(@NonNull final RegistryState state) throws ClientException {
        final String methodTag = TAG + ":saveState";
        try {
            final JSONArray array = new JSONArray();
            for (final KeyMetadata km : state.keys) {
                array.put(km.toJson());
            }

            final SharedPreferences.Editor editor = getSharedPreferences().edit();
            editor.putString(PREFS_KEY_KEYS, array.toString());
            if (state.activeVersion != null) {
                editor.putString(PREFS_KEY_ACTIVE_VERSION, state.activeVersion);
            } else {
                editor.remove(PREFS_KEY_ACTIVE_VERSION);
            }
            editor.apply();
        } catch (final JSONException e) {
            Logger.error(methodTag, "Failed to serialize key metadata", e);
            throw new ClientException(ClientException.JSON_PARSE_FAILURE,
                    "Failed to serialize key registry metadata", e);
        }
    }

    /**
     * Returns the {@link SharedPreferences} instance for key metadata storage.
     *
     * @return a private {@link SharedPreferences} instance.
     */
    @NonNull
    private SharedPreferences getSharedPreferences() {
        return mContext.getSharedPreferences(METADATA_PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Returns the {@link File} used to store the wrapped key for {@code versionId}.
     *
     * @param versionId the version identifier.
     * @return the key {@link File}.
     */
    @NonNull
    private File getKeyFile(@NonNull final String versionId) {
        return new File(
                mContext.getDir(mContext.getPackageName(), Context.MODE_PRIVATE),
                KEY_FILE_PREFIX + versionId);
    }

    /**
     * Deletes the wrapped key file for {@code versionId}.
     *
     * @param versionId the version identifier whose file should be removed.
     */
    private void deleteKeyFile(@NonNull final String versionId) {
        FileUtil.deleteFile(getKeyFile(versionId));
    }

    /**
     * Calculates the next auto-incremented version ID based on the current registry state.
     * Version IDs follow the pattern K001, K002, K003, etc.
     *
     * @param state the current registry state.
     * @return the next version ID string.
     */
    @NonNull
    private String nextVersionId(@NonNull final RegistryState state) {
        int max = 0;
        for (final KeyMetadata km : state.keys) {
            final String vid = km.getVersionId();
            if (vid.startsWith(VERSION_ID_PREFIX)) {
                try {
                    final int num = Integer.parseInt(vid.substring(VERSION_ID_PREFIX.length()));
                    if (num > max) {
                        max = num;
                    }
                } catch (final NumberFormatException ignored) {
                    // Skip non-numeric suffixes.
                }
            }
        }
        return String.format(Locale.ROOT, "%s%03d", VERSION_ID_PREFIX, max + 1);
    }

    /**
     * Returns the index of the key with the given {@code versionId} in the state's key list,
     * or {@code -1} if not found.
     *
     * @param state     the registry state.
     * @param versionId the version ID to search for.
     * @return the index, or {@code -1} if absent.
     */
    private int indexOfKey(@NonNull final RegistryState state, @NonNull final String versionId) {
        for (int i = 0; i < state.keys.size(); i++) {
            if (versionId.equals(state.keys.get(i).getVersionId())) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Finds and returns the {@link KeyMetadata} for the given {@code versionId} in the state,
     * or {@code null} if not present.
     *
     * @param state     the registry state.
     * @param versionId the version ID to look up.
     * @return the matching metadata, or {@code null}.
     */
    @Nullable
    private KeyMetadata findKeyInState(@NonNull final RegistryState state,
                                       @NonNull final String versionId) {
        final int index = indexOfKey(state, versionId);
        return index >= 0 ? state.keys.get(index) : null;
    }

    /**
     * Loads the existing RSA wrapping key pair from the AndroidKeyStore, or creates a new one
     * if none exists.
     *
     * @return the RSA {@link KeyPair} used to wrap/unwrap symmetric keys.
     * @throws ClientException if key pair loading or generation fails.
     */
    @NonNull
    private KeyPair getOrCreateWrappingKeyPair() throws ClientException {
        final String methodTag = TAG + ":getOrCreateWrappingKeyPair";
        KeyPair keyPair = AndroidKeyStoreUtil.readKey(WRAPPING_KEY_ALIAS);
        if (keyPair == null) {
            Logger.info(methodTag, "No existing wrapping key pair, generating a new one.");
            keyPair = generateNewWrappingKeyPair();
        }
        return keyPair;
    }

    /**
     * Generates a new RSA wrapping key pair in the AndroidKeyStore.
     *
     * @return the newly generated {@link KeyPair}.
     * @throws ClientException if key pair generation fails.
     */
    @NonNull
    private KeyPair generateNewWrappingKeyPair() throws ClientException {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return generateWrappingKeyPairModernApi();
        } else {
            return generateWrappingKeyPairLegacyApi();
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    @NonNull
    private KeyPair generateWrappingKeyPairModernApi() throws ClientException {
        final AlgorithmParameterSpec spec = new KeyGenParameterSpec.Builder(
                WRAPPING_KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setKeySize(2048)
                .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1)
                .build();
        return AndroidKeyStoreUtil.generateKeyPair(WRAP_KEY_ALGORITHM, spec);
    }

    @NonNull
    private KeyPair generateWrappingKeyPairLegacyApi() throws ClientException {
        final String certInfo = String.format(Locale.ROOT, "CN=%s, OU=%s",
                WRAPPING_KEY_ALIAS,
                mContext.getPackageName());

        final Calendar start = Calendar.getInstance();
        final Calendar end = Calendar.getInstance();
        end.add(Calendar.YEAR, 100);

        @SuppressWarnings("deprecation")
        final AlgorithmParameterSpec spec = new KeyPairGeneratorSpec.Builder(mContext)
                .setAlias(WRAPPING_KEY_ALIAS)
                .setSubject(new X500Principal(certInfo))
                .setSerialNumber(BigInteger.ONE)
                .setStartDate(start.getTime())
                .setEndDate(end.getTime())
                .build();
        return AndroidKeyStoreUtil.generateKeyPair(WRAP_KEY_ALGORITHM, spec);
    }

    // -------------------------------------------------------------------------
    // Internal state holder
    // -------------------------------------------------------------------------

    /**
     * Mutable holder for in-memory registry state, loaded from and saved to SharedPreferences.
     */
    private static final class RegistryState {
        /** Version ID of the currently active key, or {@code null} if none. */
        @Nullable
        String activeVersion;

        /** Ordered list of all known key metadata entries. */
        @NonNull
        List<KeyMetadata> keys = new ArrayList<>();
    }
}
