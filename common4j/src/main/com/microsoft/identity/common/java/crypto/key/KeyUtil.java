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

import static com.microsoft.identity.common.java.AuthenticationConstants.ENCODING_UTF8;

import com.microsoft.identity.common.java.logging.Logger;
import com.microsoft.identity.common.java.util.Base64;
import com.microsoft.identity.common.java.util.StringUtil;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import lombok.NonNull;

/**
 * Utility class for Key operations.
 */
public class KeyUtil {

    private static final String TAG = KeyUtil.class.getSimpleName();

    /**
     * A string to return when the this class fails to derive thumbprint.
     */
    public static final String UNKNOWN_THUMBPRINT = "UNKNOWN_THUMBPRINT";

    /**
     * HMac key hashing algorithm.
     */
    public static final String HMAC_KEY_HASH_ALGORITHM = "SHA-256";

    /**
     * HMac algorithm (for {@link Mac#getInstance(String)})
     */
    public static final String HMAC_ALGORITHM = "HmacSHA256";

    /**
     * HMac Key spec algorithm.
     */
    private static final String HMAC_KEYSPEC_ALGORITHM = "AES";

    /**
     * Derive a thumbprint from the given {@link AbstractSecretKeyLoader}.
     *
     * @param keyLoader ISecretKeyLoader to obtain the key (calculate the thumbprint from).
     * @return a thumbprint. Will return {@link KeyUtil#UNKNOWN_THUMBPRINT} if it fails to derived one.
     */
    public static String getKeyThumbPrint(final @NonNull AbstractSecretKeyLoader keyLoader) {
        final String methodName = ":getKeyThumbPrint";
        try {
            return getKeyThumbPrint(keyLoader.getKey());
        } catch (final Throwable e) {
            Logger.warn(TAG + methodName, "failed to load key:" + e.getMessage());
            return UNKNOWN_THUMBPRINT;
        }
    }

    /**
     * Derive a thumbprint from the given key.
     *
     * @param key SecretKey to calculate the thumbprint from.
     *
     * @return a thumbprint. Will return {@link KeyUtil#UNKNOWN_THUMBPRINT} if it fails to derived one.
     */
    public static String getKeyThumbPrint(@NonNull final SecretKey key) {
        final String methodName = ":getKeyThumbPrint";
        try {
            return getKeyThumbPrintFromHmacKey(getHMacKey(key));
        } catch (Throwable e) {
            Logger.warn(TAG + methodName, "failed to calculate thumbprint:" + e.getMessage());
            return UNKNOWN_THUMBPRINT;
        }
    }

    /**
     * Derive a thumbprint from the given hmac key.
     *
     * @param hmacKey hmacKey of the secretKey.
     *
     * @return a thumbprint. Will return {@link KeyUtil#UNKNOWN_THUMBPRINT} if it fails to derived one.
     */
    public static String getKeyThumbPrintFromHmacKey(@NonNull final SecretKey hmacKey) {
        final String methodName = ":getKeyThumbPrintFromHmacKey";
        try {
            final byte[] thumbprintBytes = "012345678910111213141516".getBytes(ENCODING_UTF8);

            final Mac thumbprintMac = Mac.getInstance(HMAC_ALGORITHM);
            thumbprintMac.init(hmacKey);
            byte[] thumbPrintFinal = thumbprintMac.doFinal(thumbprintBytes);
            return Base64.encodeUrlSafeString(thumbPrintFinal);
        } catch (final Throwable e) {
            Logger.warn(TAG + methodName, "failed to calculate thumbprint:" + e.getMessage());
            return UNKNOWN_THUMBPRINT;
        }
    }

    /**
     * Derive a HMAC key from given key.
     *
     * @param key SecretKey from which HMAC key has to be derived
     * @return SecretKey
     * @throws NoSuchAlgorithmException if HMAC_KEY_HASH_ALGORITHM is not supported.
     */
    public static SecretKey getHMacKey(@NonNull final SecretKey key) throws NoSuchAlgorithmException {
        // Some keys may not produce byte[] with getEncoded
        final byte[] encodedKey = key.getEncoded();
        if (encodedKey != null) {
            final MessageDigest digester = MessageDigest.getInstance(HMAC_KEY_HASH_ALGORITHM);
            return new SecretKeySpec(digester.digest(encodedKey), HMAC_KEYSPEC_ALGORITHM);
        }

        return key;
    }
}
