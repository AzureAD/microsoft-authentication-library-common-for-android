package com.microsoft.identity.common.internal.servertelemetry;

import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationResult;
import com.microsoft.identity.common.internal.providers.oauth2.TokenResult;
import com.microsoft.identity.common.internal.result.AcquireTokenResult;

public class TelemetryUtils {

    static String errorFromAcquireTokenResult(final AcquireTokenResult acquireTokenResult) {
        final String errorFromAuthorization = getErrorFromAuthorizationResult(acquireTokenResult.getAuthorizationResult());
        if (errorFromAuthorization != null) {
            return errorFromAuthorization;
        } else {
            return getErrorFromTokenResult(acquireTokenResult.getTokenResult());
        }
    }

    private static String getErrorFromAuthorizationResult(final AuthorizationResult authorizationResult) {
        if (authorizationResult != null && authorizationResult.getErrorResponse() != null) {
            return authorizationResult.getErrorResponse().getError();
        } else {
            return null;
        }
    }

    private static String getErrorFromTokenResult(final TokenResult tokenResult) {
        if (tokenResult != null && tokenResult.getErrorResponse() != null) {
            return tokenResult.getErrorResponse().getError();
        } else {
            return null;
        }
    }

}
