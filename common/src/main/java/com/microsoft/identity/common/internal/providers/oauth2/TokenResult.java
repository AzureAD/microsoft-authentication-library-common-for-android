package com.microsoft.identity.common.internal.providers.oauth2;

public class TokenResult {

    private boolean mSuccess = false;

    private TokenResponse mTokenResponse;
    private TokenErrorResponse mTokenErrorResponse;

    public TokenResult(TokenResponse response, TokenErrorResponse errorResponse) {

        this.mTokenResponse = response;
        this.mTokenErrorResponse = errorResponse;

        if (response != null) {
            mSuccess = true;
        }

    }

    public TokenResponse getTokenResponse() {
        return this.mTokenResponse;
    }

    public TokenErrorResponse getErrorResponse() {
        return this.mTokenErrorResponse;
    }

    public boolean getSuccess() {
        return this.mSuccess;
    }

}
