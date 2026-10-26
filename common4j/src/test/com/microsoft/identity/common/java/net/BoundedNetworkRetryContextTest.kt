// Copyright (c) Microsoft Corporation.
// All rights reserved.
// Licensed under the MIT License.

package com.microsoft.identity.common.java.net

import com.microsoft.identity.common.java.exception.ClientException
import com.microsoft.identity.common.java.exception.ConnectionError
import com.microsoft.identity.common.java.opentelemetry.AttributeName
import com.microsoft.identity.common.java.opentelemetry.OtelContextExtension
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.context.Context
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.CALLS_REAL_METHODS
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.EOFException
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import javax.net.ssl.SSLException
import javax.net.ssl.SSLHandshakeException
import javax.net.ssl.SSLPeerUnverifiedException

class BoundedNetworkRetryContextTest {
    private val span = mock(Span::class.java, CALLS_REAL_METHODS)
    private val success = HttpResponse(200, "{}", emptyMap())
    private val dnsFailure = ConnectionError.getClientException(UnknownHostException())
    private var nowMs = 0L
    private var waitedMs = 0L

    @After
    fun tearDown() {
        CancellationSignal.clearCurrentThread()
        HttpUrlConnectionFactory.clearMockedConnectionQueue()
        Thread.interrupted()
    }

    private fun context(
        enabled: Boolean = true,
        timeoutMs: Long = 30_000,
        wait: (Long) -> Unit = { waitedMs += it; nowMs += it }
    ) = BoundedNetworkRetryContext(
        enabled, span, timeoutMs, { TimeUnit.MILLISECONDS.toNanos(nowMs) }, wait
    )

    private fun verifyRetryTelemetry(retried: Boolean, succeeded: Boolean? = null) {
        verify(span).setAttribute(AttributeName.network_request_retried.name, retried)
        if (succeeded == null) {
            verify(span, never()).setAttribute(AttributeName.network_retry_succeeded.name, true)
            verify(span, never()).setAttribute(AttributeName.network_retry_succeeded.name, false)
        } else {
            verify(span).setAttribute(AttributeName.network_retry_succeeded.name, succeeded)
        }
    }

    @Test
    fun flightOff_preservesFailureWithoutRetryTelemetry() {
        val scope = context(enabled = false)
        var calls = 0
        val thrown = assertThrows(ClientException::class.java) {
            scope.attempt(NoRetryPolicy(), Callable { calls++; throw dnsFailure })
        }
        scope.close()

        assertSame(dnsFailure, thrown)
        assertEquals(1, calls)
        assertEquals(0, waitedMs)
        verifyNoRetryTelemetry()
    }

    private fun verifyNoRetryTelemetry() {
        for (attribute in listOf(
            AttributeName.network_retry_enabled,
            AttributeName.network_request_retried,
            AttributeName.network_retry_succeeded
        )) {
            verify(span, never()).setAttribute(attribute.name, true)
            verify(span, never()).setAttribute(attribute.name, false)
        }
    }

    @Test
    fun flightOff_openUsesLegacySpanScopeWithoutRetryTelemetry() {
        val parent = Context.current()
        BoundedNetworkRetryContext.open(false, span, 0).use {
            assertSame(span, Span.current())
            assertEquals(60_000, BoundedNetworkRetryContext.capTimeoutMs(60_000))
            assertSame(dnsFailure, assertThrows(ClientException::class.java) {
                BoundedNetworkRetryContext.wrap(NoRetryPolicy()).attempt(Callable { throw dnsFailure })
            })
        }

        assertSame(parent, Context.current())
        verifyNoRetryTelemetry()
    }

    @Test
    fun flightOff_contextFailureRetainsLegacySafeBehavior() {
        verifyContextSetupFailure(enabled = false, failure = NullPointerException())
    }

    @Test
    fun enabledScope_contextFailureFallsBackWithoutRetrying() {
        verifyContextSetupFailure(enabled = true, failure = NullPointerException())
    }

    @Test
    fun enabledScope_abstractMethodErrorFallsBackWithoutRetrying() {
        verifyContextSetupFailure(enabled = true, failure = AbstractMethodError())
    }

    private fun verifyContextSetupFailure(enabled: Boolean, failure: Throwable) {
        val parent = Context.current()
        doThrow(failure).`when`(span).storeInContext(any(Context::class.java))
        var calls = 0

        BoundedNetworkRetryContext.open(enabled, span, 30_000).use {
            assertSame(parent, Context.current())
            assertEquals(60_000, BoundedNetworkRetryContext.capTimeoutMs(60_000))
            assertSame(dnsFailure, assertThrows(ClientException::class.java) {
                BoundedNetworkRetryContext.wrap(NoRetryPolicy()).attempt(Callable {
                    calls++
                    throw dnsFailure
                })
            })
        }

        assertEquals(1, calls)
        assertSame(parent, Context.current())
        verifyNoRetryTelemetry()
    }

