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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.microsoft.identity.common.java.providers.RawAuthorizationResult
import com.microsoft.identity.common.logging.Logger
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SilentWebViewAuthorizationFragment : WebViewAuthorizationFragment() {

    companion object {
        private const val TAG = "SilentWebViewAuthorizationFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? = super.onCreateView(inflater, container, savedInstanceState)?.apply {
        visibility = View.GONE
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        cancelAuthorizationOnTimeOut(5000)
    }

    /**
     * Cancels the authorization flow after a specified timeout.
     * This is useful for silent flows where we want to avoid waiting indefinitely.
     * Tied to fragment lifecycle to ensure it is cancelled if the fragment is no longer visible.
     *
     * @param timeOutInMs The timeout duration in milliseconds.
     */
    private fun cancelAuthorizationOnTimeOut(timeOutInMs : Long) = viewLifecycleOwner.lifecycleScope.launch{
        val methodTag = "$TAG:cancel"
        delay(timeOutInMs)
        Logger.info(methodTag, "Received Authorization flow cancel request from SDK")
        sendResult(RawAuthorizationResult.ResultCode.TIMED_OUT)
        finish()
    }
}