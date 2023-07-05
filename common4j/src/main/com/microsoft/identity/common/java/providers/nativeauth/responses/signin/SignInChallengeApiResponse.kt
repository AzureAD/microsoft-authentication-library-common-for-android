package com.microsoft.identity.common.java.providers.nativeauth.responses.signin

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.microsoft.identity.common.java.logging.LogSession
import com.microsoft.identity.common.java.providers.nativeauth.IApiResponse
import com.microsoft.identity.common.java.providers.nativeauth.interactors.InnerError
import com.microsoft.identity.common.java.util.isOOB
import com.microsoft.identity.common.java.util.isPassword
import com.microsoft.identity.common.java.util.isRedirect
import java.net.HttpURLConnection

data class SignInChallengeApiResponse(
    @Expose override var statusCode: Int,
    @Expose @SerializedName("credential_token") val credentialToken: String?,
    @Expose @SerializedName("challenge_type") val challengeType: String?,
    @Expose @SerializedName("binding_method") val bindingMethod: String?,
    @Expose @SerializedName("challenge_target_label") val challengeTargetLabel: String?,
    @Expose @SerializedName("challenge_channel") val challengeChannel: String?,
    @Expose @SerializedName("code_length") val codeLength: Int?,
    @Expose @SerializedName("interval") val interval: Int?,
    @Expose @SerializedName("error") val error: String?,
    @Expose @SerializedName("details") val details: List<Map<String, String>>?,
    @Expose @SerializedName("error_codes") val errorCodes: List<Int>?,
    @Expose @SerializedName("error_description") val errorDescription: String?,
    @Expose @SerializedName("error_uri") val errorUri: String?,
    @Expose @SerializedName("inner_errors") val innerErrors: List<InnerError>?,
): IApiResponse(statusCode) {

    companion object {
        private val TAG = SignInChallengeApiResponse::class.java.simpleName
    }

    fun toResult(): SignInChallengeApiResult {
        LogSession.logMethodCall(TAG)

        return when (statusCode) {

            // Handle 400 errors
            HttpURLConnection.HTTP_BAD_REQUEST -> {
                if (error == "invalid_grant") {
                    SignInChallengeApiResult.UnknownError(
                        error = error,
                        errorDescription = errorDescription.orEmpty(),
                        details = details,
                        errorCodes = errorCodes.orEmpty()
                    )
                }
                else {
                    SignInChallengeApiResult.UnknownError(
                        error = error.orEmpty(),
                        errorDescription = errorDescription.orEmpty(),
                        details = details,
                        errorCodes = errorCodes.orEmpty()
                    )
                }
            }

            // Handle success and redirect
            HttpURLConnection.HTTP_OK -> {
                return when {
                    challengeType.isRedirect() -> {
                        SignInChallengeApiResult.Redirect
                    }
                    challengeType.isOOB() -> {
                        return when {
                            challengeTargetLabel.isNullOrBlank() -> {
                                SignInChallengeApiResult.UnknownError(
                                    error = "invalid_state",
                                    errorDescription = "SignIn /challenge did not return a challenge_target_label with oob challenge type",
                                    details = details,
                                    errorCodes = errorCodes.orEmpty()
                                )
                            }
                            challengeChannel.isNullOrBlank() -> {
                                SignInChallengeApiResult.UnknownError(
                                    error = "invalid_state",
                                    errorDescription = "SignIn /challenge did not return a challenge_channel with oob challenge type",
                                    details = details,
                                    errorCodes = errorCodes.orEmpty()
                                )
                            }
                            codeLength == null -> {
                                SignInChallengeApiResult.UnknownError(
                                    error = "invalid_state",
                                    errorDescription = "SignIn /challenge did not return a code_length with oob challenge type",
                                    details = details,
                                    errorCodes = errorCodes.orEmpty()
                                )
                            }
                            else -> {
                                SignInChallengeApiResult.OOBRequired(
                                    credentialToken = credentialToken
                                        ?: return SignInChallengeApiResult.UnknownError(
                                            error = "invalid_state",
                                            errorDescription = "SignIn /challenge did not return a flow token with oob challenge type",
                                            details = details,
                                            errorCodes = errorCodes.orEmpty()
                                        ),
                                    challengeTargetLabel = challengeTargetLabel,
                                    codeLength = codeLength,
                                    challengeChannel = challengeChannel
                                )
                            }
                        }
                    }
                    challengeType.isPassword() -> {
                        SignInChallengeApiResult.PasswordRequired(
                            credentialToken = credentialToken
                                ?: return SignInChallengeApiResult.UnknownError(
                                    error = "invalid_state",
                                    errorDescription = "SignIn /challenge did not return a flow token with password challenge type",
                                    details = details,
                                    errorCodes = errorCodes.orEmpty()
                                )
                        )
                    }
                    else -> {
                        SignInChallengeApiResult.UnknownError(
                            error = error.orEmpty(),
                            errorDescription = errorDescription.orEmpty(),
                            details = details,
                            errorCodes = errorCodes.orEmpty()
                        )
                    }
                }
            }

            // Catch uncommon status codes
            else -> {
                SignInChallengeApiResult.UnknownError(
                    error = error.orEmpty(),
                    errorDescription = errorDescription.orEmpty(),
                    details = details,
                    errorCodes = errorCodes.orEmpty()
                )
            }
        }
    }
}