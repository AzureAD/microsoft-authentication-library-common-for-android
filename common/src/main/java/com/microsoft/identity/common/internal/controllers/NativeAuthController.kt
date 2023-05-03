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
package com.microsoft.identity.common.internal.controllers

import com.microsoft.identity.common.internal.util.CommandUtil
import com.microsoft.identity.common.java.AuthenticationConstants
import com.microsoft.identity.common.java.cache.ICacheRecord
import com.microsoft.identity.common.java.commands.parameters.nativeauth.BaseSignInStartCommandParameters
import com.microsoft.identity.common.java.commands.parameters.nativeauth.SignInResendCodeCommandParameters
import com.microsoft.identity.common.java.commands.parameters.nativeauth.SignInStartCommandParameters
import com.microsoft.identity.common.java.commands.parameters.nativeauth.SignInStartWithPasswordCommandParameters
import com.microsoft.identity.common.java.commands.parameters.nativeauth.SignInSubmitCodeCommandParameters
import com.microsoft.identity.common.java.controllers.results.Complete
import com.microsoft.identity.common.java.controllers.results.EmailVerificationRequired
import com.microsoft.identity.common.java.controllers.results.IncorrectCode
import com.microsoft.identity.common.java.controllers.results.InvalidAuthenticationType
import com.microsoft.identity.common.java.controllers.results.PasswordIncorrect
import com.microsoft.identity.common.java.controllers.results.Redirect
import com.microsoft.identity.common.java.controllers.results.SignInResendCodeCommandResult
import com.microsoft.identity.common.java.controllers.results.SignInStartCommandResult
import com.microsoft.identity.common.java.controllers.results.SignInSubmitCodeCommandResult
import com.microsoft.identity.common.java.controllers.results.UnknownError
import com.microsoft.identity.common.java.controllers.results.UserNotFound
import com.microsoft.identity.common.java.logging.Logger
import com.microsoft.identity.common.java.providers.microsoft.microsoftsts.MicrosoftStsAuthorizationRequest
import com.microsoft.identity.common.java.providers.microsoft.microsoftsts.MicrosoftStsOAuth2Strategy
import com.microsoft.identity.common.java.providers.nativeauth.NativeAuthOAuth2Strategy
import com.microsoft.identity.common.java.providers.nativeauth.responses.signin.SignInChallengeApiResult
import com.microsoft.identity.common.java.providers.nativeauth.responses.signin.SignInInitiateApiResult
import com.microsoft.identity.common.java.providers.nativeauth.responses.signin.SignInTokenApiResult
import com.microsoft.identity.common.java.providers.oauth2.OAuth2StrategyParameters
import com.microsoft.identity.common.java.request.SdkType
import com.microsoft.identity.common.java.result.LocalAuthenticationResult
import com.microsoft.identity.common.java.util.StringUtil
import lombok.EqualsAndHashCode
import java.net.URL

/**
 * The implementation of MSAL Controller for Native Authentication.
 */
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
class NativeAuthController : BaseNativeAuthController() {

    companion object {
        private val TAG = NativeAuthController::class.java.simpleName
    }

    fun signInStart(parameters: BaseSignInStartCommandParameters): SignInStartCommandResult {
        val methodTag = "$TAG:signInStart"

        Logger.verbose(
            methodTag,
            "Performing sign-in start..."
        )
        try {
            val strategyParameters = OAuth2StrategyParameters.builder()
                .platformComponents(parameters.platformComponents)
                .build()

            val oAuth2Strategy = parameters
                .authority
                .createOAuth2Strategy(strategyParameters)

            val isROPCCall = parameters is SignInStartWithPasswordCommandParameters

            return if (isROPCCall) {
                signInStartROPC(parameters as SignInStartWithPasswordCommandParameters, oAuth2Strategy)
            } else {
                signInStartNonROPC(parameters as SignInStartCommandParameters, oAuth2Strategy)
            }
            // TODO add case for use of signInSLT
        } catch (e: Exception) {
            Logger.error(
                methodTag,
                "Error occurred while performing sign-in start",
                e
            )
            throw e
        }
    }

