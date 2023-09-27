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

package com.microsoft.identity.common.java.authorities

import com.microsoft.identity.common.java.exception.ClientException
import com.microsoft.identity.common.java.logging.LogSession
import com.microsoft.identity.common.java.logging.Logger
import com.microsoft.identity.common.java.providers.nativeauth.NativeAuthConstants
import com.microsoft.identity.common.java.providers.nativeauth.NativeAuthOAuth2Configuration
import com.microsoft.identity.common.java.providers.nativeauth.NativeAuthOAuth2Strategy
import com.microsoft.identity.common.java.providers.nativeauth.NativeAuthOAuth2StrategyFactory
import com.microsoft.identity.common.java.providers.oauth2.OAuth2StrategyParameters

/**
 * NativeAuthCIAMAuthority class creates a custom authority for native auth flows.
 */
class NativeAuthCIAMAuthority (
    private val authorityUrl: String,
    val clientId: String
) : CIAMAuthority(authorityUrl) {
    companion object {
        private val TAG = NativeAuthCIAMAuthority::class.java.simpleName
        //This parameter ensures that authorization endpoint is not fetched from OpenId Configuration.
        private const val NATIVE_AUTH_USE_OPENID_CONFIGURATION = false

        @Throws(Exception::class)
        fun getAuthorityFromAuthorityUrl(authorityUrl: String, clientId: String):
                NativeAuthCIAMAuthority {
            // Piggy back on the existing authority creation to improve reliability.
            val authority = Authority.getAuthorityFromAuthorityUrl(authorityUrl)

            if (authority is NativeAuthCIAMAuthority) {
                // Authority is already a NativeAuthCIAMAuthority
                return authority as NativeAuthCIAMAuthority
            }
            else if (authority is CIAMAuthority) {
                // Authority returned was a base CIAM authority
                NativeAuthCIAMAuthority(
                    authorityUrl = authority.authorityUri.toString(),
                    clientId = clientId
                )
            }

            //The authority created above is neither NativeAuthCIAMAuthority nor CIAMAuthority
            throw ClientException(ClientException.NATIVE_AUTH_INVALID_CIAM_AUTHORITY)
        }
    }


    // TODO audience, slice, flight parameters, multiple clouds supported,
    // isAuthorityHostValidationEnabled (AzureActiveDirectoryOAuth2Configuration). Consider extending
    // AzureActiveDirectoryAuthority
    init {
        mAuthorityTypeString = "AAD_NA" // AAD Native Auth
        mAuthorityUrlString = authorityUrl
    }

    private fun createNativeAuthOAuth2Configuration(challengeTypes: List<String>?): NativeAuthOAuth2Configuration {
       LogSession.logMethodCall(TAG, "${TAG}.createNativeAuthOAuth2Configuration")
        return NativeAuthOAuth2Configuration(
            authorityUrl = this.authorityURL,
            clientId = this.clientId,
            challengeType = getChallengeTypesWithDefault(challengeTypes)
        )
    }

    /**
     * Challenge types represent different authentication flows supported by the auth server and
     * client application.
     * e.g. "password" challenge type requires a user to supply username and password. "oob" challenge
     * type requires user to submit a code sent to them via out of band mechanism.
     * "redirect" is added by the SDK as a default challenge type, as the server always expects
     * this. Duplicates are removed, and the list is then converted in a whitespace separated string
     * (e.g. "oob password redirect")
     */
    private fun getChallengeTypesWithDefault(challengeTypes: List<String>?): String {
        LogSession.logMethodCall(TAG, "${TAG}.getChallengeTypesWithDefault")
        Logger.info(TAG, "Challenge Types passed = $challengeTypes")
        return (challengeTypes ?: emptyList()).plus(listOf(NativeAuthConstants.GrantType.REDIRECT)).distinct().joinToString(" ")
    }

    @Throws(ClientException::class)
    override fun createOAuth2Strategy(parameters: OAuth2StrategyParameters): NativeAuthOAuth2Strategy {
        val config = createNativeAuthOAuth2Configuration(parameters.mChallengeTypes)

        // CIAM Authorities fetch endpoints from open id configuration, communicate that to
        // strategy through parameters
        parameters.setUsingOpenIdConfiguration(NATIVE_AUTH_USE_OPENID_CONFIGURATION)

        return NativeAuthOAuth2StrategyFactory.createStrategy(
            config = config,
            strategyParameters = parameters,
        )
    }
}
