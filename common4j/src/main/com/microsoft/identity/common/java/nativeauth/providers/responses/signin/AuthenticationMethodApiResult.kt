package com.microsoft.identity.common.java.nativeauth.providers.responses.signin

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.microsoft.identity.common.java.nativeauth.util.ILoggable

data class AuthenticationMethodApiResult(
    @Expose @SerializedName("id") val id: String,
    @Expose @SerializedName("challenge_type") val challengeType: String,
    @SerializedName("login_hint") val loginHint: String,
    @Expose @SerializedName("challenge_channel") val challengeChannel: String,
) : ILoggable {
    override fun toUnsanitizedString() = "AuthenticationMethod(id=$id, " +
            "challenge_type=$challengeType, login_hint=$loginHint, " +
            "challenge_channel=$challengeChannel)"

    override fun toString()= "AuthenticationMethod(id=$id)"
}