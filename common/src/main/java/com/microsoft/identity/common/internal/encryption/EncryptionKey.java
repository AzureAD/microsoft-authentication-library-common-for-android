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

package com.microsoft.identity.common.internal.encryption;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

class EncryptionKey {
    /**
     * HMac key hashing algorithm.
     */
    private static final String HMAC_KEY_HASH_ALGORITHM = "SHA256";

    private KeyType mKeyType = null;
    private SecretKey mKey = null;
    private SecretKey mHMACKey = null;

    public EncryptionKey(@NonNull final KeyType keyType,
                         @NonNull final SecretKey key) throws NoSuchAlgorithmException {
        mKeyType = keyType;
        mKey = key;
        mHMACKey = deriveHMACKeyFromSecretKey(mKey);
    }

    @NonNull
    KeyType getKeyType() {
        return mKeyType;
    }

    @NonNull
    SecretKey getKey() {
        return mKey;
    }

    @NonNull
    SecretKey getHMACKey() {
        return mHMACKey;
    }

    /**
     * Derive HMAC key from given key.
     *
     * @param key SecretKey from which HMAC key has to be derived
     * @return SecretKey
     * @throws NoSuchAlgorithmException
     */
    private SecretKey deriveHMACKeyFromSecretKey(@NonNull final SecretKey key) throws NoSuchAlgorithmException {
        // Some keys may not produce byte[] with getEncoded
        final byte[] encodedKey = key.getEncoded();
        if (encodedKey != null) {
            final MessageDigest digester = MessageDigest.getInstance(HMAC_KEY_HASH_ALGORITHM);
            return new SecretKeySpec(digester.digest(encodedKey), BaseEncryptionManager.KEYSPEC_ALGORITHM);
        }

        return key;
    }
}
