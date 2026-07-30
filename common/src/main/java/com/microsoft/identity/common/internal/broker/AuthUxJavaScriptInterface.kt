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
import com.google.gson.GsonBuilder
import com.google.gson.JsonParseException
import com.google.gson.JsonSyntaxException
import com.google.gson.stream.MalformedJsonException
import com.microsoft.identity.common.adal.internal.AuthenticationConstants
import com.microsoft.identity.common.internal.numberMatch.NumberMatchHelper
import com.microsoft.identity.common.java.opentelemetry.AttributeName
import com.microsoft.identity.common.java.opentelemetry.SpanExtension
import com.microsoft.identity.common.logging.Logger
import java.net.URI
import java.net.URISyntaxException

/**
 * Callback seam used to forward an opaque Auth UX telemetry signal (a server error code) received
 * over the JS bridge to the onboarding telemetry sink.
 *
 * Kept deliberately minimal and decoupled from the concrete telemetry recorder so this bridge does
 * not take a dependency on the onboarding recorder plumbing. The concrete wiring — resolving the
 * active [com.microsoft.identity.common.internal.telemetry.OnboardingTelemetryRecorder] and
 * appending the code to the onboarding blob's blocking-errors list (subject to the non-blocking
 * exclusion list) — is supplied by the host and handled downstream (see AB#3688632).
 */
fun interface AuthUxTelemetrySink {
    /**
     * Route an opaque Auth UX server error code to onboarding telemetry.
     *
     * @param errorCode The server-emitted error code (e.g. an STS error code such as "530003"),
     *  treated as an opaque telemetry value. Guaranteed non-empty by the caller.
     */
    fun onAuthUxServerError(errorCode: String)
}

/**
 * JavaScript API to receive JSON string payloads from AuthUX in order to facilitate calling various
 * broker methods.
 *
 * @property telemetrySink Optional sink invoked for the non-mutating
 *  [ActionNames.LOG_TELEMETRY] action to forward an opaque Auth UX server error code to
 *  onboarding telemetry. When null (the default), `log_telemetry` messages are still parsed and
 *  validated but produce no telemetry side effect. Supplying a default keeps existing no-arg
 *  construction (e.g. from the WebView host) source-compatible.
 */
class AuthUxJavaScriptInterface @JvmOverloads constructor(
    private val telemetrySink: AuthUxTelemetrySink? = null
) {

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
         * Helper method to determine if uri is a valid Uri for the JS Interface
         * @param uriString uri being loaded
         * @return true if uri is a valid, safe uri, false otherwise
         */
        fun isValidUriForInterface(uriString: String?): Boolean {
            // If uri is null or empty, return false
            if (uriString.isNullOrEmpty()) {
                return false
            }

            val uri: URI
            try {
                uri = URI(uriString)
            } catch (e: URISyntaxException) {
                Logger.warn(TAG, "URISyntaxException received. uri: $uriString, Message: ${e.message}")
                return false
            }

            val host = uri.host

            // A scheme-only URI (e.g. "openid-vc://?request_uri=...") has a null host. Such URIs
            // are not AAD interface URIs, so reject them instead of throwing on a null host.
            if (host == null) {
                return false
            }

            // Otherwise, make sure uri is a valid uri
            // We only want to allow URIs that have the AAD uri hosts
            return host.endsWith(AuthenticationConstants.Broker.AAD_GLOBAL_URL_HOST_SUFFIX) ||
                    host.endsWith(AuthenticationConstants.Broker.AAD_INTUNE_MDM_URL_HOST_SUFFIX) ||
                    host.endsWith(AuthenticationConstants.Broker.AAD_US_URL_HOST_SUFFIX) ||
                    host.endsWith(AuthenticationConstants.Broker.AAD_CHINA_URL_HOST_SUFFIX)
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

            val span = SpanExtension.current()
            val actionName = payloadObject.actionName
            span.setAttribute(AttributeName.authux_js_action_name.name, actionName)
            val actionComponent = payloadObject.actionComponent
            span.setAttribute(AttributeName.authux_js_action_component.name, actionComponent)

            val parameters = payloadObject.params
            if (parameters == null) {
                Logger.warn(methodTag, "Payload from AuthUX contained no \"params\" field.")
                return
            }

            val operation = parameters.operation
            if (operation != null) {
                span.setAttribute(AttributeName.authux_js_operation.name, operation)
            }

            Logger.info(methodTag, "Action name: [$actionName], operation: [$operation]")

            when {
                operation == OperationNames.NUMBER_MATCHING ->
                    NumberMatchHelper.storeNumberMatch(
                        parameters.sessionId,
                        parameters.codeMatch
                    )

                actionName == ActionNames.LOG_TELEMETRY -> {
                    // Dedicated, non-mutating telemetry path (H3): dispatched by action_name (the
                    // log_telemetry action carries no params.operation) and must never touch the
                    // number-match / write_data device-store path. The append to the onboarding
                    // blob (with the non-blocking exclusion list) is handled downstream by the
                    // supplied sink — see AB#3688632.
                    val errorCode = parameters.errorCode
                    if (errorCode.isNullOrEmpty()) {
                        Logger.warn(
                            methodTag,
                            "log_telemetry payload contained no \"errorCode\"; ignoring (no-op)."
                        )
                    } else {
                        telemetrySink?.onAuthUxServerError(errorCode)
                    }
                }

                else ->
                    Logger.warn(
                        methodTag,
                        "Payload from AuthUX contained an unknown action/operation."
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
     * Operation names dispatched via the `params.operation` field (number-match / `write_data` path).
     */
    object OperationNames {
        const val NUMBER_MATCHING = "number_matching"
    }

    /**
     * Top-level `action_name` values dispatched directly (independent of `params.operation`).
     */
    object ActionNames {
        const val LOG_TELEMETRY = "log_telemetry"
    }
}
