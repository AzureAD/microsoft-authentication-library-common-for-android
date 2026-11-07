// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.identity.common.internal.controllers;

import android.content.Intent;
import android.os.Bundle;

import androidx.test.platform.app.InstrumentationRegistry;

import com.google.gson.Gson;
import com.microsoft.identity.common.adal.internal.AuthenticationConstants;
import com.microsoft.identity.common.components.MockPlatformComponentsFactory;
import com.microsoft.identity.common.internal.broker.BrokerResult;
import com.microsoft.identity.common.internal.broker.ipc.BrokerOperationBundle;
import com.microsoft.identity.common.internal.broker.ipc.IIpcStrategy;
import com.microsoft.identity.common.java.authorities.AzureActiveDirectoryAudience;
import com.microsoft.identity.common.java.authorities.AzureActiveDirectoryAuthority;
import com.microsoft.identity.common.java.authscheme.BearerAuthenticationSchemeInternal;
import com.microsoft.identity.common.java.cache.CacheRecord;
import com.microsoft.identity.common.java.cache.MsalOAuth2TokenCache;
import com.microsoft.identity.common.java.commands.parameters.InteractiveTokenCommandParameters;
import com.microsoft.identity.common.java.commands.parameters.SilentTokenCommandParameters;
import com.microsoft.identity.common.java.dto.AccountRecord;
import com.microsoft.identity.common.java.dto.IdTokenRecord;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.interfaces.IPlatformComponents;
import com.microsoft.identity.common.java.request.SdkType;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verifyNoInteractions;

@RunWith(RobolectricTestRunner.class)
public class BrokerTargetBindingTest {
    @Test
    public void silentBrokerCachedSuccessIsValidated() throws Exception {
        final IPlatformComponents components =
                MockPlatformComponentsFactory.getNonFunctionalBuilder().build();
        final IdTokenRecord idToken = new IdTokenRecord();
        idToken.setSecret(token("+sm"));
        final CacheRecord record = new CacheRecord();
        record.setIdToken(idToken);
        final BrokerResult response = new BrokerResult.Builder()
                .success(true).servicedFromCache(true)
                .tenantProfileRecords(Collections.singletonList(record)).build();
        final IIpcStrategy strategy = new IIpcStrategy() {
            @Override
            public Bundle communicateToBroker(final BrokerOperationBundle request) {
                final Bundle bundle = new Bundle();
                bundle.putString(AuthenticationConstants.Broker.NEGOTIATED_BP_VERSION_KEY, "1.0");
                if (request.getOperation() == BrokerOperationBundle.Operation.MSAL_ACQUIRE_TOKEN_SILENT) {
                    bundle.putBoolean(AuthenticationConstants.Broker.BROKER_REQUEST_V2_SUCCESS, true);
                    bundle.putString(AuthenticationConstants.Broker.BROKER_RESULT_V2,
                            new Gson().toJson(response));
                }
                return bundle;
            }

            @Override
            public boolean isSupportedByTargetedBroker(final String packageName) {
                return true;
            }

            @Override
            public Type getType() {
                return Type.CONTENT_PROVIDER;
            }
        };
        final BrokerMsalController controller = new BrokerMsalController(
                InstrumentationRegistry.getInstrumentation().getContext(), components,
                "test.broker", Collections.singletonList(strategy));
        final SilentTokenCommandParameters parameters = SilentTokenCommandParameters.builder()
                .platformComponents(components)
                .authority(new AzureActiveDirectoryAuthority())
                .account(new AccountRecord())
                .scopes(Collections.singleton("user.read"))
                .authenticationScheme(new BearerAuthenticationSchemeInternal())
                .sdkType(SdkType.MSAL)
                .requiredBrokerProtocolVersion("1.0")
                .claimsRequestJson("{\"id_token\":{\"xms_agvs\":{"
                        + "\"additionalProperties\":[\"target-mc\"]}}}")
                .build();
        try {
            controller.acquireTokenSilent(parameters);
            fail("A broker cache hit must not bypass target binding");
        } catch (final ClientException exception) {
            assertEquals(ClientException.AGE_ASSURANCE_TARGET_MISMATCH, exception.getErrorCode());
        }
    }

    @Test
    public void interactiveBrokerMismatchCannotReachClientMsaCache() throws Exception {
        for (final String localReceipt : new String[]{"+sm", "-mc"}) {
            final IPlatformComponents components =
                    MockPlatformComponentsFactory.getNonFunctionalBuilder().build();
            final MsalOAuth2TokenCache cache = mock(MsalOAuth2TokenCache.class);
            final BrokerMsalController controller = spy(new BrokerMsalController(
                    InstrumentationRegistry.getInstrumentation().getContext(), components,
                    "test.broker"));
            final InteractiveTokenCommandParameters parameters =
                    InteractiveTokenCommandParameters.builder()
                            .platformComponents(components)
                            .oAuth2TokenCache(cache)
                            .requiredBrokerProtocolVersion("1.0")
                            .claimsRequestJson("{\"id_token\":{\"xms_agvs\":{"
                                    + "\"additionalProperties\":[\"target-mc\"]}}}")
                            .build();
            final IdTokenRecord idToken = new IdTokenRecord();
            idToken.setSecret(token(localReceipt));
            final CacheRecord record = new CacheRecord();
            record.setIdToken(idToken);
            final BrokerResult brokerResult = new BrokerResult.Builder()
                    .success(true)
                    .tenantId(AzureActiveDirectoryAudience.MSA_MEGA_TENANT_ID)
                    // The root token used for MSA persistence must be checked independently
                    // of the tenant-profile token selected for success delivery.
                    .idToken(token("+sm"))
                    .tenantProfileRecords(Collections.singletonList(record))
                    .build();
            final Bundle bundle = new Bundle();
            bundle.putBoolean(AuthenticationConstants.Broker.BROKER_REQUEST_V2_SUCCESS, true);
            bundle.putString(AuthenticationConstants.Broker.NEGOTIATED_BP_VERSION_KEY, "1.0");
            bundle.putString(AuthenticationConstants.Broker.BROKER_RESULT_V2,
                    new Gson().toJson(brokerResult));
            doReturn(new Intent()).when(controller).getBrokerAuthorizationIntent(parameters);
            doReturn(bundle).when(controller).acquireTokenInternal(any(), any());

            try {
                controller.acquireToken(parameters);
                fail("Mismatched broker tokens must not be returned or cached");
            } catch (final ClientException exception) {
                assertEquals(ClientException.AGE_ASSURANCE_TARGET_MISMATCH, exception.getErrorCode());
            }
            verifyNoInteractions(cache);
        }
    }

    private static String token(final String receipt) {
        return new PlainJWT(new JWTClaimsSet.Builder()
                .claim("xms_agvs", 0).claim("xms_agvt", receipt).build()).serialize();
    }
}
