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

import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.AZURE_AUTHENTICATOR_APP_PACKAGE_NAME;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.COMPANY_PORTAL_APP_PACKAGE_NAME;

/**
 * Encryption manager for broker flows.
 * */
public class BrokerEncryptionManager extends EncryptionManagerBase {
    private static final String TAG = BrokerEncryptionManager.class.getName();

    /**
     * A flag to turn on/off keystore encryption on Broker apps.
     */
    public static final boolean sShouldEncryptWithKeyStoreKey = false;

    /**
     * Singleton object.
     * */
    private static BrokerEncryptionManager sInstance;

    public static synchronized BrokerEncryptionManager getInstance(@NonNull Context context) {
        if (sInstance == null) {
            sInstance = new BrokerEncryptionManager(context);
        }

        return sInstance;
    }

    protected BrokerEncryptionManager(@NonNull Context context) {
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

        // The current app runtime is the broker; load its secret key.
        if (!sShouldEncryptWithKeyStoreKey &&
                AuthenticationSettings.INSTANCE.getBrokerSecretKeys().containsKey(getPackageName())) {

            // Try to read keystore key - so that we get telemetry data on its reliability.
            // If anything happens, do not crash the app.
            try {
                loadSecretKey(KeyType.KEYSTORE_ENCRYPTED_KEY);
            } catch (Exception e) {
                // Best effort.
            }

            if (AZURE_AUTHENTICATOR_APP_PACKAGE_NAME.equalsIgnoreCase(getPackageName())) {
                return new Pair<>(loadSecretKey(KeyType.LEGACY_AUTHENTICATOR_APP_KEY), VERSION_USER_DEFINED);
            } else {
                return new Pair<>(loadSecretKey(KeyType.LEGACY_COMPANY_PORTAL_KEY), VERSION_USER_DEFINED);
            }
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
            case LEGACY_AUTHENTICATOR_APP_KEY:
                return getSecretKey(AuthenticationSettings.INSTANCE.getBrokerSecretKeys().get(AZURE_AUTHENTICATOR_APP_PACKAGE_NAME));

            case LEGACY_COMPANY_PORTAL_KEY:
                return getSecretKey(AuthenticationSettings.INSTANCE.getBrokerSecretKeys().get(COMPANY_PORTAL_APP_PACKAGE_NAME));

            case KEYSTORE_ENCRYPTED_KEY:
                return loadKeyStoreEncryptedKey();
        }

        Logger.verbose(TAG + methodName, "Unknown KeyType. This code should never be reached.");
        throw new GeneralSecurityException(ErrorStrings.UNKNOWN_ERROR);
    }
}
