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

/**
 * Encapsulates the possible responses from the broker.  Both successful response and error response.
 */
public class BrokerResult implements Parcelable {

    private BrokerTokenResponse mTokenResponse;
    private BrokerErrorResponse mErrorResponse;
    private boolean mSuccess;

    /**
     * Constructor for create successful broker result
     *
     * @param tokenResult
     */
    public BrokerResult(BrokerTokenResponse tokenResult) {
        mTokenResponse = tokenResult;
        mSuccess = true;
        mErrorResponse = null;
    }

    /**
     * Constructor for creating an unsuccessful broker response
     *
     * @param errorResponse
     */
    public BrokerResult(BrokerErrorResponse errorResponse) {
        mTokenResponse = null;
        mSuccess = false;
        mErrorResponse = errorResponse;
    }

    protected BrokerResult(Parcel in) {
        mSuccess = in.readInt() != 0;
        mTokenResponse = in.readParcelable(BrokerTokenResponse.class.getClassLoader());
        mErrorResponse = in.readParcelable(BrokerErrorResponse.class.getClassLoader());

    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt((mSuccess ? 1 : 0));
        dest.writeParcelable(mTokenResponse, flags);
        dest.writeParcelable(mErrorResponse, flags);
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
        return mSuccess;
    }

    /**
     * Gets the token result associated with a successful request
     *
     * @return
     */
    public BrokerTokenResponse getTokenResult() {
        return mTokenResponse;
    }

    /**
     * Gets the error result associated with a failed request
     *
     * @return
     */
    public BrokerErrorResponse getErrorResult() {
        return mErrorResponse;
    }

}
