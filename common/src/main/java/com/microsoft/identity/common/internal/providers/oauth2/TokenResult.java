package com.microsoft.identity.common.internal.providers.oauth2;

/**
 * Holds the request of a token request.  The request will either contain the success result or the error result.
 */
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

    /**
     * Returns the TokenResponse (success) associated with the request.
     * @return TokenResponse
     */
    public TokenResponse getTokenResponse() {
        return this.mTokenResponse;
    }

    /**
     * Returns the TokenErrorResponse associated with the request.
     * @return TokenErrorResponse
     */
    public TokenErrorResponse getErrorResponse() {
        return this.mTokenErrorResponse;
    }

    /**
     * Returns whether the token request was successful or not.
     * @return boolean
     */
    public boolean getSuccess() {
        return this.mSuccess;
    }

}
