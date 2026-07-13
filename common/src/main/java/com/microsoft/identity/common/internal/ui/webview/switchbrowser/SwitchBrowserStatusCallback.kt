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

import android.net.Uri

/**
 * Status of the switch_browser flow, reported by [SwitchBrowserProtocolCoordinator]. The host
 * (e.g. `WebViewAuthorizationFragment`) implements this and reacts; several events may drive the
 * same reaction. All methods are invoked on the main thread.
 */
interface SwitchBrowserStatusCallback {

    /** WebView -> browser hop began */
    fun onSwitchBrowserStarted()

    /** browser -> WebView hop began on resume */
    fun onSwitchBrowserResumed()

    /** Resume produced the URI/headers to continue the flow in the WebView. */
    fun onSwitchBrowserCompleted(uri: Uri, headers: HashMap<String, String>)

    /** A hop failed; [error] is the terminal failure to surface. */
    fun onSwitchBrowserFailed(error: Throwable)
}
