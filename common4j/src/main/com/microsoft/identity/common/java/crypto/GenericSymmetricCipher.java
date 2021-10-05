package com.microsoft.identity.common.java.crypto;

import com.microsoft.identity.common.java.crypto.SigningAlgorithm;
import com.microsoft.identity.common.java.crypto.SymmetricAlgorithm;
import com.microsoft.identity.common.java.crypto.SymmetricCipher;

import java.security.KeyStore;

import lombok.Builder;
import lombok.NonNull;
import lombok.experimental.Accessors;

@Builder
@Accessors(prefix = "m")
public class GenericSymmetricCipher implements SymmetricCipher {
    @NonNull
    private final SymmetricAlgorithm mValue;
    @NonNull
    private final String mMacString;
    @NonNull
    private final int mKeySize;
    @NonNull
    private final String mName;
    public SymmetricAlgorithm cipher() {
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

    @Override
    public String name() {
        return mName;
    }
}
