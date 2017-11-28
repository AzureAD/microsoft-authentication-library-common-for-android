package com.microsoft.identity.common.internal.providers.oauth2;

import android.media.session.MediaSession;

import com.microsoft.identity.common.adal.internal.util.StringExtensions;

/**
 * Created by shoatman on 11/27/2017.
 */

public class AccessToken {

    public AccessToken(TokenResponse response){
        this.mExpiresIn = response.getExpiresIn();
        this.mTokenReceivedTime = response.getResponseReceivedTime();
        this.mTokenType = response.getTokenType();
        this.mRawAccessToken = response.getAccessToken();
    }


    /**
     * A buffer of ten minutes (in milliseconds) for token expiration
     */
    protected final long mTokenExpiredBuffer = 600000;

    protected long mExpiresIn;
    protected String mTokenType;
    protected long mTokenReceivedTime;
    protected String mRawAccessToken;

    public String getAccessToken(){return mRawAccessToken;}

    public boolean IsExpired(){
        long currentTime = System.currentTimeMillis();
        long currentTimeWithBuffer = currentTime + mTokenExpiredBuffer;
        long expiresOn = mTokenReceivedTime + (mExpiresIn * 1000);

        return expiresOn > currentTimeWithBuffer;
    }

}
