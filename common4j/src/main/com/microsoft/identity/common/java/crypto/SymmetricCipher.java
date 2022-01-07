package com.microsoft.identity.common.java.crypto;

public interface SymmetricCipher extends CryptoSuite {
    SymmetricCipher AES_GCM_NONE_HMACSHA256 =
            GenericSymmetricCipher.builder().value(SymmetricAlgorithm.Builder.of("AES/GCM/NoPadding"))
        .macString("HmacSHA256").keySize(256).name("AES_GCM_NONE_HMACSHA256").build();

    String name();
}
