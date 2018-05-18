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
     * A buffer of ten minutes (in milliseconds) for token expiration.
     */
    private final long mTokenExpiredBuffer = 600000;
    private static final int SECONDS_MS = 1000;

    private long mExpiresIn;
    private String mTokenType;
    private long mTokenReceivedTime;
    private String mRawAccessToken;

    /**
     * Constructor of AccessToken.
     *
     * @param response TokenResponse object
     */
    public AccessToken(TokenResponse response) {
        mExpiresIn = response.getExpiresIn();
        mTokenReceivedTime = response.getResponseReceivedTime();
        mTokenType = response.getTokenType();
        mRawAccessToken = response.getAccessToken();
    }

    /**
     * @return mRawAccessToken of the access token object.
     */
    public String getAccessToken() {
        return mRawAccessToken;
    }

    /**
     * @return true if the access token is expired, false otherwise.
     */
    public boolean isExpired() {
        long currentTime = System.currentTimeMillis();
        long currentTimeWithBuffer = currentTime + mTokenExpiredBuffer;
        long expiresOn = mTokenReceivedTime + (mExpiresIn * SECONDS_MS);

        return expiresOn > currentTimeWithBuffer;
    }

    /**
     * @return mTokenExpiredBuffer of the access token object.
     */
    public long getTokenExpiredBuffer() {
        return mTokenExpiredBuffer;
    }

    /**
     * @return mExpiresIn of the access token object.
     */
    public long getExpiresIn() {
        return mExpiresIn;
    }

    /**
     * @param expiresIn expires in.
     */
    public void setExpiresIn(long expiresIn) {
        mExpiresIn = expiresIn;
    }

    /**
     * @return mTokenType of the access token object.
     */
    public String getTokenType() {
        return mTokenType;
    }

    /**
     * @param tokenType token type.
     */
    public void setTokenType(String tokenType) {
        mTokenType = tokenType;
    }

    /**
     * @return mTokenReceivedTime of the access token object.
     */
    public long getTokenReceivedTime() {
        return mTokenReceivedTime;
    }

    /**
     * @param tokenReceivedTime token received time.
     */
    public void setTokenReceivedTime(long tokenReceivedTime) {
        mTokenReceivedTime = tokenReceivedTime;
    }

    /**
     * @return mRawAccessToken of the access token object.
     */
    public String getRawAccessToken() {
        return mRawAccessToken;
    }

    /**
     * @param rawAccessToken raw access token.
     */
    public void setRawAccessToken(String rawAccessToken) {
        mRawAccessToken = rawAccessToken;
    }
}
