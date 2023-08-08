package com.microsoft.identity.common.internal.fido

import android.webkit.WebView
import com.microsoft.identity.common.internal.ui.webview.challengehandlers.IChallengeHandler
import com.microsoft.identity.common.java.opentelemetry.IFidoTelemetryHelper

/**
 * Abstract class that handles a FidoChallenge.
 */
abstract class AbstractFidoChallengeHandler
/**
 * Constructs an AbstractFidoChallengeHandler.
 * @param webView current WebView.
 * @param telemetryHelper IFidoTelemetryHelper instance.
 */ (
    val webView: WebView,
    val telemetryHelper: IFidoTelemetryHelper
) : IChallengeHandler<IFidoChallenge, Void>