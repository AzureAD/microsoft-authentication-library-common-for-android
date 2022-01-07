package com.microsoft.identity.common.java.crypto;

import java.security.KeyStore;
import java.security.spec.AlgorithmParameterSpec;

import javax.crypto.Cipher;

public interface AsymmetricCipher extends CryptoSuite {
    public static final AsymmetricCipher RSA_NONE_OAEPWithSHA_1AndMGF1PaddingAndHmacSha256 = new AsymmetricCipher() {
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

        @Override
        public SigningAlgorithm signingAlgorithm() {
            return SigningAlgorithm.SHA_256_WITH_RSA;
        }

        @Override
        public AlgorithmParameterSpec cryptoSpec(Object... args) {
            return null;
        }

        @Override
        public void initialize(Cipher cipher, Object... args) {

        }
    };
    public static final AsymmetricCipher RSA_ECB_PKCS1_PADDING_HMACSHA256 = new AsymmetricCipher(){
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

        @Override
        public SigningAlgorithm signingAlgorithm() {
            return SigningAlgorithm.SHA_256_WITH_RSA;
        }

        @Override
        public AlgorithmParameterSpec cryptoSpec(Object... args) {
            return null;
        }

        @Override
        public void initialize(Cipher cipher, Object... args) {

        }
    };
}
