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
package com.microsoft.identity.common.internal.commands;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.app.Application;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Unit tests for {@link BrokerInstallResumeSinkWaiter}'s single-shot resolution: exactly one of
 * {@code onBrokerAvailable} / {@code onGiveUp} runs, {@code onBrokerAvailable} only fires once Company
 * Portal is a valid broker (after a fresh discovery), and resolution is terminal.
 * <p>
 * The waiter is driven through the {@code createForTesting} seam and its {@code @VisibleForTesting}
 * {@code onForeground()} / {@code giveUp()} hooks so the test is deterministic (no executor/looper).
 */
@RunWith(RobolectricTestRunner.class)
public class BrokerInstallResumeSinkWaiterTest {

    /** Test double for the broker-availability seam. */
    private static final class FakeChecker
            implements BrokerInstallResumeSinkWaiter.IBrokerAvailabilityChecker {
        boolean cpValid;
        final AtomicInteger refreshCount = new AtomicInteger(0);

        FakeChecker(final boolean cpValid) {
            this.cpValid = cpValid;
        }

        @Override
        public boolean isCompanyPortalValidBroker() {
            return cpValid;
        }

        @Override
        public void refreshBrokerDiscovery() {
            refreshCount.incrementAndGet();
        }
    }

    private static Application app() {
        return ApplicationProvider.getApplicationContext();
    }

    @Test
    public void onForeground_whenCompanyPortalValid_runsOnBrokerAvailableOnceAndRefreshes() {
        final AtomicBoolean available = new AtomicBoolean(false);
        final AtomicBoolean gaveUp = new AtomicBoolean(false);
        final FakeChecker checker = new FakeChecker(true);

        final BrokerInstallResumeSinkWaiter waiter = BrokerInstallResumeSinkWaiter.createForTesting(
                app(), () -> available.set(true), () -> gaveUp.set(true), 1000L, checker);

        waiter.onForeground();

        assertTrue("onBrokerAvailable should run when CP is valid", available.get());
        assertFalse("onGiveUp must not run", gaveUp.get());
        assertEquals("fresh discovery should run exactly once", 1, checker.refreshCount.get());

        // Single-shot: a second foreground must not re-fire.
        available.set(false);
        waiter.onForeground();
        assertFalse("resolution is terminal; must not re-fire", available.get());
        assertEquals(1, checker.refreshCount.get());
    }

    @Test
    public void onForeground_whenCompanyPortalNotValid_doesNothing() {
        final AtomicBoolean available = new AtomicBoolean(false);
        final AtomicBoolean gaveUp = new AtomicBoolean(false);
        final FakeChecker checker = new FakeChecker(false);

        final BrokerInstallResumeSinkWaiter waiter = BrokerInstallResumeSinkWaiter.createForTesting(
                app(), () -> available.set(true), () -> gaveUp.set(true), 1000L, checker);

        waiter.onForeground();

        assertFalse(available.get());
        assertFalse(gaveUp.get());
        assertEquals("no discovery when CP not valid", 0, checker.refreshCount.get());
    }

    @Test
    public void giveUp_whenUnresolved_runsOnGiveUpOnce() {
        final AtomicBoolean available = new AtomicBoolean(false);
        final AtomicBoolean gaveUp = new AtomicBoolean(false);

        final BrokerInstallResumeSinkWaiter waiter = BrokerInstallResumeSinkWaiter.createForTesting(
                app(), () -> available.set(true), () -> gaveUp.set(true), 1000L, new FakeChecker(false));

        waiter.giveUp();

        assertTrue(gaveUp.get());
        assertFalse(available.get());
    }

    @Test
    public void giveUp_afterBrokerAvailableResolved_isNoOp() {
        final AtomicBoolean available = new AtomicBoolean(false);
        final AtomicBoolean gaveUp = new AtomicBoolean(false);

        final BrokerInstallResumeSinkWaiter waiter = BrokerInstallResumeSinkWaiter.createForTesting(
                app(), () -> available.set(true), () -> gaveUp.set(true), 1000L, new FakeChecker(true));

        waiter.onForeground();
        assertTrue(available.get());

        waiter.giveUp();
        assertFalse("give-up after resolution must be a no-op", gaveUp.get());
    }
}
