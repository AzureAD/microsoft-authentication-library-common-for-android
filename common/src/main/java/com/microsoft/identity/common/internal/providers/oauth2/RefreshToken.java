package com.microsoft.identity.common.internal.providers.oauth2;


public class RefreshToken {

    public RefreshToken(TokenResponse response) {
        this.mTokenReceivedTime = response.getResponseReceivedTime();
        this.mRawRefreshToken = response.getRefreshToken();
    }

    protected long mTokenReceivedTime;
    protected String mRawRefreshToken;

    public String getRefreshToken() {
        return mRawRefreshToken;
    }

}
