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
package com.microsoft.identity.common.java.controllers

import com.microsoft.identity.common.java.result.FinalizableResultFuture
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * Tests simulating the stale entry eviction logic in [CommandDispatcher].
 *
 * These tests validate the ownership-aware map cleanup semantics without
 * requiring the full CommandDispatcher and its heavyweight dependencies.
 * The tests mirror the exact map operations from:
 * - Timeout handler (submitAcquireTokenSilentSync catch TimeoutException)
 * - Worker finally block (submitSilentReturningFuture worker Runnable)
 * - Dedup branch (submitSilentReturningFuture map lookup)
 *
 * Each test covers a scenario identified during the work item 61756756 investigation.
 */
class CommandDispatcherStaleEvictionTest {

    // Simulate sExecutingCommandMap with String keys for simplicity
    private lateinit var executingCommandMap: ConcurrentMap<String, FinalizableResultFuture<String>>
    private val mapAccessLock = Any()
    private val executor = Executors.newFixedThreadPool(4)

    @Before
    fun setUp() {
        executingCommandMap = ConcurrentHashMap()
    }

    @After
    fun tearDown() {
        executor.shutdownNow()
    }

    /**
     * Verifies that after timeout + eviction, the map no longer contains the
     * timed-out command, and a new request can insert a fresh future.
     *
     * Simulates: worker hangs → caller times out → stale entry evicted →
     * new request creates fresh future → new worker executes successfully.
     */
    @Test
    fun testTimeoutEvictsStaleEntry() {
        val command = "AcquireTokenSilent:user@contoso.com:scope1"
        val stuckFuture = FinalizableResultFuture<String>()

        // Insert original entry (simulates submitSilentReturningFuture)
        executingCommandMap[command] = stuckFuture

        // Simulate timeout handler with ownership-aware eviction
        synchronized(mapAccessLock) {
            val mapFuture = executingCommandMap[command]
            if (mapFuture === stuckFuture) {
                executingCommandMap.remove(command)
            }
        }

        // Map should be empty after eviction
        assertFalse("Map should not contain evicted command", executingCommandMap.containsKey(command))
        assertEquals("Map should be empty", 0, executingCommandMap.size)

        // New request should be able to insert a fresh future
        val freshFuture = FinalizableResultFuture<String>()
        executingCommandMap[command] = freshFuture
        assertSame("New future should be in map", freshFuture, executingCommandMap[command])
        assertNotSame("New future should differ from stuck future", stuckFuture, freshFuture)
    }

    /**
     * Verifies that when the old stuck worker eventually completes after
     * stale eviction, it does NOT remove the replacement future.
     *
     * Simulates: worker A hangs → timeout → stale eviction →
     * new request inserts future B → worker A completes → future B survives.
     */
    @Test
    fun testOldWorkerDoesNotRemoveReplacementFuture() {
        val command = "AcquireTokenSilent:user@contoso.com:scope1"
        val stuckFuture = FinalizableResultFuture<String>()
        val replacementFuture = FinalizableResultFuture<String>()

        // Phase 1: Insert original entry
        executingCommandMap[command] = stuckFuture

        // Phase 2: Timeout evicts stale entry
        synchronized(mapAccessLock) {
            val mapFuture = executingCommandMap[command]
            if (mapFuture === stuckFuture) {
                executingCommandMap.remove(command)
            }
        }

        // Phase 3: New request inserts replacement
        executingCommandMap[command] = replacementFuture
        assertSame("Replacement future should be in map", replacementFuture, executingCommandMap[command])

        // Phase 4: Old stuck worker finally completes — ownership-aware cleanup
        synchronized(mapAccessLock) {
            val mapFuture = executingCommandMap[command]
            if (mapFuture === stuckFuture) {
                // Would remove, but ownership check prevents it
                executingCommandMap.remove(command)
            }
            // mapFuture !== stuckFuture, so skip removal
        }

        // Replacement future must survive
        assertSame(
            "Replacement future must NOT be removed by old worker",
            replacementFuture,
            executingCommandMap[command]
        )
    }

