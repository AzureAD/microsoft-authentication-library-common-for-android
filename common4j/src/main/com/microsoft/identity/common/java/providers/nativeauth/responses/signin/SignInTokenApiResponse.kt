package com.microsoft.identity.common.java.providers.nativeauth.responses.signin

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.microsoft.identity.common.java.logging.LogSession
import com.microsoft.identity.common.java.providers.nativeauth.IApiResponse
import com.microsoft.identity.common.java.providers.nativeauth.interactors.InnerError
import com.microsoft.identity.common.java.util.isInvalidAuthenticationType
import com.microsoft.identity.common.java.util.isInvalidGrant
import com.microsoft.identity.common.java.util.isOtpCodeIncorrect
import com.microsoft.identity.common.java.util.isInvalidCredentials
import com.microsoft.identity.common.java.util.isUserNotFound
import java.net.HttpURLConnection

data class SignInTokenApiResponse(
    @Expose override var statusCode: Int,
    @Expose @SerializedName("token_type") val tokenType: String?,
    @Expose @SerializedName("scope") val scope: String?,
    @Expose @SerializedName("expires_in") val expiresIn: Long?,
    @Expose @SerializedName("ext_expires_in") val extExpiresIn: Long?,
    @SerializedName("access_token") val accessToken: String?,
    @SerializedName("refresh_token") val refreshToken: String?,
    @Expose @SerializedName("id_token") val idToken: String?,
    @Expose @SerializedName("error") val error: String?,
    @Expose @SerializedName("error_description") val errorDescription: String?,
    @Expose @SerializedName("error_uri") val errorUri: String?,
    @Expose @SerializedName("details") val details: List<Map<String, String>>?,
    @Expose @SerializedName("error_codes") val errorCodes: List<Int>?,
    @Expose @SerializedName("inner_errors") val innerErrors: List<InnerError>?,
    @Expose @SerializedName("credential_token") val credentialToken: String?,
    @Expose @SerializedName("client_info") val clientInfo: String?,
): IApiResponse(statusCode) {

    companion object {
        private val TAG = SignInTokenApiResponse::class.java.simpleName
    }
    fun toErrorResult(): SignInTokenApiResult {
        LogSession.logMethodCall(TAG)

        return if (error.isInvalidGrant()) {
            return when {
                errorCodes.isNullOrEmpty() -> {
                    SignInTokenApiResult.UnknownError(
                        error = error,
                        errorDescription = errorDescription,
                        details = details,
                        errorCodes = errorCodes
                    )
                }
                errorCodes[0].isUserNotFound() -> {
                    SignInTokenApiResult.UserNotFound(
                        error = error.orEmpty(),
                        errorDescription = errorDescription.orEmpty(),
                        errorCodes = errorCodes
                    )
                }
                errorCodes[0].isInvalidCredentials() -> {
                    SignInTokenApiResult.InvalidCredentials(
                        error = error.orEmpty(),
                        errorDescription = errorDescription.orEmpty(),
                        errorCodes = errorCodes
                    )
                }
                errorCodes[0].isOtpCodeIncorrect() -> {
                    SignInTokenApiResult.CodeIncorrect(
                        error = error.orEmpty(),
                        errorDescription = errorDescription.orEmpty(),
                        errorCodes = errorCodes
                    )
                }
                errorCodes[0].isInvalidAuthenticationType() -> {
                    SignInTokenApiResult.InvalidAuthenticationType(
                        error = error.orEmpty(),
                        errorDescription = errorDescription.orEmpty(),
                        errorCodes = errorCodes
                    )
                }
                else -> {
                    SignInTokenApiResult.UnknownError(
                        error = error,
                        errorDescription = errorDescription,
                        details = details,
                        errorCodes = errorCodes
                    )
                }
            }
        }
        else {
            SignInTokenApiResult.UnknownError(
                error = error,
                errorDescription = errorDescription,
                details = details,
                errorCodes = errorCodes
            )
        }
    }
}