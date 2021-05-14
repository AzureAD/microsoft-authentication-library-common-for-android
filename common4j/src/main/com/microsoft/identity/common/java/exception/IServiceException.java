package com.microsoft.identity.common.java.exception;

import java.util.HashMap;
import java.util.List;

/**
 * Interface for Service exception of Broker/MSAL.
 */
public interface IServiceException extends IBaseException {

    /**
     * When {@link java.net.SocketTimeoutException} is thrown, no status code will be caught.
     * Will use 0 instead.
     */
    int DEFAULT_STATUS_CODE = 0;

    /**
     * Returns an OAuth Suberror code associated to this exception.
     * */
    String getOAuthSubErrorCode();

    /**
     * Returns an HTTP status code of the response that triggers this exception.
     * */
    int getHttpStatusCode();

    /**
     * Returns body of the Http Response that triggers this exception.
     * */
    HashMap<String, String> getHttpResponseBody();

    /**
     * Returns Header of the Http Response that triggers this exception.
     * */
    HashMap<String, List<String>> getHttpResponseHeaders();
}
