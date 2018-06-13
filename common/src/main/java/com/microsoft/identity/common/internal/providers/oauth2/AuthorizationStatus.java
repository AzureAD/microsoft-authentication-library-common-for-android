package com.microsoft.identity.common.internal.providers.oauth2;

/**
 * Enum for representing different authorization status values.
 */
public enum AuthorizationStatus {
    /**
     * Code is successfully returned.
     */
    SUCCESS,

    /**
     * User press device back button.
     */
    USER_CANCEL,

    /**
     * Returned URI contains error.
     */
    FAIL,

    /**
     * AuthenticationActivity detects the invalid request.
     */
    INVALID_REQUEST
    //TODO:  Investigate how chrome tab returns http timeout error
}
