package com.microsoft.identity.common.internal.providers.keys;


/**
 * Configuration information for the client certificate to be used
 */
public class ClientCertificateConfiguration {
    private String mAlias;
    private String mPassword;

    public String getAlias(){
        return mAlias;
    }

    public void setAlias(String alias){
        this.mAlias = alias;
    }

    public String getPassword(){
        return mPassword;
    }

    public void setPassword(String password){
        this.mPassword = password;
    }
}
