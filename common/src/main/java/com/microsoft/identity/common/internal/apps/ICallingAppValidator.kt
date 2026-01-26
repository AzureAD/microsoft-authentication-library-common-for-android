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
                ErrorStrings.BROKER_APP_VERIFICATION_FAILED
            )

        val allowedCallers: List<IAppIdentity> = allowedApps.filter {
            it.packageName.equals(callingAppPackageName, ignoreCase = true)
        }

        if (allowedCallers.isEmpty()) {
            throw ClientException(
                ErrorStrings.UNAUTHORIZED_CLIENT,
                "${ErrorStrings.BROKER_APP_VERIFICATION_FAILED}: $callingAppPackageName"
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

    fun isSignedByKnownKeys(
        callingAppPackageName: String,
        expectedThumbprints: Set<String>
    ): Boolean {
        val methodTag = "$TAG:isSignedByKnownKeys"
        try {
            //getSigningCertificateForApp
            val signingCertificates =
                PackageUtils.readCertDataForApp(callingAppPackageName, context)
            //validateSigningCertificate
            PackageUtils.verifySignatureHash(
                signingCertificates,
                expectedThumbprints.iterator()
            )

            val requiresCertChainValidation = CommonFlightsManager.getFlightsProvider()
                .isFlightEnabled(CommonFlight.RE_ENABLE_VALIDATE_SIGNING_CERT_CHAIN_BROKER_APPS)
            // Removing the outdated check, but we can b`ring it back with a feature flag.
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