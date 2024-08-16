//  Copyright (c) Microsoft Corporation.
//  All rights reserved.
//
//  This code is licensed under the MIT License.
//
//  Permission is hereby granted, free of charge, to any person obtaining a copy
//  of this software and associated documentation files(the "Software"), to deal
//  in the Software without restriction, including without limitation the rights
//  to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
//  copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions :
//
//  The above copyright notice and this permission notice shall be included in
//  all copies or substantial portions of the Software.
//
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//  THE SOFTWARE.
package com.microsoft.identity.common.internal.fido

import android.os.Build
import com.microsoft.identity.common.java.constants.FidoConstants.Companion.WEBAUTHN_QUERY_PARAMETER_FIELD
import com.microsoft.identity.common.java.constants.FidoConstants.Companion.WEBAUTHN_QUERY_PARAMETER_VALUE
import com.microsoft.identity.common.java.logging.Logger
import java.util.AbstractMap

/**
 * A utility class to help with FIDO related operations.
 */
class FidoUtil {

    companion object {

        private val TAG: String = FidoUtil::class.simpleName.toString()

        /**
         * Updates the query string parameters with the WebAuthn capability parameter (or removes the parameter) if applicable.
         *
         * @param originalList The original list of query string parameters.
         * @param isWebAuthnCapable A boolean indicating whether the host app intends to be WebAuthn capable.
         */
        @JvmStatic
        fun updateWithOrDeleteWebAuthnParam(originalList: List<Map.Entry<String, String>>, isWebAuthnCapable: Boolean) : ArrayList<Map.Entry<String, String>> {
            val methodTag = "$TAG:UpdateWithOrDeleteWebAuthnParam"
            val webauthnParam = AbstractMap.SimpleEntry<String, String>(
                WEBAUTHN_QUERY_PARAMETER_FIELD,
                WEBAUTHN_QUERY_PARAMETER_VALUE
            )
            val result = ArrayList<Map.Entry<String, String>>(originalList)
            // Check the OS version. As of the time this is written, passkeys are only supported on devices that run Android 9 (API 28) or higher.
            // https://developer.android.com/identity/sign-in/credential-manager
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                Logger.info(
                    methodTag,
                    "Device is running on an Android version less than 9 (API 28), which is the minimum level for passkeys."
                )

                // If we don't want to add this query string param, then we should also remove other instances of it that might be already present from MSAL/OneAuth-MSAL.
                result.remove(webauthnParam)
            } else if (isWebAuthnCapable && !originalList.contains(webauthnParam)) {
                result.add(webauthnParam)
            }
            return result
        }
    }
}
