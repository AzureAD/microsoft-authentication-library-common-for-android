//  Copyright (c) Microsoft Corporation.
//  All rights reserved.
//
//  This code is licensed under the MIT License.
//
//  Permission is hereby granted, free of charge, to any person obtaining a copy
//  of this software and associated documentation files(the "Software"), to deal
//  in the Software without restriction, including without limitation the rights
//  to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
//  copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions :
//
//  The above copyright notice and this permission notice shall be included in
//  all copies or substantial portions of the Software.
//
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//  THE SOFTWARE.
package com.microsoft.identity.common.java.nativeauth.providers

import com.microsoft.identity.common.java.AuthenticationConstants
import com.microsoft.identity.common.java.exception.ClientException
import com.microsoft.identity.common.java.interfaces.PlatformComponents
import com.microsoft.identity.common.java.nativeauth.commands.parameters.ResetPasswordStartCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.ResetPasswordSubmitCodeCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.ResetPasswordSubmitNewPasswordCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.SignInStartCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.SignInSubmitCodeCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.SignInSubmitPasswordCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.SignInWithContinuationTokenCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.SignUpStartCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.SignUpSubmitCodeCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.SignUpSubmitPasswordCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.SignUpSubmitUserAttributesCommandParameters
import com.microsoft.identity.common.java.nativeauth.providers.requests.NativeAuthRequest.Companion.toJsonString
import com.microsoft.identity.common.java.nativeauth.providers.requests.signin.SignInTokenRequest
import com.microsoft.identity.common.java.util.ObjectMapper
import com.microsoft.identity.common.nativeauth.ApiConstants
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.mockito.kotlin.mock
import java.net.URL

class NativeAuthRequestProviderTest {
    private val username = "user@email.com"
    private val password = "verySafePassword".toCharArray()
    private val clientId = "1234"
    private val tenant = "samtoso.onmicrosoft.com"
    private val tokenEndpoint = URL("https://contoso.com/1234/token")
    private val challengeType = "oob redirect"
    private val capabilities = "mfa_required registration_required"
    private val userAttributes = mapOf("city" to "Dublin")
    private val emptyUserAttributes = emptyMap<String, String>()
    private val oobGrantType = "oob"
    private val passwordGrantType = "password"
    private val oobCode = "123456"
    private val emptyString = ""
    private val emptyPassword = "".toCharArray()
    private val continuationToken = "uY29tL2F1dGhlbnRpY"
    private val challengeId = "902rnwfn3"
    private val correlationId = "jsdfo4nslkjsrg"
    private val challengeTarget = "mail@contoso.com"
    private val challengeChannel = "email"
    private val grantType = NativeAuthConstants.GrantType.OOB

    private val mockConfig = mockk<NativeAuthOAuth2Configuration> {
        every { getSignUpStartEndpoint() } returns ApiConstants.MockApi.signUpStartRequestUrl
        every { getSignUpChallengeEndpoint() } returns ApiConstants.MockApi.signUpChallengeRequestUrl
        every { getSignUpContinueEndpoint() } returns ApiConstants.MockApi.signUpContinueRequestUrl
        every { getSignInInitiateEndpoint() } returns ApiConstants.MockApi.signInInitiateRequestUrl
        every { getSignInChallengeEndpoint() } returns ApiConstants.MockApi.signInChallengeRequestUrl
        every { getSignInIntrospectEndpoint() } returns ApiConstants.MockApi.signInIntrospectRequestUrl
        every { getSignInTokenEndpoint() } returns ApiConstants.MockApi.signInTokenRequestUrl
        every { getResetPasswordStartEndpoint() } returns ApiConstants.MockApi.ssprStartRequestUrl
        every { getResetPasswordChallengeEndpoint() } returns ApiConstants.MockApi.ssprChallengeRequestUrl
        every { getResetPasswordContinueEndpoint() } returns ApiConstants.MockApi.ssprContinueRequestUrl
        every { getResetPasswordSubmitEndpoint() } returns ApiConstants.MockApi.ssprSubmitRequestUrl
        every { getResetPasswordPollCompletionEndpoint() } returns ApiConstants.MockApi.ssprPollCompletionRequestUrl
        every { getJITIntrospectEndpoint() } returns ApiConstants.MockApi.jitIntrospectRequestUrl
        every { getJITChallengeEndpoint() } returns ApiConstants.MockApi.jitChallengeRequestUrl
        every { getJITContinueEndpoint() } returns ApiConstants.MockApi.jitContinueRequestUrl
        every { challengeType } returns this@NativeAuthRequestProviderTest.challengeType
        every { clientId } returns this@NativeAuthRequestProviderTest.clientId
        every { capabilities } returns this@NativeAuthRequestProviderTest.capabilities
        every { useMockApiForNativeAuth } returns true
    }

    private val nativeAuthRequestProvider =
        NativeAuthRequestProvider(
            config = mockConfig
        )

    // signup start tests
    @Test
    fun testSignUpStartWithEmptyUsernameShouldNotThrowException() {
        val commandParameters = SignUpStartCommandParameters.builder()
            .platformComponents(mock<PlatformComponents>())
            .username(emptyString)
            .clientId(clientId)
            .correlationId(correlationId)
            .build()

        nativeAuthRequestProvider.createSignUpStartRequest(
            commandParameters = commandParameters
        )
    }

    @Test
    fun testSignUpStartWithEmptyPasswordShouldNotThrowException() {
        val commandParameters = SignUpStartCommandParameters.builder()
            .platformComponents(mock<PlatformComponents>())
            .username(username)
            .password(emptyPassword)
            .clientId(clientId)
            .correlationId(correlationId)
            .build()

        val request = nativeAuthRequestProvider.createSignUpStartRequest(
            commandParameters = commandParameters
        )

        Assert.assertNotNull(request)
    }

    @Test(expected = ClientException::class)
    fun testSignUpStartWithEmptyClientIdShouldThrowException() {
        every { mockConfig.clientId } returns emptyString

        val commandParameters = SignUpStartCommandParameters.builder()
            .platformComponents(mock<PlatformComponents>())
            .username(username)
            .clientId(emptyString)
            .correlationId(correlationId)
            .build()

        nativeAuthRequestProvider.createSignUpStartRequest(
            commandParameters = commandParameters
        )
    }

    @Test(expected = ClientException::class)
    fun testSignUpStartWithEmptyChallengeTypesShouldThrowException() {
        every { mockConfig.challengeType } returns emptyString

        val commandParameters = SignUpStartCommandParameters.builder()
            .platformComponents(mock<PlatformComponents>())
            .username(username)
            .clientId(clientId)
            .correlationId(correlationId)
            .build()

        nativeAuthRequestProvider.createSignUpStartRequest(
            commandParameters = commandParameters
        )
    }

    @Test
    fun testSignUpStartWithEmptyCapabilitiesShouldThrowNotException() {
        every { mockConfig.capabilities } returns emptyString

        val commandParameters = SignUpStartCommandParameters.builder()
            .platformComponents(mock<PlatformComponents>())
            .username(username)
            .clientId(clientId)
            .correlationId(correlationId)
            .build()

        nativeAuthRequestProvider.createSignUpStartRequest(
            commandParameters = commandParameters
        )
    }

