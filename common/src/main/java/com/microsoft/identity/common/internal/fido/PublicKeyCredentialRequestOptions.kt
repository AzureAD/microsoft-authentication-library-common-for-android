package com.microsoft.identity.common.internal.fido

import com.squareup.moshi.JsonClass
//https://w3c.github.io/webauthn/#dictdef-publickeycredentialrequestoptions

data class PublicKeyCredentialRequestOptions(
    val challenge: String,
    val rpId: String,
    val allowCredentials: List<PublicKeyCredentialDescriptor>,
    val userVerification: String
)
