package com.microsoft.identity.common.internal.broker;

/**
 * Encapsulates the broker error result
 */
public class BrokerErrorResult {
    private String mErrorCode;
    private String mErrorMessage;
    private String mOAuthError;
    private String mOAuthSubError;
    private String mOAuthErrorMetadata;
    private String mOAuthErrorDescription;
    private String mHttpResponseBody;
    private String mHttpResponseHeaders;

}
