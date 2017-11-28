package com.microsoft.identity.common.internal.providers.oauth2;

/**
 * Created by shoatman on 11/27/2017.
 */

public class AccessToken {

    /**
     * A buffer of ten minutes (in milliseconds) for token expiration
     */
    protected final long mTokenExpiredBuffer = 600000;

    protected long mExpiresIn;
    protected String mTokenType;
    protected long mTokenReceivedTime;
    protected String mRawAccessToken;

    public AccessToken(TokenResponse response) {
        this.mExpiresIn = response.getExpiresIn();
        this.mTokenReceivedTime = response.getResponseReceivedTime();
        this.mTokenType = response.getTokenType();
        this.mRawAccessToken = response.getAccessToken();
    }

    public String getAccessToken() {
        return mRawAccessToken;
    }

    public boolean IsExpired() {
        long currentTime = System.currentTimeMillis();
        long currentTimeWithBuffer = currentTime + mTokenExpiredBuffer;
        long expiresOn = mTokenReceivedTime + (mExpiresIn * 1000);

        return expiresOn > currentTimeWithBuffer;
    }

}
