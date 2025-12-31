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

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.os.Build
import android.webkit.WebView
import androidx.annotation.UiThread
import androidx.credentials.PublicKeyCredential
import androidx.webkit.JavaScriptReplyProxy
import androidx.webkit.WebMessageCompat
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import com.microsoft.identity.common.BuildConfig
import com.microsoft.identity.common.internal.ui.webview.AzureActiveDirectoryWebViewClient
import com.microsoft.identity.common.java.exception.ClientException
import com.microsoft.identity.common.logging.Logger
import io.opentelemetry.api.trace.SpanContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicBoolean

/**
 * WebView message listener for handling WebAuthN/Passkey authentication flows.
 *
 * Intercepts postMessage() calls from JavaScript to handle credential creation and retrieval
 * using the Android Credential Manager API. Only accepts requests from allowed origins.
 *
 * @property coroutineScope Scope for launching credential operations.
 * @property credentialManagerHandler Handles passkey creation and retrieval.
 */
class PasskeyWebListener(
    private val coroutineScope: CoroutineScope,
    private val credentialManagerHandler: CredentialManagerHandler,
    private val spanContext: SpanContext?
) : WebViewCompat.WebMessageListener {

    /** Tracks if a WebAuthN request is currently pending. Only one request is allowed at a time. */
    private val havePendingRequest = AtomicBoolean(false)

    /**
     * Handles postMessage() calls from the web page for WebAuthN requests.
     *
     * @param view The WebView that received the message.
     * @param message The message received from the web page.
     * @param sourceOrigin The origin of the message.
     * @param isMainFrame True if the message originated from the main frame.
     * @param replyProxy Proxy for sending responses back to JavaScript.
     */
    @UiThread
    override fun onPostMessage(
        view: WebView,
        message: WebMessageCompat,
        sourceOrigin: Uri,
        isMainFrame: Boolean,
        replyProxy: JavaScriptReplyProxy,
    ) {
        parseMessage(message.data, replyProxy)?.let { webAuthNMessage ->
            onRequest(
                webAuthNMessage = webAuthNMessage,
                sourceOrigin = sourceOrigin,
                isMainFrame = isMainFrame,
                javaScriptReplyProxy = replyProxy
            )
        }
    }

    /**
     * Processes an incoming WebAuthN request.
     *
     * @param webAuthNMessage Parsed WebAuthN message.
     * @param sourceOrigin Origin of the request.
     * @param isMainFrame True if request is from the main frame.
     * @param javaScriptReplyProxy Proxy for sending responses.
     */
    private fun onRequest(
        webAuthNMessage: WebAuthNMessage,
        sourceOrigin: Uri,
        isMainFrame: Boolean,
        javaScriptReplyProxy: JavaScriptReplyProxy
    ) {
        val methodTag = "$TAG:onRequest"
        Logger.info(
            methodTag,
            "Received WebAuthN request of type: ${webAuthNMessage.type} from origin: $sourceOrigin"
        )
        val passkeyReplyChannel = PasskeyReplyChannel(
            replyProxy = javaScriptReplyProxy,
            requestType = webAuthNMessage.type,
            spanContext = spanContext
        )

        // Only allow one request at a time.
        if (havePendingRequest.get()) {
            passkeyReplyChannel.postError(
                ClientException(
                    ClientException.REQUEST_IN_PROGRESS,
                    "A WebAuthN request is already in progress."
                )
            )
            return
        }
        havePendingRequest.set(true)

        // Only allow requests from the main frame.
        if (!isMainFrame) {
            passkeyReplyChannel.postError(
                ClientException(
                    ClientException.UNSUPPORTED_OPERATION,
                    "WebAuthN requests from iframes are not supported."
                )
            )
            havePendingRequest.set(false)
            return
        }

        when (webAuthNMessage.type) {
            CREATE_UNIQUE_KEY ->
                this.coroutineScope.launch {
                    handleCreateFlow(
                        credentialManagerHandler,
                        webAuthNMessage.request,
                        passkeyReplyChannel
                    )
                    havePendingRequest.set(false)
                }

            GET_UNIQUE_KEY -> this.coroutineScope.launch {
                handleGetFlow(
                    credentialManagerHandler,
                    webAuthNMessage.request,
                    passkeyReplyChannel
                )
                havePendingRequest.set(false)
            }

            else -> {
                passkeyReplyChannel.postError(
                    ClientException(
                        ClientException.UNSUPPORTED_OPERATION,
                        "Unsupported WebAuthN request type: ${webAuthNMessage.type}"
                    )
                )
                havePendingRequest.set(false)
            }
        }
    }

    /**
     * Handles the WebAuthN get flow to retrieve an existing passkey.
     *
     * @param credentialManagerHandler Handler for credential operations.
     * @param message JSON string with the get request parameters.
     * @param reply Channel for sending the response.
     */
    private suspend fun handleGetFlow(
        credentialManagerHandler: CredentialManagerHandler,
        message: String,
        reply: PasskeyReplyChannel
    ) {
        runCatching { credentialManagerHandler.getPasskey(message) }
            .onSuccess { credentialResponse ->
                val publicKeyCredential = credentialResponse.credential as? PublicKeyCredential
                if (publicKeyCredential != null) {
                    reply.postSuccess(publicKeyCredential.authenticationResponseJson)
                } else {
                    reply.postError(
                        ClientException(
                            ClientException.UNSUPPORTED_OPERATION,
                            "Retrieved credential is not a PublicKeyCredential."
                        )
                    )                }
           }
            .onFailure { throwable ->
                reply.postError(throwable)
            }
    }

    /**
     * Handles the WebAuthN create flow to register a new passkey.
     *
     * @param credentialManagerHandler Handler for credential operations.
     * @param message JSON string with the create request parameters.
     * @param reply Channel for sending the response.
     */
    private suspend fun handleCreateFlow(
        credentialManagerHandler: CredentialManagerHandler,
        message: String,
        reply: PasskeyReplyChannel
    ) {
        runCatching { credentialManagerHandler.createPasskey(message) }
            .onSuccess { createCredentialResponse ->
                reply.postSuccess(createCredentialResponse.registrationResponseJson)
            }
            .onFailure { throwable ->
                reply.postError(throwable)
            }
    }

    /**
     * Parses a JSON message into a [WebAuthNMessage].
     *
     * Expected format: `{"type": "create|get", "request": "<JSON payload>"}`
     *
     * @param messageData JSON string to parse.
     * @param javaScriptReplyProxy Proxy for error responses.
     * @return Parsed [WebAuthNMessage] or null if invalid.
     */
    private fun parseMessage(
        messageData: String?,
        javaScriptReplyProxy: JavaScriptReplyProxy
    ): WebAuthNMessage? {
        val passkeyReplyChannel = PasskeyReplyChannel(
            replyProxy = javaScriptReplyProxy,
            spanContext = spanContext
        )
        return runCatching {
            if (messageData.isNullOrBlank()) {
                throw ClientException(ClientException.MISSING_PARAMETER, "Message data is null or blank")
            }
            val json = JSONObject(messageData)
            val type = json.optString(TYPE_KEY).takeIf { it.isNotBlank() }
            val request = json.optString(REQUEST_KEY).takeIf { it.isNotBlank() }

            if (type == null) {
                throw ClientException(ClientException.MISSING_PARAMETER, "Missing required key: type")
            } else if (request == null) {
                throw ClientException(ClientException.MISSING_PARAMETER, "Missing required key: request")
            } else {
                WebAuthNMessage(type, request)
            }
        }.onFailure { throwable ->
            passkeyReplyChannel.postError(throwable)
        }.getOrNull()
    }

    /** Internal representation of a WebAuthN message with type and request payload. */
    private data class WebAuthNMessage(val type: String, val request: String)

    companion object {
        const val TAG = "PasskeyWebListener"

        /** WebAuthN request type for creating a new credential. */
        const val CREATE_UNIQUE_KEY = "create"

        /** WebAuthN request type for retrieving an existing credential. */
        const val GET_UNIQUE_KEY = "get"

        /** JSON key for the request type field. */
        const val TYPE_KEY = "type"

        /** JSON key for the request payload field. */
        const val REQUEST_KEY = "request"

        /** Name of the JavaScript message port interface. */
        private const val INTERFACE_NAME = "__webauthn_interface__"

        /**
         * Minified JavaScript code that intercepts WebAuthN API calls.
         *
         * ⚠️ IMPORTANT: This is the MINIFIED version of js-bridge.js
         *
         * Source file: common/src/main/java/com/microsoft/identity/common/internal/providers/oauth2/js-bridge.js
         *
         * When updating:
         * 1. Modify the source file (js-bridge.js) with your changes
         * 2. Minify the updated JavaScript code
         * 3. Replace the string below with the new minified version
         * 4. Verify the minified code works correctly through testing
         *
         * DO NOT modify this constant directly - always update the source file first!
         */
        private const val WEB_AUTHN_INTERFACE_JS_MINIFIED = """
            var __webauthn_interface__,__webauthn_hooks__;!function(e){__webauthn_interface__.addEventListener("message",(function(e){console.log(e.data);var n=JSON.parse(e.data);"get"===n.type?o(n):"create"===n.type?l(n):console.log("Incorrect response format for reply: "+n.type)}));var n=null,t=null,r=null,a=null;function o(e){if(null!==n&&null!==r){if("success"!=e.status){var o=r;return n=null,r=null,void o(new DOMException(e.data.domExceptionMessage,e.data.domExceptionName))}var s=u(e.data),i=n;n=null,r=null,i(s)}else console.log("Reply failure: Resolve: "+t+" and reject: "+a)}function s(e){var n=e.length%4;return Uint8Array.from(atob(e.replace(/-/g,"+").replace(/_/g,"/").padEnd(e.length+(0===n?0:4-n),"=")),(function(e){return e.charCodeAt(0)})).buffer}function i(e){return btoa(Array.from(new Uint8Array(e),(function(e){return String.fromCharCode(e)})).join("")).replace(/\+/g,"-").replace(/\//g,"_").replace(/=+${'$'}/,"")}function l(e){if(null!==t&&null!==a){if("success"!=e.status){var n=a;return t=null,a=null,void n(new DOMException(e.data.domExceptionMessage,e.data.domExceptionName))}var r=u(e.data),o=t;t=null,a=null,o(r)}else console.log("Reply failure: Resolve: "+t+" and reject: "+a)}function u(e){return e.rawId=s(e.rawId),e.response.clientDataJSON=s(e.response.clientDataJSON),e.response.hasOwnProperty("attestationObject")&&(e.response.attestationObject=s(e.response.attestationObject)),e.response.hasOwnProperty("authenticatorData")&&(e.response.authenticatorData=s(e.response.authenticatorData)),e.response.hasOwnProperty("signature")&&(e.response.signature=s(e.response.signature)),e.response.hasOwnProperty("userHandle")&&(e.response.userHandle=s(e.response.userHandle)),e.getClientExtensionResults=function(){return{}},e.response.getTransports=function(){return e.response.hasOwnProperty("transports")?e.response.transports:[]},e}e.create=function(n){if(!("publicKey"in n))return e.originalCreateFunction(n);var r=new Promise((function(e,n){t=e,a=n})),o=n.publicKey;if(o.hasOwnProperty("challenge")){var s=i(o.challenge);o.challenge=s}if(o.hasOwnProperty("user")&&o.user.hasOwnProperty("id")){var l=i(o.user.id);o.user.id=l}if(o.hasOwnProperty("excludeCredentials")&&Array.isArray(o.excludeCredentials)&&o.excludeCredentials.length>0)for(var u=0;u<o.excludeCredentials.length;u++){var c=o.excludeCredentials[u];c&&c.hasOwnProperty("id")&&(c.id=i(c.id))}var p={type:"create",request:o},_=JSON.stringify(p);return __webauthn_interface__.postMessage(_),r},e.get=function(t){if(!("publicKey"in t))return e.originalGetFunction(t);var a=new Promise((function(e,t){n=e,r=t})),o=t.publicKey;if(o.hasOwnProperty("challenge")){var s=i(o.challenge);o.challenge=s}var l={type:"get",request:o},u=JSON.stringify(l);return __webauthn_interface__.postMessage(u),a},e.onReplyGet=o,e.CM_base64url_decode=s,e.CM_base64url_encode=i,e.onReplyCreate=l}(__webauthn_hooks__||(__webauthn_hooks__={})),__webauthn_hooks__.originalGetFunction=navigator.credentials.get,__webauthn_hooks__.originalCreateFunction=navigator.credentials.create,navigator.credentials.get=__webauthn_hooks__.get,navigator.credentials.create=__webauthn_hooks__.create,window.PublicKeyCredential=function(){},window.PublicKeyCredential.isUserVerifyingPlatformAuthenticatorAvailable=function(){return Promise.resolve(!0)};
         """




        /**
         * Attaches the passkey listener to a WebView.
         *
         * Requires Android 9+ and WebView WEB_MESSAGE_LISTENER support.
         *
         * @param webView WebView to attach to.
         * @param activity Activity context for credential operations.
         * @param webClient WebViewClient to inject JavaScript into.
         * @return True if successfully hooked, false otherwise.
         */
        @JvmStatic
        fun hook(
            webView: WebView,
            activity: Activity,
            webClient: AzureActiveDirectoryWebViewClient,
            spanContext: SpanContext?
        ): Boolean {
            val methodTag = "$TAG:hook"

            // Passkey features are supported only on Android 9 (API 28) and higher.
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                Logger.warn(
                    methodTag,
                    "Passkey functionality requires Android 9 (Pie) or higher. " +
                            "Current version: ${Build.VERSION.SDK_INT}"
                )
                return false
            }

            return if (WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)) {
                Logger.verbose(methodTag, "WEB_MESSAGE_LISTENER is supported on this WebView.")

                // Attach the WebMessageListener that handles WebAuthN/Passkey communication.
                WebViewCompat.addWebMessageListener(
                    webView,
                    INTERFACE_NAME,
                    PasskeyOriginRulesManager.getAllowedOriginRules(),
                    PasskeyWebListener(
                        coroutineScope = CoroutineScope(Dispatchers.Default),
                        credentialManagerHandler = CredentialManagerHandler(activity),
                        spanContext = spanContext
                    )
                )

                Logger.info(methodTag, "PasskeyWebListener successfully hooked into WebView.")

                // Injects the JavaScript interface early in the page load lifecycle.
                val scriptToInject = if (BuildConfig.DEBUG) {
                    WebView.setWebContentsDebuggingEnabled(true)
                    loadJsBridgeScript(activity)
                } else {
                    WEB_AUTHN_INTERFACE_JS_MINIFIED
                }
                webClient.addPasskeyRegistrationJsScript(scriptToInject)

                true
            } else {
                Logger.warn(methodTag, "WEB_MESSAGE_LISTENER not supported on this device/WebView.")
                false
            }
        }

        /**
         * Loads the full js-bridge.js script from assets for debugging.
         */
        private fun loadJsBridgeScript(context: Context): String {
            return try {
                context.assets.open("js-bridge.js").bufferedReader().use { it.readText() }
            } catch (e: Exception) {
                Logger.warn(TAG, "Failed to load js-bridge.js from assets, falling back to minified version: ${e.message}")
                WEB_AUTHN_INTERFACE_JS_MINIFIED
            }
        }

    }
}
