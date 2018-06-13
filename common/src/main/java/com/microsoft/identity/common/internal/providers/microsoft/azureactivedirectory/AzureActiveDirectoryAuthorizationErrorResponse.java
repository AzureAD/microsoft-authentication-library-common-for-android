package com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectory;

import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsAuthorizationErrorResponse;

public class AzureActiveDirectoryAuthorizationErrorResponse extends MicrosoftStsAuthorizationErrorResponse {

    private String mErrorCodes;

    public AzureActiveDirectoryAuthorizationErrorResponse(final String error, final String errorDescription) {
        super(error, errorDescription);
    }

    public String getErrorCodes() {
        return mErrorCodes;
    }

    public void setErrorCodes(final String mErrorCodes) {
        this.mErrorCodes = mErrorCodes;
    }

}
