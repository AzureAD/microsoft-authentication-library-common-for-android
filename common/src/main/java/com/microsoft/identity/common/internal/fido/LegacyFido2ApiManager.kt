package com.microsoft.identity.common.internal.fido

import android.content.Context
import com.google.android.gms.fido.common.Transport
import com.google.android.gms.fido.fido2.Fido2ApiClient
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialDescriptor
import com.google.android.gms.tasks.OnSuccessListener
import com.microsoft.identity.common.internal.providers.oauth2.WebViewAuthorizationFragment
import java.util.ArrayList

class LegacyFido2ApiManager (val context: Context, val fragment: WebViewAuthorizationFragment) : IFidoManager {

    private val legacyApi = Fido2ApiClient(context)

    override suspend fun authenticate(challenge: String,
                                      relyingPartyIdentifier: String,
                                      allowedCredentials: List<String>?,
                                      userVerificationPolicy: String): String {
        val publicKeyCredentialDescriptorList = ArrayList<PublicKeyCredentialDescriptor>()
        allowedCredentials?.let {
            for (id in allowedCredentials) {
                publicKeyCredentialDescriptorList.add(
                    PublicKeyCredentialDescriptor("public-key", id.toByteArray(), ArrayList())
                )
            }
        }
        val requestOptions = com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialRequestOptions.Builder()
            .setChallenge(challenge.toByteArray())
            .setRpId(relyingPartyIdentifier)
            .setAllowList(publicKeyCredentialDescriptorList)
            .build()
        val result = legacyApi.getSignPendingIntent(requestOptions)

        result.addOnSuccessListener(OnSuccessListener { pendingIntent ->
            if (pendingIntent != null) {
                fragment.fidoLauncher.launch(
                    LegacyFido2ApiObject(
                        callback = { assertion, succeeded ->
                            if (succeeded) {
                                respondToChallenge(
                                    submitUrl = submitUrl,
                                    assertion = assertion,
                                    context = context,
                                    span = span
                                )
                            } else {
                                respondToChallengeWithError(
                                    submitUrl = submitUrl,
                                    context = context,
                                    span = span,
                                    errorMessage = assertion
                                )
                            }
                        },
                        pendingIntent = pendingIntent
                    ))
                fragment.fidoLauncher.await()
            }
        })
    }
}