    @Test
    fun testSignUpStartExpectedCapabilitiesStringAddedToRequestBody() {
        every { mockConfig.capabilities } returns capabilities

        val commandParameters = SignUpStartCommandParameters.builder()
            .platformComponents(mock<PlatformComponents>())
            .username(username)
            .clientId(clientId)
            .correlationId(correlationId)
            .build()

        val result = nativeAuthRequestProvider.createSignUpStartRequest(
            commandParameters = commandParameters
        )

        assertEquals(capabilities, result.parameters.capabilities)
    }

    @Test
    fun testSignUpStartWithUnsetCorrelationIdShouldNotHaveHeader() {
        val commandParameters = SignUpStartCommandParameters.builder()
            .platformComponents(mock<PlatformComponents>())
            .username(username)
            .clientId(clientId)
            .correlationId("UNSET")
            .build()

        val result = nativeAuthRequestProvider.createSignUpStartRequest(
            commandParameters = commandParameters
        )

        assertNull(result.headers[AuthenticationConstants.AAD.CLIENT_REQUEST_ID])
    }

    @Test
    fun testSignUpStartSuccess() {
        val commandParameters = SignUpStartCommandParameters.builder()
            .platformComponents(mock<PlatformComponents>())
            .username(username)
            .clientId(clientId)
            .userAttributes(userAttributes)
            .correlationId(correlationId)
            .build()

        val result = nativeAuthRequestProvider.createSignUpStartRequest(
            commandParameters = commandParameters
        )

        assertEquals(username, result.parameters.username)
        assertEquals(clientId, result.parameters.clientId)
        assertEquals(challengeType, result.parameters.challengeType)
        assertEquals(ApiConstants.MockApi.signUpStartRequestUrl, result.requestUrl)
        assertEquals(userAttributes.toJsonString(userAttributes), result.parameters.attributes)
        assertEquals(result.headers[AuthenticationConstants.AAD.CLIENT_REQUEST_ID], correlationId)
    }

    @Test
    fun testSignUpStartWithPasswordSuccess() {
        val commandParameters = SignUpStartCommandParameters.builder()
            .platformComponents(mock<PlatformComponents>())
            .username(username)
            .password(password)
            .clientId(clientId)
            .userAttributes(userAttributes)
            .correlationId(correlationId)
            .build()

        val result = nativeAuthRequestProvider.createSignUpStartRequest(
            commandParameters = commandParameters
        )

        assertEquals(username, result.parameters.username)
        assertEquals(clientId, result.parameters.clientId)
        assertEquals(challengeType, result.parameters.challengeType)
        assertEquals(ApiConstants.MockApi.signUpStartRequestUrl, result.requestUrl)
        assertEquals(userAttributes.toJsonString(userAttributes), result.parameters.attributes)
        assertEquals(result.headers[AuthenticationConstants.AAD.CLIENT_REQUEST_ID], correlationId)
    }

    @Test
    fun testSignUpSubmitCodeWithUnsetCorrelationIdShouldNotHaveHeader() {
        val commandParameters = SignUpSubmitCodeCommandParameters.builder()
            .platformComponents(mock<PlatformComponents>())
            .continuationToken(continuationToken)
            .code(oobCode)
            .clientId(clientId)
            .correlationId("UNSET")
            .build()

        val result = nativeAuthRequestProvider.createSignUpSubmitCodeRequest(
            commandParameters = commandParameters
        )

        assertNull(result.headers[AuthenticationConstants.AAD.CLIENT_REQUEST_ID])
    }

    @Test
    fun testSignUpSubmitCodeSuccess() {
        val commandParameters = SignUpSubmitCodeCommandParameters.builder()
            .platformComponents(mock<PlatformComponents>())
            .continuationToken(continuationToken)
            .code(oobCode)
            .clientId(clientId)
            .correlationId(correlationId)
            .build()

        val result = nativeAuthRequestProvider.createSignUpSubmitCodeRequest(
            commandParameters = commandParameters
        )

        assertEquals(oobCode, result.parameters.oob)
        assertEquals(continuationToken, result.parameters.continuationToken)
        assertEquals(oobGrantType, result.parameters.grantType)
        assertEquals(ApiConstants.MockApi.signUpContinueRequestUrl, result.requestUrl)
        assertEquals(result.headers[AuthenticationConstants.AAD.CLIENT_REQUEST_ID], correlationId)
    }

    @Test
    fun testSignUpSubmitPasswordWithUnsetCorrelationIdShouldNotHaveHeader() {
        val commandParameters = SignUpSubmitPasswordCommandParameters.builder()
            .platformComponents(mock<PlatformComponents>())
            .continuationToken(continuationToken)
            .password(password)
            .clientId(clientId)
            .correlationId("UNSET")
            .build()

        val result = nativeAuthRequestProvider.createSignUpSubmitPasswordRequest(
            commandParameters = commandParameters
        )

        assertNull(result.headers[AuthenticationConstants.AAD.CLIENT_REQUEST_ID])
    }

    @Test
    fun testSignUpSubmitPasswordSuccess() {
        val commandParameters = SignUpSubmitPasswordCommandParameters.builder()
            .platformComponents(mock<PlatformComponents>())
            .continuationToken(continuationToken)
            .password(password)
            .clientId(clientId)
            .correlationId(correlationId)
            .build()

        val result = nativeAuthRequestProvider.createSignUpSubmitPasswordRequest(
            commandParameters = commandParameters
        )

        assertEquals(password.toString(), result.parameters.password.toString())
        assertEquals(continuationToken, result.parameters.continuationToken)
        assertEquals(passwordGrantType, result.parameters.grantType)
        assertEquals(ApiConstants.MockApi.signUpContinueRequestUrl, result.requestUrl)
        assertEquals(result.headers[AuthenticationConstants.AAD.CLIENT_REQUEST_ID], correlationId)
    }

    @Test
    fun testSignUpSubmitUserAttributesWithUnsetCorrelationIdShouldNotHaveHeader() {
        val commandParameters = SignUpSubmitUserAttributesCommandParameters.builder()
            .platformComponents(mock<PlatformComponents>())
            .continuationToken(continuationToken)
            .userAttributes(userAttributes)
            .clientId(clientId)
            .correlationId("UNSET")
            .build()

        val result = nativeAuthRequestProvider.createSignUpSubmitUserAttributesRequest(
            commandParameters = commandParameters
        )

        assertNull(result.headers[AuthenticationConstants.AAD.CLIENT_REQUEST_ID])
    }

    @Test
    fun testSignUpSubmitUserAttributesSuccess() {
        val commandParameters = SignUpSubmitUserAttributesCommandParameters.builder()
            .platformComponents(mock<PlatformComponents>())
            .continuationToken(continuationToken)
            .userAttributes(userAttributes)
            .clientId(clientId)
            .correlationId(correlationId)
            .build()

        val result = nativeAuthRequestProvider.createSignUpSubmitUserAttributesRequest(
            commandParameters = commandParameters
        )

        assertEquals(userAttributes.toJsonString(userAttributes), result.parameters.attributes)
        assertEquals(continuationToken, result.parameters.continuationToken)
        assertEquals(ApiConstants.MockApi.signUpContinueRequestUrl, result.requestUrl)
        assertEquals(result.headers[AuthenticationConstants.AAD.CLIENT_REQUEST_ID], correlationId)
    }

    @Test(expected = ClientException::class)
    fun testSignUpSubmitEmptyUserAttributesShouldThrowException() {
        val commandParameters = SignUpSubmitUserAttributesCommandParameters.builder()
            .platformComponents(mock<PlatformComponents>())
            .continuationToken(continuationToken)
            .userAttributes(emptyUserAttributes)
            .clientId(clientId)
            .correlationId(correlationId)
            .build()

        nativeAuthRequestProvider.createSignUpSubmitUserAttributesRequest(
            commandParameters = commandParameters
        )
    }

