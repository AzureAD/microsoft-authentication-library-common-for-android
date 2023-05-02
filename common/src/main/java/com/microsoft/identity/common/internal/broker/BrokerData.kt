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
import com.microsoft.identity.common.adal.internal.AuthenticationConstants
import com.microsoft.identity.common.java.exception.ClientException
import lombok.ToString
import java.util.*

/**
 * Represents Package Name and Signature Hash (hash of the app's signing certificate)
 * of a broker app.
 */
@ToString
data class BrokerData(val packageName : String,
                      val signatureHash : String) {

    override fun equals(other: Any?): Boolean {
        if (other !is BrokerData){
            return false
        }

        return packageName.equals(other.packageName, ignoreCase = true) &&
                signatureHash == other.signatureHash
    }

    override fun hashCode(): Int {
        var result = packageName.hashCode()
        result = 31 * result + signatureHash.hashCode()
        return result
    }

    companion object {
        val debugMicrosoftAuthenticator = BrokerData(
            AuthenticationConstants.Broker.AZURE_AUTHENTICATOR_APP_PACKAGE_NAME,
            AuthenticationConstants.Broker.AZURE_AUTHENTICATOR_APP_DEBUG_SIGNATURE
        )

        val prodMicrosoftAuthenticator = BrokerData(
            AuthenticationConstants.Broker.AZURE_AUTHENTICATOR_APP_PACKAGE_NAME,
            AuthenticationConstants.Broker.AZURE_AUTHENTICATOR_APP_RELEASE_SIGNATURE
        )

        val prodCompanyPortal = BrokerData(
            AuthenticationConstants.Broker.COMPANY_PORTAL_APP_PACKAGE_NAME,
            AuthenticationConstants.Broker.COMPANY_PORTAL_APP_RELEASE_SIGNATURE
        )

        val debugBrokerHost = BrokerData(
            AuthenticationConstants.Broker.BROKER_HOST_APP_PACKAGE_NAME,
            AuthenticationConstants.Broker.BROKER_HOST_APP_SIGNATURE
        )

        val debugBrokers: Set<BrokerData> =
            Collections.unmodifiableSet(object : HashSet<BrokerData>() {
                init {
                    add(debugMicrosoftAuthenticator)
                    add(debugBrokerHost)
                }
            })

        val prodBrokers: Set<BrokerData> =
            Collections.unmodifiableSet(object : HashSet<BrokerData>() {
                init {
                    add(prodMicrosoftAuthenticator)
                    add(prodCompanyPortal)
                }
            })

        val allBrokers: Set<BrokerData> =
            Collections.unmodifiableSet(object : HashSet<BrokerData>() {
                init {
                    addAll(debugBrokers)
                    addAll(prodBrokers)
                }
            })

        /**
         * Given a broker package name, verify its signature and return a BrokerData object.
         *
         * @throws ClientException an exception containing mismatch signature hashes as its error message.
         */
        @Throws(ClientException::class)
        fun getBrokerDataForBrokerApp(
            context: Context,
            brokerPackageName: String
        ): BrokerData {

            // Verify the signature to make sure that we're not binding to malicious apps.
            val validator = BrokerValidator(context)
            return BrokerData(
                brokerPackageName,
                validator.verifySignatureAndThrow(brokerPackageName)
            )
        }
    }
}
