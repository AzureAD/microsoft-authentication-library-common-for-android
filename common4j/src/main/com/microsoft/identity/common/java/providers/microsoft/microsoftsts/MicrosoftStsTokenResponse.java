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
package com.microsoft.identity.common.java.providers.microsoft.microsoftsts;

import com.google.gson.annotations.SerializedName;
import com.microsoft.identity.common.java.providers.microsoft.MicrosoftTokenResponse;
import com.microsoft.identity.common.java.providers.oauth2.TokenResponse;

/**
 * {@link TokenResponse} subclass for the Microsoft STS (V2).
 */
public class MicrosoftStsTokenResponse extends MicrosoftTokenResponse {

    @SerializedName("not_before")
    private String mExpiresNotBefore;

    @SerializedName("continuation_token")
    private String mContinuationToken;

    /**
     * Gets expires not before as String ( epoch time in seconds)
     * @return
     */
    public String getExpiresNotBefore() {
        return mExpiresNotBefore;
    }

    /**
     * Sets expires not before as String ( epoch time in seconds)
     * @param expiresNotBefore
     */
    public void setExpiresNotBefore(final String expiresNotBefore) {
        mExpiresNotBefore = expiresNotBefore;
    }

    /**
     * Gets the continuation token returned with an inline broker PRT response.
     *
     * @return continuation token, or null when one was not returned.
     */
    public String getContinuationToken() {
        return mContinuationToken;
    }

    /**
     * Sets the continuation token returned with an inline broker PRT response.
     *
     * @param continuationToken continuation token.
     */
    public void setContinuationToken(final String continuationToken) {
        mContinuationToken = continuationToken;
    }
}