    @Test(expected = ClientException::class)
    fun testSignUpSubmitEmptyPasswordShouldThrowException() {
        val commandParameters = SignUpSubmitPasswordCommandParameters.builder()
            .platformComponents(mock<PlatformComponents>())
            .continuationToken(continuationToken)
            .password(emptyPassword)
            .clientId(clientId)
            .correlationId(correlationId)
            .build()

        nativeAuthRequestProvider.createSignUpSubmitPasswordRequest(
            commandParameters = commandParameters
        )
    }

    @Test(expected = ClientException::class)
    fun testSignUpSubmitEmptyCodedShouldThrowException() {
        val commandParameters = SignUpSubmitCodeCommandParameters.builder()
            .platformComponents(mock<PlatformComponents>())
            .continuationToken(continuationToken)
            .code(emptyString)
            .clientId(clientId)
            .correlationId(correlationId)
            .build()

        nativeAuthRequestProvider.createSignUpSubmitCodeRequest(
            commandParameters = commandParameters
        )
    }

    // signup challenge tests
    @Test
    fun testSignUpChallengeSuccess() {
        val result = nativeAuthRequestProvider.createSignUpChallengeRequest(
            continuationToken = continuationToken,
            correlationId = correlationId
        )

        assertEquals(challengeType, result.parameters.challengeType)
        assertEquals(continuationToken, result.parameters.continuationToken)
        assertEquals(ApiConstants.MockApi.signUpChallengeRequestUrl, result.requestUrl)
        assertEquals(result.headers[AuthenticationConstants.AAD.CLIENT_REQUEST_ID], correlationId)
    }

    @Test
    fun testSignUpChallengeWithUnsetCorrelationIdShouldNotHaveHeader() {
        val result = nativeAuthRequestProvider.createSignUpChallengeRequest(
            continuationToken = continuationToken,
            correlationId = "UNSET"
        )

        assertNull(result.headers[AuthenticationConstants.AAD.CLIENT_REQUEST_ID])
    }

    @Test(expected = ClientException::class)
    fun testSignUpChallengeWithEmptyContinuationTokenShouldThrowException() {
        nativeAuthRequestProvider.createSignUpChallengeRequest(
            continuationToken = emptyString,
            correlationId = correlationId
        )
    }

    @Test(expected = ClientException::class)
    fun testSignUpChallengeWithEmptyClientIdShouldThrowException() {
        every { mockConfig.clientId } returns emptyString

        nativeAuthRequestProvider.createSignUpChallengeRequest(
            continuationToken = continuationToken,
            correlationId = correlationId
        )
    }

    @Test(expected = ClientException::class)
    fun testSignUpChallengeWithEmptyChallengeTypesShouldThrowException() {
        every { mockConfig.challengeType } returns emptyString

        nativeAuthRequestProvider.createSignUpChallengeRequest(
            continuationToken = continuationToken,
            correlationId = correlationId
        )
    }

    // signin tests
    @Test
    fun testSignInInitiateWithEmptyUsernameShouldNotThrowException() {
        val commandParameters = SignInStartCommandParameters.builder()
            .platformComponents(mock<PlatformComponents>())
            .username(emptyString)
            .correlationId(correlationId)
            .build()

        nativeAuthRequestProvider.createSignInInitiateRequest(
            commandParameters = commandParameters
        )
    }

    @Test
    fun testSignInInitiateWithUnsetCorrelationIdShouldNotHaveHeader() {
        val commandParameters = SignInStartCommandParameters.builder()
            .platformComponents(mock<PlatformComponents>())
            .username(emptyString)
            .correlationId("UNSET")
            .build()

        val result = nativeAuthRequestProvider.createSignInInitiateRequest(
            commandParameters = commandParameters
        )

        assertNull(result.headers[AuthenticationConstants.AAD.CLIENT_REQUEST_ID])
    }

    @Test(expected = ClientException::class)
    fun testSignInInitiateWithEmptyClientIdShouldThrowException() {
        every { mockConfig.clientId } returns emptyString

        val commandParameters = SignInStartCommandParameters.builder()
            .platformComponents(mock<PlatformComponents>())
            .username(username)
            .correlationId(correlationId)
            .build()

        nativeAuthRequestProvider.createSignInInitiateRequest(
            commandParameters = commandParameters
        )
    }

    @Test(expected = ClientException::class)
    fun testSignInInitiateWithEmptyChallengeTypesShouldThrowException() {
        every { mockConfig.challengeType } returns emptyString

        val commandParameters = SignInStartCommandParameters.builder()
            .platformComponents(mock<PlatformComponents>())
            .username(username)
            .correlationId(correlationId)
            .build()

        nativeAuthRequestProvider.createSignInInitiateRequest(
            commandParameters = commandParameters
        )
    }

    @Test
    fun testSignInInitiateWithEmptyCapabilitiesShouldThrowNotException() {
        every { mockConfig.capabilities } returns emptyString

        val commandParameters = SignInStartCommandParameters.builder()
            .platformComponents(mock<PlatformComponents>())
            .username(username)
            .correlationId(correlationId)
            .build()

        nativeAuthRequestProvider.createSignInInitiateRequest(
            commandParameters = commandParameters
        )
    }

    @Test
    fun testSignInInitiateExpectedCapabilitiesStringAddedToRequestBody() {
        every { mockConfig.capabilities } returns capabilities

        val commandParameters = SignInStartCommandParameters.builder()
            .platformComponents(mock<PlatformComponents>())
            .username(username)
            .correlationId(correlationId)
            .build()

        val result = nativeAuthRequestProvider.createSignInInitiateRequest(
            commandParameters = commandParameters
        )

        assertEquals(capabilities, result.parameters.capabilities)
    }

    @Test
    fun testSignInInitiateSuccess() {
        val commandParameters = SignInStartCommandParameters.builder()
            .platformComponents(mock<PlatformComponents>())
            .username(username)
            .correlationId(correlationId)
            .build()

        val result = nativeAuthRequestProvider.createSignInInitiateRequest(
            commandParameters = commandParameters
        )

        assertEquals(username, result.parameters.username)
        assertEquals(clientId, result.parameters.clientId)
        assertEquals(challengeType, result.parameters.challengeType)
        assertEquals(ApiConstants.MockApi.signInInitiateRequestUrl, result.requestUrl)
        assertEquals(result.headers[AuthenticationConstants.AAD.CLIENT_REQUEST_ID], correlationId)
    }

    @Test(expected = ClientException::class)
    fun testSignInInitiateWithPasswordCommandParametersWithEmptyClientIdShouldThrowException() {
        every { mockConfig.clientId } returns emptyString

        val commandParameters = SignInStartCommandParameters.builder()
            .platformComponents(mock<PlatformComponents>())
            .username(username)
            .password(password)
            .correlationId(correlationId)
            .build()

        nativeAuthRequestProvider.createSignInInitiateRequest(
            commandParameters = commandParameters
        )
    }

    @Test(expected = ClientException::class)
    fun testSignInInitiateWithPasswordCommandParametersWithEmptyChallengeTypeShouldThrowException() {
        every { mockConfig.challengeType } returns emptyString

        val commandParameters = SignInStartCommandParameters.builder()
            .platformComponents(mock<PlatformComponents>())
            .username(username)
            .password(password)
            .correlationId(correlationId)
            .build()

        nativeAuthRequestProvider.createSignInInitiateRequest(
            commandParameters = commandParameters
        )
    }