    @Test
    fun firstAttemptSuccess_doesNotWaitOrRetry() {
        val scope = context()
        assertSame(success, scope.attempt(NoRetryPolicy(), Callable { success }))
        scope.close()

        assertEquals(0, waitedMs)
        verifyRetryTelemetry(false)
    }

    @Test
    fun dnsFailure_recoversOnceAndReportsRetrySuccess() {
        val scope = context()
        var calls = 0
        val response = scope.attempt(NoRetryPolicy(), Callable {
            calls++
            if (calls == 1) throw dnsFailure
            success
        })
        scope.close()

        assertSame(success, response)
        assertEquals(2, calls)
        assertEquals(1_000, waitedMs)
        verifyRetryTelemetry(true, true)
        verify(span).setAttribute(AttributeName.network_retry_enabled.name, true)
    }

    @Test
    fun connectFailure_isNotRetried() {
        val scope = context()
        val failure = ConnectionError.getClientException(ConnectException())
        var calls = 0
        assertSame(failure, assertThrows(ClientException::class.java) {
            scope.attempt(NoRetryPolicy(), Callable { calls++; throw failure })
        })
        scope.close()

        assertEquals(1, calls)
        assertEquals(0, waitedMs)
        verifyRetryTelemetry(false)
    }

    @Test
    fun repeatedFailure_exhaustsOneRetryAndPreservesLastException() {
        val scope = context()
        val lastFailure = ConnectionError.getClientException(UnknownHostException())
        var calls = 0
        val thrown = assertThrows(ClientException::class.java) {
            scope.attempt(NoRetryPolicy(), Callable {
                if (++calls == 1) throw dnsFailure
                throw lastFailure
            })
        }
        scope.close()

        assertSame(lastFailure, thrown)
        assertEquals(2, calls)
        verifyRetryTelemetry(true, false)
    }

    @Test
    fun httpRecovery_doesNotImplySuccessfulTokenAcquisition() {
        val scope = context()
        var calls = 0
        scope.attempt(NoRetryPolicy(), Callable {
            if (++calls == 1) throw dnsFailure
            success
        })
        span.setStatus(StatusCode.ERROR)
        scope.close()

        verifyRetryTelemetry(true, true)
        verify(span).setStatus(StatusCode.ERROR)
    }

    @Test
    fun serviceErrorAfterRetry_isNotReportedAsRecoveryOrRetriedAgain() {
        val scope = context()
        val response = HttpResponse(503, "{}", emptyMap())
        var calls = 0
        assertSame(response, scope.attempt(NoRetryPolicy(), Callable {
            if (++calls == 1) throw dnsFailure
            response
        }))
        scope.close()

        assertEquals(2, calls)
        verifyRetryTelemetry(true, false)
    }

    @Test
    fun ineligibleFailures_doNotReceiveNewRetries() {
        val failures = listOf(
            ConnectionError.getClientException(SSLHandshakeException("certificate validation")),
            ConnectionError.getClientException(SSLPeerUnverifiedException("hostname mismatch")),
            ConnectionError.getClientException(SSLException("handshake")),
            ConnectionError.getClientException(SocketException("reset")),
            ConnectionError.getClientException(EOFException()),
            ConnectionError.getClientException(SocketTimeoutException()),
            ConnectionError.getClientException(UnknownHostException()).also {
                it.subErrorCode = ConnectionError.NETWORK_TEMPORARILY_UNAVAILABLE.value
            },
            ClientException(ClientException.TIMED_OUT, "cancelled"),
            ClientException("other", "not an IO error", UnknownHostException())
        )
        for (failure in failures) {
            var calls = 0
            val scope = context()
            assertSame(failure, assertThrows(ClientException::class.java) {
                scope.attempt(NoRetryPolicy(), Callable { calls++; throw failure })
            })
            assertEquals(1, calls)
        }
        assertEquals(0, waitedMs)
    }

    @Test
    fun legacyRetry_isNotMultipliedByNewPolicy() {
        val scope = context()
        val legacyPolicy = IRetryPolicy<HttpResponse> { supplier ->
            supplier.call()
            supplier.call()
        }
        var calls = 0
        assertSame(dnsFailure, assertThrows(ClientException::class.java) {
            scope.attempt(legacyPolicy, Callable {
                if (++calls == 1) return@Callable HttpResponse(503, "{}", emptyMap())
                throw dnsFailure
            })
        })
        scope.close()

        assertEquals(2, calls)
        assertEquals(0, waitedMs)
        verifyRetryTelemetry(false)
    }

