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
package com.microsoft.identity.common.java.net

import com.microsoft.identity.http.MockConnection
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.verify
import java.net.URL

class UrlConnectionHttpClientRedirectConfigurationTest {

    @After
    fun tearDown() {
        HttpUrlConnectionFactory.clearMockedConnectionQueue()
    }

    @Test
    fun getDefaultInstance_whenSendingRequest_keepsRedirectsEnabled() {
        val connection = MockConnection.getMockedConnectionWithSuccessResponse()
        HttpUrlConnectionFactory.addMockedConnection(connection)

        UrlConnectionHttpClient.getDefaultInstance().get(
            URL("https://login.contoso.com/tenant"),
            emptyMap()
        )

        verify(connection).setInstanceFollowRedirects(true)
    }

    @Test
    fun builder_whenFollowRedirectsConfiguredFalse_disablesInstanceRedirects() {
        val connection = MockConnection.getMockedConnectionWithSuccessResponse()
        HttpUrlConnectionFactory.addMockedConnection(connection)

        val builder = UrlConnectionHttpClient.builder()
            .retryPolicy(NoRetryPolicy())
        val configuredBuilder = builder.javaClass.getMethod(
            "followRedirects",
            java.lang.Boolean::class.java
        ).invoke(builder, java.lang.Boolean.FALSE)
        val client = configuredBuilder.javaClass.getMethod("build").invoke(configuredBuilder) as UrlConnectionHttpClient

        client.get(URL("https://login.contoso.com/tenant"), emptyMap())

        verify(connection).setInstanceFollowRedirects(false)
    }

    @Test
    fun createDefaultConfiguredInstance_whenRedirectsDisabled_preservesDefaultRetryPolicy() {
        val client = UrlConnectionHttpClient.createDefaultConfiguredInstance(false)
        val retryPolicyField = UrlConnectionHttpClient::class.java.getDeclaredField("retryPolicy")
        retryPolicyField.isAccessible = true

        assertTrue(retryPolicyField.get(client) is StatusCodeAndExceptionRetry)
    }
}
