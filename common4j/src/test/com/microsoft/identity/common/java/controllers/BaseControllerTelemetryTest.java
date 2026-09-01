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
package com.microsoft.identity.common.java.controllers;

import com.microsoft.identity.common.java.broker.telemetry.EventCollector;
import com.microsoft.identity.common.java.broker.telemetry.EventTag;
import com.microsoft.identity.common.java.broker.telemetry.ExecutionEvent;
import com.microsoft.identity.common.java.commands.parameters.CommandParameters;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.providers.oauth2.OAuth2Strategy;
import com.microsoft.identity.common.java.providers.oauth2.TokenRequest;
import com.microsoft.identity.common.java.providers.oauth2.TokenResponse;
import com.microsoft.identity.common.java.providers.oauth2.TokenResult;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Tests the telemetry instrumentation that {@link BaseController#executeTokenRequest} wraps around
 * the token request.
 * <p>
 * The behaviour under test is ordering, not merely presence. {@code BrokerNetworkCallEnd} is emitted
 * from a {@code finally} block specifically so that a network call which throws still produces a
 * closing event; without it, a failed call is indistinguishable from one that never returned. These
 * tests pin that guarantee, and pin that instrumentation does not swallow the original exception.
 * <p>
 * {@code executeTokenRequest} is exercised directly rather than through
 * {@link BaseController#performSilentTokenRequest}, which would first require platform components,
 * a network-availability check and the {@code Authority.getKnownAuthorityResult} gate — none of
 * which have any bearing on tag ordering.
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class BaseControllerTelemetryTest {

    private static final String BROKER_NAME = "test-broker";
    private static final String BROKER_VERSION = "1.0";
    private static final String AUTH_OUTCOME = "test";
    private static final String CORRELATION_ID = "test-correlation-id";

    private BaseController mController;
    private OAuth2Strategy mStrategy;
    private TokenRequest mTokenRequest;
    private EventCollector mEventCollector;
    private CommandParameters mParameters;

    @Before
    public void setUp() {
        // CALLS_REAL_METHODS lets the real executeTokenRequest body run without having to stub
        // the twelve abstract members BaseController declares, none of which it touches.
        mController = Mockito.mock(BaseController.class, Mockito.CALLS_REAL_METHODS);
        mStrategy = Mockito.mock(OAuth2Strategy.class);
        mTokenRequest = Mockito.mock(TokenRequest.class);
        mEventCollector = new EventCollector(CORRELATION_ID);
        mParameters = Mockito.mock(CommandParameters.class);
        Mockito.when(mParameters.getEventCollector()).thenReturn(mEventCollector);
    }

    /**
     * @return the tags recorded so far, in the order they were added.
     */
    private List<EventTag> recordedTags() {
        final List<ExecutionEvent> events = mEventCollector
                .toBrokerIpcTelemetry(BROKER_NAME, BROKER_VERSION, AUTH_OUTCOME)
                .getPerformanceRecord()
                .getExecutionFlow();

        final List<EventTag> tags = new ArrayList<>();
        for (final ExecutionEvent event : events) {
            tags.add(event.getTag());
        }
        return tags;
    }

    /**
     * Builds a real {@link TokenResult} rather than a mock: {@code getSuccess()} is derived from
     * whether a success response is present, so constructing one exercises the real contract and
     * keeps this helper free of stubbing (nested stubbing inside a {@code when(...)} argument is
     * what Mockito's UnfinishedStubbingException guards against).
     */
    private TokenResult tokenResultWithSuccess(final boolean success) {
        return success
                ? new TokenResult(Mockito.mock(TokenResponse.class))
                : new TokenResult();
    }

    @Test
    public void executeTokenRequest_whenCallSucceeds_recordsTagsInOrder() throws Exception {
        final TokenResult expected = tokenResultWithSuccess(true);
        Mockito.when(mStrategy.requestToken(Mockito.any(TokenRequest.class))).thenReturn(expected);

        final TokenResult actual = mController.executeTokenRequest(mStrategy, mTokenRequest, mParameters);

        Assert.assertSame(expected, actual);
        Assert.assertEquals(
                Arrays.asList(
                        EventTag.BrokerNetworkCallStart,
                        EventTag.CommonHttpRequestExecute,
                        EventTag.CommonHttpResponseReceived,
                        EventTag.BrokerNetworkCallEnd,
                        EventTag.BrokerTokenAcquired
                ),
                recordedTags()
        );
    }

    /**
     * A transport-level success carrying an unsuccessful token response still closes the network
     * call, then records the failure. The response was received, so CommonHttpResponseReceived is
     * expected here but not on the throwing path below.
     */
    @Test
    public void executeTokenRequest_whenTokenResultUnsuccessful_recordsNetworkCallFailed() throws Exception {
        final TokenResult unsuccessful = tokenResultWithSuccess(false);
        Mockito.when(mStrategy.requestToken(Mockito.any(TokenRequest.class))).thenReturn(unsuccessful);

        mController.executeTokenRequest(mStrategy, mTokenRequest, mParameters);

        Assert.assertEquals(
                Arrays.asList(
                        EventTag.BrokerNetworkCallStart,
                        EventTag.CommonHttpRequestExecute,
                        EventTag.CommonHttpResponseReceived,
                        EventTag.BrokerNetworkCallEnd,
                        EventTag.BrokerNetworkCallFailed
                ),
                recordedTags()
        );
    }

    /**
     * The point of the {@code finally}: a throwing call must still close the network call, and must
     * do so <em>before</em> the failure is recorded, so the event stream reads as
     * start -> end -> failed rather than leaving the call open forever.
     */
    @Test
    public void executeTokenRequest_whenRequestThrowsIOException_recordsEndBeforeFailedAndPropagates() throws Exception {
        final IOException expected = new IOException("network down");
        Mockito.when(mStrategy.requestToken(Mockito.any(TokenRequest.class))).thenThrow(expected);

        try {
            mController.executeTokenRequest(mStrategy, mTokenRequest, mParameters);
            Assert.fail("Expected the original IOException to propagate");
        } catch (final IOException actual) {
            Assert.assertSame("Instrumentation must not replace the original exception", expected, actual);
        }

        Assert.assertEquals(
                Arrays.asList(
                        EventTag.BrokerNetworkCallStart,
                        EventTag.CommonHttpRequestExecute,
                        EventTag.BrokerNetworkCallEnd,
                        EventTag.BrokerNetworkCallFailed
                ),
                recordedTags()
        );
    }

    /**
     * RuntimeException is caught by the same clause as the checked exceptions and must also
     * propagate unchanged rather than being wrapped by the instrumentation.
     */
    @Test
    public void executeTokenRequest_whenRequestThrowsRuntimeException_propagatesUnchanged() throws Exception {
        final RuntimeException expected = new IllegalStateException("boom");
        Mockito.when(mStrategy.requestToken(Mockito.any(TokenRequest.class))).thenThrow(expected);

        try {
            mController.executeTokenRequest(mStrategy, mTokenRequest, mParameters);
            Assert.fail("Expected the original RuntimeException to propagate");
        } catch (final RuntimeException actual) {
            Assert.assertSame(expected, actual);
        }

        Assert.assertEquals(
                Arrays.asList(
                        EventTag.BrokerNetworkCallStart,
                        EventTag.CommonHttpRequestExecute,
                        EventTag.BrokerNetworkCallEnd,
                        EventTag.BrokerNetworkCallFailed
                ),
                recordedTags()
        );
    }

    /**
     * Collection is opt-in: outside a broker flow no collector is attached, and the token request
     * must behave exactly as it did before the instrumentation was added.
     */
    @Test
    public void executeTokenRequest_whenNoEventCollector_stillReturnsResult() throws Exception {
        Mockito.when(mParameters.getEventCollector()).thenReturn(null);
        final TokenResult expected = tokenResultWithSuccess(true);
        Mockito.when(mStrategy.requestToken(Mockito.any(TokenRequest.class))).thenReturn(expected);

        Assert.assertSame(expected, mController.executeTokenRequest(mStrategy, mTokenRequest, mParameters));
    }

    /**
     * A null collector must not turn a failed network call into a different failure mode; the
     * original exception still propagates.
     */
    @Test
    public void executeTokenRequest_whenNoEventCollectorAndRequestThrows_propagatesOriginal() throws Exception {
        Mockito.when(mParameters.getEventCollector()).thenReturn(null);
        final ClientException expected = new ClientException("test_error", "test message");
        Mockito.when(mStrategy.requestToken(Mockito.any(TokenRequest.class))).thenThrow(expected);

        try {
            mController.executeTokenRequest(mStrategy, mTokenRequest, mParameters);
            Assert.fail("Expected the original ClientException to propagate");
        } catch (final ClientException actual) {
            Assert.assertSame(expected, actual);
        }
    }
}
