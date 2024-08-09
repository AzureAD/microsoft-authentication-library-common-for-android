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

import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.net.HttpResponse;
import com.microsoft.identity.common.java.util.ObjectMapper;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.UUID;

import lombok.NonNull;

/**
 * Tests for {@link MicrosoftStsTokenRequest}

 */
@RunWith(JUnit4.class)
public final class MicrosoftStsTokenRequestTests {

    @Test
    public void testCorrelationIdSerializedCorrectly(){
        UUID correlationId = UUID.randomUUID();
        MicrosoftStsTokenRequest request = new MicrosoftStsTokenRequest();
        request.setCorrelationId(correlationId);

        String jsonRequest = ObjectMapper.serializeObjectToJsonString(request);

        MicrosoftStsTokenRequest deserializedRequest = ObjectMapper.deserializeJsonStringToObject(jsonRequest, MicrosoftStsTokenRequest.class);

        Assert.assertEquals(correlationId, deserializedRequest.getCorrelationId());
    }

    @Test
    public void testGetTokenResponseHandler(){
        MicrosoftStsTokenRequest request = new MicrosoftStsTokenRequest();
        Assert.assertNotNull(request.getTokenResponseHandler());
        Assert.assertTrue(request.getTokenResponseHandler() instanceof MicrosoftStsTokenResponseHandler);
        final AbstractMicrosoftStsTokenResponseHandler mockHandler = new AbstractMicrosoftStsTokenResponseHandler() {
            @Override
            protected MicrosoftStsTokenResponse getSuccessfulResponse(@NonNull HttpResponse httpResponse) {
                return new MicrosoftStsTokenResponse();
            }
        };
        request.setTokenResponseHandler(mockHandler);
        Assert.assertEquals(mockHandler, request.getTokenResponseHandler());
    }
}
