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

package com.microsoft.identity.common.java.net;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.microsoft.identity.common.java.net.util.MockConnection;
import com.microsoft.identity.common.java.net.util.ResponseBody;

import org.junit.After;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;

/**
 * Tests for {@link UrlConnectionHttpClient}.
 *
 * "Specific" = use "UrlConnectionHttpClient.get()" for HttpTestMethod.Get.
 * "Not specific" = use the underlying "UrlConnectionHttpClient.method()" for HttpTestMethod.Get.
 */
public final class UrlConnectionHttpClientTest {

    private static final Charset UTF8 = StandardCharsets.UTF_8;
    private static final String CONTENT_TYPE_KEY = "Content-Type";
    private static final String CONTENT_TYPE_VALUE = "application/x-www-form-urlencoded";

    // The UrlConnectionHttpClient.getDefaultInstance() comes with a retry logic.
    // For non-retry scenario, we need to create a separate client.
    private static final UrlConnectionHttpClient sNoRetryClient = UrlConnectionHttpClient.builder()
            .retryPolicy(new NoRetryPolicy())
            .build();

    @After
    public void tearDown() {
        HttpUrlConnectionFactory.clearMockedConnectionQueue();
        SSLSocketFactoryWrapper.setLastHandshakeTLSversion("");
    }

    /**
     * Verify the expected exception is thrown when sending get request with null url.
     */
    @Test(expected = NullPointerException.class)
    public void testNullRequestUrl() throws IOException {
        sNoRetryClient.get(null, Collections.<String, String>emptyMap());
    }

    /**
     * Verify that HTTP GET succeed
     * - via {@link UrlConnectionHttpClient#method(String, URL, Map, byte[])}
     * - with retry logic.
     */
    @Test
    public void testHttpGetSucceed_WithMethod() throws Exception {
        testHttpMethodSucceed(HttpTestMethod.GET, false, true);
    }

    /**
     * Verify that HTTP GET succeed
     * - via {@link UrlConnectionHttpClient#get(URL, Map)}
     * - with retry logic.
     */
    @Test
    public void testHttpGetSucceed_WithGet() throws Exception {
        testHttpMethodSucceed(HttpTestMethod.GET, true, true);
    }

    /**
     * Verify that HTTP GET succeeds
     * - via {@link UrlConnectionHttpClient#method(String, URL, Map, byte[])}
     * - without retry logic.
     */
    @Test
    public void testHttpGetSucceed_WithMethod_NoRetry() throws Exception {
        testHttpMethodSucceed(HttpTestMethod.GET, false, false);
    }

    /**
     * Verify that HTTP GET succeed
     * - via {@link UrlConnectionHttpClient#get(URL, Map)}
     * - without retry logic.
     */
    @Test
    public void testHttpGetSucceed_WithGet_NoRetry() throws Exception {
        testHttpMethodSucceed(HttpTestMethod.GET, true, false);
    }

    /**
     * Verify that HTTP POST succeed
     * - via {@link UrlConnectionHttpClient#method(String, URL, Map, byte[])}
     * - with retry logic.
     */
    @Test
    public void testHttpPostSucceed_WithMethod() throws Exception {
        testHttpMethodSucceed(HttpTestMethod.HEAD, false, true);
    }

    /**
     * Verify that HTTP POST succeed
     * - via {@link UrlConnectionHttpClient#post(URL, Map, byte[])}
     * - with retry logic.
     */
    @Test
    public void testHttpPostSucceed_WithPost() throws Exception {
        testHttpMethodSucceed(HttpTestMethod.HEAD, true, true);
    }

    /**
     * Verify that HTTP POST succeed
     * - via {@link UrlConnectionHttpClient#method(String, URL, Map, byte[])}
     * - without retry logic.
     */
    @Test
    public void testHttpPostSucceed_WithMethod_NoRetry() throws Exception {
        testHttpMethodSucceed(HttpTestMethod.HEAD, false, false);
    }

    /**
     * Verify that HTTP POST succeed
     * - via {@link UrlConnectionHttpClient#post(URL, Map, byte[])}
     * - without retry logic.
     */
    @Test
    public void testHttpPostSucceed_WithPost_NoRetry() throws Exception {
        testHttpMethodSucceed(HttpTestMethod.HEAD, true, false);
    }

    /**
     * Verify that HTTP HEAD succeed
     * - via {@link UrlConnectionHttpClient#method(String, URL, Map, byte[])}
     * - with retry logic.
     */
    @Test
    public void testHttpHeadSucceed_WithMethod() throws Exception {
        testHttpMethodSucceed(HttpTestMethod.HEAD, false, true);
    }

    /**
     * Verify that HTTP HEAD succeed
     * - via {@link UrlConnectionHttpClient#head(URL, Map)}
     * - with retry logic.
     */
    @Test
    public void testHttpHeadSucceed_WithHead() throws Exception {
        testHttpMethodSucceed(HttpTestMethod.HEAD, true, true);
    }

    /**
     * Verify that HTTP HEAD succeed
     * - via {@link UrlConnectionHttpClient#method(String, URL, Map, byte[])}
     * - without retry logic.
     */
    @Test
    public void testHttpHeadSucceed_WithMethod_NoRetry() throws Exception {
        testHttpMethodSucceed(HttpTestMethod.HEAD, false, false);
    }

    /**
     * Verify that HTTP HEAD succeed
     * - via {@link UrlConnectionHttpClient#head(URL, Map)}
     * - without retry logic.
     */
    @Test
    public void testHttpHeadSucceed_WithHead_NoRetry() throws Exception {
        testHttpMethodSucceed(HttpTestMethod.HEAD, true, false);
    }