    @Test
    fun testSignInInitiateWithPasswordCommandParametersWithEmptyUsernameShouldNotThrowException() {
        val commandParameters = SignInStartCommandParameters.builder()
            .platformComponents(mock<PlatformComponents>())
            .username(emptyString)
            .password(password)
            .correlationId(correlationId)
            .build()

        nativeAuthRequestProvider.createSignInInitiateRequest(
            commandParameters = commandParameters
        )
    }

    // The password is not sent to /initiate (but to /token), so no password check (and fail) has
    // to be done here.
    @Test
    fun testSignInInitiateWithPasswordCommandParametersWithEmptyPasswordShouldNotThrowException() {
        val commandParameters = SignInStartCommandParameters.builder()
            .platformComponents(mock<PlatformComponents>())
            .username(username)
            .password(emptyPassword)
            .correlationId(correlationId)
            .build()

        nativeAuthRequestProvider.createSignInInitiateRequest(
            commandParameters = commandParameters
        )
    }

    @Test(expected = ClientException::class)
    fun testSignInIntrospectWithEmptyClientIdShouldThrowException() {
        every { mockConfig.clientId } returns emptyString

        nativeAuthRequestProvider.createIntrospectRequest(
            continuationToken = continuationToken,
            correlationId = correlationId
        )
    }

    @Test
    fun testSignInIntrospectSuccess() {
        val result = nativeAuthRequestProvider.createIntrospectRequest(
            continuationToken = continuationToken,
            correlationId = correlationId
        )

        assertEquals(clientId, result.parameters.clientId)
        assertEquals(continuationToken, result.parameters.continuationToken)
        assertEquals(ApiConstants.MockApi.signInIntrospectRequestUrl, result.requestUrl)
        assertEquals(result.headers[AuthenticationConstants.AAD.CLIENT_REQUEST_ID], correlationId)
    }

    @Test(expected = ClientException::class)
    fun testSignInIntrospectWithEmptyContinuationTokenShouldThrowException() {
        nativeAuthRequestProvider.createIntrospectRequest(
            continuationToken = emptyString,
            correlationId = correlationId
        )
    }

    @Test(expected = ClientException::class)
    fun testSignInDefaultChallengeWithEmptyClientIdShouldThrowException() {
        every { mockConfig.clientId } returns emptyString

        nativeAuthRequestProvider.createSignInDefaultChallengeRequest(
            continuationToken = continuationToken,
            correlationId = correlationId
        )
    }

    @Test(expected = ClientException::class)
    fun testSignInDefaultChallengeWithEmptyChallengeTypeShouldThrowException() {
        every { mockConfig.challengeType } returns emptyString

        nativeAuthRequestProvider.createSignInDefaultChallengeRequest(
            continuationToken = continuationToken,
            correlationId = correlationId
        )
    }

    @Test(expected = ClientException::class)
    fun testSignInDefaultChallengeWithEmptyContinuationTokenShouldThrowException() {
        nativeAuthRequestProvider.createSignInDefaultChallengeRequest(
            continuationToken = emptyString,
            correlationId = correlationId
        )
    }

    @Test
    fun testSignInDefaultChallengeWithUnsetCorrelationIdShouldNotHaveHeader() {
        val result = nativeAuthRequestProvider.createSignInDefaultChallengeRequest(
            continuationToken = continuationToken,
            correlationId = "UNSET"
        )

        assertNull(result.headers[AuthenticationConstants.AAD.CLIENT_REQUEST_ID])
    }

    @Test
    fun testSignInDefaultChallengeSuccess() {
        val result = nativeAuthRequestProvider.createSignInDefaultChallengeRequest(
            continuationToken = continuationToken,
            correlationId = correlationId
        )

        assertEquals(clientId, result.parameters.clientId)
        assertEquals(continuationToken, result.parameters.continuationToken)
        assertEquals(ApiConstants.MockApi.signInChallengeRequestUrl, result.requestUrl)
        assertEquals(result.headers[AuthenticationConstants.AAD.CLIENT_REQUEST_ID], correlationId)
    }

    @Test(expected = ClientException::class)
    fun testSignInSelectedChallengeWithEmptyClientIdShouldThrowException() {
        every { mockConfig.clientId } returns emptyString

        nativeAuthRequestProvider.createSignInSelectedChallengeRequest(
            continuationToken = continuationToken,
            challengeId = challengeId,
            correlationId = correlationId
        )
    }

    @Test(expected = ClientException::class)
    fun testSignInSelectedChallengeWithEmptyChallengeIdShouldThrowException() {
        nativeAuthRequestProvider.createSignInSelectedChallengeRequest(
            continuationToken = continuationToken,
            challengeId = emptyString,
            correlationId = correlationId
        )
    }

    @Test(expected = ClientException::class)
    fun testSignInSelectedChallengeWithEmptyContinuationTokenShouldThrowException() {
        nativeAuthRequestProvider.createSignInSelectedChallengeRequest(
            continuationToken = emptyString,
            correlationId = correlationId,
            challengeId = challengeId
        )
    }

    @Test
    fun testSignInSelectedChallengeWithUnsetCorrelationIdShouldNotHaveHeader() {
        val result = nativeAuthRequestProvider.createSignInSelectedChallengeRequest(
            continuationToken = continuationToken,
            correlationId = "UNSET",
            challengeId = challengeId
        )

        assertNull(result.headers[AuthenticationConstants.AAD.CLIENT_REQUEST_ID])
    }

    @Test
    fun testSignInSelectedChallengeSuccess() {
        val result = nativeAuthRequestProvider.createSignInSelectedChallengeRequest(
            continuationToken = continuationToken,
            correlationId = correlationId,
            challengeId = challengeId
        )

        assertEquals(clientId, result.parameters.clientId)
        assertEquals(continuationToken, result.parameters.continuationToken)
        assertEquals(ApiConstants.MockApi.signInChallengeRequestUrl, result.requestUrl)
        assertEquals(result.headers[AuthenticationConstants.AAD.CLIENT_REQUEST_ID], correlationId)
    }

    @Test
    fun testSignInTokenWithContinuationTokenUnsetCorrelationIdShouldNotHaveHeader() {
        val commandParameters = SignInWithContinuationTokenCommandParameters.builder()
            .platformComponents(mock<PlatformComponents>())
            .continuationToken(continuationToken)
            .username(username)
            .correlationId("UNSET")
            .build()

        val result = nativeAuthRequestProvider.createContinuationTokenTokenRequest(
            commandParameters = commandParameters
        )

        assertNull(result.headers[AuthenticationConstants.AAD.CLIENT_REQUEST_ID])
    }

    @Test
    fun testSignInTokenWithContinuationTokenSuccess() {
        val commandParameters = SignInWithContinuationTokenCommandParameters.builder()
            .platformComponents(mock<PlatformComponents>())
            .continuationToken(continuationToken)
            .username(username)
            .correlationId(correlationId)
            .build()

        val result = nativeAuthRequestProvider.createContinuationTokenTokenRequest(
            commandParameters = commandParameters
        )

        assertEquals(username, result.parameters.username)
        assertEquals(clientId, result.parameters.clientId)
        assertEquals(challengeType, result.parameters.challengeType)
        assertEquals(ApiConstants.MockApi.signInTokenRequestUrl, result.requestUrl)
        assertEquals(result.headers[AuthenticationConstants.AAD.CLIENT_REQUEST_ID], correlationId)
    }

