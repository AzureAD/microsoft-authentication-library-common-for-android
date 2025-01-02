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
package com.microsoft.identity.common.internal.ui.webview.challengehandlers

import android.content.Context
import android.content.Intent
import com.microsoft.identity.common.internal.ui.browser.CustomTabsManager
import com.microsoft.identity.common.java.browser.Browser
import com.microsoft.identity.common.logging.Logger

class SwitchBrowserHandler(
    private val context: Context,
    private val customTabsManager: CustomTabsManager,
    private val browser: Browser
) : IChallengeHandler<SwitchBrowserChallenge, Boolean>  {


    companion object {
        private val TAG = SwitchBrowserHandler::class.simpleName
    }

    /**
     * Process difference kinds of challenge request.
     *
     * @param challenge challenge request
     * @return GenericResponse
     */
    override fun processChallenge(challenge: SwitchBrowserChallenge): Boolean {
        val methodTag = "$TAG:processChallenge"
        val browserIntent: Intent
        if (browser.isCustomTabsServiceSupported) {
            Logger.info(methodTag, "CustomTabsService is supported.")
            //create customTabsIntent
            if (!customTabsManager.bind(context, browser.packageName)) {
                Logger.warn(methodTag, "Failed to bind CustomTabsService.")
                browserIntent = Intent(Intent.ACTION_VIEW)
            } else {
                browserIntent = customTabsManager.customTabsIntent.intent
            }
        } else {
            Logger.warn(methodTag, "CustomTabsService is NOT supported")
            browserIntent = Intent(Intent.ACTION_VIEW)
        }
        browserIntent.setPackage(browser.packageName)
        browserIntent.setData(challenge.uri)
        browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(browserIntent)
        return true
    }


    //if (mCustomTabManager != null) {
   //     mCustomTabManager.unbind();
   // }
}