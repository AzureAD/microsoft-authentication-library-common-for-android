package com.microsoft.identity.common.internal.fido

//https://w3c.github.io/webauthn/#dictdef-publickeycredentialdescriptor
data class PublicKeyCredentialDescriptor(
    val type: String,
    val id: String
)