    @Test(expected = ClientException::class)
    fun testSignInTokenWithEmptyContinuationTokenShouldThrowException() {
        val commandParameters = SignInWithContinuationTokenCommandParameters.builder()
            .platformComponents(mock<PlatformComponents>())
            .continuationToken(emptyString)
            .username(username)
            .correlationId(correlationId)
            .build()

        nativeAuthRequestProvider.createContinuationTokenTokenRequest(
            commandParameters = commandParameters
        )
    }

    fun testSignInTokenWithEmptyUsernameShouldNotThrowException() {
        val commandParameters = SignInWithContinuationTokenCommandParameters.builder()
            .platformComponents(mock<PlatformComponents>())
            .continuationToken(continuationToken)
            .username(emptyString)
            .correlationId(correlationId)
            .build()

        val result = nativeAuthRequestProvider.createContinuationTokenTokenRequest(
            commandParameters = commandParameters
        )
        assertEquals(username, result.parameters.username)
        assertEquals(result.headers[AuthenticationConstants.AAD.CLIENT_REQUEST_ID], correlationId)
    }

    @Test(expected = ClientException::class)
    fun testSignInTokenWithEmptyOOBShouldThrowException() {
        val commandParameters = SignInSubmitCodeCommandParameters.builder()
            .platformComponents(mock<PlatformComponents>())
            .code(emptyString)
            .continuationToken(continuationToken)
            .correlationId(correlationId)
            .build()

        nativeAuthRequestProvider.createOOBTokenRequest(
            commandParameters = commandParameters
        )
    }

    @Test
    fun testSignInTokenOOBShouldContainsCorrectParams() {
        val commandParameters = SignInSubmitCodeCommandParameters.builder()
            .platformComponents(mock<PlatformComponents>())
            .continuationToken(continuationToken)
            .correlationId(correlationId)
            .code("code")
            .claimsRequestJson("claims")
            .isMFAGrantType(false)
            .build()

        val request = nativeAuthRequestProvider.createOOBTokenRequest(
            commandParameters = commandParameters
        )
        assertEquals(request.parameters.oob, commandParameters.code)
        assertEquals(request.parameters.grantType, NativeAuthConstants.GrantType.OOB)
        assertEquals(request.parameters.claimsRequestJson, commandParameters.claimsRequestJson)
        assertEquals(request.parameters.continuationToken, commandParameters.continuationToken)
        assertEquals(request.parameters.challengeType, mockConfig.challengeType)
        assertNull(request.parameters.scope)
    }

    @Test
    fun testSignInTokenPasswordShouldContainsCorrectParams() {
        val commandParameters = SignInSubmitPasswordCommandParameters.builder()
            .platformComponents(mock<PlatformComponents>())
            .continuationToken(continuationToken)
            .correlationId(correlationId)
            .password("pwd".toCharArray())
            .claimsRequestJson("claims")
            .build()

        val request = nativeAuthRequestProvider.createPasswordTokenRequest(
            commandParameters
        )
        assertEquals(request.parameters.password, commandParameters.password)
        assertEquals(request.parameters.claimsRequestJson, commandParameters.claimsRequestJson)
        assertEquals(request.parameters.continuationToken, commandParameters.continuationToken)
        assertEquals(request.parameters.challengeType, mockConfig.challengeType)
        assertNull(request.parameters.scope)
    }

    @Test
    fun testSignInTokenContinuationShouldContainsCorrectParams() {
        val scopes = arrayListOf("OOB", "PASSWORD")
        val headers = mapOf("key" to "value")
        val claims = "claims"
        val request = SignInTokenRequest.createContinuationTokenRequest(
            continuationToken,
            clientId,
            username,
            scopes,
            challengeType,
            ApiConstants.MockApi.signInTokenRequestUrl.toString(),
            headers,
            claimsRequestJson = claims
        )
        assertNull(request.parameters.password)
        assertEquals(request.parameters.claimsRequestJson, claims)
        assertEquals(request.parameters.scope, "OOB PASSWORD")
        assertEquals(request.parameters.continuationToken, continuationToken)
        assertEquals(request.parameters.challengeType, challengeType)
        assertEquals(request.parameters.username, username)
        assertEquals(request.parameters.clientId, clientId)
        assertEquals(request.headers, headers)
    }

    @Test(expected = ClientException::class)
    fun testPasswordTokenRequestWithEmptyPasswordShouldThrowException() {
        val commandParameters = SignInSubmitPasswordCommandParameters.builder()
            .platformComponents(mock<PlatformComponents>())
            .password(emptyPassword)
            .continuationToken(continuationToken)
            .correlationId(correlationId)
            .build()

        nativeAuthRequestProvider.createPasswordTokenRequest(
            commandParameters = commandParameters
        )
    }

    @Test(expected = ClientException::class)
    fun testPasswordTokenRequestWithEmptyContinuationTokenShouldThrowException() {
        val commandParameters = SignInSubmitPasswordCommandParameters.builder()
            .platformComponents(mock<PlatformComponents>())
            .password(password)
            .continuationToken(emptyString)
            .correlationId(correlationId)
            .build()

        nativeAuthRequestProvider.createPasswordTokenRequest(
            commandParameters = commandParameters
        )
    }

    @Test
    fun testPasswordTokenRequestWithUnsetCorrelationIdShouldNotHaveHeader() {
        val commandParameters = SignInSubmitPasswordCommandParameters.builder()
            .platformComponents(mock<PlatformComponents>())
            .password(password)
            .continuationToken(continuationToken)
            .correlationId("UNSET")
            .build()

        val result = nativeAuthRequestProvider.createPasswordTokenRequest(
            commandParameters = commandParameters
        )

        assertNull(result.headers[AuthenticationConstants.AAD.CLIENT_REQUEST_ID])
    }
    @Test
    fun testPasswordTokenRequestSuccess() {
        val commandParameters = SignInSubmitPasswordCommandParameters.builder()
            .platformComponents(mock<PlatformComponents>())
            .password(password)
            .continuationToken(continuationToken)
            .correlationId(correlationId)
            .build()

        val result = nativeAuthRequestProvider.createPasswordTokenRequest(
            commandParameters = commandParameters
        )

        assertEquals(clientId, result.parameters.clientId)
        assertEquals(challengeType, result.parameters.challengeType)
        assertEquals(ApiConstants.MockApi.signInTokenRequestUrl, result.requestUrl)
        assertEquals(result.headers[AuthenticationConstants.AAD.CLIENT_REQUEST_ID], correlationId)
    }

    @Test(expected = ClientException::class)
    fun testSignUpContinueWithEmptyContinuationTokenThrowException() {
        val commandParameters = SignUpSubmitCodeCommandParameters.builder()
            .platformComponents(mock<PlatformComponents>())
            .continuationToken(emptyString)
            .correlationId(correlationId)
            .code(oobCode)
            .build()

        nativeAuthRequestProvider.createSignUpSubmitCodeRequest(
            commandParameters = commandParameters
        )
    }

    @Test(expected = ClientException::class)
    fun testSignUpContinueWithEmptyClientIdShouldThrowException() {
        every { mockConfig.clientId } returns emptyString

        val commandParameters = SignUpSubmitCodeCommandParameters.builder()
            .platformComponents(mock<PlatformComponents>())
            .continuationToken(continuationToken)
            .code(oobCode)
            .correlationId(correlationId)
            .build()

        nativeAuthRequestProvider.createSignUpSubmitCodeRequest(
            commandParameters = commandParameters
        )
    }

