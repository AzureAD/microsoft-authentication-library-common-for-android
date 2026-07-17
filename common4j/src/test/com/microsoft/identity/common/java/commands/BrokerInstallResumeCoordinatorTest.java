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
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.microsoft.identity.common.java.commands.parameters.InteractiveTokenCommandParameters;
import com.microsoft.identity.common.java.commands.parameters.SilentTokenCommandParameters;
import com.microsoft.identity.common.java.exception.BaseException;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.exception.ServiceException;
import com.microsoft.identity.common.java.interfaces.IPlatformComponents;

import org.junit.Test;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Unit tests for {@link BrokerInstallResumeCoordinator} — the resume orchestration spine (PBI-4): project
 * parked interactive params to silent (login_hint = UPN), invoke the platform submitter, and deliver the
 * outcome to the original callback exactly once.
 */
public class BrokerInstallResumeCoordinatorTest {

    private static final String UPN = "upn@contoso.com";

    private static InteractiveTokenCommandParameters interactiveParams() {
        return InteractiveTokenCommandParameters.builder()
                .platformComponents(mock(IPlatformComponents.class))
                .clientId("client-123")
                .redirectUri("msauth://com.contoso.app/hash")
                .correlationId("cid-abc")
                .scopes(Collections.singleton("User.Read"))
                .loginHint("old-hint@contoso.com")
                .build();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static ParkedRecord recordWithParams(final CommandCallback callback,
                                                 final InteractiveTokenCommandParameters params) {
        final InteractiveTokenCommand command = mock(InteractiveTokenCommand.class);
        when(command.getCallback()).thenReturn(callback);
        when(command.getParameters()).thenReturn(params);
        return new ParkedRecord(command, UPN, Long.MAX_VALUE);
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void resume_success_projectsSilentParamsWithUpn_andDeliversResult() {
        final CommandCallback callback = mock(CommandCallback.class);
        final ParkedRecord record = recordWithParams(callback, interactiveParams());
        final Object token = new Object();
        final AtomicReference<SilentTokenCommandParameters> captured = new AtomicReference<>();

        final boolean delivered = BrokerInstallResumeCoordinator.resume(record, (params, rec) -> {
            captured.set(params);
            return token;
        }, null);

        assertTrue(delivered);
        assertTrue(record.isResolved());
        verify(callback, times(1)).onTaskCompleted(eq(token));
        verify(callback, never()).onError(any());
        assertEquals("login_hint must be the UPN on the silent retry", UPN, captured.get().getLoginHint());
        assertEquals("client-123", captured.get().getClientId());
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void resume_whenSubmitterThrows_deliversErrorToOriginalCallback() {
        final CommandCallback callback = mock(CommandCallback.class);
        final ParkedRecord record = recordWithParams(callback, interactiveParams());
        final BaseException failure = new ServiceException("invalid_grant", "no token", null);

        final boolean delivered = BrokerInstallResumeCoordinator.resume(record, (params, rec) -> {
            throw failure;
        }, null);

        assertTrue(delivered);
        verify(callback, times(1)).onError(eq(failure));
        verify(callback, never()).onTaskCompleted(any());
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void resume_whenAlreadyResolved_isNoOp_andDoesNotSubmit() {
        final CommandCallback callback = mock(CommandCallback.class);
        final ParkedRecord record = recordWithParams(callback, interactiveParams());
        assertTrue("pre-resolve", record.tryResolve());
        final AtomicReference<Boolean> submitted = new AtomicReference<>(false);

        final boolean delivered = BrokerInstallResumeCoordinator.resume(record, (params, rec) -> {
            submitted.set(true);
            return new Object();
        }, null);

        assertFalse(delivered);
        assertFalse("submitter must not run for an already-resolved record", submitted.get());
        verify(callback, never()).onTaskCompleted(any());
        verify(callback, never()).onError(any());
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void resume_missingInteractiveParams_deliversClientErrorWithoutSubmitting() {
        final CommandCallback callback = mock(CommandCallback.class);
        final InteractiveTokenCommand command = mock(InteractiveTokenCommand.class);
        when(command.getCallback()).thenReturn(callback);
        // Not an InteractiveTokenCommandParameters -> triggers the defensive guard.
        when(command.getParameters()).thenReturn(mock(SilentTokenCommandParameters.class));
        final ParkedRecord record = new ParkedRecord(command, UPN, Long.MAX_VALUE);
        final AtomicReference<Boolean> submitted = new AtomicReference<>(false);

        final boolean delivered = BrokerInstallResumeCoordinator.resume(record, (params, rec) -> {
            submitted.set(true);
            return new Object();
        }, null);

        assertTrue(delivered);
        assertFalse(submitted.get());
        verify(callback, times(1)).onError(any(ClientException.class));
    }
}
