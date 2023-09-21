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
import androidx.lifecycle.ViewTreeLifecycleOwner
import androidx.lifecycle.findViewTreeLifecycleOwner
import com.microsoft.identity.common.java.opentelemetry.IFidoTelemetryHelper

/**
 * Instantiates AbstractFidoChallengeHandler objects.
 */
class FidoChallengeHandlerFactory {
    companion object {
        /**
         * Creates a FidoChallengeHandler.
         * @param manager IFidoManager instance.
         * @param webView current WebView.
         * @param telemetryHelper IFidoTelemetryHelper instance.
         * @return an AbstractFidoChallengeHandler.
         */
        @JvmStatic
        fun createFidoChallengeHandler(
            manager: IFidoManager,
            webView: WebView,
            telemetryHelper: IFidoTelemetryHelper
        ): AbstractFidoChallengeHandler {
            //Once we get security key support, this is where we will prompt a user with a dialog to choose which type of credentials they want to sign in with.
            //But for now, this will always return a PassKeyFidoChallengeHandler or throw an exception.
            return PasskeyFidoChallengeHandler(
                manager,
                webView,
                ViewTreeLifecycleOwner.get(webView),
                telemetryHelper
            )
        }
    }
}
