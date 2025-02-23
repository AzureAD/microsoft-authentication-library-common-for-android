package com.microsoft.identity.common.internal.providers.oauth2

import android.content.Context
import android.content.Intent
import com.microsoft.identity.common.java.ui.AuthorizationAgent

/**
 * Parameters for the authorization activity.
 *
 * @param context                    Android application context
 * @param authIntent                 Android intent used by the authorization activity to launch the specific implementation of authorization (BROWSER, EMBEDDED)
 * @param requestUrl                 The authorization request in URL format
 * @param redirectUri                The expected redirect URI associated with the authorization request
 * @param requestHeader             Additional HTTP headers included with the authorization request
 * @param authorizationAgent         The means by which authorization should be performed (EMBEDDED, WEBVIEW) NOTE: This should move to library configuration
 * @param clientId                  The client ID of the application making the request
 * @param webViewZoomEnabled         This parameter is specific to embedded and controls whether webview zoom is enabled... NOTE: Needs refactoring
 * @param webViewZoomControlsEnabled This parameter is specific to embedded and controls whether webview zoom controls are enabled... NOTE: Needs refactoring
 * @param sourceLibraryName                    Product name to be of library making the request
 * @param sourceLibraryVersion             Product version to be of library making the request
 */
data class AuthorizationActivityParameters @JvmOverloads constructor(
    val context: Context,
    val authIntent: Intent?,
    val requestUrl: String,
    val redirectUri: String,
    val requestHeader: HashMap<String, String>?,
    val authorizationAgent: AuthorizationAgent,
    val clientId: String? = null,
    val webViewZoomEnabled: Boolean = true,
    val webViewZoomControlsEnabled: Boolean = true,
    val sourceLibraryName : String? = null,
    val sourceLibraryVersion: String? = null
)
