package com.microsoft.identity.common.internal.encryption;

import static com.microsoft.identity.common.internal.encryption.BaseEncryptionManager.VERSION_ANDROID_KEY_STORE;
import static com.microsoft.identity.common.internal.encryption.BaseEncryptionManager.VERSION_USER_DEFINED;

/**
 * Type of Secret key to be used.
 */
public enum KeyType {
    LEGACY_AUTHENTICATOR_APP_KEY,
    LEGACY_COMPANY_PORTAL_KEY,
    ADAL_USER_DEFINED_KEY,
    KEYSTORE_ENCRYPTED_KEY;

    String getBlobVersion(){
        switch (this) {
            case ADAL_USER_DEFINED_KEY:
            case LEGACY_COMPANY_PORTAL_KEY:
            case LEGACY_AUTHENTICATOR_APP_KEY:
                return VERSION_USER_DEFINED;

            case KEYSTORE_ENCRYPTED_KEY:
                return VERSION_ANDROID_KEY_STORE;

            default:
                throw new IllegalArgumentException("Unexpected KeyType");
        }
    }
}