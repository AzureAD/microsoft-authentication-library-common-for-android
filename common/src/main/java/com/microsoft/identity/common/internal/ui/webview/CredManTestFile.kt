package com.microsoft.identity.common.internal.ui.webview

import android.app.Activity
import android.util.Base64
import android.util.Log
import androidx.credentials.*
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.coroutineScope
import kotlinx.coroutines.*
import java.security.SecureRandom

class CredManTestFile(val activity: Activity) {

    fun callCredManRegister(lifecycle: Lifecycle) {
        val jsonRequest = """{"challenge":"<challenge>","rp":{"name":"Microsoft","id":"login.microsoft.com"},"user":{"id":"2HzoHm_hY0CjuEESY9tY6-3SdjmNHOoNqaPDcZGzsr0","name":"user1","displayName":"user1"},"pubKeyCredParams":[{"type":"public-key","alg":-7},{"type":"public-key","alg":-257}],"timeout":1800000,"excludeCredentials":[],"authenticatorSelection":{"authenticatorAttachment":"platform","residentKey":"required"}}""".trimIndent().replace("<challenge>", getEncodedChallenge())
        val credentialManager = CredentialManager.create(activity)
        val createPublicKeyCredentialRequest = CreatePublicKeyCredentialRequest(requestJson = jsonRequest)

        lifecycle.coroutineScope.launch {
            try {
                val result = credentialManager.createCredential(
                    request = createPublicKeyCredentialRequest,
                    context = activity,
                ) as CreatePublicKeyCredentialResponse

                val id = result.registrationResponseJson

                id?.let {
                    Log.v("CredentialTag: ", id)
                }
            } catch (e: GetCredentialException) {
                e.message?.let { Log.e("CredentialTag: ", it) }
            }
        }

    }

    fun callCredManSignIn(lifecycle: Lifecycle) {
        //testing something here
        val credentialManager = CredentialManager.create(activity)
        val jsonRequest =
            """{"challenge":"<challenge>","allowCredentials":[{ type: "public-key", id: "ByxI8eo3JCg2aC5nuR4pJw" }],"timeout":1800000,"userVerification":"required","rpId":"login.microsoft.com"}""".trimIndent().replace("<challenge>", getEncodedChallenge())
        val getPublicKeyCredentialOption = GetPublicKeyCredentialOption(jsonRequest)
        val getCredentialRequest = GetCredentialRequest(listOf(getPublicKeyCredentialOption))

        lifecycle.coroutineScope.launch {
            try {
                val result = credentialManager.getCredential(
                    request = getCredentialRequest,
                    context = activity,
                ) as GetCredentialResponse

                val credential = result.credential as PublicKeyCredential
                val response = credential.authenticationResponseJson

                response?.let {
                    Log.v("CredentialTag: ", response)
                }
            } catch (e: GetCredentialException) {
                e.message?.let { Log.e("CredentialTag: ", it) }
            }
        }
    }

    private fun getEncodedChallenge(): String {
        val random = SecureRandom()
        val bytes = ByteArray(32)
        random.nextBytes(bytes)
        return Base64.encodeToString(
            bytes,
            Base64.NO_WRAP or Base64.URL_SAFE or Base64.NO_PADDING
        )
    }
}