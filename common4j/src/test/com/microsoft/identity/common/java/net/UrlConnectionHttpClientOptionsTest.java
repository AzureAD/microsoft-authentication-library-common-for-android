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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.exception.ConnectionError;
import com.microsoft.identity.common.java.logging.DiagnosticContext;
import com.microsoft.identity.common.java.logging.Logger;
import com.microsoft.identity.common.java.telemetry.AbstractTelemetryContext;
import com.microsoft.identity.common.java.telemetry.Telemetry;
import com.microsoft.identity.common.java.telemetry.TelemetryConfiguration;
import com.microsoft.identity.common.java.telemetry.TelemetryEventStrings;
import com.microsoft.identity.common.java.telemetry.observers.ITelemetryDefaultObserver;
import com.microsoft.identity.common.java.util.ported.Supplier;

import org.junit.After;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.HttpRetryException;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HttpsURLConnection;

public class UrlConnectionHttpClientOptionsTest {
    private static final int MAX_BODY_BYTES = 262144;
    private static final byte[] REQUEST_BODY = "synthetic-proof".getBytes(StandardCharsets.UTF_8);
    private static final String SENTINEL = "PRIVATE-url-jwt-account-header-error-description";
    private static final Map<String, String> REQUEST_HEADERS = Collections.singletonMap(
            "Content-Type", "application/x-www-form-urlencoded");

    @After
    public void clearConnections() {
        HttpUrlConnectionFactory.clearMockedConnectionQueue();
    }

    @Test
    public void nativeOptions_configureBothEntryPointsBeforeOutput_withoutRetryingStatuses() throws Exception {
        for (final boolean patch : new boolean[]{false, true}) {
            for (final int status : new int[]{200, 301, 302, 307, 308, 401, 407, 500, 503, 504}) {
                final HttpURLConnection connection = connection(status, new byte[0], false);
                final OutputStream output = connection.getOutputStream();
                Mockito.clearInvocations(connection);
                HttpUrlConnectionFactory.addMockedConnection(connection);
                HttpUrlConnectionFactory.addMockedConnection(Mockito.mock(HttpURLConnection.class));

                final HttpResponse response = patch
                        ? nativeClient().patch(url(), REQUEST_HEADERS, REQUEST_BODY)
                        : nativeClient().post(url(), REQUEST_HEADERS, REQUEST_BODY);

                assertEquals(status, response.getStatusCode());
                final InOrder order = Mockito.inOrder(connection, output);
                order.verify(connection).setInstanceFollowRedirects(false);
                order.verify(connection).setUseCaches(false);
                order.verify(connection).setFixedLengthStreamingMode(REQUEST_BODY.length);
                order.verify(connection).getOutputStream();
                order.verify(output).write(REQUEST_BODY);
                Mockito.verify(output).close();
                Mockito.verify(connection).getInputStream();
                assertEquals("No second connection may be used for a response status.",
                        1, HttpUrlConnectionFactory.getMockedConnectionCountInQueue());
                HttpUrlConnectionFactory.clearMockedConnectionQueue();
            }
        }
    }

