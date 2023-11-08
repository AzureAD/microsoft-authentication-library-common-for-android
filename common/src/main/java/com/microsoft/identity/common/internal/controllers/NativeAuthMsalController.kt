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

import androidx.annotation.VisibleForTesting
import com.microsoft.identity.common.internal.commands.RefreshOnCommand
import com.microsoft.identity.common.internal.telemetry.Telemetry
import com.microsoft.identity.common.internal.telemetry.events.ApiEndEvent
import com.microsoft.identity.common.internal.util.CommandUtil
import com.microsoft.identity.common.java.AuthenticationConstants
import com.microsoft.identity.common.java.cache.ICacheRecord
import com.microsoft.identity.common.java.commands.parameters.SilentTokenCommandParameters
import com.microsoft.identity.common.java.commands.parameters.nativeauth.AcquireTokenNoFixedScopesCommandParameters
import com.microsoft.identity.common.java.commands.parameters.nativeauth.BaseNativeAuthCommandParameters
import com.microsoft.identity.common.java.commands.parameters.nativeauth.BaseSignInTokenCommandParameters
import com.microsoft.identity.common.java.commands.parameters.nativeauth.SignInResendCodeCommandParameters
import com.microsoft.identity.common.java.commands.parameters.nativeauth.SignInStartCommandParameters
import com.microsoft.identity.common.java.commands.parameters.nativeauth.SignInStartUsingPasswordCommandParameters
import com.microsoft.identity.common.java.commands.parameters.nativeauth.SignInSubmitCodeCommandParameters
import com.microsoft.identity.common.java.commands.parameters.nativeauth.SignInSubmitPasswordCommandParameters
import com.microsoft.identity.common.java.commands.parameters.nativeauth.SignInWithSLTCommandParameters
import com.microsoft.identity.common.java.configuration.LibraryConfiguration
import com.microsoft.identity.common.java.controllers.CommandDispatcher
import com.microsoft.identity.common.java.controllers.results.INativeAuthCommandResult
import com.microsoft.identity.common.java.controllers.results.SignInCommandResult
import com.microsoft.identity.common.java.controllers.results.SignInResendCodeCommandResult
import com.microsoft.identity.common.java.controllers.results.SignInStartCommandResult
import com.microsoft.identity.common.java.controllers.results.SignInSubmitCodeCommandResult
import com.microsoft.identity.common.java.controllers.results.SignInSubmitPasswordCommandResult
import com.microsoft.identity.common.java.controllers.results.SignInWithSLTCommandResult
import com.microsoft.identity.common.java.dto.AccountRecord
import com.microsoft.identity.common.java.eststelemetry.PublicApiId
import com.microsoft.identity.common.java.exception.ArgumentException
import com.microsoft.identity.common.java.exception.ClientException
import com.microsoft.identity.common.java.exception.ErrorStrings
import com.microsoft.identity.common.java.exception.RefreshTokenNotFoundException
import com.microsoft.identity.common.java.exception.ServiceException
import com.microsoft.identity.common.java.logging.LogSession
import com.microsoft.identity.common.java.logging.Logger
import com.microsoft.identity.common.java.providers.microsoft.microsoftsts.MicrosoftStsAuthorizationRequest
import com.microsoft.identity.common.java.providers.microsoft.microsoftsts.MicrosoftStsOAuth2Strategy
import com.microsoft.identity.common.java.providers.nativeauth.NativeAuthOAuth2Strategy
import com.microsoft.identity.common.java.providers.nativeauth.responses.ApiErrorResult
import com.microsoft.identity.common.java.providers.nativeauth.responses.signin.SignInChallengeApiResult
import com.microsoft.identity.common.java.providers.nativeauth.responses.signin.SignInInitiateApiResult
import com.microsoft.identity.common.java.providers.nativeauth.responses.signin.SignInTokenApiResult
import com.microsoft.identity.common.java.providers.oauth2.OAuth2Strategy
import com.microsoft.identity.common.java.providers.oauth2.OAuth2StrategyParameters
import com.microsoft.identity.common.java.providers.oauth2.OAuth2TokenCache
import com.microsoft.identity.common.java.request.SdkType
import com.microsoft.identity.common.java.result.AcquireTokenResult
import com.microsoft.identity.common.java.result.LocalAuthenticationResult
import com.microsoft.identity.common.java.telemetry.TelemetryEventStrings
import com.microsoft.identity.common.java.util.StringUtil
import lombok.EqualsAndHashCode
import java.io.IOException
import java.net.URL