    private fun signInStartROPC(
        parameters: SignInStartWithPasswordCommandParameters,
        oAuth2Strategy: NativeAuthOAuth2Strategy,
    ): SignInStartCommandResult {
        val methodTag = "$TAG:signInRocp"

        Logger.verbose(
            methodTag,
            "ROPC flow initiated"
        )

        val mergedScopes = addDefaultScopes(parameters.scopes)
        val parametersWithScopes = CommandUtil.createSignInStartWithPasswordCommandParametersWithScopes(
            parameters,
            mergedScopes
        )

        val tokenApiResult = performROPCTokenRequest(
            oAuth2Strategy = oAuth2Strategy,
            parameters = parametersWithScopes
        )
        when (tokenApiResult) {
            is SignInTokenApiResult.CredentialRequired -> {
                val challengeApiResult = performSignInChallengeCall(
                    oAuth2Strategy = oAuth2Strategy,
                    credentialToken = tokenApiResult.credentialToken
                )
                return processSignInChallengeApiResult(challengeApiResult)
            }
            SignInTokenApiResult.Redirect -> {
                return Redirect
            }
            is SignInTokenApiResult.Success -> {
                val records: List<ICacheRecord> = saveTokens(
                    oAuth2Strategy as MicrosoftStsOAuth2Strategy,
                    createAuthorizationRequest(
                        strategy = oAuth2Strategy,
                        scopes = parametersWithScopes.scopes,
                        clientId = parametersWithScopes.clientId
                    ),
                    tokenApiResult.tokenResponse,
                    parametersWithScopes.oAuth2TokenCache
                )

                // The first element in the returned list is the item we *just* saved, the rest of
                // the elements are necessary to construct the full IAccount + TenantProfile
                val newestRecord = records[0]

                return Complete(
                    authenticationResult = LocalAuthenticationResult(
                        finalizeCacheRecordForResult(
                            newestRecord,
                            parametersWithScopes.authenticationScheme
                        ),
                        records,
                        SdkType.MSAL,
                        false
                    )
                )
            }
            is SignInTokenApiResult.UnknownError -> {
                return UnknownError(
                    errorCode = tokenApiResult.error,
                    errorDescription = tokenApiResult.errorDescription
                )
            }
            is SignInTokenApiResult.PasswordIncorrect -> {
                return PasswordIncorrect(
                    errorCode = tokenApiResult.error,
                    errorDescription = tokenApiResult.errorDescription
                )
            }
            is SignInTokenApiResult.UserNotFound -> {
                return UserNotFound(
                    errorCode = tokenApiResult.error,
                    errorDescription = tokenApiResult.errorDescription
                )
            }
            is SignInTokenApiResult.CodeIncorrect -> {
                // TODO add correlation ID https://identitydivision.visualstudio.com/Engineering/_workitems/edit/2503124
                Logger.warn(
                    TAG,
                    "Unexpected API result: $tokenApiResult",
                )
                return UnknownError(
                    errorCode = "unexpected_api_result",
                    errorDescription = "API returned unexpected result: $tokenApiResult"
                )
            }
        }
    }

    private fun signInStartNonROPC(
        parameters: SignInStartCommandParameters,
        oAuth2Strategy: NativeAuthOAuth2Strategy,
    ): SignInStartCommandResult {
        val methodTag = "$TAG:signInStartNonROPC"

        Logger.verbose(
            methodTag,
            "non-ROPC flow initiated"
        )

        val mergedScopes = addDefaultScopes(parameters.scopes)
        val parametersWithScopes = CommandUtil.createSignInStartCommandParametersWithScopes(
            parameters,
            mergedScopes
        )

        val initiateApiResult = performSignInInitiateCall(
            oAuth2Strategy = oAuth2Strategy,
            parameters = parametersWithScopes
        )
        when (initiateApiResult) {
            SignInInitiateApiResult.Redirect -> {
                return Redirect
            }
            is SignInInitiateApiResult.Success -> {
                val challengeApiResult = performSignInChallengeCall(
                    oAuth2Strategy = oAuth2Strategy,
                    credentialToken = initiateApiResult.credentialToken
                )
                return processSignInChallengeApiResult(challengeApiResult)
            }
            is SignInInitiateApiResult.UnknownError -> {
                return UnknownError(
                    errorCode = initiateApiResult.error,
                    errorDescription = initiateApiResult.errorDescription
                )
            }
            is SignInInitiateApiResult.UserNotFound -> {
                return UserNotFound(
                    errorCode = initiateApiResult.error,
                    errorDescription = initiateApiResult.errorDescription
                )
            }
        }
    }

