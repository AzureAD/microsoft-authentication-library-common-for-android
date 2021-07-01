package com.microsoft.identity.common.java.crypto;

import lombok.NonNull;

/**
 * Signing algorithms supported by our underlying keystore. Not all algs available at all device
 * levels.
 */
public enum SigningAlgorithm {
    //@RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    MD5_WITH_RSA("MD5withRSA"),

    //@RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    NONE_WITH_RSA("NONEwithRSA"),

    SHA_1_WITH_RSA("SHA1withRSA"),

    //@RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    SHA_256_WITH_RSA("SHA256withRSA"),

    //@RequiresApi(Build.VERSION_CODES.M)
    SHA_256_WITH_RSA_PSS("SHA256withRSA/PSS"),

    //@RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    SHA_384_WITH_RSA("SHA384withRSA"),

    //@RequiresApi(Build.VERSION_CODES.M)
    SHA_384_WITH_RSA_PSS("SHA384withRSA/PSS"),

    //@RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    SHA_512_WITH_RSA("SHA512withRSA"),

    //@RequiresApi(Build.VERSION_CODES.M)
    SHA_512_WITH_RSA_PSS("SHA512withRSA/PSS");

    private final String mValue;

    SigningAlgorithm(@NonNull final String value) {
        mValue = value;
    }

    @Override
    @NonNull
    public String toString() {
        return mValue;
    }
}