/**
 * The implementation of MSAL Controller for Native Authentication.
 */
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
class NativeAuthMsalController : BaseNativeAuthController() {

    companion object {
        private val TAG = NativeAuthMsalController::class.java.simpleName
    }

    /**
     * Acts as the starting point for the Sign In flow.
     * Starts off with the sign in initiate call, and returns any error results if produced.
     * If successful, continues ot the sign in challenge call, again returning any error results if produced.
     * If the challenge endpoint returns a password required result,
     * a call to the token endpoint will be made with the password provided in the SignInStartCommandParameters,
     * again returning any error results, or returning the token if the call succeeds.
     * If a token is acquired, it is also cached locally as part of this flow.
     */
    fun signInStart(parameters: SignInStartCommandParameters): SignInStartCommandResult {
        LogSession.logMethodCall(TAG, "${TAG}.signInStart")

        try {
            val oAuth2Strategy = createOAuth2Strategy(parameters)
            val initiateApiResult = performSignInInitiateCall(
                oAuth2Strategy = oAuth2Strategy,
                parameters = parameters)

            if (parameters is SignInStartUsingPasswordCommandParameters)
            {
                Logger.verbose(TAG, "Parameters is of type SignInStartUsingPasswordCommandParameters");
                val mergedScopes = addDefaultScopes(parameters.scopes)
                var parametersWithScopes = CommandUtil.createSignInStartCommandParametersWithScopes(
                    parameters as SignInStartUsingPasswordCommandParameters,
                    mergedScopes)

                try {
                    return processSignInInitiateApiResult(
                        initiateApiResult = initiateApiResult,
                        oAuth2Strategy = oAuth2Strategy,
                        parametersWithScopes = parametersWithScopes,
                        usePassword = true)
                } finally {
                    StringUtil.overwriteWithNull(parametersWithScopes.password)
                }
            }
            else
            {
                Logger.verbose(TAG, "Parameters is not of type SignInStartUsingPasswordCommandParameters");
                return processSignInInitiateApiResult(
                    initiateApiResult = initiateApiResult,
                    oAuth2Strategy = oAuth2Strategy)
            }
        } catch (e: Exception) {
            Logger.error(TAG, "Exception thrown in signInStart", e)
            throw e
        }
    }

    /**
     * Makes a call to the /token endpoint with the provided Short Lived Token (SLT), and caches the returned token
     * if successful. In case of error [INativeAuthCommandResult.UnknownError] is returned.
     */
    fun signInWithSLT(parameters: SignInWithSLTCommandParameters): SignInWithSLTCommandResult {
        LogSession.logMethodCall(TAG, "${TAG}.signInWithSLT")

        try {
            val oAuth2Strategy = createOAuth2Strategy(parameters)

            val mergedScopes = addDefaultScopes(parameters.scopes)
            val parametersWithScopes = CommandUtil.createSignInWithSLTCommandParametersWithScopes(
                parameters,
                mergedScopes
            )

            val tokenApiResult = performSLTTokenRequest(
                oAuth2Strategy = oAuth2Strategy,
                parameters = parametersWithScopes
            )

            when (tokenApiResult) {
                is SignInTokenApiResult.Success -> {
                    return saveAndReturnTokens(
                        oAuth2Strategy = oAuth2Strategy,
                        parametersWithScopes = parametersWithScopes,
                        tokenApiResult = tokenApiResult
                    )
                }
                is SignInTokenApiResult.InvalidAuthenticationType,
                is SignInTokenApiResult.MFARequired, is SignInTokenApiResult.CodeIncorrect,
                is SignInTokenApiResult.UserNotFound, is SignInTokenApiResult.InvalidCredentials,
                is SignInTokenApiResult.UnknownError -> {
                    Logger.warn(
                        TAG,
                        "Unexpected result: $tokenApiResult"
                    )
                    tokenApiResult as ApiErrorResult

                    return INativeAuthCommandResult.UnknownError(
                        error = tokenApiResult.error,
                        errorDescription = "API returned unexpected result: $tokenApiResult",
                        errorCodes = tokenApiResult.errorCodes
                    )
                }
            }
        } catch (e: Exception) {
            Logger.error(TAG, "Exception thrown in signInWithSLT", e)
            throw e
        }
    }

