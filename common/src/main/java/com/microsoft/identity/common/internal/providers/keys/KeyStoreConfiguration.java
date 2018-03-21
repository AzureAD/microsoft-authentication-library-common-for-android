package com.microsoft.identity.common.internal.providers.keys;

/**
 * Class holds information necessary to instantiate a keystore in order to retrieve and access
 * a ClientCertificateConfiguration and the private key associated with that ClientCertificateConfiguration
 * NOTE: This class should move to library configuration
 */
public class KeyStoreConfiguration {

    private final String mKeyStoreType;
    private final String mKeyStoreProvider;
    private final char[] mKeyStorePassword;

    public KeyStoreConfiguration(String keyStoreType, String keyStoreProvider, char[] keyStorePassword){
        this.mKeyStoreType = keyStoreType;
        this.mKeyStoreProvider = keyStoreProvider;
        this.mKeyStorePassword = keyStorePassword;
    }

    public String getKeyStoreType() {
        return mKeyStoreType;
    }

    public String getKeyStoreProvider() {
        return mKeyStoreProvider;
    }

    public char[] getKeyStorePassword() {
        return mKeyStorePassword;
    }


}
