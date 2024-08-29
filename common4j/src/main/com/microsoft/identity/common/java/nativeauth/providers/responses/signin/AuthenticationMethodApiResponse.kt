package com.microsoft.identity.common.java.nativeauth.providers.responses.signin

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.microsoft.identity.common.java.nativeauth.util.ILoggable

data class AuthenticationMethodApiResponse(
    @Expose @SerializedName("id") val id: String?,
    @Expose @SerializedName("challenge_type") val challengeType: String?,
    @SerializedName("login_hint") val loginHint: String?,
    @Expose @SerializedName("challenge_channel") val challengeChannel: String?,
) : ILoggable {
    override fun toUnsanitizedString() = "AuthenticationMethod(id=$id, " +
            "challenge_type=$challengeType, login_hint=$loginHint, " +
            "challenge_channel=$challengeChannel)"

    override fun toString() = "AuthenticationMethod(id=$id)"
}

/**
 * Converts a list of [AuthenticationMethodApiResponse] to a list of [AuthenticationMethodApiResult]
 */
internal fun List<AuthenticationMethodApiResponse>.toListOfAuthenticationMethodApiResult(): List<AuthenticationMethodApiResult> {
    return this.map { it.toAuthenticationMethodApiResult() }
}

/**
 * Converts a [AuthenticationMethodApiResponse] API response to a [AuthenticationMethodApiResult]
 * @throws IllegalStateException if any of the fields are null or empty
 */
@Throws(IllegalStateException::class)
internal fun AuthenticationMethodApiResponse.toAuthenticationMethodApiResult(): AuthenticationMethodApiResult {
    return AuthenticationMethodApiResult(
        id = this.id ?: throw IllegalStateException("Required field id is empty"),
        challengeType = this.challengeType ?: throw IllegalStateException("Required field challengeType is empty"),
        loginHint = this.loginHint ?: throw IllegalStateException("Required loginHint id is empty"),
        challengeChannel = this.challengeChannel ?: throw IllegalStateException("Required challengeChannel id is empty")
    )
}