    /**
     * Makes a call to the /token endpoint with the provided OOB code, and caches the returned token
     * if successful.  In case of error [INativeAuthCommandResult.UnknownError] is returned.
     */
    fun signInSubmitCode(parameters: SignInSubmitCodeCommandParameters): SignInSubmitCodeCommandResult {
        LogSession.logMethodCall(TAG, "${TAG}.signInSubmitCode")

        try {
            // Add default scopes
            val mergedScopes: List<String> = addDefaultScopes(parameters.scopes)

            val parametersWithScopes =
                CommandUtil.createSignInSubmitCodeCommandParametersWithScopes(
                    parameters,
                    mergedScopes
                )

            val oAuth2Strategy = createOAuth2Strategy(parameters)

            val tokenApiResult = performOOBTokenRequest(
                oAuth2Strategy = oAuth2Strategy,
                parameters = parametersWithScopes
            )
            return when (tokenApiResult) {
                is SignInTokenApiResult.Success -> {
                    saveAndReturnTokens(
                        oAuth2Strategy = oAuth2Strategy,
                        parametersWithScopes = parametersWithScopes,
                        tokenApiResult = tokenApiResult
                    )
                }
                is SignInTokenApiResult.CodeIncorrect -> {
                    SignInCommandResult.IncorrectCode(
                        error = tokenApiResult.error,
                        errorDescription = tokenApiResult.errorDescription,
                        errorCodes = tokenApiResult.errorCodes
                    )
                }

                is SignInTokenApiResult.UnknownError, is SignInTokenApiResult.InvalidAuthenticationType,
                is SignInTokenApiResult.MFARequired, is SignInTokenApiResult.InvalidCredentials,
                is SignInTokenApiResult.UserNotFound -> {
                    Logger.warn(
                        TAG,
                        "Unexpected result: $tokenApiResult"
                    )
                    tokenApiResult as ApiErrorResult
                    INativeAuthCommandResult.UnknownError(
                        error = tokenApiResult.error,
                        errorDescription = tokenApiResult.errorDescription,
                        details = tokenApiResult.details,
                        errorCodes = tokenApiResult.errorCodes
                    )
                }
            }
        } catch (e: Exception) {
            Logger.error(TAG, "Exception thrown in signInSubmitCode", e)
            throw e
        }
    }

    /**
     * Makes a call to the resend code endpoint, mapping responses returned from the server into a command result.
     */
    fun signInResendCode(parameters: SignInResendCodeCommandParameters): SignInResendCodeCommandResult {
        LogSession.logMethodCall(TAG, "${TAG}.signInResendCode")

        try {
            val oAuth2Strategy = createOAuth2Strategy(parameters)

            val result = performSignInChallengeCall(
                oAuth2Strategy = oAuth2Strategy,
                credentialToken = parameters.credentialToken
            )
            return when (result) {
                is SignInChallengeApiResult.OOBRequired -> {
                    SignInCommandResult.CodeRequired(
                        credentialToken = result.credentialToken,
                        codeLength = result.codeLength,
                        challengeTargetLabel = result.challengeTargetLabel,
                        challengeChannel = result.challengeChannel,
                    )
                }
                is SignInChallengeApiResult.PasswordRequired -> {
                    Logger.warn(
                        TAG,
                        "Unexpected result: $result"
                    )
                    INativeAuthCommandResult.UnknownError(
                        error = "unexpected_api_result",
                        errorDescription = "API returned unexpected result: $result"
                    )
                }
                SignInChallengeApiResult.Redirect -> {
                    INativeAuthCommandResult.Redirect()
                }
                is SignInChallengeApiResult.UnknownError -> {
                    Logger.warn(
                        TAG,
                        "Unexpected result: $result"
                    )
                    INativeAuthCommandResult.UnknownError(
                        error = result.error,
                        errorDescription = result.errorDescription,
                        details = result.details,
                        errorCodes = result.errorCodes
                    )
                }
            }
        } catch (e: Exception) {
            Logger.error(TAG, "Exception thrown in signInResendCode", e)
            throw e
        }
    }

