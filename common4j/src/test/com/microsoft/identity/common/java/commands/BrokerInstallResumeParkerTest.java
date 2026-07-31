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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.microsoft.identity.common.java.controllers.CommandResult;
import com.microsoft.identity.common.java.exception.BrokerInstallationRequiredException;
import com.microsoft.identity.common.java.exception.ServiceException;
import com.microsoft.identity.common.java.flighting.CommonFlight;
import com.microsoft.identity.common.java.flighting.MockFlightsProvider;

import org.junit.Test;

import java.util.UUID;

/**
 * Unit tests for {@link BrokerInstallResumeParker} — the flight-gated decision logic for parking a
 * broker-install interactive request and suppressing its terminal callback (PBI-1).
 */
public class BrokerInstallResumeParkerTest {

    private static final long TTL = BrokerInstallResumeRegistry.DEFAULT_PARK_TTL_MILLISECONDS;
    private static final long NOW = 1_000_000L;
    private static final String UPN = "idlab1@msidlab4.onmicrosoft.com";

    private static MockFlightsProvider flights(final boolean enabled) {
        final MockFlightsProvider provider = new MockFlightsProvider();
        provider.addFlight(CommonFlight.ENABLE_BROKER_INSTALL_RESUME.getKey(), String.valueOf(enabled));
        return provider;
    }

    private static InteractiveTokenCommand interactiveCommand(final String cid) {
        final InteractiveTokenCommand command = mock(InteractiveTokenCommand.class);
        when(command.getCorrelationId()).thenReturn(cid);
        return command;
    }

    private static CommandResult<?> brokerInstallErrorResult(final String cid) {
        return new CommandResult<>(
                CommandResult.ResultStatus.ERROR,
                new BrokerInstallationRequiredException("broker_needs_to_be_installed",
                        "Device needs to have broker installed", UPN, null),
                cid);
    }

    // region parkIfEligible

    @Test
    public void parkIfEligible_eligible_parksAndReturnsTrue() {
        final BrokerInstallResumeRegistry registry = new BrokerInstallResumeRegistry();
        final String cid = UUID.randomUUID().toString();
        final InteractiveTokenCommand command = interactiveCommand(cid);

        final boolean parked = BrokerInstallResumeParker.parkIfEligible(
                command, brokerInstallErrorResult(cid), flights(true), registry, TTL, NOW);

        assertTrue(parked);
        final ParkedRecord record = registry.peek(cid);
        assertNotNull("the request must be parked", record);
        assertEquals(UPN, record.getUpn());
        assertSame(command, record.getInteractiveTokenCommand());
        assertEquals(NOW + TTL, record.getExpiresAtEpochMs());
    }

    @Test
    public void parkIfEligible_flightOff_returnsFalse_andDoesNotPark() {
        final BrokerInstallResumeRegistry registry = new BrokerInstallResumeRegistry();
        final String cid = UUID.randomUUID().toString();

        final boolean parked = BrokerInstallResumeParker.parkIfEligible(
                interactiveCommand(cid), brokerInstallErrorResult(cid), flights(false), registry, TTL, NOW);

        assertFalse(parked);
        assertTrue(registry.isEmpty());
    }

    @Test
    public void parkIfEligible_nonInteractiveCommand_returnsFalse() {
        final BrokerInstallResumeRegistry registry = new BrokerInstallResumeRegistry();
        final String cid = UUID.randomUUID().toString();
        final SilentTokenCommand silent = mock(SilentTokenCommand.class);
        when(silent.getCorrelationId()).thenReturn(cid);

        final boolean parked = BrokerInstallResumeParker.parkIfEligible(
                silent, brokerInstallErrorResult(cid), flights(true), registry, TTL, NOW);

        assertFalse("silent commands must never be parked", parked);
        assertTrue(registry.isEmpty());
    }

    @Test
    public void parkIfEligible_errorButNotBrokerInstall_returnsFalse() {
        final BrokerInstallResumeRegistry registry = new BrokerInstallResumeRegistry();
        final String cid = UUID.randomUUID().toString();
        final CommandResult<?> plainServiceError = new CommandResult<>(
                CommandResult.ResultStatus.ERROR,
                new ServiceException("some_other_error", "desc", null),
                cid);

        final boolean parked = BrokerInstallResumeParker.parkIfEligible(
                interactiveCommand(cid), plainServiceError, flights(true), registry, TTL, NOW);

        assertFalse(parked);
        assertTrue(registry.isEmpty());
    }

    @Test
    public void parkIfEligible_completedResult_returnsFalse() {
        final BrokerInstallResumeRegistry registry = new BrokerInstallResumeRegistry();
        final String cid = UUID.randomUUID().toString();
        final CommandResult<?> completed = new CommandResult<>(
                CommandResult.ResultStatus.COMPLETED, "token", cid);

        final boolean parked = BrokerInstallResumeParker.parkIfEligible(
                interactiveCommand(cid), completed, flights(true), registry, TTL, NOW);

        assertFalse(parked);
        assertTrue(registry.isEmpty());
    }

    // endregion

    // region isCallbackSuppressed

    @Test
    public void isCallbackSuppressed_parkedAndFlightOn_true() {
        final BrokerInstallResumeRegistry registry = new BrokerInstallResumeRegistry();
        final String cid = UUID.randomUUID().toString();
        registry.park(cid, new ParkedRecord(null, UPN, Long.MAX_VALUE));

        assertTrue(BrokerInstallResumeParker.isCallbackSuppressed(cid, flights(true), registry));
    }

    @Test
    public void isCallbackSuppressed_notParked_false() {
        final BrokerInstallResumeRegistry registry = new BrokerInstallResumeRegistry();
        assertFalse(BrokerInstallResumeParker.isCallbackSuppressed(
                UUID.randomUUID().toString(), flights(true), registry));
    }

    @Test
    public void isCallbackSuppressed_flightOff_false_evenIfParked() {
        final BrokerInstallResumeRegistry registry = new BrokerInstallResumeRegistry();
        final String cid = UUID.randomUUID().toString();
        registry.park(cid, new ParkedRecord(null, UPN, Long.MAX_VALUE));

        assertFalse(BrokerInstallResumeParker.isCallbackSuppressed(cid, flights(false), registry));
    }

    // endregion
}
