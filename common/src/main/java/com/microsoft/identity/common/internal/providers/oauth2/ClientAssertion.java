package com.microsoft.identity.common.internal.providers.oauth2;

/**
 * A class representing a client assertion used by the authorization server to authenticate
 * the client application
 */
public abstract class ClientAssertion {

    protected String mClientAssertion;
    protected String mClientAssertionType;


    public String getClientAssertion() {
        return mClientAssertion;
    }

    public void setClientAssertion(String clientAssertion) {
        this.mClientAssertion = clientAssertion;
    }

    public String getClientAssertionType() {
        return mClientAssertionType;
    }

    public void setClientAssertionType(String clientAssertionType) {
        this.mClientAssertionType = clientAssertionType;
    }
}
