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
 * Immutable metadata describing a symmetric encryption key version.
 *
 * <p>Used by the KeyVersionRegistry to track the lifecycle of symmetric keys and by
 * encryption managers to select the active key or fall back to deprecated keys for
 * decryption only.</p>
 */
public final class KeyMetadata {

    /** Default symmetric encryption algorithm. */
    public static final String DEFAULT_ALGORITHM = "AES/CBC/PKCS5Padding";

    /** Default key size in bits. */
    public static final int DEFAULT_KEY_SIZE = 256;

    private static final String FIELD_VERSION_ID = "versionId";
    private static final String FIELD_CREATED_AT_MILLIS = "createdAtMillis";
    private static final String FIELD_ALGORITHM = "algorithm";
    private static final String FIELD_KEY_SIZE = "keySize";
    private static final String FIELD_IS_DEPRECATED = "isDeprecated";

    /**
     * Key identifier, e.g. {@code "K001"}, {@code "K002"}.
     */
    private final String mVersionId;

    /**
     * Unix timestamp (milliseconds) at which this key was created.
     */
    private final long mCreatedAtMillis;

    /**
     * Encryption algorithm associated with this key (e.g. {@code "AES/CBC/PKCS5Padding"}).
     */
    private final String mAlgorithm;

    /**
     * Key size in bits (e.g. {@code 256}).
     */
    private final int mKeySize;

    /**
     * When {@code true} the key may only be used for decryption; new encryptions must use
     * a non-deprecated key.
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
     * Returns the key version identifier.
     *
     * @return non-null version id string.
     */
    @NonNull
    public String getVersionId() {
        return mVersionId;
    }

    /**
     * Returns the Unix timestamp (milliseconds) at which this key was created.
     *
     * @return creation time in epoch milliseconds.
     */
    public long getCreatedAtMillis() {
        return mCreatedAtMillis;
    }

    /**
     * Returns the encryption algorithm string for this key.
     *
     * @return non-null algorithm string.
     */
    @NonNull
    public String getAlgorithm() {
        return mAlgorithm;
    }

    /**
     * Returns the key size in bits.
     *
     * @return key size in bits.
     */
    public int getKeySize() {
        return mKeySize;
    }

    /**
     * Returns whether this key is deprecated. A deprecated key may only be used for
     * decryption; new data must be encrypted with a non-deprecated key.
     *
     * @return {@code true} if the key is deprecated.
     */
    public boolean isDeprecated() {
        return mIsDeprecated;
    }

    /**
     * Serializes this instance to a JSON string.
     *
     * @return a JSON string representation of this {@link KeyMetadata}.
     * @throws JSONException if serialization fails.
     */
    @NonNull
    public String toJson() throws JSONException {
        final JSONObject json = new JSONObject();
        json.put(FIELD_VERSION_ID, mVersionId);
        json.put(FIELD_CREATED_AT_MILLIS, mCreatedAtMillis);
        json.put(FIELD_ALGORITHM, mAlgorithm);
        json.put(FIELD_KEY_SIZE, mKeySize);
        json.put(FIELD_IS_DEPRECATED, mIsDeprecated);
        return json.toString();
    }

    /**
     * Deserializes a {@link KeyMetadata} instance from a JSON string.
     *
     * @param json the JSON string produced by {@link #toJson()}.
     * @return a reconstructed {@link KeyMetadata} instance.
     * @throws JSONException if {@code json} is malformed or is missing required fields.
     */
    @NonNull
    public static KeyMetadata fromJson(@NonNull final String json) throws JSONException {
        final JSONObject jsonObject = new JSONObject(json);
        return new Builder()
                .versionId(jsonObject.getString(FIELD_VERSION_ID))
                .createdAtMillis(jsonObject.getLong(FIELD_CREATED_AT_MILLIS))
                .algorithm(jsonObject.getString(FIELD_ALGORITHM))
                .keySize(jsonObject.getInt(FIELD_KEY_SIZE))
                .isDeprecated(jsonObject.getBoolean(FIELD_IS_DEPRECATED))
                .build();
    }

    /**
     * Builder for constructing {@link KeyMetadata} instances.
     */
    public static final class Builder {

        private String mVersionId;
        private long mCreatedAtMillis;
        private String mAlgorithm = DEFAULT_ALGORITHM;
        private int mKeySize = DEFAULT_KEY_SIZE;
        private boolean mIsDeprecated = false;

        /**
         * Sets the key version identifier.
         *
         * @param versionId non-null version id, e.g. {@code "K001"}.
         * @return this builder.
         */
        @NonNull
        public Builder versionId(@NonNull final String versionId) {
            mVersionId = versionId;
            return this;
        }

        /**
         * Sets the creation timestamp.
         *
         * @param createdAtMillis Unix timestamp in milliseconds.
         * @return this builder.
         */
        @NonNull
        public Builder createdAtMillis(final long createdAtMillis) {
            mCreatedAtMillis = createdAtMillis;
            return this;
        }

        /**
         * Sets the encryption algorithm. Defaults to {@link #DEFAULT_ALGORITHM}.
         *
         * @param algorithm non-null algorithm string.
         * @return this builder.
         */
        @NonNull
        public Builder algorithm(@NonNull final String algorithm) {
            mAlgorithm = algorithm;
            return this;
        }

        /**
         * Sets the key size in bits. Defaults to {@link #DEFAULT_KEY_SIZE}.
         *
         * @param keySize key size in bits; must be positive.
         * @return this builder.
         * @throws IllegalArgumentException if {@code keySize} is not positive.
         */
        @NonNull
        public Builder keySize(final int keySize) {
            if (keySize <= 0) {
                throw new IllegalArgumentException("keySize must be a positive value.");
            }
            mKeySize = keySize;
            return this;
        }

        /**
         * Sets whether the key is deprecated.
         *
         * @param isDeprecated {@code true} if this key should only be used for decryption.
         * @return this builder.
         */
        @NonNull
        public Builder isDeprecated(final boolean isDeprecated) {
            mIsDeprecated = isDeprecated;
            return this;
        }

        /**
         * Builds a new {@link KeyMetadata} instance.
         *
         * @return a new {@link KeyMetadata}.
         * @throws IllegalStateException if {@code versionId} has not been set.
         */
        @NonNull
        public KeyMetadata build() {
            if (mVersionId == null || mVersionId.isEmpty()) {
                throw new IllegalStateException("versionId must be set before calling build().");
            }
            return new KeyMetadata(this);
        }
    }
}
