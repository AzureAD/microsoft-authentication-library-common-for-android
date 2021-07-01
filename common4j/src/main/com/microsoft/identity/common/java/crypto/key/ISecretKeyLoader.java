package com.microsoft.identity.common.java.crypto.key;

import com.microsoft.identity.common.java.exception.ClientException;

import javax.crypto.SecretKey;

import lombok.NonNull;

public interface ISecretKeyLoader {

    @NonNull
    String getAlias();

    @NonNull
    SecretKey getKey() throws ClientException;

    @NonNull
    String getKeySpecAlgorithm();

    @NonNull
    String getKeyIdentifier();

    @NonNull
    String getCipherAlgorithm();
}
