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
package com.microsoft.identity.common.java.net.util;

import com.microsoft.identity.common.java.net.HttpResponse;

import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;

import lombok.NonNull;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class MockConnection {
    public static HttpURLConnection getMockedConnectionWithSuccessResponse() throws IOException {
        final HttpURLConnection mockedHttpUrlConnection = getCommonHttpUrlConnection();

        Mockito.when(mockedHttpUrlConnection.getInputStream()).thenReturn(createInputStream(ResponseBody.SUCCESS));
        Mockito.when(mockedHttpUrlConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);

        return mockedHttpUrlConnection;
    }

    public static HttpURLConnection getMockedConnectionWithFailureResponse(final int statusCode)
            throws IOException {
        return getMockedConnectionWithFailureResponse(statusCode, ResponseBody.GENERIC_ERROR);
    }

    public static HttpURLConnection getMockedConnectionWithFailureResponse(final int statusCode,
                                                                           final String responseBody)
            throws IOException {
        final HttpURLConnection mockedHttpUrlConnection = getCommonHttpUrlConnection();

        Mockito.when(mockedHttpUrlConnection.getInputStream()).thenThrow(IOException.class);
        Mockito.when(mockedHttpUrlConnection.getErrorStream()).thenReturn(createInputStream(responseBody));
        Mockito.when(mockedHttpUrlConnection.getResponseCode()).thenReturn(statusCode);

        return mockedHttpUrlConnection;
    }

    public static HttpURLConnection getMockedConnectionWithSocketTimeout() throws IOException {
        final HttpURLConnection mockedUrlConnection = getCommonHttpUrlConnection();

        Mockito.when(mockedUrlConnection.getInputStream()).thenThrow(SocketTimeoutException.class);
        return mockedUrlConnection;
    }

    public static HttpURLConnection getCommonHttpUrlConnection() throws IOException {
        final HttpURLConnection mockedConnection = Mockito.mock(HttpURLConnection.class);
        Mockito.doNothing().when(mockedConnection).setConnectTimeout(Mockito.anyInt());
        Mockito.doNothing().when(mockedConnection).setDoInput(Mockito.anyBoolean());
        Mockito.doReturn(System.currentTimeMillis() / 1000).when(mockedConnection).getDate();
        return mockedConnection;
    }

    public static void verifySuccessHttpResponse(@NonNull final HttpResponse httpResponse) {
        assertNotNull(httpResponse);
        assertEquals(httpResponse.getStatusCode(), HttpURLConnection.HTTP_OK);
        assertEquals(httpResponse.getBody(), ResponseBody.SUCCESS);
    }

    private static InputStream createInputStream(final String input) {
        return input == null ? null : new ByteArrayInputStream(input.getBytes());
    }
}
