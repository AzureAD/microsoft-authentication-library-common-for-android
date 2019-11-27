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
package com.microsoft.identity.common.internal.ui;

import android.content.Context;
import androidx.annotation.NonNull;

import com.microsoft.identity.common.exception.ClientException;
import com.microsoft.identity.common.exception.ErrorStrings;
import com.microsoft.identity.common.internal.logging.Logger;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationStrategy;
import com.microsoft.identity.common.internal.request.AcquireTokenOperationParameters;
import com.microsoft.identity.common.internal.request.BrokerAcquireTokenOperationParameters;
import com.microsoft.identity.common.internal.ui.browser.BrowserAuthorizationStrategy;
import com.microsoft.identity.common.internal.ui.browser.BrowserSelector;
import com.microsoft.identity.common.internal.ui.webview.EmbeddedWebViewAuthorizationStrategy;

public class AuthorizationStrategyFactory<GenericAuthorizationStrategy extends AuthorizationStrategy> {
    private static final String TAG = AuthorizationStrategyFactory.class.getSimpleName();

    private static AuthorizationStrategyFactory sInstance = null;

    public static AuthorizationStrategyFactory getInstance() {
        if (sInstance == null) {
            sInstance = new AuthorizationStrategyFactory();
        }
        return sInstance;
    }

    public GenericAuthorizationStrategy getAuthorizationStrategy(@NonNull final AcquireTokenOperationParameters parameters) {
        //Valid if available browser installed. Will fallback to embedded webView if no browser available.
        final AuthorizationAgent validatedAuthorizationAgent = validAuthorizationAgent(
                parameters.getAuthorizationAgent(),
                parameters.getActivity().getApplicationContext()
        );
        boolean isBrokerRequest = (parameters instanceof BrokerAcquireTokenOperationParameters);

        if (validatedAuthorizationAgent == AuthorizationAgent.WEBVIEW) {
            Logger.info(TAG, "Use webView for authorization.");
            return (GenericAuthorizationStrategy) (new EmbeddedWebViewAuthorizationStrategy(parameters.getActivity()));
        } else if (validatedAuthorizationAgent == AuthorizationAgent.DEFAULT) {
            // When the authorization agent is set to DEFAULT,
            // Use device browser auth flow as default.
            // Fall back to webview if no browser found.
            try {
                BrowserSelector.select(parameters.getAppContext(), parameters.getBrowserSafeList());
            } catch (final ClientException exception) {
                Logger.info(TAG, "No supported browser available found. Fallback to the webView authorization agent.");
                if (exception.getErrorCode().equalsIgnoreCase(ErrorStrings.NO_AVAILABLE_BROWSER_FOUND)) {
                    return (GenericAuthorizationStrategy) (new EmbeddedWebViewAuthorizationStrategy(parameters.getActivity()));
                }
            }
            Logger.info(TAG, "Use browser for authorization.");
            final BrowserAuthorizationStrategy browserAuthorizationStrategy = new BrowserAuthorizationStrategy(
                    parameters.getActivity(),
                    isBrokerRequest
            );
            browserAuthorizationStrategy.setBrowserSafeList(parameters.getBrowserSafeList());
            return (GenericAuthorizationStrategy) browserAuthorizationStrategy;
        } else {
            Logger.info(TAG, "Use browser for authorization.");
            final BrowserAuthorizationStrategy browserAuthorizationStrategy = new BrowserAuthorizationStrategy(
                    parameters.getActivity(),
                    isBrokerRequest
            );
            browserAuthorizationStrategy.setBrowserSafeList(parameters.getBrowserSafeList());
            return (GenericAuthorizationStrategy) browserAuthorizationStrategy;
        }
    }

    private AuthorizationAgent validAuthorizationAgent(final AuthorizationAgent agent, final Context context) {
        if (agent != AuthorizationAgent.WEBVIEW
                && BrowserSelector.getAllBrowsers(context).isEmpty()) {
            Logger.verbose(TAG, "Unable to use browser to do the authorization because "
                    + ErrorStrings.NO_AVAILABLE_BROWSER_FOUND + " Use embedded webView instead.");
            return AuthorizationAgent.WEBVIEW;
        } else {
            return agent;
        }
    }
}
