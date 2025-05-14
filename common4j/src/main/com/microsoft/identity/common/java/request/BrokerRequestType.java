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
    RESOLVE_INTERRUPT,

    /**
     * Request type indicates a token request which is performed during an interrupt flow specifically to get tokens for DRS service.
     * NOTE : Only use this type when the request is for DRS service as this should not be used while serving any client side requests.
     */
    BROKER_DRS_REQUEST

}
