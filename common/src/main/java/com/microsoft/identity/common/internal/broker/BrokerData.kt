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
package com.microsoft.identity.common.internal.broker

import android.content.Context
import com.microsoft.identity.common.BuildConfig
import com.microsoft.identity.common.adal.internal.AuthenticationConstants
import com.microsoft.identity.common.java.logging.Logger
import java.util.Collections

/**
 * Represents Package Name and signing certificate thumbprint of a broker app.
 *
 * @param packageName                   Package name of the app.
 * @param signingCertificateThumbprint  signing certificate thumbprint (SHA-512) of the app.
 *                                      This value is unique per signing key.
 */
data class BrokerData(val packageName : String,
                      val signingCertificateThumbprint : String) {

    override fun equals(other: Any?): Boolean {
        if (other !is BrokerData){
            return false
        }

        return packageName.equals(other.packageName, ignoreCase = true) &&
                signingCertificateThumbprint == other.signingCertificateThumbprint
    }

    // Auto generated by IDE.
    override fun hashCode(): Int {
        var result = packageName.hashCode()
        result = 31 * result + signingCertificateThumbprint.hashCode()
        return result
    }

    override fun toString(): String {
        return "$packageName::$signingCertificateThumbprint"
    }

    companion object {
        val TAG = BrokerData::class.simpleName

        /**
         * Determines if the debug brokers should be trusted or not.
         * This should only be set to true only during testing.
         */
        private var sShouldTrustDebugBrokers = BuildConfig.DEBUG

        @JvmStatic
        fun setShouldTrustDebugBrokers(value: Boolean) {
            val methodTag = "$TAG:setShouldTrustDebugBrokers"
            if (!BuildConfig.DEBUG && value) {
                Logger.warn(methodTag, "You are forcing to trust debug brokers in non-debug builds.")
            }
            sShouldTrustDebugBrokers = value
        }

        @JvmStatic
        fun getShouldTrustDebugBrokers(): Boolean {
            return sShouldTrustDebugBrokers
        }

        @JvmStatic
        val debugMicrosoftAuthenticator = BrokerData(
            AuthenticationConstants.Broker.AZURE_AUTHENTICATOR_APP_PACKAGE_NAME,
            AuthenticationConstants.Broker.AZURE_AUTHENTICATOR_APP_DEBUG_SIGNATURE_SHA512
        )

        @JvmStatic
        val prodMicrosoftAuthenticator = BrokerData(
            AuthenticationConstants.Broker.AZURE_AUTHENTICATOR_APP_PACKAGE_NAME,
            AuthenticationConstants.Broker.AZURE_AUTHENTICATOR_APP_RELEASE_SIGNATURE_SHA512
        )

        @JvmStatic
        val prodCompanyPortal = BrokerData(
            AuthenticationConstants.Broker.COMPANY_PORTAL_APP_PACKAGE_NAME,
            AuthenticationConstants.Broker.COMPANY_PORTAL_APP_RELEASE_SIGNATURE_SHA512
        )

        @JvmStatic
        val debugBrokerHost = BrokerData(
            AuthenticationConstants.Broker.BROKER_HOST_APP_PACKAGE_NAME,
            AuthenticationConstants.Broker.BROKER_HOST_APP_SIGNATURE_SHA512
        )

        @JvmStatic
        val debugMockCp = BrokerData(
            AuthenticationConstants.Broker.MOCK_CP_PACKAGE_NAME,
            AuthenticationConstants.Broker.MOCK_CP_SIGNATURE_SHA512
        )

        @JvmStatic
        val debugMockAuthApp = BrokerData(
            AuthenticationConstants.Broker.MOCK_AUTH_APP_PACKAGE_NAME,
            AuthenticationConstants.Broker.MOCK_AUTH_APP_SIGNATURE_SHA512
        )

        @JvmStatic
        val debugMockLtw = BrokerData(
            AuthenticationConstants.Broker.MOCK_LTW_PACKAGE_NAME,
            AuthenticationConstants.Broker.MOCK_LTW_SIGNATURE_SHA512
        )

        @JvmStatic
        val prodLTW = BrokerData(
            AuthenticationConstants.Broker.LTW_APP_PACKAGE_NAME,
            AuthenticationConstants.Broker.LTW_APP_SHA512_RELEASE_SIGNATURE
        )

        @JvmStatic
        val debugLTW = BrokerData(
            AuthenticationConstants.Broker.LTW_APP_PACKAGE_NAME,
            AuthenticationConstants.Broker.LTW_APP_SHA512_DEBUG_SIGNATURE
        )

        @JvmStatic
        val accountManagerBrokers: Set<String> =
            Collections.unmodifiableSet(object : HashSet<String>() {
                init {
                    add(AuthenticationConstants.Broker.AZURE_AUTHENTICATOR_APP_PACKAGE_NAME)
                    add(AuthenticationConstants.Broker.COMPANY_PORTAL_APP_PACKAGE_NAME)
                    add(AuthenticationConstants.Broker.BROKER_HOST_APP_PACKAGE_NAME)
                    add(AuthenticationConstants.Broker.MOCK_AUTH_APP_PACKAGE_NAME)
                    add(AuthenticationConstants.Broker.MOCK_CP_PACKAGE_NAME)
                }
            })

        @JvmStatic
        val debugBrokers: Set<BrokerData> =
            Collections.unmodifiableSet(object : HashSet<BrokerData>() {
                init {
                    add(debugMicrosoftAuthenticator)
                    add(debugLTW)
                    add(debugBrokerHost)
                    add(debugMockCp)
                    add(debugMockAuthApp)
                    add(debugMockLtw)
                }
            })

        @JvmStatic
        val prodBrokers: Set<BrokerData> =
            Collections.unmodifiableSet(object : HashSet<BrokerData>() {
                init {
                    add(prodMicrosoftAuthenticator)
                    add(prodCompanyPortal)
                    add(prodLTW)
                }
            })

        @JvmStatic
        val allBrokers: Set<BrokerData> =
            Collections.unmodifiableSet(object : HashSet<BrokerData>() {
                init {
                    addAll(debugBrokers)
                    addAll(prodBrokers)
                }
            })

        /**
         * Returns the list of known broker apps (which SDK should make requests to).
         * see [sShouldTrustDebugBrokers] for more info regarding testing.
         **/
        @JvmStatic
        fun getKnownBrokerApps() : Set<BrokerData> {
            return if (sShouldTrustDebugBrokers) allBrokers else prodBrokers
        }

        /**
         * Returns true if the owner of the [Context] is a broker app
         * which relies on AccountManager as a broker discovery mechanism.
         * */
        @JvmStatic
        fun isAccountManagerSupported(packageName: String): Boolean {
            return accountManagerBrokers.contains(packageName)
        }
    }
}
