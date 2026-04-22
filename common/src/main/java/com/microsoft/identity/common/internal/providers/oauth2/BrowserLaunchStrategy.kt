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

/**
 * Strategy interface for launching a browser-based authentication flow.
 */
internal interface BrowserLaunchStrategy {

    /**
     * Launches the browser flow for the specified package and URI.
     *
     * @param browserPackageName Browser package name selected for the auth request.
     * @param processUri URI to open in the selected browser.
     */
    fun launch(browserPackageName: String, processUri: String)

    /**
     * Indicates whether returning to [SwitchBrowserActivity] should be treated as cancellation.
     *
     * Return true for strategies where user cancellation is expected to appear as a plain
     * [SwitchBrowserActivity.onResume] callback without a completion callback/result intent.
     */
    fun handlesCancellationOnResume(): Boolean

    /**
     * Performs cleanup for any resources allocated by the strategy.
     */
    fun cleanup()
}