    // sspr start tests
    @Test
    fun testResetPasswordStartWithEmptyUsernameShouldNotThrowException() {
        val commandParameters = ResetPasswordStartCommandParameters.builder()
            .platformComponents(mock<PlatformComponents>())
            .username(emptyString)
            .correlationId(correlationId)
            .build()

        nativeAuthRequestProvider.createResetPasswordStartRequest(
            commandParameters = commandParameters
        )
    }

    @Test(expected = ClientException::class)
    fun testResetPasswordStartWithEmptyClientIdShouldThrowException() {
        every { mockConfig.clientId } returns emptyString

        val commandParameters = ResetPasswordStartCommandParameters.builder()
            .platformComponents(mock<PlatformComponents>())
            .username(username)
            .correlationId(correlationId)
            .build()

        nativeAuthRequestProvider.createResetPasswordStartRequest(
            commandParameters = commandParameters
        )
    }

    @Test(expected = ClientException::class)
    fun testResetPasswordStartWithEmptyChallengeTypeShouldThrowException() {
        every { mockConfig.challengeType } returns emptyString

        val commandParameters = ResetPasswordStartCommandParameters.builder()
            .platformComponents(mock<PlatformComponents>())
            .username(username)
            .correlationId(correlationId)
            .build()

        nativeAuthRequestProvider.createResetPasswordStartRequest(
            commandParameters = commandParameters
        )
    }

    @Test
    fun testResetPasswordStartWithEmptyCapabilitiesShouldNotThrowException() {
        every { mockConfig.capabilities } returns emptyString

        val commandParameters = ResetPasswordStartCommandParameters.builder()
            .platformComponents(mock<PlatformComponents>())
            .username(username)
            .correlationId(correlationId)
            .build()

        nativeAuthRequestProvider.createResetPasswordStartRequest(
            commandParameters = commandParameters
        )
    }

    @Test
    fun testResetPasswordStartExpectedCapabilitiesStringAddedToRequestBody() {
        every { mockConfig.capabilities } returns capabilities

        val commandParameters = ResetPasswordStartCommandParameters.builder()
            .platformComponents(mock<PlatformComponents>())
            .username(username)
            .correlationId(correlationId)
            .build()

        val result = nativeAuthRequestProvider.createResetPasswordStartRequest(
            commandParameters = commandParameters
        )

        assertEquals(capabilities, result.parameters.capabilities)
    }

    @Test
    fun testResetPasswordStartWithUnsetCorrelationIdShouldNotHaveHeader() {
        val commandParameters = ResetPasswordStartCommandParameters.builder()
            .platformComponents(mock<PlatformComponents>())
            .username(username)
            .correlationId("UNSET")
            .build()

        val result = nativeAuthRequestProvider.createResetPasswordStartRequest(
            commandParameters = commandParameters
        )

        assertNull(result.headers[AuthenticationConstants.AAD.CLIENT_REQUEST_ID])
    }

    @Test
    fun testResetPasswordStartSuccess() {
        val commandParameters = ResetPasswordStartCommandParameters.builder()
            .platformComponents(mock<PlatformComponents>())
            .username(username)
            .correlationId(correlationId)
            .build()

        val result = nativeAuthRequestProvider.createResetPasswordStartRequest(
            commandParameters = commandParameters
        )

        assertEquals(username, result.parameters.username)
        assertEquals(clientId, result.parameters.clientId)
        assertEquals(challengeType, result.parameters.challengeType)
        assertEquals(ApiConstants.MockApi.ssprStartRequestUrl, result.requestUrl)
        assertEquals(result.headers[AuthenticationConstants.AAD.CLIENT_REQUEST_ID], correlationId)
    }

    // ResetPassword challenge tests
    @Test(expected = ClientException::class)
    fun testResetPasswordChallengeWithEmptyClientIdShouldThrowException() {
        every { mockConfig.clientId } returns emptyString

        nativeAuthRequestProvider.createResetPasswordChallengeRequest(
            continuationToken = continuationToken,
            correlationId = correlationId
        )
    }

    @Test(expected = ClientException::class)
    fun testResetPasswordChallengeWithEmptyContinuationTokenShouldThrowException() {
        nativeAuthRequestProvider.createResetPasswordChallengeRequest(
            continuationToken = emptyString,
            correlationId = correlationId
        )
    }

    @Test
    fun testResetPasswordChallengeWithUnsetCorrelationIdShouldNotHaveHeader() {
        val result = nativeAuthRequestProvider.createResetPasswordChallengeRequest(
            continuationToken = continuationToken,
            correlationId = "UNSET"
        )

        assertNull(result.headers[AuthenticationConstants.AAD.CLIENT_REQUEST_ID])
    }

    @Test
    fun testResetPasswordChallengeSuccess() {
        val result = nativeAuthRequestProvider.createResetPasswordChallengeRequest(
            continuationToken = continuationToken,
            correlationId = correlationId
        )

        assertEquals(clientId, result.parameters.clientId)
        assertEquals(ApiConstants.MockApi.ssprChallengeRequestUrl, result.requestUrl)
        assertEquals(result.headers[AuthenticationConstants.AAD.CLIENT_REQUEST_ID], correlationId)
    }

    // ResetPassword continue tests
    @Test(expected = ClientException::class)
    fun testResetPasswordContinueWithEmptyClientIdShouldThrowException() {
        every { mockConfig.clientId } returns emptyString

        val commandParameters = ResetPasswordSubmitCodeCommandParameters.builder()
            .platformComponents(mock<PlatformComponents>())
            .code(oobCode)
            .continuationToken(continuationToken)
            .correlationId(correlationId)
            .build()

        nativeAuthRequestProvider.createResetPasswordContinueRequest(
            commandParameters = commandParameters
        )
    }

    @Test(expected = ClientException::class)
    fun testResetPasswordContinueWithEmptycontinuationTokenShouldThrowException() {
        val commandParameters = ResetPasswordSubmitCodeCommandParameters.builder()
            .platformComponents(mock<PlatformComponents>())
            .code(oobCode)
            .continuationToken(emptyString)
            .correlationId(correlationId)
            .build()

        nativeAuthRequestProvider.createResetPasswordContinueRequest(
            commandParameters = commandParameters
        )
    }

    @Test(expected = ClientException::class)
    fun testResetPasswordContinueWithEmptyOobCodeShouldThrowException() {
        val commandParameters = ResetPasswordSubmitCodeCommandParameters.builder()
            .platformComponents(mock<PlatformComponents>())
            .code(emptyString)
            .continuationToken(continuationToken)
            .correlationId(correlationId)
            .build()

        nativeAuthRequestProvider.createResetPasswordContinueRequest(
            commandParameters = commandParameters
        )
    }

    @Test
    fun testResetPasswordContinueWithUnsetCorrelationIdShouldNotHaveHeader() {
        val commandParameters = ResetPasswordSubmitCodeCommandParameters.builder()
            .platformComponents(mock<PlatformComponents>())
            .code(oobCode)
            .continuationToken(continuationToken)
            .correlationId("UNSET")
            .build()

        val result = nativeAuthRequestProvider.createResetPasswordContinueRequest(
            commandParameters = commandParameters
        )

        assertNull(result.headers[AuthenticationConstants.AAD.CLIENT_REQUEST_ID])
    }

