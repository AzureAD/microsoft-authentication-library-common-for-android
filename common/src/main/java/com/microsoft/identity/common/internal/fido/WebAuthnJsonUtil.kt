package com.microsoft.identity.common.internal.fido

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

class WebAuthnJsonUtil {
    companion object {
        @JvmStatic
        fun AuthFidoChallengeToPublicKeyCredentialRequestOptions(challengeObject: AuthFidoChallenge): String {
            //Create classes
            val publicKeyCredDescriptorList = ArrayList<PublicKeyCredentialDescriptor>()
            for (id in challengeObject.allowedCredentials) {
                publicKeyCredDescriptorList.add(
                    PublicKeyCredentialDescriptor(
                        type = "public-key",
                        id = id
                    )
                )
            }

            val request = PublicKeyCredentialRequestOptions(
                challenge = challengeObject.challenge,
                rpId = challengeObject.relyingPartyIdentifier,
                allowCredentials = publicKeyCredDescriptorList,
                userVerification = challengeObject.userVerificationPolicy
            )

            val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
            val jsonAdapter = moshi.adapter(PublicKeyCredentialRequestOptions::class.java)
            return jsonAdapter.toJson(request).toString()
        }
    }
}