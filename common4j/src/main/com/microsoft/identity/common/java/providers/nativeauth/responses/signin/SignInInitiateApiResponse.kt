package com.microsoft.identity.common.java.providers.nativeauth.responses.signin

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.microsoft.identity.common.java.logging.LogSession
import com.microsoft.identity.common.java.providers.nativeauth.IApiResponse
import com.microsoft.identity.common.java.providers.nativeauth.interactors.InnerError
import com.microsoft.identity.common.java.util.isInvalidGrant
import com.microsoft.identity.common.java.util.isRedirect
import com.microsoft.identity.common.java.util.isUserNotFound
import java.net.HttpURLConnection

data class SignInInitiateApiResponse(
    @Expose override var statusCode: Int,
    @Expose @SerializedName("credential_token") val credentialToken: String?,
    @Expose @SerializedName("challenge_type") val challengeType: String?,
    @Expose @SerializedName("error") val error: String?,
    @Expose @SerializedName("error_description") val errorDescription: String?,
    @Expose @SerializedName("error_uri") val errorUri: String?,
    @Expose @SerializedName("details") val details: List<Map<String, String>>?,
    @Expose @SerializedName("error_codes") val errorCodes: List<Int>?,
    @Expose @SerializedName("inner_errors") val innerErrors: List<InnerError>?,
): IApiResponse(statusCode) {

    companion object {
        private val TAG = SignInInitiateApiResponse::class.java.simpleName
    }

    fun toResult(): SignInInitiateApiResult {
        LogSession.logMethodCall(TAG)

        return when (statusCode) {

            // Handle 400 errors
            HttpURLConnection.HTTP_BAD_REQUEST -> {
                if (error.isInvalidGrant()) {
                    return when {
                        errorCodes.isNullOrEmpty() -> {
                            SignInInitiateApiResult.UnknownError(
                                error = error.orEmpty(),
                                errorDescription = errorDescription.orEmpty(),
                                details = details,
                                errorCodes = errorCodes.orEmpty()
                            )
                        }
                        errorCodes[0].isUserNotFound() -> {
                            SignInInitiateApiResult.UserNotFound(
                                error = error.orEmpty(),
                                errorDescription = errorDescription.orEmpty(),
                                errorCodes = errorCodes
                            )
                        }
                        else -> {
                            SignInInitiateApiResult.UnknownError(
                                error = error.orEmpty(),
                                errorDescription = errorDescription.orEmpty(),
                                details = details,
                                errorCodes = errorCodes
                            )
                        }
                    }
                }
                else {
                    SignInInitiateApiResult.UnknownError(
                        error = error.orEmpty(),
                        errorDescription = errorDescription.orEmpty(),
                        details = details,
                        errorCodes = errorCodes.orEmpty()
                    )
                }
            }

            // Handle success and redirect
            HttpURLConnection.HTTP_OK -> {
                if (challengeType.isRedirect()) {
                    SignInInitiateApiResult.Redirect
                }
                else {
                    SignInInitiateApiResult.Success(
                        credentialToken = credentialToken
                            ?: return SignInInitiateApiResult.UnknownError(
                                error = "invalid_state",
                                errorDescription = "SignIn /initiate did not return a flow token",
                                details = details,
                                errorCodes = errorCodes.orEmpty()
                            )
                    )
                }
            }

            // Catch uncommon status codes
            else -> {
                SignInInitiateApiResult.UnknownError(
                    error = error.orEmpty(),
                    errorDescription = errorDescription.orEmpty(),
                    details = details,
                    errorCodes = errorCodes.orEmpty()
                )
            }
        }
    }
}

