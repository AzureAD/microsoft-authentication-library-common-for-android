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

import android.app.Activity;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.microsoft.identity.common.internal.ui.browser.BrowserSelector;
import com.microsoft.identity.common.java.browser.Browser;
import com.microsoft.identity.common.internal.ui.browser.DefaultBrowserAuthorizationStrategy;
import com.microsoft.identity.common.java.WarningType;
import com.microsoft.identity.common.java.commands.parameters.BrokerInteractiveTokenCommandParameters;
import com.microsoft.identity.common.java.commands.parameters.InteractiveTokenCommandParameters;
import com.microsoft.identity.common.java.configuration.LibraryConfiguration;
import com.microsoft.identity.common.java.providers.oauth2.IAuthorizationStrategy;
import com.microsoft.identity.common.internal.ui.webview.EmbeddedWebViewAuthorizationStrategy;
import com.microsoft.identity.common.java.ui.AuthorizationAgent;
import com.microsoft.identity.common.logging.Logger;
import com.microsoft.identity.common.java.strategies.IAuthorizationStrategyFactory;


import lombok.Builder;
import lombok.experimental.Accessors;

// Suppressing rawtype warnings due to the generic types AuthorizationStrategy, AuthorizationStrategyFactory, EmbeddedWebViewAuthorizationStrategy and BrowserAuthorizationStrategy
@SuppressWarnings(WarningType.rawtype_warning)
@Builder
@Accessors(prefix = "m")
public class AndroidAuthorizationStrategyFactory implements IAuthorizationStrategyFactory<IAuthorizationStrategy> {
    private static final String TAG = AndroidAuthorizationStrategyFactory.class.getSimpleName();

    private final Context mContext;
    private final Activity mActivity;
    private final Fragment mFragment;

    /**
     * Get the authorization strategy.
     *
     * @param parameters The parameters for the interactive token command.
     *                   authorizationAgent        The authorization agent provided by the caller.
     *                   browser                   The browser to use for authorization.
     *                   isBrokerRequest           True if the request is from browser.
     * @return The authorization strategy.
     */
    @Override
    @NonNull
    public IAuthorizationStrategy getAuthorizationStrategy(@NonNull final InteractiveTokenCommandParameters parameters) {
        final String methodTag = TAG + ":getAuthorizationStrategy";
        final boolean isBrokerRequest = parameters instanceof BrokerInteractiveTokenCommandParameters;
        final Browser browser = new BrowserSelector(mContext).select(
                parameters.getBrowserSafeList(),
                parameters.getPreferredBrowser()
        );

        // Use embedded webView if no browser available or authorization agent is webView
        if (authorizationAgent == AuthorizationAgent.WEBVIEW || browser == null) {
            Logger.info(methodTag, "WebView authorization, browser: " + browser);
            return getGenericAuthorizationStrategy();
        }

        Logger.info(methodTag, "Browser authorization, browser: " + browser);
        return getBrowserAuthorizationStrategy(browser, isBrowserRequest);
    }

    /**
     * Get current task browser authorization strategy or default browser authorization strategy.
     * If the authorization is in current task, use current task browser authorization strategy.
     *
     * @param browser         The browser to use for authorization.
     * @param isBrokerRequest True if the request is from broker.
     * @return The browser authorization strategy.
     */
    private IAuthorizationStrategy getBrowserAuthorizationStrategy(@NonNull final Browser browser,
                                                                   final boolean isBrokerRequest) {
        if (LibraryConfiguration.getInstance().isAuthorizationInCurrentTask()) {
            return new CurrentTaskBrowserAuthorizationStrategy(
                    mContext,
                    mActivity,
                    mFragment,
                    browser);
        } else {
            return new DefaultBrowserAuthorizationStrategy(
                    mContext,
                    mActivity,
                    mFragment,
                    isBrokerRequest,
                    browser
            );
        }
    }

    /**
     * Get the generic authorization strategy.
     *
     * @return The embedded web view authorization strategy.
     */
    private IAuthorizationStrategy getGenericAuthorizationStrategy() {
        return new EmbeddedWebViewAuthorizationStrategy(mContext, mActivity, mFragment);
    }
}