    /**
     * Makes a call to the submit password endpoint, mapping responses returned from the server into a command result.
     */
    fun signInSubmitPassword(parameters: SignInSubmitPasswordCommandParameters): SignInSubmitPasswordCommandResult {
        LogSession.logMethodCall(TAG, "${TAG}.signInSubmitPassword")

        try {
            val oAuth2Strategy = createOAuth2Strategy(parameters)

            val mergedScopes = addDefaultScopes(parameters.scopes)
            val parametersWithScopes =
                CommandUtil.createSignInSubmitPasswordCommandParametersWithScopes(
                    parameters,
                    mergedScopes
                )

            try {
                return performPasswordTokenCall(
                    oAuth2Strategy = oAuth2Strategy,
                    parameters = parametersWithScopes
                ).toSignInSubmitPasswordCommandResult(
                    oAuth2Strategy = oAuth2Strategy,
                    parametersWithScopes = parametersWithScopes
                )
            } finally {
                StringUtil.overwriteWithNull(parametersWithScopes.password)
            }
        } catch (e: Exception) {
            Logger.error(TAG, "Exception thrown in signInSubmitPassword", e)
            throw e
        }
    }

    /**
     * Native-auth specific implementation of fetching a token from the cache, and/or refreshing it.
     * Main differences with standard implementation in [LocalMSALController] are:
     * - No scopes are passed in as part of the (developer provided) parameters. The scopes from the
     * Access Token cache are used to make the refresh token call.
     * - When the RT is expired or the refresh token call fails, a different exception is thrown.
     */
    @Throws(
        IOException::class,
        ClientException::class,
        ArgumentException::class,
        ServiceException::class
    )
    fun acquireTokenSilent(
        parameters: AcquireTokenNoFixedScopesCommandParameters
    ): AcquireTokenResult {
        LogSession.logMethodCall(TAG, "${TAG}.acquireTokenSilent")

        val acquireTokenSilentResult = AcquireTokenResult()

        // Validate original AcquireTokenNoScopesCommandParameters parameters
        parameters.validate()

        // Convert AcquireTokenNoScopesCommandParameters into SilentTokenCommandParameters,
        // so we can use it in BaseController.getCachedAccountRecord()
        val silentTokenCommandParameters =
            CommandUtil.convertAcquireTokenNoFixedScopesCommandParameters(
                parameters
            )

        // We want to retrieve all tokens from the cache, regardless of their scopes. Since in the
        // native auth get token flow the developer doesn't have the ability to specify scopes,
        // we can't filter on it.
        // In reality only 1 token should be returned, as native auth currently doesn't support
        // multiple tokens.
        val targetAccount: AccountRecord = getCachedAccountRecord(silentTokenCommandParameters)

        // Build up params for Strategy construction
        val authScheme = silentTokenCommandParameters.authenticationScheme
        val strategyParameters = OAuth2StrategyParameters.builder()
            .platformComponents(parameters.platformComponents)
            .authenticationScheme(authScheme)
            .build()
        val strategy = silentTokenCommandParameters.authority.createOAuth2Strategy(strategyParameters)

        val tokenCache = silentTokenCommandParameters.oAuth2TokenCache
        val cacheRecords = tokenCache.loadWithAggregatedAccountData(
            silentTokenCommandParameters.clientId,
            parameters.applicationIdentifier,
            parameters.mamEnrollmentId,
            null,
            targetAccount,
            authScheme
        ) as List<ICacheRecord>

        // The first element is the 'fully-loaded' CacheRecord which may contain the AccountRecord,
        // AccessTokenRecord, RefreshTokenRecord, and IdTokenRecord... (if all of those artifacts exist)
        // subsequent CacheRecords represent other profiles (projections) of this principal in
        // other tenants. Those tokens will be 'sparse', meaning that their AT/RT will not be loaded
        val fullCacheRecord = cacheRecords[0]

        if (accessTokenIsNull(fullCacheRecord)) {
            throw ServiceException(ErrorStrings.NATIVE_AUTH_NO_ACCESS_TOKEN_FOUND, "No access token found during refresh - user must be signed out.", null)
        }

        if (LibraryConfiguration.getInstance().isRefreshInEnabled &&
            fullCacheRecord.accessToken != null && fullCacheRecord.accessToken.refreshOnIsActive()
        ) {
            Logger.info(
                TAG,
                "RefreshOn is active. This will extend your token usage in the rare case servers are not available."
            )
        }
        if (LibraryConfiguration.getInstance().isRefreshInEnabled &&
            fullCacheRecord.accessToken != null && fullCacheRecord.accessToken.shouldRefresh()
        ) {
            if (!fullCacheRecord.accessToken.isExpired) {
                setAcquireTokenResult(acquireTokenSilentResult, silentTokenCommandParameters, cacheRecords)
                val refreshOnCommand =
                    RefreshOnCommand(parameters, this, PublicApiId.MSAL_REFRESH_ON)
                CommandDispatcher.submitAndForget(refreshOnCommand)
            } else {
                Logger.warn(
                    TAG,
                    "Access token is expired. Removing from cache..."

                )

                renewAT(
                    silentTokenCommandParameters,
                    acquireTokenSilentResult,
                    tokenCache,
                    strategy,
                    fullCacheRecord
                )
            }
        } else if (accessTokenIsNull(fullCacheRecord) ||
            refreshTokenIsNull(fullCacheRecord) ||
            silentTokenCommandParameters.isForceRefresh ||
            !isRequestAuthorityRealmSameAsATRealm(
                silentTokenCommandParameters.authority,
                fullCacheRecord.accessToken
            ) ||
            !strategy.validateCachedResult(authScheme, fullCacheRecord)
        ) {
            if (!refreshTokenIsNull(fullCacheRecord)) {
                // No AT found, but the RT checks out, so we'll use it
                renewAT(
                    silentTokenCommandParameters,
                    acquireTokenSilentResult,
                    tokenCache,
                    strategy,
                    fullCacheRecord
                )
            } else {
                val exception = ServiceException(
                    ErrorStrings.NO_TOKENS_FOUND,
                    "No refresh token was found.",
                    null
                )
                Telemetry.emit(
                    ApiEndEvent()
                        .putException(exception)
                        .putApiId(TelemetryEventStrings.Api.LOCAL_ACQUIRE_TOKEN_SILENT)
                )
                throw exception
            }
        } else if (fullCacheRecord.accessToken.isExpired) {
            Logger.warn(
                TAG,
                "Access token is expired. Removing from cache..."
            )

            renewAT(
                silentTokenCommandParameters,
                acquireTokenSilentResult,
                tokenCache,
                strategy,
                fullCacheRecord
            )
        } else {
            Logger.verbose(
                TAG,
                "Returning silent result"
            )
            setAcquireTokenResult(acquireTokenSilentResult, silentTokenCommandParameters, cacheRecords)
        }
        Telemetry.emit(
            ApiEndEvent()
                .putResult(acquireTokenSilentResult)
                .putApiId(TelemetryEventStrings.Api.LOCAL_ACQUIRE_TOKEN_SILENT)
        )
        return acquireTokenSilentResult
    }

