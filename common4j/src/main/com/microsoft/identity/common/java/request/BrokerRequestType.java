package com.microsoft.identity.common.java.request;

public enum BrokerRequestType {

    /**
     * Request type indicates regular acquire token request from adal or msal, Default value.
     */
    REGULAR,

    /**
     * Request type indicates a token request to get Broker Refresh Token while doing WPJ.
     */
    BROKER_RT_REQUEST,

    /**
     * Request type indicates a token request which is performed during an interrupt flow.
     */
    RESOLVE_INTERRUPT

}
