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

    // Store number matches in a static hash map
    // No need to persist this storage beyond the current broker process, but we need to keep them
    // long enough for AuthApp to call the broker api to fetch the number match
    companion object {
        val TAG = NumberMatchHelper::class.java.simpleName
        val numberMatchMap: HashMap<String, String> = HashMap()
        const val SESSION_ID_ATTRIBUTE_NAME = "sessionID"
        const val NUMBER_MATCH_ATTRIBUTE_NAME = "numberMatch"

        /**
         * Method to add a key:value pair of sessionID:numberMatch to static hashmap. This hashmap will be accessed
         * by broker api to get the number match for a particular sessionID.
         */
        fun storeNumberMatch(sessionID:String?, numberMatch:String?) {
            // If both parameters are non-null, add a new entry to the hashmap
            if (sessionID != null && numberMatch != null) {
                val methodTag = "$TAG:storeNumberMatch"
                Logger.info(
                    methodTag,
                    "Adding entry in NumberMatch hashmap for session ID: $sessionID"
                )
                numberMatchMap[sessionID] = numberMatch
            }

            // If either parameter is null, do nothing
        }

        /**
         * Clear existing number match key:value pairs
         */
        fun clearNumberMatchMap() {
            numberMatchMap.clear()
        }
    }
}