    @Test
    fun requestBudget_isSharedAcrossSeparateHttpCalls() {
        val scope = context()
        var calls = 0
        scope.attempt(NoRetryPolicy(), Callable {
            if (++calls == 1) throw dnsFailure
            success
        })
        assertThrows(ClientException::class.java) {
            scope.attempt(NoRetryPolicy(), Callable { calls++; throw dnsFailure })
        }
        scope.close()

        assertEquals(3, calls)
        assertEquals(1_000, waitedMs)
        verifyRetryTelemetry(true, true)
    }

    @Test
    fun insufficientRemainingBudget_skipsRetryWithoutWaiting() {
        val scope = context(timeoutMs = 1_999)
        assertSame(dnsFailure, assertThrows(ClientException::class.java) {
            scope.attempt(NoRetryPolicy(), Callable { throw dnsFailure })
        })
        scope.close()

        assertEquals(0, waitedMs)
        verifyRetryTelemetry(false)
    }

    @Test
    fun deadlineDuringBackoff_doesNotStartAnotherAttempt() {
        val scope = context(wait = { nowMs += 30_000 })
        var calls = 0
        assertSame(dnsFailure, assertThrows(ClientException::class.java) {
            scope.attempt(NoRetryPolicy(), Callable { calls++; throw dnsFailure })
        })
        scope.close()

        assertEquals(1, calls)
        verifyRetryTelemetry(false)
    }

    @Test
    fun cancellationDuringBackoff_doesNotStartAnotherAttempt() {
        val signal = CancellationSignal()
        CancellationSignal.setForCurrentThread(signal)
        val scope = context(wait = { signal.cancel() })
        var calls = 0
        assertSame(dnsFailure, assertThrows(ClientException::class.java) {
            scope.attempt(NoRetryPolicy(), Callable { calls++; throw dnsFailure })
        })
        scope.close()

        assertEquals(1, calls)
        verifyRetryTelemetry(false)
    }

    @Test
    fun interruptedBackoff_restoresInterruptAndPreservesNetworkFailure() {
        val scope = context(wait = { throw InterruptedException() })
        var calls = 0
        assertSame(dnsFailure, assertThrows(ClientException::class.java) {
            scope.attempt(NoRetryPolicy(), Callable { calls++; throw dnsFailure })
        })
        assertTrue(Thread.currentThread().isInterrupted)
        scope.close()

        assertEquals(1, calls)
        verifyRetryTelemetry(false)
    }

    @Test
    fun closingRequestDuringBackoff_preventsLateRetry() {
        lateinit var scope: BoundedNetworkRetryContext
        scope = context(wait = { scope.close() })
        var calls = 0
        assertSame(dnsFailure, assertThrows(ClientException::class.java) {
            scope.attempt(NoRetryPolicy(), Callable { calls++; throw dnsFailure })
        })
        assertEquals(1, calls)
        verifyRetryTelemetry(false)
    }

    @Test
    fun requestEndsWhileRetryIsRunning_doesNotReportUnfinishedRetryAsFailure() {
        val scope = context()
        var calls = 0
        scope.attempt(NoRetryPolicy(), Callable {
            if (++calls == 1) throw dnsFailure
            scope.close()
            success
        })

        verifyRetryTelemetry(true)
    }

    @Test
    fun contextPropagatesToWorker_andRestoresAfterClose() {
        val executor = Executors.newSingleThreadExecutor()
        val policy = BoundedNetworkRetryContext.wrap(NoRetryPolicy())
        var calls = 0
        try {
            BoundedNetworkRetryContext.open(true, span, 30_000).use {
                val runnable = requireNotNull(OtelContextExtension.current()).wrap(Runnable {
                    policy.attempt(Callable {
                        if (++calls == 1) throw dnsFailure
                        success
                    })
                })
                executor.submit(runnable).get(5, TimeUnit.SECONDS)
            }
            assertEquals(2, calls)
            verifyRetryTelemetry(true, true)
            assertSame(success, policy.attempt(Callable { success }))
            assertEquals(60_000, BoundedNetworkRetryContext.capTimeoutMs(60_000))
        } finally {
            executor.shutdownNow()
        }
    }

