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

import cz.msebera.android.httpclient.extras.Base64;
import lombok.NonNull;

import static com.microsoft.identity.common.java.exception.ClientException.NO_SUCH_ALGORITHM;

public abstract class AES256KeyLoader implements ISecretKeyLoader {
    private static final Object TAG = AES256KeyLoader.class.getSimpleName();

    /**
     * Key size
     */
    private static final int KEY_SIZE = 256;

    /**
     * Key spec algorithm.
     */
    public static final String AES_ALGORITHM = "AES";

    /**
     * AES is 16 bytes (128 bits), thus PKCS#5 padding should not work, but in
     * Java AES/CBC/PKCS5Padding is default(!) algorithm name, thus PKCS5 here
     * probably doing PKCS7. We decide to go with Java default string.
     */
    private static final String CIPHER_ALGORITHM = "AES/CBC/PKCS5Padding";

    @Override
    @NonNull
    public String getKeySpecAlgorithm() {
        return AES_ALGORITHM;
    }

    @Override
    @NonNull
    public String getCipherAlgorithm(){
        return CIPHER_ALGORITHM;
    }

    /**
     * generate a random AES-256 secret key.
     *
     * @return SecretKey.
     */
    @NonNull
    protected SecretKey generateRandomKey() throws ClientException {
        final String methodName = ":generateRandomKey";

        try {
            final KeyGenerator keygen = KeyGenerator.getInstance(AES_ALGORITHM);
            keygen.init(KEY_SIZE, new SecureRandom());
            return keygen.generateKey();
        } catch (NoSuchAlgorithmException e) {
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
     * generate a random AES-256 secret key from rawbytes.
     * <p>
     * If a non AES-256 rawBytes is provided, this will still return a SecretKey,
     * but an exception would be thrown in {@link StorageEncryptionManager}
     * during encryption/decryption.
     *
     * @return SecretKey.
     */
    @NonNull
    protected SecretKey generateKeyFromRawBytes(@NonNull final byte[] rawBytes) {
        return new SecretKeySpec(rawBytes, AES_ALGORITHM);
    }

    /**
     * Serializes a {@link SecretKey} into a {@link String}.
     */
    public String serializeSecretKey(@NonNull final SecretKey key) {
        return Base64.encodeToString(key.getEncoded(), Base64.DEFAULT);
    }

    /**
     * Deserializes a {@link String} into a {@link SecretKey}.
     */
    public SecretKey deserializeSecretKey(@NonNull final String serializedKey) {
        return generateKeyFromRawBytes(Base64.decode(serializedKey, Base64.DEFAULT));
    }

}