    @Throws(ClientException::class)
    private fun setAcquireTokenResult(
        acquireTokenSilentResult: AcquireTokenResult,
        parametersWithScopes: SilentTokenCommandParameters,
        cacheRecords: List<ICacheRecord>
    ) {
        val fullCacheRecord = cacheRecords[0]
        acquireTokenSilentResult.localAuthenticationResult = LocalAuthenticationResult(
            finalizeCacheRecordForResult(
                fullCacheRecord,
                parametersWithScopes.authenticationScheme
            ),
            cacheRecords,
            SdkType.MSAL,
            true
        )
    }

    @VisibleForTesting
    fun performSLTTokenRequest(
        oAuth2Strategy: NativeAuthOAuth2Strategy,
        parameters: SignInWithSLTCommandParameters
    ): SignInTokenApiResult {
        LogSession.logMethodCall(TAG, "${TAG}.performSLTTokenRequest")
        return oAuth2Strategy.performSLTTokenRequest(
            parameters = parameters
        )
    }

    private fun performOOBTokenRequest(
        oAuth2Strategy: NativeAuthOAuth2Strategy,
        parameters: SignInSubmitCodeCommandParameters
    ): SignInTokenApiResult {
        LogSession.logMethodCall(TAG, "${TAG}.performOOBTokenRequest")
        return oAuth2Strategy.performOOBTokenRequest(
            parameters = parameters
        )
    }

