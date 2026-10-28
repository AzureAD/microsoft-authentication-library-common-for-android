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

package com.microsoft.identity.common.internal.ui.webview.authux

import android.webkit.WebView
import androidx.annotation.VisibleForTesting
import androidx.webkit.ScriptHandler
import com.microsoft.identity.common.internal.broker.AuthUxJavaScriptInterface
import com.microsoft.identity.common.internal.broker.AuthUxRequestCapabilities
import com.microsoft.identity.common.internal.broker.AuthUxTelemetrySink
import com.microsoft.identity.common.logging.Logger
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Owns Auth UX transport selection and lifecycle for one WebView authorization request.
 */
class AuthUxBridgeSession private constructor(
    val mode: AuthUxTransportMode,
    private var webView: WebView?,
    private val processor: AuthUxJavaScriptInterface?,
    private val webKitAdapter: AuthUxWebKitAdapter,
    private var scriptHandler: ScriptHandler?,
    private var listenerRegistered: Boolean,
    private var javaScriptInterfaceAdded: Boolean
) : AutoCloseable {
    private val closed = AtomicBoolean(false)

    /**
     * Updates legacy JavaScript-interface exposure when a main-frame navigation starts.
     *
     * The interface is added for approved Auth UX URLs and removed before an unapproved or
     * federated page loads. WebMessageListener and disabled sessions require no per-navigation
     * changes because their origin-scoped surfaces are registered for the request lifetime.
     *
     * @param view the WebView starting the navigation.
     * @param url the URL being loaded, or null when unavailable.
     */
    fun onPageStarted(view: WebView, url: String?) {
        if (mode != AuthUxTransportMode.JAVASCRIPT_INTERFACE || closed.get()) {
            return
        }

        if (AuthUxJavaScriptInterface.isValidUriForInterface(url)) {
            if (!javaScriptInterfaceAdded) {
                val javaScriptInterfaceProcessor = processor ?: return
                view.addJavascriptInterface(
                    javaScriptInterfaceProcessor,
                    AuthUxJavaScriptInterface.getInterfaceName()
                )
                javaScriptInterfaceAdded = true
                Logger.info(TAG, "Added Auth UX JavaScript interface for an approved navigation.")
            }
        } else if (javaScriptInterfaceAdded) {
            view.removeJavascriptInterface(AuthUxJavaScriptInterface.getInterfaceName())
            javaScriptInterfaceAdded = false
            Logger.info(TAG, "Removed Auth UX JavaScript interface for an unapproved navigation.")
        }
    }

    /**
     * Installs the page-facing compatibility facade after an approved legacy page finishes loading.
     *
     * The facade forwards `window.broker.postMessageToBroker(...)` calls to the installed
     * JavaScript interface. Other transport modes use a document-start facade or expose no bridge.
     *
     * @param view the WebView that finished loading.
     */
    fun onPageFinished(view: WebView) {
        if (mode == AuthUxTransportMode.JAVASCRIPT_INTERFACE &&
            javaScriptInterfaceAdded &&
            !closed.get()
        ) {
            view.evaluateJavascript(JAVASCRIPT_INTERFACE_FACADE_SCRIPT, null)
            Logger.info(TAG, "Requested Auth UX JavaScript-interface facade injection.")
        }
    }

    /**
     * Closes message processing and removes all request-owned bridge surfaces.
     *
     * Processing is closed before transport cleanup so queued callbacks become inert. Cleanup is
     * best-effort, idempotent, and releases the document-start script, WebMessageListener, legacy
     * JavaScript interface, and retained WebView reference owned by this session.
     */
    override fun close() {
        if (!closed.compareAndSet(false, true)) {
            Logger.info(TAG, "Ignoring duplicate Auth UX bridge session close.")
            return
        }

        Logger.info(TAG, "Closing Auth UX bridge session for $mode transport.")
        processor?.close()
        val currentWebView = webView

        try {
            if (scriptHandler != null) {
                scriptHandler?.remove()
                Logger.info(TAG, "Removed Auth UX document-start script.")
            }
        } catch (t: Throwable) {
            Logger.error(TAG, "Failed to remove Auth UX document-start script.", t)
        } finally {
            scriptHandler = null
        }

        if (currentWebView != null && listenerRegistered) {
            try {
                webKitAdapter.removeWebMessageListener(currentWebView, PRIVATE_OBJECT_NAME)
            } catch (t: Throwable) {
                Logger.error(TAG, "Failed to remove Auth UX WebMessageListener.", t)
            } finally {
                listenerRegistered = false
            }
        }

        if (currentWebView != null && javaScriptInterfaceAdded) {
            try {
                currentWebView.removeJavascriptInterface(AuthUxJavaScriptInterface.getInterfaceName())
                Logger.info(TAG, "Removed Auth UX JavaScript interface during session close.")
            } catch (t: Throwable) {
                Logger.error(TAG, "Failed to remove Auth UX JavaScript interface.", t)
            } finally {
                javaScriptInterfaceAdded = false
            }
        }

        webView = null
        Logger.info(TAG, "Closed Auth UX bridge session.")
    }

    companion object {
        private val TAG = AuthUxBridgeSession::class.java.simpleName
        const val PRIVATE_OBJECT_NAME = "__authUxNativeBridge"

        /**
         * Compatibility facade between the existing Auth UX webpage API and AndroidX
         * WebMessageListener.
         *
         * The webpage sends Mobile Onboarding and Number Matching messages through
         * `window.broker.postMessageToBroker(message)`, while AndroidX exposes the private native
         * object as `$PRIVATE_OBJECT_NAME.postMessage(String)`. This facade preserves the webpage
         * contract, converts the message to JSON, and forwards it to that private object.
         *
         * It is registered as a document-start script so the API exists before page scripts
         * execute, avoiding a race where an early Auth UX message could be sent before the facade
         * is available. The script initializes only in the main frame; native callback validation
         * independently enforces the same main-frame requirement.
         */
        private val WEB_MESSAGE_LISTENER_FACADE_SCRIPT = """
            (function () {
                if (window.top !== window.self) { return; }
                var broker = window.broker;
                if (broker == null) {
                    broker = {};
                    window.broker = broker;
                } else if (typeof broker !== "object" && typeof broker !== "function") {
                    return;
                }
                broker.postMessageToBroker = function (message) {
                    window.$PRIVATE_OBJECT_NAME.postMessage(JSON.stringify(message));
                };
            })();
        """.trimIndent()

        /**
         * Compatibility facade between the existing Auth UX webpage API and
         * `addJavascriptInterface`.
         *
         * Android exposes the annotated native method as
         * `window.broker.receiveAuthUxMessage(String)`, while the webpage sends Mobile Onboarding
         * and Number Matching messages through `window.broker.postMessageToBroker(message)`. This
         * facade preserves the webpage contract, converts the message to JSON, and forwards it to
         * the annotated native method.
         *
         * It is evaluated after a trusted page finishes loading and only while the JavaScript
         * interface is installed for that page.
         */
        private val JAVASCRIPT_INTERFACE_FACADE_SCRIPT =
            "window.${AuthUxJavaScriptInterface.getInterfaceName()}.postMessageToBroker = function(message) { " +
                "window.${AuthUxJavaScriptInterface.getInterfaceName()}.receiveAuthUxMessage(JSON.stringify(message)); " +
                "};"

        /**
         * Creates the request-owned Auth UX bridge and selects exactly one transport.
         *
         * Requests without an allowed Auth UX capability are disabled. WebMessageListener is used
         * only when [preferWebMessageListener] is true, the initial URL fits the fixed registration
         * rules, and both required AndroidX features are available; otherwise the request uses
         * `addJavascriptInterface`. The caller captures flight policy once before creation. Listener
         * setup is transactional, and fallback is allowed only after complete rollback so two
         * native bridges cannot remain exposed at the same time.
         */
        @JvmStatic
        @JvmOverloads
        fun create(
            webView: WebView,
            initialUrl: String?,
            isBrokerAuthProcess: Boolean,
            telemetrySink: AuthUxTelemetrySink?,
            brokerAuthUxBridgeEnabled: Boolean,
            preferWebMessageListener: Boolean,
            webKitAdapter: AuthUxWebKitAdapter = DefaultAuthUxWebKitAdapter,
            originPolicy: AuthUxOriginPolicy = AuthUxOriginPolicy()
        ): AuthUxBridgeSession {
            // Capabilities are request-scoped. Broker behavior requires bridge enablement, while a
            // non-broker request can receive only onboarding telemetry when a recorder is present.
            val capabilities = when {
                isBrokerAuthProcess && !brokerAuthUxBridgeEnabled -> null
                isBrokerAuthProcess -> AuthUxRequestCapabilities.BROKER
                telemetrySink != null -> AuthUxRequestCapabilities(
                    telemetryAllowed = true,
                    numberMatchingAllowed = false
                )
                else -> {
                    // Non-broker requests without a recorder have no supported Auth UX capability.
                    null
                }
            }

            if (capabilities == null) {
                if (isBrokerAuthProcess) {
                    Logger.info(
                        TAG,
                        "Auth UX bridge disabled for this Broker request because " +
                            "brokerAuthUxBridgeEnabled is false."
                    )
                } else {
                    Logger.info(
                        TAG,
                        "Auth UX bridge disabled because no onboarding telemetry recorder is available."
                    )
                }
                return disabled(webKitAdapter)
            }

            val processor = AuthUxJavaScriptInterface(
                telemetrySink = telemetrySink,
                capabilities = capabilities
            )

            if (!preferWebMessageListener) {
                Logger.info(
                    TAG,
                    "WebMessageListener preference disabled; selecting addJavascriptInterface."
                )
                return javaScriptInterface(webView, initialUrl, processor, webKitAdapter)
            }

            // WebMessageListener and its document-start facade form one transport. Select it only
            // when the initial document can receive both under the same registration rules and the
            // runtime supports both required AndroidX features.
            if (!originPolicy.isInitialUrlSupportedByRegistrationRules(initialUrl) ||
                !webKitAdapter.isWebMessageListenerSupported() ||
                !webKitAdapter.isDocumentStartScriptSupported()
            ) {
                Logger.info(
                    TAG,
                    "WebMessageListener prerequisites are unavailable; selecting " +
                        "addJavascriptInterface."
                )
                return javaScriptInterface(webView, initialUrl, processor, webKitAdapter)
            }
            val allowedOrigins = originPolicy.getAllowedOriginRules()

            var listenerMayBeRegistered = false
            var scriptRegistrationAttempted = false
            var handler: ScriptHandler? = null
            return try {
                // Assume registration may have modified WebView state before throwing so rollback
                // always attempts listener removal after this call begins.
                listenerMayBeRegistered = true
                webKitAdapter.addWebMessageListener(
                    webView,
                    PRIVATE_OBJECT_NAME,
                    allowedOrigins,
                    AuthUxWebMessageListener(processor, originPolicy)
                )
                // A thrown registration call may have installed the script without returning its
                // removal handle. Treat that state as uncertain rather than falling back.
                scriptRegistrationAttempted = true
                handler = webKitAdapter.addDocumentStartScript(
                    webView,
                    WEB_MESSAGE_LISTENER_FACADE_SCRIPT,
                    allowedOrigins
                )
                Logger.info(TAG, "Selected WebMessageListener Auth UX transport.")
                AuthUxBridgeSession(
                    mode = AuthUxTransportMode.WEB_MESSAGE_LISTENER,
                    webView = webView,
                    processor = processor,
                    webKitAdapter = webKitAdapter,
                    scriptHandler = handler,
                    listenerRegistered = true,
                    javaScriptInterfaceAdded = false
                )
            } catch (t: Throwable) {
                Logger.error(TAG, "WebMessageListener Auth UX setup failed; rolling back.", t)
                var rollbackComplete = !scriptRegistrationAttempted || handler != null
                try {
                    handler?.remove()
                } catch (cleanupFailure: Throwable) {
                    rollbackComplete = false
                    Logger.error(TAG, "Failed to roll back Auth UX document-start script.", cleanupFailure)
                }
                if (listenerMayBeRegistered) {
                    try {
                        webKitAdapter.removeWebMessageListener(webView, PRIVATE_OBJECT_NAME)
                    } catch (cleanupFailure: Throwable) {
                        rollbackComplete = false
                        Logger.error(TAG, "Failed to roll back Auth UX WebMessageListener.", cleanupFailure)
                    }
                }

                // Falling back is safe only after complete rollback. Otherwise disable the bridge
                // to avoid exposing both transports after a partial setup failure.
                if (rollbackComplete) {
                    Logger.info(
                        TAG,
                        "WebMessageListener rollback completed; selecting addJavascriptInterface."
                    )
                    javaScriptInterface(webView, initialUrl, processor, webKitAdapter)
                } else {
                    Logger.warn(
                        TAG,
                        "Auth UX bridge disabled because WebMessageListener rollback was incomplete."
                    )
                    processor.close()
                    disabled(webKitAdapter)
                }
            }
        }

        @VisibleForTesting
        fun getWebMessageListenerFacadeScript(): String = WEB_MESSAGE_LISTENER_FACADE_SCRIPT

        /**
         * Creates the navigation-managed `addJavascriptInterface` transport.
         *
         * The native interface is installed immediately only when [initialUrl] has an approved
         * Auth UX host. The session remains in [AuthUxTransportMode.JAVASCRIPT_INTERFACE] when the
         * initial URL is absent or untrusted so [onPageStarted] can add the interface after a later
         * navigation to an approved host, and remove it again when navigation leaves that host.
         */
        private fun javaScriptInterface(
            webView: WebView,
            initialUrl: String?,
            processor: AuthUxJavaScriptInterface,
            webKitAdapter: AuthUxWebKitAdapter
        ): AuthUxBridgeSession {
            // Track the actual installation state separately from the selected transport mode.
            val shouldAdd = AuthUxJavaScriptInterface.isValidUriForInterface(initialUrl)
            if (shouldAdd) {
                webView.addJavascriptInterface(processor, AuthUxJavaScriptInterface.getInterfaceName())
            }
            Logger.info(TAG, "Selected addJavascriptInterface Auth UX transport.")
            return AuthUxBridgeSession(
                mode = AuthUxTransportMode.JAVASCRIPT_INTERFACE,
                webView = webView,
                processor = processor,
                webKitAdapter = webKitAdapter,
                scriptHandler = null,
                listenerRegistered = false,
                javaScriptInterfaceAdded = shouldAdd
            )
        }

        private fun disabled(webKitAdapter: AuthUxWebKitAdapter): AuthUxBridgeSession =
            AuthUxBridgeSession(
                mode = AuthUxTransportMode.DISABLED,
                webView = null,
                processor = null,
                webKitAdapter = webKitAdapter,
                scriptHandler = null,
                listenerRegistered = false,
                javaScriptInterfaceAdded = false
            )
    }
}
