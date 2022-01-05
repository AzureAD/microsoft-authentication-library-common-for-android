/**
 * // Copyright (c) Microsoft Corporation.
 * // All rights reserved.
 * //
 * // This code is licensed under the MIT License.
 * //
 * // Permission is hereby granted, free of charge, to any person obtaining a copy
 * // of this software and associated documentation files(the "Software"), to deal
 * // in the Software without restriction, including without limitation the rights
 * // to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
 * // copies of the Software, and to permit persons to whom the Software is
 * // furnished to do so, subject to the following conditions :
 * //
 * // The above copyright notice and this permission notice shall be included in
 * // all copies or substantial portions of the Software.
 * //
 * // THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * // IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * // FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * // AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * // LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * // OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * // THE SOFTWARE.
 * */

package com.microsoft.identity.common.internal.providers.oauth2;

/**
 * The response type values defined by the
 * [OAuth 2.0](https://tools.ietf.org/html/rfc6749) and
 * [OpenID Connect Core 1.0](http://openid.net/specs/openid-connect-core-1_0.html)
 **/

public final class ResponseType {
    /**
     * For requesting an authorization code.
     *
     * @see "The OAuth 2.0 Authorization Framework (RFC 6749), Section 3.1.1
     * <https://tools.ietf.org/html/rfc6749#section-3.1.1>"
     */
    public static final String CODE = "code";

    /**
     * For requesting an access token via an implicit grant.
     *
     * @see "The OAuth 2.0 Authorization Framework (RFC 6749), Section 3.1.1
     * <https://tools.ietf.org/html/rfc6749#section-3.1.1>"
     */
    public static final String TOKEN = "token";

    /**
     * For requesting an OpenID Conenct ID Token.
     *
     * @see "The OAuth 2.0 Authorization Framework (RFC 6749), Section 3.1.1
     * <https://tools.ietf.org/html/rfc6749#section-3.1.1>"
     */
    public static final String ID_TOKEN = "id_token";
}
