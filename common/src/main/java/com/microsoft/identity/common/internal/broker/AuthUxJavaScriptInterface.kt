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
package com.microsoft.identity.common.internal.broker

import android.webkit.JavascriptInterface
import com.google.gson.stream.MalformedJsonException
import com.microsoft.identity.common.internal.numberMatch.NumberMatchHelper
import com.microsoft.identity.common.java.util.JsonUtil
import com.microsoft.identity.common.logging.Logger

/**
 * JavaScript API to receive JSON string payloads from AuthUX in order to facilitate calling various
 * broker methods.
 */
class AuthUxJavaScriptInterface {

    // Store number matches in a static hash map
    // No need to persist this storage beyond the current broker process, but we need to keep them
    // long enough for AuthApp to call the broker api to fetch the number match
    companion object {
        val TAG = AuthUxJavaScriptInterface::class.java.simpleName
        private const val JAVASCRIPT_INTERFACE_NAME = "ClientBrokerJS"

        fun getInterfaceName() : String {
            return JAVASCRIPT_INTERFACE_NAME
        }
    }

    @JavascriptInterface
    fun postMessageToBroker(jsonPayload: String) {
        val methodTag = "$TAG:postMessageToBroker"
        Logger.info(methodTag, "Received a payload from AuthUX through JavaScript API.")

        try {
            val parsedJson = JsonUtil.extractJsonObjectIntoMap(jsonPayload)

            val correlationID = parsedJson["correlationID"]
            Logger.info(methodTag, "Correlation ID during JavaScript Call: [$correlationID]")

            // TODO: Leaving these here, as these will be relevant for next WebCP feature
            // val actionName = parsedJson["action_name"]
            // val actionComponent = parsedJson["action_component"]

            val parameters = JsonUtil.extractJsonObjectIntoMap(parsedJson["params"])
            val function = parameters["function"]
            val data = JsonUtil.extractJsonObjectIntoMap(parameters["data"])
            Logger.info(methodTag, "Function name: [$function]")

            when (function) {
                FunctionNames.NUMBER_MATCH.name ->
                    NumberMatchHelper.storeNumberMatch(
                        data[NumberMatchHelper.SESSION_ID_ATTRIBUTE_NAME],
                        data[NumberMatchHelper.NUMBER_MATCH_ATTRIBUTE_NAME])
                else ->
                    Logger.warn(methodTag, "Payload from AuthUX contained an unknown function name.")
            }
        } catch (e: Exception) { // If we run into exceptions, we don't want to kill the broker
            when (e) {
                is NullPointerException -> {
                    Logger.warn(methodTag, "Payload with missing mandatory fields sent through JavaScriptInterface")
                }
                is MalformedJsonException -> {
                    Logger.warn(methodTag, "Malformed JSON payload sent through JavaScriptInterface")
                }
                else -> {
                    Logger.warn(methodTag, "Unknown error occurred while processing the payload.")
                }
            }
        }
    }

    /**
     * Enum class to hold function names
     */
    enum class FunctionNames {
        NUMBER_MATCH
    }
}
