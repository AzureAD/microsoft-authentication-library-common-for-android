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
package com.microsoft.identity.common.java.crypto;

import org.json.JSONException;
import org.json.JSONObject;

import lombok.NonNull;

/**
 * Immutable data class that stores metadata for a symmetric encryption key.
 * <p>
 * Used by the KeyVersionRegistry to track multiple key versions, enabling
 * SDL-compliant symmetric key rotation in the Android Broker.
 */
public final class KeyMetadata {

    private static final String JSON_KEY_VERSION_ID = "version_id";
    private static final String JSON_KEY_CREATED_AT_MILLIS = "created_at_millis";
    private static final String JSON_KEY_ALGORITHM = "algorithm";
    private static final String JSON_KEY_KEY_SIZE = "key_size";
    private static final String JSON_KEY_IS_DEPRECATED = "is_deprecated";

    /**
     * Default encryption algorithm used for symmetric keys.
     */
    public static final String DEFAULT_ALGORITHM = "AES/CBC/PKCS5Padding";

    /**
     * Default key size in bits.
     */
    public static final int DEFAULT_KEY_SIZE = 256;

    /**
     * Unique identifier for this key version, e.g. "K001", "K002".
     */
    @NonNull
    private final String mVersionId;

    /**
     * Unix timestamp (in milliseconds) when this key was created.
     */
    private final long mCreatedAtMillis;

    /**
     * Encryption algorithm used with this key, e.g. "AES/CBC/PKCS5Padding".
     */
    @NonNull
    private final String mAlgorithm;

    /**
     * Key size in bits, e.g. 256.
     */
    private final int mKeySize;

    /**
     * When {@code true}, this key may only be used for decryption (not encryption).
     * Deprecated keys are retained solely to decrypt data encrypted by older key versions.
     */
    private final boolean mIsDeprecated;

    private KeyMetadata(@NonNull final Builder builder) {
        mVersionId = builder.mVersionId;
        mCreatedAtMillis = builder.mCreatedAtMillis;
        mAlgorithm = builder.mAlgorithm;
        mKeySize = builder.mKeySize;
        mIsDeprecated = builder.mIsDeprecated;
    }

    /**
     * Returns the unique identifier for this key version.
     *
     * @return version id string, e.g. "K001"
     */
    @NonNull
    public String getVersionId() {
        return mVersionId;
    }

    /**
     * Returns the Unix timestamp (in milliseconds) when this key was created.
     *
     * @return creation time in epoch milliseconds
     */
    public long getCreatedAtMillis() {
        return mCreatedAtMillis;
    }

    /**
     * Returns the encryption algorithm associated with this key.
     *
     * @return algorithm string, e.g. "AES/CBC/PKCS5Padding"
     */
    @NonNull
    public String getAlgorithm() {
        return mAlgorithm;
    }

    /**
     * Returns the key size in bits.
     *
     * @return key size, e.g. 256
     */
    public int getKeySize() {
        return mKeySize;
    }

    /**
     * Returns whether this key is deprecated.
     * <p>
     * A deprecated key may only be used for decryption, not encryption.
     *
     * @return {@code true} if the key is deprecated; {@code false} otherwise
     */
    public boolean isDeprecated() {
        return mIsDeprecated;
    }

    /**
     * Serializes this {@link KeyMetadata} instance to a JSON string.
     *
     * @return JSON string representation of this object
     * @throws JSONException if JSON construction fails
     */
    @NonNull
    public String toJson() throws JSONException {
        final JSONObject json = new JSONObject();
        json.put(JSON_KEY_VERSION_ID, mVersionId);
        json.put(JSON_KEY_CREATED_AT_MILLIS, mCreatedAtMillis);
        json.put(JSON_KEY_ALGORITHM, mAlgorithm);
        json.put(JSON_KEY_KEY_SIZE, mKeySize);
        json.put(JSON_KEY_IS_DEPRECATED, mIsDeprecated);
        return json.toString();
    }

    /**
     * Deserializes a {@link KeyMetadata} instance from a JSON string produced by {@link #toJson()}.
     *
     * @param json JSON string to parse; must not be null
     * @return reconstructed {@link KeyMetadata} instance
     * @throws JSONException if {@code json} is malformed or missing required fields
     */
    @NonNull
    public static KeyMetadata fromJson(@NonNull final String json) throws JSONException {
        final JSONObject jsonObject = new JSONObject(json);
        return new Builder(jsonObject.getString(JSON_KEY_VERSION_ID))
                .createdAtMillis(jsonObject.getLong(JSON_KEY_CREATED_AT_MILLIS))
                .algorithm(jsonObject.getString(JSON_KEY_ALGORITHM))
                .keySize(jsonObject.getInt(JSON_KEY_KEY_SIZE))
                .isDeprecated(jsonObject.getBoolean(JSON_KEY_IS_DEPRECATED))
                .build();
    }

    /**
     * Builder for constructing immutable {@link KeyMetadata} instances.
     */
    public static final class Builder {

        @NonNull
        private final String mVersionId;
        private long mCreatedAtMillis;
        @NonNull
        private String mAlgorithm = DEFAULT_ALGORITHM;
        private int mKeySize = DEFAULT_KEY_SIZE;
        private boolean mIsDeprecated = false;

        /**
         * Creates a new Builder with the required version id.
         *
         * @param versionId unique identifier for the key version, e.g. "K001"; must not be null or empty
         */
        public Builder(@NonNull final String versionId) {
            if (versionId.isEmpty()) {
                throw new IllegalArgumentException("versionId must not be empty");
            }
            mVersionId = versionId;
        }

        /**
         * Sets the Unix timestamp (in milliseconds) when the key was created.
         *
         * @param createdAtMillis creation time in epoch milliseconds; must be non-negative
         * @return this builder
         */
        @NonNull
        public Builder createdAtMillis(final long createdAtMillis) {
            if (createdAtMillis < 0) {
                throw new IllegalArgumentException("createdAtMillis must be non-negative");
            }
            mCreatedAtMillis = createdAtMillis;
            return this;
        }

        /**
         * Sets the encryption algorithm. Defaults to {@link KeyMetadata#DEFAULT_ALGORITHM}.
         *
         * @param algorithm encryption algorithm string; must not be null
         * @return this builder
         */
        @NonNull
        public Builder algorithm(@NonNull final String algorithm) {
            mAlgorithm = algorithm;
            return this;
        }

        /**
         * Sets the key size in bits. Defaults to {@link KeyMetadata#DEFAULT_KEY_SIZE}.
         *
         * @param keySize key size in bits; must be positive
         * @return this builder
         */
        @NonNull
        public Builder keySize(final int keySize) {
            if (keySize <= 0) {
                throw new IllegalArgumentException("keySize must be positive");
            }
            mKeySize = keySize;
            return this;
        }

        /**
         * Sets whether the key is deprecated. Defaults to {@code false}.
         * <p>
         * A deprecated key may only be used for decryption, not encryption.
         *
         * @param isDeprecated {@code true} if the key should be treated as deprecated
         * @return this builder
         */
        @NonNull
        public Builder isDeprecated(final boolean isDeprecated) {
            mIsDeprecated = isDeprecated;
            return this;
        }

        /**
         * Constructs and returns the {@link KeyMetadata} instance.
         * <p>
         * If {@link #createdAtMillis(long)} was not called, {@code createdAtMillis} defaults to 0.
         *
         * @return new immutable {@link KeyMetadata}
         */
        @NonNull
        public KeyMetadata build() {
            return new KeyMetadata(this);
        }
    }
}
