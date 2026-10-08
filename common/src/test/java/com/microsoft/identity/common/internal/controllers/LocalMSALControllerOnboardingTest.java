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
package com.microsoft.identity.common.internal.controllers;

import android.app.Activity;
import android.net.Uri;
import android.webkit.WebView;
import androidx.test.core.app.ApplicationProvider;
import androidx.webkit.JavaScriptReplyProxy;
import androidx.webkit.ScriptHandler;
import androidx.webkit.WebMessageCompat;
import androidx.webkit.WebViewCompat;
import androidx.webkit.WebViewFeature;
import com.microsoft.identity.common.internal.broker.AuthUxTelemetryWebMessageListener;
import com.microsoft.identity.common.internal.commands.parameters.AndroidInteractiveTokenCommandParameters;
import com.microsoft.identity.common.internal.telemetry.OnboardingRecorderRegistry;
import com.microsoft.identity.common.internal.telemetry.OnboardingTelemetryRecorder;
import com.microsoft.identity.common.internal.telemetry.OnboardingTelemetryRequest;
import com.microsoft.identity.common.internal.ui.webview.AzureActiveDirectoryWebViewClient;
import com.microsoft.identity.common.java.authorities.Authority;
import com.microsoft.identity.common.java.authscheme.AbstractAuthenticationScheme;
import com.microsoft.identity.common.java.authscheme.BearerAuthenticationSchemeInternal;
import com.microsoft.identity.common.java.cache.ICacheRecord;
import com.microsoft.identity.common.java.commands.parameters.InteractiveTokenCommandParameters;
import com.microsoft.identity.common.java.commands.parameters.TokenCommandParameters;
import com.microsoft.identity.common.java.controllers.ExceptionAdapter;
import com.microsoft.identity.common.java.dto.AccessTokenRecord;
import com.microsoft.identity.common.java.dto.AccountRecord;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.exception.BaseException;
import com.microsoft.identity.common.java.interfaces.IPlatformComponents;
import com.microsoft.identity.common.java.logging.DiagnosticContext;
import com.microsoft.identity.common.java.providers.microsoft.microsoftsts.MicrosoftStsAuthorizationRequest;
import com.microsoft.identity.common.java.providers.oauth2.AuthorizationRequest;
import com.microsoft.identity.common.java.providers.oauth2.AuthorizationResponse;
import com.microsoft.identity.common.java.providers.oauth2.AuthorizationResult;
import com.microsoft.identity.common.java.providers.oauth2.AuthorizationStatus;
import com.microsoft.identity.common.java.providers.oauth2.IAuthorizationStrategy;
import com.microsoft.identity.common.java.providers.oauth2.OAuth2Strategy;
import com.microsoft.identity.common.java.providers.oauth2.OAuth2TokenCache;
import com.microsoft.identity.common.java.providers.oauth2.TokenErrorResponse;
import com.microsoft.identity.common.java.providers.oauth2.TokenResponse;
import com.microsoft.identity.common.java.providers.oauth2.TokenResult;
import com.microsoft.identity.common.java.result.AcquireTokenResult;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.robolectric.RobolectricTestRunner;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Exercises the real local controller and OAuth2 authorization launch, without network or UI. */
@RunWith(RobolectricTestRunner.class)
@SuppressWarnings({"rawtypes", "unchecked"})
public class LocalMSALControllerOnboardingTest {
    private static final String SEED = "{\"schema_version\":\"1.0.0\","
            + "\"session_correlation_id\":\"seed-session\",\"onboarding_mode\":\"non-brokered\"}";
    private final UUID requestId = UUID.randomUUID();
    private final String staleId = UUID.randomUUID().toString();
    private MockedStatic<Authority> authorities;
    private Authority authority;
    private IPlatformComponents platform;
    private IAuthorizationStrategy authorizationStrategy;
    private OAuth2Strategy strategy;
    private Future<AuthorizationResult> authorizationFuture;
    private OnboardingTelemetryRecorder recorder;
    private TestController controller;
    private boolean expectSuppliedRecorder = true;

