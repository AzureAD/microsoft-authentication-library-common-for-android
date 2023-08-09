package com.microsoft.identity.common.java.providers.nativeauth

import com.microsoft.identity.common.java.exception.ClientException
import com.microsoft.identity.common.java.net.HttpResponse
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class NativeAuthResponseHandlerTest {
    private val handler = NativeAuthResponseHandler()
    private val nullHttpResponse = HttpResponse(400, null, null)

    @Test
    fun testSignUpStartEmptyResponse() {
        val response = handler.getSignUpStartResultFromHttpResponse(nullHttpResponse)

        Assert.assertEquals(response.statusCode, nullHttpResponse.statusCode)
        Assert.assertNull(response.challengeType)
        Assert.assertNull(response.error)
        Assert.assertNull(response.details)
        Assert.assertNull(response.signupToken)
        Assert.assertNull(response.error)
        Assert.assertNull(response.errorDescription)
        Assert.assertNull(response.invalidAttributes)
        Assert.assertNull(response.unverifiedAttributes)
    }

    @Test
    fun testSignUpChallengeEmptyResponse() {
        val response = handler.getSignUpChallengeResultFromHttpResponse(nullHttpResponse)

        Assert.assertEquals(response.statusCode, nullHttpResponse.statusCode)
        Assert.assertNull(response.challengeType)
        Assert.assertNull(response.challengeChannel)
        Assert.assertNull(response.error)
        Assert.assertNull(response.details)
        Assert.assertNull(response.challengeTargetLabel)
        Assert.assertNull(response.bindingMethod)
        Assert.assertNull(response.codeLength)
        Assert.assertNull(response.error)
        Assert.assertNull(response.errorDescription)
        Assert.assertNull(response.interval)
        Assert.assertNull(response.signupToken)
    }

    @Test
    fun testSignUpContinueEmptyResponse() {
        val response = handler.getSignUpContinueResultFromHttpResponse(nullHttpResponse)

        Assert.assertEquals(response.statusCode, nullHttpResponse.statusCode)
        Assert.assertNull(response.error)
        Assert.assertNull(response.details)
        Assert.assertNull(response.signInSLT)
        Assert.assertNull(response.invalidAttributes)
        Assert.assertNull(response.details)
        Assert.assertNull(response.expiresIn)
        Assert.assertNull(response.error)
        Assert.assertNull(response.errorDescription)
        Assert.assertNull(response.requiredAttributes)
        Assert.assertNull(response.signupToken)
        Assert.assertNull(response.unverifiedAttributes)
    }

    @Test
    fun testSignInInitiateEmptyResponse() {
        val response = handler.getSignInInitiateResultFromHttpResponse(nullHttpResponse)

        Assert.assertEquals(response.statusCode, nullHttpResponse.statusCode)
        Assert.assertNull(response.error)
        Assert.assertNull(response.details)
        Assert.assertNull(response.challengeType)
        Assert.assertNull(response.credentialToken)
        Assert.assertNull(response.details)
        Assert.assertNull(response.errorUri)
        Assert.assertNull(response.error)
        Assert.assertNull(response.errorDescription)
        Assert.assertNull(response.innerErrors)
    }

    @Test
    fun testSignInChallengeEmptyResponse() {
        val response = handler.getSignInChallengeResultFromHttpResponse(nullHttpResponse)

        Assert.assertEquals(response.statusCode, nullHttpResponse.statusCode)
        Assert.assertNull(response.error)
        Assert.assertNull(response.details)
        Assert.assertNull(response.challengeType)
        Assert.assertNull(response.credentialToken)
        Assert.assertNull(response.details)
        Assert.assertNull(response.errorUri)
        Assert.assertNull(response.error)
        Assert.assertNull(response.errorDescription)
        Assert.assertNull(response.innerErrors)
        Assert.assertNull(response.challengeChannel)
        Assert.assertNull(response.challengeTargetLabel)
        Assert.assertNull(response.interval)
    }

    @Test
    fun testSignInTokenEmptyResponse() {
        val response = handler.getSignInTokenApiResultFromHttpResponse(nullHttpResponse)

        Assert.assertNotNull(response)
    }

    @Test
    fun testResetPasswordStartEmptyResponse() {
        val response = handler.getResetPasswordChallengeApiResponseFromHttpResponse(nullHttpResponse)

        Assert.assertEquals(response.statusCode, nullHttpResponse.statusCode)
        Assert.assertNull(response.error)
        Assert.assertNull(response.details)
        Assert.assertNull(response.challengeType)
        Assert.assertNull(response.challengeChannel)
        Assert.assertNull(response.challengeTargetLabel)
        Assert.assertNull(response.passwordResetToken)
        Assert.assertNull(response.details)
        Assert.assertNull(response.errorUri)
        Assert.assertNull(response.error)
        Assert.assertNull(response.errorDescription)
        Assert.assertNull(response.innerErrors)
        Assert.assertNull(response.bindingMethod)
        Assert.assertNull(response.interval)
    }

    @Test
    fun testResetPasswordChallengeEmptyResponse() {
        val response = handler.getResetPasswordStartApiResponseFromHttpResponse(nullHttpResponse)

        Assert.assertEquals(response.statusCode, nullHttpResponse.statusCode)
        Assert.assertNull(response.error)
        Assert.assertNull(response.details)
        Assert.assertNull(response.challengeType)
        Assert.assertNull(response.passwordResetToken)
        Assert.assertNull(response.details)
        Assert.assertNull(response.errorUri)
        Assert.assertNull(response.error)
        Assert.assertNull(response.errorDescription)
        Assert.assertNull(response.innerErrors)
    }

    @Test
    fun testResetPasswordContinueEmptyResponse() {
        val response = handler.getResetPasswordContinueApiResponseFromHttpResponse(nullHttpResponse)

        Assert.assertEquals(response.statusCode, nullHttpResponse.statusCode)
        Assert.assertNull(response.error)
        Assert.assertNull(response.details)
        Assert.assertNull(response.challengeType)
        Assert.assertNull(response.passwordResetToken)
        Assert.assertNull(response.passwordSubmitToken)
        Assert.assertNull(response.details)
        Assert.assertNull(response.errorUri)
        Assert.assertNull(response.error)
        Assert.assertNull(response.errorDescription)
        Assert.assertNull(response.innerErrors)
        Assert.assertNull(response.expiresIn)
    }

    @Test
    fun testResetPasswordSubmitEmptyResponse() {
        val response = handler.getResetPasswordSubmitApiResponseFromHttpResponse(nullHttpResponse)

        Assert.assertEquals(response.statusCode, nullHttpResponse.statusCode)
        Assert.assertNull(response.error)
        Assert.assertNull(response.details)
        Assert.assertNull(response.passwordResetToken)
        Assert.assertNull(response.details)
        Assert.assertNull(response.errorUri)
        Assert.assertNull(response.error)
        Assert.assertNull(response.errorDescription)
        Assert.assertNull(response.innerErrors)
    }

    @Test
    fun testResetPasswordPollEmptyResponse() {
        val response = handler.getResetPasswordPollCompletionApiResponseFromHttpResponse(nullHttpResponse)

        Assert.assertEquals(response.statusCode, nullHttpResponse.statusCode)
        Assert.assertNull(response.error)
        Assert.assertNull(response.details)
        Assert.assertNull(response.details)
        Assert.assertNull(response.errorUri)
        Assert.assertNull(response.error)
        Assert.assertNull(response.errorDescription)
        Assert.assertNull(response.innerErrors)
    }
}