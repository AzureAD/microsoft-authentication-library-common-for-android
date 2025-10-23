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
import com.microsoft.identity.common.logging.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicBoolean

/**
 * WebView message listener for handling WebAuthn/Passkey authentication flows.
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
) : WebViewCompat.WebMessageListener {

    /** Tracks if a WebAuthn request is currently pending. Only one request is allowed at a time. */
    private val havePendingRequest = AtomicBoolean(false)

    /**
     * Handles postMessage() calls from the web page for WebAuthn requests.
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
        parseMessage(message.data, replyProxy)?.let { webAuthnMessage ->
            onRequest(
                webAuthnMessage = webAuthnMessage,
                sourceOrigin = sourceOrigin,
                isMainFrame = isMainFrame,
                javaScriptReplyProxy = replyProxy
            )
        }
    }

    /**
     * Processes an incoming WebAuthn request.
     *
     * @param webAuthnMessage Parsed WebAuthn message.
     * @param sourceOrigin Origin of the request.
     * @param isMainFrame True if request is from the main frame.
     * @param javaScriptReplyProxy Proxy for sending responses.
     */
    private fun onRequest(
        webAuthnMessage: WebAuthnMessage,
        sourceOrigin: Uri,
        isMainFrame: Boolean,
        javaScriptReplyProxy: JavaScriptReplyProxy) {
        val methodTag = "$TAG:onRequest"
        Logger.info(methodTag, "Received WebAuthn request of type: ${webAuthnMessage.type} from origin: $sourceOrigin")
        val passkeyReplyChannel = PasskeyReplyChannel(javaScriptReplyProxy, webAuthnMessage.type)

        // Only allow one request at a time.
        if (havePendingRequest.get()) {
            passkeyReplyChannel.postError("Request already in progress")
            return
        }
        havePendingRequest.set(true)

        // Only allow requests from the main frame.
        if (!isMainFrame) {
            passkeyReplyChannel.postError("Requests from iframes are not supported")
            havePendingRequest.set(false)
            return
        }

        when (webAuthnMessage.type) {
            CREATE_UNIQUE_KEY ->
                this.coroutineScope.launch {
                    handleCreateFlow(credentialManagerHandler, webAuthnMessage.request, passkeyReplyChannel)
                    havePendingRequest.set(false)
                }
            GET_UNIQUE_KEY -> this.coroutineScope.launch {
                handleGetFlow(credentialManagerHandler, webAuthnMessage.request, passkeyReplyChannel)
                havePendingRequest.set(false)
            }
            else -> {
                passkeyReplyChannel.postError("Unknown request type: ${webAuthnMessage.type}")
                havePendingRequest.set(false)
            }
        }
    }

    /**
     * Handles the WebAuthn get flow to retrieve an existing passkey.
     *
     * @param credentialManagerHandler Handler for credential operations.
     * @param message JSON string with the get request parameters.
     * @param reply Channel for sending the response.
     */
    private suspend fun handleGetFlow(
        credentialManagerHandler: CredentialManagerHandler,
        message: String,
        reply: PasskeyReplyChannel) {
        try {
            val getCredentialResponse = credentialManagerHandler.getPasskey(message)
            reply.postSuccess(
                (getCredentialResponse.credential as PublicKeyCredential).authenticationResponseJson
            )
        } catch (t: Throwable) {
            reply.postError(t)
        }
    }

    /**
     * Handles the WebAuthn create flow to register a new passkey.
     *
     * @param credentialManagerHandler Handler for credential operations.
     * @param message JSON string with the create request parameters.
     * @param reply Channel for sending the response.
     */
    private suspend fun handleCreateFlow(
        credentialManagerHandler: CredentialManagerHandler,
        message: String,
        reply: PasskeyReplyChannel) {
        try {
            val createCredentialResponse = credentialManagerHandler.createPasskey(message)
            reply.postSuccess(createCredentialResponse.registrationResponseJson)
        } catch (t: Throwable) {
            reply.postError(t)
        }
    }

    /**
     * Parses a JSON message into a [WebAuthnMessage].
     *
     * Expected format: `{"type": "create|get", "request": "<JSON payload>"}`
     *
     * @param messageData JSON string to parse.
     * @param javaScriptReplyProxy Proxy for error responses.
     * @return Parsed [WebAuthnMessage] or null if invalid.
     */
    private fun parseMessage(messageData: String?, javaScriptReplyProxy: JavaScriptReplyProxy): WebAuthnMessage? {

        val passkeyReplyChannel = PasskeyReplyChannel(javaScriptReplyProxy)
        if (messageData.isNullOrBlank()) {
            passkeyReplyChannel.postError("Received empty message data")
            return null
        }

        return runCatching {
            val json = JSONObject(messageData)
            val type = json.optString(TYPE_KEY).takeIf { it.isNotBlank() }
            val request = json.optString(REQUEST_KEY).takeIf { it.isNotBlank() }

            if (type == null ) {
                passkeyReplyChannel.postError("Missing required key: type")
                null
            } else if (request == null) {
                passkeyReplyChannel.postError("Missing required key: request")
                null
            } else {
                WebAuthnMessage(type, request)
            }
        }.onFailure { throwable ->
            passkeyReplyChannel.postError(throwable)
        }.getOrNull()
    }

    /** Internal representation of a WebAuthn message with type and request payload. */
    private data class WebAuthnMessage(val type: String, val request: String)

    companion object {
        const val TAG = "PasskeyWebListener"

        /** WebAuthn request type for creating a new credential. */
        const val CREATE_UNIQUE_KEY = "create"

        /** WebAuthn request type for retrieving an existing credential. */
        const val GET_UNIQUE_KEY = "get"

        /** JSON key for the request type field. */
        const val TYPE_KEY = "type"

        /** JSON key for the request payload field. */
        const val REQUEST_KEY = "request"

        /** Name of the JavaScript message port interface. */
        private const val INTERFACE_NAME = "__webauthn_interface__"

        /**
         * Minified JavaScript code that intercepts WebAuthn API calls.
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
            var __webauthn_interface__,__webauthn_hooks__;!function(e){__webauthn_interface__.addEventListener("message",function e(n){console.log(n.data);var t=JSON.parse(n.data);"get"===t.type?s(t):"create"===t.type?u(t):console.log("Incorrect response format for reply: "+t.type)});var n=null,t=null,r=null,a=null;function s(e){if(null===n||null===r){console.log("Reply failure: Resolve: "+t+" and reject: "+a);return}if("success"!=e.status){var s=r;n=null,r=null,s(new DOMException(e.data.domExceptionMessage,e.data.domExceptionName));return}var o=i(e.data),l=n;n=null,r=null,l(o)}function o(e){var n=e.length%4;return Uint8Array.from(atob(e.replace(/-/g,"+").replace(/_/g,"/").padEnd(e.length+(0===n?0:4-n),"=")),function(e){return e.charCodeAt(0)}).buffer}function l(e){return btoa(Array.from(new Uint8Array(e),function(e){return String.fromCharCode(e)}).join("")).replace(/\+/g,"-").replace(/\//g,"_").replace(/=+${'$'}/,"")}function u(e){if(null===t||null===a){console.log("Reply failure: Resolve: "+t+" and reject: "+a);return}if("success"!=e.status){var n=a;t=null,a=null,n(new DOMException(e.data.domExceptionMessage,e.data.domExceptionName));return}var r=i(e.data),s=t;t=null,a=null,s(r)}function i(e){return e.rawId=o(e.rawId),e.response.clientDataJSON=o(e.response.clientDataJSON),e.response.hasOwnProperty("attestationObject")&&(e.response.attestationObject=o(e.response.attestationObject)),e.response.hasOwnProperty("authenticatorData")&&(e.response.authenticatorData=o(e.response.authenticatorData)),e.response.hasOwnProperty("signature")&&(e.response.signature=o(e.response.signature)),e.response.hasOwnProperty("userHandle")&&(e.response.userHandle=o(e.response.userHandle)),e.getClientExtensionResults=function e(){return{}},e.response.getTransports=function n(){return e.response.hasOwnProperty("transports")?e.response.transports:[]},e}e.create=function n(r){if(!("publicKey"in r))return e.originalCreateFunction(r);var s=new Promise(function(e,n){t=e,a=n}),o=r.publicKey;if(o.hasOwnProperty("challenge")){var u=l(o.challenge);o.challenge=u}if(o.hasOwnProperty("user")&&o.user.hasOwnProperty("id")){var i=l(o.user.id);o.user.id=i}if(o.hasOwnProperty("excludeCredentials")&&Array.isArray(o.excludeCredentials)&&o.excludeCredentials.length>0)for(var c=0;c<o.excludeCredentials.length;c++){var p=o.excludeCredentials[c];p&&p.hasOwnProperty("id")&&(p.id=l(p.id))}var d=JSON.stringify({type:"create",request:o});return __webauthn_interface__.postMessage(d),s},e.get=function t(a){if(!("publicKey"in a))return e.originalGetFunction(a);var s=new Promise(function(e,t){n=e,r=t}),o=a.publicKey;if(o.hasOwnProperty("challenge")){var u=l(o.challenge);o.challenge=u}var i=JSON.stringify({type:"get",request:o});return __webauthn_interface__.postMessage(i),s},e.onReplyGet=s,e.CM_base64url_decode=o,e.CM_base64url_encode=l,e.onReplyCreate=u}(__webauthn_hooks__||(__webauthn_hooks__={})),__webauthn_hooks__.originalGetFunction=navigator.credentials.get,__webauthn_hooks__.originalCreateFunction=navigator.credentials.create,navigator.credentials.get=__webauthn_hooks__.get,navigator.credentials.create=__webauthn_hooks__.create,window.PublicKeyCredential=function(){},window.PublicKeyCredential.isUserVerifyingPlatformAuthenticatorAvailable=function(){return Promise.resolve(!1)};
         """

        /** Allowed origins that can use the WebAuthn interface. */
        private val ALLOWED_ORIGIN_RULES_PRODUCTION = setOf(
            "https://login.microsoft.com",
            "https://account.live.com",
            "https://mysignins.microsoft.com",
            "https://mysignins.azure.us",
            "https://mysignins.microsoft.scloud",
            "https://mysignins.eaglex.ic.gov",
            "https://login.microsoftonline.us",
            "https://login.microsoftonline.microsoft.scloud",
            "https://login.microsoftonline.eaglex.ic.gov"
        )

        /** Allowed origins for pre-production/testing environments. */
        private val ALLOWED_ORIGIN_PRE_PRODUCTION = setOf(
            "https://account.live-int.com",
            "https://login.windows-ppe.net",
            "https://mysignins-ppe.microsoft.com"
        )

        /**
         * Gets the set of allowed origin rules based on build configuration.
         *
         * @return Set of allowed origin rules.
         */
        private fun getAllowedOriginRules(): Set<String> {
            val mutableSet = ALLOWED_ORIGIN_RULES_PRODUCTION.toMutableSet()
            if (BuildConfig.DEBUG) {
               mutableSet.addAll(ALLOWED_ORIGIN_PRE_PRODUCTION)
            }
            return mutableSet.toSet()
        }

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
            webClient: AzureActiveDirectoryWebViewClient
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

            // Uncomment for debugging: view console.log messages from injected JS
            // WebView.setWebContentsDebuggingEnabled(true)

            return if (WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)) {
                Logger.verbose(methodTag, "WEB_MESSAGE_LISTENER is supported on this WebView.")

                // Attach the WebMessageListener that handles WebAuthn/Passkey communication.
                WebViewCompat.addWebMessageListener(
                    webView,
                    INTERFACE_NAME,
                    getAllowedOriginRules(),
                    PasskeyWebListener(
                        coroutineScope = CoroutineScope(Dispatchers.Default),
                        credentialManagerHandler = CredentialManagerHandler(activity)
                    )
                )

                Logger.info(methodTag, "PasskeyWebListener successfully hooked into WebView.")

                // Injects the JavaScript interface early in the page load lifecycle.
                webClient.addOnPageStartedScript(
                    TAG,
                    WEB_AUTHN_INTERFACE_JS_MINIFIED,
                    getAllowedOriginRules()
                )
                true
            } else {
                Logger.warn(methodTag, "WEB_MESSAGE_LISTENER not supported on this device/WebView.")
                false
            }
        }
    }
}
