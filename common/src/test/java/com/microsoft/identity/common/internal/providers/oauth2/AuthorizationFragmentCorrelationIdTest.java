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
package com.microsoft.identity.common.internal.providers.oauth2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.WebView;

import androidx.fragment.app.FragmentActivity;
import androidx.test.core.app.ApplicationProvider;
import androidx.webkit.JavaScriptReplyProxy;
import androidx.webkit.ScriptHandler;
import androidx.webkit.WebMessageCompat;
import androidx.webkit.WebViewCompat;
import androidx.webkit.WebViewFeature;

import com.microsoft.identity.common.internal.broker.AuthUxJavaScriptInterface;
import com.microsoft.identity.common.internal.telemetry.OnboardingRecorderRegistry;
import com.microsoft.identity.common.internal.telemetry.OnboardingTelemetryRecorder;
import com.microsoft.identity.common.internal.ui.webview.AzureActiveDirectoryWebViewClient;
import com.microsoft.identity.common.internal.ui.webview.authux.AuthUxBridgeSession;
import com.microsoft.identity.common.internal.ui.webview.switchbrowser.SwitchBrowserProtocolCoordinator;
import com.microsoft.identity.common.java.logging.DiagnosticContext;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class AuthorizationFragmentCorrelationIdTest {
    private static final String CORRELATION_ID = "correlation-id";

    private static class TestAuthorizationFragment extends AuthorizationFragment {
    }

    @Before
    public void setUp() {
        OnboardingRecorderRegistry.clearForTest();
    }

    @After
    public void tearDown() {
        OnboardingRecorderRegistry.clearForTest();
        DiagnosticContext.INSTANCE.clear();
        ApplicationProvider.<Context>getApplicationContext().getSharedPreferences(
                "com.microsoft.oneauth.session_correlation_cache", Context.MODE_PRIVATE)
                .edit().clear().commit();
    }

    @Test
    public void testCorrelationIdRoundTripsThroughSavedState() {
        final TestAuthorizationFragment fragment = new TestAuthorizationFragment();
        fragment.extractState(state(CORRELATION_ID));
        final Bundle savedState = new Bundle();

        fragment.onSaveInstanceState(savedState);

        final TestAuthorizationFragment recreated = new TestAuthorizationFragment();
        recreated.extractState(savedState);
        assertEquals(CORRELATION_ID, recreated.getCorrelationId());
    }

    @Test
    public void testMissingCorrelationIdIsNotSaved() {
        final TestAuthorizationFragment fragment = new TestAuthorizationFragment();
        fragment.extractState(new Bundle());
        final Bundle savedState = new Bundle();

        fragment.onSaveInstanceState(savedState);

        assertFalse(savedState.containsKey(DiagnosticContext.CORRELATION_ID));
        assertNull(fragment.getCorrelationId());
    }

    @Test
    public void testWebViewAttachesMatchingRecorderBeforeBridgeInitialization() {
        final OnboardingTelemetryRecorder recorder = recorder();
        OnboardingRecorderRegistry.register(CORRELATION_ID, recorder);
        final WebViewAuthorizationFragment fragment = new WebViewAuthorizationFragment();
        fragment.extractState(state(CORRELATION_ID));
        final AzureActiveDirectoryWebViewClient client =
                Mockito.mock(AzureActiveDirectoryWebViewClient.class);
        final WebView webView = Mockito.mock(WebView.class);

        fragment.initializeAuthUxJavaScriptApi(client, webView, "https://login.microsoftonline.com");

        final InOrder ordered = inOrder(client);
        ordered.verify(client).setOnboardingTelemetryRecorder(recorder);
        ordered.verify(client).initializeAuthUxJavaScriptApi(
                webView,
                "https://login.microsoftonline.com"
        );
    }

    @Test
    public void testMissingRecorderAttachesNullBeforeBridgeInitialization() {
        final WebViewAuthorizationFragment fragment = new WebViewAuthorizationFragment();
        fragment.extractState(state("missing"));
        final AzureActiveDirectoryWebViewClient client =
                Mockito.mock(AzureActiveDirectoryWebViewClient.class);
        final WebView webView = Mockito.mock(WebView.class);

        fragment.initializeAuthUxJavaScriptApi(client, webView, "https://login.microsoftonline.com");

        final InOrder ordered = inOrder(client);
        ordered.verify(client).setOnboardingTelemetryRecorder(null);
        ordered.verify(client).initializeAuthUxJavaScriptApi(
                webView,
                "https://login.microsoftonline.com"
        );
    }

    private static Bundle state(final String correlationId) {
        final Bundle state = new Bundle();
        state.putString(DiagnosticContext.CORRELATION_ID, correlationId);
        return state;
    }

    @Test
    public void testRestoredRequestCorrelationReachesRegisteredBridges() throws Exception {
        for (final boolean useListener : new boolean[]{false, true}) {
            final OnboardingTelemetryRecorder recorder = recorder();
            OnboardingRecorderRegistry.register(CORRELATION_ID, recorder);
            final WebViewAuthorizationFragment original = new WebViewAuthorizationFragment();
            final Bundle launchState = state(CORRELATION_ID);
            launchState.putString(
                    com.microsoft.identity.common.adal.internal.AuthenticationConstants
                            .AuthorizationIntentKey.REDIRECT_URI,
                    "msauth://test");
            original.extractState(launchState);
            final Bundle savedState = new Bundle();
            original.onSaveInstanceState(savedState);

            final WebViewAuthorizationFragment restored = new WebViewAuthorizationFragment();
            restored.setSwitchBrowserProtocolCoordinator(
                    Mockito.mock(SwitchBrowserProtocolCoordinator.class));
            restored.extractState(savedState);
            final FragmentActivity activity = Robolectric.buildActivity(FragmentActivity.class).get();
            final AzureActiveDirectoryWebViewClient client = restored.createAADWebViewClient(activity);
            final WebView webView = Mockito.mock(WebView.class);

            try (final MockedStatic<WebViewFeature> features = Mockito.mockStatic(WebViewFeature.class);
                 final MockedStatic<WebViewCompat> webKit = Mockito.mockStatic(WebViewCompat.class)) {
                features.when(() -> WebViewFeature.isFeatureSupported(
                        WebViewFeature.WEB_MESSAGE_LISTENER)).thenReturn(useListener);
                features.when(() -> WebViewFeature.isFeatureSupported(
                        WebViewFeature.DOCUMENT_START_SCRIPT)).thenReturn(useListener);
                webKit.when(() -> WebViewCompat.addDocumentStartJavaScript(
                        eq(webView), anyString(), any()))
                        .thenReturn(Mockito.mock(ScriptHandler.class));
                try {
                    restored.initializeAuthUxJavaScriptApi(
                            client, webView, "https://login.microsoftonline.com");
                    final WebViewCompat.WebMessageListener listener;
                    final AuthUxJavaScriptInterface processor;
                    if (useListener) {
                        final ArgumentCaptor<WebViewCompat.WebMessageListener> captor =
                                ArgumentCaptor.forClass(WebViewCompat.WebMessageListener.class);
                        webKit.verify(() -> WebViewCompat.addWebMessageListener(
                                eq(webView), eq(AuthUxBridgeSession.PRIVATE_OBJECT_NAME),
                                any(), captor.capture()));
                        listener = captor.getValue();
                        processor = null;
                    } else {
                        final ArgumentCaptor<AuthUxJavaScriptInterface> captor =
                                ArgumentCaptor.forClass(AuthUxJavaScriptInterface.class);
                        Mockito.verify(webView).addJavascriptInterface(captor.capture(), eq("broker"));
                        processor = captor.getValue();
                        listener = null;
                    }

                    for (final String correlationId : new String[]{"another-request", CORRELATION_ID}) {
                        final String payload = "{\"correlationID\":\"" + correlationId
                                + "\",\"action_name\":\"log_telemetry\","
                                + "\"action_component\":\"page\",\"params\":{\"errorCode\":\"53003\"}}";
                        if (useListener) {
                            assertNotNull(listener);
                            listener.onPostMessage(
                                    webView, new WebMessageCompat(payload),
                                    Uri.parse("https://login.microsoftonline.com"), true,
                                    Mockito.mock(JavaScriptReplyProxy.class));
                        } else {
                            assertNotNull(processor);
                            processor.receiveAuthUxMessage(payload);
                        }
                        final JSONArray errors = new JSONObject(recorder.finalizeBlob())
                                .getJSONArray("blocking_errors");
                        assertEquals(CORRELATION_ID.equals(correlationId) ? 1 : 0, errors.length());
                        if (errors.length() != 0) {
                            assertEquals("53003", errors.getString(0));
                        }
                    }
                } finally {
                    client.closeAuthUxBridgeSession();
                }
            } finally {
                OnboardingRecorderRegistry.unregister(CORRELATION_ID, recorder);
                ApplicationProvider.<Context>getApplicationContext().getSharedPreferences(
                        "com.microsoft.oneauth.session_correlation_cache", Context.MODE_PRIVATE)
                        .edit().clear().commit();
            }
        }
    }

    private static OnboardingTelemetryRecorder recorder() {
        return new OnboardingTelemetryRecorder(
                "{\"schema_version\":\"1.0.0\","
                        + "\"session_correlation_id\":\"session\","
                        + "\"onboarding_mode\":\"non-brokered\"}",
                "client-id",
                "scope",
                ApplicationProvider.getApplicationContext()
        );
    }
}
