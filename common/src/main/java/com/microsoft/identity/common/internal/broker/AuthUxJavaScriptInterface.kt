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

import android.content.Context
import android.webkit.JavascriptInterface
import com.google.gson.GsonBuilder
import com.google.gson.JsonParseException
import com.google.gson.JsonSyntaxException
import com.google.gson.stream.MalformedJsonException
import com.microsoft.identity.common.adal.internal.AuthenticationConstants
import com.microsoft.identity.common.internal.numberMatch.NumberMatchHelper
import com.microsoft.identity.common.logging.Logger
import java.net.MalformedURLException
import java.net.URL

/**
 * JavaScript API to receive JSON string payloads from AuthUX in order to facilitate calling various
 * broker methods.
 */
class AuthUxJavaScriptInterface(private val context: Context) {

    // Store number matches in a static hash map
    // No need to persist this storage beyond the current broker process, but we need to keep them
    // long enough for AuthApp to call the broker api to fetch the number match
    companion object {
        val TAG = AuthUxJavaScriptInterface::class.java.simpleName
        private const val JAVASCRIPT_INTERFACE_NAME = "broker"

        fun getInterfaceName(): String {
            return JAVASCRIPT_INTERFACE_NAME
        }

        /**
         * Helper method to determine if url is a valid Url for the JS Interface
         * @param url url being loaded
         * @return true if url is a valid, safe url, false otherwise
         */
        fun isValidUrlForInterface(urlString: String?): Boolean {
            // If url is null, return false
            if (urlString.isNullOrEmpty()) {
                return false
            }

            val url: URL
            try {
                url = URL(urlString)
            } catch (e: MalformedURLException) {
                // If url is not a valid URL, return false
                Logger.warn(TAG, "Malformed URL passed.")
                return false

            }

            val host = url.host

            // Otherwise, make sure url is a valid url
            // We only want to allow URLs that have the AAD or MSA url hosts
            return host.startsWith(AuthenticationConstants.Broker.AAD_URL_HOST_PREFIX) ||
                    host.startsWith(AuthenticationConstants.Broker.MSA_URL_HOST_PREFIX)
        }
    }

    /**
     * Method to receive a JSON string payload from AuthUX through JavaScript API.
     * Schema for the Json Payload:
     *         {
     *             "correlationID": "SOME_CORRELATION_ID" ,
     *             "action_name":"write_data",
     *             "action_component":"broker",
     *             "params":
     *             {
     *                 "function": "NUMBER_MATCH",
     *                 "data":
     *                 {
     *                     "sessionID": "$mockSessionId",
     *                     "numberMatch": "$mockNumberMatchValue"
     *                 }
     *             }
     *         }
     * TODO: This is currently the schema set for numberMatch, there may be some additions made for
     *  the more generalized JSON Schema for future Server-side to broker communication through JS.
     *
     * https://microsoft-my.sharepoint-df.com/:w:/p/veenasoman/EY1AZIeT8X5KrXVz97Vx520B3Jj0fBLSPlklnoRvcmbh0Q?e=VzNFd1&ovuser=72f988bf-86f1-41af-91ab-2d7cd011db47%2Cfadidurah%40microsoft.com&clickparams=eyJBcHBOYW1lIjoiVGVhbXMtRGVza3RvcCIsIkFwcFZlcnNpb24iOiI0OS8yNTA1MDQwMTYwOSIsIkhhc0ZlZGVyYXRlZFVzZXIiOmZhbHNlfQ%3D%3D
     */
    @JavascriptInterface
    fun receiveAuthUxMessage(jsonPayload: String) {
        val methodTag = "$TAG:receiveAuthUxMessage"
        Logger.info(methodTag, "Received a payload from AuthUX through JavaScript API.")

        try {
            val payloadObject = parseJsonToAuthUxJsonPayloadObject(jsonPayload)

            Logger.info(
                methodTag,
                "Correlation ID during JavaScript Call: [${payloadObject.correlationId}]"
            )


            // TODO: Leaving these here, as these will be relevant for next WebCP feature
            // val actionName = payloadObject.actionName
            // val actionComponent = payloadObject.actionComponent

            val parameters = payloadObject.params
            if (parameters == null) {
                Logger.warn(methodTag, "Payload from AuthUX contained no \"params\" field.")
                return
            }

            val operation = parameters.operation

            Logger.info(methodTag, "Function name: [$operation]")

            when (operation) {
                OperationNames.NUMBER_MATCHING ->
                    NumberMatchHelper.storeNumberMatch(
                        context,
                        parameters.sessionId,
                        parameters.codeMatch
                    )

                else ->
                    Logger.warn(
                        methodTag,
                        "Payload from AuthUX contained an unknown function name."
                    )
            }
        } catch (e: Exception) { // If we run into exceptions, we don't want to kill the broker
            when (e) {
                is NullPointerException -> {
                    Logger.error(
                        methodTag,
                        "Payload with missing mandatory fields sent through JavaScriptInterface",
                        e
                    )
                }

                is MalformedJsonException, is JsonSyntaxException, is JsonParseException -> {
                    Logger.error(
                        methodTag,
                        "Error Parsing JSON payload sent through JavaScriptInterface",
                        e
                    )
                }

                else -> {
                    Logger.error(
                        methodTag,
                        "Unknown error occurred while processing the payload.",
                        e
                    )
                }
            }
        }
    }

    private fun parseJsonToAuthUxJsonPayloadObject(jsonString: String): AuthUxJsonPayload {
        val gson = GsonBuilder()
            .registerTypeAdapter(AuthUxJsonPayload::class.java, AuthUxJsonPayloadKTDeserializer())
            .create()
        return gson.fromJson(jsonString, AuthUxJsonPayload::class.java)
    }

    /**
     * Enum class representing the operation names that can be called from AuthUX.
     */
    object OperationNames {
        const val NUMBER_MATCHING = "number_matching"
    }
}
