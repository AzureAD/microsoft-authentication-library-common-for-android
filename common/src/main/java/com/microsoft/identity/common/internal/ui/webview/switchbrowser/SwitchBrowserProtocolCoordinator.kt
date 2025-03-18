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
package com.microsoft.identity.common.internal.ui.webview.switchbrowser

import android.app.Activity
import android.net.Uri
import android.os.Bundle
import com.microsoft.identity.common.adal.internal.AuthenticationConstants.SWITCH_BROWSER
import com.microsoft.identity.common.internal.ui.webview.challengehandlers.SwitchBrowserRequestHandler
import com.microsoft.identity.common.internal.ui.webview.switchbrowser.SwitchBrowserUriHelper.buildResumeUri
import com.microsoft.identity.common.java.AuthenticationConstants.AAD.AUTHORIZATION
import com.microsoft.identity.common.java.exception.ClientException
import com.microsoft.identity.common.logging.Logger

/**
 * SwitchBrowserProtocolCoordinator is responsible for coordinating the switch browser protocol.
 * Contains the handler to process the switch browser request and resume action.
 */
class SwitchBrowserProtocolCoordinator(
    val switchBrowserRequestHandler: SwitchBrowserRequestHandler) {
    constructor(activity: Activity) : this(SwitchBrowserRequestHandler(activity))

    companion object {
        private const val TAG = "SwitchBrowserProtocolCoordinator"
    }

    /**
     * Processes the switch browser resume action.
     *
     * @param extras The bundle containing the switch browser action URI and authorization code.
     * @param onSuccessAction The action to perform on success.
     *
     * The [onSuccessAction] function takes two parameters: the resume URL and the headers.
     * In this case, [onSuccessAction] will launch the WebView with the provided resume URI and headers.
     */
    @Throws(ClientException::class)
    fun processSwitchBrowserResume(
        extras: Bundle,
        onSuccessAction: (Uri, HashMap<String, String>) -> Unit
    ) {
        val methodTag = "$TAG:processSwitchBrowserResume"
        val actionUri = extras.getString(SWITCH_BROWSER.ACTION_URI)
        val code = extras.getString(SWITCH_BROWSER.CODE)
        if (actionUri.isNullOrEmpty() || code.isNullOrEmpty()) {
            throw ClientException(
                ClientException.MISSING_PARAMETER,
                "Action URI is null/empty: ${actionUri == null}, code is null/empty: ${code == null}"
            )
        }
        val resumeUri = buildResumeUri(actionUri)
        val headers = hashMapOf(AUTHORIZATION to code)
        onSuccessAction(resumeUri, headers)
        // Reset the challenge state after processing the resume action
        switchBrowserRequestHandler.resetChallengeState()
        Logger.info(methodTag, "Switch browser resume action processed successfully.")
    }

    /**
     * Check if the handler processed a switch browser request.
     * if so, it means we are resuming the switch browser flow.
     *
     * @return boolean
     */
    fun isExpectingSwitchBrowserResume(): Boolean {
        val methodTag = "$TAG:isExpectingSwitchBrowserResume"
        Logger.verbose(methodTag, " expectingRequest: ${switchBrowserRequestHandler.isChallengeHandled}")
        return switchBrowserRequestHandler.isChallengeHandled
    }
}
