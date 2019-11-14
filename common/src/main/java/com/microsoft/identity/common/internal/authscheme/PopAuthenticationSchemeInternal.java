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

import androidx.annotation.NonNull;

import java.net.URL;

public class PopAuthenticationSchemeInternal extends TokenAuthenticationScheme {

    private static final String SCHEME_POP = "PoP";

    private String mHttpMethod;
    private URL mUrl;
    private String mNonce;
    private IDevicePopManager mPopManager;

    protected PopAuthenticationSchemeInternal(@NonNull final String method,
                                              @NonNull final URL url,
                                              @NonNull final String nonce) {
        super(SCHEME_POP);
        mHttpMethod = method;
        mUrl = url;
        mNonce = nonce;
    }

    final void setDevicePopManager(@NonNull final IDevicePopManager popManager) {
        mPopManager = popManager;
    }

    @Override
    String getAuthorizationRequestHeader() {
        return getName()
                + " "
                + mPopManager.getAuthorizationHeaderValue(
                mHttpMethod,
                mUrl,
                getAccessToken(),
                mNonce
        );
    }

}