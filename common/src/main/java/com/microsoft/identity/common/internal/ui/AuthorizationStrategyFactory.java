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

import com.microsoft.identity.common.java.WarningType;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.exception.ErrorStrings;
import com.microsoft.identity.common.internal.commands.parameters.BrokerInteractiveTokenCommandParameters;
import com.microsoft.identity.common.internal.commands.parameters.InteractiveTokenCommandParameters;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationStrategy;
import com.microsoft.identity.common.internal.ui.browser.BrowserAuthorizationStrategy;
import com.microsoft.identity.common.internal.ui.browser.BrowserDescriptor;
import com.microsoft.identity.common.internal.ui.browser.BrowserSelector;
import com.microsoft.identity.common.internal.ui.webview.EmbeddedWebViewAuthorizationStrategy;
import com.microsoft.identity.common.logging.Logger;

import java.util.List;

// Suppressing rawtype warnings due to the generic types AuthorizationStrategy, AuthorizationStrategyFactory, EmbeddedWebViewAuthorizationStrategy and BrowserAuthorizationStrategy
@SuppressWarnings(WarningType.rawtype_warning)
public class AuthorizationStrategyFactory<GenericAuthorizationStrategy extends AuthorizationStrategy> {
    private static final String TAG = AuthorizationStrategyFactory.class.getSimpleName();

    private static AuthorizationStrategyFactory sInstance = null;

    public static AuthorizationStrategyFactory getInstance() {
        if (sInstance == null) {
            sInstance = new AuthorizationStrategyFactory();
        }
        return sInstance;
    }

    public GenericAuthorizationStrategy getAuthorizationStrategy(
            @NonNull final InteractiveTokenCommandParameters parameters) {
        final Context context = parameters.getAndroidApplicationContext();

        //Valid if available browser installed. Will fallback to embedded webView if no browser available.
        final AuthorizationAgent validatedAuthorizationAgent = validAuthorizationAgent(
                parameters.getAuthorizationAgent(),
                context
        );

        boolean isBrokerRequest = (parameters instanceof BrokerInteractiveTokenCommandParameters);

        if (validatedAuthorizationAgent == AuthorizationAgent.WEBVIEW) {
            Logger.info(TAG, "Use webView for authorization.");
            return getGenericAuthorizationStrategy(parameters, context);
        } else if (validatedAuthorizationAgent == AuthorizationAgent.DEFAULT) {
            // When the authorization agent is set to DEFAULT,
            // Use device browser auth flow as default.
            // Fall back to webview if no browser found.
            try {
                BrowserSelector.select(context, parameters.getBrowserSafeList());
            } catch (final ClientException exception) {
                Logger.info(TAG, "No supported browser available found. Fallback to the webView authorization agent.");
                if (ErrorStrings.NO_AVAILABLE_BROWSER_FOUND.equalsIgnoreCase(exception.getErrorCode())) {
                    return getGenericAuthorizationStrategy(parameters, context);
                }
            }
            Logger.info(TAG, "Use browser for authorization.");
            final BrowserAuthorizationStrategy browserAuthorizationStrategy = new BrowserAuthorizationStrategy(
                    context,
                    parameters.getActivity(),
                    parameters.getFragment(),
                    isBrokerRequest
            );

            // Suppressing unchecked warnings due to generic type not provided for parameters
            @SuppressWarnings(WarningType.unchecked_warning)
            List<BrowserDescriptor > browserSafeList = parameters.getBrowserSafeList();

            browserAuthorizationStrategySetBrowserSafeList(browserAuthorizationStrategy, browserSafeList);

            // Suppressing unchecked warnings due to casting of BrowserAuthorizationStrategy to GenericAuthorizationStrategy
            @SuppressWarnings(WarningType.unchecked_warning)
            GenericAuthorizationStrategy genericAuthorizationStrategy = (GenericAuthorizationStrategy) browserAuthorizationStrategy;

            return genericAuthorizationStrategy;
        } else {
            Logger.info(TAG, "Use browser for authorization.");
            final BrowserAuthorizationStrategy browserAuthorizationStrategy = new BrowserAuthorizationStrategy(
                    context,
                    parameters.getActivity(),
                    parameters.getFragment(),
                    isBrokerRequest
            );

            browserAuthorizationStrategySetBrowserSafeList(browserAuthorizationStrategy, parameters.getBrowserSafeList());

            // Suppressing unchecked warnings due to casting of BrowserAuthorizationStrategy to GenericAuthorizationStrategy
            @SuppressWarnings(WarningType.unchecked_warning)
            GenericAuthorizationStrategy genericAuthorizationStrategy = (GenericAuthorizationStrategy) browserAuthorizationStrategy;

            return genericAuthorizationStrategy;
        }
    }

    // Suppressing unchecked warnings due to casting browserSafeList generic type List<BrowserDescriptor> as generic type was not provided for browserAuthorizationStrategy.
    @SuppressWarnings(WarningType.unchecked_warning)
    private void browserAuthorizationStrategySetBrowserSafeList(BrowserAuthorizationStrategy browserAuthorizationStrategy, List<BrowserDescriptor> browserSafeList) {
        browserAuthorizationStrategy.setBrowserSafeList(browserSafeList);
    }

    // Suppressing unchecked warnings due to casting of EmbeddedWebViewAuthorizationStrategy to GenericAuthorizationStrategy
    @SuppressWarnings(WarningType.unchecked_warning)
    private GenericAuthorizationStrategy getGenericAuthorizationStrategy(@NonNull InteractiveTokenCommandParameters parameters, Context context) {
        return (GenericAuthorizationStrategy) (
                new EmbeddedWebViewAuthorizationStrategy(
                        context,
                        parameters.getActivity(),
                        parameters.getFragment()));
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
