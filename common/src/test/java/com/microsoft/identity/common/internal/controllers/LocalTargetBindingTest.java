// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.identity.common.internal.controllers;

import com.microsoft.identity.common.components.MockPlatformComponentsFactory;
import com.microsoft.identity.common.java.authorities.Authority;
import com.microsoft.identity.common.java.authorities.CIAMAuthority;
import com.microsoft.identity.common.java.authscheme.BearerAuthenticationSchemeInternal;
import com.microsoft.identity.common.java.cache.CacheRecord;
import com.microsoft.identity.common.java.cache.MsalOAuth2TokenCache;
import com.microsoft.identity.common.java.commands.parameters.SilentTokenCommandParameters;
import com.microsoft.identity.common.java.configuration.LibraryConfiguration;
import com.microsoft.identity.common.java.controllers.BaseController;
import com.microsoft.identity.common.java.controllers.CommandDispatcher;
import com.microsoft.identity.common.java.dto.AccessTokenRecord;
import com.microsoft.identity.common.java.dto.AccountRecord;
import com.microsoft.identity.common.java.dto.IdTokenRecord;
import com.microsoft.identity.common.java.dto.RefreshTokenRecord;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.providers.oauth2.OAuth2Strategy;
import com.microsoft.identity.common.java.request.SdkType;
import com.microsoft.identity.common.nativeauth.internal.controllers.NativeAuthMsalController;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.robolectric.RobolectricTestRunner;

import java.util.Collections;
import java.util.HashSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class LocalTargetBindingTest {
    @Test
    public void mismatchedCacheReceiptFailsBeforeRefreshOnOrNetwork() throws Exception {
        for (final BaseController controller : new BaseController[]{
                new LocalMSALController(), new NativeAuthMsalController()}) {
            for (final boolean refreshOn : new boolean[]{false, true}) {
                assertCachedReceiptRejected(controller, refreshOn);
            }
        }
    }

    private void assertCachedReceiptRejected(
            final BaseController controller, final boolean refreshOn) throws Exception {
        final AccountRecord account = new AccountRecord();
        final AccessTokenRecord accessToken = mock(AccessTokenRecord.class);
        when(accessToken.shouldRefresh()).thenReturn(refreshOn);
        when(accessToken.getSecret()).thenReturn("opaque-access-token");
        final IdTokenRecord idToken = new IdTokenRecord();
        idToken.setSecret(new PlainJWT(new JWTClaimsSet.Builder()
                .claim("xms_agvs", 0).claim("xms_agvt", "+sm").build()).serialize());
        final CacheRecord record = new CacheRecord();
        record.setAccount(account);
        record.setAccessToken(accessToken);
        record.setRefreshToken(new RefreshTokenRecord());
        record.setIdToken(idToken);

        final OAuth2Strategy strategy = mock(OAuth2Strategy.class);
        when(strategy.validateCachedResult(any(), any())).thenReturn(true);
        final Authority authority = mock(CIAMAuthority.class);
        when(authority.createOAuth2Strategy(any())).thenReturn(strategy);
        final MsalOAuth2TokenCache cache = mock(MsalOAuth2TokenCache.class);
        when(cache.getAccountByLocalAccountId(any(), anyString(), any())).thenReturn(account);
        when(cache.loadWithAggregatedAccountData(
                anyString(), anyString(), isNull(), anyString(), same(account), any()))
                .thenReturn(Collections.singletonList(record));

        final SilentTokenCommandParameters parameters = SilentTokenCommandParameters.builder()
                .platformComponents(MockPlatformComponentsFactory.getNonFunctionalBuilder().build())
                .authority(authority)
                .clientId("test-client")
                .oAuth2TokenCache(cache)
                .account(account)
                .scopes(new HashSet<>(Collections.singleton("user.read")))
                .authenticationScheme(new BearerAuthenticationSchemeInternal())
                .sdkType(SdkType.MSAL)
                .claimsRequestJson("{\"id_token\":{\"xms_agvs\":{"
                        + "\"additionalProperties\":[\"target-mc\"]}}}")
                .build();

        try (final MockedStatic<LibraryConfiguration> configuration =
                     mockStatic(LibraryConfiguration.class);
             final MockedStatic<CommandDispatcher> dispatcher =
                     mockStatic(CommandDispatcher.class)) {
            configuration.when(LibraryConfiguration::getInstance).thenReturn(
                    LibraryConfiguration.builder().refreshInEnabled(refreshOn).build());
            try {
                controller.acquireTokenSilent(parameters);
                fail("A mismatched cached ID token must fail without automatic recovery");
            } catch (final ClientException exception) {
                assertEquals(ClientException.AGE_ASSURANCE_TARGET_MISMATCH,
                        exception.getErrorCode());
            }
            dispatcher.verifyNoInteractions();
            verify(strategy, never()).requestToken(any());
            verify(cache, never()).removeCredential(any());
        }
    }
}