    @Test
    fun testResetPasswordContinueSuccess() {
        val commandParameters = ResetPasswordSubmitCodeCommandParameters.builder()
            .platformComponents(mock<PlatformComponents>())
            .code(oobCode)
            .continuationToken(continuationToken)
            .correlationId(correlationId)
            .build()

        val result = nativeAuthRequestProvider.createResetPasswordContinueRequest(
            commandParameters = commandParameters
        )

        assertEquals(clientId, result.parameters.clientId)
        assertEquals(ApiConstants.MockApi.ssprContinueRequestUrl, result.requestUrl)
        assertEquals(result.headers[AuthenticationConstants.AAD.CLIENT_REQUEST_ID], correlationId)
    }

    // ResetPassword submit tests
    @Test(expected = ClientException::class)
    fun testResetPasswordSubmitWithEmptyClientIdShouldThrowException() {
        every { mockConfig.clientId } returns emptyString

        val commandParameters = ResetPasswordSubmitNewPasswordCommandParameters.builder()
            .platformComponents(mock<PlatformComponents>())
            .continuationToken(continuationToken)
            .newPassword(password)
            .correlationId(correlationId)
            .build()

        nativeAuthRequestProvider.createResetPasswordSubmitRequest(
            commandParameters = commandParameters
        )
    }

    @Test(expected = ClientException::class)
    fun testResetPasswordSubmitWithEmptyPasswordShouldThrowException() {
        val commandParameters = ResetPasswordSubmitNewPasswordCommandParameters.builder()
            .platformComponents(mock<PlatformComponents>())
            .continuationToken(continuationToken)
            .newPassword(emptyPassword)
            .correlationId(correlationId)
            .build()

        nativeAuthRequestProvider.createResetPasswordSubmitRequest(
            commandParameters = commandParameters
        )
    }

    @Test(expected = ClientException::class)
    fun testResetPasswordSubmitWithEmptyContinuationTokenShouldThrowException() {
        val commandParameters = ResetPasswordSubmitNewPasswordCommandParameters.builder()
            .platformComponents(mock<PlatformComponents>())
            .continuationToken(emptyString)
            .newPassword(password)
            .correlationId(correlationId)
            .build()

        nativeAuthRequestProvider.createResetPasswordSubmitRequest(
            commandParameters = commandParameters
        )
    }

    @Test
    fun testResetPasswordSubmitWithUnsetCorrelationIdShouldNotHaveHeader() {
        val commandParameters = ResetPasswordSubmitNewPasswordCommandParameters.builder()
            .platformComponents(mock<PlatformComponents>())
            .continuationToken(continuationToken)
            .newPassword(password)
            .correlationId("UNSET")
            .build()

        val result = nativeAuthRequestProvider.createResetPasswordSubmitRequest(
            commandParameters = commandParameters
        )

        assertNull(result.headers[AuthenticationConstants.AAD.CLIENT_REQUEST_ID])
    }

    @Test
    fun testResetPasswordSubmitSuccess() {
        val commandParameters = ResetPasswordSubmitNewPasswordCommandParameters.builder()
            .platformComponents(mock<PlatformComponents>())
            .continuationToken(continuationToken)
            .newPassword(password)
            .correlationId(correlationId)
            .build()

        val result = nativeAuthRequestProvider.createResetPasswordSubmitRequest(
            commandParameters = commandParameters
        )

        assertEquals(clientId, result.parameters.clientId)
        assertEquals(ApiConstants.MockApi.ssprSubmitRequestUrl, result.requestUrl)
        assertEquals(result.headers[AuthenticationConstants.AAD.CLIENT_REQUEST_ID], correlationId)
    }

    // ResetPassword completion poll tests
    @Test(expected = ClientException::class)
    fun testResetPasswordPollCompletionWithEmptyClientIdShouldThrowException() {
        every { mockConfig.clientId } returns emptyString

        nativeAuthRequestProvider.createResetPasswordPollCompletionRequest(
            continuationToken = continuationToken,
            correlationId = correlationId
        )
    }

    @Test(expected = ClientException::class)
    fun testResetPasswordPollCompletionWithEmptyContinuationTokenShouldThrowException() {
        nativeAuthRequestProvider.createResetPasswordPollCompletionRequest(
            continuationToken = emptyString,
            correlationId = correlationId
        )
    }

    @Test
    fun testResetPasswordPollCompletionWithUnsetCorrelationIdShouldNotHaveHeader() {
        val result = nativeAuthRequestProvider.createResetPasswordPollCompletionRequest(
            continuationToken = continuationToken,
            correlationId = "UNSET"
        )

        assertNull(result.headers[AuthenticationConstants.AAD.CLIENT_REQUEST_ID])
    }

    @Test
    fun testResetPasswordPollCompletionSuccess() {
        val result = nativeAuthRequestProvider.createResetPasswordPollCompletionRequest(
            continuationToken = continuationToken,
            correlationId = correlationId
        )

        assertEquals(clientId, result.parameters.clientId)
        assertEquals(ApiConstants.MockApi.ssprPollCompletionRequestUrl, result.requestUrl)
        assertEquals(result.headers[AuthenticationConstants.AAD.CLIENT_REQUEST_ID], correlationId)
    }

    // region JIT tests

    @Test(expected = ClientException::class)
    fun testJITIntrospectWithEmptyContinuationTokenShouldThrowException() {
        val continuationToken = ""

        nativeAuthRequestProvider.createJITIntrospectRequest(
            continuationToken,
            correlationId
        )
    }

    @Test(expected = ClientException::class)
    fun testJITIntrospectWithEmptyClientIdShouldThrowException() {
        every { mockConfig.clientId } returns emptyString

        nativeAuthRequestProvider.createJITIntrospectRequest(
            continuationToken,
            correlationId
        )
    }

    @Test
    fun testJITIntrospectWithUnsetCorrelationIdShouldNotHaveHeader() {
        val correlationId = "UNSET"

        val result = nativeAuthRequestProvider.createJITIntrospectRequest(
            continuationToken,
            correlationId
        )

        assertNull(result.headers[AuthenticationConstants.AAD.CLIENT_REQUEST_ID])
    }

    @Test
    fun testJITIntrospectRequestSuccess() {
        val result = nativeAuthRequestProvider.createJITIntrospectRequest(
            continuationToken,
            correlationId
        )

        assertEquals(clientId, result.parameters.clientId)
        assertEquals(continuationToken, result.parameters.continuationToken)
        assertEquals(ApiConstants.MockApi.jitIntrospectRequestUrl, result.requestUrl)
        assertEquals(result.headers[AuthenticationConstants.AAD.CLIENT_REQUEST_ID], correlationId)
    }

    @Test(expected = ClientException::class)
    fun testJITChallengeWithEmptyContinuationTokenShouldThrowException() {
        val continuationToken = ""

        nativeAuthRequestProvider.createJITChallengeRequest(
            continuationToken =  continuationToken,
            challengeType = challengeType,
            challengeTarget = challengeTarget,
            challengeChannel =  challengeChannel,
            correlationId = correlationId
        )
    }

    @Test(expected = ClientException::class)
    fun testJITChallengeWithEmptyChallengeTypeShouldThrowException() {
        val challengeType = ""

        nativeAuthRequestProvider.createJITChallengeRequest(
            continuationToken =  continuationToken,
            challengeType = challengeType,
            challengeTarget = challengeTarget,
            challengeChannel =  challengeChannel,
            correlationId = correlationId
        )
    }

