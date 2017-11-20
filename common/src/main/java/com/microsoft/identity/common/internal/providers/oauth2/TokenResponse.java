package com.microsoft.identity.common.internal.providers.oauth2;

/**
 * This is the class encapsulating the details of the TokenResponse (oAuth2/OIDC)
 * https://tools.ietf.org/html/rfc6749#section-4.1.4
 * It should include all of the required and optional parameters based on the protocol and
 * support an extension to allow the authorization server / openid provider to send back additional information
 */
public class TokenResponse {

    /**
     * RECOMMENDED.  The lifetime in seconds of the access token.  For
     * example, the value "3600" denotes that the access token will
     * expire in one hour from the time the response was generated.
     * If omitted, the authorization server SHOULD provide the
     * expiration time via other means or document the default value.
     *
     * @see <a href="https://tools.ietf.org/html/rfc6749#section-5.1">RFC 6749 - Successful Response</a>
     */
    protected int mExpiresIn;

    /**
     * REQUIRED.  The access token issued by the authorization server.
     *
     * @see <a href="https://tools.ietf.org/html/rfc6749#section-5.1">RFC 6749 - Successful Response</a>
     */
    protected String mAccessToken;

    /**
     * REQUIRED.  The type of the token issued as described in
     * <a href="https://tools.ietf.org/html/rfc6749#section-7.1">Section 7.1.</a>
     * Value is case insensitive.
     *
     * @see <a href="https://tools.ietf.org/html/rfc6749#section-5.1">RFC 6749 - Successful Response</a>
     */
    protected String mTokenType;

    /**
     * OPTIONAL.  The refresh token, which can be used to obtain new
     * access tokens using the same authorization grant as described
     * in <a href="https://tools.ietf.org/html/rfc6749#section-6">Section 6.</a>
     *
     * @see <a href="https://tools.ietf.org/html/rfc6749#section-5.1">RFC 6749 - Successful Response</a>
     */
    protected String mRefreshToken;

    /**
     * OPTIONAL, if identical to the scope requested by the client;
     * otherwise, REQUIRED.  The scope of the access token as
     * described by <a href="https://tools.ietf.org/html/rfc6749#section-3.3">Section 3.3.</a>
     *
     * @see <a href="https://tools.ietf.org/html/rfc6749#section-5.1">RFC 6749 - Successful Response</a>
     */
    protected String mScope;

    /**
     * REQUIRED if the "state" parameter was present in the client
     * authorization request.  The exact value received from the
     * client.
     *
     * @see <a href="https://tools.ietf.org/html/rfc6749#section-4.2.2">RFC 6749 - Access Token Response</a>
     */
    protected String mState;

    /**
     * Gets the response expires_in.
     *
     * @return The expires_in to get.
     */
    public int getExpiresIn() {
        return mExpiresIn;
    }

    /**
     * Sets the response expires_in.
     *
     * @param mExpiresIn The expires_in to set.
     */
    public void setExpiresIn(int mExpiresIn) {
        this.mExpiresIn = mExpiresIn;
    }

    /**
     * Gets the response access_token.
     *
     * @return The access_token to get.
     */
    public String getAccessToken() {
        return mAccessToken;
    }

    /**
     * Sets the response access_token.
     *
     * @param mAccessToken The access_token to set.
     */
    public void setAccessToken(String mAccessToken) {
        this.mAccessToken = mAccessToken;
    }

    /**
     * Gets the response token_type.
     *
     * @return The token_type to get.
     */
    public String getTokenType() {
        return mTokenType;
    }

    /**
     * Sets the response token_type.
     *
     * @param mTokenType The token_type to set.
     */
    public void setTokenType(String mTokenType) {
        this.mTokenType = mTokenType;
    }

    /**
     * Gets the response refresh_token.
     *
     * @return The refresh_token to get.
     */
    public String getRefreshToken() {
        return mRefreshToken;
    }

    /**
     * Sets the response refresh_token.
     *
     * @param mRefreshToken The refresh_token to set.
     */
    public void setRefreshToken(String mRefreshToken) {
        this.mRefreshToken = mRefreshToken;
    }

    /**
     * Gets the response scope.
     *
     * @return The scope to get.
     */
    public String getScope() {
        return mScope;
    }

    /**
     * Sets the response scope.
     *
     * @param mScope The scope to set.
     */
    public void setScope(String mScope) {
        this.mScope = mScope;
    }

    /**
     * Gets the response state.
     *
     * @return The state to get.
     */
    public String getState() {
        return mState;
    }

    /**
     * Sets the response state.
     *
     * @param mState The state to set.
     */
    public void setState(String mState) {
        this.mState = mState;
    }
}
