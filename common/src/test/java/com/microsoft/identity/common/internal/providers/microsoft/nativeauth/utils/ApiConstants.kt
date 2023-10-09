package com.microsoft.identity.common.internal.providers.microsoft.nativeauth.utils

import java.net.URL

interface ApiConstants {
    companion object {
        val signUpStartRequestUrl = URL("https://native-ux-mock-api.azurewebsites.net/1234/signup/v1.0/start")
        val signUpChallengeRequestUrl = URL("https://native-ux-mock-api.azurewebsites.net/1234/signup/v1.0/challenge")
        val signUpContinueRequestUrl = URL("https://native-ux-mock-api.azurewebsites.net/1234/signup/v1.0/continue")
        val signInInitiateRequestUrl = URL("https://native-ux-mock-api.azurewebsites.net/1234/oauth2/v2.0/initiate")
        val signInChallengeRequestUrl = URL("https://native-ux-mock-api.azurewebsites.net/1234/oauth2/v2.0/challenge")
        val signInTokenRequestUrl = URL("https://native-ux-mock-api.azurewebsites.net/1234/oauth2/v2.0/token")
        val ssprStartRequestUrl = URL("https://native-ux-mock-api.azurewebsites.net/1234/resetpassword/v1.0/start")
        val ssprChallengeRequestUrl = URL("https://native-ux-mock-api.azurewebsites.net/1234/resetpassword/v1.0/challenge")
        val ssprContinueRequestUrl = URL("https://native-ux-mock-api.azurewebsites.net/1234/resetpassword/v1.0/continue")
        val ssprSubmitRequestUrl = URL("https://native-ux-mock-api.azurewebsites.net/1234/resetpassword/v1.0/submit")
        val ssprPollCompletionRequestUrl = URL("https://native-ux-mock-api.azurewebsites.net/1234/resetpassword/v1.0/poll_completion")
        val tokenEndpoint = URL("https://contoso.com/1234/token")
    }
}
