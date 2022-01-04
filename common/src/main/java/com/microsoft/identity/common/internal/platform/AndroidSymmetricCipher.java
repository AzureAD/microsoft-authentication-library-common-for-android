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
package com.microsoft.identity.common.internal.platform;

import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.microsoft.identity.common.java.crypto.Algorithm;
import com.microsoft.identity.common.java.crypto.CryptoSuite;
import com.microsoft.identity.common.java.crypto.SigningAlgorithm;
import com.microsoft.identity.common.java.crypto.SymmetricAlgorithm;

import java.nio.ByteBuffer;
import java.security.KeyStore;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidParameterSpecException;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;

/**
 * Having generalized Cipher to CryptoSuite, enable us to distinguish symmetric
 * ciphers of interest.
 */
public enum AndroidSymmetricCipher implements CryptoSuite {

    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN)
    AES_GCM_NONE_HMACSHA256(SymmetricAlgorithm.Builder.of("AES/GCM/NoPadding"), "HmacSHA256", 256) {
        @Override
        public AlgorithmParameterSpec cryptoSpec(Object... args) {
            if (args.length == 1 && args[0] instanceof byte[]) {
                return new IvParameterSpec((byte[]) args[0]);
            } else if (args.length == 2 && args[0] instanceof Cipher && args[1] instanceof byte[]) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    try {
                        GCMParameterSpec spec = ((Cipher) args[0]).getParameters().getParameterSpec(GCMParameterSpec.class);
                        System.arraycopy(spec.getIV(), 0, (byte[]) args[1], 0, 12);
                        return spec;
                    } catch (InvalidParameterSpecException e) {
                       return null;
                    }
                } else {
                    try {
                        IvParameterSpec spec = ((Cipher) args[0]).getParameters().getParameterSpec(IvParameterSpec.class);
                        System.arraycopy(spec.getIV(), 0, (byte[]) args[1], 0, 12);
                        return spec;
                    } catch (InvalidParameterSpecException e) {
                        return null;
                    }
                }
            }
            if (args.length != 2 && !(args[0] instanceof Integer)) {
                // TODO: log.
                return null;
            }
            byte[] iv;
            if (args.length == 2) {
                iv = (byte[]) args[1];
            } else {
                iv = Arrays.copyOfRange((byte[]) args[1], (Integer) args[2], (Integer) args[3]);
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                return new GCMParameterSpec((Integer) args[0], iv);
            } else {
                return new IvParameterSpec(iv);
            }
        }

        @Override
        public void initialize(Cipher cipher, Object... args) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                if (args == null || (args.length == 1 && args[0] == null)) {
                    return;
                } else if (args.length == 1 && args[0] instanceof byte[]) {
                    cipher.updateAAD((byte[]) args[0]);
                } else if (args.length == 1 && args[0] instanceof ByteBuffer) {
                    cipher.updateAAD((ByteBuffer) args[0]);
                } else if (args.length == 3 && args[0] instanceof byte[] && args[1] instanceof Integer && args[2] instanceof Integer) {
                    cipher.updateAAD((byte[]) args[0], (Integer) args[1], (Integer) args[2]);
                }
            }
        }

        public KeyGenParameterSpec.Builder decorateKeyGenerator(@NonNull final KeyGenParameterSpec.Builder spec) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                return spec.setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                        .setKeySize(keySize());
            } else {
                return spec;
            }
        }
    };

    SymmetricAlgorithm mValue;
    String mMacString;
    int mKeySize;

    AndroidSymmetricCipher(@NonNull final SymmetricAlgorithm value, @NonNull String macValue, int keySize) {
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

    public abstract @NonNull KeyGenParameterSpec.Builder decorateKeyGenerator(@NonNull final KeyGenParameterSpec.Builder spec);
}
