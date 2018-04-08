package com.microsoft.identity.common.internal.providers.oauth2;

import com.google.gson.annotations.SerializedName;

public class TokenErrorResponse {

    @SerializedName("error")
    protected String mError;
    @SerializedName("error_description")
    protected String mErrorDescription;
    @SerializedName("error_uri")
    protected String mErrorUri;


    public String getError() {
        return mError;
    }

    public void setError(String error) {
        this.mError = error;
    }

    public String getErrorDescription() {
        return mErrorDescription;
    }

    public void setErrorDescription(String errorDescription) {
        this.mErrorDescription = errorDescription;
    }

    public String getErrorUri() {
        return mErrorUri;
    }

    public void setErrorUri(String errorUri) {
        this.mErrorUri = errorUri;
    }
}
