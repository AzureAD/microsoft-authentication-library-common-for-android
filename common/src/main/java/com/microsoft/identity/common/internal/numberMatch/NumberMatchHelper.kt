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
package com.microsoft.identity.common.internal.numberMatch

import com.microsoft.identity.common.logging.Logger

/**
 * Helper to facilitate NumberMatchFlow. Used in conjunction with {@link AuthUxJavaScriptInterface}
 * When authenticator is installed, and phone uses MFA or PSI in an interactive flow, a number
 * matching challenge is issued, where used is given a number and asked to open authenticator and check
 * for the same number in authenticator UI. This feature cuts out one UI step, where this API is used to
 * supply the number match value and store it in ephemeral storage (kept as long as current broker
 * process is alive), where Authenticator can call a broker API to fetch the number match, and immediately
 * prompt user for consent, rather than first asking them to check the number match.
 */
class NumberMatchHelper {

    // Store number matches in a Content Provider
    companion object {
        val TAG = NumberMatchHelper::class.java.simpleName
        val numberMatchMap: HashMap<String, String> = HashMap()

        /**
         * Method to add a key:value pair of sessionID:numberMatch to Content Provider.
         */
        fun storeNumberMatch(sessionId: String?, numberMatch: String?) {
            val methodTag = "$TAG:storeNumberMatch"
            Logger.info(methodTag,
                "Adding entry in NumberMatch Content Provider for session ID: $sessionId")

            // If both parameters are non-null, add a new entry to the hashmap
            if (sessionId != null && numberMatch != null) {
                numberMatchMap[sessionId] = numberMatch
            } else {
                Logger.warn(methodTag,
                    "Either session ID or number match is null. Nothing to add for number match."
                )
            }
        }

        /**
         * Clear all number match data from ContentProvider.
         */
        fun clearNumberMatchMap() {
            numberMatchMap.clear()
        }
    }
}
