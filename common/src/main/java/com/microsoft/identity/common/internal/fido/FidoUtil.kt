package com.microsoft.identity.common.internal.fido

import android.os.Build
import com.microsoft.identity.common.java.constants.FidoConstants.Companion.WEBAUTHN_QUERY_PARAMETER_FIELD
import com.microsoft.identity.common.java.constants.FidoConstants.Companion.WEBAUTHN_QUERY_PARAMETER_VALUE
import com.microsoft.identity.common.java.logging.Logger
import java.util.AbstractMap

class FidoUtil {

    companion object {

        private val TAG: String = FidoUtil::class.simpleName.toString()

        fun UpdateWithOrDeleteWebAuthnParam(originalList: List<Map<String, String>>, isWebAuthnCapable: Boolean) {
            val methodTag = "$TAG:UpdateWithOrDeleteWebAuthnParam"
            val webauthnParam = AbstractMap.SimpleEntry<String, String>(
                WEBAUTHN_QUERY_PARAMETER_FIELD,
                WEBAUTHN_QUERY_PARAMETER_VALUE
            )


            // Check the OS version. As of the time this is written, passkeys are only supported on devices that run Android 9 (API 28) or higher.
            // https://developer.android.com/identity/sign-in/credential-manager
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                Logger.info(
                    methodTag,
                    "Device is running on an Android version less than 9 (API 28), which is the minimum level for passkeys."
                )

                // If we don't want to add this query string param, then we should also remove other instances of it that might be already present from MSAL/OneAuth-MSAL.
                //queryParams.remove(webauthnParam)
            }

        }
    }
}