    @Before
    public void setUp() throws Exception {
        OnboardingRecorderRegistry.clearForTest();
        DiagnosticContext.INSTANCE.getRequestContext().put(DiagnosticContext.CORRELATION_ID, staleId);
        recorder = spy(new OnboardingTelemetryRecorder(
                SEED, "client", "scope", ApplicationProvider.getApplicationContext()));
        platform = mock(IPlatformComponents.class, RETURNS_DEEP_STUBS);
        authority = mock(Authority.class);
        strategy = mock(OAuth2Strategy.class);
        authorizationStrategy = mock(IAuthorizationStrategy.class);
        authorizationFuture = mock(Future.class);
        when(authority.createOAuth2Strategy(any())).thenReturn(strategy);
        when(platform.getAuthorizationStrategyFactory()
                .getAuthorizationStrategy(any(), any(), any(), eq(false)))
                .thenReturn(authorizationStrategy);
        final Authority.KnownAuthorityResult known = mock(Authority.KnownAuthorityResult.class);
        when(known.getKnown()).thenReturn(true);
        authorities = mockStatic(Authority.class);
        authorities.when(() -> Authority.getKnownAuthorityResult(authority)).thenReturn(known);
        doCallRealMethod().when(strategy).requestAuthorization(any(), any());
        when(authorizationStrategy.requestAuthorization(any(), same(strategy))).thenAnswer(call -> {
            assertSame(controller.request, call.getArgument(0));
            final OnboardingTelemetryRecorder live = OnboardingRecorderRegistry.get(requestId.toString());
            assertNotNull(live);
            if (expectSuppliedRecorder) {
                assertSame(recorder, live);
                verify(recorder, never()).finalizeBlob();
            }
            assertNull(OnboardingRecorderRegistry.get("seed-session"));
            assertNull(OnboardingRecorderRegistry.get(staleId));
            live.addStep("authorization-view");
            return authorizationFuture;
        });
        controller = new TestController();
    }

    @After
    public void tearDown() {
        if (authorities != null) authorities.close();
        OnboardingRecorderRegistry.clearForTest();
        DiagnosticContext.INSTANCE.clear();
        Thread.interrupted();
    }

    @Test
    public void suppliedRecorderSurvivesAuthorizationAndTokenSuccess() throws Exception {
        authorization(AuthorizationStatus.SUCCESS);
        controller.tokenResult = new TokenResult(new TokenResponse());
        final AcquireTokenResult result = controller.acquireToken(parameters(recorder));
        assertTrue(result.getSucceeded());
        assertNotNull(result.getLocalAuthenticationResult());
        assertEquals(2, new JSONObject(result.getOnboardingBlob()).getJSONArray("steps_list").length());
        verify(recorder, times(1)).finalizeBlob();
        verify(authorizationStrategy).requestAuthorization(same(controller.request), same(strategy));
        assertEquals(0, OnboardingRecorderRegistry.size());
    }

    @Test
    public void cancellationAndAuthorizationErrorFinalizeTheSameRecorder() throws Exception {
        for (final AuthorizationStatus status : new AuthorizationStatus[]{
                AuthorizationStatus.USER_CANCEL, AuthorizationStatus.SDK_CANCEL, AuthorizationStatus.FAIL}) {
            clearInvocations(recorder);
            authorization(status);
            final AcquireTokenResult result = controller.acquireToken(parameters(recorder));
            assertEquals(status, result.getAuthorizationResult().getAuthorizationStatus());
            assertNotNull(result.getOnboardingBlob());
            assertFalse(controller.tokenRequested);
            verify(recorder, times(1)).finalizeBlob();
            assertEquals(0, OnboardingRecorderRegistry.size());
        }
    }

