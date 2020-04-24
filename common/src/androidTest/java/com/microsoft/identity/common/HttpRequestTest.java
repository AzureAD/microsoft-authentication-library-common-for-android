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

package com.microsoft.identity.common;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.microsoft.identity.common.internal.net.HttpRequest;
import com.microsoft.identity.common.internal.net.HttpResponse;
import com.microsoft.identity.common.internal.net.HttpUrlConnectionFactory;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mockito;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.util.Collections;

import static com.microsoft.identity.common.MockUtil.getMockedConnectionWithFailureResponse;
import static com.microsoft.identity.common.internal.net.HttpUrlConnectionFactory.addMockedConnection;
import static com.microsoft.identity.common.internal.net.HttpUrlConnectionFactory.getMockedConnectionCountInQueue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Tests for {@link HttpRequest}.
 * <p>
 * We need to fix setup of these tests as something is missing post moving from MSAL to common
 */
@RunWith(AndroidJUnit4.class)
public final class HttpRequestTest {

    @After
    public void tearDown() {
        HttpUrlConnectionFactory.clearMockedConnectionQueue();
    }

    /**
     * Verify the expected exception is thrown when sending get request with null url.
     */
    @Test(expected = NullPointerException.class)
    public void testNullRequestUrl() throws IOException {
        HttpRequest.sendGet(null, Collections.<String, String>emptyMap());
    }

    /**
     * Verify that http get succeed and no retry happens.
     */
    @Test
    public void testHttpGetSucceed() throws IOException {
        // prepare the connection, only one connection will be made.
        final HttpURLConnection mockedSuccessConnection =
                MockUtil.getMockedConnectionWithSuccessResponse(
                        getSuccessResponse()
                );
        addMockedConnection(mockedSuccessConnection);

        try {
            assertEquals(1, getMockedConnectionCountInQueue());
            final HttpResponse response = sendHttpGet();
            verifySuccessHttpResponse(response);
        } catch (final IOException e) {
            fail();
        }

        assertEquals(0, getMockedConnectionCountInQueue());
        final InOrder inOrder = Mockito.inOrder(mockedSuccessConnection);
        inOrder.verify(mockedSuccessConnection).getInputStream();
        inOrder.verify(mockedSuccessConnection, Mockito.never()).getErrorStream();
        inOrder.verify(mockedSuccessConnection).getResponseCode();
        inOrder.verify(mockedSuccessConnection).getHeaderFields();
        inOrder.verifyNoMoreInteractions();
    }

    /**
     * Verify that http post succeeds and no retry happens.
     */
    @Test
    public void testHttpPostSucceed() throws IOException {
        // prepare the connection, only one connection will be made.
        final HttpURLConnection mockedSuccessConnection =
                MockUtil.getMockedConnectionWithSuccessResponse(
                        getSuccessResponse()
                );
        mockRequestBody(mockedSuccessConnection);
        addMockedConnection(mockedSuccessConnection);

        try {
            assertEquals(1, getMockedConnectionCountInQueue());
            final HttpResponse response = sendHttpPost();
            verifySuccessHttpResponse(response);
        } catch (final IOException e) {
            fail();
        }

        assertEquals(0, getMockedConnectionCountInQueue());
        final InOrder inOrder = Mockito.inOrder(mockedSuccessConnection);
        // default times for verify is 1.
        inOrder.verify(mockedSuccessConnection).getInputStream();
        inOrder.verify(mockedSuccessConnection, Mockito.never()).getErrorStream();
        inOrder.verify(mockedSuccessConnection).getResponseCode();
        inOrder.verify(mockedSuccessConnection).getHeaderFields();
        inOrder.verifyNoMoreInteractions();
    }

