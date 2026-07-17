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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.microsoft.identity.common.java.exception.BaseException;
import com.microsoft.identity.common.java.exception.ServiceException;

import org.junit.Test;

import java.util.UUID;

/**
 * Unit tests for {@link BrokerInstallResumeEngine} — single-resolution sink delivery and TTL-expiry
 * resolution (PBI-4). Guarantees the parked callback fires exactly once and that expired parks are
 * resolved with the original error (never hang).
 */
public class BrokerInstallResumeEngineTest {

    private static final BrokerInstallResumeEngine.IExpiredParkedRequestErrorFactory ERROR_FACTORY =
            record -> new ServiceException("broker_needs_to_be_installed",
                    "Device needs to have broker installed", null);

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static ParkedRecord recordWithCallback(final CommandCallback callback, final long expiresAt) {
        final InteractiveTokenCommand command = mock(InteractiveTokenCommand.class);
        when(command.getCallback()).thenReturn(callback);
        return new ParkedRecord(command, "upn@contoso.com", expiresAt);
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void deliverSuccess_firesOnTaskCompletedExactlyOnce() {
        final CommandCallback callback = mock(CommandCallback.class);
        final ParkedRecord record = recordWithCallback(callback, Long.MAX_VALUE);
        final Object result = new Object();

        assertTrue(BrokerInstallResumeEngine.deliverSuccess(record, result));
        assertFalse("second delivery is a no-op", BrokerInstallResumeEngine.deliverSuccess(record, new Object()));

        verify(callback, times(1)).onTaskCompleted(eq(result));
        verify(callback, never()).onError(any());
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void deliverError_firesOnErrorExactlyOnce() {
        final CommandCallback callback = mock(CommandCallback.class);
        final ParkedRecord record = recordWithCallback(callback, Long.MAX_VALUE);
        final BaseException error = new ServiceException("e", "d", null);

        assertTrue(BrokerInstallResumeEngine.deliverError(record, error));
        assertFalse(BrokerInstallResumeEngine.deliverError(record, new ServiceException("e2", "d2", null)));

        verify(callback, times(1)).onError(eq(error));
        verify(callback, never()).onTaskCompleted(any());
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void afterSuccess_errorDeliveryIsSuppressed() {
        final CommandCallback callback = mock(CommandCallback.class);
        final ParkedRecord record = recordWithCallback(callback, Long.MAX_VALUE);

        assertTrue(BrokerInstallResumeEngine.deliverSuccess(record, "token"));
        assertFalse("cannot error after success", BrokerInstallResumeEngine.deliverError(record, new ServiceException("e", "d", null)));

        verify(callback, times(1)).onTaskCompleted(any());
        verify(callback, never()).onError(any());
    }

    @Test
    public void deliverSuccess_withNullCommand_resolvesWithoutThrowing() {
        final ParkedRecord record = new ParkedRecord(null, "upn", Long.MAX_VALUE);
        assertTrue(BrokerInstallResumeEngine.deliverSuccess(record, "token"));
        assertTrue(record.isResolved());
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void sweepAndResolveExpired_resolvesOnlyExpired_withOriginalError() {
        final BrokerInstallResumeRegistry registry = new BrokerInstallResumeRegistry();
        final long now = 100_000L;

        final CommandCallback expiredCallback = mock(CommandCallback.class);
        final CommandCallback liveCallback = mock(CommandCallback.class);
        final String expiredCid = UUID.randomUUID().toString();
        final String liveCid = UUID.randomUUID().toString();
        registry.park(expiredCid, recordWithCallback(expiredCallback, now - 1));
        registry.park(liveCid, recordWithCallback(liveCallback, now + 60_000L));

        final int resolved = BrokerInstallResumeEngine.sweepAndResolveExpired(registry, now, ERROR_FACTORY);

        assertEquals(1, resolved);
        verify(expiredCallback, times(1)).onError(any(ServiceException.class));
        verify(liveCallback, never()).onError(any());
        assertNull("expired record removed", registry.peek(expiredCid));
        assertTrue("live record untouched", registry.peek(liveCid) != null);
    }

    @Test
    public void sweepAndResolveExpired_nothingExpired_returnsZero() {
        final BrokerInstallResumeRegistry registry = new BrokerInstallResumeRegistry();
        registry.park(UUID.randomUUID().toString(),
                recordWithCallback(mock(CommandCallback.class), Long.MAX_VALUE));

        assertEquals(0, BrokerInstallResumeEngine.sweepAndResolveExpired(
                registry, System.currentTimeMillis(), ERROR_FACTORY));
    }
}
