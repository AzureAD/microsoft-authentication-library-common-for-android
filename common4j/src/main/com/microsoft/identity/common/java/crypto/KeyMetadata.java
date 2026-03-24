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

import lombok.Getter;
import lombok.NonNull;

/**
 * Immutable metadata describing a symmetric encryption key version.
 *
 * <p>Used by the KeyVersionRegistry to track the lifecycle of symmetric keys and by
 * encryption managers to select the active key or fall back to deprecated keys for
 * decryption only.</p>
 */
@Getter
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
    private final String versionId;

    /**
     * Unix timestamp (milliseconds) at which this key was created.
     */
    private final long createdAtMillis;

    /**
     * Encryption algorithm for this key (e.g. {@code "AES/CBC/PKCS5Padding"}).
     * Defaults to {@link #DEFAULT_ALGORITHM}.
     */
    private final String algorithm;

    /**
     * Key size in bits (e.g. {@code 256}).
     * Defaults to {@link #DEFAULT_KEY_SIZE}.
     */
    private final int keySize;

    /**
     * When {@code true} the key may only be used for decryption; new encryptions must use
     * a non-deprecated key.
     */
    private final boolean deprecated;

    /**
     * All-args constructor called by the builder; validates required fields.
     *
     * @throws IllegalStateException     if {@code versionId} is null or blank.
     * @throws IllegalArgumentException  if {@code keySize} is not positive.
     */
    private KeyMetadata(final String versionId, final long createdAtMillis,
                        final String algorithm, final int keySize, final boolean deprecated) {
        if (versionId == null || versionId.trim().isEmpty()) {
            throw new IllegalStateException("versionId must be a non-blank string.");
        }
        if (keySize <= 0) {
            throw new IllegalArgumentException("keySize must be a positive value.");
        }
        this.versionId = versionId;
        this.createdAtMillis = createdAtMillis;
        this.algorithm = algorithm;
        this.keySize = keySize;
        this.deprecated = deprecated;
    }

    /**
     * Returns a new {@link Builder} for constructing a {@link KeyMetadata} instance.
     *
     * @return a new {@link Builder}.
     */
    @NonNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link KeyMetadata}.
     *
     * <p>{@code algorithm}, {@code keySize}, and {@code deprecated} are optional and fall back
     * to {@link #DEFAULT_ALGORITHM}, {@link #DEFAULT_KEY_SIZE}, and {@code false}
     * respectively. {@code versionId} and {@code createdAtMillis} are required.</p>
     */
    public static final class Builder {

        private String versionId;
        private long createdAtMillis;
        private String algorithm = DEFAULT_ALGORITHM;
        private int keySize = DEFAULT_KEY_SIZE;
        private boolean deprecated = false;

        private Builder() {}

        /**
         * Sets the key version identifier (required).
         *
         * @param versionId a non-blank key identifier, e.g. {@code "K001"}.
         * @return this builder.
         */
        @NonNull
        public Builder versionId(@NonNull final String versionId) {
            this.versionId = versionId;
            return this;
        }

        /**
         * Sets the Unix timestamp (milliseconds) at which this key was created (required).
         *
         * @param createdAtMillis creation timestamp in milliseconds.
         * @return this builder.
         */
        @NonNull
        public Builder createdAtMillis(final long createdAtMillis) {
            this.createdAtMillis = createdAtMillis;
            return this;
        }

        /**
         * Sets the encryption algorithm (optional; defaults to {@link #DEFAULT_ALGORITHM}).
         *
         * @param algorithm the algorithm string, e.g. {@code "AES/CBC/PKCS5Padding"}.
         * @return this builder.
         */
        @NonNull
        public Builder algorithm(@NonNull final String algorithm) {
            this.algorithm = algorithm;
            return this;
        }

        /**
         * Sets the key size in bits (optional; defaults to {@link #DEFAULT_KEY_SIZE}).
         *
         * @param keySize a positive key size, e.g. {@code 256}.
         * @return this builder.
         */
        @NonNull
        public Builder keySize(final int keySize) {
            this.keySize = keySize;
            return this;
        }

        /**
         * Marks the key as deprecated (optional; defaults to {@code false}).
         *
         * @param deprecated {@code true} if the key should only be used for decryption.
         * @return this builder.
         */
        @NonNull
        public Builder deprecated(final boolean deprecated) {
            this.deprecated = deprecated;
            return this;
        }

        /**
         * Builds and validates a {@link KeyMetadata} instance.
         *
         * @return a new {@link KeyMetadata}.
         * @throws IllegalStateException    if {@code versionId} is null or blank.
         * @throws IllegalArgumentException if {@code keySize} is not positive.
         */
        @NonNull
        public KeyMetadata build() {
            return new KeyMetadata(versionId, createdAtMillis, algorithm, keySize, deprecated);
        }
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
        json.put(FIELD_VERSION_ID, versionId);
        json.put(FIELD_CREATED_AT_MILLIS, createdAtMillis);
        json.put(FIELD_ALGORITHM, algorithm);
        json.put(FIELD_KEY_SIZE, keySize);
        json.put(FIELD_IS_DEPRECATED, deprecated);
        return json.toString();
    }

    /**
     * Deserializes a {@link KeyMetadata} instance from a JSON string.
     *
     * <p>{@code algorithm}, {@code keySize}, and {@code deprecated} are optional in the JSON;
     * missing fields fall back to their defaults ({@link #DEFAULT_ALGORITHM},
     * {@link #DEFAULT_KEY_SIZE}, and {@code false} respectively). Only {@code versionId} and
     * {@code createdAtMillis} are required.</p>
     *
     * @param json the JSON string produced by {@link #toJson()}.
     * @return a reconstructed {@link KeyMetadata} instance.
     * @throws JSONException if {@code json} is malformed or missing required fields.
     */
    @NonNull
    public static KeyMetadata fromJson(@NonNull final String json) throws JSONException {
        final JSONObject obj = new JSONObject(json);
        try {
            return KeyMetadata.builder()
                    .versionId(obj.getString(FIELD_VERSION_ID))
                    .createdAtMillis(obj.getLong(FIELD_CREATED_AT_MILLIS))
                    .algorithm(obj.optString(FIELD_ALGORITHM, DEFAULT_ALGORITHM))
                    .keySize(obj.optInt(FIELD_KEY_SIZE, DEFAULT_KEY_SIZE))
                    .deprecated(obj.optBoolean(FIELD_IS_DEPRECATED, false))
                    .build();
        } catch (final IllegalArgumentException | IllegalStateException e) {
            throw new JSONException("Invalid key metadata JSON: " + e.getMessage());
        }
    }
}