    /**
     * Verify the correct response is returned if first network call fails with internal error,
     * and retry succeeds.
     */
    @Test
    public void testHttpPostFailedWith500RetrySucceed() throws IOException {
        // Set up two connections, the first is failed with 500, the second one succeeds.
        final HttpURLConnection firstConnection =
                getMockedConnectionWithFailureResponse(
                        HttpURLConnection.HTTP_INTERNAL_ERROR,
                        getErrorResponse()
                );
        mockRequestBody(firstConnection);

        final HttpURLConnection secondConnection =
                MockUtil.getMockedConnectionWithSuccessResponse(getSuccessResponse());
        mockRequestBody(secondConnection);

        addMockedConnection(firstConnection);
        addMockedConnection(secondConnection);

        try {
            assertEquals(2, getMockedConnectionCountInQueue());
            final HttpResponse response = sendHttpPost();
            verifySuccessHttpResponse(response);
        } catch (final IOException e) {
            fail();
        }

        assertEquals(0, getMockedConnectionCountInQueue());

        final InOrder inOrder = Mockito.inOrder(firstConnection, secondConnection);
        inOrder.verify(firstConnection).getInputStream();
        inOrder.verify(firstConnection).getErrorStream();
        inOrder.verify(firstConnection).getResponseCode();
        // no HttpResponse is created, no need to verify getHeaderFields.

        inOrder.verify(secondConnection).getInputStream();
        inOrder.verify(secondConnection, Mockito.never()).getErrorStream();
        inOrder.verify(secondConnection).getResponseCode();
        inOrder.verify(secondConnection).getHeaderFields();

        inOrder.verifyNoMoreInteractions();
    }

    /**
     * Verify that the initial post request failed with
     * {@link HttpURLConnection#HTTP_UNAVAILABLE} and retry succeeds.
     */
    @Test
    public void testHttpPostFailedWith503RetrySucceed() throws IOException {
        // Set up two connections, the first is failed with 503, the second one succeeds.
        final HttpURLConnection firstConnection =
                getMockedConnectionWithFailureResponse(
                        HttpURLConnection.HTTP_UNAVAILABLE,
                        getErrorResponse()
                );
        mockRequestBody(firstConnection);

        final HttpURLConnection secondConnection =
                MockUtil.getMockedConnectionWithSuccessResponse(
                        getSuccessResponse()
                );
        mockRequestBody(secondConnection);

        addMockedConnection(firstConnection);
        addMockedConnection(secondConnection);

        try {
            assertEquals(2, getMockedConnectionCountInQueue());
            final HttpResponse response = sendHttpPost();
            verifySuccessHttpResponse(response);
        } catch (final IOException e) {
            fail();
        }

        assertEquals(0, getMockedConnectionCountInQueue());

        final InOrder inOrder = Mockito.inOrder(firstConnection, secondConnection);
        inOrder.verify(firstConnection).getInputStream();
        inOrder.verify(firstConnection).getErrorStream();
        inOrder.verify(firstConnection).getResponseCode();
        // No HttpResponse created, no interaction on getHeaderFields

        inOrder.verify(secondConnection).getInputStream();
        inOrder.verify(secondConnection, Mockito.never()).getErrorStream();
        inOrder.verify(secondConnection).getResponseCode();
        inOrder.verify(secondConnection).getHeaderFields();

        inOrder.verifyNoMoreInteractions();
    }

