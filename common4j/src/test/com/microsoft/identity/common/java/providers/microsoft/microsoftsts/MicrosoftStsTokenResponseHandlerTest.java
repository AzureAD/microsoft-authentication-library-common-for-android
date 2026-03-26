// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// This code is licensed under the MIT License.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files(the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions :
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.
package com.microsoft.identity.common.java.providers.microsoft.microsoftsts;

import com.microsoft.identity.common.java.flighting.CommonFlight;
import com.microsoft.identity.common.java.flighting.CommonFlightsManager;
import com.microsoft.identity.common.java.flighting.MockFlightsManager;
import com.microsoft.identity.common.java.flighting.MockFlightsProvider;
import com.microsoft.identity.common.java.net.HttpConstants;
import com.microsoft.identity.common.java.net.HttpResponse;
import com.microsoft.identity.common.java.opentelemetry.AttributeName;
import com.microsoft.identity.common.java.opentelemetry.SpanExtension;
import com.microsoft.identity.common.java.providers.microsoft.MicrosoftTokenErrorResponse;
import com.microsoft.identity.common.java.providers.oauth2.TokenResult;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.MockedStatic;

import java.net.URLEncoder;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import io.opentelemetry.api.trace.Span;
import lombok.SneakyThrows;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link MicrosoftStsTokenResponseHandler}
 */
@RunWith(JUnit4.class)
public class MicrosoftStsTokenResponseHandlerTest {
    private static final String MOCK_TOKEN_SUCCESS_RESPONSE = "{\n" +
            "\t\"token_type\": \"Bearer\",\n" +
            "\t\"scope\": \"mock_scope_1\",\n" +
            "\t\"expires_in\": 3599,\n" +
            "\t\"ext_expires_in\": 3599,\n" +
            "\t\"access_token\": \"b06d0810-12ff-4a4e-850b-4bda1540d895\",\n" +
            "\t\"refresh_token\": \"6b80f5b5-d53c-4c46-992d-66c5dcd4cfb1\",\n" +
            "\t\"id_token\": \"95608142-3a7a-4643-a543-6db44e403e97\",\n" +
            "\t\"client_info\": \"2245f73e-287a-41c4-ba87-560809ad06b9\"\n" +
            "}";

    @After
    public void tearDown() {
        CommonFlightsManager.INSTANCE.resetFlightsManager();
    }

    @SneakyThrows
    @Test
    public void testHandleTokenResponse_Success() {
        final HashMap<String, List<String>> headers = new HashMap<>();
        headers.put("Content-Type", Collections.singletonList("application/json; charset=utf-8"));
        final HttpResponse mockErrorResponse = new HttpResponse(200, MOCK_TOKEN_SUCCESS_RESPONSE, headers);
        final MicrosoftStsTokenResponseHandler handler = new MicrosoftStsTokenResponseHandler();
        final TokenResult tokenResult = handler.handleTokenResponse(mockErrorResponse);
        Assert.assertNotNull(tokenResult);
        Assert.assertTrue(tokenResult.getSuccess());
        Assert.assertNotNull(tokenResult.getSuccessResponse());
        Assert.assertNull(tokenResult.getErrorResponse());
        Assert.assertTrue(tokenResult.getSuccessResponse() instanceof MicrosoftStsTokenResponse);
        final MicrosoftStsTokenResponse successResponse = (MicrosoftStsTokenResponse) tokenResult.getSuccessResponse();
        Assert.assertEquals("b06d0810-12ff-4a4e-850b-4bda1540d895", successResponse.getAccessToken());
        Assert.assertEquals("6b80f5b5-d53c-4c46-992d-66c5dcd4cfb1", successResponse.getRefreshToken());
        Assert.assertEquals("95608142-3a7a-4643-a543-6db44e403e97", successResponse.getIdToken());
        Assert.assertEquals("Bearer", successResponse.getTokenType());
    }

    @SneakyThrows
    @Test
    public void testHandleTokenResponse_Error() {
        final HttpResponse mockErrorResponse = new HttpResponse(400, "Bad Request", null);
        final MicrosoftStsTokenResponseHandler handler = new MicrosoftStsTokenResponseHandler();
        final TokenResult tokenResult = handler.handleTokenResponse(mockErrorResponse);
        Assert.assertNotNull(tokenResult);
        Assert.assertFalse(tokenResult.getSuccess());
        Assert.assertNull(tokenResult.getSuccessResponse());
        Assert.assertNotNull(tokenResult.getErrorResponse());
        Assert.assertTrue(tokenResult.getErrorResponse() instanceof MicrosoftTokenErrorResponse);
        final MicrosoftTokenErrorResponse errorResponse = (MicrosoftTokenErrorResponse) tokenResult.getErrorResponse();
        Assert.assertEquals(400, errorResponse.getStatusCode());
        Assert.assertEquals("Bad Request", errorResponse.getResponseBody());
    }

