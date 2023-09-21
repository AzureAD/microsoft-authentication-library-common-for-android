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
package com.microsoft.identity.common.internal.fido

import android.webkit.WebView
import androidx.test.core.app.ApplicationProvider
import com.microsoft.identity.common.java.constants.FidoConstants

class ExtendedTestWebView : WebView(ApplicationProvider.getApplicationContext()) {
    var urlLoaded = false
    var headers : MutableMap<String, String>? = null

    override fun post(action: Runnable?): Boolean {
        //Running the action right away instead of putting in a message queue.
        action?.run()
        return true
    }

    override fun loadUrl(url: String, additionalHttpHeaders: MutableMap<String, String>) {
        urlLoaded = true
        headers = additionalHttpHeaders
    }

    fun isRegularAssertion() : Boolean {
        headers?.let {
            val assertion = it[FidoConstants.PASSKEY_RESPONSE_ASSERTION_HEADER]
                ?: throw Exception("No assertion header found.")
            return assertion == TestFidoManager.SAMPLE_ASSERTION
        }
        throw Exception("Headers is null.")
    }

    fun hasContext() : Boolean {
        headers?.let {
            val context = it[FidoConstants.PASSKEY_RESPONSE_CONTEXT_HEADER]
                ?: throw Exception("No context header found.")
            return context.isNotEmpty()
        }
        throw Exception("Headers is null.")
    }

    fun hasFlowToken() : Boolean {
        headers?.let {
            val flowToken = it[FidoConstants.PASSKEY_RESPONSE_FLOWTOKEN_HEADER]
                ?: throw Exception("No flow token header found.")
            return flowToken.isNotEmpty()
        }
        throw Exception("Headers is null.")
    }
}
