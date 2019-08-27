package com.microsoft.identity.common.adal.internal.cache;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;

import com.microsoft.identity.common.adal.internal.AuthenticationSettings;
import com.microsoft.identity.common.exception.ErrorStrings;
import com.microsoft.identity.common.internal.logging.Logger;

import java.io.IOException;
import java.security.GeneralSecurityException;

import javax.crypto.SecretKey;

/**
 * EncryptionManager class for non-broker (ADAL and MSAL) flows.
 * */
public class MsalEncryptionManager extends EncryptionManagerBase {
    private static final String TAG = MsalEncryptionManager.class.getName();

    /**
     * Singleton object.
     * */
    private static MsalEncryptionManager sInstance;

    public static synchronized MsalEncryptionManager getInstance(@NonNull Context context) {
        if (sInstance == null) {
            sInstance = new MsalEncryptionManager(context);
        }

        return sInstance;
    }

    protected MsalEncryptionManager(@NonNull Context context) {
        super(context);
    }

    @Override
    public synchronized Pair<SecretKey, String> loadSecretKeyForEncryption() throws IOException,
            GeneralSecurityException {
        final String methodName = ":loadSecretKeyForEncryption";

        // Loading key only once for performance. If API is upgraded, it will
        // restart the device anyway. It will load the correct key for new API.
        if (isEncryptionKeyLoaded()) {
            return getCachedEncryptionKey();
        }

        // Try to get user defined key (ADAL/MSAL).
        if (AuthenticationSettings.INSTANCE.getSecretKeyData() != null) {
            return new Pair<>(loadSecretKey(IEncryptionManager.KeyType.ADAL_USER_DEFINED_KEY), VERSION_USER_DEFINED);
        }

        // Try loading existing keystore-encrypted key. If it doesn't exist, create a new one.
        try {
            SecretKey key = loadSecretKey(KeyType.KEYSTORE_ENCRYPTED_KEY);
            if (key != null) {
                return new Pair<>(key, VERSION_ANDROID_KEY_STORE);
            }
        } catch (final IOException | GeneralSecurityException e) {
            // If we fail to load key, proceed and generate a new one.
        }

        Logger.verbose(TAG + methodName, "Keystore-encrypted key does not exist, try to generate new keys.");
        return new Pair<>(generateKeyStoreEncryptedKey(), VERSION_ANDROID_KEY_STORE);
    }

    /**
     * Given the key type, load a secret key.
     *
     * @return SecretKey. Null if there isn't any.
     */
    @Nullable
    public SecretKey loadSecretKey(@NonNull final KeyType keyType) throws IOException, GeneralSecurityException {
        final String methodName = ":loadSecretKey";

        switch (keyType) {
            case ADAL_USER_DEFINED_KEY:
                return getSecretKey(AuthenticationSettings.INSTANCE.getSecretKeyData());

            case KEYSTORE_ENCRYPTED_KEY:
                return loadKeyStoreEncryptedKey();
        }

        Logger.verbose(TAG + methodName, "Unknown KeyType. This code should never be reached.");
        throw new GeneralSecurityException(ErrorStrings.UNKNOWN_ERROR);
    }
}
