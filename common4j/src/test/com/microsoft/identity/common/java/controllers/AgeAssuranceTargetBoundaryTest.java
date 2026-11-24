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

package com.microsoft.identity.common.java.controllers;

import com.microsoft.identity.common.java.commands.parameters.TokenCommandParameters;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.interfaces.IPlatformComponents;
import com.microsoft.identity.common.java.providers.microsoft.MicrosoftTokenRequest;
import com.microsoft.identity.common.java.providers.oauth2.OAuth2Strategy;
import com.microsoft.identity.common.java.providers.oauth2.TokenResponse;
import com.microsoft.identity.common.java.providers.oauth2.TokenResult;
import com.microsoft.identity.common.java.result.AcquireTokenResult;
import com.microsoft.identity.common.java.result.ILocalAuthenticationResult;

import org.junit.Test;
import org.mockito.Mockito;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collections;

import static com.microsoft.identity.common.java.AuthenticationConstants.Broker.LOOKUP_MODE_VALUE;
import static com.microsoft.identity.common.java.AuthenticationConstants.Broker.NATIVEBROKER_KEY;
import static com.microsoft.identity.common.java.AuthenticationConstants.Broker.NATIVEBROKER_MODE_KEY;
import static com.microsoft.identity.common.java.AuthenticationConstants.Broker.NATIVEBROKER_VALUE;
import static com.microsoft.identity.common.java.providers.oauth2.AgeAssuranceTargetValidatorTest.claims;
import static com.microsoft.identity.common.java.providers.oauth2.AgeAssuranceTargetValidatorTest.token;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AgeAssuranceTargetBoundaryTest {
    @Test
    public void networkMismatchThrowsBeforeReturningTokensForPersistence() throws Exception {
        final BaseController controller = mock(BaseController.class, Mockito.CALLS_REAL_METHODS);
        final OAuth2Strategy strategy = mock(OAuth2Strategy.class);
        final MicrosoftTokenRequest wireRequest = new MicrosoftTokenRequest();
        // The wire request may have been modified; validation binds to the original operation.
        wireRequest.setClaims(claims("target-sm"));
        final TokenResponse response = new TokenResponse();
        response.setIdToken(token("+sm"));
        when(strategy.requestToken(wireRequest)).thenReturn(new TokenResult(response));
        final TokenCommandParameters parameters = parameters(claims("target-mc"));

        try {
            controller.executeTokenRequest(strategy, wireRequest, parameters);
            fail("A mismatched response must not be returned for persistence");
        } catch (final ClientException exception) {
            assertEquals(ClientException.AGE_ASSURANCE_TARGET_MISMATCH, exception.getErrorCode());
        }

        response.setIdToken(token("-mc"));
        assertSame(response,
                controller.executeTokenRequest(strategy, wireRequest, parameters).getTokenResponse());
    }

    @Test
    public void cachedAndFreshSuccessResultsAreRejectedBeforeDelivery() {
        for (final boolean fromCache : new boolean[]{true, false}) {
            for (final String request : new String[]{claims("target-mc"), claims(), null}) {
                final AcquireTokenResult result = success(token("+sm"), fromCache);
                final CommandResult converted = CommandDispatcher.getCommandResultFromTokenResult(
                        result, parameters(request));
                assertEquals(CommandResult.ResultStatus.ERROR, converted.getStatus());
                assertEquals(ClientException.AGE_ASSURANCE_TARGET_MISMATCH,
                        ((ClientException) converted.getResult()).getErrorCode());
            }
        }
    }

    @Test
    public void untargetedNetworkResponseIsCheckedBeforePersistence() throws Exception {
        final BaseController controller = mock(BaseController.class, Mockito.CALLS_REAL_METHODS);
        final OAuth2Strategy strategy = mock(OAuth2Strategy.class);
        final MicrosoftTokenRequest wireRequest = new MicrosoftTokenRequest();
        final TokenResponse response = new TokenResponse();
        when(strategy.requestToken(wireRequest)).thenReturn(new TokenResult(response));
        for (final String request : new String[]{null, claims()}) {
            response.setIdToken(token("+mc"));
            try {
                controller.executeTokenRequest(strategy, wireRequest, parameters(request));
                fail("An unsolicited named receipt must not be persisted");
            } catch (final ClientException exception) {
                assertEquals(ClientException.AGE_ASSURANCE_TARGET_MISMATCH, exception.getErrorCode());
            }
            for (final String idToken : new String[]{null, token(null), token(""), token("-ALL")}) {
                response.setIdToken(idToken);
                assertSame(response, controller.executeTokenRequest(
                        strategy, wireRequest, parameters(request)).getTokenResponse());
            }
        }
    }

    @Test
    public void lookupContextIsPreservedAtNetworkAndDispatchBoundaries() throws Exception {
        final BaseController controller = mock(BaseController.class, Mockito.CALLS_REAL_METHODS);
        final OAuth2Strategy strategy = mock(OAuth2Strategy.class);
        final MicrosoftTokenRequest wireRequest = new MicrosoftTokenRequest();
        final TokenResponse response = new TokenResponse();
        response.setIdToken("none");
        when(strategy.requestToken(wireRequest)).thenReturn(new TokenResult(response));

        for (final boolean lookup : new boolean[]{true, false}) {
            for (final String request : new String[]{null, claims(), claims("target-mc"),
                    claims("target-all")}) {
                final TokenCommandParameters parameters = TokenCommandParameters.builder()
                        .platformComponents(mock(IPlatformComponents.class))
                        .claimsRequestJson(request)
                        .extraTokenBodyParameters(lookup ? Arrays.asList(
                                new AbstractMap.SimpleEntry<>(NATIVEBROKER_KEY, NATIVEBROKER_VALUE),
                                new AbstractMap.SimpleEntry<>(NATIVEBROKER_MODE_KEY, LOOKUP_MODE_VALUE))
                                : Collections.emptyList())
                        .build();
                final boolean accepted = lookup && (request == null || request.equals(claims()));
                try {
                    assertSame(response, controller.executeTokenRequest(
                            strategy, wireRequest, parameters).getTokenResponse());
                    if (!accepted) {
                        fail("The sentinel requires trusted lookup mode without targets");
                    }
                } catch (final ClientException exception) {
                    if (accepted) {
                        throw exception;
                    }
                    assertEquals(ClientException.AGE_ASSURANCE_TARGET_MISMATCH,
                            exception.getErrorCode());
                }
                for (final boolean fromCache : new boolean[]{true, false}) {
                    final AcquireTokenResult result = success("none", fromCache);
                    final CommandResult converted = CommandDispatcher.getCommandResultFromTokenResult(
                            result, parameters);
                    assertEquals(accepted ? CommandResult.ResultStatus.COMPLETED
                            : CommandResult.ResultStatus.ERROR, converted.getStatus());
                    if (accepted) {
                        assertSame(result.getLocalAuthenticationResult(), converted.getResult());
                    } else {
                        assertEquals(ClientException.AGE_ASSURANCE_TARGET_MISMATCH,
                                ((ClientException) converted.getResult()).getErrorCode());
                    }
                }
            }
        }
    }

    @Test
    public void acceptedOrUntargetedResultIsUnchanged() {
        final AcquireTokenResult result = success(token("-mc"), true);
        final CommandResult matched = CommandDispatcher.getCommandResultFromTokenResult(
                result, parameters(claims("target-mc")));
        assertEquals(CommandResult.ResultStatus.COMPLETED, matched.getStatus());
        assertSame(result.getLocalAuthenticationResult(), matched.getResult());

        final AcquireTokenResult opaque = success(null, false);
        final CommandResult untargeted = CommandDispatcher.getCommandResultFromTokenResult(
                opaque, parameters(claims("target-mc").replace("id_token", "access_token")));
        assertEquals(CommandResult.ResultStatus.COMPLETED, untargeted.getStatus());
        assertSame(opaque.getLocalAuthenticationResult(), untargeted.getResult());

        for (final String idToken : new String[]{token(null), token(""), token("+ALL")}) {
            final AcquireTokenResult untargetedResult = success(idToken, true);
            final CommandResult accepted = CommandDispatcher.getCommandResultFromTokenResult(
                    untargetedResult, parameters(null));
            assertEquals(CommandResult.ResultStatus.COMPLETED, accepted.getStatus());
            assertSame(untargetedResult.getLocalAuthenticationResult(), accepted.getResult());
        }
    }

    private static TokenCommandParameters parameters(final String claims) {
        final TokenCommandParameters parameters = mock(TokenCommandParameters.class);
        when(parameters.getClaimsRequestJson()).thenReturn(claims);
        when(parameters.getCorrelationId()).thenReturn("test-correlation");
        return parameters;
    }

    private static AcquireTokenResult success(final String token, final boolean fromCache) {
        final ILocalAuthenticationResult local = mock(ILocalAuthenticationResult.class);
        when(local.getIdToken()).thenReturn(token);
        when(local.isServicedFromCache()).thenReturn(fromCache);
        final AcquireTokenResult result = new AcquireTokenResult();
        result.setLocalAuthenticationResult(local);
        return result;
    }
}
