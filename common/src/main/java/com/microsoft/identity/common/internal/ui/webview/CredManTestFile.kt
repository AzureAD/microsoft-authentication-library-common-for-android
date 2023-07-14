package com.microsoft.identity.common.internal.ui.webview

import android.app.Activity
import android.os.CancellationSignal
import android.util.Base64
import android.view.View
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.coroutineScope
import kotlinx.coroutines.*
import java.security.SecureRandom

class CredManTestFile(val activity: Activity) {

    fun callCredManRegister(lifecycle: Lifecycle) {
        val jsonRequest = """{"challenge":"<challenge>","rp":{"name":"Microsoft","id":"login.microsoft.com"},"user":{"id":"2HzoHm_hY0CjuEESY9tY6-3SdjmNHOoNqaPDcZGzsr0","name":"hello","displayName":"hello"},"pubKeyCredParams":[{"type":"public-key","alg":-7},{"type":"public-key","alg":-257}],"timeout":1800000,"excludeCredentials":[],"authenticatorSelection":{"authenticatorAttachment":"platform","residentKey":"required"}}""".trimIndent().replace("<challenge>", getEncodedChallenge())
        val credentialManager = CredentialManager.create(activity)
        val createPublicKeyCredentialRequest = CreatePublicKeyCredentialRequest(requestJson = jsonRequest)

        lifecycle.coroutineScope.launch {
            try {
                credentialManager.createCredential(
                    request = createPublicKeyCredentialRequest,
                    activity = activity,
                )
            } catch (e: GetCredentialException) {

            }
        }

    }

    fun callCredManSignIn(lifecycle: Lifecycle) {
        //testing something here
        val credentialManager = CredentialManager.create(activity)
        val jsonRequest =
            "{\"challenge\":\"<challenge>\",\"allowCredentials\":[],\"timeout\":1800000,\"userVerification\":\"required\",\"rpId\":\"login.microsoft.com\"}".trimIndent().replace("<challenge>", getEncodedChallenge())
        val getPublicKeyCredentialOption = GetPublicKeyCredentialOption(jsonRequest)
        val getCredentialRequest = GetCredentialRequest(listOf(getPublicKeyCredentialOption))

        lifecycle.coroutineScope.launch {
            try {
                val result = credentialManager.getCredential(
                    request = getCredentialRequest,
                    activity = activity,
                )
            } catch (e: GetCredentialException) {

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