// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.identity.common.java.controllers;

import com.microsoft.identity.common.java.commands.parameters.TokenCommandParameters;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.providers.microsoft.MicrosoftTokenRequest;
import com.microsoft.identity.common.java.providers.oauth2.OAuth2Strategy;
import com.microsoft.identity.common.java.providers.oauth2.TokenResponse;
import com.microsoft.identity.common.java.providers.oauth2.TokenResult;
import com.microsoft.identity.common.java.result.AcquireTokenResult;
import com.microsoft.identity.common.java.result.ILocalAuthenticationResult;

import org.junit.Test;
import org.mockito.Mockito;

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
            final AcquireTokenResult result = success(token("+sm"), fromCache);
            final CommandResult converted = CommandDispatcher.getCommandResultFromTokenResult(
                    result, parameters(claims("target-mc")));
            assertEquals(CommandResult.ResultStatus.ERROR, converted.getStatus());
            assertEquals(ClientException.AGE_ASSURANCE_TARGET_MISMATCH,
                    ((ClientException) converted.getResult()).getErrorCode());
        }
    }

    @Test
    public void acceptedOrUntargetedResultIsUnchanged() {
        final AcquireTokenResult result = success(token("-mc"), true);
        final CommandResult matched = CommandDispatcher.getCommandResultFromTokenResult(
                result, parameters(claims("target-mc")));
        assertEquals(CommandResult.ResultStatus.COMPLETED, matched.getStatus());
        assertSame(result.getLocalAuthenticationResult(), matched.getResult());

        final AcquireTokenResult opaque = success("not-a-token", false);
        final CommandResult untargeted = CommandDispatcher.getCommandResultFromTokenResult(
                opaque, parameters(claims("target-mc").replace("id_token", "access_token")));
        assertEquals(CommandResult.ResultStatus.COMPLETED, untargeted.getStatus());
        assertSame(opaque.getLocalAuthenticationResult(), untargeted.getResult());
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
