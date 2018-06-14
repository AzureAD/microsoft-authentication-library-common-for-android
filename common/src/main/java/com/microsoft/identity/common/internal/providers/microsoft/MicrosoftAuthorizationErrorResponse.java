package com.microsoft.identity.common.internal.providers.microsoft;

import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationErrorResponse;

/**
 * Sub class of {@link AuthorizationErrorResponse}.
 * Encapsulates Microsoft specific Authorization Result errors in addition to standard OAuth2 errors.
 */
public class MicrosoftAuthorizationErrorResponse extends AuthorizationErrorResponse {
    /* Constants */
    public static final String AUTHORIZATION_FAILED = "authorization_failed";

    public static final String USER_CANCEL = "user_cancelled";

    public static final String NULL_INTENT = "Received null intent";

    public static final String AUTHORIZATION_SERVER_INVALID_RESPONSE = "The authorization server returned an invalid response.";

    public static final String USER_CANCELLED_FLOW = "User pressed device back button to cancel the flow.";

    public static final String STATE_NOT_THE_SAME = "Returned state from authorize endpoint is not the same as the one sent";

    public static final String STATE_NOT_RETURNED = "State is not returned";

    public static final String UNKNOWN_ERROR = "Unknown error";

    public static final String UNKNOWN_RESULT_CODE = "Unknown result code returned ";

    public static final String BROKER_NEEDS_TO_BE_INSTALLED = "Device needs to have broker installed";

    /**
     * Constructor of {@link MicrosoftAuthorizationErrorResponse}.
     * @param error  error string returned from the Authorization Server.
     * @param errorDescription  description of the error.
     */
    public MicrosoftAuthorizationErrorResponse(String error, String errorDescription) {
        super(error, errorDescription);
    }

}
