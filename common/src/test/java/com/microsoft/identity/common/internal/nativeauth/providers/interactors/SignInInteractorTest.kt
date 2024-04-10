package com.microsoft.identity.common.internal.nativeauth.providers.interactors

import com.microsoft.identity.common.java.interfaces.PlatformComponents
import com.microsoft.identity.common.java.nativeauth.commands.parameters.SignInStartCommandParameters
import com.microsoft.identity.common.java.nativeauth.providers.NativeAuthRequestProvider
import com.microsoft.identity.common.java.nativeauth.providers.NativeAuthResponseHandler
import com.microsoft.identity.common.java.nativeauth.providers.interactors.SignInInteractor
import com.microsoft.identity.common.java.nativeauth.providers.requests.signin.SignInInitiateRequest
import com.microsoft.identity.common.java.nativeauth.providers.responses.signin.SignInInitiateApiResponse
import com.microsoft.identity.common.java.nativeauth.providers.responses.signin.SignInInitiateApiResult
import com.microsoft.identity.common.java.net.HttpResponse
import com.microsoft.identity.common.java.net.UrlConnectionHttpClient
import com.microsoft.identity.common.java.util.ObjectMapper
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.whenever
import java.net.URL

class SignInInteractorTest {
    private val USERNAME = "user@email.com"
    private val CORRELATION_ID = "123456"

    private val mockUrlConnectionHttpClient = mock<UrlConnectionHttpClient>()
    private val mockNativeAuthRequestProvider = mock<NativeAuthRequestProvider>()
    private val mockNativeAuthResponseHandler = mock<NativeAuthResponseHandler>()

    private lateinit var signInInteractor: SignInInteractor

    @Before
    fun setup() {
        signInInteractor = SignInInteractor(
            httpClient = mockUrlConnectionHttpClient,
            nativeAuthRequestProvider = mockNativeAuthRequestProvider,
            nativeAuthResponseHandler = mockNativeAuthResponseHandler
        )
    }

    @Test
    fun testSignInInitiate() {
        // Arrange
        val parameters = SignInStartCommandParameters.builder()
            .platformComponents(mock<PlatformComponents>())
            .username(USERNAME)
            .correlationId(CORRELATION_ID)
            .build()

        val mockRequest = mock<SignInInitiateRequest>()
        val mockRequestParameters = mock<SignInInitiateRequest.NativeAuthRequestSignInInitiateParameters>()
        whenever(mockRequest.parameters).thenReturn(mockRequestParameters)
        whenever(mockNativeAuthRequestProvider.createSignInInitiateRequest(parameters)).thenReturn(mockRequest)

        val mockEncodedRequest = mock<String>()
        whenever(ObjectMapper.serializeObjectToFormUrlEncoded(mockRequestParameters)).thenReturn(mockEncodedRequest)

        val mockHeaders = mock<Map<String, String>>()
        whenever(mockRequest.headers).thenReturn(mockHeaders)

        val mockRequestUrl = mock<URL>()
        whenever(mockRequest.requestUrl).thenReturn(mockRequestUrl)

        val mockHttpResponse = mock<HttpResponse>()
        whenever(mockUrlConnectionHttpClient.post(
            mockRequestUrl,
            mockHeaders,
            mockEncodedRequest)
        ).thenReturn(mockHttpResponse)

        val mockApiResponse = spy<SignInInitiateApiResponse>()
        whenever(mockNativeAuthResponseHandler.getSignInInitiateResultFromHttpResponse(
            CORRELATION_ID,
            mockHttpResponse
        )).thenReturn(mockApiResponse)

        val mockApiResult = mock<SignInInitiateApiResult>()
        whenever(mockApiResponse.toResult()).thenReturn(mockApiResult)

        // Act
        val result = signInInteractor.performSignInInitiate(parameters)

        // Assert
        Assert.assertEquals(mockApiResult, result)
    }
}