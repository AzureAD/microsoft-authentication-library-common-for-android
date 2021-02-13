package com.microsoft.identity.common.internal.platform;

import com.microsoft.identity.common.exception.ClientException;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableEntryException;
import java.util.Date;


public interface IKeyManager<K extends KeyStore.Entry> {
    boolean exists();
    boolean hasThumbprint(byte[] thumbprint);
    String getKeyAlias();
    Date getCreationDate() throws ClientException;
    boolean clear();
    K getEntry() throws UnrecoverableEntryException, NoSuchAlgorithmException, KeyStoreException;
}
