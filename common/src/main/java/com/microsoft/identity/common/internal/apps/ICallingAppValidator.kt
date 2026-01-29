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
package com.microsoft.identity.common.internal.apps

import android.content.Context
import com.microsoft.identity.common.internal.util.PackageUtils
import com.microsoft.identity.common.java.broker.IAppIdentity
import com.microsoft.identity.common.java.exception.ClientException
import com.microsoft.identity.common.java.exception.ErrorStrings
import com.microsoft.identity.common.java.flighting.CommonFlight
import com.microsoft.identity.common.java.flighting.CommonFlightsManager
import com.microsoft.identity.common.logging.Logger

/**
 * Validates a calling app by UID and throws if it is not in the allowed list.
 */
interface ICallingAppValidator {

    companion object {
        private val TAG = ICallingAppValidator::class.simpleName
    }

    /**
     * A list of allowed app identities.
     */
    val allowedApps: Set<IAppIdentity>

    /**
     * The application [Context].
     */
    val context: Context

    /**
     * Validates the caller and throws a [com.microsoft.identity.common.java.exception.ClientException] if validation fails or the caller is unauthorized.
     **
     * @param callingUid The identifier of the caller
     * @throws com.microsoft.identity.common.java.exception.ClientException if the caller cannot be validated or is unauthorized.
     */
    @Throws(ClientException::class)
    fun requireAllowedCaller(callingUid: Int) {
        val callingAppPackageName =
            PackageUtils.getPackageName(context, callingUid) ?: throw ClientException(
                ErrorStrings.UNAUTHORIZED_CLIENT,
                ErrorStrings.APP_PACKAGE_NAME_NOT_FOUND
            )

        val allowedCallers: List<IAppIdentity> = allowedApps.filter {
            it.packageName.equals(callingAppPackageName, ignoreCase = true)
        }

        if (allowedCallers.isEmpty()) {
            throw ClientException(
                ErrorStrings.UNAUTHORIZED_CLIENT,
                "$callingAppPackageName is not in the list of allowed callers."
            )
        }
        val expectedThumbprints: Set<String> = allowedCallers
            .map { it.signingCertificateThumbprint }
            .toSet()

        val isSignedByKnownKeys = isSignedByKnownKeys(
            callingAppPackageName,
            expectedThumbprints
        )
        if (!isSignedByKnownKeys) {
            throw ClientException(
                ErrorStrings.UNAUTHORIZED_CLIENT,
                "${ErrorStrings.BROKER_APP_VERIFICATION_FAILED}: $callingAppPackageName"
            )
        }
    }

    /**
     * Validates that the calling app is signed by one of the expected signing keys.
     **
     * @param callingAppPackageName The package name of the calling app.
     * @param expectedThumbprints The set of expected signing certificate thumbprints.
     * @return true if the calling app is signed by one of the expected keys, false otherwise.
     */
    fun isSignedByKnownKeys(
        callingAppPackageName: String,
        expectedThumbprints: Set<String>
    ): Boolean {
        val methodTag = "$TAG:isSignedByKnownKeys"
        try {
            val signingCertificates =
                PackageUtils.readCertDataForApp(callingAppPackageName, context)
            PackageUtils.verifySignatureHash(
                signingCertificates,
                expectedThumbprints.iterator()
            )

            val requiresCertChainValidation = CommonFlightsManager.getFlightsProvider()
                .isFlightEnabled(CommonFlight.RE_ENABLE_VALIDATE_SIGNING_CERT_CHAIN_BROKER_APPS)
            // Removing the outdated check, but we can bring it back with a feature flag.
            if (requiresCertChainValidation) {
                // Perform the certificate chain validation. If there is only one cert returned,
                // no need to perform certificate chain validation.
                if (signingCertificates.size > 1) {
                    PackageUtils.verifyCertificateChain(signingCertificates)
                }
            }
            return true
        } catch (throwable: Throwable) {
            Logger.verbose(
                methodTag,
                "$callingAppPackageName signature validation failed. ${throwable.message}"
            )
            return false
        }
    }
}
