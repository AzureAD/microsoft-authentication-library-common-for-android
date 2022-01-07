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

import java.nio.ByteBuffer;
import java.security.KeyStore;
import java.security.spec.AlgorithmParameterSpec;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;

import lombok.NonNull;

/**
 * Having generalized Cipher to CryptoSuite, enable us to distinguish symmetric
 * ciphers of interest.
 */
public enum XplatSymmetricCipher implements CryptoSuite {

    AES_GCM_NONE_HMACSHA256(SymmetricAlgorithm.Builder.of("AES/GCM/NoPadding"), "HmacSHA256", 256) {
        /**
         * This instance of cryptospec takes two arguments:
         * <ol>
         *     <li> an integer, the authentication tag length <string>in bits</strong></li>
         *     <li> a byte array representing the initialization vector to use</li>
         * </ol>
         * @param args
         * @return
         */
        @Override
        public AlgorithmParameterSpec cryptoSpec(Object... args) {
            if (args.length != 2 || !(args[0] instanceof Integer)) {
                // TODO: log.
                return null;
            }
            byte[] iv = (byte[]) args[1];
            return new GCMParameterSpec((Integer) args[0], iv);
        }

        /**
         * This initialize method had three different argument formulations:
         * <ul>
         * <li>a single argument, a byte array of the additional auth data</li>
         * <li>a single argument, a byte buffer of the additional auth data</li>
         * <li>three arguments, a byte array, start potition and length indicating the addtional
         * auth data</li>
         * </ul>
         * @param cipher
         * @param args
         */
        @Override
        public void initialize(Cipher cipher, Object... args) {
                if (args.length == 1 && args[0] instanceof byte[]) {
                    cipher.updateAAD((byte[]) args[0]);
                } else if (args.length == 1 && args[0] instanceof ByteBuffer) {
                    cipher.updateAAD((ByteBuffer) args[0]);
                } else if (args.length == 3 && args[0] instanceof byte[] && args[1] instanceof Integer && args[2] instanceof Integer) {
                    cipher.updateAAD((byte[]) args[0], (Integer) args[1], (Integer) args[2]);
                }
        }
    },
    AES_CBC_NONE_HMACSHA256(SymmetricAlgorithm.Builder.of("AES/CBC/NoPadding"), "HmacSHA256", 256) {
        @Override
        public AlgorithmParameterSpec cryptoSpec(Object... args) {
            if (args.length != 1 || !(args[0] instanceof byte[])) {
                // TODO: log.
                return null;
            }
            byte[] iv = (byte[]) args[0];
            return new IvParameterSpec(iv);
        }

        @Override
        public void initialize(Cipher cipher, Object... args) {  }
    };

    SymmetricAlgorithm mValue;
    String mMacString;
    int mKeySize;

    XplatSymmetricCipher(@NonNull final SymmetricAlgorithm value, @NonNull String macValue, int keySize) {
        mValue = value;
        mMacString = macValue;
        mKeySize = keySize;
    }

    @Override
    public Algorithm cipher() {
        return mValue;
    }

    @Override
    public String macName() {
        return mMacString;
    }

    @Override
    public boolean isAsymmetric() {
        return false;
    }

    @Override
    public Class<? extends KeyStore.Entry> keyClass() {
        return KeyStore.SecretKeyEntry.class;
    }

    @Override
    public int keySize() {
        return mKeySize;
    }

    @Override
    public SigningAlgorithm signingAlgorithm() {
        return null;
    }
}
