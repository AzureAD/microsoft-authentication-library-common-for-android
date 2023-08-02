package com.microsoft.identity.common.internal.fido

import org.junit.Test

class WebAuthnJsonUtilTest {

    @Test
    fun testThatThisWorks() {
        val authChallenge = AuthFidoChallenge(
            challenge = "blah",
            relyingPartyIdentifier = "login.microsoft.com",
            userVerificationPolicy = "policy",
            version = "1.0",
            submitUrl = "submiturl",
            keyTypes = listOf("Passkey"),
            context = "contextString",
            allowedCredentials = listOf("123","456","789")
        )
        val result = WebAuthnJsonUtil.AuthFidoChallengeToPublicKeyCredentialRequestOptions(authChallenge)
        println(result)
    }
}