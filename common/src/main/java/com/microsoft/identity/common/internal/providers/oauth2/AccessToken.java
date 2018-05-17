// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// This code is licensed under the MIT License.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files(the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions :
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.
package com.microsoft.identity.common.internal.providers.oauth2;

public class AccessToken {
    /**
     * A buffer of ten minutes (in milliseconds) for token expiration
     */
    private final long mTokenExpiredBuffer = 600000;
    private static final int SECONDS_MS = 1000;

    private long mExpiresIn;
    private String mTokenType;
    private long mTokenReceivedTime;
    private String mRawAccessToken;

    public AccessToken(TokenResponse response) {
        this.mExpiresIn = response.getExpiresIn();
        this.mTokenReceivedTime = response.getResponseReceivedTime();
        this.mTokenType = response.getTokenType();
        this.mRawAccessToken = response.getAccessToken();
    }

    public String getAccessToken() {
        return mRawAccessToken;
    }

    public boolean isExpired() {
        long currentTime = System.currentTimeMillis();
        long currentTimeWithBuffer = currentTime + mTokenExpiredBuffer;
        long expiresOn = mTokenReceivedTime + (mExpiresIn * SECONDS_MS);

        return expiresOn > currentTimeWithBuffer;
    }

    public long getTokenExpiredBuffer() {
        return mTokenExpiredBuffer;
    }

    public long getExpiresIn() {
        return mExpiresIn;
    }

    public void setExpiresIn(long expiresIn) {
        this.mExpiresIn = expiresIn;
    }

    public String getTokenType() {
        return mTokenType;
    }

    public void setTokenType(String tokenType) {
        this.mTokenType = tokenType;
    }

    public long getTokenReceivedTime() {
        return mTokenReceivedTime;
    }

    public void setTokenReceivedTime(long tokenReceivedTime) {
        this.mTokenReceivedTime = tokenReceivedTime;
    }

    public String getRawAccessToken() {
        return mRawAccessToken;
    }

    public void setRawAccessToken(String rawAccessToken) {
        this.mRawAccessToken = rawAccessToken;
    }
}
