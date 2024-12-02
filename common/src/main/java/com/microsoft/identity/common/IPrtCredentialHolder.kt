package com.microsoft.identity.common;

import android.app.Activity

/**
 * Consumer of commons needs to implement [IPrtCredentialHolder] interface
 * and set it using CommonPrtCredentialHolder.initializeCommonPrtCredentialHolder(@NonNull IPrtCredentialHolder prtCredentialHolder)
 * to provide prtCredentialHolder to common module.
 */
interface IPrtCredentialHolder {

    /**
     * Gets refresh token credential using nonce retrieved from webview.
     */
    fun getRefreshTokenCredentialUsingNewNonce(authorityStr : String, username : String, nonce : String, prtHeader : String, activity : Activity) : String?
}
