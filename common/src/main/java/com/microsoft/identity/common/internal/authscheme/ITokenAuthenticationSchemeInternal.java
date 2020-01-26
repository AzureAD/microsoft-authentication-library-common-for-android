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
package com.microsoft.identity.common.internal.authscheme;

import com.microsoft.identity.common.exception.ClientException;

/**
 * Internal representation of properties necessary for token-based auth schemes.
 */
public interface ITokenAuthenticationSchemeInternal {

    /**
     * Sets the access token.
     *
     * @param accessToken The access token to set.
     */
    void setAccessToken(String accessToken);

    /**
     * Gets the access token.
     *
     * @return The access token to get.
     */
    String getAccessToken();

    /**
     * Returns the access token as it appears in the Authorization header. Includes any signing
     * or post-processing which may associated with the auth scheme.
     * <p>
     * For PoP requests, this method yields the signed JWT w/ req_cnf claim. For Bearer, the raw token
     * is returne.
     *
     * @return The access token as it appears in the finalized Authorization header.
     */
    String getAccessTokenForAuthorizationHeader() throws ClientException;

    /**
     * Gets the value used in the Authorization header.
     *
     * @return The Authorization header value.
     * @see <a href="https://tools.ietf.org/html/rfc7235#section-4.2">RFC-7235/§4.2</a>
     */
    String getAuthorizationRequestHeader() throws ClientException;
}
