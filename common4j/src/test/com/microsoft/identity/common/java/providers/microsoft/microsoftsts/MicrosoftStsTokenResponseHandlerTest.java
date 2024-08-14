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

import com.microsoft.identity.common.java.net.HttpResponse;
import com.microsoft.identity.common.java.providers.microsoft.MicrosoftTokenErrorResponse;
import com.microsoft.identity.common.java.providers.oauth2.TokenResult;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

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
}