    fun signInSubmitCode(parameters: SignInSubmitCodeCommandParameters): SignInSubmitCodeCommandResult {
        val methodTag = "$TAG:signInSubmitCode"

        Logger.verbose(
            methodTag,
            "Performing sign-in submit code..."
        )
        try {
            // Add default scopes
            val mergedScopes: List<String> = addDefaultScopes(parameters.scopes)

            val parametersWithScopes = CommandUtil.createSignInSubmitCodeCommandParametersWithScopes(
                parameters,
                mergedScopes
            )

            val strategyParameters = OAuth2StrategyParameters.builder()
                .platformComponents(parameters.platformComponents)
                .build()

            val oAuth2Strategy = parametersWithScopes
                .authority
                .createOAuth2Strategy(strategyParameters)

            val tokenApiResult = performOOBTokenRequest(
                oAuth2Strategy = oAuth2Strategy,
                parameters = parametersWithScopes
            )
            return when (tokenApiResult) {
                SignInTokenApiResult.Redirect -> {
                    Redirect
                }
                is SignInTokenApiResult.Success -> {
                    val records: List<ICacheRecord> = saveTokens(
                        oAuth2Strategy as MicrosoftStsOAuth2Strategy,
                        createAuthorizationRequest(
                            strategy = oAuth2Strategy,
                            scopes = parametersWithScopes.scopes,
                            clientId = parametersWithScopes.clientId
                        ),
                        tokenApiResult.tokenResponse,
                        parametersWithScopes.oAuth2TokenCache
                    )

                    // The first element in the returned list is the item we *just* saved, the rest of
                    // the elements are necessary to construct the full IAccount + TenantProfile
                    val newestRecord = records[0]

                    return Complete(
                        authenticationResult = LocalAuthenticationResult(
                            finalizeCacheRecordForResult(
                                newestRecord,
                                parametersWithScopes.authenticationScheme
                            ),
                            records,
                            SdkType.MSAL,
                            false
                        )
                    )
                }
                is SignInTokenApiResult.CodeIncorrect -> {
                    return IncorrectCode(
                        errorCode = tokenApiResult.error,
                        errorDescription = tokenApiResult.errorDescription
                    )
                }
                is SignInTokenApiResult.UnknownError -> {
                    return UnknownError(
                        errorCode = tokenApiResult.error,
                        errorDescription = tokenApiResult.errorDescription
                    )
                }
                is SignInTokenApiResult.CredentialRequired, is SignInTokenApiResult.PasswordIncorrect,
                is SignInTokenApiResult.UserNotFound -> {
                    // TODO add correlation ID https://identitydivision.visualstudio.com/Engineering/_workitems/edit/2503124
                    Logger.warn(
                        TAG,
                        "Unexpected API result: $tokenApiResult",
                    )
                    return UnknownError(
                        errorCode = "unexpected_api_result",
                        errorDescription = "API returned unexpected result: $tokenApiResult"
                    )
                }
            }
        } catch (e: Exception) {
            Logger.error(
                methodTag,
                "Error occurred while performing sign-in start",
                e
            )
            throw e
        }
    }

