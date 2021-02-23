package com.microsoft.identity.common.internal.platform;

import com.microsoft.identity.common.exception.ClientException;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.UnrecoverableEntryException;

public interface AsymmetricKeyAccessor extends KeyAccessor {
    String getPublicKey(IDevicePopManager.PublicKeyFormat format) throws ClientException;

    public PublicKey getPublicKey() throws UnrecoverableEntryException, NoSuchAlgorithmException, KeyStoreException;
}
