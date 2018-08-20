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
import android.net.Uri;

import com.microsoft.identity.common.exception.ClientException;

import java.util.concurrent.Future;

/**
 * Abstracts the behavior associated with gathering a user authorization for an access token (oAuth)
 * and/or authentication information (OIDC)
 * Possible implementations include: EmbeddedWebViewAuthorizationStrategy, SystemWebViewAuthorizationStrategy, Device Code, etc...
 */
public abstract class AuthorizationStrategy {
    public static final int BROWSER_FLOW = 1001;

    public static final String REQUEST_URL_KEY = "com.microsoft.identity.request.url.key";

    public static final String REQUEST_ID = "com.microsoft.identity.request.id";

    public static final String CUSTOM_TAB_REDIRECT = "com.microsoft.identity.customtab.redirect";

    public static final String AUTHORIZATION_FINAL_URL = "com.microsoft.identity.client.final.url";

    public static final class UIResponse {
        public static final int CANCEL = 2001;

        public static final int AUTH_CODE_ERROR = 2002;

        public static final int AUTH_CODE_COMPLETE = 2003;

        public static final String ERROR_CODE = "error_code";

        public static final String ERROR_DESCRIPTION = "error_description";
    }

    /**
     * Perform the authorization request.
     *
     * @param requestUrl authorization request url
     */
    public abstract Future<AuthorizationResult> requestAuthorization(final Uri requestUrl) throws ClientException;

    public abstract void completeAuthorization(int requestCode, int resultCode, final Intent data);
}
