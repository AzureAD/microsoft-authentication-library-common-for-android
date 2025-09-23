package com.microsoft.identity.common.internal.providers.oauth2

import android.annotation.SuppressLint
import android.app.Activity
import android.net.Uri
import android.webkit.WebView
import androidx.webkit.JavaScriptReplyProxy
import androidx.webkit.WebMessageCompat
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewCompat.WebMessageListener
import androidx.webkit.WebViewFeature
import com.microsoft.identity.common.logging.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob


object WebViewMessageListener {

    @SuppressLint("RequiresFeature")
    private val DEFAULT_LISTENER =
        WebMessageListener { view: WebView?, message: WebMessageCompat, sourceOrigin: Uri?, isMainFrame: Boolean, replyProxy: JavaScriptReplyProxy ->
            // Handle messages coming from JS
            val data = message.data
            // Example: log or send back a reply
            replyProxy.postMessage("Android received: $data")
        }

    fun setup(webView: WebView, activity: Activity) {
        val methodTag = "WebViewMessageListener:setup"
        Logger.info(methodTag, "Setting up WebView message listener")
        WebView.setWebContentsDebuggingEnabled(true)
        val coroutineScope = CoroutineScope(Dispatchers.Default)
        //val coroutineScope = CoroutineScope(kotlinx.coroutines.Dispatchers.IO + SupervisorJob())
        val credentialManagerHandler = CredentialManagerHandler(activity)

        val rules = setOf("*")
        if (WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)) {
            Logger.info(methodTag, "WEB_MESSAGE_LISTENER supported on this device/WebView.")
            // Add listener for all origins — you can restrict to specific origins instead
            WebViewCompat.addWebMessageListener(
                webView,
                PasskeyWebListener.INTERFACE_NAME,
                rules,
                PasskeyWebListener(activity,coroutineScope, credentialManagerHandler)
            )
        } else {
            // Fallback if feature not supported
            Logger.warn(methodTag, "WEB_MESSAGE_LISTENER not supported on this device/WebView.")
        }
    }
}
