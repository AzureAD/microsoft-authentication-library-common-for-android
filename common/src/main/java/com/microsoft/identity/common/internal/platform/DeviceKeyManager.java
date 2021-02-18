package com.microsoft.identity.common.internal.platform;

import com.microsoft.identity.common.exception.ClientException;
import com.microsoft.identity.common.internal.logging.Logger;
import com.microsoft.identity.common.internal.util.Supplier;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableEntryException;
import java.util.Arrays;
import java.util.Date;

import lombok.Builder;
import lombok.Getter;
import lombok.experimental.Accessors;

/**
 * A manager class for providing access to a particular entry in a KeyStore.
 * @param <K> the type of KeyStore.Entry being managed.
 */
@Builder
@Accessors(prefix = "m")
public class DeviceKeyManager<K extends KeyStore.Entry> implements IKeyManager<K> {

    private static final String TAG = DeviceKeyManager.class.getSimpleName();
    private final KeyStore mKeyStore;
    @Getter
    private final String mKeyAlias;
    private final Supplier<byte[]> mThumbprintSupplier;

    @Override
    public boolean exists() {
        boolean exists = false;

        try {
            exists = mKeyStore.containsAlias(mKeyAlias);
        } catch (final KeyStoreException e) {
            Logger.error(
                    TAG,
                    "Error while querying KeyStore",
                    e
            );
        }

        return exists;

    }

    @Override
    public boolean hasThumbprint(byte[] thumbprint) {
        return Arrays.equals(thumbprint, mThumbprintSupplier.get());
    }

    @Override
    public Date getCreationDate() throws ClientException {
        try {
            return mKeyStore.getCreationDate(mKeyAlias);
        } catch (KeyStoreException e) {
            Logger.error(
                    TAG,
                    "Error while getting creation date for alias " + mKeyAlias,
                    e
            );
            throw new ClientException(ClientException.KEYSTORE_NOT_INITIALIZED, e.getMessage(), e);
        }
    }

    @Override
    public boolean clear() {
        boolean deleted = false;

        try {
            mKeyStore.deleteEntry(mKeyAlias);
            deleted = true;
        } catch (final KeyStoreException e) {
            Logger.error(
                    TAG,
                    "Error while clearing KeyStore",
                    e
            );
        }

        return deleted;
    }

    public K getEntry() throws UnrecoverableEntryException, NoSuchAlgorithmException, KeyStoreException {
        return (K) mKeyStore.getEntry(mKeyAlias, null);
    }
}