    @Test
    public void tokenEndpointErrorFinalizesAfterTokenExchange() throws Exception {
        authorization(AuthorizationStatus.SUCCESS);
        controller.tokenResult = new TokenResult(new TokenErrorResponse());
        final AcquireTokenResult result = controller.acquireToken(parameters(recorder));
        assertFalse(result.getSucceeded());
        assertNotNull(result.getOnboardingBlob());
        assertTrue(controller.tokenRequested);
        verify(recorder, times(1)).finalizeBlob();
        assertEquals(0, OnboardingRecorderRegistry.size());
    }

    @Test
    public void launchDomainFailureCarriesBlobAndCleansRegistration() throws Exception {
        final ClientException failure = new ClientException("launch_failed");
        doThrow(failure).when(authorizationStrategy).requestAuthorization(any(), same(strategy));
        try {
            controller.acquireToken(parameters(recorder));
            fail("Expected launch failure");
        } catch (final ClientException actual) {
            assertSame(failure, actual);
            assertNotNull(actual.getOnboardingBlob());
        }
        verify(recorder, times(1)).finalizeBlob();
        assertEquals(0, OnboardingRecorderRegistry.size());
    }

    @Test
    public void tokenDomainFailureCarriesBlobAndCleansRegistration() throws Exception {
        authorization(AuthorizationStatus.SUCCESS);
        controller.domainFailure = new ClientException("token_failed");
        try {
            controller.acquireToken(parameters(recorder));
            fail("Expected token failure");
        } catch (final ClientException actual) {
            assertSame(controller.domainFailure, actual);
            assertNotNull(actual.getOnboardingBlob());
        }
        assertEquals(0, OnboardingRecorderRegistry.size());
    }

    @Test
    public void executionFailurePreservesDomainCauseAndBlob() throws Exception {
        final ClientException cause = new ClientException("future_failed");
        final ExecutionException failure = new ExecutionException(cause);
        when(authorizationFuture.get()).thenThrow(failure);
        try {
            controller.acquireToken(parameters(recorder));
            fail("Expected future failure");
        } catch (final ExecutionException actual) {
            assertSame(failure, actual);
            assertSame(cause, actual.getCause());
            assertNotNull(cause.getOnboardingBlob());
        }
        assertEquals(0, OnboardingRecorderRegistry.size());
    }

    @Test
    public void checkedIoFailurePreservesPublicClassificationAndCarriesBlob() throws Exception {
        authorization(AuthorizationStatus.SUCCESS);
        controller.ioFailure = new IOException("token_io");
        try {
            controller.acquireToken(parameters(recorder));
            fail("Expected I/O failure");
        } catch (final ExecutionException actual) {
            assertMappedFailure(actual, controller.ioFailure);
        }
        verify(recorder, times(1)).finalizeBlob();
        assertEquals(0, OnboardingRecorderRegistry.size());
    }

    @Test
    public void ownedInterruptionFinalizesAndCleansRegistration() throws Exception {
        final InterruptedException failure = new InterruptedException("interrupted");
        when(authorizationFuture.get()).thenThrow(failure);
        try {
            controller.acquireToken(parameters(recorder));
            fail("Expected interruption");
        } catch (final ExecutionException actual) {
            assertMappedFailure(actual, failure);
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }
        verify(recorder, times(1)).finalizeBlob();
        assertEquals(0, OnboardingRecorderRegistry.size());
    }

    @Test
    public void uncheckedLaunchFailureFinalizesAndCleansRegistration() throws Exception {
        final IllegalStateException failure = new IllegalStateException("launch_failed");
        doThrow(failure).when(authorizationStrategy).requestAuthorization(any(), same(strategy));
        try {
            controller.acquireToken(parameters(recorder));
            fail("Expected launch failure");
        } catch (final ExecutionException actual) {
            assertMappedFailure(actual, failure);
        }
        verify(recorder, times(1)).finalizeBlob();
        assertEquals(0, OnboardingRecorderRegistry.size());
    }