    /**
     * Verify that HTTP PUT succeeds
     * - via {@link UrlConnectionHttpClient#method(String, URL, Map, byte[])}
     * - with retry logic.
     */
    @Test
    public void testHttpPutSucceed_WithMethod() throws Exception {
        testHttpMethodSucceed(HttpTestMethod.PUT, false, true);
    }

    /**
     * Verify that HTTP PUT succeeds
     * - via {@link UrlConnectionHttpClient#put(URL, Map, byte[])}
     * - with retry logic.
     */
    @Test
    public void testHttpPutSucceed_WithPut() throws Exception {
        testHttpMethodSucceed(HttpTestMethod.PUT, true, true);
    }

    /**
     * Verify that HTTP PUT succeeds
     * - via {@link UrlConnectionHttpClient#method(String, URL, Map, byte[])}
     * - without retry logic.
     */
    @Test
    public void testHttpPutSucceed_WithMethod_NoRetry() throws Exception {
        testHttpMethodSucceed(HttpTestMethod.PUT, false, false);
    }

    /**
     * Verify that HTTP PUT succeeds
     * - via {@link UrlConnectionHttpClient#put(URL, Map, byte[])}
     * - without retry logic.
     */
    @Test
    public void testHttpPutSucceed_WithPut_NoRetry() throws Exception {
        testHttpMethodSucceed(HttpTestMethod.PUT, true, false);
    }

    /**
     * Verify that HTTP DELETE succeeds
     * - via {@link UrlConnectionHttpClient#method(String, URL, Map, byte[])}
     * - with retry logic.
     */
    @Test
    public void testHttpDeleteSucceed_WithMethod() throws Exception {
        testHttpMethodSucceed(HttpTestMethod.DELETE, false, true);
    }

    /**
     * Verify that HTTP DELETE succeeds
     * - via {@link UrlConnectionHttpClient#delete(URL, Map, byte[])}
     * - with retry logic.
     */
    @Test
    public void testHttpDeleteSucceed_WithDelete() throws Exception {
        testHttpMethodSucceed(HttpTestMethod.DELETE, true, true);
    }

    /**
     * Verify that HTTP DELETE succeeds
     * - via {@link UrlConnectionHttpClient#method(String, URL, Map, byte[])}
     * - without retry logic.
     */
    @Test
    public void testHttpDeleteSucceed_WithMethod_NoRetry() throws Exception {
        testHttpMethodSucceed(HttpTestMethod.DELETE, false, false);
    }

    /**
     * Verify that HTTP DELETE succeeds
     * - via {@link UrlConnectionHttpClient#delete(URL, Map, byte[])}
     * - without retry logic.
     */
    @Test
    public void testHttpDeleteSucceed_WithDelete_NoRetry() throws Exception {
        testHttpMethodSucceed(HttpTestMethod.DELETE, true, false);
    }

    /**
     * Verify that HTTP TRACE succeeds
     * - via {@link UrlConnectionHttpClient#method(String, URL, Map, byte[])}
     * - with retry logic.
     */
    @Test
    public void testHttpTraceSucceed_WithMethod() throws Exception {
        testHttpMethodSucceed(HttpTestMethod.TRACE, false, true);
    }

    /**
     * Verify that HTTP TRACE succeeds
     * - via {@link UrlConnectionHttpClient#trace(URL, Map))}
     * - with retry logic.
     */
    @Test
    public void testHttpTraceSucceed_WithTrace() throws Exception {
        testHttpMethodSucceed(HttpTestMethod.TRACE, true, true);
    }

    /**
     * Verify that HTTP TRACE succeeds
     * - via {@link UrlConnectionHttpClient#method(String, URL, Map, byte[])}
     * - without retry logic.
     */
    @Test
    public void testHttpTraceSucceed_WithMethod_NoRetry() throws Exception {
        testHttpMethodSucceed(HttpTestMethod.TRACE, false, false);
    }

    /**
     * Verify that HTTP TRACE succeeds
     * - via {@link UrlConnectionHttpClient#trace(URL, Map))}
     * - without retry logic.
     */
    @Test
    public void testHttpTraceSucceed_WithTrace_NoRetry() throws Exception {
        testHttpMethodSucceed(HttpTestMethod.TRACE, true, false);
    }

    /**
     * Verify that HTTP OPTIONS succeeds
     * - via {@link UrlConnectionHttpClient#method(String, URL, Map, byte[])}
     * - with retry logic.
     */
    @Test
    public void testHttpOptionsSucceed_WithMethod() throws Exception {
        testHttpMethodSucceed(HttpTestMethod.OPTIONS, false, true);
    }

    /**
     * Verify that HTTP OPTIONS succeeds
     * - via {@link UrlConnectionHttpClient#options(URL, Map)}
     * - with retry logic.
     */
    @Test
    public void testHttpOptionsSucceed_WithOptions() throws Exception {
        testHttpMethodSucceed(HttpTestMethod.OPTIONS, true, true);
    }

    /**
     * Verify that HTTP OPTIONS succeeds
     * - via {@link UrlConnectionHttpClient#method(String, URL, Map, byte[])}
     * - without retry logic.
     */
    @Test
    public void testHttpOptionsSucceed_WithMethod_NoRetry() throws Exception {
        testHttpMethodSucceed(HttpTestMethod.OPTIONS, false, false);
    }

    /**
     * Verify that HTTP OPTIONS succeeds
     * - via {@link UrlConnectionHttpClient#options(URL, Map)}
     * - without retry logic.
     */
    @Test
    public void testHttpOptionsSucceed_WithOptions_NoRetry() throws Exception {
        testHttpMethodSucceed(HttpTestMethod.OPTIONS, true, false);
    }

    /**
     * Verify that HTTP PATCH succeeds
     * - via {@link UrlConnectionHttpClient#method(String, URL, Map, byte[])}
     * - with retry logic.
     */
    @Test
    public void testHttpPatchSucceed_WithMethod() throws Exception {
        testHttpMethodSucceed(HttpTestMethod.PATCH, false, true);
    }
    
