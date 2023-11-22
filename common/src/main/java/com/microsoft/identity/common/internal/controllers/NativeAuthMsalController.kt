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
import com.microsoft.identity.common.internal.commands.ResetPasswordSubmitNewPasswordCommand
import com.microsoft.identity.common.internal.telemetry.Telemetry
import com.microsoft.identity.common.internal.telemetry.events.ApiEndEvent
import com.microsoft.identity.common.internal.util.CommandUtil
import com.microsoft.identity.common.java.AuthenticationConstants
import com.microsoft.identity.common.java.cache.ICacheRecord
import com.microsoft.identity.common.java.commands.parameters.SilentTokenCommandParameters
import com.microsoft.identity.common.java.commands.parameters.nativeauth.AcquireTokenNoFixedScopesCommandParameters
import com.microsoft.identity.common.java.commands.parameters.nativeauth.BaseNativeAuthCommandParameters
import com.microsoft.identity.common.java.commands.parameters.nativeauth.BaseSignInTokenCommandParameters
import com.microsoft.identity.common.java.commands.parameters.nativeauth.ResetPasswordResendCodeCommandParameters
import com.microsoft.identity.common.java.commands.parameters.nativeauth.ResetPasswordStartCommandParameters
import com.microsoft.identity.common.java.commands.parameters.nativeauth.ResetPasswordSubmitCodeCommandParameters
import com.microsoft.identity.common.java.commands.parameters.nativeauth.ResetPasswordSubmitNewPasswordCommandParameters
import com.microsoft.identity.common.java.commands.parameters.nativeauth.SignInResendCodeCommandParameters
import com.microsoft.identity.common.java.commands.parameters.nativeauth.SignInStartCommandParameters
import com.microsoft.identity.common.java.commands.parameters.nativeauth.SignInStartUsingPasswordCommandParameters
import com.microsoft.identity.common.java.commands.parameters.nativeauth.SignInSubmitCodeCommandParameters
import com.microsoft.identity.common.java.commands.parameters.nativeauth.SignInSubmitPasswordCommandParameters
import com.microsoft.identity.common.java.commands.parameters.nativeauth.SignInWithSLTCommandParameters
import com.microsoft.identity.common.java.configuration.LibraryConfiguration
import com.microsoft.identity.common.java.controllers.CommandDispatcher
import com.microsoft.identity.common.java.controllers.results.INativeAuthCommandResult
import com.microsoft.identity.common.java.controllers.results.ResetPasswordCommandResult
import com.microsoft.identity.common.java.controllers.results.ResetPasswordResendCodeCommandResult
import com.microsoft.identity.common.java.controllers.results.ResetPasswordStartCommandResult
import com.microsoft.identity.common.java.controllers.results.ResetPasswordSubmitCodeCommandResult
import com.microsoft.identity.common.java.controllers.results.ResetPasswordSubmitNewPasswordCommandResult
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
import com.microsoft.identity.common.java.exception.ServiceException
import com.microsoft.identity.common.java.logging.LogSession
import com.microsoft.identity.common.java.logging.Logger
import com.microsoft.identity.common.java.providers.microsoft.microsoftsts.MicrosoftStsAuthorizationRequest
import com.microsoft.identity.common.java.providers.microsoft.microsoftsts.MicrosoftStsOAuth2Strategy
import com.microsoft.identity.common.java.providers.nativeauth.NativeAuthOAuth2Strategy
import com.microsoft.identity.common.java.providers.nativeauth.responses.ApiErrorResult
import com.microsoft.identity.common.java.providers.nativeauth.responses.resetpassword.ResetPasswordChallengeApiResult
import com.microsoft.identity.common.java.providers.nativeauth.responses.resetpassword.ResetPasswordContinueApiResult
import com.microsoft.identity.common.java.providers.nativeauth.responses.resetpassword.ResetPasswordPollCompletionApiResult
import com.microsoft.identity.common.java.providers.nativeauth.responses.resetpassword.ResetPasswordStartApiResult
import com.microsoft.identity.common.java.providers.nativeauth.responses.resetpassword.ResetPasswordSubmitApiResult
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
import com.microsoft.identity.common.java.util.ThreadUtils
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
            null,
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

    /**
     * Makes a call to the resetpassword/start endpoint with the provided username.
     * If successful, makes a call to the resetpassword/challenge endpoint, returning the result of that call.
     */
    fun resetPasswordStart(parameters: ResetPasswordStartCommandParameters): ResetPasswordStartCommandResult {
        LogSession.logMethodCall(TAG, "${TAG}.resetPasswordStart")

        try {
            val oAuth2Strategy = createOAuth2Strategy(parameters)

            val resetPasswordStartApiResult = performResetPasswordStartCall(
                oAuth2Strategy = oAuth2Strategy,
                parameters = parameters
            )

            return when (resetPasswordStartApiResult) {
                is ResetPasswordStartApiResult.Success -> {
                    performResetPasswordChallengeCall(
                        oAuth2Strategy = oAuth2Strategy,
                        passwordResetToken = resetPasswordStartApiResult.passwordResetToken
                    ).toResetPasswordStartCommandResult()
                }
                ResetPasswordStartApiResult.Redirect -> {
                    INativeAuthCommandResult.Redirect()
                }
                is ResetPasswordStartApiResult.UserNotFound -> {
                    ResetPasswordCommandResult.UserNotFound(
                        error = resetPasswordStartApiResult.error,
                        errorDescription = resetPasswordStartApiResult.errorDescription
                    )
                }
                is ResetPasswordStartApiResult.UnsupportedChallengeType, is ResetPasswordStartApiResult.UnknownError -> {
                    Logger.warn(
                        TAG,
                        "Unexpected result: $resetPasswordStartApiResult"
                    )
                    resetPasswordStartApiResult as ApiErrorResult
                    INativeAuthCommandResult.UnknownError(
                        error = resetPasswordStartApiResult.error,
                        errorDescription = resetPasswordStartApiResult.errorDescription,
                        details = resetPasswordStartApiResult.details
                    )
                }
            }
        } catch (e: Exception) {
            Logger.error(TAG, "Exception thrown in resetPasswordStart", e)
            throw e
        }
    }

    /**
     * Makes a call to the resetpassword/continue endpoint with the provided code and password reset token.
     */
    fun resetPasswordSubmitCode(parameters: ResetPasswordSubmitCodeCommandParameters): ResetPasswordSubmitCodeCommandResult {
        LogSession.logMethodCall(TAG, "${TAG}.resetPasswordSubmitCode")

        try {
            val oAuth2Strategy = createOAuth2Strategy(parameters)

            val resetPasswordContinueApiResult = performResetPasswordContinueCall(
                oAuth2Strategy = oAuth2Strategy,
                parameters = parameters
            )

            return when (resetPasswordContinueApiResult) {
                is ResetPasswordContinueApiResult.PasswordRequired -> {
                    ResetPasswordCommandResult.PasswordRequired(
                        passwordSubmitToken = resetPasswordContinueApiResult.passwordSubmitToken
                    )
                }
                is ResetPasswordContinueApiResult.CodeIncorrect -> {
                    ResetPasswordCommandResult.IncorrectCode(
                        error = resetPasswordContinueApiResult.error,
                        errorDescription = resetPasswordContinueApiResult.errorDescription
                    )
                }
                ResetPasswordContinueApiResult.Redirect -> {
                    INativeAuthCommandResult.Redirect()
                }
                is ResetPasswordContinueApiResult.ExpiredToken, is ResetPasswordContinueApiResult.UnknownError -> {
                    Logger.warn(
                        TAG,
                        "Unexpected result: $resetPasswordContinueApiResult"
                    )
                    resetPasswordContinueApiResult as ApiErrorResult
                    INativeAuthCommandResult.UnknownError(
                        error = resetPasswordContinueApiResult.error,
                        errorDescription = resetPasswordContinueApiResult.errorDescription,
                        details = resetPasswordContinueApiResult.details
                    )
                }
            }
        } catch (e: Exception) {
            Logger.error(TAG, "Exception thrown in resetPasswordSubmitCode", e)
            throw e
        }
    }

    /**
     * Makes a call to the resetpassword/challenge endpoint to trigger a code to be re-sent.
     */
    fun resetPasswordResendCode(parameters: ResetPasswordResendCodeCommandParameters): ResetPasswordResendCodeCommandResult {
        LogSession.logMethodCall(TAG, "${TAG}.resetPasswordResendCode")

        try {
            val oAuth2Strategy = createOAuth2Strategy(parameters)

            val resetPasswordChallengeApiResult = performResetPasswordChallengeCall(
                oAuth2Strategy = oAuth2Strategy,
                passwordResetToken = parameters.passwordResetToken
            )

            return when (resetPasswordChallengeApiResult) {
                is ResetPasswordChallengeApiResult.CodeRequired -> {
                    ResetPasswordCommandResult.CodeRequired(
                        passwordResetToken = resetPasswordChallengeApiResult.passwordResetToken,
                        codeLength = resetPasswordChallengeApiResult.codeLength,
                        challengeTargetLabel = resetPasswordChallengeApiResult.challengeTargetLabel,
                        challengeChannel = resetPasswordChallengeApiResult.challengeChannel
                    )
                }
                ResetPasswordChallengeApiResult.Redirect -> {
                    INativeAuthCommandResult.Redirect()
                }
                is ResetPasswordChallengeApiResult.ExpiredToken,
                is ResetPasswordChallengeApiResult.UnsupportedChallengeType,
                is ResetPasswordChallengeApiResult.UnknownError -> {
                    Logger.warn(
                        TAG,
                        "Unexpected result: $resetPasswordChallengeApiResult"
                    )
                    resetPasswordChallengeApiResult as ApiErrorResult
                    INativeAuthCommandResult.UnknownError(
                        error = resetPasswordChallengeApiResult.error,
                        errorDescription = resetPasswordChallengeApiResult.errorDescription,
                        details = resetPasswordChallengeApiResult.details
                    )
                }
            }
        } catch (e: Exception) {
            Logger.error(TAG, "Exception thrown in resetPasswordResendCode", e)
            throw e
        }
    }

    /**
     * Makes a call to the resetpassword/submit endpoint with the provided password.
     * If successful, calls the resetpassword/poll_completion endpoint, continuing to do so until a success response is returned, or the polling times out.
     */
    fun resetPasswordSubmitNewPassword(parameters: ResetPasswordSubmitNewPasswordCommandParameters): ResetPasswordSubmitNewPasswordCommandResult {
        LogSession.logMethodCall(TAG, "${TAG}.resetPasswordSubmitNewPassword")

        try {
            val oAuth2Strategy = createOAuth2Strategy(parameters)

            val resetPasswordSubmitApiResult = performResetPasswordSubmitCall(
                oAuth2Strategy = oAuth2Strategy,
                parameters = parameters
            )

            return when (resetPasswordSubmitApiResult) {
                is ResetPasswordSubmitApiResult.SubmitSuccess -> {
                    resetPasswordPollCompletion(
                        oAuth2Strategy = oAuth2Strategy,
                        passwordResetToken = resetPasswordSubmitApiResult.passwordResetToken,
                        pollIntervalInSeconds = resetPasswordSubmitApiResult.pollInterval
                    )
                }
                is ResetPasswordSubmitApiResult.PasswordInvalid -> {
                    ResetPasswordCommandResult.PasswordNotAccepted(
                        error = resetPasswordSubmitApiResult.error,
                        errorDescription = resetPasswordSubmitApiResult.errorDescription
                    )
                }

                is ResetPasswordSubmitApiResult.ExpiredToken,
                is ResetPasswordSubmitApiResult.UnknownError -> {
                    Logger.warn(
                        TAG,
                        "Unexpected result: $resetPasswordSubmitApiResult"
                    )
                    resetPasswordSubmitApiResult as ApiErrorResult
                    INativeAuthCommandResult.UnknownError(
                        error = resetPasswordSubmitApiResult.error,
                        errorDescription = resetPasswordSubmitApiResult.errorDescription,
                        details = resetPasswordSubmitApiResult.details
                    )
                }
            }
        } catch (e: Exception) {
            Logger.error(TAG, "Exception thrown in resetPasswordSubmitNewPassword", e)
            throw e
        }
    }

    private fun resetPasswordPollCompletion(
        oAuth2Strategy: NativeAuthOAuth2Strategy,
        passwordResetToken: String,
        pollIntervalInSeconds: Int?
    ): ResetPasswordSubmitNewPasswordCommandResult {
        fun pollCompletionTimedOut(startTime: Long): Boolean {
            val currentTime = System.currentTimeMillis()
            return currentTime - startTime > ResetPasswordSubmitNewPasswordCommand.POLL_COMPLETION_TIMEOUT_IN_MILISECONDS
        }

        fun pollIntervalIsAppropriate(pollIntervalInSeconds: Int?): Boolean {
            return pollIntervalInSeconds != null && pollIntervalInSeconds <= 15 && pollIntervalInSeconds >= 1
        }

        val methodTag = "$TAG:resetPasswordPollCompletion"

        LogSession.logMethodCall(TAG, "${TAG}.resetPasswordPollCompletion")

        try {
            val pollWaitInterval: Int = if (!pollIntervalIsAppropriate(pollIntervalInSeconds)) {
                ResetPasswordSubmitNewPasswordCommand.DEFAULT_POLL_COMPLETION_INTERVAL_IN_MILISECONDS
            } else {
                pollIntervalInSeconds!! * 1000
            }

            var pollCompletionApiResult = performResetPasswordPollCompletionCall(
                oAuth2Strategy = oAuth2Strategy,
                passwordResetToken = passwordResetToken
            )

            val startTime = System.currentTimeMillis()

            while (pollCompletionApiResult is ResetPasswordPollCompletionApiResult.InProgress) {
                ThreadUtils.sleepSafely(
                    pollWaitInterval,
                    methodTag,
                    "Waiting between reset password polls"
                )

                if (pollCompletionTimedOut(startTime)) {
                    Logger.warn(
                        TAG,
                        "Reset password completion timed out."
                    )
                    return ResetPasswordCommandResult.PasswordResetFailed(
                        error = ResetPasswordSubmitNewPasswordCommand.POLL_COMPLETION_TIMEOUT_ERROR_CODE,
                        errorDescription = ResetPasswordSubmitNewPasswordCommand.POLL_COMPLETION_TIMEOUT_ERROR_DESCRIPTION
                    )
                }

                pollCompletionApiResult = performResetPasswordPollCompletionCall(
                    oAuth2Strategy = oAuth2Strategy,
                    passwordResetToken = passwordResetToken
                )
            }

            return when (pollCompletionApiResult) {
                is ResetPasswordPollCompletionApiResult.PollingFailed -> {
                    ResetPasswordCommandResult.PasswordResetFailed(
                        error = pollCompletionApiResult.error,
                        errorDescription = pollCompletionApiResult.errorDescription
                    )
                }

                is ResetPasswordPollCompletionApiResult.PollingSucceeded -> {
                    ResetPasswordCommandResult.Complete
                }

                is ResetPasswordPollCompletionApiResult.InProgress -> {
                    Logger.warn(
                        TAG,
                        "in_progress received after polling, illegal state"
                    )
                    // This should never be reached, theoretically
                    INativeAuthCommandResult.UnknownError(
                        error = "illegal_state",
                        errorDescription = "in_progress received after polling concluded, illegal state"
                    )
                }
                is ResetPasswordPollCompletionApiResult.ExpiredToken,
                is ResetPasswordPollCompletionApiResult.UserNotFound,
                is ResetPasswordPollCompletionApiResult.PasswordInvalid,
                is ResetPasswordPollCompletionApiResult.UnknownError -> {
                    Logger.warn(
                        TAG,
                        "Unexpected result: $pollCompletionApiResult"
                    )
                    pollCompletionApiResult as ApiErrorResult
                    INativeAuthCommandResult.UnknownError(
                        error = pollCompletionApiResult.error,
                        errorDescription = pollCompletionApiResult.errorDescription,
                        details = pollCompletionApiResult.details
                    )
                }
            }
        } catch (e: Exception) {
            Logger.error(TAG, "Exception thrown in resetPasswordPollCompletion",e)
            throw e
        }
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

    private fun performResetPasswordStartCall(
        oAuth2Strategy: NativeAuthOAuth2Strategy,
        parameters: ResetPasswordStartCommandParameters,
    ): ResetPasswordStartApiResult {
        LogSession.logMethodCall(TAG, "${TAG}.performResetPasswordStartCall")
        return oAuth2Strategy.performResetPasswordStart(
            parameters = parameters
        )
    }

    private fun performResetPasswordChallengeCall(
        oAuth2Strategy: NativeAuthOAuth2Strategy,
        passwordResetToken: String
    ): ResetPasswordChallengeApiResult {
        LogSession.logMethodCall(TAG, "${TAG}.performResetPasswordChallengeCall")
        return oAuth2Strategy.performResetPasswordChallenge(
            passwordResetToken = passwordResetToken
        )
    }

    private fun performResetPasswordContinueCall(
        oAuth2Strategy: NativeAuthOAuth2Strategy,
        parameters: ResetPasswordSubmitCodeCommandParameters,
    ): ResetPasswordContinueApiResult {
        LogSession.logMethodCall(TAG, "${TAG}.performResetPasswordContinueCall")
        return oAuth2Strategy.performResetPasswordContinue(
            parameters = parameters
        )
    }

    private fun performResetPasswordSubmitCall(
        oAuth2Strategy: NativeAuthOAuth2Strategy,
        parameters: ResetPasswordSubmitNewPasswordCommandParameters,
    ): ResetPasswordSubmitApiResult {
        LogSession.logMethodCall(TAG, "${TAG}.performResetPasswordSubmitCall")
        return oAuth2Strategy.performResetPasswordSubmit(
            parameters = parameters,
        )
    }

    private fun performResetPasswordPollCompletionCall(
        oAuth2Strategy: NativeAuthOAuth2Strategy,
        passwordResetToken: String,
    ): ResetPasswordPollCompletionApiResult {
        LogSession.logMethodCall(TAG, "${TAG}.performResetPasswordPollCompletionCall")
        return oAuth2Strategy.performResetPasswordPollCompletion(
            passwordResetToken = passwordResetToken
        )
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

    private fun ResetPasswordChallengeApiResult.toResetPasswordStartCommandResult(): ResetPasswordStartCommandResult {
        LogSession.logMethodCall(TAG, "${TAG}.toResetPasswordStartCommandResult")
        return when (this) {
            is ResetPasswordChallengeApiResult.CodeRequired -> {
                ResetPasswordCommandResult.CodeRequired(
                    passwordResetToken = this.passwordResetToken,
                    codeLength = this.codeLength,
                    challengeTargetLabel = this.challengeTargetLabel,
                    challengeChannel = this.challengeChannel
                )
            }
            ResetPasswordChallengeApiResult.Redirect -> {
                INativeAuthCommandResult.Redirect()
            }
            is ResetPasswordChallengeApiResult.ExpiredToken -> {
                Logger.warn(
                    TAG,
                    "Expire token result: $this"
                )
                INativeAuthCommandResult.UnknownError(
                    error = this.error,
                    errorDescription = this.errorDescription
                )
            }
            is ResetPasswordChallengeApiResult.UnsupportedChallengeType -> {
                Logger.warn(
                    TAG,
                    "Unsupported challenge type: $this"
                )
                INativeAuthCommandResult.UnknownError(
                    error = this.error,
                    errorDescription = this.errorDescription
                )
            }
            is ResetPasswordChallengeApiResult.UnknownError -> {
                Logger.warn(
                    TAG,
                    "Unexpected result: $this"
                )
                INativeAuthCommandResult.UnknownError(
                    error = this.error,
                    errorDescription = this.errorDescription,
                    details = this.details
                )
            }
        }
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