    /**
     * Verify that the initial get request failed with {@link HttpURLConnection#HTTP_GATEWAY_TIMEOUT} and retry
     * succeeds.
     */
    @Test
    public void testHttpGetFailedWith504RetrySucceed() throws IOException {
        // Set up two connections, the first is failed with 503, the second one succeeds.
        final HttpURLConnection firstConnection =
                getMockedConnectionWithFailureResponse(
                        HttpURLConnection.HTTP_GATEWAY_TIMEOUT,
                        getErrorResponse()
                );
        final HttpURLConnection secondConnection =
                MockUtil.getMockedConnectionWithSuccessResponse(
                        getSuccessResponse()
                );

        addMockedConnection(firstConnection);
        addMockedConnection(secondConnection);

        try {
            assertEquals(2, getMockedConnectionCountInQueue());
            final HttpResponse response = sendHttpGet();
            verifySuccessHttpResponse(response);
        } catch (final IOException e) {
            fail();
        }

        assertEquals(0, getMockedConnectionCountInQueue());

        final InOrder inOrder = Mockito.inOrder(firstConnection, secondConnection);
        inOrder.verify(firstConnection).getInputStream();
        inOrder.verify(firstConnection).getErrorStream();
        inOrder.verify(firstConnection).getResponseCode();

        inOrder.verify(secondConnection).getInputStream();
        inOrder.verify(secondConnection, Mockito.never()).getErrorStream();
        inOrder.verify(secondConnection).getResponseCode();
        inOrder.verify(secondConnection).getHeaderFields();

        inOrder.verifyNoMoreInteractions();
    }

    /**
     * Verify that the initial post request failed with {@link SocketTimeoutException} and retry
     * succeeds.
     */
    @Test
    public void testHttpPostFailedWithSocketTimeoutRetrySucceed() throws IOException {
        // Set up two connections, the first is failed with SocketTimeout, the second one succeeds.
        final HttpURLConnection firstConnection = MockUtil.getMockedConnectionWithSocketTimeout();
        mockRequestBody(firstConnection);

        final HttpURLConnection secondConnection = MockUtil.getMockedConnectionWithSuccessResponse(
                getSuccessResponse());
        mockRequestBody(secondConnection);

        addMockedConnection(firstConnection);
        addMockedConnection(secondConnection);

        try {
            assertEquals(2, getMockedConnectionCountInQueue());
            final HttpResponse response = sendHttpPost();
            verifySuccessHttpResponse(response);
        } catch (final IOException e) {
            fail();
        }

        assertEquals(0, getMockedConnectionCountInQueue());
        final InOrder inOrder = Mockito.inOrder(firstConnection, secondConnection);
        inOrder.verify(firstConnection).getInputStream();
        inOrder.verify(firstConnection, Mockito.never()).getErrorStream();
        inOrder.verify(firstConnection, Mockito.never()).getResponseCode();

        inOrder.verify(secondConnection).getInputStream();
        inOrder.verify(secondConnection, Mockito.never()).getErrorStream();
        inOrder.verify(secondConnection).getResponseCode();
        inOrder.verify(secondConnection).getHeaderFields();

        inOrder.verifyNoMoreInteractions();
    }

    /**
     * Verify that http get request fails with {@link HttpURLConnection#HTTP_BAD_REQUEST},
     * no retry happens.
     */
    @Test
    public void testHttpGetFailedNoRetry() throws IOException {
        final HttpURLConnection mockedFailureConnection =
                getMockedConnectionWithFailureResponse(
                        HttpURLConnection.HTTP_BAD_REQUEST,
                        getErrorResponse()
                );
        addMockedConnection(mockedFailureConnection);

        try {
            assertEquals(1, getMockedConnectionCountInQueue());
            final HttpResponse response = sendHttpGet();
            assertNotNull(response);
            assertEquals(response.getStatusCode(), HttpURLConnection.HTTP_BAD_REQUEST);
            assertEquals(response.getBody(), getErrorResponse());
        } catch (final IOException e) {
            fail();
        }

        assertEquals(0, getMockedConnectionCountInQueue());

        final InOrder inOrder = Mockito.inOrder(mockedFailureConnection);
        inOrder.verify(mockedFailureConnection).getInputStream();
        inOrder.verify(mockedFailureConnection).getErrorStream();
        inOrder.verify(mockedFailureConnection).getResponseCode();
        inOrder.verify(mockedFailureConnection).getHeaderFields();

        inOrder.verifyNoMoreInteractions();
    }

