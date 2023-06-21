package com.microsoft.identity.common.java.providers.nativeauth.responses.signin

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.microsoft.identity.common.java.logging.LogSession
import com.microsoft.identity.common.java.providers.microsoft.microsoftsts.MicrosoftStsTokenResponse
import com.microsoft.identity.common.java.providers.nativeauth.IApiResponse
import com.microsoft.identity.common.java.providers.nativeauth.interactors.InnerError
import com.microsoft.identity.common.java.util.ObjectMapper
import com.microsoft.identity.common.java.util.isCredentialRequired
import com.microsoft.identity.common.java.util.isInvalidAuthenticationType
import com.microsoft.identity.common.java.util.isInvalidAuthenticationMethod
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
): IApiResponse(statusCode) {

    companion object {
        private val TAG = SignInTokenApiResponse::class.java.simpleName
    }

    fun toResult(): SignInTokenApiResult {
        LogSession.logMethodCall(TAG)

        return when (statusCode) {

            // Handle 400 errors
            HttpURLConnection.HTTP_BAD_REQUEST -> {
                if (error.isInvalidGrant()) {
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

            // Handle success and redirect
            HttpURLConnection.HTTP_OK -> {
                if (accessToken.isNullOrBlank()) {
                    SignInTokenApiResult.UnknownError(
                        error = "invalid_state",
                        errorDescription = "SignIn /token did not return an access_token with success",
                        details = details,
                        errorCodes = errorCodes
                    )
                }
                // Seems that id_token and refresh_token are nullable in some scenarios, so I
                // removed the error handling for them
                else {
                    // TODO: Do we still need this?
                    val originalJson = ObjectMapper.serializeObjectToJsonString(this)
                    val tokenResponse = ObjectMapper.deserializeJsonStringToObject(
                        originalJson,
                        MicrosoftStsTokenResponse::class.java
                    )
                    // TODO until mock API returns it
                    tokenResponse.idToken = "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsImtpZCI6ImJXOFpjTWpCQ25KWlMtaWJYNVVRRE5TdHZ4NCJ9.eyJ2ZXIiOiIyLjAiLCJpc3MiOiJodHRwczovL2xvZ2luLm1pY3Jvc29mdG9ubGluZS5jb20vOTE4ODA0MGQtNmM2Ny00YzViLWIxMTItMzZhMzA0YjY2ZGFkL3YyLjAiLCJzdWIiOiJBQUFBQUFBQUFBQUFBQUFBQUFBQUFQV0t1dkFxNDdlZmxzSjdNd2dpbWtVIiwiYXVkIjoiMDk4NGE3YjYtYmMxMy00MTQxLThiMGQtOGY3NjdlMTM2YmI3IiwiZXhwIjoxNjgxNDYzMDIzLCJpYXQiOjE2ODEzNzYzMjMsIm5iZiI6MTY4MTM3NjMyMywibmFtZSI6IlNhbW15IE9kZW5ob3ZlbiIsInByZWZlcnJlZF91c2VybmFtZSI6InNhbW15Lm9kZW5ob3ZlbkBnbWFpbC5jb20iLCJvaWQiOiIwMDAwMDAwMC0wMDAwLTAwMDAtNDQ3Yi0yNzNlZWMwMGRkNTciLCJ0aWQiOiI5MTg4MDQwZC02YzY3LTRjNWItYjExMi0zNmEzMDRiNjZkYWQiLCJhaW8iOiJEVGhGY3dSdFgwT0tqNXBTSEdOZUdVR1NVNGhaNFJoNU83TmhnUjYzMnpldEM5WmgzM3dWRypXeUJqIVFPM0twU0dXRVRla25sMDA1WE8qQWg0bXhRamVuR2VRZXIqakx3Nypkcmh1cDdTc0NJRThraUlsempYMDZuaWNWNFFFTGZxR3BoYkRuemI0RWtOZEZXTHBOTmhJJCJ9.WRe3tNCsvIuYfw8bIY1D8spFJXg-ZrGm2MiDYkUlfNR-bbW_7niJg372U-wG65OfRA99NauR511IKWcg6i5FRzx3Xcx4AfGCJhOCGagD4fRDaU4I1pE-C3lJlGY6bIodTSXIlS0VUPw_YmvzQ-X9lyJP-l-89hxQNtvCSbdm2zlSJPvdynJmRH58s4PTJSGuv7zn5Jq-Uc0s2DZx0nLfBfLee8bQpaUQamaxQ6Noz7zAjz7-TkCRriqZyvJLE9dBvRd6uSzYR_qm4VDpsH5wnGsMRvW7F_hcjjZo2gZyxI6BWy0kONF8juL6H1ar1EMi3Xn9jIU1Tde3yafjTpkmyw"
                    tokenResponse.clientInfo = "eyJ2ZXIiOiIxLjAiLCJzdWIiOiJBQUFBQUFBQUFBQUFBQUFBQUFBQUFQV0t1dkFxNDdlZmxzSjdNd2dpbWtVIiwibmFtZSI6IlNhbW15IE9kZW5ob3ZlbiIsInByZWZlcnJlZF91c2VybmFtZSI6InNhbW15Lm9kZW5ob3ZlbkBnbWFpbC5jb20iLCJvaWQiOiIwMDAwMDAwMC0wMDAwLTAwMDAtNDQ3Yi0yNzNlZWMwMGRkNTciLCJ0aWQiOiI5MTg4MDQwZC02YzY3LTRjNWItYjExMi0zNmEzMDRiNjZkYWQiLCJob21lX29pZCI6IjAwMDAwMDAwLTAwMDAtMDAwMC00NDdiLTI3M2VlYzAwZGQ1NyIsInVpZCI6IjAwMDAwMDAwLTAwMDAtMDAwMC00NDdiLTI3M2VlYzAwZGQ1NyIsInV0aWQiOiI5MTg4MDQwZC02YzY3LTRjNWItYjExMi0zNmEzMDRiNjZkYWQifQ"
                    return SignInTokenApiResult.Success(
                        tokenResponse = tokenResponse
                    )
                }
            }

            // Catch uncommon status codes
            else -> {
                SignInTokenApiResult.UnknownError(
                    error = error.orEmpty(),
                    errorDescription = errorDescription.orEmpty(),
                    details = details,
                    errorCodes = errorCodes
                )
            }
        }
    }
}