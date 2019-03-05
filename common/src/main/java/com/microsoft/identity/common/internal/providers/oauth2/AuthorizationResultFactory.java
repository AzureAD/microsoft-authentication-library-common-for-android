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

import android.content.Intent;

/**
 * Abstract Factory class which can be extended to construct provider specific {@link AuthorizationResult}.
 */

public abstract class AuthorizationResultFactory<
        GenericAuthorizationResult extends AuthorizationResult,
        GenericAuthorizationRequest extends AuthorizationRequest
        > {

    /* Authorization Response Constants */
    protected static final String CODE = "code";
    protected static final String STATE = "state";
    protected static final String ERROR = "error";
    protected static final String ERROR_SUBCODE = "error_subcode";
    protected static final String ERROR_CODE = "error_code";
    protected static final String ERROR_DESCRIPTION = "error_description";

    /**
     * Factory method which can implemented to construct provider specific {@link AuthorizationResult}.
     *
     * @param resultCode Result code from the calling Activity.
     * @param data       Intent data from the calling Activity.
     * @return {@link AuthorizationResult}
     */
    public abstract GenericAuthorizationResult createAuthorizationResult(final int resultCode, final Intent data, final GenericAuthorizationRequest request);


}
