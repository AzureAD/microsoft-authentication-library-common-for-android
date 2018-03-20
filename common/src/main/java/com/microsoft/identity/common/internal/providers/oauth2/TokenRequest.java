package com.microsoft.identity.common.internal.providers.oauth2;

/**
 * A class holding the state of the Token Request (oAuth2)
 * OAuth2 Spec: https://tools.ietf.org/html/rfc6749#section-4.1.3
 * OAuth2 Client Authentication: https://tools.ietf.org/html/rfc7521#section-4.2
 * This should include all fo the required parameters of the token request for oAuth2
 * This should provide an extension point for additional parameters to be set
 */
public class TokenRequest {

    private String mGrantType;
    private String mCode;
    private String mRedirectUri;
    private String mClientId;
    private String mClientSecret;

    public String getCode() {
        return mCode;
    }

    public void setCode(String mCode) {
        this.mCode = mCode;
    }

    public String getRedirectUri() {
        return mRedirectUri;
    }

    public void setRedirectUri(String mRedirectUri) {
        this.mRedirectUri = mRedirectUri;
    }

    public String getClientId() {
        return mClientId;
    }

    public void setClientId(String mClientId) {
        this.mClientId = mClientId;
    }

    public String getGrantType() {
        return mGrantType;
    }

    public void setGrantType(String mGrantType) {
        this.mGrantType = mGrantType;
    }
}