    @Test
    public void nonDomainFutureFailureUsesExistingClassificationAndCarriesBlob() throws Exception {
        final ExecutionException failure = new ExecutionException(new IOException("future_io"));
        when(authorizationFuture.get()).thenThrow(failure);
        try {
            controller.acquireToken(parameters(recorder));
            fail("Expected future failure");
        } catch (final ExecutionException actual) {
            assertMappedFailure(actual, failure);
        }
        verify(recorder, times(1)).finalizeBlob();
        assertEquals(0, OnboardingRecorderRegistry.size());
    }

    @Test
    public void externalOwnerIsRetainedAcrossInterruptionAndFinalizesLater() throws Exception {
        final OnboardingTelemetryRequest externalOwner = new OnboardingTelemetryRequest(recorder);
        try {
            assertTrue(externalOwner.register(requestId.toString()));
            final InterruptedException interruption = new InterruptedException("remediation");
            when(authorizationFuture.get()).thenThrow(interruption);
            try {
                controller.acquireToken(parameters(null));
                fail("Expected interruption");
            } catch (final InterruptedException actual) {
                assertSame(interruption, actual);
            }
            verify(recorder, never()).finalizeBlob();
            assertSame(recorder, OnboardingRecorderRegistry.get(requestId.toString()));

            authorization(AuthorizationStatus.SUCCESS);
            controller.tokenResult = new TokenResult(new TokenResponse());
            final AcquireTokenResult result = controller.acquireToken(parameters(recorder));
            assertNull(result.getOnboardingBlob());
            assertSame(recorder, OnboardingRecorderRegistry.get(requestId.toString()));
            externalOwner.complete(result);
            assertNotNull(result.getOnboardingBlob());
            verify(recorder, times(1)).finalizeBlob();
            assertEquals(0, OnboardingRecorderRegistry.size());
        } finally {
            externalOwner.close();
        }
    }

    @Test
    public void suppliedExternallyRegisteredRecorderIsNotFinalizedOnDomainFailure() throws Exception {
        final OnboardingTelemetryRequest externalOwner = new OnboardingTelemetryRequest(recorder);
        try {
            assertTrue(externalOwner.register(requestId.toString()));
            final ClientException failure = new ClientException("external_launch");
            doThrow(failure).when(authorizationStrategy).requestAuthorization(any(), same(strategy));
            try {
                controller.acquireToken(parameters(recorder));
                fail("Expected launch failure");
            } catch (final ClientException actual) {
                assertSame(failure, actual);
                assertNull(actual.getOnboardingBlob());
                verify(recorder, never()).finalizeBlob();
                assertSame(recorder, OnboardingRecorderRegistry.get(requestId.toString()));
                externalOwner.complete(actual);
                assertNotNull(actual.getOnboardingBlob());
                verify(recorder, times(1)).finalizeBlob();
            }
        } finally {
            externalOwner.close();
        }
        assertEquals(0, OnboardingRecorderRegistry.size());
    }

    @Test
    public void externallyOwnedIoFailureRetainsOriginalException() throws Exception {
        final OnboardingTelemetryRequest externalOwner = new OnboardingTelemetryRequest(recorder);
        try {
            assertTrue(externalOwner.register(requestId.toString()));
            authorization(AuthorizationStatus.SUCCESS);
            controller.ioFailure = new IOException("external_io");
            try {
                controller.acquireToken(parameters(recorder));
                fail("Expected I/O failure");
            } catch (final IOException actual) {
                assertSame(controller.ioFailure, actual);
            }
            verify(recorder, never()).finalizeBlob();
            assertSame(recorder, OnboardingRecorderRegistry.get(requestId.toString()));
        } finally {
            externalOwner.close();
        }
    }

    @Test
    public void uninstrumentedRuntimeFailureRetainsOriginalException() throws Exception {
        final IllegalStateException failure = new IllegalStateException("launch_failed");
        doThrow(failure).when(authorizationStrategy).requestAuthorization(any(), same(strategy));
        try {
            controller.acquireToken(parameters(null).toBuilder().onboardingSeedJson("").build());
            fail("Expected launch failure");
        } catch (final IllegalStateException actual) {
            assertSame(failure, actual);
        }
        verify(recorder, never()).finalizeBlob();
        assertEquals(0, OnboardingRecorderRegistry.size());
    }

