package com.microsoft.identity.common.internal.platform;

import com.microsoft.identity.common.internal.platform.CryptoSuite;

import java.security.KeyStore;

/**
 * Definitions for Asymmetric Crypto suites.
 */
public enum AsymmetricCipher implements CryptoSuite {
    RSA_ECB_PKCS1_PADDING_HMACSHA256 {
        @Override
        public String cipherName() {
            return "RSA/ECB/PKCS1Padding";
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
            return null;
        }

        @Override
        public int keySize() {
            return 2048;
        }
    };
}
