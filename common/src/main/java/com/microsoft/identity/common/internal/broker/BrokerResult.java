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

import com.microsoft.identity.common.internal.providers.oauth2.TokenResult;

/**
 * Encapsulates the possible responses from the broker.  Both successful response and error response.
 */
public class BrokerResult extends TokenResult implements Parcelable {

    private BrokerTokenResponse mBrokerTokenResponse;
    private BrokerErrorResponse mBrokerErrorResponse;

    /**
     * Constructor for create successful broker response
     *
     * @param tokenResponse
     */
    public BrokerResult(BrokerTokenResponse tokenResponse) {
        this(tokenResponse, null);
    }

    /**
     * Constructor for creating an unsuccessful broker response
     *
     * @param errorResponse
     */
    public BrokerResult(BrokerErrorResponse errorResponse) {
        this(null, errorResponse);
    }

    /**
     * Constructor for creating an BrokerResult
     * @param brokerTokenResponse
     * @param brokerErrorResponse
     */
    public BrokerResult(BrokerTokenResponse brokerTokenResponse, BrokerErrorResponse brokerErrorResponse) {
        super(brokerTokenResponse, brokerErrorResponse);
        mBrokerTokenResponse = brokerTokenResponse;
        mBrokerErrorResponse = brokerErrorResponse;
    }


    protected BrokerResult(Parcel in) {
        if (in != null) {
            setSuccess(in.readInt() != 0);
            mBrokerTokenResponse = in.readParcelable(BrokerTokenResponse.class.getClassLoader());
            mBrokerErrorResponse = in.readParcelable(BrokerErrorResponse.class.getClassLoader());
        }

    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        if (dest != null) {
            dest.writeInt((getSuccess() ? 1 : 0));
            dest.writeParcelable(mBrokerTokenResponse, flags);
            dest.writeParcelable(mBrokerErrorResponse, flags);
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<BrokerResult> CREATOR = new Creator<BrokerResult>() {
        @Override
        public BrokerResult createFromParcel(Parcel in) {
            return new BrokerResult(in);
        }

        @Override
        public BrokerResult[] newArray(int size) {
            return new BrokerResult[size];
        }
    };

    /**
     * Indicates whether the broker request was successful or not
     *
     * @return
     */
    public boolean isSuccessful() {
        return getSuccess();
    }

    /**
     * Gets the token result associated with a successful request
     *
     * @return
     */
    @Override
    public BrokerTokenResponse getTokenResponse() {
        return mBrokerTokenResponse;
    }

    /**
     * Gets the error result associated with a failed request
     *
     * @return
     */
    @Override
    public BrokerErrorResponse getErrorResponse() {
        return mBrokerErrorResponse;
    }

}
