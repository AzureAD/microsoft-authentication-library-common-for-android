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

public class AuthorizationErrorResponse {

    /* Constants */
    public static final String AUTHORIZATION_FAILED = "authorization_failed";

    public static final String USER_CANCEL = "user_cancelled";

    public static final String NULL_INTENT = "Received null intent";

    public static final String AUTHORIZATION_SERVER_INVALID_RESPONSE = "The authorization server returned an invalid response.";

    public static final String USER_CANCELLED_FLOW = "User pressed device back button to cancel the flow.";

    public static final String STATE_NOT_THE_SAME = "Returned state from authorize endpoint is not the same as the one sent";

    public static final String STATE_NOT_RETURNED = "State is not returned";

    public static final String UNKNOWN_ERROR = "Unknown error";

    public static final String UNKNOWN_RESULT_CODE = "Unknown result code returned ";

    public static final String BROKER_NEEDS_TO_BE_INSTALLED = "Device needs to have broker installed";


    private String mError;
    private String mErrorDescription;

    public AuthorizationErrorResponse(String error, String errorDescription) {
        mError = error;
        mErrorDescription = errorDescription;
    }

    public String getError() {
        return mError;
    }

    public void setError(String mError) {
        this.mError = mError;
    }

    public String getErrorDescription() {
        return mErrorDescription;
    }

    public void setErrorDescription(String mErrorDescription) {
        this.mErrorDescription = mErrorDescription;
    }
}
