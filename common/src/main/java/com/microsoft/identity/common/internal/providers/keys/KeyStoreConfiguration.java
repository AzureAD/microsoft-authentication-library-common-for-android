package com.microsoft.identity.common.internal.providers.keys;

/**
 * Class holds information necessary to instantiate a keystore in order to retrieve and access
 * a ClientCertificateConfiguration and the private key associated with that ClientCertificateConfiguration
 * NOTE: This class should move to library configuration
 */
public class KeyStoreConfiguration {

    private String mKeyStoreType;
    private String mKeyStoreProvider;
    private String mKeyStorePassword;


    public String getKeyStoreType() {
        return mKeyStoreType;
    }

    public void setKeyStoreType(String keyStoreType) {
        this.mKeyStoreType = keyStoreType;
    }

    public String getKeyStoreProvider() {
        return mKeyStoreProvider;
    }

    public void setKeyStoreProvider(String keyStoreProvider) {
        this.mKeyStoreProvider = keyStoreProvider;
    }

    public String getKeyStorePassword() {
        return mKeyStorePassword;
    }

    public void setKeyStorePassword(String keyStorePassword) {
        this.mKeyStorePassword = keyStorePassword;
    }
}