    /**
     * Verify that http get request fails with {@link HttpURLConnection#HTTP_UNAUTHORIZED},
     * no retry happens.
     */
    @Test
    public void testHttpPostFailedNoRetry() throws IOException {
        final HttpURLConnection mockedFailureConnection =
                getMockedConnectionWithFailureResponse(
                        HttpURLConnection.HTTP_UNAUTHORIZED,
                        getErrorResponse()
                );
        mockRequestBody(mockedFailureConnection);
        addMockedConnection(mockedFailureConnection);

        // send a post request
        try {
            assertEquals(1, getMockedConnectionCountInQueue());

            final HttpResponse response = sendHttpPost();
            assertNotNull(response);
            assertEquals(response.getStatusCode(), HttpURLConnection.HTTP_UNAUTHORIZED);
            assertEquals(response.getBody(), getErrorResponse());
        } catch (final IOException e) {
            fail();
        }

        assertEquals(0, getMockedConnectionCountInQueue());

        final InOrder inOrder = Mockito.inOrder(mockedFailureConnection);
        inOrder.verify(mockedFailureConnection).getInputStream();
        inOrder.verify(mockedFailureConnection).getErrorStream();
        inOrder.verify(mockedFailureConnection).getResponseCode();
        inOrder.verify(mockedFailureConnection).getHeaderFields();
        inOrder.verifyNoMoreInteractions();
    }

    /**
     * Verify that http get request fails with {@link HttpURLConnection#HTTP_BAD_METHOD}
     * and no response body, no retry happens.
     */
    @Test
    public void testHttpGetFailedNoRetryNoResponseBody() throws IOException {
        final HttpURLConnection mockedFailureConnection =
                getMockedConnectionWithFailureResponse(
                        HttpURLConnection.HTTP_BAD_METHOD,
                        null
                );
        addMockedConnection(mockedFailureConnection);

        try {
            assertEquals(1, getMockedConnectionCountInQueue());
            final HttpResponse response = sendHttpGet();
            assertNotNull(response);
            assertEquals(response.getStatusCode(), HttpURLConnection.HTTP_BAD_METHOD);
            assertTrue(response.getBody().isEmpty());
        } catch (final IOException e) {
            fail();
        }

        assertEquals(0, getMockedConnectionCountInQueue());

        final InOrder inOrder = Mockito.inOrder(mockedFailureConnection);
        inOrder.verify(mockedFailureConnection).getInputStream();
        inOrder.verify(mockedFailureConnection).getErrorStream();
        inOrder.verify(mockedFailureConnection).getResponseCode();
        inOrder.verify(mockedFailureConnection).getHeaderFields();
        inOrder.verifyNoMoreInteractions();
    }

    /**
     * Verify that initial http post fails with {@link HttpURLConnection#HTTP_INTERNAL_ERROR}(retryable status code
     * 500/503/504), retry fails with {@link HttpURLConnection#HTTP_UNAUTHORIZED}(non retryable status code).
     */
    @Test
    public void testHttpPostFailedWithRetryableStatusCodeRetryFailsWithNonRetryableCode() throws IOException {
        // The first connection fails with retryable status code 500, the retry connection fails with 401.
        final HttpURLConnection firstConnection =
                getMockedConnectionWithFailureResponse(
                        HttpURLConnection.HTTP_INTERNAL_ERROR,
                        getErrorResponse()
                );
        mockRequestBody(firstConnection);

        final HttpURLConnection secondConnection =
                getMockedConnectionWithFailureResponse(
                        HttpURLConnection.HTTP_UNAUTHORIZED,
                        getErrorResponse()
                );
        mockRequestBody(secondConnection);

        addMockedConnection(firstConnection);
        addMockedConnection(secondConnection);

        try {
            assertEquals(2, getMockedConnectionCountInQueue());
            final HttpResponse response = sendHttpGet();
            assertNotNull(response);
            assertEquals(response.getStatusCode(), HttpURLConnection.HTTP_UNAUTHORIZED);
            assertEquals(response.getBody(), getErrorResponse());
        } catch (final IOException e) {
            fail();
        }

        assertEquals(0, getMockedConnectionCountInQueue());

        final InOrder inOrder = Mockito.inOrder(firstConnection, secondConnection);
        inOrder.verify(firstConnection).getInputStream();
        inOrder.verify(firstConnection).getErrorStream();
        inOrder.verify(firstConnection).getResponseCode();

        inOrder.verify(secondConnection).getInputStream();
        inOrder.verify(secondConnection).getErrorStream();
        inOrder.verify(secondConnection).getResponseCode();
        inOrder.verify(secondConnection).getHeaderFields();

        inOrder.verifyNoMoreInteractions();
    }