    @Test
    public void nativeOptions_authStreamingFailure_doesNotReplay() throws Exception {
        final HttpURLConnection connection = connection(401, new byte[0], false);
        final OutputStream output = connection.getOutputStream();
        Mockito.when(connection.getInputStream()).thenThrow(new HttpRetryException(SENTINEL, 401));
        Mockito.when(connection.getErrorStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
        Mockito.clearInvocations(connection);
        HttpUrlConnectionFactory.addMockedConnection(connection);
        HttpUrlConnectionFactory.addMockedConnection(Mockito.mock(HttpURLConnection.class));

        assertEquals(401, nativeClient().post(url(), REQUEST_HEADERS, REQUEST_BODY).getStatusCode());
        Mockito.verify(connection).getOutputStream();
        Mockito.verify(output).write(REQUEST_BODY);
        assertEquals(1, HttpUrlConnectionFactory.getMockedConnectionCountInQueue());
    }

    @Test
    public void nativeOptions_responseTimeout_doesNotRetry() throws Exception {
        final HttpURLConnection connection = connection(200, new byte[0], false);
        Mockito.when(connection.getInputStream()).thenThrow(new SocketTimeoutException(SENTINEL));
        HttpUrlConnectionFactory.addMockedConnection(connection);
        HttpUrlConnectionFactory.addMockedConnection(Mockito.mock(HttpURLConnection.class));

        final ClientException error = expectIoFailure(nativeClient());
        assertTrue(ConnectionError.CONNECTION_TIMEOUT.compare(error));
        assertNull(error.getCause());
        assertFalse(error.getMessage().contains(SENTINEL));
        Mockito.verify(connection).getOutputStream();
        assertEquals(1, HttpUrlConnectionFactory.getMockedConnectionCountInQueue());
    }

    @Test
    public void nativeOptions_enforceExactByteLimit_onSuccessAndErrorStreams() throws Exception {
        for (final boolean errorStream : new boolean[]{false, true}) {
            for (final int size : new int[]{MAX_BODY_BYTES - 1, MAX_BODY_BYTES, MAX_BODY_BYTES + 1}) {
                final byte[] body = new byte[size];
                Arrays.fill(body, (byte) 'a');
                final ByteArrayInputStream input = Mockito.spy(new ByteArrayInputStream(body));
                final HttpURLConnection connection = connection(errorStream ? 400 : 200, body, errorStream);
                if (errorStream) {
                    Mockito.when(connection.getErrorStream()).thenReturn(input);
                } else {
                    Mockito.when(connection.getInputStream()).thenReturn(input);
                }
                HttpUrlConnectionFactory.addMockedConnection(connection);
                if (size <= MAX_BODY_BYTES) {
                    assertEquals(size, nativeClient().post(url(), REQUEST_HEADERS, REQUEST_BODY).getBody().length());
                } else {
                    expectIoFailure(nativeClient());
                }
                Mockito.verify(input).close();
            }
        }
    }

    @Test
    public void nativeOptions_stopReadingAtLimitPlusOneByte() throws Exception {
        final byte[] body = new byte[MAX_BODY_BYTES * 2];
        final ByteArrayInputStream input = new ByteArrayInputStream(body);
        final HttpURLConnection connection = connection(200, body, false);
        Mockito.when(connection.getInputStream()).thenReturn(input);
        HttpUrlConnectionFactory.addMockedConnection(connection);
        expectIoFailure(nativeClient());
        assertEquals(MAX_BODY_BYTES - 1, input.available());
    }

    @Test
    public void nativeOptions_rejectMalformedUtf8_onSuccessAndErrorStreams() throws Exception {
        for (final boolean errorStream : new boolean[]{false, true}) {
            HttpUrlConnectionFactory.addMockedConnection(connection(errorStream ? 400 : 200,
                    new byte[]{(byte) 0xc3, (byte) 0x28}, errorStream));
            final ClientException error = expectIoFailure(nativeClient());
            assertNull(error.getCause());
        }
    }

    @Test
    public void nativeOptions_countUtf8BytesNotCharacters_andAcceptSplitMultibyteCharacters() throws Exception {
        final byte[] body = "\u20ac".getBytes(StandardCharsets.UTF_8);
        HttpUrlConnectionFactory.addMockedConnection(connection(200, body, false));
        final UrlConnectionHttpClient tooSmall = UrlConnectionHttpClient.builder()
                .maxResponseBodyBytes(2).strictUtf8Decoding(true).redactIOExceptionDetails(true).build();
        expectIoFailure(tooSmall);

        HttpUrlConnectionFactory.addMockedConnection(connection(200, body, false));
        final UrlConnectionHttpClient exact = UrlConnectionHttpClient.builder()
                .streamBufferSize(1).maxResponseBodyBytes(3).strictUtf8Decoding(true).build();
        assertEquals("\u20ac", exact.post(url(), REQUEST_HEADERS, REQUEST_BODY).getBody());
    }

    @Test
    public void maxResponseBodyBytes_rejectsNonPositiveLimits() {
        for (final int size : new int[]{0, -1}) {
            try {
                UrlConnectionHttpClient.builder().maxResponseBodyBytes(size).build();
                fail("A byte limit must be positive.");
            } catch (final IllegalArgumentException expected) {
                assertEquals("Maximum response body size must be positive.", expected.getMessage());
            }
        }
    }

    @Test
    public void legacyConstructor_preservesPublicDescriptorAndAllTransportDefaults() throws Exception {
        assertTrue(Modifier.isPublic(UrlConnectionHttpClient.class.getConstructor(
                IRetryPolicy.class, int.class, int.class, int.class,
                Supplier.class, Supplier.class, SSLSocketFactoryWrapper.class).getModifiers()));
        for (final UrlConnectionHttpClient client : new UrlConnectionHttpClient[]{
                legacyClient(), UrlConnectionHttpClient.builder().build()}) {
            final byte[] body = new byte[MAX_BODY_BYTES + 1];
            Arrays.fill(body, (byte) 'a');
            body[0] = (byte) 0xc3;
            final HttpURLConnection connection = connection(200, body, false);
            HttpUrlConnectionFactory.addMockedConnection(connection);

            final String result = client.post(url(), REQUEST_HEADERS, REQUEST_BODY).getBody();
            assertEquals(body.length, result.length());
            assertTrue(result.contains("\ufffd"));
            Mockito.verify(connection).setInstanceFollowRedirects(true);
            Mockito.verify(connection).setUseCaches(true);
            Mockito.verify(connection, Mockito.never()).setFixedLengthStreamingMode(Mockito.anyInt());

            final IOException original = new IOException(SENTINEL);
            final HttpURLConnection failing = connection(200, new byte[0], false);
            Mockito.when(failing.getOutputStream()).thenThrow(original);
            HttpUrlConnectionFactory.addMockedConnection(failing);
            final ClientException error = expectIoFailure(client);
            assertSame(original, error.getCause());
            assertTrue(error.getMessage().contains(SENTINEL));
        }
    }

    @Test
    public void redactIOExceptionDetails_alone_preservesPrimaryAndClosesWithoutLoggingDetails() throws Exception {
        final SocketTimeoutException primary = new SocketTimeoutException(SENTINEL);
        final IOException closing = new IOException(SENTINEL + "-close");
        final InputStream input = Mockito.mock(InputStream.class);
        Mockito.when(input.read(Mockito.any(byte[].class), Mockito.anyInt(), Mockito.anyInt()))
                .thenThrow(primary);
        Mockito.doThrow(closing).when(input).close();
        final HttpURLConnection connection = connection(200, new byte[0], false);
        Mockito.when(connection.getInputStream()).thenReturn(input);
        HttpUrlConnectionFactory.addMockedConnection(connection);

        final ClientException error = expectIoFailure(UrlConnectionHttpClient.builder()
                .redactIOExceptionDetails(true).build());
        assertTrue(ConnectionError.CONNECTION_TIMEOUT.compare(error));
        assertNull(error.getCause());
        assertEquals(0, error.getSuppressed().length);
        assertSame(closing, primary.getSuppressed()[0]);
        Mockito.verify(input).close();
    }

    @Test
    public void nativeOptions_outputFailure_preservesPrimaryAndDoesNotOpenResponseOrRetry() throws Exception {
        final SocketTimeoutException primary = new SocketTimeoutException(SENTINEL);
        final IOException closing = new IOException(SENTINEL + "-close");
        final HttpURLConnection connection = connection(200, new byte[0], false);
        final OutputStream output = connection.getOutputStream();
        Mockito.doThrow(primary).when(output).write(REQUEST_BODY);
        Mockito.doThrow(closing).when(output).close();
        Mockito.clearInvocations(connection);
        HttpUrlConnectionFactory.addMockedConnection(connection);
        HttpUrlConnectionFactory.addMockedConnection(Mockito.mock(HttpURLConnection.class));

        final ClientException error = expectIoFailure(nativeClient());
        assertTrue(ConnectionError.CONNECTION_TIMEOUT.compare(error));
        assertNull(error.getCause());
        assertSame(closing, primary.getSuppressed()[0]);
        Mockito.verify(connection, Mockito.never()).getInputStream();
        Mockito.verify(output).close();
        assertEquals(1, HttpUrlConnectionFactory.getMockedConnectionCountInQueue());
    }

    @Test
    public void nativeOptions_soleCloseFailure_isAnIoFailureNotSuccess() throws Exception {
        for (final boolean closeOutput : new boolean[]{false, true}) {
            final HttpURLConnection connection = connection(200, new byte[0], false);
            if (closeOutput) {
                final OutputStream output = connection.getOutputStream();
                Mockito.doThrow(new IOException(SENTINEL)).when(output).close();
            } else {
                final InputStream input = Mockito.spy(new ByteArrayInputStream(new byte[0]));
                Mockito.doThrow(new IOException(SENTINEL)).when(input).close();
                Mockito.when(connection.getInputStream()).thenReturn(input);
            }
            HttpUrlConnectionFactory.addMockedConnection(connection);
            final ClientException error = expectIoFailure(nativeClient());
            assertNull(error.getCause());
            assertFalse(error.getMessage().contains(SENTINEL));
        }
    }

    @Test
    public void nativeOptions_sameReadAndCloseException_doesNotReplacePrimary() throws Exception {
        final SocketTimeoutException primary = new SocketTimeoutException(SENTINEL);
        final InputStream input = Mockito.mock(InputStream.class);
        Mockito.when(input.read(Mockito.any(byte[].class), Mockito.anyInt(), Mockito.anyInt())).thenThrow(primary);
        Mockito.doThrow(primary).when(input).close();
        final HttpURLConnection connection = connection(200, new byte[0], false);
        Mockito.when(connection.getInputStream()).thenReturn(input);
        HttpUrlConnectionFactory.addMockedConnection(connection);
        assertTrue(ConnectionError.CONNECTION_TIMEOUT.compare(expectIoFailure(nativeClient())));
        assertEquals(0, primary.getSuppressed().length);
    }

    @Test
    public void setupFailure_redactsOnlyWhenRequested_withoutOpeningStreamsOrRetrying() throws Exception {
        for (final boolean redact : new boolean[]{false, true}) {
            expectSetupFailure(redact);
        }
    }

    @Test
    public void outputFailure_preservesCancellationMappingAndRedaction_withoutRetryingOrRetainingCallback()
            throws Exception {
        for (final boolean redact : new boolean[]{false, true}) {
            for (final boolean cancelDuringIo : new boolean[]{false, true}) {
                expectOutputFailure(redact, cancelDuringIo);
            }
        }
    }

    @Test
    public void redaction_happensBeforeErrorEventConstruction_andNormalObserverDelivery() throws Exception {
        final Field singleton = Telemetry.class.getDeclaredField("sTelemetryInstance");
        singleton.setAccessible(true);
        final Object previousTelemetry = singleton.get(null);
        final String previousCorrelation = DiagnosticContext.INSTANCE.getRequestContext()
                .get(DiagnosticContext.CORRELATION_ID);
        final String correlation = UUID.randomUUID().toString();
        final String loggerId = "native-http-privacy-" + correlation;
        final String fenceTag = "native-http-privacy-fence";
        final List<String> logs = new CopyOnWriteArrayList<>();
        final CountDownLatch drained = new CountDownLatch(1);
        final Logger.LogLevel previousLevel = Logger.getLogLevel();
        final boolean previousPii = Logger.isAllowPii();
        final List<Map<String, String>> delivered = new ArrayList<>();
        final ITelemetryDefaultObserver observer = delivered::addAll;
        try {
            singleton.set(null, null);
            final TelemetryConfiguration configuration = new TelemetryConfiguration();
            configuration.setPiiEnabled(true);
            configuration.setDebugEnabled(true);
            final AbstractTelemetryContext context = Mockito.mock(AbstractTelemetryContext.class);
            Mockito.when(context.getProperties()).thenReturn(new ConcurrentHashMap<String, String>());
            new Telemetry.Builder().withTelemetryContext(context)
                    .defaultConfiguration(configuration).isDebugging(false).build();
            DiagnosticContext.INSTANCE.getRequestContext().put(DiagnosticContext.CORRELATION_ID, correlation);
            Telemetry.getInstance().addObserver(observer);
            Logger.setLogLevel(Logger.LogLevel.INFO);
            Logger.setAllowPii(true);
            assertTrue(Logger.setLogger(loggerId, (tag, level, message, containsPii) -> {
                if (fenceTag.equals(tag)) {
                    drained.countDown();
                } else {
                    logs.add(message);
                }
            }));

            final IOException primary = new IOException(SENTINEL
                    + " url=https://example.test/private jwt=synthetic.jwt.proof"
                    + " account=synthetic@example.test header=secret error_description=secret");
            final InputStream input = Mockito.mock(InputStream.class);
            Mockito.when(input.read(Mockito.any(byte[].class), Mockito.anyInt(), Mockito.anyInt()))
                    .thenThrow(primary);
            Mockito.doThrow(new IOException(SENTINEL + "-close")).when(input).close();
            final HttpURLConnection connection = connection(200, new byte[0], false);
            Mockito.when(connection.getInputStream()).thenReturn(input);
            HttpUrlConnectionFactory.addMockedConnection(connection);
            assertNull(expectIoFailure(nativeClient()).getCause());
            assertNull(expectSetupFailure(true).getCause());
            assertNull(expectOutputFailure(true, true).getCause());

            final List<Map<String, String>> atConstruction = Telemetry.getInstance().getMap(correlation);
            assertTrue(atConstruction.stream().anyMatch(event -> ClientException.IO_ERROR.equals(
                    event.get(TelemetryEventStrings.Key.ERROR_CODE))));
            assertTrue(atConstruction.stream().anyMatch(event -> ClientException.TIMED_OUT.equals(
                    event.get(TelemetryEventStrings.Key.ERROR_CODE))));
            assertFalse(atConstruction.toString().contains(SENTINEL));
            assertFalse(atConstruction.toString().contains("synthetic.jwt.proof"));
            Telemetry.getInstance().flush(correlation);
            assertTrue(delivered.stream().anyMatch(event -> ClientException.IO_ERROR.equals(
                    event.get(TelemetryEventStrings.Key.ERROR_CODE))));
            assertTrue(delivered.stream().anyMatch(event -> ClientException.TIMED_OUT.equals(
                    event.get(TelemetryEventStrings.Key.ERROR_CODE))));
            assertFalse(delivered.toString().contains(SENTINEL));
            assertFalse(delivered.toString().contains("synthetic@example.test"));
            Logger.info(fenceTag, "Privacy test log fence.");
            assertTrue(drained.await(30, TimeUnit.SECONDS));
            assertFalse(logs.toString().contains(SENTINEL));
        } finally {
            Logger.setLogger(loggerId, null);
            Logger.setLogLevel(previousLevel);
            Logger.setAllowPii(previousPii);
            Telemetry.getInstance().removeObserver(observer);
            singleton.set(null, previousTelemetry);
            if (previousCorrelation == null) {
                DiagnosticContext.INSTANCE.getRequestContext().remove(DiagnosticContext.CORRELATION_ID);
            } else {
                DiagnosticContext.INSTANCE.getRequestContext()
                        .put(DiagnosticContext.CORRELATION_ID, previousCorrelation);
            }
        }
    }

    private static ClientException expectSetupFailure(final boolean redact) throws Exception {
        final ProtocolException original = new ProtocolException(SENTINEL);
        final HttpURLConnection connection = connection(200, new byte[0], false);
        Mockito.doThrow(original).when(connection).setRequestMethod("POST");
        Mockito.clearInvocations(connection);
        HttpUrlConnectionFactory.addMockedConnection(connection);
        HttpUrlConnectionFactory.addMockedConnection(Mockito.mock(HttpURLConnection.class));
        try {
            final ClientException error = expectIoFailure(redact ? nativeClient() : legacyClient());
            assertFailureDetails(error, original, redact);
            assertTrue(ConnectionError.UNEXPECTED_EXCEPTION.compare(error));
            Mockito.verify(connection, Mockito.never()).getOutputStream();
            Mockito.verify(connection, Mockito.never()).getInputStream();
            assertEquals(1, HttpUrlConnectionFactory.getMockedConnectionCountInQueue());
            return error;
        } finally {
            HttpUrlConnectionFactory.clearMockedConnectionQueue();
        }
    }

    private static ClientException expectOutputFailure(final boolean redact,
                                                       final boolean cancelDuringIo) throws Exception {
        final IOException original = new IOException(SENTINEL);
        final CancellationSignal previousSignal = CancellationSignal.getCurrentThreadSignal();
        final CancellationSignal signal = new CancellationSignal();
        final HttpURLConnection connection = connection(200, new byte[0], false);
        final OutputStream output = connection.getOutputStream();
        Mockito.doAnswer(invocation -> {
            if (cancelDuringIo) {
                signal.cancel();
            }
            throw original;
        }).when(output).write(REQUEST_BODY);
        Mockito.clearInvocations(connection);
        HttpUrlConnectionFactory.addMockedConnection(connection);
        HttpUrlConnectionFactory.addMockedConnection(Mockito.mock(HttpURLConnection.class));
        CancellationSignal.setForCurrentThread(signal);
        try {
            final ClientException error = expectFailure(redact ? nativeClient() : legacyClient(),
                    cancelDuringIo ? ClientException.TIMED_OUT : ClientException.IO_ERROR);
            assertFailureDetails(error, original, redact);
            Mockito.verify(output).close();
            Mockito.verify(connection, Mockito.never()).getInputStream();
            assertEquals(1, HttpUrlConnectionFactory.getMockedConnectionCountInQueue());
            signal.cancel();
            Mockito.verify(connection, Mockito.times(cancelDuringIo ? 1 : 0)).disconnect();
            return error;
        } finally {
            if (previousSignal == null) {
                CancellationSignal.clearCurrentThread();
            } else {
                CancellationSignal.setForCurrentThread(previousSignal);
            }
            HttpUrlConnectionFactory.clearMockedConnectionQueue();
        }
    }

    private static void assertFailureDetails(final ClientException error,
                                             final IOException original,
                                             final boolean redact) {
        if (redact) {
            assertNull(error.getCause());
            assertEquals(0, error.getSuppressed().length);
            assertFalse(error.getMessage().contains(SENTINEL));
        } else {
            assertSame(original, error.getCause());
        }
    }

    private static UrlConnectionHttpClient nativeClient() {
        return UrlConnectionHttpClient.builder()
                .retryPolicy(new NoRetryPolicy())
                .followRedirects(false)
                .useFixedLengthStreamingMode(true)
                .useCaches(false)
                .maxResponseBodyBytes(MAX_BODY_BYTES)
                .strictUtf8Decoding(true)
                .redactIOExceptionDetails(true)
                .build();
    }

    private static UrlConnectionHttpClient legacyClient() {
        return new UrlConnectionHttpClient(new NoRetryPolicy(), 1024, 30000, 30000,
                null, null, Mockito.mock(SSLSocketFactoryWrapper.class));
    }

    private static URL url() throws Exception {
        return new URL("https://example.test/tenant/oauth2/token");
    }

    private static HttpURLConnection connection(final int status, final byte[] body,
                                                 final boolean errorStream) throws Exception {
        final HttpURLConnection connection = Mockito.mock(HttpsURLConnection.class);
        final OutputStream output = Mockito.mock(OutputStream.class);
        Mockito.when(connection.getOutputStream()).thenReturn(output);
        Mockito.when(connection.getResponseCode()).thenReturn(status);
        Mockito.when(connection.getHeaderFields()).thenReturn(Collections.emptyMap());
        if (errorStream) {
            Mockito.when(connection.getInputStream()).thenThrow(new IOException("HTTP response error."));
            Mockito.when(connection.getErrorStream()).thenReturn(new ByteArrayInputStream(body));
        } else {
            Mockito.when(connection.getInputStream()).thenReturn(new ByteArrayInputStream(body));
        }
        return connection;
    }

    private static ClientException expectIoFailure(final UrlConnectionHttpClient client) throws Exception {
        return expectFailure(client, ClientException.IO_ERROR);
    }

    private static ClientException expectFailure(final UrlConnectionHttpClient client,
                                                 final String expectedErrorCode) throws Exception {
        try {
            client.post(url(), REQUEST_HEADERS, REQUEST_BODY);
            fail("Expected a network failure.");
            return null;
        } catch (final ClientException e) {
            assertEquals(expectedErrorCode, e.getErrorCode());
            return e;
        }
    }
}
