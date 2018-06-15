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
package com.microsoft.identity.common.internal.ui.embeddedwebview;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.http.SslError;
import android.support.annotation.NonNull;
import android.view.View;
import android.webkit.ClientCertRequest;
import android.webkit.HttpAuthHandler;
import android.webkit.SslErrorHandler;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.microsoft.identity.common.adal.internal.util.StringExtensions;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationRequest;
import com.microsoft.identity.common.internal.util.StringUtil;

public abstract class OAuth2WebViewClient extends WebViewClient {
    private final AuthorizationRequest mRequest;
    private final Context mContext;

    /**
     * @return Authorization request
     */
    public AuthorizationRequest getRequest() {
        return mRequest;
    }

    /**
     * @return context
     */
    public Context getContext() {
        return mContext;
    }

    /**
     * Constructor for the OAuth2 basic web view client.
     * @param context app Context
     * @param request Authorization request
     */
    OAuth2WebViewClient(@NonNull final Context context, @NonNull final AuthorizationRequest request) {
        //the validation of redirect url and authorization request should be in upper level before launching the webview.
        if (null == context || null == request) {
            throw new IllegalArgumentException("Null parameter to initialize OAuth2WebViewClient.");
        }

        mContext = context;
        mRequest = request;
    }
}
