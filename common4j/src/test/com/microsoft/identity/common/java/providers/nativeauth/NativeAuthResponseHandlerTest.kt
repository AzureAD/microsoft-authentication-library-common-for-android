package com.microsoft.identity.common.java.providers.nativeauth

import com.microsoft.identity.common.java.net.HttpResponse
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class NativeAuthResponseHandlerTest {
    private val handler = NativeAuthResponseHandler()
    private val nullHttpResponse = HttpResponse(400, null, null)
    private val emptyHttpResponse = HttpResponse(400, "", mapOf())

    @Test
    fun testSignUpStartNullResponse() {
        val response = handler.getSignUpStartResultFromHttpResponse(nullHttpResponse)

        Assert.assertEquals(response.statusCode, nullHttpResponse.statusCode)
        Assert.assertNull(response.challengeType)
        Assert.assertNull(response.details)
        Assert.assertNull(response.signupToken)
        Assert.assertEquals(NativeAuthResponseHandler.DEFAULT_ERROR, response.error)
        Assert.assertEquals(NativeAuthResponseHandler.DEFAULT_ERROR_DESCRIPTION, response.errorDescription)
        Assert.assertNull(response.invalidAttributes)
        Assert.assertNull(response.unverifiedAttributes)
    }

    @Test
    fun testSignUpStartEmptyResponse() {
        val response = handler.getSignUpStartResultFromHttpResponse(emptyHttpResponse)

        Assert.assertEquals(response.statusCode, nullHttpResponse.statusCode)
        Assert.assertNull(response.challengeType)
        Assert.assertNull(response.details)
        Assert.assertNull(response.signupToken)
        Assert.assertEquals(NativeAuthResponseHandler.DEFAULT_ERROR, response.error)
        Assert.assertEquals(NativeAuthResponseHandler.DEFAULT_ERROR_DESCRIPTION, response.errorDescription)
        Assert.assertNull(response.invalidAttributes)
        Assert.assertNull(response.unverifiedAttributes)
    }

    @Test
    fun testSignUpChallengeNullResponse() {
        val response = handler.getSignUpChallengeResultFromHttpResponse(nullHttpResponse)

        Assert.assertEquals(response.statusCode, nullHttpResponse.statusCode)
        Assert.assertNull(response.challengeType)
        Assert.assertNull(response.challengeChannel)
        Assert.assertNull(response.details)
        Assert.assertNull(response.challengeTargetLabel)
        Assert.assertNull(response.bindingMethod)
        Assert.assertNull(response.codeLength)
        Assert.assertEquals(NativeAuthResponseHandler.DEFAULT_ERROR, response.error)
        Assert.assertEquals(NativeAuthResponseHandler.DEFAULT_ERROR_DESCRIPTION, response.errorDescription)
        Assert.assertNull(response.interval)
        Assert.assertNull(response.signupToken)
    }

    @Test
    fun testSignUpChallengeEmptyResponse() {
        val response = handler.getSignUpChallengeResultFromHttpResponse(emptyHttpResponse)

        Assert.assertEquals(response.statusCode, nullHttpResponse.statusCode)
        Assert.assertNull(response.challengeType)
        Assert.assertNull(response.challengeChannel)
        Assert.assertNull(response.details)
        Assert.assertNull(response.challengeTargetLabel)
        Assert.assertNull(response.bindingMethod)
        Assert.assertNull(response.codeLength)
        Assert.assertEquals(NativeAuthResponseHandler.DEFAULT_ERROR, response.error)
        Assert.assertEquals(NativeAuthResponseHandler.DEFAULT_ERROR_DESCRIPTION, response.errorDescription)
        Assert.assertNull(response.interval)
        Assert.assertNull(response.signupToken)
    }

    @Test
    fun testSignUpContinueNullResponse() {
        val response = handler.getSignUpContinueResultFromHttpResponse(nullHttpResponse)

        Assert.assertEquals(response.statusCode, nullHttpResponse.statusCode)
        Assert.assertNull(response.details)
        Assert.assertNull(response.signInSLT)
        Assert.assertNull(response.invalidAttributes)
        Assert.assertNull(response.details)
        Assert.assertNull(response.expiresIn)
        Assert.assertEquals(NativeAuthResponseHandler.DEFAULT_ERROR, response.error)
        Assert.assertEquals(NativeAuthResponseHandler.DEFAULT_ERROR_DESCRIPTION, response.errorDescription)
        Assert.assertNull(response.requiredAttributes)
        Assert.assertNull(response.signupToken)
        Assert.assertNull(response.unverifiedAttributes)
    }

    @Test
    fun testSignUpContinueEmptyResponse() {
        val response = handler.getSignUpContinueResultFromHttpResponse(emptyHttpResponse)

        Assert.assertEquals(response.statusCode, nullHttpResponse.statusCode)
        Assert.assertNull(response.details)
        Assert.assertNull(response.signInSLT)
        Assert.assertNull(response.invalidAttributes)
        Assert.assertNull(response.details)
        Assert.assertNull(response.expiresIn)
        Assert.assertEquals(NativeAuthResponseHandler.DEFAULT_ERROR, response.error)
        Assert.assertEquals(NativeAuthResponseHandler.DEFAULT_ERROR_DESCRIPTION, response.errorDescription)
        Assert.assertNull(response.requiredAttributes)
        Assert.assertNull(response.signupToken)
        Assert.assertNull(response.unverifiedAttributes)
    }

    @Test
    fun testSignInInitiateNullResponse() {
        val response = handler.getSignInInitiateResultFromHttpResponse(nullHttpResponse)

        Assert.assertEquals(response.statusCode, nullHttpResponse.statusCode)
        Assert.assertNull(response.details)
        Assert.assertNull(response.challengeType)
        Assert.assertNull(response.credentialToken)
        Assert.assertNull(response.details)
        Assert.assertNull(response.errorUri)
        Assert.assertEquals(NativeAuthResponseHandler.DEFAULT_ERROR, response.error)
        Assert.assertEquals(NativeAuthResponseHandler.DEFAULT_ERROR_DESCRIPTION, response.errorDescription)
        Assert.assertNull(response.innerErrors)
    }

    @Test
    fun testSignInInitiateEmptyResponse() {
        val response = handler.getSignInInitiateResultFromHttpResponse(emptyHttpResponse)

        Assert.assertEquals(response.statusCode, nullHttpResponse.statusCode)
        Assert.assertNull(response.details)
        Assert.assertNull(response.challengeType)
        Assert.assertNull(response.credentialToken)
        Assert.assertNull(response.details)
        Assert.assertNull(response.errorUri)
        Assert.assertEquals(NativeAuthResponseHandler.DEFAULT_ERROR, response.error)
        Assert.assertEquals(NativeAuthResponseHandler.DEFAULT_ERROR_DESCRIPTION, response.errorDescription)
        Assert.assertNull(response.innerErrors)
    }

    @Test
    fun testSignInChallengeNullResponse() {
        val response = handler.getSignInChallengeResultFromHttpResponse(nullHttpResponse)

        Assert.assertEquals(response.statusCode, nullHttpResponse.statusCode)
        Assert.assertNull(response.details)
        Assert.assertNull(response.challengeType)
        Assert.assertNull(response.credentialToken)
        Assert.assertNull(response.details)
        Assert.assertNull(response.errorUri)
        Assert.assertEquals(NativeAuthResponseHandler.DEFAULT_ERROR, response.error)
        Assert.assertEquals(NativeAuthResponseHandler.DEFAULT_ERROR_DESCRIPTION, response.errorDescription)
        Assert.assertNull(response.innerErrors)
        Assert.assertNull(response.challengeChannel)
        Assert.assertNull(response.challengeTargetLabel)
        Assert.assertNull(response.interval)
    }

    @Test
    fun testSignInChallengeEmptyResponse() {
        val response = handler.getSignInChallengeResultFromHttpResponse(emptyHttpResponse)

        Assert.assertEquals(response.statusCode, nullHttpResponse.statusCode)
        Assert.assertNull(response.details)
        Assert.assertNull(response.challengeType)
        Assert.assertNull(response.credentialToken)
        Assert.assertNull(response.details)
        Assert.assertNull(response.errorUri)
        Assert.assertEquals(NativeAuthResponseHandler.DEFAULT_ERROR, response.error)
        Assert.assertEquals(NativeAuthResponseHandler.DEFAULT_ERROR_DESCRIPTION, response.errorDescription)
        Assert.assertNull(response.innerErrors)
        Assert.assertNull(response.challengeChannel)
        Assert.assertNull(response.challengeTargetLabel)
        Assert.assertNull(response.interval)
    }

    @Test
    fun testSignInTokenNullResponse() {
        val response = handler.getSignInTokenApiResultFromHttpResponse(nullHttpResponse)

        Assert.assertNotNull(response)
    }

    @Test
    fun testSignInTokenEmptyResponse() {
        val response = handler.getSignInTokenApiResultFromHttpResponse(emptyHttpResponse)

        Assert.assertNotNull(response)
    }

    @Test
    fun testResetPasswordStartNullResponse() {
        val response = handler.getResetPasswordChallengeApiResponseFromHttpResponse(nullHttpResponse)

        Assert.assertEquals(response.statusCode, nullHttpResponse.statusCode)
        Assert.assertNull(response.details)
        Assert.assertNull(response.challengeType)
        Assert.assertNull(response.challengeChannel)
        Assert.assertNull(response.challengeTargetLabel)
        Assert.assertNull(response.passwordResetToken)
        Assert.assertNull(response.details)
        Assert.assertNull(response.errorUri)
        Assert.assertEquals(NativeAuthResponseHandler.DEFAULT_ERROR, response.error)
        Assert.assertEquals(NativeAuthResponseHandler.DEFAULT_ERROR_DESCRIPTION, response.errorDescription)
        Assert.assertNull(response.innerErrors)
        Assert.assertNull(response.bindingMethod)
        Assert.assertNull(response.interval)
    }

    @Test
    fun testResetPasswordStartEmptyResponse() {
        val response = handler.getResetPasswordChallengeApiResponseFromHttpResponse(emptyHttpResponse)

        Assert.assertEquals(response.statusCode, nullHttpResponse.statusCode)
        Assert.assertNull(response.details)
        Assert.assertNull(response.challengeType)
        Assert.assertNull(response.challengeChannel)
        Assert.assertNull(response.challengeTargetLabel)
        Assert.assertNull(response.passwordResetToken)
        Assert.assertNull(response.details)
        Assert.assertNull(response.errorUri)
        Assert.assertEquals(NativeAuthResponseHandler.DEFAULT_ERROR, response.error)
        Assert.assertEquals(NativeAuthResponseHandler.DEFAULT_ERROR_DESCRIPTION, response.errorDescription)
        Assert.assertNull(response.innerErrors)
        Assert.assertNull(response.bindingMethod)
        Assert.assertNull(response.interval)
    }

    @Test
    fun testResetPasswordChallengeNullResponse() {
        val response = handler.getResetPasswordStartApiResponseFromHttpResponse(nullHttpResponse)

        Assert.assertEquals(response.statusCode, nullHttpResponse.statusCode)
        Assert.assertNull(response.details)
        Assert.assertNull(response.challengeType)
        Assert.assertNull(response.passwordResetToken)
        Assert.assertNull(response.details)
        Assert.assertNull(response.errorUri)
        Assert.assertEquals(NativeAuthResponseHandler.DEFAULT_ERROR, response.error)
        Assert.assertEquals(NativeAuthResponseHandler.DEFAULT_ERROR_DESCRIPTION, response.errorDescription)
        Assert.assertNull(response.innerErrors)
    }

    @Test
    fun testResetPasswordChallengeEmptyResponse() {
        val response = handler.getResetPasswordStartApiResponseFromHttpResponse(emptyHttpResponse)

        Assert.assertEquals(response.statusCode, nullHttpResponse.statusCode)
        Assert.assertNull(response.details)
        Assert.assertNull(response.challengeType)
        Assert.assertNull(response.passwordResetToken)
        Assert.assertNull(response.details)
        Assert.assertNull(response.errorUri)
        Assert.assertEquals(NativeAuthResponseHandler.DEFAULT_ERROR, response.error)
        Assert.assertEquals(NativeAuthResponseHandler.DEFAULT_ERROR_DESCRIPTION, response.errorDescription)
        Assert.assertNull(response.innerErrors)
    }

    @Test
    fun testResetPasswordContinueNullResponse() {
        val response = handler.getResetPasswordContinueApiResponseFromHttpResponse(nullHttpResponse)

        Assert.assertEquals(response.statusCode, nullHttpResponse.statusCode)
        Assert.assertNull(response.details)
        Assert.assertNull(response.challengeType)
        Assert.assertNull(response.passwordResetToken)
        Assert.assertNull(response.passwordSubmitToken)
        Assert.assertNull(response.details)
        Assert.assertNull(response.errorUri)
        Assert.assertEquals(NativeAuthResponseHandler.DEFAULT_ERROR, response.error)
        Assert.assertEquals(NativeAuthResponseHandler.DEFAULT_ERROR_DESCRIPTION, response.errorDescription)
        Assert.assertNull(response.innerErrors)
        Assert.assertNull(response.expiresIn)
    }

    @Test
    fun testResetPasswordContinueEmptyResponse() {
        val response = handler.getResetPasswordContinueApiResponseFromHttpResponse(emptyHttpResponse)

        Assert.assertEquals(response.statusCode, nullHttpResponse.statusCode)
        Assert.assertNull(response.details)
        Assert.assertNull(response.challengeType)
        Assert.assertNull(response.passwordResetToken)
        Assert.assertNull(response.passwordSubmitToken)
        Assert.assertNull(response.details)
        Assert.assertNull(response.errorUri)
        Assert.assertEquals(NativeAuthResponseHandler.DEFAULT_ERROR, response.error)
        Assert.assertEquals(NativeAuthResponseHandler.DEFAULT_ERROR_DESCRIPTION, response.errorDescription)
        Assert.assertNull(response.innerErrors)
        Assert.assertNull(response.expiresIn)
    }

    @Test
    fun testResetPasswordSubmitNullResponse() {
        val response = handler.getResetPasswordSubmitApiResponseFromHttpResponse(nullHttpResponse)

        Assert.assertEquals(response.statusCode, nullHttpResponse.statusCode)
        Assert.assertNull(response.details)
        Assert.assertNull(response.passwordResetToken)
        Assert.assertNull(response.details)
        Assert.assertNull(response.errorUri)
        Assert.assertEquals(NativeAuthResponseHandler.DEFAULT_ERROR, response.error)
        Assert.assertEquals(NativeAuthResponseHandler.DEFAULT_ERROR_DESCRIPTION, response.errorDescription)
        Assert.assertNull(response.innerErrors)
    }

    @Test
    fun testResetPasswordSubmitEmptyResponse() {
        val response = handler.getResetPasswordSubmitApiResponseFromHttpResponse(emptyHttpResponse)

        Assert.assertEquals(response.statusCode, nullHttpResponse.statusCode)
        Assert.assertNull(response.details)
        Assert.assertNull(response.passwordResetToken)
        Assert.assertNull(response.details)
        Assert.assertNull(response.errorUri)
        Assert.assertEquals(NativeAuthResponseHandler.DEFAULT_ERROR, response.error)
        Assert.assertEquals(NativeAuthResponseHandler.DEFAULT_ERROR_DESCRIPTION, response.errorDescription)
        Assert.assertNull(response.innerErrors)
    }

    @Test
    fun testResetPasswordPollNullResponse() {
        val response = handler.getResetPasswordPollCompletionApiResponseFromHttpResponse(nullHttpResponse)

        Assert.assertEquals(response.statusCode, nullHttpResponse.statusCode)
        Assert.assertNull(response.details)
        Assert.assertNull(response.details)
        Assert.assertNull(response.errorUri)
        Assert.assertEquals(NativeAuthResponseHandler.DEFAULT_ERROR, response.error)
        Assert.assertEquals(NativeAuthResponseHandler.DEFAULT_ERROR_DESCRIPTION, response.errorDescription)
        Assert.assertNull(response.innerErrors)
    }

    @Test
    fun testResetPasswordPollEmptyResponse() {
        val response = handler.getResetPasswordPollCompletionApiResponseFromHttpResponse(emptyHttpResponse)

        Assert.assertEquals(response.statusCode, nullHttpResponse.statusCode)
        Assert.assertNull(response.details)
        Assert.assertNull(response.details)
        Assert.assertNull(response.errorUri)
        Assert.assertEquals(NativeAuthResponseHandler.DEFAULT_ERROR, response.error)
        Assert.assertEquals(NativeAuthResponseHandler.DEFAULT_ERROR_DESCRIPTION, response.errorDescription)
        Assert.assertNull(response.innerErrors)
    }
}