package com.microsoft.identity.common.internal.providers.keys;


/**
 * Configuration information for the client certificate to be used
 */
public class ClientCertificateMetadata {
    private String mAlias;
    private char[] mPassword;

    public ClientCertificateMetadata(String alias, char[] password) {
        this.mAlias = alias;
        this.mPassword = password;
    }

    public String getAlias() {
        return mAlias;
    }

    public char[] getPassword() {
        return mPassword;
    }

}