    fun signInResendCode(parameters: SignInResendCodeCommandParameters): SignInResendCodeCommandResult {
        val methodTag = "$TAG:signInResendCode"

        Logger.verbose(
            methodTag,
            "Performing sign-in resend code..."
        )
        try {
            val strategyParameters = OAuth2StrategyParameters.builder()
                .platformComponents(parameters.platformComponents)
                .build()

            val oAuth2Strategy = parameters
                .authority
                .createOAuth2Strategy(strategyParameters)

            val result = performSignInChallengeCall(
                oAuth2Strategy = oAuth2Strategy,
                credentialToken = parameters.credentialToken
            )
            return when (result) {
                is SignInChallengeApiResult.OOBRequired -> {
                    EmailVerificationRequired(
                        credentialToken = result.credentialToken,
                        codeLength = result.codeLength,
                        displayName = result.challengeTargetLabel
                    )
                }
                is SignInChallengeApiResult.PasswordRequired -> {
                    // TODO add correlation ID https://identitydivision.visualstudio.com/Engineering/_workitems/edit/2503124
                    UnknownError(
                        errorCode = "unexpected_password_required",
                        errorDescription = "API returned unexpected result: password required"
                    )
                }
                SignInChallengeApiResult.Redirect -> {
                    Redirect
                }
                is SignInChallengeApiResult.UnknownError -> {
                    UnknownError(
                        errorCode = result.error,
                        errorDescription = result.errorDescription
                    )
                }
            }
        } catch (e: Exception) {
            Logger.error(
                methodTag,
                "Error occurred while performing sign-in start",
                e
            )
            throw e
        }
    }

    private fun performROPCTokenRequest(
        oAuth2Strategy: NativeAuthOAuth2Strategy,
        parameters: SignInStartWithPasswordCommandParameters
    ): SignInTokenApiResult {
        return oAuth2Strategy.performROPCTokenRequest(
            parameters = parameters
        )
    }

    private fun performOOBTokenRequest(
        oAuth2Strategy: NativeAuthOAuth2Strategy,
        parameters: SignInSubmitCodeCommandParameters
    ): SignInTokenApiResult {
        return oAuth2Strategy.performOOBTokenRequest(
            parameters = parameters
        )
    }

    private fun performSignInInitiateCall(
        oAuth2Strategy: NativeAuthOAuth2Strategy,
        parameters: SignInStartCommandParameters,
    ): SignInInitiateApiResult {
        return oAuth2Strategy.performSignInInitiate(
            parameters = parameters
        )
    }

    private fun performSignInChallengeCall(
        oAuth2Strategy: NativeAuthOAuth2Strategy,
        credentialToken: String
    ): SignInChallengeApiResult {
        return oAuth2Strategy.performSignInChallenge(credentialToken = credentialToken)
    }

    private fun processSignInChallengeApiResult(signInChallengeApiResult: SignInChallengeApiResult): SignInStartCommandResult {
        return when (signInChallengeApiResult) {
            is SignInChallengeApiResult.OOBRequired -> {
                EmailVerificationRequired(
                    credentialToken = signInChallengeApiResult.credentialToken,
                    codeLength = signInChallengeApiResult.codeLength,
                    displayName = signInChallengeApiResult.challengeTargetLabel
                )
            }
            is SignInChallengeApiResult.PasswordRequired -> {
                InvalidAuthenticationType
            }
            SignInChallengeApiResult.Redirect -> {
                Redirect
            }
            is SignInChallengeApiResult.UnknownError -> {
                UnknownError(
                    errorCode = signInChallengeApiResult.error,
                    errorDescription = signInChallengeApiResult.errorDescription
                )
            }
        }
    }

    private fun createAuthorizationRequest(
        strategy: NativeAuthOAuth2Strategy,
        scopes: List<String>,
        clientId: String
    ): MicrosoftStsAuthorizationRequest {
        val builder = MicrosoftStsAuthorizationRequest.Builder()
        builder.setAuthority(URL(strategy.getAuthority()))
        builder.setClientId(clientId)
        builder.setScope(StringUtil.join(" ", scopes))
        return builder.build()
    }

    private fun addDefaultScopes(scopes: List<String>?): List<String> {
        val requestScopes = scopes?.toMutableList() ?: mutableListOf<String>()
        requestScopes.addAll(AuthenticationConstants.DEFAULT_SCOPES)
        // sanitize empty and null scopes
        requestScopes.removeAll(listOf("", null))
        return requestScopes.toList()
    }
}
