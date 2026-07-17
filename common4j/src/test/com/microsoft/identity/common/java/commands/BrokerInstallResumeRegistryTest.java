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

package com.microsoft.identity.common.java.commands;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Unit tests for {@link BrokerInstallResumeRegistry} and {@link ParkedRecord} — the in-memory park
 * store for the MAM broker-install request-resume engine (PBI-1 foundation).
 */
public class BrokerInstallResumeRegistryTest {

    private static ParkedRecord record(final String upn, final long expiresAtEpochMs) {
        return new ParkedRecord(null /* command not needed for registry semantics */, upn, expiresAtEpochMs);
    }

    // region ParkedRecord

    @Test
    public void parkedRecord_isExpired_atOrAfterExpiry() {
        final ParkedRecord record = record("u@contoso.com", 1_000L);
        assertFalse(record.isExpired(999L));
        assertTrue("expiry is inclusive", record.isExpired(1_000L));
        assertTrue(record.isExpired(1_001L));
    }

    @Test
    public void parkedRecord_tryResolve_succeedsExactlyOnce() {
        final ParkedRecord record = record("u@contoso.com", Long.MAX_VALUE);
        assertFalse(record.isResolved());
        assertTrue("first resolve wins", record.tryResolve());
        assertFalse("second resolve loses", record.tryResolve());
        assertFalse(record.tryResolve());
        assertTrue(record.isResolved());
    }

    @Test
    public void parkedRecord_tryResolve_isThreadSafe_onlyOneWinner() throws Exception {
        final ParkedRecord record = record("u@contoso.com", Long.MAX_VALUE);
        final int threads = 32;
        final ExecutorService pool = Executors.newFixedThreadPool(threads);
        final AtomicInteger winners = new AtomicInteger(0);
        try {
            final java.util.concurrent.CountDownLatch start = new java.util.concurrent.CountDownLatch(1);
            final java.util.concurrent.CountDownLatch done = new java.util.concurrent.CountDownLatch(threads);
            for (int i = 0; i < threads; i++) {
                pool.submit(() -> {
                    try {
                        start.await();
                        if (record.tryResolve()) {
                            winners.incrementAndGet();
                        }
                    } catch (final InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        done.countDown();
                    }
                });
            }
            start.countDown();
            assertTrue(done.await(10, TimeUnit.SECONDS));
        } finally {
            pool.shutdownNow();
        }
        assertEquals("exactly one thread may resolve the sink", 1, winners.get());
    }

    // endregion

    // region Registry

    @Test
    public void park_thenPeek_returnsSameRecord_withoutRemoving() {
        final BrokerInstallResumeRegistry registry = new BrokerInstallResumeRegistry();
        final String cid = UUID.randomUUID().toString();
        final ParkedRecord parked = record("u@contoso.com", Long.MAX_VALUE);

        registry.park(cid, parked);

        assertSame(parked, registry.peek(cid));
        assertEquals(1, registry.size());
        assertFalse(registry.isEmpty());
    }

    @Test
    public void match_isSingleUse_secondMatchReturnsNull() {
        final BrokerInstallResumeRegistry registry = new BrokerInstallResumeRegistry();
        final String cid = UUID.randomUUID().toString();
        final ParkedRecord parked = record("u@contoso.com", Long.MAX_VALUE);
        registry.park(cid, parked);

        assertSame(parked, registry.match(cid));
        assertNull("match consumes the record", registry.match(cid));
        assertTrue(registry.isEmpty());
    }

    @Test
    public void match_unknownCid_returnsNull_isBenignNoOp() {
        final BrokerInstallResumeRegistry registry = new BrokerInstallResumeRegistry();
        assertNull(registry.match(UUID.randomUUID().toString()));
    }

    @Test
    public void concurrentDistinctCids_coexist_andDoNotCollide() {
        final BrokerInstallResumeRegistry registry = new BrokerInstallResumeRegistry();
        final int count = 200;
        final String[] cids = new String[count];
        for (int i = 0; i < count; i++) {
            cids[i] = UUID.randomUUID().toString();
        }

        java.util.stream.IntStream.range(0, count).parallel().forEach(i ->
                registry.park(cids[i], record("u" + i + "@contoso.com", Long.MAX_VALUE)));

        assertEquals(count, registry.size());
        // Each cid resolves to its own distinct record (no collision / overwrite).
        for (int i = 0; i < count; i++) {
            final ParkedRecord matched = registry.match(cids[i]);
            assertNotNull(matched);
            assertEquals("u" + i + "@contoso.com", matched.getUpn());
        }
        assertTrue(registry.isEmpty());
    }

    @Test
    public void sweepExpired_removesOnlyExpired_andReturnsThem() {
        final BrokerInstallResumeRegistry registry = new BrokerInstallResumeRegistry();
        final long now = 10_000L;

        final String expiredCid = UUID.randomUUID().toString();
        final String liveCid = UUID.randomUUID().toString();
        final ParkedRecord expired = record("expired@contoso.com", now - 1);   // already past
        final ParkedRecord live = record("live@contoso.com", now + 60_000L);   // still valid
        registry.park(expiredCid, expired);
        registry.park(liveCid, live);

        final List<ParkedRecord> swept = registry.sweepExpired(now);

        assertEquals(1, swept.size());
        assertSame(expired, swept.get(0));
        assertNull("expired record was removed", registry.peek(expiredCid));
        assertSame("live record is untouched", live, registry.peek(liveCid));
    }

    @Test
    public void sweepExpired_whenNothingExpired_returnsEmpty() {
        final BrokerInstallResumeRegistry registry = new BrokerInstallResumeRegistry();
        registry.park(UUID.randomUUID().toString(), record("u@contoso.com", Long.MAX_VALUE));
        assertTrue(registry.sweepExpired(System.currentTimeMillis()).isEmpty());
        assertEquals(1, registry.size());
    }

    @Test
    public void clear_removesAll() {
        final BrokerInstallResumeRegistry registry = new BrokerInstallResumeRegistry();
        registry.park(UUID.randomUUID().toString(), record("u@contoso.com", Long.MAX_VALUE));
        registry.park(UUID.randomUUID().toString(), record("v@contoso.com", Long.MAX_VALUE));
        registry.clear();
        assertTrue(registry.isEmpty());
    }

    @Test
    public void defaultTtl_isSevenMinutes() {
        assertEquals(7L * 60L * 1000L, BrokerInstallResumeRegistry.DEFAULT_PARK_TTL_MILLISECONDS);
    }

    @Test
    public void getInstance_returnsProcessSingleton() {
        assertSame(BrokerInstallResumeRegistry.getInstance(), BrokerInstallResumeRegistry.getInstance());
    }

    // endregion
}
