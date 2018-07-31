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
package com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectory;

import com.microsoft.identity.common.internal.providers.microsoft.MicrosoftAuthorizationErrorResponse;

/**
 * Sub class of {@link MicrosoftAuthorizationErrorResponse}.
 * Encapsulates Azure Active Directory specific Authorization Result errors in addition to Microsoft error parameters.
 */
public class AzureActiveDirectoryAuthorizationErrorResponse extends MicrosoftAuthorizationErrorResponse {

    private String mErrorCodes;

    /**
     * Constructor of {@link AzureActiveDirectoryAuthorizationErrorResponse}.
     *
     * @param error            Error string returned from the Authorization Server.
     * @param errorDescription Description string of the error.
     */
    public AzureActiveDirectoryAuthorizationErrorResponse(final String error, final String errorDescription) {
        super(error, errorDescription);
    }

    /**
     * Getter method for error codes.
     *
     * @return error codes.
     */
    public String getErrorCodes() {
        return mErrorCodes;
    }

    /**
     * Setter method for error codes.
     *
     * @param errorCodes error codes.
     */
    public void setErrorCodes(final String errorCodes) {
        mErrorCodes = errorCodes;
    }

}