    @Test
    fun socketTimeouts_areCappedOnlyWithinEnabledScope() {
        assertEquals(30_000, BoundedNetworkRetryContext.capTimeoutMs(30_000))
        BoundedNetworkRetryContext.open(false, span, 5_000).use {
            assertEquals(30_000, BoundedNetworkRetryContext.capTimeoutMs(30_000))
            assertEquals(0, BoundedNetworkRetryContext.capTimeoutMs(0))
        }
        BoundedNetworkRetryContext.open(true, span, 5_000).use {
            assertTrue(BoundedNetworkRetryContext.capTimeoutMs(30_000) in 1..5_000)
            assertTrue(BoundedNetworkRetryContext.capTimeoutMs(0) in 1..5_000)
            assertEquals(10, BoundedNetworkRetryContext.capTimeoutMs(10))
        }
        assertEquals(30_000, BoundedNetworkRetryContext.capTimeoutMs(30_000))
    }

    @Test
    fun separateRequests_haveIndependentRetryBudgets() {
        repeat(2) {
            val scope = context()
            var calls = 0
            scope.attempt(NoRetryPolicy(), Callable {
                if (++calls == 1) throw dnsFailure
                success
            })
            assertEquals(2, calls)
        }
        assertEquals(2_000, waitedMs)
    }

    @Test
    fun concurrentHttpCalls_shareOneAdditionalAttempt() {
        val executor = Executors.newFixedThreadPool(2)
        val firstAttempts = CountDownLatch(2)
        val calls = AtomicInteger()
        val scope = BoundedNetworkRetryContext(true, span, 30_000, waitForRetry = {})
        try {
            val futures = (1..2).map {
                executor.submit(Callable {
                    var localCalls = 0
                    try {
                        scope.attempt(NoRetryPolicy(), Callable {
                            calls.incrementAndGet()
                            if (++localCalls == 1) {
                                firstAttempts.countDown()
                                assertTrue(firstAttempts.await(5, TimeUnit.SECONDS))
                                throw dnsFailure
                            }
                            success
                        })
                        true
                    } catch (exception: ClientException) {
                        assertSame(dnsFailure, exception)
                        false
                    }
                })
            }
            assertEquals(1, futures.count { it.get(5, TimeUnit.SECONDS) })
            assertEquals(3, calls.get())
            scope.close()
            verifyRetryTelemetry(true, true)
        } finally {
            executor.shutdownNow()
        }
    }

    @Test
    fun defaultClient_flightOnRetriesActualHttpSend() {
        queueConnection(UnknownHostException())
        queueConnection()
        BoundedNetworkRetryContext.open(true, span, 30_000).use {
            assertEquals(200, UrlConnectionHttpClient.getDefaultInstance().post(
                URL("http://localhost.invalid/token"), emptyMap(), byteArrayOf(1)
            ).statusCode)
        }
        assertEquals(0, HttpUrlConnectionFactory.getMockedConnectionCountInQueue())
        verifyRetryTelemetry(true, true)
    }

    @Test
    fun defaultClient_flightOffDoesNotRetryDns() {
        queueConnection(UnknownHostException())
        queueConnection()
        BoundedNetworkRetryContext.open(false, span, 30_000).use {
            assertThrows(ClientException::class.java) {
                UrlConnectionHttpClient.getDefaultInstance().post(
                    URL("http://localhost.invalid/token"), emptyMap(), byteArrayOf(1)
                )
            }
        }
        assertEquals(1, HttpUrlConnectionFactory.getMockedConnectionCountInQueue())
        verifyNoRetryTelemetry()
    }

    @Test
    fun customClientPolicy_isNotReplacedByDefaultClientExperiment() {
        queueConnection(UnknownHostException())
        queueConnection()
        BoundedNetworkRetryContext.open(true, span, 30_000).use {
            assertThrows(ClientException::class.java) {
                UrlConnectionHttpClient.builder().build().post(
                    URL("http://localhost.invalid/token"), emptyMap(), byteArrayOf(1)
                )
            }
        }
        assertEquals(1, HttpUrlConnectionFactory.getMockedConnectionCountInQueue())
        verifyRetryTelemetry(false)
    }

    private fun queueConnection(failure: UnknownHostException? = null) {
        val connection = mock(HttpURLConnection::class.java)
        if (failure == null) {
            `when`(connection.outputStream).thenReturn(ByteArrayOutputStream())
            `when`(connection.inputStream).thenReturn(ByteArrayInputStream("{}".toByteArray()))
            `when`(connection.responseCode).thenReturn(200)
            `when`(connection.headerFields).thenReturn(emptyMap())
        } else {
            `when`(connection.outputStream).thenThrow(failure)
        }
        HttpUrlConnectionFactory.addMockedConnection(connection)
    }
}
