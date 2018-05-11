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
package com.microsoft.identity.common.internal.providers.microsoft;

import com.google.gson.annotations.SerializedName;
import com.microsoft.identity.common.internal.providers.oauth2.TokenErrorResponse;

import java.util.List;

public class MicrosoftTokenErrorResponse extends TokenErrorResponse {

    @SerializedName("error_codes")
    protected List<Long> mErrorCodes;

    @SerializedName("timestamp")
    protected String mTimeStamp;

    @SerializedName("trace_id")
    protected String mTraceId;

    @SerializedName("correlation_id")
    protected String mCorrelationId;


    public List<Long> getErrorCodes() {
        return mErrorCodes;
    }

    public void setErrorCodes(List<Long> errorCodes) {
        this.mErrorCodes = errorCodes;
    }

    public String getTimeStamp() {
        return mTimeStamp;
    }

    public void setTimeStamp(String timeStamp) {
        this.mTimeStamp = timeStamp;
    }

    public String getTraceId() {
        return mTraceId;
    }

    public void setTraceId(String traceId) {
        this.mTraceId = traceId;
    }

    public String getCorrelationId() {
        return mCorrelationId;
    }

    public void setCorrelationId(String correlationId) {
        this.mCorrelationId = correlationId;
    }
}
