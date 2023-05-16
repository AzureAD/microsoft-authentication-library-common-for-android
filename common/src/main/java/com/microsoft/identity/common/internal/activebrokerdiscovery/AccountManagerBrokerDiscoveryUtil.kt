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
import android.accounts.AuthenticatorDescription
import android.content.Context
import com.microsoft.identity.common.internal.broker.BrokerData
import com.microsoft.identity.common.java.AuthenticationConstants
import com.microsoft.identity.common.java.exception.ClientException
import com.microsoft.identity.common.java.logging.Logger

/**
 * A class for getting active broker from [AccountManager]
 *
 * @param knownBrokerApps       apps that are known to ship broker.
 * @param isSignedByKnownKeys   a method for validating if that the installed counterpart of the given [BrokerData] is
 *                              signed by known keys.
 * @param getAccountManagerApps a method for returning apps which owns [AccountManager] accounts.
 **/
class AccountManagerBrokerDiscoveryUtil(private val knownBrokerApps: Set<BrokerData>,
                                        private val isSignedByKnownKeys: (BrokerData) -> Boolean,
                                        private val getAccountManagerApps: () -> Array<AuthenticatorDescription>
) {

    companion object {
        val TAG = AccountManagerBrokerDiscoveryUtil::class.simpleName
    }

    constructor(context: Context): this(
        knownBrokerApps = BrokerData.getKnownBrokerApps(),
        isSignedByKnownKeys = { brokerData ->
            BrokerData.isSignedByKnownKeys(brokerData, context)
        },
        getAccountManagerApps = {
            AccountManager.get(context).authenticatorTypes
        }
    )

    /**
     * Returns the owner of [AuthenticationConstants.Broker.BROKER_ACCOUNT_TYPE], which we (previously)
     * use to determine the active broker (if installed).
     **/
    fun getActiveBrokerFromAccountManager() : BrokerData? {
        val methodTag = "${TAG}:getActiveBrokerFromAccountManager"

        val authenticators = try {
            getAccountManagerApps()
        } catch (t: Throwable) {
            throw ClientException(ClientException.ACCOUNT_MANAGER_FAILED, t.message)
        }

        Logger.info(methodTag, "${authenticators.size} Authenticators registered.")

        authenticators.forEach { authenticator ->
            val packageName = authenticator.packageName.trim()
            val accountType = authenticator.type.trim()
            Logger.info(methodTag, "Authenticator: $packageName type: $accountType")

            if (AuthenticationConstants.Broker.BROKER_ACCOUNT_TYPE.equals(accountType, ignoreCase = true)) {
                Logger.info(methodTag, "Verify: $packageName")

                val brokerData = knownBrokerApps.find {
                    it.packageName.equals(packageName, ignoreCase = true)
                }

                if (brokerData?.let { isSignedByKnownKeys(it) } == true) {
                    return brokerData
                }
            }
        }

        Logger.info(methodTag, "No valid broker is found")
        return null
    }
}