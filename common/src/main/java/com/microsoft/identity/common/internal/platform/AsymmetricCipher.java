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

import androidx.annotation.RequiresApi;

import com.microsoft.identity.common.java.crypto.CryptoSuite;
import com.microsoft.identity.common.java.crypto.SigningAlgorithm;

import java.security.KeyStore;

/**
 * Definitions for Asymmetric Crypto suites.
 */
public enum AsymmetricCipher implements CryptoSuite {
    RSA_NONE_OAEPWithSHA_1AndMGF1PaddingAndHmacSha256 {
        @Override
        public AsymmetricAlgorithm cipher() {
            return IDevicePopManager.Cipher.RSA_NONE_OAEPWithSHA_1AndMGF1Padding;
        }

        @Override
        public String macName() {
            return "HmacSHA256";
        }

        @Override
        public boolean isAsymmetric() {
            return true;
        }

        @Override
        public Class<? extends KeyStore.Entry> keyClass() {
            return KeyStore.PrivateKeyEntry.class;
        }

        @Override
        public int keySize() {
            return 2048;
        }

        @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
        @Override
        public SigningAlgorithm signingAlgorithm() {
            return SigningAlgorithm.SHA_256_WITH_RSA;
        }
    },
    RSA_ECB_PKCS1_PADDING_HMACSHA256 {
        @Override
        public AsymmetricAlgorithm cipher() {
            return AsymmetricAlgorithm.Builder.of("RSA/ECB/PKCS1Padding");
        }

        @Override
        public String macName() {
            return "HmacSHA256";
        }

        @Override
        public boolean isAsymmetric() {
            return true;
        }

        @Override
        public Class<? extends KeyStore.Entry> keyClass() {
            return KeyStore.PrivateKeyEntry.class;
        }

        @Override
        public int keySize() {
            return 2048;
        }

        @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
        @Override
        public SigningAlgorithm signingAlgorithm() {
            return SigningAlgorithm.SHA_256_WITH_RSA;
        }
    };
}