    @VisibleForTesting
    fun performPasswordTokenCall(
        oAuth2Strategy: NativeAuthOAuth2Strategy,
        parameters: SignInSubmitPasswordCommandParameters
    ): SignInTokenApiResult {
        LogSession.logMethodCall(TAG, "${TAG}.performPasswordTokenCall")
        return oAuth2Strategy.performPasswordTokenRequest(
            parameters = parameters
        )
    }

    private fun performSignInInitiateCall(
        oAuth2Strategy: NativeAuthOAuth2Strategy,
        parameters: SignInStartCommandParameters,
    ): SignInInitiateApiResult {
        LogSession.logMethodCall(TAG, "${TAG}.performSignInInitiateCall")
        return oAuth2Strategy.performSignInInitiate(
            parameters = parameters
        )
    }

    private fun performSignInChallengeCall(
        oAuth2Strategy: NativeAuthOAuth2Strategy,
        credentialToken: String
    ): SignInChallengeApiResult {
        LogSession.logMethodCall(TAG, "${TAG}.performSignInChallengeCall")
        return oAuth2Strategy.performSignInChallenge(credentialToken = credentialToken)
    }

    private fun saveAndReturnTokens(
        oAuth2Strategy: NativeAuthOAuth2Strategy,
        parametersWithScopes: BaseSignInTokenCommandParameters,
        tokenApiResult: SignInTokenApiResult.Success
    ): SignInCommandResult.Complete {
        LogSession.logMethodCall(TAG, "${TAG}.saveAndReturnTokens")
        val records: List<ICacheRecord> = saveTokens(
            oAuth2Strategy as MicrosoftStsOAuth2Strategy,
            createAuthorizationRequest(
                strategy = oAuth2Strategy,
                scopes = parametersWithScopes.scopes,
                clientId = parametersWithScopes.clientId,
                applicationIdentifier = parametersWithScopes.applicationIdentifier
            ),
            tokenApiResult.tokenResponse,
            parametersWithScopes.oAuth2TokenCache
        )

        // The first element in the returned list is the item we *just* saved, the rest of
        // the elements are necessary to construct the full IAccount + TenantProfile
        val newestRecord = records[0]

        return SignInCommandResult.Complete(
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

    private fun createAuthorizationRequest(
        strategy: NativeAuthOAuth2Strategy,
        scopes: List<String>,
        clientId: String,
        applicationIdentifier: String
    ): MicrosoftStsAuthorizationRequest {
        LogSession.logMethodCall(TAG, "${TAG}.createAuthorizationRequest")
        val builder = MicrosoftStsAuthorizationRequest.Builder()
        builder.setAuthority(URL(strategy.getAuthority()))
        builder.setClientId(clientId)
        builder.setScope(StringUtil.join(" ", scopes))
        builder.setApplicationIdentifier(applicationIdentifier)
        return builder.build()
    }

    private fun addDefaultScopes(scopes: List<String>?): List<String> {
        LogSession.logMethodCall(TAG, "${TAG}.addDefaultScopes")
        val requestScopes = scopes?.toMutableList() ?: mutableListOf()
        requestScopes.addAll(AuthenticationConstants.DEFAULT_SCOPES)
        // sanitize empty and null scopes
        requestScopes.removeAll(listOf("", null))
        return requestScopes.toList()
    }

    @Throws(
        IOException::class,
        ClientException::class,
        ServiceException::class
    )
    private fun renewAT(
        parameters: SilentTokenCommandParameters,
        acquireTokenSilentResult: AcquireTokenResult,
        tokenCache: OAuth2TokenCache<*, *, *>,
        strategy: OAuth2Strategy<*, *, *, *, *, *, *, *, *, *, *, *, *>,
        cacheRecord: ICacheRecord
    ) {
        Logger.verbose(
            TAG,
            "Renewing access token..."
        )

        // Add the AT's scopes to the parameters so that they can be used to perform the refresh
        // token call.
        val accessTokenScopes = cacheRecord.accessToken
            .target?.split(" ".toRegex())?.dropLastWhile { it.isEmpty() }?.toSet()

        val parametersWithScopes = parameters.toBuilder()
            .scopes(accessTokenScopes)
            .build()

        renewAccessToken(
            parametersWithScopes,
            acquireTokenSilentResult,
            tokenCache,
            strategy,
            cacheRecord
        )
    }

    private fun SignInTokenApiResult.toSignInStartCommandResult(
        oAuth2Strategy: NativeAuthOAuth2Strategy,
        parametersWithScopes: SignInStartUsingPasswordCommandParameters,
    ): SignInStartCommandResult {
        LogSession.logMethodCall(TAG, "${TAG}.execute")
        return when (this) {
            is SignInTokenApiResult.InvalidCredentials -> {
                SignInCommandResult.InvalidCredentials(
                    error = this.error,
                    errorDescription = this.errorDescription,
                    errorCodes = this.errorCodes
                )
            }
            is SignInTokenApiResult.Success -> {
                saveAndReturnTokens(
                    oAuth2Strategy = oAuth2Strategy,
                    parametersWithScopes = parametersWithScopes,
                    tokenApiResult = this
                )
            }
            is SignInTokenApiResult.CodeIncorrect, is SignInTokenApiResult.MFARequired,
            is SignInTokenApiResult.InvalidAuthenticationType, is SignInTokenApiResult.UserNotFound,
            is SignInTokenApiResult.UnknownError -> {
                Logger.warn(
                    TAG,
                    "Unexpected result: $this"
                )
                this as ApiErrorResult
                INativeAuthCommandResult.UnknownError(
                    error = "unexpected_api_result",
                    errorDescription = "API returned unexpected result: $this",
                    errorCodes = this.errorCodes
                )
            }
        }
    }

    private fun SignInTokenApiResult.toSignInSubmitPasswordCommandResult(
        oAuth2Strategy: NativeAuthOAuth2Strategy,
        parametersWithScopes: SignInSubmitPasswordCommandParameters,
    ): SignInSubmitPasswordCommandResult {
        LogSession.logMethodCall(TAG, "${TAG}.execute")

        return when (this) {
            is SignInTokenApiResult.InvalidCredentials -> {
                SignInCommandResult.InvalidCredentials(
                    error = this.error,
                    errorDescription = this.errorDescription,
                    errorCodes = this.errorCodes
                )
            }
            is SignInTokenApiResult.Success -> {
                saveAndReturnTokens(
                    oAuth2Strategy = oAuth2Strategy,
                    parametersWithScopes = parametersWithScopes,
                    tokenApiResult = this
                )
            }
            is SignInTokenApiResult.UserNotFound, is SignInTokenApiResult.CodeIncorrect,
            is SignInTokenApiResult.MFARequired, is SignInTokenApiResult.InvalidAuthenticationType,
            is SignInTokenApiResult.UnknownError -> {
                Logger.warn(
                    TAG,
                    "Unexpected result: $this"
                )
                this as ApiErrorResult
                INativeAuthCommandResult.UnknownError(
                    error = "unexpected_api_result",
                    errorDescription = "API returned unexpected result: $this",
                    errorCodes = this.errorCodes
                )
            }
        }
    }

    @VisibleForTesting
    fun processSignInInitiateApiResult(
        initiateApiResult: SignInInitiateApiResult,
        parametersWithScopes: SignInStartUsingPasswordCommandParameters? = null,
        oAuth2Strategy: NativeAuthOAuth2Strategy,
        usePassword: Boolean = false
    ): SignInStartCommandResult {
        return when (initiateApiResult) {
            SignInInitiateApiResult.Redirect -> {
                INativeAuthCommandResult.Redirect()
            }
            is SignInInitiateApiResult.Success -> {
                val signInChallengeResult = performSignInChallengeCall(
                    oAuth2Strategy = oAuth2Strategy,
                    credentialToken = initiateApiResult.credentialToken
                )
                return processSignInChallengeCall(
                    result = signInChallengeResult,
                    oAuth2Strategy = oAuth2Strategy,
                    parametersWithScopes = parametersWithScopes,
                    usePassword = usePassword
                )
            }
            is SignInInitiateApiResult.UserNotFound -> {
                SignInCommandResult.UserNotFound(
                    error = initiateApiResult.error,
                    errorDescription = initiateApiResult.errorDescription,
                    errorCodes = initiateApiResult.errorCodes
                )
            }
            is SignInInitiateApiResult.UnknownError -> {
                Logger.warn(
                    TAG,
                    "Unexpected result: $initiateApiResult"
                )
                INativeAuthCommandResult.UnknownError(
                    error = initiateApiResult.error,
                    errorDescription = initiateApiResult.errorDescription,
                    errorCodes = initiateApiResult.errorCodes
                )
            }
        }
    }

    private fun processSignInChallengeCall(
        oAuth2Strategy: NativeAuthOAuth2Strategy,
        parametersWithScopes: SignInStartUsingPasswordCommandParameters?,
        result: SignInChallengeApiResult,
        usePassword: Boolean
    ): SignInStartCommandResult {
        return when (result) {
            is SignInChallengeApiResult.OOBRequired -> {
                SignInCommandResult.CodeRequired(
                    credentialToken = result.credentialToken,
                    codeLength = result.codeLength,
                    challengeTargetLabel = result.challengeTargetLabel,
                    challengeChannel = result.challengeChannel
                )
            }
            is SignInChallengeApiResult.PasswordRequired -> {
                if (usePassword) {
                    if (parametersWithScopes == null) {
                        // In password flows, we will be sending the password and scopes to the token
                        // endpoint. So we need this parameter to be set.
                        throw IllegalArgumentException("Parameters must be provided in password flow")
                    }

                    val signInSubmitPasswordCommandParameters =
                        CommandUtil.createSignInSubmitPasswordCommandParameters(
                            parametersWithScopes,
                            result.credentialToken
                        )
                    try {
                        return performPasswordTokenCall(
                            oAuth2Strategy = oAuth2Strategy,
                            parameters = signInSubmitPasswordCommandParameters
                        ).toSignInStartCommandResult(
                            oAuth2Strategy = oAuth2Strategy,
                            parametersWithScopes = parametersWithScopes
                        )
                    } finally {
                        StringUtil.overwriteWithNull(signInSubmitPasswordCommandParameters.password)
                    }
                } else {
                    SignInCommandResult.PasswordRequired(
                        credentialToken = result.credentialToken
                    )
                }
            }
            SignInChallengeApiResult.Redirect -> {
                INativeAuthCommandResult.Redirect()
            }

            is SignInChallengeApiResult.UnknownError -> {
                Logger.warn(
                    TAG,
                    "Unexpected result: $result"
                )
                INativeAuthCommandResult.UnknownError(
                    error = result.error,
                    errorDescription = result.errorDescription,
                    details = result.details,
                    errorCodes = result.errorCodes
                )
            }
        }
    }

    private fun createOAuth2Strategy(parameters: BaseNativeAuthCommandParameters): NativeAuthOAuth2Strategy {
        val strategyParameters = OAuth2StrategyParameters.builder()
            .platformComponents(parameters.platformComponents)
            .challengeTypes(parameters.challengeType)
            .build()

        return parameters
            .authority
            .createOAuth2Strategy(strategyParameters)
    }
}