    /**
     * Verify that HTTP PATCH succeeds
     * - via {@link UrlConnectionHttpClient#patch(URL, Map, byte[])}}
     * - with retry logic.
     */
    @Test
    public void testHttpPatchSucceed_WithPatch() throws Exception {
        testHttpMethodSucceed(HttpTestMethod.PATCH, true, true);
    }

    /**
     * Verify that HTTP PATCH succeeds
     * - via {@link UrlConnectionHttpClient#method(String, URL, Map, byte[])}
     * - without retry logic.
     */
    @Test
    public void testHttpPatchSucceed_WithMethod_NoRetry() throws Exception {
        testHttpMethodSucceed(HttpTestMethod.PATCH, false, false);
    }

    /**
     * Verify that HTTP PATCH succeeds
     * - via {@link UrlConnectionHttpClient#patch(URL, Map, byte[])}
     * - without retry logic.
     */
    @Test
    public void testHttpPatchSucceed_WithPatch_NoRetry() throws Exception {
        testHttpMethodSucceed(HttpTestMethod.PATCH, true, false);
    }

    /**
     * Verify that when an HTTP method succeeds, no retry happens.
     */
    private void testHttpMethodSucceed(HttpTestMethod method, boolean specific, boolean retries) throws Exception {
        // prepare the connection, only one connection will be made.
        final HttpURLConnection mockedSuccessConnection = MockConnection.getMockedConnectionWithSuccessResponse();
        mockRequestBody(mockedSuccessConnection);
        HttpUrlConnectionFactory.addMockedConnection(mockedSuccessConnection);

        try {
            assertEquals(1, HttpUrlConnectionFactory.getMockedConnectionCountInQueue());
            final HttpResponse response = sendMethod(method, specific, retries);
            MockConnection.verifySuccessHttpResponse(response);
        } catch (final IOException e) {
            fail();
        }

        assertEquals(0, HttpUrlConnectionFactory.getMockedConnectionCountInQueue());
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
    private void testHttpMethodFailedWithStatusCodeRetrySucceed(HttpTestMethod method, int code) throws Exception {
        // Set up two connections, the first is failed with 500, the second one succeeds.
        final HttpURLConnection firstConnection =
                MockConnection.getMockedConnectionWithFailureResponse(code);
        mockRequestBody(firstConnection);

        final HttpURLConnection secondConnection =
                MockConnection.getMockedConnectionWithSuccessResponse();
        mockRequestBody(secondConnection);

        HttpUrlConnectionFactory.addMockedConnection(firstConnection);
        HttpUrlConnectionFactory.addMockedConnection(secondConnection);

        try {
            assertEquals(2, HttpUrlConnectionFactory.getMockedConnectionCountInQueue());
            final HttpResponse response = sendWithMethod(method);
            MockConnection.verifySuccessHttpResponse(response);
        } catch (final IOException e) {
            fail();
        }

        assertEquals(0, HttpUrlConnectionFactory.getMockedConnectionCountInQueue());

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
     * Verify the correct response is returned if first network call fails with internal error,
     * and retry also fails.
     */
    @Test
    public void testPostFailsWithTwoErrors500() throws Exception {
        final HttpURLConnection firstConnection =
                MockConnection.getMockedConnectionWithFailureResponse(500);
        mockRequestBody(firstConnection);

        final HttpURLConnection secondConnection =
                MockConnection.getMockedConnectionWithFailureResponse(500);
        mockRequestBody(secondConnection);

        HttpUrlConnectionFactory.addMockedConnection(firstConnection);
        HttpUrlConnectionFactory.addMockedConnection(secondConnection);

        assertEquals(2, HttpUrlConnectionFactory.getMockedConnectionCountInQueue());

        sendWithMethod(HttpTestMethod.POST);

        final InOrder inOrder = Mockito.inOrder(firstConnection, secondConnection);
        inOrder.verify(firstConnection).getInputStream();
        inOrder.verify(firstConnection).getErrorStream();
        inOrder.verify(firstConnection).getResponseCode();
        inOrder.verify(firstConnection).getDate();
        inOrder.verify(firstConnection).getHeaderFields();
        inOrder.verify(secondConnection).getInputStream();
        inOrder.verify(secondConnection).getErrorStream();
        inOrder.verify(secondConnection).getResponseCode();
        inOrder.verify(secondConnection).getDate();
        inOrder.verify(secondConnection).getHeaderFields();

        inOrder.verifyNoMoreInteractions();
    }

    /**
     * Verify that if the client is configured without retry, no retry happens.
     */
    @Test
    public void testNoRetryDoesNotRetry() throws Exception {
        for (int code : Arrays.asList(500, 503, 504, 404, 499)) {
            for (HttpTestMethod method : HttpTestMethod.values()) {
                testHttpMethodFailedWithStatusCodeWithoutRetryNoRetryHappens(method, code);
            }
        }
    }

    private void testHttpMethodFailedWithStatusCodeWithoutRetryNoRetryHappens(HttpTestMethod method, int code) throws Exception {
        // Set up two connections, the first is failed with 500, the second one does not occur.
        final HttpURLConnection firstConnection =
                MockConnection.getMockedConnectionWithFailureResponse(code);
        mockRequestBody(firstConnection);

        final HttpURLConnection secondConnection =
                MockConnection.getMockedConnectionWithSuccessResponse();
        mockRequestBody(secondConnection);

        HttpUrlConnectionFactory.addMockedConnection(firstConnection);
        HttpUrlConnectionFactory.addMockedConnection(secondConnection);

        try {
            assertEquals(2, HttpUrlConnectionFactory.getMockedConnectionCountInQueue());
            final HttpResponse response = sendMethod(method, false, false);
            assertEquals(code, response.getStatusCode());
        } catch (final IOException e) {
            fail();
        }

        assertEquals(1, HttpUrlConnectionFactory.getMockedConnectionCountInQueue());

        final InOrder inOrder = Mockito.inOrder(firstConnection, secondConnection);
        inOrder.verify(firstConnection).getInputStream();
        inOrder.verify(firstConnection).getErrorStream();
        inOrder.verify(firstConnection).getResponseCode();
        inOrder.verify(firstConnection).getDate();
        inOrder.verify(firstConnection).getHeaderFields();
        inOrder.verifyNoMoreInteractions();

        HttpUrlConnectionFactory.clearMockedConnectionQueue();
    }

    /**
     * Verify the correct response is returned if first network call fails with internal error,
     * and retry succeeds.
     */
    @Test
    public void testHttpPostFailedWith500RetrySucceed() throws Exception {
        testHttpMethodFailedWithStatusCodeRetrySucceed(HttpTestMethod.POST, 500);
    }

    /**
     * Verify the correct response is returned if first network call fails with internal error,
     * and retry succeeds.
     */
    @Test
    public void testHttpGetFailedWith500RetrySucceed() throws Exception {
        testHttpMethodFailedWithStatusCodeRetrySucceed(HttpTestMethod.GET, 500);
    }

    /**
     * Verify the correct response is returned if first network call fails with internal error,
     * and retry succeeds.
     */
    @Test
    public void testHttpPutFailedWith500RetrySucceed() throws Exception {
        testHttpMethodFailedWithStatusCodeRetrySucceed(HttpTestMethod.PUT, 500);
    }

    /**
     * Verify the correct response is returned if first network call fails with internal error,
     * and retry succeeds.
     */
    @Test
    public void testHttpOptionsFailedWith500RetrySucceed() throws Exception {
        testHttpMethodFailedWithStatusCodeRetrySucceed(HttpTestMethod.OPTIONS, 500);
    }

    /**
     * Verify that the initial post request failed with
     * {@link HttpURLConnection#HTTP_UNAVAILABLE} and retry succeeds.
     */
    @Test
    public void testHttpPostFailedWith503RetrySucceed() throws Exception {
        testHttpMethodFailedWithStatusCodeRetrySucceed(HttpTestMethod.POST, 503);
    }

    /**
     * Verify that the initial get request failed with {@link HttpURLConnection#HTTP_GATEWAY_TIMEOUT} and retry
     * succeeds.
     */
    @Test
    public void testHttpGetFailedWith504RetrySucceed() throws Exception {
        testHttpMethodFailedWithStatusCodeRetrySucceed(HttpTestMethod.GET, 504);
    }

    /**
     * Verify that the initial post request failed with {@link SocketTimeoutException} and retry
     * succeeds.
     */
    @Test
    public void testHttpMethodsFailWithSocketTimeoutRetrySucceed() throws Exception {
        for (HttpTestMethod method : HttpTestMethod.values()) {
            testHttpMethodFailedWithSocketTimeoutRetrySucceed(method);
        }
    }

    /**
     * Verify that the initial post request failed with {@link SocketTimeoutException} and retry
     * succeeds.
     */
    private void testHttpMethodFailedWithSocketTimeoutRetrySucceed(HttpTestMethod method) throws Exception {
        // Set up two connections, the first is failed with SocketTimeout, the second one succeeds.
        final HttpURLConnection firstConnection = MockConnection.getMockedConnectionWithSocketTimeout();
        mockRequestBody(firstConnection);

        final HttpURLConnection secondConnection = MockConnection.getMockedConnectionWithSuccessResponse();
        mockRequestBody(secondConnection);

        HttpUrlConnectionFactory.addMockedConnection(firstConnection);
        HttpUrlConnectionFactory.addMockedConnection(secondConnection);

        try {
            assertEquals(2, HttpUrlConnectionFactory.getMockedConnectionCountInQueue());
            final HttpResponse response = sendWithMethod(method);
            MockConnection.verifySuccessHttpResponse(response);
        } catch (final IOException e) {
            fail();
        }

        assertEquals(0, HttpUrlConnectionFactory.getMockedConnectionCountInQueue());
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
    public void testHttpGetFailedNoRetry() throws Exception {
        testHttpMethodFailedNoRetry(HttpTestMethod.GET, HttpURLConnection.HTTP_BAD_REQUEST);
    }

    /**
     * Verify that http POST request fails with {@link HttpURLConnection#HTTP_UNAUTHORIZED},
     * no retry happens.
     */
    @Test
    public void testHttpPostFailedNoRetry() throws Exception {
        testHttpMethodFailedNoRetry(HttpTestMethod.POST, HttpURLConnection.HTTP_UNAUTHORIZED);
    }

    @Test
    public void testHttpPutFailedNoRetry() throws Exception {
        testHttpMethodFailedNoRetry(HttpTestMethod.PUT, HttpURLConnection.HTTP_UNAUTHORIZED);
    }

    @Test
    public void testHttpOptionsFailedNoRetry() throws Exception {
        testHttpMethodFailedNoRetry(HttpTestMethod.OPTIONS, HttpURLConnection.HTTP_UNAUTHORIZED);
    }

    @Test
    public void testHttpTraceFailedNoRetry() throws Exception {
        testHttpMethodFailedNoRetry(HttpTestMethod.TRACE, HttpURLConnection.HTTP_UNAUTHORIZED);
    }

    @Test
    public void testHttpPatchtFailedNoRetry() throws Exception {
        testHttpMethodFailedNoRetry(HttpTestMethod.PATCH, HttpURLConnection.HTTP_UNAUTHORIZED);
    }

    @Test
    public void testHttpDeleteFailedNoRetry() throws Exception {
        testHttpMethodFailedNoRetry(HttpTestMethod.DELETE, HttpURLConnection.HTTP_UNAUTHORIZED);
    }

    @Test
    public void testHttpHeadFailedNoRetry() throws Exception {
        testHttpMethodFailedNoRetry(HttpTestMethod.HEAD, HttpURLConnection.HTTP_UNAUTHORIZED);
    }

    /**
     * Verify that http request fails with {@link HttpURLConnection#HTTP_UNAUTHORIZED},
     * no retry happens.
     */
    private void testHttpMethodFailedNoRetry(HttpTestMethod method, int code) throws Exception {
        final HttpURLConnection mockedFailureConnection =
                MockConnection.getMockedConnectionWithFailureResponse(code);
        mockRequestBody(mockedFailureConnection);
        HttpUrlConnectionFactory.addMockedConnection(mockedFailureConnection);

        // send a post request
        try {
            assertEquals(1, HttpUrlConnectionFactory.getMockedConnectionCountInQueue());

            final HttpResponse response = sendWithMethod(method);
            assertNotNull(response);
            assertEquals(response.getStatusCode(), code);
            assertEquals(response.getBody(), ResponseBody.GENERIC_ERROR);
        } catch (final IOException e) {
            fail();
        }

        assertEquals(0, HttpUrlConnectionFactory.getMockedConnectionCountInQueue());

        final InOrder inOrder = Mockito.inOrder(mockedFailureConnection);
        inOrder.verify(mockedFailureConnection).getInputStream();
        inOrder.verify(mockedFailureConnection).getErrorStream();
        inOrder.verify(mockedFailureConnection).getResponseCode();
        inOrder.verify(mockedFailureConnection).getDate();
        inOrder.verify(mockedFailureConnection).getHeaderFields();
        inOrder.verifyNoMoreInteractions();
    }

    /**
     * Verify that http get request fails with {@link HttpURLConnection#HTTP_BAD_METHOD}
     * and no response body, no retry happens.
     */
    public void testHttpMethodFailedNoRetryNoResponseBody(HttpTestMethod method) throws Exception {
        final HttpURLConnection mockedFailureConnection =
                MockConnection.getMockedConnectionWithFailureResponse(HttpURLConnection.HTTP_BAD_METHOD, null);
        HttpUrlConnectionFactory.addMockedConnection(mockedFailureConnection);

        try {
            assertEquals(1, HttpUrlConnectionFactory.getMockedConnectionCountInQueue());
            final HttpResponse response = sendWithMethod(method);
            assertNotNull(response);
            assertEquals(response.getStatusCode(), HttpURLConnection.HTTP_BAD_METHOD);
            assertTrue(response.getBody().isEmpty());
        } catch (final IOException e) {
            fail();
        }

        assertEquals(0, HttpUrlConnectionFactory.getMockedConnectionCountInQueue());

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
    public void testHttpGetFailedNoRetryNoResponseBody() throws Exception {
        testHttpMethodFailedNoRetryNoResponseBody(HttpTestMethod.GET);
    }

    /**
     * Verify that initial http post fails with {@link HttpURLConnection#HTTP_INTERNAL_ERROR}(retryable status code
     * 500/503/504), retry fails with {@link HttpURLConnection#HTTP_UNAUTHORIZED}(non retryable status code).
     */
    @Test
    public void testHttpPostFailedWithRetryableStatusCodeRetryFailsWithNonRetryableCode() throws Exception {
        sendMethodWithRetryableStatusCodeRetryFailsWithNonRetryableCode(HttpTestMethod.POST);

    }

    @Test
    public void testHttpPutFailedWithRetryableStatusCodeRetryFailsWithNonRetryableCode() throws Exception {
        sendMethodWithRetryableStatusCodeRetryFailsWithNonRetryableCode(HttpTestMethod.PUT);

    }

    @Test
    public void testHttpPatchFailedWithRetryableStatusCodeRetryFailsWithNonRetryableCode() throws Exception {
        sendMethodWithRetryableStatusCodeRetryFailsWithNonRetryableCode(HttpTestMethod.PATCH);

    }

    @Test
    public void testHttpOptionsFailedWithRetryableStatusCodeRetryFailsWithNonRetryableCode() throws Exception {
        sendMethodWithRetryableStatusCodeRetryFailsWithNonRetryableCode(HttpTestMethod.OPTIONS);

    }

    @Test
    public void testHttpTraceFailedWithRetryableStatusCodeRetryFailsWithNonRetryableCode() throws Exception {
        sendMethodWithRetryableStatusCodeRetryFailsWithNonRetryableCode(HttpTestMethod.TRACE);

    }

    @Test
    public void testHttpGetFailedWithRetryableStatusCodeRetryFailsWithNonRetryableCode() throws Exception {
        sendMethodWithRetryableStatusCodeRetryFailsWithNonRetryableCode(HttpTestMethod.GET);

    }

    @Test
    public void testHttpHeadFailedWithRetryableStatusCodeRetryFailsWithNonRetryableCode() throws Exception {
        sendMethodWithRetryableStatusCodeRetryFailsWithNonRetryableCode(HttpTestMethod.HEAD);
    }

    @Test
    public void testHttpDeleteFailedWithRetryableStatusCodeRetryFailsWithNonRetryableCode() throws Exception {
        sendMethodWithRetryableStatusCodeRetryFailsWithNonRetryableCode(HttpTestMethod.DELETE);

    }

    private void sendMethodWithRetryableStatusCodeRetryFailsWithNonRetryableCode(HttpTestMethod method) throws Exception {
        // The first connection fails with retryable status code 500, the retry connection fails with 401.
        final HttpURLConnection firstConnection =
                MockConnection.getMockedConnectionWithFailureResponse(
                        HttpURLConnection.HTTP_INTERNAL_ERROR
                );
        mockRequestBody(firstConnection);

        final HttpURLConnection secondConnection =
                MockConnection.getMockedConnectionWithFailureResponse(
                        HttpURLConnection.HTTP_UNAUTHORIZED
                );
        mockRequestBody(secondConnection);

        HttpUrlConnectionFactory.addMockedConnection(firstConnection);
        HttpUrlConnectionFactory.addMockedConnection(secondConnection);

        try {
            assertEquals(2, HttpUrlConnectionFactory.getMockedConnectionCountInQueue());
            final HttpResponse response = sendWithMethod(method);
            assertNotNull(response);
            assertEquals(response.getStatusCode(), HttpURLConnection.HTTP_UNAUTHORIZED);
            assertEquals(response.getBody(), ResponseBody.GENERIC_ERROR);
        } catch (final IOException e) {
            fail();
        }

        assertEquals(0, HttpUrlConnectionFactory.getMockedConnectionCountInQueue());

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
    public void testHttpPostFailedWithSocketTimeoutRetryFailedWithNonRetryableCode() throws Exception {
        // The first connection fails with retryable SocketTimeout, the retry connection fails with 400.
        final HttpURLConnection firstConnection = MockConnection.getMockedConnectionWithSocketTimeout();
        mockRequestBody(firstConnection);

        final HttpURLConnection secondConnection = MockConnection.getMockedConnectionWithFailureResponse(
                HttpURLConnection.HTTP_BAD_REQUEST
        );
        mockRequestBody(secondConnection);

        HttpUrlConnectionFactory.addMockedConnection(firstConnection);
        HttpUrlConnectionFactory.addMockedConnection(secondConnection);

        try {
            assertEquals(2, HttpUrlConnectionFactory.getMockedConnectionCountInQueue());
            final HttpResponse response = sendWithMethod(HttpTestMethod.POST);
            assertNotNull(response);
            assertEquals(response.getStatusCode(), HttpURLConnection.HTTP_BAD_REQUEST);
            assertEquals(response.getBody(), ResponseBody.GENERIC_ERROR);
        } catch (final IOException e) {
            fail();
        }

        assertEquals(0, HttpUrlConnectionFactory.getMockedConnectionCountInQueue());
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

    /**
     * Verify that initial http post fails with {@link SocketTimeoutException}, when we're not
     * running in legacy retry mode the socket timeout exception propagates to the caller.
     */
    @Test(expected = SocketTimeoutException.class)
    public void testHttpPostFailedWithSocketTimeoutNoRetriesDoesNotRetry() throws Exception {
        // The first connection fails with retryable SocketTimeout, the retry connection fails with 400.
        final HttpURLConnection firstConnection = MockConnection.getMockedConnectionWithSocketTimeout();
        mockRequestBody(firstConnection);

        final HttpURLConnection secondConnection = MockConnection.getMockedConnectionWithSocketTimeout();
        mockRequestBody(secondConnection);

        HttpUrlConnectionFactory.addMockedConnection(firstConnection);
        HttpUrlConnectionFactory.addMockedConnection(secondConnection);

        try {
            assertEquals(2, HttpUrlConnectionFactory.getMockedConnectionCountInQueue());
            sendWithMethodWithoutRetry(HttpTestMethod.POST);
        } catch (final IOException e) {
            if (!(e instanceof SocketTimeoutException)) {
                fail();
            }
            throw e;
        } finally {
            assertEquals(1, HttpUrlConnectionFactory.getMockedConnectionCountInQueue());
            final InOrder inOrder = Mockito.inOrder(firstConnection, secondConnection);
            inOrder.verify(firstConnection).getInputStream();
            inOrder.verify(firstConnection, Mockito.never()).getErrorStream();
            inOrder.verify(firstConnection, Mockito.never()).getResponseCode();
            inOrder.verifyNoMoreInteractions();
        }
    }

    private enum HttpTestMethod {
        GET {
            boolean canHaveBody() {
                return false;
            }

            HttpResponse specific(URL url, Map<String, String> headers, byte[] body) throws Exception {
                return UrlConnectionHttpClient.getBuilderWithDefaultRetryPolicy().build().get(url, headers);
            }

            HttpResponse specificNoRetry(URL url, Map<String, String> headers, byte[] body) throws Exception {
                return sNoRetryClient.get(url, headers);
            }
        },
        HEAD {
            boolean canHaveBody() {
                return false;
            }

            HttpResponse specific(URL url, Map<String, String> headers, byte[] body) throws Exception {
                return UrlConnectionHttpClient.getBuilderWithDefaultRetryPolicy().build().head(url, headers);
            }

            HttpResponse specificNoRetry(URL url, Map<String, String> headers, byte[] body) throws Exception {
                return sNoRetryClient.head(url, headers);
            }
        },
        PUT {
            HttpResponse specific(URL url, Map<String, String> headers, byte[] body) throws Exception {
                return UrlConnectionHttpClient.getBuilderWithDefaultRetryPolicy().build().put(url, headers, body);
            }

            HttpResponse specificNoRetry(URL url, Map<String, String> headers, byte[] body) throws Exception {
                return sNoRetryClient.put(url, headers, body);
            }
        },
        POST {
            HttpResponse specific(URL url, Map<String, String> headers, byte[] body) throws Exception {
                return UrlConnectionHttpClient.getBuilderWithDefaultRetryPolicy().build().post(url, headers, body);
            }

            HttpResponse specificNoRetry(URL url, Map<String, String> headers, byte[] body) throws Exception {
                return sNoRetryClient.post(url, headers, body);
            }
        },
        OPTIONS {
            HttpResponse specific(URL url, Map<String, String> headers, byte[] body) throws Exception {
                return UrlConnectionHttpClient.getBuilderWithDefaultRetryPolicy().build().options(url, headers);
            }

            HttpResponse specificNoRetry(URL url, Map<String, String> headers, byte[] body) throws Exception {
                return sNoRetryClient.options(url, headers);
            }
        },
        TRACE {
            boolean canHaveBody() {
                return false;
            }

            HttpResponse specific(URL url, Map<String, String> headers, byte[] body) throws Exception {
                return UrlConnectionHttpClient.getBuilderWithDefaultRetryPolicy().build().trace(url, headers);
            }

            HttpResponse specificNoRetry(URL url, Map<String, String> headers, byte[] body) throws Exception {
                return sNoRetryClient.trace(url, headers);
            }
        },
        PATCH {
            HttpResponse specific(URL url, Map<String, String> headers, byte[] body) throws Exception {
                return UrlConnectionHttpClient.getBuilderWithDefaultRetryPolicy().build().patch(url, headers, body);
            }

            HttpResponse specificNoRetry(URL url, Map<String, String> headers, byte[] body) throws Exception {
                return sNoRetryClient.patch(url, headers, body);
            }
        },
        DELETE {
            HttpResponse specific(URL url, Map<String, String> headers, byte[] body) throws Exception {
                return UrlConnectionHttpClient.getBuilderWithDefaultRetryPolicy().build().delete(url, headers, body);
            }

            HttpResponse specificNoRetry(URL url, Map<String, String> headers, byte[] body) throws Exception {
                return sNoRetryClient.delete(url, headers, body);
            }
        };

        abstract HttpResponse specific(URL url, Map<String, String> headers, byte[] body) throws Exception;

        abstract HttpResponse specificNoRetry(URL url, Map<String, String> headers, byte[] body) throws Exception;

        boolean canHaveBody() {
            return true;
        }
    }

    private HttpResponse sendMethod(final HttpTestMethod method, boolean specific, boolean retries) throws Exception {
        if (specific){
            if (retries) {
                return sendSpecific(method);
            } else {
                return sendSpecificWithoutRetry(method);
            }
        } else {
            if (retries) {
                return sendWithMethod(method);
            } else {
                return sendWithMethodWithoutRetry(method);
            }
        }
    }

    private HttpResponse sendSpecific(final HttpTestMethod method) throws Exception {
        URL validRequestUrl = getRequestUrl();
        return method.specific(
                validRequestUrl,
                method.canHaveBody() ? Collections.singletonMap(CONTENT_TYPE_KEY, CONTENT_TYPE_VALUE) : Collections.<String, String>emptyMap(),
                method.canHaveBody() ? UUID.nameUUIDFromBytes((validRequestUrl.toString() + method).getBytes(UTF8)).toString().getBytes(UTF8) : null);
    }

    private HttpResponse sendSpecificWithoutRetry(final HttpTestMethod method) throws Exception {
        URL validRequestUrl = getRequestUrl();
        return method.specificNoRetry(
                validRequestUrl,
                method.canHaveBody() ? Collections.singletonMap(CONTENT_TYPE_KEY, CONTENT_TYPE_VALUE) : Collections.<String, String>emptyMap(),
                method.canHaveBody() ? UUID.nameUUIDFromBytes((validRequestUrl.toString() + method).getBytes(UTF8)).toString().getBytes(UTF8) : null);
    }

    private HttpResponse sendWithMethod(final HttpTestMethod method) throws Exception {
        URL validRequestUrl = getRequestUrl();
        return UrlConnectionHttpClient.getBuilderWithDefaultRetryPolicy().build().method(
                method.name(),
                validRequestUrl,
                method.canHaveBody() ? Collections.singletonMap(CONTENT_TYPE_KEY, CONTENT_TYPE_VALUE) : Collections.<String, String>emptyMap(),
                method.canHaveBody() ? UUID.nameUUIDFromBytes((validRequestUrl.toString() + method).getBytes(UTF8)).toString().getBytes(UTF8) : null
        );
    }

    private HttpResponse sendWithMethodWithoutRetry(final HttpTestMethod method) throws Exception {
        URL validRequestUrl = getRequestUrl();
        return sNoRetryClient.method(
                method.name(),
                validRequestUrl,
                method.canHaveBody() ? Collections.singletonMap(CONTENT_TYPE_KEY, CONTENT_TYPE_VALUE) : Collections.<String, String>emptyMap(),
                method.canHaveBody() ? UUID.nameUUIDFromBytes((validRequestUrl.toString() + method).getBytes(UTF8)).toString().getBytes(UTF8) : null
        );
    }

    private void mockRequestBody(final HttpURLConnection mockedConnection) throws IOException {
        Mockito.when(mockedConnection.getOutputStream())
                .thenReturn(Mockito.mock(OutputStream.class));
    }

    private URL getRequestUrl() throws MalformedURLException {
        return new URL("https://login.microsoftonline.com/common");
    }

    /**
     * Note: The following tests are made against https://badssl.com
     *       and they could change the URL/configuration at any time.
     *       If these test fails, check the website first.
     */

    @Test
    public void testNoSSL() throws IOException {
        final HttpResponse response = sNoRetryClient.method(
                HttpClient.HttpMethod.GET,
                new URL("http://http.badssl.com/"),
                new LinkedHashMap<String, String>(),
                null
        );

        Assert.assertEquals(200,response.getStatusCode());
        Assert.assertEquals("", SSLSocketFactoryWrapper.getLastHandshakeTLSversion());
    }

    @Test
    @Ignore("Ignored because our pipeline doesn't support TLS1.0 and TLS1.1. This still can be run locally.")
    public void testTLS1() throws IOException {
        final HttpResponse response = sNoRetryClient.method(
                HttpClient.HttpMethod.GET,
                new URL("https://tls-v1-0.badssl.com:1010/"),
                new LinkedHashMap<String, String>(),
                null
        );

        Assert.assertEquals(200, response.getStatusCode());
        Assert.assertEquals("TLSv1", SSLSocketFactoryWrapper.getLastHandshakeTLSversion());
    }

    @Test
    @Ignore("Ignored because our pipeline doesn't support TLS1.0 and TLS1.1. This still can be run locally.")
    public void testTLS11() throws IOException, NoSuchAlgorithmException, KeyManagementException {
        final HttpResponse response = sNoRetryClient.method(
                HttpClient.HttpMethod.GET,
                new URL("https://tls-v1-1.badssl.com:1011/"),
                new LinkedHashMap<String, String>(),
                null
        );

        Assert.assertEquals(200, response.getStatusCode());
        Assert.assertEquals("TLSv1.1", SSLSocketFactoryWrapper.getLastHandshakeTLSversion());
    }

    @Test
    public void testTLS12() throws IOException {
        final HttpResponse response = sNoRetryClient.method(
                HttpClient.HttpMethod.GET,
                new URL("https://tls-v1-2.badssl.com:1012/"),
                new LinkedHashMap<String, String>(),
                null
        );

        Assert.assertEquals(200, response.getStatusCode());
        Assert.assertEquals("TLSv1.2", SSLSocketFactoryWrapper.getLastHandshakeTLSversion());
    }

    @Test
    public void testTLSWithTLS11Context() throws IOException, NoSuchAlgorithmException, KeyManagementException {
        final SSLContext context = SSLContext.getInstance("TLSv1.1");
        context.init(null, null, new SecureRandom());

        final HttpClient client = UrlConnectionHttpClient.builder()
                .retryPolicy(new NoRetryPolicy())
                .sslContext(context)
                .build();

        // Microsoft.com supports TLS 1.3
        // https://www.ssllabs.com/ssltest/analyze.html?d=www.microsoft.com&s=2600%3a1406%3a1400%3a69d%3a0%3a0%3a0%3a356e&hideResults=on&ignoreMismatch=on
        final HttpResponse response = client.method(
                HttpClient.HttpMethod.GET,
                new URL("https://www.microsoft.com/"),
                new LinkedHashMap<String, String>(),
                null
        );

        Assert.assertEquals(200, response.getStatusCode());

        // The version specified in SSLContext.getInstance() will always be overwritten by
        // SSLSocketFactoryWrapper.SUPPORTED_SSL_PROTOCOLS (or any values that was set during
        // the initialization of UrlConnectionHttpClient.
        Assert.assertEquals("TLSv1.3", SSLSocketFactoryWrapper.getLastHandshakeTLSversion());
    }

    @Test
    public void testPickHighestTLS() throws IOException {
        // Microsoft.com supports TLS 1.3
        // https://www.ssllabs.com/ssltest/analyze.html?d=www.microsoft.com&s=2600%3a1406%3a1400%3a69d%3a0%3a0%3a0%3a356e&hideResults=on&ignoreMismatch=on
        final HttpResponse response = sNoRetryClient.method(
                HttpClient.HttpMethod.GET,
                new URL("https://www.microsoft.com/"),
                new LinkedHashMap<String, String>(),
                null
        );

        Assert.assertEquals(200, response.getStatusCode());
        Assert.assertEquals("TLSv1.3", SSLSocketFactoryWrapper.getLastHandshakeTLSversion());
    }

    @Test(expected = SSLHandshakeException.class)
    public void testConnectingToTLS13ServerWhileEnforcing12OnClientSide() throws IOException {
        final UrlConnectionHttpClient client = UrlConnectionHttpClient.builder()
                .supportedSslProtocols(Arrays.asList("TLSv1.3"))
                .build();

        final HttpResponse response = client.method(
                HttpClient.HttpMethod.GET,
                new URL("https://tls-v1-2.badssl.com:1012/"),
                new LinkedHashMap<String, String>(),
                null
        );

        Assert.fail();
    }

    @Test
    public void testSpecifyingSupportedSSLVersion() throws IOException {
        final UrlConnectionHttpClient client = UrlConnectionHttpClient.builder()
                .supportedSslProtocols(Arrays.asList("TLSv1.2"))
                .build();

        // Microsoft.com supports TLS 1.3
        // https://www.ssllabs.com/ssltest/analyze.html?d=www.microsoft.com&s=2600%3a1406%3a1400%3a69d%3a0%3a0%3a0%3a356e&hideResults=on&ignoreMismatch=on
        final HttpResponse response = client.method(
                HttpClient.HttpMethod.GET,
                new URL("https://www.microsoft.com/"),
                new LinkedHashMap<String, String>(),
                null
        );

        Assert.assertEquals(200, response.getStatusCode());
        Assert.assertEquals("TLSv1.2", SSLSocketFactoryWrapper.getLastHandshakeTLSversion());
    }

    @Test(expected = IllegalStateException.class)
    public void testConnectingToHttpsButGetHttpUrlConnection() throws IOException {
        HttpUrlConnectionFactory.addMockedConnection(MockConnection.getMockedHttpConnection());
        final HttpResponse response = sNoRetryClient.method(
                HttpClient.HttpMethod.GET,
                new URL("https://www.microsoft.com/"),
                new LinkedHashMap<String, String>(),
                null
        );

        Assert.fail();
    }

    @Test
    public void testConnectingToHttp() throws IOException {
        HttpUrlConnectionFactory.addMockedConnection(MockConnection.getMockedHttpConnection());
        final HttpResponse response = sNoRetryClient.method(
                HttpClient.HttpMethod.GET,
                new URL("http://www.somewebsite.com/"),
                new LinkedHashMap<String, String>(),
                null
        );

        Assert.assertEquals(200, response.getStatusCode());
    }
}