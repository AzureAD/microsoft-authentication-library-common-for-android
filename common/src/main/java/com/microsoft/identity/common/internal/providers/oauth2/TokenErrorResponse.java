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

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class TokenErrorResponse implements IErrorResponse {

    @Expose()
    private int mStatusCode;

    private String mResponseBody;

    @Expose()
    private String mResponseHeadersJson;

    @Expose()
    @SerializedName("error")
    private String mError;

    @Expose()
    @SerializedName("suberror")
    private String mSubError;

    @Expose()
    @SerializedName("error_description")
    private String mErrorDescription;

    @Expose()
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
     * @return mSubError of the suberror response.
     */
    public String getSubError() {
        return mSubError;
    }

    /**
     * @param subError suberror string of the token error response.
     */
    public void setSubError(final String subError) {
        mSubError = subError;
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

    /**
     * Gets the response status code.
     *
     * @return The status code to get.
     */
    public int getStatusCode() {
        return mStatusCode;
    }

    /**
     * Sets the response status code.
     *
     * @param statusCode The status code to set.
     */
    public void setStatusCode(final int statusCode) {
        this.mStatusCode = statusCode;
    }

    /**
     * Gets the response body.
     *
     * @return The response body to get.
     */
    public String getResponseBody() {
        return mResponseBody;
    }

    /**
     * Sets the response body.
     *
     * @param responseBody The response body to set.
     */
    public void setResponseBody(final String responseBody) {
        this.mResponseBody = responseBody;
    }

    /**
     * Gets the response headers.
     *
     * @return The response headers to get.
     */
    public String getResponseHeadersJson() {
        return mResponseHeadersJson;
    }

    /**
     * Sets the response headers.
     *
     * @param responseHeadersJson The response headers to set.
     */
    public void setResponseHeadersJson(final String responseHeadersJson) {
        this.mResponseHeadersJson = responseHeadersJson;
    }

    //CHECKSTYLE:OFF
    @Override
    public String toString() {
        return "TokenErrorResponse{" +
                "mStatusCode=" + mStatusCode +
                ", mResponseBody='" + mResponseBody + '\'' +
                ", mResponseHeadersJson=" + mResponseHeadersJson +
                ", mError='" + mError + '\'' +
                ", mSubError='" + mSubError + '\'' +
                ", mErrorDescription='" + mErrorDescription + '\'' +
                ", mErrorUri='" + mErrorUri + '\'' +
                '}';
    }
    //CHECKSTYLE:ON
}
