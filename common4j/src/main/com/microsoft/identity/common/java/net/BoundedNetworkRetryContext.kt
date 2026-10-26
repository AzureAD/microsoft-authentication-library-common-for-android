// Copyright (c) Microsoft Corporation.
// All rights reserved.
// Licensed under the MIT License.

package com.microsoft.identity.common.java.net

import com.microsoft.identity.common.java.exception.ClientException
import com.microsoft.identity.common.java.exception.ConnectionError
import com.microsoft.identity.common.java.logging.Logger
import com.microsoft.identity.common.java.opentelemetry.AttributeName
import com.microsoft.identity.common.java.opentelemetry.OtelContextExtension
import com.microsoft.identity.common.java.opentelemetry.SpanExtension
import io.opentelemetry.api.trace.Span
import io.opentelemetry.context.ContextKey
import io.opentelemetry.context.Scope
import java.net.UnknownHostException
import java.util.concurrent.Callable
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * Opt-in, request-scoped DNS retry budget for the default HTTP client.
 *
 * The existing retry policy runs first. An extra attempt is allowed only if that policy made
 * exactly one attempt, so a single HTTP request still makes at most two transport attempts.
 * Context propagation shares the additional-attempt budget across the command's worker threads.
 */
class BoundedNetworkRetryContext internal constructor(
    private val enabled: Boolean,
    private val span: Span,
    timeoutMs: Long,
    private val nanoTime: () -> Long = System::nanoTime,
    private val waitForRetry: (Long) -> Unit = TimeUnit.MILLISECONDS::sleep
) : AutoCloseable {
    private val startedAt = nanoTime()
    private val budgetNanos = TimeUnit.MILLISECONDS.toNanos(timeoutMs.coerceAtLeast(0))
    private val closed = AtomicBoolean(false)
    private val retryReserved = AtomicBoolean(false)
    private val retried = AtomicBoolean(false)
    private val retrySucceeded = AtomicReference<Boolean?>(null)
    private var scope: Scope? = null

    private fun remainingMs(): Long =
        TimeUnit.NANOSECONDS.toMillis(budgetNanos - (nanoTime() - startedAt))

    private fun isStopped(): Boolean =
        closed.get() || CancellationSignal.getCurrentThreadSignal()?.isCancelled == true ||
            Thread.currentThread().isInterrupted || remainingMs() <= 0

    private fun isEligible(exception: ClientException): Boolean =
        exception.errorCode == ClientException.IO_ERROR &&
            ConnectionError.NO_NETWORK.compare(exception) &&
            exception.cause is UnknownHostException

    internal fun attempt(
        legacyPolicy: IRetryPolicy<HttpResponse>,
        supplier: Callable<HttpResponse>
    ): HttpResponse {
        if (!enabled) {
            return legacyPolicy.attempt(supplier)
        }
        var callAttempts = 0
        var additionalAttempt = false
        val observed = Callable {
            if (enabled && isStopped()) {
                throw ClientException(ClientException.TIMED_OUT, "Network retry request is no longer active")
            }
            callAttempts++
            if (additionalAttempt) {
                retried.set(true)
            }
            supplier.call()
        }
        try {
            return legacyPolicy.attempt(observed)
        } catch (exception: ClientException) {
            if (!isEligible(exception)) {
                throw exception
            }
            if (!enabled || callAttempts != 1 || isStopped() ||
                remainingMs() < BACKOFF_MS + MIN_ATTEMPT_BUDGET_MS ||
                !retryReserved.compareAndSet(false, true)) {
                throw exception
            }
            // This is the synchronous HTTP policy contract, already running on the command worker.
            try {
                waitForRetry(BACKOFF_MS)
            } catch (interrupted: InterruptedException) {
                Thread.currentThread().interrupt()
                Logger.info(TAG, "Bounded network retry interrupted before the additional attempt")
                throw exception
            }
            if (isStopped() || remainingMs() < MIN_ATTEMPT_BUDGET_MS) {
                throw exception
            }
            additionalAttempt = true
            // Do not nest the legacy policy: its own retry would multiply transport attempts.
            val response = try {
                NoRetryPolicy().attempt(observed)
            } catch (retryException: ClientException) {
                if (retried.get()) {
                    retrySucceeded.set(false)
                }
                throw retryException
            }
            retrySucceeded.set(response.statusCode < 400)
            return response
        }
    }

    /** Stops further retries, restores the caller's context, and records the request summary. */
    override fun close() {
        if (!closed.compareAndSet(false, true) || !enabled) {
            return
        }
        scope?.close()
        val succeeded = retrySucceeded.get()
        span.setAttribute(AttributeName.network_retry_enabled.name, enabled)
        span.setAttribute(AttributeName.network_request_retried.name, retried.get())
        if (succeeded != null) {
            span.setAttribute(AttributeName.network_retry_succeeded.name, succeeded)
        }
    }

    companion object {
        private const val TAG = "BoundedNetworkRetryContext"
        private const val BACKOFF_MS = 1_000L
        private const val MIN_ATTEMPT_BUDGET_MS = 1_000L
        private val KEY by lazy { ContextKey.named<BoundedNetworkRetryContext>("bounded-network-retry") }

        /**
         * Makes the supplied span and a Broker ATS retry scope current until closed.
         * Disabled retries or context setup failures retain the legacy safe span scope.
         */
        @JvmStatic
        fun open(enabled: Boolean, span: Span, timeoutMs: Long): AutoCloseable {
            if (!enabled) {
                return SpanExtension.makeCurrentSpan(span)
            }
            val result = BoundedNetworkRetryContext(enabled, span, timeoutMs)
            try {
                result.scope = requireNotNull(OtelContextExtension.current())
                    .with(KEY, result).with(span).makeCurrent()
                return result
            } catch (exception: Exception) {
                Logger.error(TAG, "Unable to install bounded network retry context", exception)
            } catch (error: AbstractMethodError) {
                Logger.error(TAG, "Unable to install bounded network retry context", error)
            }
            return SpanExtension.makeCurrentSpan(span)
        }

        /** Decorates only the default client's policy, looking up the scope at execution time. */
        @JvmStatic
        fun wrap(legacyPolicy: IRetryPolicy<HttpResponse>): IRetryPolicy<HttpResponse> =
            IRetryPolicy { supplier ->
                val current = OtelContextExtension.current()?.get(KEY)
                if (current == null) legacyPolicy.attempt(supplier) else current.attempt(legacyPolicy, supplier)
            }

        /** Caps configured socket timeouts to the treatment request's remaining budget. */
        @JvmStatic
        fun capTimeoutMs(configuredTimeoutMs: Int): Int {
            val current = OtelContextExtension.current()?.get(KEY)
            if (current == null || !current.enabled) {
                return configuredTimeoutMs
            }
            val remaining = current.remainingMs().coerceIn(1, Int.MAX_VALUE.toLong()).toInt()
            return if (configuredTimeoutMs == 0) remaining else minOf(configuredTimeoutMs, remaining)
        }
    }
}
