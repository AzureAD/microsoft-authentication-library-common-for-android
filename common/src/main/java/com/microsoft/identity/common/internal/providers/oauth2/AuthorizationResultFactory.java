package com.microsoft.identity.common.internal.providers.oauth2;

import android.content.Intent;

/**
 * Abstract Factory class to construct {@link AuthorizationResult}
 */

public abstract class AuthorizationResultFactory {

    /* Authorization Response Constants */
    protected static final String CODE = "code";
    protected static final String STATE = "state";
    protected static final String ERROR = "error";
    protected static final String ERROR_CODE = "error_code";
    protected static final String ERROR_DESCRIPTION = "error_description";

    /**
     * Factory method to construct {@link AuthorizationResult}
     *
     * @param resultCode Result code from the calling Activity
     * @param data       Intent data from the calling Activity
     * @return {@link AuthorizationResult}
     */
    public abstract AuthorizationResult createAuthorizationResult(final int resultCode, final Intent data);


}
