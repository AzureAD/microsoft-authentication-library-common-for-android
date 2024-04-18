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
package com.microsoft.identity.common.java.crypto.key;

import com.microsoft.identity.common.java.crypto.StorageEncryptionManager;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.logging.Logger;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import lombok.NonNull;

import static com.microsoft.identity.common.java.exception.ClientException.NO_SUCH_ALGORITHM;

/**
 * Abstracts how a {@link SecretKey} is loaded/cached/sourced/used.
 */
public abstract class AbstractSecretKeyLoader {
    private static final String TAG = AbstractSecretKeyLoader.class.getSimpleName();

    /**
     * Returns this key's alias/name.
     * Each key will have a unique alias/name.
     */
    @NonNull
    public abstract String getAlias();

    /**
     * Returns the key.
     */
    @NonNull
    public abstract SecretKey getKey() throws ClientException;

    /**
     * Returns the Algorithm of this key.
     * This must be compatible with {@link KeyGenerator#getInstance(String)}
     */
    @NonNull
    protected abstract String getKeySpecAlgorithm();

    /**
     * Returns the size of this key.
     * This must be compatible with {@link KeyGenerator#init(int, SecureRandom)} )}
     */
    protected abstract int getKeySize();

    /**
     * Gets an identifier of this key type.
     * This might be padded into the encrypted string.
     */
    @NonNull
    public abstract String getKeyTypeIdentifier();

    /**
     * Gets the cipher algorithm that is meant to be used with this key type.
     */
    @NonNull
    public abstract String getCipherAlgorithm();

    /**
     * Generate a random AES-256 secret key.
     *
     * @return SecretKey.
     */
    @NonNull
    protected SecretKey generateRandomKey() throws ClientException {
        final String methodName = ":generateRandomKey";

        try {
            final KeyGenerator keygen = KeyGenerator.getInstance(getKeySpecAlgorithm());
            keygen.init(getKeySize(), new SecureRandom());
            return keygen.generateKey();
        } catch (final NoSuchAlgorithmException e) {
            final ClientException clientException = new ClientException(
                    NO_SUCH_ALGORITHM,
                    e.getMessage(),
                    e
            );

            Logger.error(
                    TAG + methodName,
                    clientException.getErrorCode(),
                    e
            );

            throw clientException;
        }
    }

    /**
     * Generate a random AES-256 secret key from rawbytes.
     * <p>
     * If a non AES-256 rawBytes is provided, this will still return a SecretKey,
     * but an exception would be thrown in {@link StorageEncryptionManager}
     * during encryption/decryption.
     *
     * @return SecretKey.
     */
    @NonNull
    protected SecretKey generateKeyFromRawBytes(@NonNull final byte[] rawBytes) {
        return new SecretKeySpec(rawBytes, getKeySpecAlgorithm());
    }
}
