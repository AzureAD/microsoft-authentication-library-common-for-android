package com.microsoft.identity.common.internal.providers.li;

import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsAuthorizationResult;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationErrorResponse;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationResponse;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationResult;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationStatus;

public class LiAuthorizationResult extends AuthorizationResult<AuthorizationResponse, AuthorizationErrorResponse> {


    /**
     * Constructor of {@link MicrosoftStsAuthorizationResult}.
     *
     * @param authStatus   {@link AuthorizationStatus}
     * @param authResponse {@link AuthorizationResponse}
     */
    public LiAuthorizationResult(final AuthorizationStatus authStatus, final AuthorizationResponse authResponse) {
        setAuthorizationResponse(authResponse);
        setAuthorizationStatus(authStatus);
    }

    /**
     * Constructor of {@link MicrosoftStsAuthorizationResult}.
     *
     * @param authStatus    {@link AuthorizationStatus}
     * @param errorResponse {@link AuthorizationErrorResponse}
     */
    public LiAuthorizationResult(final AuthorizationStatus authStatus, final AuthorizationErrorResponse errorResponse) {
        setAuthorizationErrorResponse(errorResponse);
        setAuthorizationStatus(authStatus);
    }

}
