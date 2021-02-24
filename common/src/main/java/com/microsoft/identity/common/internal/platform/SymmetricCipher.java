package com.microsoft.identity.common.internal.platform;

import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.security.KeyStore;

/**
 * Having generalized Cipher to CryptoSuite, enable us to distinguish symmetric
 * ciphers of interest.
 */
public enum SymmetricCipher implements CryptoSuite {

    @RequiresApi(Build.VERSION_CODES.M)
    AES_GCM_NONE_HMACSHA256("AES/GCM/NoPadding", "HmacSHA256", 256);

    String mValue;
    String mMacString;
    int mKeySize;

    SymmetricCipher(@NonNull final String value, @NonNull String macValue, int keySize) {
        mValue = value;
        mMacString = macValue;
        mKeySize = keySize;
    }

    @Override
    public java.lang.String cipherName() {
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
}