    private void assertMappedFailure(final ExecutionException wrapper, final Exception original) {
        final BaseException expected = ExceptionAdapter.baseExceptionFromException(original);
        final BaseException actual = ExceptionAdapter.baseExceptionFromException(wrapper);
        assertSame(wrapper.getCause(), actual);
        assertEquals(expected.getErrorCode(), actual.getErrorCode());
        assertSame(expected.getCause(), actual.getCause());
        assertNotNull(actual.getOnboardingBlob());
    }

    @Test
    public void seedCreatesRecorderOnlyWhenNoSuppliedOrExternalRecorderExists() throws Exception {
        expectSuppliedRecorder = false;
        authorization(AuthorizationStatus.USER_CANCEL);
        final Activity activity = mock(Activity.class);
        when(activity.getApplicationContext()).thenReturn(ApplicationProvider.getApplicationContext());
        final AcquireTokenResult result = controller.acquireToken(
                parameters(null).toBuilder().activity(activity).build());
        assertEquals("seed-session", new JSONObject(result.getOnboardingBlob())
                .getString("session_correlation_id"));
        verify(recorder, never()).finalizeBlob();
        assertEquals(0, OnboardingRecorderRegistry.size());
    }

    @Test
    public void telemetryFinalizationFailureDoesNotChangeCancellation() throws Exception {
        authorization(AuthorizationStatus.USER_CANCEL);
        doThrow(new IllegalStateException("serialization")).when(recorder).finalizeBlob();
        final AcquireTokenResult result = controller.acquireToken(parameters(recorder));
        assertEquals(AuthorizationStatus.USER_CANCEL,
                result.getAuthorizationResult().getAuthorizationStatus());
        assertNull(result.getOnboardingBlob());
        assertEquals(0, OnboardingRecorderRegistry.size());
    }

    @Test
    public void missingSeedAndRecorderLeaveAuthenticationUninstrumented() throws Exception {
        authorization(AuthorizationStatus.USER_CANCEL);
        doAnswer(call -> {
            assertNull(OnboardingRecorderRegistry.get(requestId.toString()));
            return authorizationFuture;
        }).when(authorizationStrategy).requestAuthorization(any(), same(strategy));
        final AcquireTokenResult result = controller.acquireToken(
                parameters(null).toBuilder().onboardingSeedJson("").build());
        assertEquals(AuthorizationStatus.USER_CANCEL,
                result.getAuthorizationResult().getAuthorizationStatus());
        assertNull(result.getOnboardingBlob());
    }

    @Test
    public void registryCapacityNeverEvictsOtherRequestsOrFailsAuthentication() throws Exception {
        final OnboardingTelemetryRecorder activeRecorder = new OnboardingTelemetryRecorder(
                SEED, "other-client", "scope", ApplicationProvider.getApplicationContext());
        for (int index = 0; index < 16; index++) {
            OnboardingRecorderRegistry.register("active-" + index, activeRecorder);
        }
        authorization(AuthorizationStatus.USER_CANCEL);
        doAnswer(call -> {
            assertNull(OnboardingRecorderRegistry.get(requestId.toString()));
            return authorizationFuture;
        }).when(authorizationStrategy).requestAuthorization(any(), same(strategy));
        final AcquireTokenResult result = controller.acquireToken(parameters(recorder));
        assertNotNull(result.getOnboardingBlob());
        assertEquals(16, OnboardingRecorderRegistry.size());
        for (int index = 0; index < 16; index++) {
            assertSame(activeRecorder, OnboardingRecorderRegistry.get("active-" + index));
        }
    }

    @Test
    public void launchedBridgeRetainsSameOwnerAcrossViewsAndIgnoresDetachedCallbacks() throws Exception {
        assertBridgeOwnerContract("non-brokered", false);
    }

