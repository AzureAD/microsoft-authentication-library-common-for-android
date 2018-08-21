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
package com.microsoft.identity.common.internal.providers.microsoft.microsoftsts;

import com.microsoft.identity.common.internal.providers.microsoft.MicrosoftAuthorizationResult;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationErrorResponse;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationResponse;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationStatus;

/**
 * Sub class of {@link MicrosoftAuthorizationResult}.
 * Encapsulates Microsoft Authorization Response and additional MicrosoftSTS specific parameters and errors.
 */
public class MicrosoftStsAuthorizationResult
        extends MicrosoftAuthorizationResult<MicrosoftStsAuthorizationResponse, MicrosoftStsAuthorizationErrorResponse> {

    /**
     * Constructor of {@link MicrosoftStsAuthorizationResult}.
     *
     * @param authStatus   {@link AuthorizationStatus}
     * @param authResponse {@link AuthorizationResponse}
     */
    public MicrosoftStsAuthorizationResult(final AuthorizationStatus authStatus, final MicrosoftStsAuthorizationResponse authResponse) {
        super(authStatus, authResponse);
    }

    /**
     * Constructor of {@link MicrosoftStsAuthorizationResult}.
     *
     * @param authStatus    {@link AuthorizationStatus}
     * @param errorResponse {@link AuthorizationErrorResponse}
     */
    public MicrosoftStsAuthorizationResult(final AuthorizationStatus authStatus, final MicrosoftStsAuthorizationErrorResponse errorResponse) {
        super(authStatus, errorResponse);
    }

}
