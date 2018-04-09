package com.microsoft.identity.common.internal.providers.oauth2;

/**
 * A class representing a client assertion used by the authorization server to authenticate
 * the client application
 * Adding support for client authentication for internal use with test execution
 * https://tools.ietf.org/html/rfc7521#section-6.1
 */
public abstract class ClientAssertion {

    public final static String DEFAULT_CLIENT_ASSERTION_TYPE = "urn:ietf:params:oauth:client-assertion-type:jwt-bearer";

    protected String mClientAssertion;
    protected String mClientAssertionType = DEFAULT_CLIENT_ASSERTION_TYPE;


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