    /** API integration only: this does not exercise or claim wiring in an external broker host. */
    @Test
    public void brokerModeExternalOwnerApiRetainsBridgeEventsUntilExplicitCompletion() throws Exception {
        assertBridgeOwnerContract("brokered", true);
    }

    /** API integration only: external non-brokered/OneAuth hosts are not present in this repository. */
    @Test
    public void nonbrokerModeExternalOwnerApiRetainsBridgeEventsUntilExplicitCompletion() throws Exception {
        assertBridgeOwnerContract("non-brokered", true);
    }

    private void assertBridgeOwnerContract(final String mode, final boolean externallyOwned) throws Exception {
        recorder = spy(new OnboardingTelemetryRecorder(
                SEED.replace("non-brokered", mode),
                "client", "scope", ApplicationProvider.getApplicationContext()));
        final OnboardingTelemetryRequest externalOwner =
                externallyOwned ? new OnboardingTelemetryRequest(recorder) : null;
        authorization(AuthorizationStatus.SUCCESS);
        controller.tokenResult = new TokenResult(new TokenResponse());
        // Skip unrelated certificate/UI construction, but execute the production bridge methods.
        final AzureActiveDirectoryWebViewClient client =
                mock(AzureActiveDirectoryWebViewClient.class, CALLS_REAL_METHODS);
        final Activity activity = mock(Activity.class);
        when(activity.getApplicationContext()).thenReturn(ApplicationProvider.getApplicationContext());
        doReturn(activity).when(client).getActivity();
        final WebView firstView = mock(WebView.class);
        final WebView resumedView = mock(WebView.class);
        final ScriptHandler script = mock(ScriptHandler.class);
        final AtomicReference<WebViewCompat.WebMessageListener> registered = new AtomicReference<>();

        try (MockedStatic<WebViewFeature> features = mockStatic(WebViewFeature.class);
             MockedStatic<WebViewCompat> compat = mockStatic(WebViewCompat.class)) {
            if (externalOwner != null) {
                assertTrue(externalOwner.register(requestId.toString()));
            }
            features.when(() -> WebViewFeature.isFeatureSupported(anyString())).thenReturn(true);
            compat.when(() -> WebViewCompat.addWebMessageListener(
                    any(), anyString(), anySet(), any())).thenAnswer(call -> {
                        registered.set(call.getArgument(3));
                        return null;
                    });
            compat.when(() -> WebViewCompat.addDocumentStartJavaScript(
                    any(), anyString(), anySet())).thenReturn(script);
            compat.when(() -> WebViewCompat.removeWebMessageListener(any(), anyString()))
                    .thenThrow(new IllegalStateException("native cleanup failed"));

            doAnswer(call -> {
                assertSame(recorder, OnboardingRecorderRegistry.get(requestId.toString()));
                client.setOnboardingTelemetryRecorder(
                        OnboardingRecorderRegistry.get(requestId.toString()));
                client.initializeAuthUxJavaScriptApi(firstView, "https://login.microsoftonline.com");
                final WebViewCompat.WebMessageListener oldListener = registered.get();
                assertTrue(oldListener instanceof AuthUxTelemetryWebMessageListener);
                postBlockingError(oldListener, firstView);
                client.removeAuthUxTelemetryWebMessageApi();
                postBlockingError(oldListener, firstView);
                assertSame(recorder, OnboardingRecorderRegistry.get(requestId.toString()));
                verify(recorder, never()).finalizeBlob();

                client.initializeAuthUxJavaScriptApi(resumedView, "https://login.microsoftonline.com");
                postBlockingError(oldListener, firstView);
                postBlockingError(registered.get(), resumedView);
                client.removeAuthUxTelemetryWebMessageApi();
                verify(recorder, never()).finalizeBlob();
                return authorizationFuture;
            }).when(authorizationStrategy).requestAuthorization(any(), same(strategy));

            final AcquireTokenResult result = controller.acquireToken(parameters(recorder));
            assertTrue(result.getSucceeded());
            if (externalOwner != null) {
                assertNull(result.getOnboardingBlob());
                verify(recorder, never()).finalizeBlob();
                assertSame(recorder, OnboardingRecorderRegistry.get(requestId.toString()));
                externalOwner.complete(result);
            }
            assertEquals(mode, new JSONObject(result.getOnboardingBlob()).getString("onboarding_mode"));
            assertEquals(2, new JSONObject(result.getOnboardingBlob())
                    .getJSONArray("blocking_errors").length());
            postBlockingError(registered.get(), resumedView);
            verify(recorder, times(2)).addBlockingError("530003");
            verify(recorder, times(1)).finalizeBlob();
            verify(script, times(2)).remove();
            assertEquals(0, OnboardingRecorderRegistry.size());
        } finally {
            client.removeAuthUxTelemetryWebMessageApi();
            if (externalOwner != null) {
                externalOwner.close();
            }
        }
    }

