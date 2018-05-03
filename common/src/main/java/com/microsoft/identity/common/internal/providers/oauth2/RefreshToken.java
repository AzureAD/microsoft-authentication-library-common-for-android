package com.microsoft.identity.common.internal.providers.oauth2;

import com.microsoft.identity.common.internal.dto.IRefreshToken;

public abstract class RefreshToken implements IRefreshToken {

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
