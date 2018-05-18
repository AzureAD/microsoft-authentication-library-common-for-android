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

import com.microsoft.identity.common.internal.dto.IRefreshToken;

public abstract class RefreshToken implements IRefreshToken {

    /**
     * Constructor of RefreshToken.
     *
     * @param response TokenResponse object.
     */
    public RefreshToken(TokenResponse response) {
        this.mTokenReceivedTime = response.getResponseReceivedTime();
        this.mRawRefreshToken = response.getRefreshToken();
    }

    private long mTokenReceivedTime;
    private String mRawRefreshToken;

    /**
     * @param rawRefreshToken raw refresh token of RefreshToken object.
     */
    protected void setRawRefreshToken(String rawRefreshToken) {
        this.mRawRefreshToken = rawRefreshToken;
    }

    /**
     * @param tokenReceivedTime received time of refresh token.
     */
    protected void setTokenReceivedTime(long tokenReceivedTime) {
        this.mTokenReceivedTime = tokenReceivedTime;
    }

    /**
     * @return mRawRefreshToken of RefreshToken object.
     */
    public String getRefreshToken() {
        return mRawRefreshToken;
    }

    /**
     * @return mTokenReceivedTime of RefreshToken object.
     */
    public long getTokenReceivedTime() {
        return mTokenReceivedTime;
    }
}