    @Test(expected = ClientException::class)
    fun testJITChallengeWithEmptyChallengeTargetShouldThrowException() {
        val challengeTarget = ""

        nativeAuthRequestProvider.createJITChallengeRequest(
            continuationToken =  continuationToken,
            challengeType = challengeType,
            challengeTarget = challengeTarget,
            challengeChannel =  challengeChannel,
            correlationId = correlationId
        )
    }

    @Test(expected = ClientException::class)
    fun testJITChallengeWithEmptyChallengeChannelShouldThrowException() {
        val challengeChannel = ""

        nativeAuthRequestProvider.createJITChallengeRequest(
            continuationToken =  continuationToken,
            challengeType = challengeType,
            challengeTarget = challengeTarget,
            challengeChannel =  challengeChannel,
            correlationId = correlationId
        )
    }

    @Test
    fun testJITChallengeWithUnsetCorrelationIdShouldNotHaveHeader() {
        val correlationId = "UNSET"

        val result = nativeAuthRequestProvider.createJITChallengeRequest(
            continuationToken =  continuationToken,
            challengeType = challengeType,
            challengeTarget = challengeTarget,
            challengeChannel =  challengeChannel,
            correlationId = correlationId
        )

        assertNull(result.headers[AuthenticationConstants.AAD.CLIENT_REQUEST_ID])
    }

    @Test(expected = ClientException::class)
    fun testJITChallengeWithEmptyClientIdShouldThrowException() {
        every { mockConfig.clientId } returns emptyString

        nativeAuthRequestProvider.createJITChallengeRequest(
            continuationToken =  continuationToken,
            challengeType = challengeType,
            challengeTarget = challengeTarget,
            challengeChannel =  challengeChannel,
            correlationId = correlationId
        )
    }

    @Test
    fun testJITChallengeRequestSuccess() {
        val result = nativeAuthRequestProvider.createJITChallengeRequest(
            continuationToken =  continuationToken,
            challengeType = challengeType,
            challengeTarget = challengeTarget,
            challengeChannel =  challengeChannel,
            correlationId = correlationId
        )

        assertEquals(clientId, result.parameters.clientId)
        assertEquals(continuationToken, result.parameters.continuationToken)
        assertEquals(challengeType, result.parameters.challengeType)
        assertEquals(challengeTarget, result.parameters.challengeTarget)
        assertEquals(challengeChannel, result.parameters.challengeChannel)
        assertEquals(ApiConstants.MockApi.jitChallengeRequestUrl, result.requestUrl)
        assertEquals(result.headers[AuthenticationConstants.AAD.CLIENT_REQUEST_ID], correlationId)
    }

    @Test(expected = ClientException::class)
    fun testJITContinueWithEmptyContinuationTokenShouldThrowException() {
        val continuationToken = ""

        nativeAuthRequestProvider.createJITContinueRequest(
            continuationToken =  continuationToken,
            grantType = grantType,
            code = oobCode,
            correlationId = correlationId
        )
    }

    @Test(expected = ClientException::class)
    fun testJITContinueWithEmptyGrantTypeShouldThrowException() {
        val grantType = ""

        nativeAuthRequestProvider.createJITContinueRequest(
            continuationToken =  continuationToken,
            grantType = grantType,
            code = oobCode,
            correlationId = correlationId
        )
    }

    @Test
    fun testJITContinueWithContinuationTokenGrantType() {
        val grantType = NativeAuthConstants.GrantType.CONTINUATION_TOKEN

        val result = nativeAuthRequestProvider.createJITContinueRequest(
            continuationToken =  continuationToken,
            grantType = grantType,
            code = oobCode,
            correlationId = correlationId
        )
        assertEquals(clientId, result.parameters.clientId)
        assertEquals(continuationToken, result.parameters.continuationToken)
        assertEquals(grantType, result.parameters.grantType)
        assertEquals(oobCode, result.parameters.oob)
    }

    @Test
    fun testJITContinueWithUnsetCorrelationIdShouldNotHaveHeader() {
        val correlationId = "UNSET"

        val result = nativeAuthRequestProvider.createJITContinueRequest(
            continuationToken =  continuationToken,
            grantType = grantType,
            code = oobCode,
            correlationId = correlationId
        )

        assertNull(result.headers[AuthenticationConstants.AAD.CLIENT_REQUEST_ID])
    }

    @Test(expected = ClientException::class)
    fun testJITContinueWithEmptyClientIdShouldThrowException() {
        every { mockConfig.clientId } returns emptyString

        nativeAuthRequestProvider.createJITContinueRequest(
            continuationToken =  continuationToken,
            grantType = grantType,
            code = oobCode,
            correlationId = correlationId
        )
    }

    @Test
    fun testJITContinueRequestSuccess() {
        val result = nativeAuthRequestProvider.createJITContinueRequest(
            continuationToken =  continuationToken,
            grantType = grantType,
            code = oobCode,
            correlationId = correlationId
        )

        assertEquals(clientId, result.parameters.clientId)
        assertEquals(continuationToken, result.parameters.continuationToken)
        assertEquals(grantType, result.parameters.grantType)
        assertEquals(oobCode, result.parameters.oob)
        assertEquals(ApiConstants.MockApi.jitContinueRequestUrl, result.requestUrl)
        assertEquals(result.headers[AuthenticationConstants.AAD.CLIENT_REQUEST_ID], correlationId)
    }

    // Test for null capabilities filtering in serialization

    @Test
    fun testSignInInitiateWithNullCapabilitiesFiltersOutFromSerializedRequestBody() {
        // Set capabilities to null in config
        every { mockConfig.capabilities } returns null

        val commandParameters = SignInStartCommandParameters.builder()
            .platformComponents(mock<PlatformComponents>())
            .username(username)
            .clientId(clientId)
            .correlationId(correlationId)
            .build()

        val result = nativeAuthRequestProvider.createSignInInitiateRequest(
            commandParameters = commandParameters
        )

        // Verify that the request object has null capabilities
        assertNull("Capabilities should be null in the request parameters", result.parameters.capabilities)

        // Test that ObjectMapper.serializeObjectToFormUrlEncoded should filter out null values
        val serializedParams = ObjectMapper.serializeObjectToFormUrlEncoded(result.parameters)
        
        // Verify that capabilities parameter is not included in the serialized form
        Assert.assertFalse(
            "Serialized request body should not contain 'capabilities' parameter when it is null",
            serializedParams.contains("capabilities")
        )
    }

    @Test
    fun testSignInInitiateWithNonNullCapabilitiesIncludedInSerializedRequestBody() {
        // Set capabilities to a valid value in config
        every { mockConfig.capabilities } returns capabilities

        val commandParameters = SignInStartCommandParameters.builder()
            .platformComponents(mock<PlatformComponents>())
            .username(username)
            .clientId(clientId)
            .correlationId(correlationId)
            .build()

        val result = nativeAuthRequestProvider.createSignInInitiateRequest(
            commandParameters = commandParameters
        )

        // Verify that the request object has the expected capabilities
        assertEquals("Capabilities should match the configured value", capabilities, result.parameters.capabilities)

        // Test that ObjectMapper.serializeObjectToFormUrlEncoded includes non-null values
        val serializedParams = ObjectMapper.serializeObjectToFormUrlEncoded(result.parameters)
        
        // Verify that capabilities parameter IS included in the serialized form when not null
        Assert.assertTrue(
            "Serialized request body should contain 'capabilities' parameter when it has a value",
            serializedParams.contains("capabilities=${capabilities.replace(" ", "+")}")
        )
    }

    //endregion

}
