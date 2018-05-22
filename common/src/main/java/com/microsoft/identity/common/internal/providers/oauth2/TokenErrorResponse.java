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

import com.google.gson.annotations.SerializedName;

public class TokenErrorResponse {

    @SerializedName("error")
    private String mError;

    @SerializedName("error_description")
    private String mErrorDescription;

    @SerializedName("error_uri")
    private String mErrorUri;

    /**
     * @return mError of the token error response.
     */
    public String getError() {
        return mError;
    }

    /**
     * @param error error string of the token error response.
     */
    public void setError(final String error) {
        mError = error;
    }

    /**
     * @return mErrorDescription of the token error response.
     */
    public String getErrorDescription() {
        return mErrorDescription;
    }

    /**
     * @param errorDescription error description details.
     */
    public void setErrorDescription(final String errorDescription) {
        mErrorDescription = errorDescription;
    }

    /**
     * @return mErrorUri of the token error response.
     */
    public String getErrorUri() {
        return mErrorUri;
    }

    /**
     * @param errorUri error URI string.
     */
    public void setErrorUri(final String errorUri) {
        mErrorUri = errorUri;
    }

    @Override
    public String toString() {
        return "TokenErrorResponse{" +
                "mError='" + mError + '\'' +
                ", mErrorDescription='" + mErrorDescription + '\'' +
                ", mErrorUri='" + mErrorUri + '\'' +
                '}';
    }
}
