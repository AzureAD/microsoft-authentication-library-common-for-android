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

import com.microsoft.identity.common.java.WarningType;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.providers.RawAuthorizationResult;

import java.util.concurrent.Future;

import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * Abstracts the behavior associated with gathering a user authorization for an access token (oAuth)
 * and/or authentication information (OIDC)
 * Possible implementations include: EmbeddedWebViewAuthorizationStrategy, SystemWebViewAuthorizationStrategy, Device Code, etc...
 */
// Suppressing rawtype warnings due to the generic types AuthorizationResult, OAuth2Strategy and AuthorizationRequest
@SuppressWarnings(WarningType.rawtype_warning)
public interface IAuthorizationStrategy<
        GenericOAuth2Strategy extends OAuth2Strategy,
        GenericAuthorizationRequest extends AuthorizationRequest> {
    /**
     * Perform the authorization request.
     */
    Future<AuthorizationResult> requestAuthorization(GenericAuthorizationRequest authorizationRequest,
                                                     GenericOAuth2Strategy oAuth2Strategy)
            throws ClientException;

    /**
     * Submit the authorization results.
     *
     * @param requestCode
     * @param data
     */
    void completeAuthorization(int requestCode, @NonNull final RawAuthorizationResult data);
}
