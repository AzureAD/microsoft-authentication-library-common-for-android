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
package com.microsoft.identity.common.java.providers.oauth2;

import com.google.gson.annotations.Expose;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * Class to encapsulate Authorization Result errors.
 */
@Getter
@Setter
@Accessors(prefix = "m")
public class AuthorizationErrorResponse implements IErrorResponse{

    /**
     * Error returned due to Authorization request failure.
     */
    @Expose()
    private String mError;

    /**
     * Error description returned due to Authorization request failure
     * */
    @Expose()
    private String mErrorDescription;

    /**
     * Set when the account needs to be workplace joined to access the resource.
     */
    private String mUpnToWpj;

    /**
     * Set to true when the account needs to be workplace joined with hardware backed keys.
     */
    private boolean mTokenProtectionRequired;

    /**
     * Constructor of {@link AuthorizationErrorResponse}.
     *
     * @param error            error returned due to Authorization request failure.
     * @param errorDescription error description returned due to Authorization request failure.
     */
    public AuthorizationErrorResponse(final String error, final String errorDescription) {
        mError = error;
        mErrorDescription = errorDescription;
    }

    public static class Fields {
        public final static String ERROR = "error";
        public final static String ERROR_DESCRIPTION = "error_description";
    }
}
