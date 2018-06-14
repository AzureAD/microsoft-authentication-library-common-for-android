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

import android.annotation.TargetApi;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;

import com.microsoft.identity.common.adal.internal.AuthenticationConstants;
import com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectory.AzureActiveDirectoryAuthorizationRequest;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationRequest;
import com.microsoft.identity.common.internal.util.StringUtil;

import java.util.Locale;

/**
 * For webview client, we do not distinguish V1 from V2.
 * Thus we name V1 and V2 webviewclient as AADWebViewClient, synced with the naming in the iOS common library.
 *
 * The only differences between V1 and V2 is
 * 1. on the start url construction, which is handled in the Authorization request classes.
 * 2. the auth result is handled in the Authorization result classes.
 */
class AzureActiveDirectoryWebViewClient extends OAuth2WebViewClient {
    //TODO change the AuthorizationRequest type into MicrosoftAuthorizationRequest
    AzureActiveDirectoryWebViewClient(final Context context, final String redirectURL, final AuthorizationRequest request) {
        super(context, redirectURL, request);
    }

    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        if (StringUtil.isEmpty(url)) {
            throw new IllegalArgumentException("Empty url.");
        }

        return handleUrl(view, url);
    }

    @Override
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
        final Uri requestUrl = request.getUrl();
        return  handleUrl(view, requestUrl.toString());
    }

    private boolean handleUrl(final WebView view, final String url) {
        final String formattedURL = url.toLowerCase(Locale.US);
        if (formattedURL.startsWith(AuthenticationConstants.Broker.PKEYAUTH_REDIRECT)) {
            //TODO handle Pkeyauth challenge
        } else if (formattedURL.startsWith(getRedirectUrl().toLowerCase(Locale.US))) {
            processRedirectUrl(view, url);
        } else if (formattedURL.startsWith(AuthenticationConstants.Broker.BROWSER_EXT_PREFIX)) {
            // handle external website request
        } else if (formattedURL.startsWith(AuthenticationConstants.Broker.BROWSER_EXT_INSTALL_PREFIX)) {
            // handle install request
        } else {
            // handle invalid url
        }
        return false;
    }

    private void processRedirectUrl(final WebView view, final String url) { //NOPMD
        //TODO
        throw new UnsupportedOperationException("Not implemented yet.");

    }

    void showSpinner(final boolean showSpinner) {
        // TODO
        throw new UnsupportedOperationException("Not implemented yet.");
    }
}