    @SneakyThrows
    @Test
    public void testHandleTokenResponse_xMsClientDataHeader_flightEnabled_attributesEmitted() {
        // Enable the flight
        final MockFlightsProvider flightsProvider = new MockFlightsProvider();
        flightsProvider.addFlight(CommonFlight.ENABLE_SERVER_CLIENT_DATA_TELEMETRY.getKey(), "true");
        final MockFlightsManager flightsManager = new MockFlightsManager();
        flightsManager.setMockBrokerFlightsProvider(flightsProvider);
        CommonFlightsManager.INSTANCE.initializeCommonFlightsManager(flightsManager);

        final String json = "{\"Error\":\"50001\",\"SubError\":\"sub1\",\"AccountType\":\"e\"}";
        final String encodedHeader = URLEncoder.encode(json, "UTF-8");

        final HashMap<String, List<String>> headers = new HashMap<>();
        headers.put("Content-Type", Collections.singletonList("application/json; charset=utf-8"));
        headers.put(HttpConstants.HeaderField.X_MS_CLIENTDATA,
                Collections.singletonList(encodedHeader));

        final Span mockSpan = mock(Span.class);
        when(mockSpan.setAttribute(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString())).thenReturn(mockSpan);

        try (MockedStatic<SpanExtension> spanExtensionMock = mockStatic(SpanExtension.class)) {
            spanExtensionMock.when(SpanExtension::current).thenReturn(mockSpan);

            final HttpResponse response = new HttpResponse(200, MOCK_TOKEN_SUCCESS_RESPONSE, headers);
            new MicrosoftStsTokenResponseHandler().handleTokenResponse(response);

            verify(mockSpan).setAttribute(AttributeName.server_error.name(), "50001");
            verify(mockSpan).setAttribute(AttributeName.server_sub_error.name(), "sub1");
            verify(mockSpan).setAttribute(AttributeName.account_type.name(), "AAD");
        }
    }

    @SneakyThrows
    @Test
    public void testHandleTokenResponse_noXMsClientDataHeader_noNullPointerException() {
        // Enable the flight – but no header present
        final MockFlightsProvider flightsProvider = new MockFlightsProvider();
        flightsProvider.addFlight(CommonFlight.ENABLE_SERVER_CLIENT_DATA_TELEMETRY.getKey(), "true");
        final MockFlightsManager flightsManager = new MockFlightsManager();
        flightsManager.setMockBrokerFlightsProvider(flightsProvider);
        CommonFlightsManager.INSTANCE.initializeCommonFlightsManager(flightsManager);

        final HashMap<String, List<String>> headers = new HashMap<>();
        headers.put("Content-Type", Collections.singletonList("application/json; charset=utf-8"));

        final HttpResponse response = new HttpResponse(200, MOCK_TOKEN_SUCCESS_RESPONSE, headers);
        // Should not throw
        final TokenResult result = new MicrosoftStsTokenResponseHandler().handleTokenResponse(response);
        Assert.assertNotNull(result);
    }

    @SneakyThrows
    @Test
    public void testHandleTokenResponse_xMsClientDataHeader_flightDisabled_attributesNotEmitted() {
        // Flight is disabled by default – no custom provider needed

        final String json = "{\"Error\":\"50001\"}";
        final String encodedHeader = URLEncoder.encode(json, "UTF-8");

        final HashMap<String, List<String>> headers = new HashMap<>();
        headers.put("Content-Type", Collections.singletonList("application/json; charset=utf-8"));
        headers.put(HttpConstants.HeaderField.X_MS_CLIENTDATA,
                Collections.singletonList(encodedHeader));

        final Span mockSpan = mock(Span.class);
        when(mockSpan.setAttribute(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString())).thenReturn(mockSpan);

        try (MockedStatic<SpanExtension> spanExtensionMock = mockStatic(SpanExtension.class)) {
            spanExtensionMock.when(SpanExtension::current).thenReturn(mockSpan);

            final HttpResponse response = new HttpResponse(200, MOCK_TOKEN_SUCCESS_RESPONSE, headers);
            new MicrosoftStsTokenResponseHandler().handleTokenResponse(response);

            verify(mockSpan, never()).setAttribute(
                    org.mockito.ArgumentMatchers.eq(AttributeName.server_error.name()),
                    org.mockito.ArgumentMatchers.anyString());
        }
    }
}