    private void postBlockingError(final WebViewCompat.WebMessageListener listener,
                                  final WebView view) {
        listener.onPostMessage(view, new WebMessageCompat(
                        "{\"correlationID\":\"page-correlation\",\"action_name\":\"log_telemetry\","
                                + "\"action_component\":\"broker\",\"params\":{\"errorCode\":\"530003\"}}"),
                Uri.parse("https://login.microsoftonline.com"), true,
                mock(JavaScriptReplyProxy.class));
    }

    private AndroidInteractiveTokenCommandParameters parameters(
            final OnboardingTelemetryRecorder supplied) {
        return AndroidInteractiveTokenCommandParameters.builder()
                .platformComponents(platform)
                .authority(authority)
                .clientId("client")
                .correlationId(UUID.randomUUID().toString())
                .scopes(new HashSet<>(Collections.singleton("scope")))
                .authenticationScheme(new BearerAuthenticationSchemeInternal())
                .onboardingSeedJson(SEED)
                .onboardingTelemetryRecorder(supplied)
                .build();
    }

    private void authorization(final AuthorizationStatus status) throws Exception {
        final AuthorizationResult result = mock(AuthorizationResult.class);
        when(result.getAuthorizationStatus()).thenReturn(status);
        when(result.getAuthorizationResponse()).thenReturn(mock(AuthorizationResponse.class));
        doReturn(result).when(authorizationFuture).get();
    }

    private final class TestController extends LocalMSALController {
        private final MicrosoftStsAuthorizationRequest request = mock(MicrosoftStsAuthorizationRequest.class);
        private TokenResult tokenResult;
        private ClientException domainFailure;
        private IOException ioFailure;
        private boolean tokenRequested;

        private TestController() {
            when(request.getCorrelationId()).thenReturn(requestId);
        }

        @Override
        protected AuthorizationRequest getAuthorizationRequest(
                final OAuth2Strategy strategy, final TokenCommandParameters parameters) {
            return request;
        }

        @Override
        protected TokenResult performTokenRequest(final OAuth2Strategy strategy,
                final AuthorizationRequest request, final AuthorizationResponse response,
                final InteractiveTokenCommandParameters parameters) throws IOException, ClientException {
            tokenRequested = true;
            assertSame(recorder, OnboardingRecorderRegistry.get(requestId.toString()));
            verify(recorder, never()).finalizeBlob();
            recorder.addStep("token-exchange");
            if (domainFailure != null) throw domainFailure;
            if (ioFailure != null) throw ioFailure;
            return tokenResult;
        }

        @Override
        protected List<ICacheRecord> saveTokens(final OAuth2Strategy strategy,
                final AuthorizationRequest request, final TokenResponse response,
                final OAuth2TokenCache cache) {
            final ICacheRecord record = mock(ICacheRecord.class);
            when(record.getAccessToken()).thenReturn(new AccessTokenRecord());
            when(record.getAccount()).thenReturn(new AccountRecord());
            return Collections.singletonList(record);
        }

        @Override
        public ICacheRecord finalizeCacheRecordForResult(final ICacheRecord record,
                final AbstractAuthenticationScheme scheme) {
            return record;
        }
    }
}
