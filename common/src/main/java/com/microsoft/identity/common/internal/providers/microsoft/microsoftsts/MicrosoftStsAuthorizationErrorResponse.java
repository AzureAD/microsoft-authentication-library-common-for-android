package com.microsoft.identity.common.internal.providers.microsoft.microsoftsts;

import com.microsoft.identity.common.internal.providers.microsoft.MicrosoftAuthorizationErrorResponse;

public class MicrosoftStsAuthorizationErrorResponse extends MicrosoftAuthorizationErrorResponse {

    public MicrosoftStsAuthorizationErrorResponse(final String error, final String errorDescription) {
        super(error, errorDescription);
    }

}
