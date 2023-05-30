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
package com.microsoft.identity.common.internal.activebrokerdiscovery

import android.accounts.AccountManager
import android.content.Context
import com.microsoft.identity.common.internal.broker.BrokerData
import com.microsoft.identity.common.internal.broker.BrokerValidator
import com.microsoft.identity.common.java.AuthenticationConstants.Broker.BROKER_ACCOUNT_TYPE
import com.microsoft.identity.common.logging.Logger

class LegacyBrokerDiscoveryClient(val context: Context): IBrokerDiscoveryClient {

    companion object {
        private val TAG = LegacyBrokerDiscoveryClient::class.simpleName

        /**
         * Determines which app is the broker based on having the work account registration in Account Manager.
         *
         * Known issue: When we're in an AccountManager callback (Especially on older Android devices, i.e. Android 10)
         * Android For Work throws a SecurityException when we're calling AccountManager.getAuthenticatorTypes()
         *
         * E.g. Company Portal main process can call this freely, broker process can call this freely,
         * but once running in AccountManager on the work profile (user != 0),
         * apparently sometimes it tries to get accounts from user 0 (personal profile) and fails.
         *
         * @return Package Name of the broker
         */
        private fun getActiveBrokerPackageName(context: Context): String? {
            val methodTag = "$TAG:getActiveBrokerPackageName"

            val authenticators = AccountManager.get(context).authenticatorTypes

            Logger.info(methodTag, "${authenticators.size} Authenticators registered.")
            val brokerValidator = BrokerValidator(context)
            for (authenticator in authenticators) {
                Logger.info(methodTag,
                    "Authenticator: ${authenticator.packageName} type: ${authenticator.type}")
                if (BROKER_ACCOUNT_TYPE.equals(authenticator.type.trim { it <= ' ' }, ignoreCase = true)) {
                    Logger.info(methodTag, "Verify: " + authenticator.packageName)
                    brokerValidator.verifySignatureAndThrow(authenticator.packageName)
                    return authenticator.packageName
                }
            }

            Logger.info(methodTag,
                "None of the authenticators, is type: $BROKER_ACCOUNT_TYPE")
            return null
        }
    }

    override fun getActiveBroker(shouldSkipCache: Boolean): BrokerData? {
        val methodTag = "$TAG:getActiveBroker"
        return try {
            val activeBrokerPkgName =
                getActiveBrokerPackageName(context) ?:
                return null

            BrokerData.getKnownBrokerApps().firstOrNull {
                it.packageName == activeBrokerPkgName
            }
        } catch (e: Exception) {
            Logger.error(methodTag, "Failed to get active broker", e)
            null
        }
    }

}