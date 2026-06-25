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

import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.exception.ErrorStrings;
import com.microsoft.identity.common.java.exception.ServiceException;
import com.microsoft.identity.common.java.exception.UiRequiredException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class NetworkFailureCooldownTest {

    private NetworkFailureCooldown mCooldown;

    @Before
    public void setUp() {
        mCooldown = NetworkFailureCooldown.getInstance();
        mCooldown.resetForTest();
    }

    @After
    public void tearDown() {
        mCooldown.resetForTest();
    }

    @Test
    public void breakerDoesNotTripBelowThreshold() {
        for (int i = 0; i < NetworkFailureCooldown.FAILURE_THRESHOLD - 1; i++) {
            mCooldown.recordOutcome(new ClientException(ClientException.IO_ERROR, "boom"));
        }
        assertFalse("Should not trip below threshold", mCooldown.isInCooldown());
    }

    @Test
    public void breakerTripsAtThreshold() {
        for (int i = 0; i < NetworkFailureCooldown.FAILURE_THRESHOLD; i++) {
            mCooldown.recordOutcome(new ClientException(ClientException.IO_ERROR, "boom"));
        }
        assertTrue("Should trip at threshold", mCooldown.isInCooldown());
    }

    @Test
    public void successResetsCounter() {
        for (int i = 0; i < NetworkFailureCooldown.FAILURE_THRESHOLD; i++) {
            mCooldown.recordOutcome(new ClientException(ClientException.IO_ERROR, "boom"));
        }
        assertTrue(mCooldown.isInCooldown());
        mCooldown.recordOutcome(null);
        assertFalse("Success should clear the cooldown", mCooldown.isInCooldown());
    }

    @Test
    public void nonNetworkExceptionsAreIgnored() {
        for (int i = 0; i < NetworkFailureCooldown.FAILURE_THRESHOLD * 2; i++) {
            mCooldown.recordOutcome(new UiRequiredException(
                    ErrorStrings.INVALID_BROKER_REFRESH_TOKEN, "user must sign in"));
            mCooldown.recordOutcome(new ClientException(
                    ErrorStrings.NO_TOKENS_FOUND, "nothing cached"));
        }
        assertFalse("Non-network failures must not trip the breaker", mCooldown.isInCooldown());
    }

    @Test
    public void deviceNetworkUnavailableIsClassifiedAsNetworkFailure() {
        for (int i = 0; i < NetworkFailureCooldown.FAILURE_THRESHOLD; i++) {
            mCooldown.recordOutcome(new ClientException(
                    ErrorStrings.DEVICE_NETWORK_NOT_AVAILABLE, "offline"));
        }
        assertTrue(mCooldown.isInCooldown());
    }

    @Test
    public void serviceExceptionWithIoCauseIsClassifiedAsNetworkFailure() {
        for (int i = 0; i < NetworkFailureCooldown.FAILURE_THRESHOLD; i++) {
            final ServiceException svc = new ServiceException(
                    "unreachable", "proxy is dead", new IOException("connect timed out"));
            mCooldown.recordOutcome(svc);
        }
        assertTrue("ServiceException wrapping IOException should be a network failure",
                mCooldown.isInCooldown());
    }

    @Test
    public void cooldownExceptionUsesIoErrorCode() {
        for (int i = 0; i < NetworkFailureCooldown.FAILURE_THRESHOLD; i++) {
            mCooldown.recordOutcome(new ClientException(ClientException.IO_ERROR, "boom"));
        }
        final ClientException e = mCooldown.buildCooldownException();
        assertEquals(ErrorStrings.IO_ERROR, e.getErrorCode());
    }
}