    /**
     * Verify that initial http post fails with {@link SocketTimeoutException}, retry fails with
     * {@link HttpURLConnection#HTTP_BAD_REQUEST}(non retryable status code).
     */
    @Test
    public void testHttpPostFailedWithSocketTimeoutRetryFailedWithNonRetryableCode() throws IOException {
        // The first connection fails with retryable SocketTimeout, the retry connection fails with 400.
        final HttpURLConnection firstConnection = MockUtil.getMockedConnectionWithSocketTimeout();
        mockRequestBody(firstConnection);

        final HttpURLConnection secondConnection = getMockedConnectionWithFailureResponse(
                HttpURLConnection.HTTP_BAD_REQUEST, getErrorResponse());
        mockRequestBody(secondConnection);

        addMockedConnection(firstConnection);
        addMockedConnection(secondConnection);

        try {
            assertEquals(2, getMockedConnectionCountInQueue());
            final HttpResponse response = sendHttpGet();
            assertNotNull(response);
            assertEquals(response.getStatusCode(), HttpURLConnection.HTTP_BAD_REQUEST);
            assertEquals(response.getBody(), getErrorResponse());
        } catch (final IOException e) {
            fail();
        }

        assertEquals(0, getMockedConnectionCountInQueue());
        final InOrder inOrder = Mockito.inOrder(firstConnection, secondConnection);
        inOrder.verify(firstConnection).getInputStream();
        inOrder.verify(firstConnection, Mockito.never()).getErrorStream();
        inOrder.verify(firstConnection, Mockito.never()).getResponseCode();

        inOrder.verify(secondConnection).getInputStream();
        inOrder.verify(secondConnection).getErrorStream();
        inOrder.verify(secondConnection).getResponseCode();
        inOrder.verify(secondConnection).getHeaderFields();

        inOrder.verifyNoMoreInteractions();
    }


    private void verifySuccessHttpResponse(final HttpResponse httpResponse) {
        assertNotNull(httpResponse);
        assertEquals(httpResponse.getStatusCode(), HttpURLConnection.HTTP_OK);
        assertEquals(httpResponse.getBody(), getSuccessResponse());
    }

    /**
     * Send http get request.
     */
    private HttpResponse sendHttpGet() throws IOException {
        return HttpRequest.sendGet(
                Util.getValidRequestUrl(),
                Collections.<String, String>emptyMap()
        );
    }

    /**
     * Send http post request.
     */
    private HttpResponse sendHttpPost() throws IOException {
        return HttpRequest.sendPost(
                Util.getValidRequestUrl(),
                Collections.<String, String>emptyMap(),
                "SomeRequestMessage".getBytes(),
                "application/x-www-form-urlencoded"
        );
    }

    /**
     * @return Successful response from server.
     */
    private String getSuccessResponse() {
        return "{\"response\":\"success response\"}";
    }

    /**
     * @return Error response from server.
     */
    private String getErrorResponse() {
        return "{\"response\":\"error response\"}";
    }

    private void mockRequestBody(final HttpURLConnection mockedConnection) throws IOException {
        Mockito.when(mockedConnection.getOutputStream())
                .thenReturn(Mockito.mock(OutputStream.class));
    }
}
