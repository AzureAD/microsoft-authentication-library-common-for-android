// Copyright (c) Microsoft Corporation.
// All rights reserved.

package com.microsoft.identity.common.java.jwt;

import com.microsoft.identity.common.java.exception.ClientException;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWEObject;
import com.nimbusds.jose.crypto.ECDHDecrypter;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;

import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.text.ParseException;

import lombok.NonNull;

/**
 * Decrypts ECDH-ES/A256GCM JWE token responses using the caller-generated ephemeral EC key.
 */
public class EcdhEsJweResponseDecryptor implements IJweResponseDecryptor {

    @NonNull
    private final ECPrivateKey mPrivateKey;

    @NonNull
    private final ECPublicKey mPublicKey;

    public EcdhEsJweResponseDecryptor(@NonNull final ECPrivateKey privateKey,
                                      @NonNull final ECPublicKey publicKey) {
        mPrivateKey = privateKey;
        mPublicKey = publicKey;
    }

    @Override
    public String decryptJwe(@NonNull final String jwe) throws ClientException {
        try {
            final JWEObject jweObject = JWEObject.parse(jwe);
            final ECKey ecJwk = new ECKey.Builder(Curve.P_256, mPublicKey)
                    .privateKey(mPrivateKey)
                    .build();
            jweObject.decrypt(new ECDHDecrypter(ecJwk));
            return jweObject.getPayload().toString();
        } catch (final ParseException | JOSEException e) {
            throw new ClientException(ClientException.DECRYPTION_FAILURE, "Failed to decrypt ECDH-ES JWE response.", e);
        }
    }
}
