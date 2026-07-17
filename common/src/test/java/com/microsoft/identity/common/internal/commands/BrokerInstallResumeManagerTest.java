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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.microsoft.identity.common.internal.commands.BrokerInstallResumeManager.ICompanyPortalTrust;
import com.microsoft.identity.common.java.commands.BrokerInstallResumeCoordinator.ISilentResumeSubmitter;
import com.microsoft.identity.common.java.commands.BrokerInstallResumeRegistry;
import com.microsoft.identity.common.java.commands.CommandCallback;
import com.microsoft.identity.common.java.commands.InteractiveTokenCommand;
import com.microsoft.identity.common.java.commands.ParkedRecord;
import com.microsoft.identity.common.java.commands.parameters.InteractiveTokenCommandParameters;
import com.microsoft.identity.common.java.exception.BrokerInstallationRequiredException;
import com.microsoft.identity.common.java.flighting.CommonFlight;
import com.microsoft.identity.common.java.flighting.IFlightsProvider;
import com.microsoft.identity.common.java.interfaces.IPlatformComponents;

import org.junit.Test;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Unit tests for {@link BrokerInstallResumeManager}'s Android-independent core: the flight gate, Company
 * Portal trust gate, registry lookup, and hand-off to the coordinator (Phase 5).
 */
public class BrokerInstallResumeManagerTest {

    private static final String CP = "com.microsoft.windowsintune.companyportal";

    private static IFlightsProvider flights(final boolean enabled) {
        final IFlightsProvider flights = mock(IFlightsProvider.class);
        when(flights.isFlightEnabled(CommonFlight.ENABLE_BROKER_INSTALL_RESUME)).thenReturn(enabled);
        return flights;
    }

    private static ICompanyPortalTrust cpTrust(final boolean callerTrusted, final boolean installed) {
        final ICompanyPortalTrust trust = mock(ICompanyPortalTrust.class);
        when(trust.isTrustedCompanyPortal(any())).thenReturn(callerTrusted);
        when(trust.isCompanyPortalInstalledAndValid()).thenReturn(installed);
        return trust;
    }

