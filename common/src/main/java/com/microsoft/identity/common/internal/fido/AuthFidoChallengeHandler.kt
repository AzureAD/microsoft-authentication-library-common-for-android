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
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.microsoft.identity.common.internal.ui.webview.challengehandlers.IChallengeHandler
import com.microsoft.identity.common.java.constants.FidoConstants
import com.microsoft.identity.common.java.exception.ClientException
import com.microsoft.identity.common.java.opentelemetry.AttributeName
import com.microsoft.identity.common.java.opentelemetry.OTelUtility
import com.microsoft.identity.common.java.opentelemetry.SpanName
import com.microsoft.identity.common.logging.Logger
import io.opentelemetry.api.trace.SpanContext
import io.opentelemetry.api.trace.StatusCode
import kotlinx.coroutines.launch
import java.util.*
import java.net.MalformedURLException
import java.net.URL

/**
 * Handles a FidoChallenge by either creating or authenticating with a passkey.
 */
class AuthFidoChallengeHandler
/**
 * Creates an AuthFidoChallengeHandler instance.
 * @param fidoManager IFidoManager instance.
 * @param webView Current WebView.
 * @param spanContext Current spanContext, if present.
 * @param lifecycleOwner instance to get coroutine scope from.
 */(
    private val fidoManager: IFidoManager,
    private val webView: WebView,
    private val spanContext : SpanContext?,
    private val lifecycleOwner: LifecycleOwner?
) : IChallengeHandler<FidoChallenge, Void> {
    val TAG = AuthFidoChallengeHandler::class.simpleName.toString()
    val span = if (spanContext != null) {
        OTelUtility.createSpanFromParent(SpanName.Fido.name, spanContext)
    } else {
        OTelUtility.createSpan(SpanName.Fido.name)
    }

    override fun processChallenge(fidoChallenge: FidoChallenge): Void? {
        val methodTag = "$TAG:processChallenge"
        span.setAttribute(
            AttributeName.fido_challenge.name,
            fidoChallenge::class.simpleName.toString()
        );
        // First verify submitUrl and context. Without these two, we can't respond back to the server.
        // If either one of these are missing or malformed, throw an exception and let the main WebViewClient handle it.
        val submitUrl = validateSubmitUrl(fidoChallenge.submitUrl)
        val context = validateRequiredParameter(
            FidoRequestField.CONTEXT.fieldName,
            fidoChallenge.context
        )
        val authChallenge: String
        val relyingPartyIdentifier: String
        val userVerificationPolicy: String
        val allowedCredentials: List<String>?
        try {
            authChallenge = validateRequiredParameter(
                FidoRequestField.CHALLENGE.fieldName,
                fidoChallenge.challenge
            )
            relyingPartyIdentifier = validateRequiredParameter(
                FidoRequestField.RELYING_PARTY_IDENTIFIER.fieldName,
                fidoChallenge.relyingPartyIdentifier
            )
            userVerificationPolicy = validateRequiredParameter(
                FidoRequestField.USER_VERIFICATION_POLICY.fieldName,
                fidoChallenge.userVerificationPolicy
            )
            allowedCredentials = validateOptionalListParameter(
                AuthFidoRequestField.ALLOWED_CREDENTIALS.fieldName,
                fidoChallenge.allowedCredentials
            )
            validateProtocolVersion(fidoChallenge.version)
            //Not currently using keyTypes, but should validate in case we use it in the future.
            validateOptionalListParameter(
                AuthFidoRequestField.KEY_TYPES.fieldName,
                fidoChallenge.keyTypes
            )
        } catch (e : Exception) {
            respondToChallengeWithError(
                submitUrl = submitUrl,
                context = context,
                errorMessage = e.message.toString(),
                exception = e,
                methodTag = methodTag
            )
            return null
        }
        if (lifecycleOwner == null) {
            respondToChallengeWithError(
                submitUrl = submitUrl,
                context = context,
                errorMessage = "Cannot get lifecycle owner needed for FIDO API calls.",
                methodTag = methodTag
            )
            return null
        }
        lifecycleOwner.lifecycleScope.launch {
            try {
                val assertion = fidoManager.authenticate(
                    challenge = authChallenge,
                    relyingPartyIdentifier = relyingPartyIdentifier,
                    allowedCredentials = allowedCredentials,
                    userVerificationPolicy = userVerificationPolicy
                )
                span.setStatus(StatusCode.OK)
                respondToChallenge(
                    submitUrl = submitUrl,
                    assertion = assertion,
                    context = context
                )
            } catch (e : Exception) {
                respondToChallengeWithError(
                    submitUrl = submitUrl,
                    context = context,
                    errorMessage = e.message.toString(),
                    exception = e,
                    methodTag = methodTag
                )
            }
        }
        return null
    }

    /**
     * Validates that the given required parameter is not null or empty.
     * @param field passkey protocol field
     * @param value value for a passkey protocol parameter
     * @return validated parameter value
     * @throws ClientException if the parameter is null or empty.
     */
    @Throws(ClientException::class)
    fun validateRequiredParameter(field: String, value: String?): String {
        if (value == null) {
            throw ClientException(ClientException.PASSKEY_PROTOCOL_REQUEST_PARSING_ERROR, "$field not provided")
        } else if (value.isBlank()) {
            throw ClientException(ClientException.PASSKEY_PROTOCOL_REQUEST_PARSING_ERROR, "$field is empty")
        }
        return value
    }

    /**
     * Validates that the submitUrl parameter is not null, empty, or malformed.
     * @param value value for the submitUrl passkey protocol parameter.
     * @return validated parameter value
     * @throws ClientException if the parameter is null, empty, or malformed.
     */
    @Throws(ClientException::class)
    fun validateSubmitUrl(value: String?): String {
        val submitUrl = validateRequiredParameter(FidoRequestField.SUBMIT_URL.fieldName, value)
        try {
            URL(submitUrl)
        } catch (e : MalformedURLException) {
            throw ClientException(ClientException.PASSKEY_PROTOCOL_REQUEST_PARSING_ERROR, "${FidoRequestField.SUBMIT_URL.fieldName} value is malformed.")
        }
        return submitUrl
    }

    /**
     * Validates that the protocol version is not null or empty, and is a version that we currently support.
     * @param value value for the version passkey protocol parameter.
     * @return validated parameter value
     * @throws ClientException if the parameter is null, empty, or an unsupported version.
     */
    @Throws(ClientException::class)
    fun validateProtocolVersion(value: String?): String {
        val version = validateRequiredParameter(FidoRequestField.VERSION.fieldName, value)
        if (version == FidoConstants.PASSKEY_PROTOCOL_VERSION) {
            return version
        }
        throw ClientException(ClientException.PASSKEY_PROTOCOL_REQUEST_PARSING_ERROR, "Provided protocol version is not currently supported.")
    }

    /**
     * Validates that the given optional parameter is not empty and turns into a list.
     * @param value value for a passkey protocol parameter
     * @param field passkey protocol field
     * @return validated parameter value, or null if not provided.
     * @throws ClientException if the parameter is empty
     */
    @Throws(ClientException::class)
    fun validateOptionalListParameter(field: String, value: List<String>?): List<String>? {
        if (value != null && value.isEmpty()) {
            throw ClientException(ClientException.PASSKEY_PROTOCOL_REQUEST_PARSING_ERROR, "$field is empty")
        }
        return value
    }

    /**
     * Makes a post request in the WebView with the submitUrl and headers.
     * @param submitUrl The url to which the client submits the response to the server's challenge.
     * @param context Server state that needs to be maintained between challenge and response.
     * @param assertion string representing response with signed challenge.
     */
    fun respondToChallenge(submitUrl: String,
                           assertion: String,
                           context: String) {
        val methodTag = "$TAG:respondToChallenge"
        span.end()
        //We're splitting the context value here because ESTS is expected to also send the flow token in the same string.
        //They want us to send the flow token value via a header separate from the actual context value.
        val splitContextList = context.split(FidoConstants.PASSKEY_CONTEXT_DELIMITER)
        val actualContext: String
        val flowToken: String
        if (splitContextList.size == 2) {
            actualContext = splitContextList[0]
            flowToken = splitContextList[1]
        } else {
            //Put everything under the actual context header.
            actualContext = splitContextList[0]
            flowToken = ""
        }
        val header = mapOf(
            FidoConstants.PASSKEY_RESPONSE_ASSERTION_HEADER to assertion,
            FidoConstants.PASSKEY_RESPONSE_CONTEXT_HEADER to actualContext,
            FidoConstants.PASSKEY_RESPONSE_FLOWTOKEN_HEADER to flowToken
        )
        webView.post {
            Logger.info(methodTag, "Responding to Fido challenge.")
            webView.loadUrl(submitUrl + "&dc=ESTS-PUB-SCUS-LZ1-FD000-TEST1&fidotest=true", header)
        }
    }

    /**
     * Helper method to respond to the server with an error.
     * @param submitUrl The url to which the client submits the response to the server's challenge.
     * @param context Server state that needs to be maintained between challenge and response.
     * @param errorMessage Error message string.
     * @param exception Exception associated with error. Default null.
     * @param assertion String representing response with signed challenge.
     */
    fun respondToChallengeWithError(submitUrl: String,
                                    context: String,
                                    errorMessage: String,
                                    exception: Exception? = null,
                                    methodTag: String? = "$TAG:respondToChallengeWithError"
    ) {
        Logger.error(methodTag, errorMessage, exception)
        if (exception != null) {
            span.recordException(exception)
            span.setStatus(StatusCode.ERROR)
        } else {
            span.setStatus(StatusCode.ERROR, errorMessage)
        }
        respondToChallenge(submitUrl, errorMessage, context)
    }
}
