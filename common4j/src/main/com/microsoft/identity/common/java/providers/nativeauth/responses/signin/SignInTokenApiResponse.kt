package com.microsoft.identity.common.java.providers.nativeauth.responses.signin

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.microsoft.identity.common.java.exception.ClientException
import com.microsoft.identity.common.java.providers.microsoft.microsoftsts.MicrosoftStsTokenResponse
import com.microsoft.identity.common.java.providers.nativeauth.IApiResponse
import com.microsoft.identity.common.java.providers.nativeauth.interactors.InnerError
import com.microsoft.identity.common.java.util.ObjectMapper
import com.microsoft.identity.common.java.util.isCredentialRequired
import com.microsoft.identity.common.java.util.isInvalidGrant
import com.microsoft.identity.common.java.util.isOtpCodeIncorrect
import com.microsoft.identity.common.java.util.isPasswordIncorrect
import com.microsoft.identity.common.java.util.isUserAccountDoesNotExist
import java.net.HttpURLConnection

// TODO are these fields considered PII?
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
    @Expose @SerializedName("error_codes") val errorCodes: List<Int>?,
    @Expose @SerializedName("inner_errors") val innerErrors: List<InnerError>?,
    @Expose @SerializedName("credential_token") val credentialToken: String?,
): IApiResponse(statusCode) {
    fun toResult(): SignInTokenApiResult {
        if (statusCode >= HttpURLConnection.HTTP_BAD_REQUEST) {
            if (error.isCredentialRequired()) {
                if (credentialToken.isNullOrBlank()) {
                    throw ClientException("credential_token is null or empty")
                }
                return SignInTokenApiResult.CredentialRequired(credentialToken = credentialToken)
            } else if (error.isInvalidGrant()) {
                if (errorCodes.isNullOrEmpty()) {
                    SignInTokenApiResult.UnknownError(error, errorDescription)
                } else if (errorCodes[0].isUserAccountDoesNotExist()) {
                    return SignInTokenApiResult.UserNotFound(error = error.orEmpty(), errorDescription = errorDescription.orEmpty())
                } else if (errorCodes[0].isPasswordIncorrect()){
                    return SignInTokenApiResult.PasswordIncorrect(error = error.orEmpty(), errorDescription = errorDescription.orEmpty())
                } else if (errorCodes[0].isOtpCodeIncorrect()) {
                    return SignInTokenApiResult.CodeIncorrect(error = error.orEmpty(), errorDescription = errorDescription.orEmpty())
                } else {
                    return SignInTokenApiResult.UnknownError(error, errorDescription)
                }
            }
            // TODO log the API response, in a PII-safe way
            return SignInTokenApiResult.UnknownError(error, errorDescription)

        } else {
            if (accessToken.isNullOrBlank()) {
                throw ClientException("access_token is null or empty")
            }
            if (refreshToken.isNullOrBlank()) {
                throw ClientException("refresh_token is null or empty")
            }
            if (idToken.isNullOrBlank()) {
                throw ClientException("id_token is null or empty")
            } else {
                val originalJson = ObjectMapper.serializeObjectToJsonString(this)
                val tokenResponse = ObjectMapper.deserializeJsonStringToObject(
                    originalJson,
                    MicrosoftStsTokenResponse::class.java
                )
                // TODO until mock API returns it
                tokenResponse.idToken = "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsImtpZCI6ImJXOFpjTWpCQ25KWlMtaWJYNVVRRE5TdHZ4NCJ9.eyJ2ZXIiOiIyLjAiLCJpc3MiOiJodHRwczovL2xvZ2luLm1pY3Jvc29mdG9ubGluZS5jb20vOTE4ODA0MGQtNmM2Ny00YzViLWIxMTItMzZhMzA0YjY2ZGFkL3YyLjAiLCJzdWIiOiJBQUFBQUFBQUFBQUFBQUFBQUFBQUFQV0t1dkFxNDdlZmxzSjdNd2dpbWtVIiwiYXVkIjoiMDk4NGE3YjYtYmMxMy00MTQxLThiMGQtOGY3NjdlMTM2YmI3IiwiZXhwIjoxNjgxNDYzMDIzLCJpYXQiOjE2ODEzNzYzMjMsIm5iZiI6MTY4MTM3NjMyMywibmFtZSI6IlNhbW15IE9kZW5ob3ZlbiIsInByZWZlcnJlZF91c2VybmFtZSI6InNhbW15Lm9kZW5ob3ZlbkBnbWFpbC5jb20iLCJvaWQiOiIwMDAwMDAwMC0wMDAwLTAwMDAtNDQ3Yi0yNzNlZWMwMGRkNTciLCJ0aWQiOiI5MTg4MDQwZC02YzY3LTRjNWItYjExMi0zNmEzMDRiNjZkYWQiLCJhaW8iOiJEVGhGY3dSdFgwT0tqNXBTSEdOZUdVR1NVNGhaNFJoNU83TmhnUjYzMnpldEM5WmgzM3dWRypXeUJqIVFPM0twU0dXRVRla25sMDA1WE8qQWg0bXhRamVuR2VRZXIqakx3Nypkcmh1cDdTc0NJRThraUlsempYMDZuaWNWNFFFTGZxR3BoYkRuemI0RWtOZEZXTHBOTmhJJCJ9.WRe3tNCsvIuYfw8bIY1D8spFJXg-ZrGm2MiDYkUlfNR-bbW_7niJg372U-wG65OfRA99NauR511IKWcg6i5FRzx3Xcx4AfGCJhOCGagD4fRDaU4I1pE-C3lJlGY6bIodTSXIlS0VUPw_YmvzQ-X9lyJP-l-89hxQNtvCSbdm2zlSJPvdynJmRH58s4PTJSGuv7zn5Jq-Uc0s2DZx0nLfBfLee8bQpaUQamaxQ6Noz7zAjz7-TkCRriqZyvJLE9dBvRd6uSzYR_qm4VDpsH5wnGsMRvW7F_hcjjZo2gZyxI6BWy0kONF8juL6H1ar1EMi3Xn9jIU1Tde3yafjTpkmyw"
                tokenResponse.clientInfo = "eyJ2ZXIiOiIxLjAiLCJzdWIiOiJBQUFBQUFBQUFBQUFBQUFBQUFBQUFQV0t1dkFxNDdlZmxzSjdNd2dpbWtVIiwibmFtZSI6IlNhbW15IE9kZW5ob3ZlbiIsInByZWZlcnJlZF91c2VybmFtZSI6InNhbW15Lm9kZW5ob3ZlbkBnbWFpbC5jb20iLCJvaWQiOiIwMDAwMDAwMC0wMDAwLTAwMDAtNDQ3Yi0yNzNlZWMwMGRkNTciLCJ0aWQiOiI5MTg4MDQwZC02YzY3LTRjNWItYjExMi0zNmEzMDRiNjZkYWQiLCJob21lX29pZCI6IjAwMDAwMDAwLTAwMDAtMDAwMC00NDdiLTI3M2VlYzAwZGQ1NyIsInVpZCI6IjAwMDAwMDAwLTAwMDAtMDAwMC00NDdiLTI3M2VlYzAwZGQ1NyIsInV0aWQiOiI5MTg4MDQwZC02YzY3LTRjNWItYjExMi0zNmEzMDRiNjZkYWQifQ"
                return SignInTokenApiResult.Success(tokenResponse = tokenResponse)
            }
        }
    }
}