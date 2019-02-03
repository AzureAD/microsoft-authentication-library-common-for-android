//  Copyright (c) Microsoft Corporation.
//  All rights reserved.
//
//  This code is licensed under the MIT License.
//
//  Permission is hereby granted, free of charge, to any person obtaining a copy
//  of this software and associated documentation files(the "Software"), to deal
//  in the Software without restriction, including without limitation the rights
//  to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
//  copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions :
//
//  The above copyright notice and this permission notice shall be included in
//  all copies or substantial portions of the Software.
//
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//  THE SOFTWARE.
package com.microsoft.identity.common.internal.broker;

import android.os.Parcel;
import android.os.Parcelable;

import com.microsoft.identity.common.internal.providers.microsoft.MicrosoftTokenErrorResponse;

import java.util.ArrayList;

/**
 * Encapsulates the broker error result
 */
public class BrokerErrorResponse extends MicrosoftTokenErrorResponse implements Parcelable {

    public static final String INTERACTION_REQUIRED = "interaction_required";

    public static final String INVALID_GRANT = "invalid_grant";


    private String mOAuthError;
    private String mOAuthSubError;
    private String mOAuthErrorMetadata;
    private String mOAuthErrorDescription;
    private int mHttpStatusCode;
    private String mHttpResponseBody;
    private String mHttpResponseHeaders;


    public BrokerErrorResponse(){

    }

    public BrokerErrorResponse(MicrosoftTokenErrorResponse microsoftTokenErrorResponse){
        setError(microsoftTokenErrorResponse.getError());
        setErrorDescription(microsoftTokenErrorResponse.getErrorDescription());
        setErrorUri(microsoftTokenErrorResponse.getErrorUri());
        setErrorCodes(microsoftTokenErrorResponse.getErrorCodes());
        setTimeStamp(microsoftTokenErrorResponse.getTimeStamp());
        setTraceId(microsoftTokenErrorResponse.getTraceId());
        setCorrelationId(microsoftTokenErrorResponse.getCorrelationId());
    }

    protected BrokerErrorResponse(Parcel in) {
        if(in!=null) {
            setError(in.readString());
            setErrorDescription(in.readString());
            setErrorUri(in.readString());
            ArrayList errorcodes = new ArrayList();
            in.readList(errorcodes, ArrayList.class.getClassLoader());
            setErrorCodes(errorcodes);
            setTimeStamp(in.readString());
            setTraceId(in.readString());
            setCorrelationId(in.readString());
            setOAuthError(in.readString());
            setOAuthSubError(in.readString());
            setOAuthErrorMetadata(in.readString());
            setOAuthErrorDescription(in.readString());
            setHttpStatusCode(in.readInt());
            setHttpResponseBody(in.readString());
            setHttpResponseHeaders(in.readString());
        }
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        if(dest!=null) {
            dest.writeString(getError());
            dest.writeString(getErrorDescription());
            dest.writeString(getErrorUri());
            dest.writeList(getErrorCodes());
            dest.writeString(getTimeStamp());
            dest.writeString(getTraceId());
            dest.writeString(getCorrelationId());
            dest.writeString(getOAuthError());
            dest.writeString(getOAuthSubError());
            dest.writeString(getOAuthErrorMetadata());
            dest.writeString(getOAuthErrorDescription());
            dest.writeInt(getHttpStatusCode());
            dest.writeString(getHttpResponseBody());
            dest.writeString(getHttpResponseHeaders());
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<BrokerErrorResponse> CREATOR = new Creator<BrokerErrorResponse>() {
        @Override
        public BrokerErrorResponse createFromParcel(Parcel in) {
            return new BrokerErrorResponse(in);
        }

        @Override
        public BrokerErrorResponse[] newArray(int size) {
            return new BrokerErrorResponse[size];
        }
    };

    public boolean isInteractionRequired() {
        return mOAuthError.equalsIgnoreCase(INTERACTION_REQUIRED);
    }

    public boolean isInvalidGrant() {
        return mOAuthError.equalsIgnoreCase(INVALID_GRANT);
    }

    public String getOAuthError() {
        return mOAuthError;
    }

    public void setOAuthError(String oAuthError) {
        this.mOAuthError = oAuthError;
    }

    public String getOAuthSubError() {
        return mOAuthSubError;
    }

    public void setOAuthSubError(String oAuthSubError) {
        this.mOAuthSubError = oAuthSubError;
    }

    public String getOAuthErrorMetadata() {
        return mOAuthErrorMetadata;
    }

    public void setOAuthErrorMetadata(String oAuthErrorMetadata) {
        this.mOAuthErrorMetadata = oAuthErrorMetadata;
    }

    public String getOAuthErrorDescription() {
        return mOAuthErrorDescription;
    }

    public void setOAuthErrorDescription(String oAuthErrorDescription) {
        this.mOAuthErrorDescription = oAuthErrorDescription;
    }

    public String getHttpResponseBody() {
        return mHttpResponseBody;
    }

    public void setHttpResponseBody(String httpResponseBody) {
        this.mHttpResponseBody = httpResponseBody;
    }

    public String getHttpResponseHeaders() {
        return mHttpResponseHeaders;
    }

    public void setHttpResponseHeaders(String httpResponseHeaders) {
        this.mHttpResponseHeaders = httpResponseHeaders;
    }

    public int getHttpStatusCode() {
        return mHttpStatusCode;
    }

    public void setHttpStatusCode(int httpStatusCode) {
        mHttpStatusCode = httpStatusCode;
    }
}
