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
package com.microsoft.identity.common.java.providers.microsoft;

import com.google.gson.annotations.SerializedName;
import com.microsoft.identity.common.java.providers.oauth2.TokenErrorResponse;

import java.util.List;

public class MicrosoftTokenErrorResponse extends TokenErrorResponse {

    @SerializedName("error_codes")
    private List<Long> mErrorCodes;

    @SerializedName("timestamp")
    private String mTimeStamp;

    @SerializedName("trace_id")
    private String mTraceId;

    @SerializedName("correlation_id")
    private String mCorrelationId;

    @SerializedName("oAuth_metadata")
    private String mOAuthErrorMetadata;

    /**
     * @return mErrorCodes of the Microsoft token error response.
     */
    public List<Long> getErrorCodes() {
        return mErrorCodes;
    }

    /**
     * @param errorCodes error codes of the Microsoft token error response.
     */
    public void setErrorCodes(final List<Long> errorCodes) {
        mErrorCodes = errorCodes;
    }

    /**
     * @return mTimeStamp of the Microsoft token error response.
     */
    public String getTimeStamp() {
        return mTimeStamp;
    }

    /**
     * @param timeStamp time stamp of the Microsoft token error response.
     */
    public void setTimeStamp(final String timeStamp) {
        mTimeStamp = timeStamp;
    }

    /**
     * @return mTraceId of the Microsoft token error response.
     */
    public String getTraceId() {
        return mTraceId;
    }

    /**
     * @param traceId trace ID of the Microsoft token error response.
     */
    public void setTraceId(final String traceId) {
        mTraceId = traceId;
    }

    /**
     * @return mCorrelationId of the Microsoft token error response.
     */
    public String getCorrelationId() {
        return mCorrelationId;
    }

    /**
     * @param correlationId correlation ID of the Microsoft token error response.
     */
    public void setCorrelationId(final String correlationId) {
        mCorrelationId = correlationId;
    }

    /**
     *
     * @return mOAuthErrorMetadata of Microsoft token error response
     */
    public String getOAuthErrorMetadata() {
        return mOAuthErrorMetadata;
    }

    /**
     *
     * @param oAuthErrorMetadata of Microsoft token error response
     */
    public void setOAuthErrorMetadata(final String oAuthErrorMetadata) {
        this.mOAuthErrorMetadata = oAuthErrorMetadata;
    }
}