    /**
     * Returns the process-wide registry singleton, cleared for test isolation. The registry's own
     * constructor is package-private; tests here live in a different package, so we use the singleton and
     * clear it. Tests run sequentially, so this is safe.
     */
    private static BrokerInstallResumeRegistry freshRegistry() {
        final BrokerInstallResumeRegistry registry = BrokerInstallResumeRegistry.getInstance();
        registry.clear();
        return registry;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static ParkedRecord parkedRecord(final CommandCallback callback) {
        final InteractiveTokenCommandParameters params = InteractiveTokenCommandParameters.builder()
                .platformComponents(mock(IPlatformComponents.class))
                .clientId("client-123")
                .redirectUri("msauth://com.contoso.app/hash")
                .correlationId(UUID.randomUUID().toString())
                .scopes(Collections.singleton("User.Read"))
                .build();
        final InteractiveTokenCommand command = mock(InteractiveTokenCommand.class);
        when(command.getCallback()).thenReturn(callback);
        when(command.getParameters()).thenReturn(params);
        when(command.getCorrelationId()).thenReturn(params.getCorrelationId());
        return new ParkedRecord(command, "upn@contoso.com", Long.MAX_VALUE);
    }

    @Test
    public void onResumeRedirect_flightOff_isNoOp() {
        final BrokerInstallResumeManager mgr = BrokerInstallResumeManager.newInstanceForTesting();
        final BrokerInstallResumeRegistry registry = freshRegistry();
        registry.park("cid", parkedRecord(mock(CommandCallback.class)));

        assertFalse(mgr.onResumeRedirect("cid", CP, flights(false), cpTrust(true, true), registry));
        assertEquals("record must remain parked", 1, registry.size());
    }

    @Test
    public void onResumeRedirect_untrustedCaller_isRejected() {
        final BrokerInstallResumeManager mgr = BrokerInstallResumeManager.newInstanceForTesting();
        final BrokerInstallResumeRegistry registry = freshRegistry();
        registry.park("cid", parkedRecord(mock(CommandCallback.class)));

        assertFalse(mgr.onResumeRedirect("cid", "com.evil.app", flights(true), cpTrust(false, true), registry));
        assertEquals("record must remain parked", 1, registry.size());
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void onResumeRedirect_trustedCallerWithMatch_resumesAndDelivers() {
        final BrokerInstallResumeManager mgr = BrokerInstallResumeManager.newInstanceForTesting();
        final AtomicBoolean submitted = new AtomicBoolean(false);
        final Object token = new Object();
        mgr.registerSilentResumeSubmitter((params, record) -> {
            submitted.set(true);
            return token;
        });
        final CommandCallback callback = mock(CommandCallback.class);
        final BrokerInstallResumeRegistry registry = freshRegistry();
        registry.park("cid", parkedRecord(callback));

        assertTrue(mgr.onResumeRedirect("cid", CP, flights(true), cpTrust(true, true), registry));
        assertTrue(submitted.get());
        verify(callback, times(1)).onTaskCompleted(token);
        assertTrue("record must be consumed", registry.isEmpty());
    }

    @Test
    public void onResumeRedirect_noParkedMatch_returnsFalse() {
        final BrokerInstallResumeManager mgr = BrokerInstallResumeManager.newInstanceForTesting();
        final BrokerInstallResumeRegistry registry = freshRegistry();

        assertFalse(mgr.onResumeRedirect("missing", CP, flights(true), cpTrust(true, true), registry));
    }

    @Test
    public void onResumeRedirect_emptyCid_returnsFalse_withoutTouchingRegistry() {
        final BrokerInstallResumeManager mgr = BrokerInstallResumeManager.newInstanceForTesting();
        final BrokerInstallResumeRegistry registry = freshRegistry();
        registry.park("cid", parkedRecord(mock(CommandCallback.class)));

        assertFalse(mgr.onResumeRedirect("", CP, flights(true), cpTrust(true, true), registry));
        assertFalse(mgr.onResumeRedirectByCapability("", flights(true), cpTrust(false, true), registry));
        assertEquals("empty cid must not disturb parked records", 1, registry.size());
    }

    @Test
    public void onResumeRedirectByCapability_cpNotInstalled_isRejected() {
        final BrokerInstallResumeManager mgr = BrokerInstallResumeManager.newInstanceForTesting();
        final BrokerInstallResumeRegistry registry = freshRegistry();
        registry.park("cid", parkedRecord(mock(CommandCallback.class)));

        assertFalse(mgr.onResumeRedirectByCapability("cid", flights(true), cpTrust(false, false), registry));
        assertEquals("record must remain parked when CP is not yet valid", 1, registry.size());
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void onResumeRedirectByCapability_cpInstalledWithMatch_resumes() {
        final BrokerInstallResumeManager mgr = BrokerInstallResumeManager.newInstanceForTesting();
        mgr.registerSilentResumeSubmitter((params, record) -> new Object());
        final CommandCallback callback = mock(CommandCallback.class);
        final BrokerInstallResumeRegistry registry = freshRegistry();
        registry.park("cid", parkedRecord(callback));

        assertTrue(mgr.onResumeRedirectByCapability("cid", flights(true), cpTrust(false, true), registry));
        verify(callback, times(1)).onTaskCompleted(any());
    }

    @Test
    public void onAppForegrounded_cpNotInstalled_leavesRequestsParked() {
        final BrokerInstallResumeManager mgr = BrokerInstallResumeManager.newInstanceForTesting();
        final BrokerInstallResumeRegistry registry = freshRegistry();
        registry.park("cid", parkedRecord(mock(CommandCallback.class)));

        assertEquals(0, mgr.onAppForegrounded(flights(true), cpTrust(false, false), registry));
        assertEquals(1, registry.size());
    }

    @Test
    public void onAppForegrounded_flightOffOrEmpty_isNoOp() {
        final BrokerInstallResumeManager mgr = BrokerInstallResumeManager.newInstanceForTesting();
        final BrokerInstallResumeRegistry empty = freshRegistry();
        assertEquals(0, mgr.onAppForegrounded(flights(false), cpTrust(true, true), empty));
        assertEquals(0, mgr.onAppForegrounded(flights(true), cpTrust(true, true), empty));
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void onAppForegrounded_cpInstalledWithParked_resumesAll() {
        final BrokerInstallResumeManager mgr = BrokerInstallResumeManager.newInstanceForTesting();
        mgr.registerSilentResumeSubmitter((params, record) -> new Object());
        final CommandCallback callback = mock(CommandCallback.class);
        final BrokerInstallResumeRegistry registry = freshRegistry();
        registry.park("cid", parkedRecord(callback));

        assertEquals(1, mgr.onAppForegrounded(flights(true), cpTrust(true, true), registry));
        verify(callback, times(1)).onTaskCompleted(any());
        assertTrue(registry.isEmpty());
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void resume_withNoSubmitterRegistered_deliversInstallRequiredError() {
        // No submitter registered -> the parked sink is resolved with the original install-required error
        // (the request must never hang).
        final BrokerInstallResumeManager mgr = BrokerInstallResumeManager.newInstanceForTesting();
        final CommandCallback callback = mock(CommandCallback.class);
        final BrokerInstallResumeRegistry registry = freshRegistry();
        registry.park("cid", parkedRecord(callback));

        assertTrue(mgr.onResumeRedirect("cid", CP, flights(true), cpTrust(true, true), registry));
        verify(callback, times(1)).onError(any(BrokerInstallationRequiredException.class));
        verify(callback, never()).onTaskCompleted(any());
    }
}