    /**
     * Verifies that callers already dedup'd to the old future continue waiting
     * on that future and are not affected by eviction.
     *
     * Eviction only prevents NEW requests from dedup'ing to the stuck future.
     * Existing waiters must timeout independently.
     */
    @Test
    fun testExistingDedupCallersRemainOnOldFuture() {
        val command = "AcquireTokenSilent:user@contoso.com:scope1"
        val stuckFuture = FinalizableResultFuture<String>()

        executingCommandMap[command] = stuckFuture

        // Caller A and B both get the same future (dedup'd)
        val callerAFuture = executingCommandMap[command]
        val callerBFuture = executingCommandMap[command]
        assertSame("Both callers should share the same future", callerAFuture, callerBFuture)

        // Evict stale entry
        synchronized(mapAccessLock) {
            val mapFuture = executingCommandMap[command]
            if (mapFuture === stuckFuture) {
                executingCommandMap.remove(command)
            }
        }

        // Existing callers still hold reference to the stuck future
        assertSame("Caller A should still have the stuck future", stuckFuture, callerAFuture)
        assertSame("Caller B should still have the stuck future", stuckFuture, callerBFuture)

        // Verify callers can still timeout on their future independently
        var timedOut = false
        try {
            stuckFuture.get(50, TimeUnit.MILLISECONDS)
        } catch (e: TimeoutException) {
            timedOut = true
        }
        assertTrue("Existing callers should timeout on old future", timedOut)
    }

    /**
     * Verifies that when the eviction flight is off (simulated by skipping
     * eviction), timeout does NOT evict the map entry.
     */
    @Test
    fun testFlightOffPreservesOldBehavior() {
        val command = "AcquireTokenSilent:user@contoso.com:scope1"
        val stuckFuture = FinalizableResultFuture<String>()

        executingCommandMap[command] = stuckFuture

        // Simulate flight off: timeout handler does NOT evict
        val flightEnabled = false
        if (flightEnabled) {
            synchronized(mapAccessLock) {
                val mapFuture = executingCommandMap[command]
                if (mapFuture === stuckFuture) {
                    executingCommandMap.remove(command)
                }
            }
        }

        // Map should still contain the stuck entry
        assertSame(
            "With flight off, stuck future should remain in map",
            stuckFuture,
            executingCommandMap[command]
        )

        // New request should get the stuck future (dedup behavior preserved)
        val dedupFuture = executingCommandMap[command]
        assertSame("New request should dedup to stuck future", stuckFuture, dedupFuture)
    }

    /**
     * Verifies that if a replacement future is inserted between the timeout
     * and the eviction lock acquisition, the eviction is skipped.
     *
     * Simulates the race: timeout fires → new request replaces entry →
     * timeout handler acquires lock → identity check prevents removal.
     */
    @Test
    fun testEvictionSkippedWhenMapEntryAlreadyReplaced() {
        val command = "AcquireTokenSilent:user@contoso.com:scope1"
        val stuckFuture = FinalizableResultFuture<String>()
        val replacementFuture = FinalizableResultFuture<String>()

        // Phase 1: Insert original entry
        executingCommandMap[command] = stuckFuture

        // Phase 2: Before timeout handler acquires lock, a new request replaces the entry
        executingCommandMap[command] = replacementFuture

        // Phase 3: Timeout handler acquires lock and checks ownership
        var evicted = false
        synchronized(mapAccessLock) {
            val mapFuture = executingCommandMap[command]
            if (mapFuture === stuckFuture) {
                executingCommandMap.remove(command)
                evicted = true
            }
        }

        // Eviction should be skipped — mapFuture !== stuckFuture
        assertFalse("Eviction should be skipped when entry already replaced", evicted)
        assertSame(
            "Replacement future should remain in map",
            replacementFuture,
            executingCommandMap[command]
        )
    }

    /**
     * Verifies that [FinalizableResultFuture.getElapsedMillis] returns a reasonable
     * elapsed time for stale-dedup diagnostics.
     */
    @Test
    fun testFutureElapsedMillisReturnsPositiveValue() {
        val future = FinalizableResultFuture<String>()

        // Immediately after creation, elapsed should be very small
        val immediateElapsed = future.elapsedMillis
        assertTrue("Elapsed should be >= 0", immediateElapsed >= 0)

        // After a short sleep, elapsed should increase
        Thread.sleep(100)
        val laterElapsed = future.elapsedMillis
        assertTrue(
            "Elapsed should be >= 50ms after 100ms sleep (was ${laterElapsed}ms)",
            laterElapsed >= 50
        )
    }
}
