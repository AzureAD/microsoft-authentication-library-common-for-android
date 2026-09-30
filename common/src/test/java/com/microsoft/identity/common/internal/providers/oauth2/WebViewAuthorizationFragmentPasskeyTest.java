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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;

import android.os.Build;
import android.os.Bundle;
import android.webkit.WebView;
import android.widget.ProgressBar;

import com.microsoft.identity.common.internal.telemetry.OnboardingRecorderRegistry;
import com.microsoft.identity.common.internal.telemetry.OnboardingTelemetryRecorder;
import com.microsoft.identity.common.internal.ui.webview.AzureActiveDirectoryWebViewClient;
import com.microsoft.identity.common.java.logging.DiagnosticContext;
import com.microsoft.identity.common.java.providers.RawAuthorizationResult;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.MockedStatic;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;

import java.util.HashMap;

/**
 * Tests passkey listener cleanup during the authorization WebView lifecycle.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.P)
public class WebViewAuthorizationFragmentPasskeyTest {

    @After
    public void clearRegistry() {
        OnboardingRecorderRegistry.clearForTest();
    }

    @Test
    public void launchWebView_attachesSameRecorderBeforeInitializationAndFirstLoad() {
        assertRecorderAttachedBeforeLoad(false);
        assertRecorderAttachedBeforeLoad(true);
    }

    private void assertRecorderAttachedBeforeLoad(final boolean brokered) {
        final WebViewAuthorizationFragment fragment = new WebViewAuthorizationFragment();
        final WebView webView = mock(WebView.class);
        final AzureActiveDirectoryWebViewClient client = mock(AzureActiveDirectoryWebViewClient.class);
        final OnboardingTelemetryRecorder recorder = mock(OnboardingTelemetryRecorder.class);
        final String correlationId = brokered ? "broker-request" : "app-request";
        final String url = "https://login.microsoftonline.com/authorize";
        final HashMap<String, String> headers = new HashMap<>();
        OnboardingRecorderRegistry.register(correlationId, recorder);
        ReflectionHelpers.setField(fragment, "mCorrelationId", correlationId);
        ReflectionHelpers.setField(fragment, "isBrokerRequest", brokered);
        ReflectionHelpers.setField(fragment, "mWebView", webView);
        ReflectionHelpers.setField(fragment, "mAADWebViewClient", client);
        ReflectionHelpers.setField(fragment, "mProgressBar", mock(ProgressBar.class));
        doAnswer(invocation -> {
            ((Runnable) invocation.getArgument(0)).run();
            return true;
        }).when(webView).post(any(Runnable.class));

        fragment.launchWebView(url, headers);

        final InOrder order = inOrder(client, webView);
        order.verify(client).setOnboardingTelemetryRecorder(recorder);
        order.verify(client).initializeAuthUxJavaScriptApi(webView, url);
        order.verify(webView).loadUrl(url, headers);
        fragment.onDestroyView();
        assertSame(recorder, OnboardingRecorderRegistry.get(correlationId));
        // View recreation reattaches by the saved request ID, without finalizing the owner.
        final Bundle state = new Bundle();
        fragment.onSaveInstanceState(state);
        assertEquals(correlationId, state.getString(DiagnosticContext.CORRELATION_ID));
    }

    @Test
    public void sendResult_detachesBridgeBeforeNotifyingRequestOwner() {
        final WebViewAuthorizationFragment fragment = new WebViewAuthorizationFragment();
        final AzureActiveDirectoryWebViewClient client = mock(AzureActiveDirectoryWebViewClient.class);
        ReflectionHelpers.setField(fragment, "mAADWebViewClient", client);
        doAnswer(invocation -> {
            assertFalse(ReflectionHelpers.getField(fragment, "mAuthResultSent"));
            return null;
        }).when(client).removeAuthUxTelemetryWebMessageApi();

        fragment.sendResult(RawAuthorizationResult.fromResultCode(
                RawAuthorizationResult.ResultCode.CANCELLED));

        verify(client).removeAuthUxTelemetryWebMessageApi();
        assertTrue(ReflectionHelpers.getField(fragment, "mAuthResultSent"));
    }

    @Test
    public void onDestroyView_whenPasskeyListenerHooked_unhooksListener() {
        final WebViewAuthorizationFragment fragment = new WebViewAuthorizationFragment();
        final WebView webView = mock(WebView.class);
        ReflectionHelpers.setField(fragment, "mWebView", webView);
        ReflectionHelpers.setField(fragment, "mPasskeyWebListenerHooked", true);

        try (final MockedStatic<PasskeyWebListener> passkeyWebListener = mockStatic(PasskeyWebListener.class)) {
            fragment.onDestroyView();

            passkeyWebListener.verify(() -> PasskeyWebListener.unhook(webView));
        }
        assertFalse(ReflectionHelpers.getField(fragment, "mPasskeyWebListenerHooked"));
    }

    @Test
    public void onDestroyView_whenPasskeyListenerNotHooked_doesNotUnhookListener() {
        final WebViewAuthorizationFragment fragment = new WebViewAuthorizationFragment();
        ReflectionHelpers.setField(fragment, "mWebView", mock(WebView.class));

        try (final MockedStatic<PasskeyWebListener> passkeyWebListener = mockStatic(PasskeyWebListener.class)) {
            fragment.onDestroyView();

            passkeyWebListener.verifyNoInteractions();
        }
    }

    @Test
    public void onDestroyView_removesAuthUxTelemetryWebMessageApi() {
        final WebViewAuthorizationFragment fragment = new WebViewAuthorizationFragment();
        final AzureActiveDirectoryWebViewClient client =
                mock(AzureActiveDirectoryWebViewClient.class);
        ReflectionHelpers.setField(fragment, "mAADWebViewClient", client);

        fragment.onDestroyView();

        verify(client).removeAuthUxTelemetryWebMessageApi();
    }
}
