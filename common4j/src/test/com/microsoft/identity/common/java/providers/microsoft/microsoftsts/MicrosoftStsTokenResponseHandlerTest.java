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
import com.microsoft.identity.common.java.net.HttpResponse;
import com.microsoft.identity.common.java.providers.microsoft.MicrosoftTokenErrorResponse;
import com.microsoft.identity.common.java.providers.oauth2.TokenResult;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import lombok.SneakyThrows;

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

    /**
     * When ENABLE_SERVER_CLIENT_DATA_TELEMETRY is enabled and the response contains the
     * x-ms-clientdata header, handleTokenResponse should not crash (the parsed info is
     * emitted to the current span which is a no-op in unit tests).
     */
    @SneakyThrows
    @Test
    public void testHandleTokenResponse_withClientDataHeader_flightEnabled_doesNotCrash() {
        enableClientDataFlight();

        final String clientDataJson = URLEncoder.encode("{\"at\":\"m\",\"e\":\"50058\"}", StandardCharsets.UTF_8.name());
        final HashMap<String, List<String>> headers = new HashMap<>();
        headers.put("Content-Type", Collections.singletonList("application/json; charset=utf-8"));
        headers.put("x-ms-clientdata", Collections.singletonList(clientDataJson));

        final HttpResponse response = new HttpResponse(200, MOCK_TOKEN_SUCCESS_RESPONSE, headers);
        final MicrosoftStsTokenResponseHandler handler = new MicrosoftStsTokenResponseHandler();
        final TokenResult tokenResult = handler.handleTokenResponse(response);

        Assert.assertNotNull(tokenResult);
        Assert.assertTrue(tokenResult.getSuccess());
    }

    /**
     * When ENABLE_SERVER_CLIENT_DATA_TELEMETRY is disabled, the x-ms-clientdata header should
     * be silently ignored.
     */
    @SneakyThrows
    @Test
    public void testHandleTokenResponse_withClientDataHeader_flightDisabled_headerIgnored() {
        // Flight is disabled by default; ensure manager is at default state.
        final String clientDataJson = URLEncoder.encode("{\"at\":\"m\",\"e\":\"50058\"}", StandardCharsets.UTF_8.name());
        final HashMap<String, List<String>> headers = new HashMap<>();
        headers.put("Content-Type", Collections.singletonList("application/json; charset=utf-8"));
        headers.put("x-ms-clientdata", Collections.singletonList(clientDataJson));

        final HttpResponse response = new HttpResponse(200, MOCK_TOKEN_SUCCESS_RESPONSE, headers);
        final MicrosoftStsTokenResponseHandler handler = new MicrosoftStsTokenResponseHandler();
        final TokenResult tokenResult = handler.handleTokenResponse(response);

        Assert.assertNotNull(tokenResult);
        Assert.assertTrue(tokenResult.getSuccess());
    }

    /**
     * When ENABLE_SERVER_CLIENT_DATA_TELEMETRY is enabled but no x-ms-clientdata header is
     * present, no crash should occur.
     */
    @SneakyThrows
    @Test
    public void testHandleTokenResponse_noClientDataHeader_flightEnabled_doesNotCrash() {
        enableClientDataFlight();

        final HashMap<String, List<String>> headers = new HashMap<>();
        headers.put("Content-Type", Collections.singletonList("application/json; charset=utf-8"));

        final HttpResponse response = new HttpResponse(200, MOCK_TOKEN_SUCCESS_RESPONSE, headers);
        final MicrosoftStsTokenResponseHandler handler = new MicrosoftStsTokenResponseHandler();
        final TokenResult tokenResult = handler.handleTokenResponse(response);

        Assert.assertNotNull(tokenResult);
        Assert.assertTrue(tokenResult.getSuccess());
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private void enableClientDataFlight() {
        final MockFlightsProvider mockFlightsProvider = new MockFlightsProvider();
        mockFlightsProvider.addFlight(
                CommonFlight.ENABLE_SERVER_CLIENT_DATA_TELEMETRY.getKey(), "true");
        // Also enable the other default-true flights to keep behaviour correct.
        mockFlightsProvider.addFlight(
                CommonFlight.EXPOSE_CCS_REQUEST_ID_IN_TOKENRESPONSE.getKey(), "true");
        mockFlightsProvider.addFlight(
                CommonFlight.EXPOSE_CCS_REQUEST_SEQUENCE_IN_TOKENRESPONSE.getKey(), "true");

        final MockFlightsManager mockFlightsManager = new MockFlightsManager();
        mockFlightsManager.setMockBrokerFlightsProvider(mockFlightsProvider);
        CommonFlightsManager.INSTANCE.initializeCommonFlightsManager(mockFlightsManager);
    }
}
