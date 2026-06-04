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
package com.microsoft.identity.common.internal.providers.oauth2

import android.content.Context
import android.content.Intent
import com.microsoft.identity.common.java.ui.AuthorizationAgent

/**
 * Parameters for the authorization activity.
 *
 * @param context                    Android application context
 * @param authIntent                 Android intent used by the authorization activity to launch the specific implementation of authorization (BROWSER, EMBEDDED)
 * @param requestUrl                 The authorization request in URL format
 * @param redirectUri                The expected redirect URI associated with the authorization request
 * @param requestHeader              Additional HTTP headers included with the authorization request
 * @param authorizationAgent         The means by which authorization should be performed (EMBEDDED, WEBVIEW) NOTE: This should move to library configuration
 * @param webViewZoomEnabled         This parameter is specific to embedded and controls whether webview zoom is enabled... NOTE: Needs refactoring
 * @param webViewZoomControlsEnabled This parameter is specific to embedded and controls whether webview zoom controls are enabled... NOTE: Needs refactoring
 * @param sourceLibraryName          Product name to be of library making the request
 * @param sourceLibraryVersion       Product version to be of library making the request
 * @param utid                       The tenant unique id, if applicable
 * @param webViewEnableSilentAuthorizationFlowTimeOutMs                 If set to a non-null value, this indicates that the flow is silent and specifies the timeout for the silent authorization flow in milliseconds.
 * @param isWebViewWebCpEnabled        This parameter controls whether webcp URLs should be handled within the WebView or redirected to external browser
 * @param enableSwitchBrowser       When true, the factory will check for browser availability and append switch_browser=1 to the request URL to opt-in to the Switch Browser protocol
 */
data class AuthorizationActivityParameters @JvmOverloads constructor(
    val context: Context,
    val authIntent: Intent?,
    val requestUrl: String,
    val redirectUri: String,
    val requestHeader: HashMap<String, String>?,
    val authorizationAgent: AuthorizationAgent,
    val webViewZoomEnabled: Boolean = true,
    val webViewZoomControlsEnabled: Boolean = true,
    val sourceLibraryName: String? = null,
    val sourceLibraryVersion: String? = null,
    /**
     * The tenant unique id
     */
    val utid: String? = null,
    val webViewEnableSilentAuthorizationFlowTimeOutMs: Long? = null,
    val isWebViewWebCpEnabled: Boolean = false,
    val enableSwitchBrowser: Boolean = false,
)
