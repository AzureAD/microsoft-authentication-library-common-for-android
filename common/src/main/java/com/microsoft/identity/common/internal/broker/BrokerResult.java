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

/**
 * Encapsulates the possible responses from the broker.  Both successful response and error response.
 */
public class BrokerResult {

    private BrokerTokenResult mTokenResult;
    private BrokerErrorResult mErrorResult;
    private Boolean mSuccess = false;

    /**
     * Constructor for create successful broker result
     *
     * @param tokenResult
     */
    public BrokerResult(BrokerTokenResult tokenResult) {
        mTokenResult = tokenResult;
        mSuccess = true;
        mErrorResult = null;
    }

    /**
     * Constructor for creating an unsuccessful broker response
     *
     * @param errorResult
     */
    public BrokerResult(BrokerErrorResult errorResult) {
        mTokenResult = null;
        mSuccess = false;
        mErrorResult = errorResult;
    }

    /**
     * Indicates whether the broker request was successful or not
     *
     * @return
     */
    public Boolean getSucceeded() {
        return mSuccess;
    }

    /**
     * Gets the token result associated with a successful request
     *
     * @return
     */
    public BrokerTokenResult getTokenResult() {
        return mTokenResult;
    }

    /**
     * Gets the error result associated with a failed request
     *
     * @return
     */
    public BrokerErrorResult getErrorResult() {
        return mErrorResult;
    }

}
