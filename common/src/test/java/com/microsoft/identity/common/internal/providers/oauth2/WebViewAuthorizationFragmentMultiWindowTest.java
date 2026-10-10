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
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.content.Context;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;

import androidx.fragment.app.FragmentActivity;
import androidx.test.core.app.ApplicationProvider;

import com.microsoft.identity.common.R;
import com.microsoft.identity.common.adal.internal.AuthenticationConstants;
import com.microsoft.identity.common.internal.mocks.MockCommonFlightsManager;
import com.microsoft.identity.common.internal.ui.webview.AzureActiveDirectoryWebViewClient;
import com.microsoft.identity.common.java.flighting.CommonFlight;
import com.microsoft.identity.common.java.flighting.CommonFlightsManager;
import com.microsoft.identity.common.java.flighting.IFlightsProvider;
import com.microsoft.identity.common.java.logging.DiagnosticContext;
import com.microsoft.identity.common.java.logging.IRequestContext;
import com.microsoft.identity.common.java.logging.RequestContext;
import com.microsoft.identity.common.java.opentelemetry.AttributeName;
import com.microsoft.identity.common.java.opentelemetry.OTelUtility;
import com.microsoft.identity.common.java.opentelemetry.SpanName;
import com.microsoft.identity.common.logging.Logger;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.InOrder;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.util.ReflectionHelpers;

import java.util.Locale;
import java.util.HashMap;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.common.Attributes;

/**
 * Tests for the multi-window (target=_blank) URL handling logic in
 * {@link WebViewAuthorizationFragment}.
 */
@RunWith(RobolectricTestRunner.class)
public class WebViewAuthorizationFragmentMultiWindowTest {

    private WebViewAuthorizationFragment mFragment;
    private Context mContext;

    private static final String HTTP_URL = "http://example.com/tlr/start";
    private static final String FTP_URL = "ftp://files.example.com/document.pdf";

    // Target URLs for handleInterceptedUrlFromNewWindow
    private static final String HTTPS_TARGET_URL = "https://terms.example.com/privacy";
    private static final String HTTP_TARGET_URL = "http://terms.example.com/privacy";
    private static final String OPENID_VC_TARGET_URL = "openid-vc://authorize?request_uri=https%3A%2F%2Fexample.com";
    private static final String HOSTLESS_TARGET_URL = "custom-scheme:path";

    @Before
    public void setUp() throws Exception {
        mContext = ApplicationProvider.getApplicationContext();
        mFragment = new WebViewAuthorizationFragment();
    }

    @After
    public void tearDown() {
        CommonFlightsManager.INSTANCE.resetFlightsManager();
    }

    private WebResourceRequest mockRequest(final String url) {
        final WebResourceRequest request = mock(WebResourceRequest.class);
        when(request.getUrl()).thenReturn(Uri.parse(url));
        return request;
    }

    private Span mockSpan() {
        return mock(Span.class);
    }

    private WebChromeClient setUpWindowClient(final WebView mainWebView) {
        setOpenIdVcRedirectFlightEnabled(false);
        when(mainWebView.getSettings()).thenReturn(mock(WebSettings.class));
        when(mainWebView.getContext()).thenReturn(mContext);
        final View rootView = mock(View.class);
        when(rootView.findViewById(R.id.common_auth_webview)).thenReturn(mainWebView);
        ReflectionHelpers.setField(mFragment, "mRequestHeaders", new HashMap<String, String>());
        ReflectionHelpers.callInstanceMethod(mFragment, "setUpWebView",
                ReflectionHelpers.ClassParameter.from(View.class, rootView),
                ReflectionHelpers.ClassParameter.from(AzureActiveDirectoryWebViewClient.class,
                        mock(AzureActiveDirectoryWebViewClient.class)));
        final ArgumentCaptor<WebChromeClient> client = ArgumentCaptor.forClass(WebChromeClient.class);
        verify(mainWebView).setWebChromeClient(client.capture());
        return client.getValue();
    }

    @Test
    public void testOnCreateWindow_hostUnavailable_logsWithoutTelemetry() {
        final WebView mainWebView = mock(WebView.class);
        final WebChromeClient client = setUpWindowClient(mainWebView);
        try (final MockedStatic<OTelUtility> telemetry = mockStatic(OTelUtility.class);
             final MockedStatic<Logger> logger = mockStatic(Logger.class)) {
            assertFalse(client.onCreateWindow(mainWebView, false, true, null));
            logger.verify(() -> Logger.error(ArgumentMatchers.anyString(),
                    eq("onCreateWindow: authorization host is unavailable."), ArgumentMatchers.isNull()));
            telemetry.verifyNoInteractions();
        }
    }

    @Test
    public void testOnCreateWindow_invalidTransport_logsWithoutTelemetry() {
        mFragment = spy(mFragment);
        when(mFragment.getActivity()).thenReturn(mock(FragmentActivity.class));
        final WebView mainWebView = mock(WebView.class);
        final WebChromeClient client = setUpWindowClient(mainWebView);
        final Message invalidMessage = Message.obtain();
        invalidMessage.obj = new Object();
        try (final MockedStatic<OTelUtility> telemetry = mockStatic(OTelUtility.class);
             final MockedStatic<Logger> logger = mockStatic(Logger.class)) {
            assertFalse(client.onCreateWindow(mainWebView, false, true, null));
            assertFalse(client.onCreateWindow(mainWebView, false, true, invalidMessage));
            logger.verify(() -> Logger.error(ArgumentMatchers.anyString(),
                    eq("onCreateWindow: missing or invalid WebView transport."),
                    ArgumentMatchers.isNull()), times(2));
            telemetry.verifyNoInteractions();
        }
    }

    @Test
    public void testOnCreateWindow_creationThrows_logsFailureWithoutTelemetry() {
        mFragment = spy(mFragment);
        when(mFragment.getActivity()).thenReturn(mock(FragmentActivity.class));
        final WebView mainWebView = mock(WebView.class);
        final WebChromeClient client = setUpWindowClient(mainWebView);
        final IllegalStateException failure = new IllegalStateException("Window creation failed.");
        when(mainWebView.getContext()).thenThrow(failure);
        final Message message = Message.obtain();
        message.obj = mainWebView.new WebViewTransport();
        try (final MockedStatic<OTelUtility> telemetry = mockStatic(OTelUtility.class);
             final MockedStatic<Logger> logger = mockStatic(Logger.class)) {
            assertFalse(client.onCreateWindow(mainWebView, false, true, message));
            logger.verify(() -> Logger.error(ArgumentMatchers.anyString(),
                    eq("Error handling target=_blank navigation."),
                    eq(failure)));
            telemetry.verifyNoInteractions();
        }
    }

    @Test
    public void testOnCreateWindow_createsAndEndsSpanOnlyOnNavigation() {
        mFragment = spy(mFragment);
        final String correlationId = "11111111-1111-4111-8111-111111111111";
        ReflectionHelpers.setField(mFragment, "mCorrelationId", correlationId);
        final AuthorizationActivity host = mock(AuthorizationActivity.class);
        final SpanContext parentSpanContext = SpanContext.getInvalid();
        when(host.getSpanContext()).thenReturn(parentSpanContext);
        when(mFragment.getActivity()).thenReturn(host);
        final WebView mainWebView = mock(WebView.class);
        final WebChromeClient client = setUpWindowClient(mainWebView);
        final WebView.WebViewTransport transport = mainWebView.new WebViewTransport();
        final Message message = Message.obtain(new Handler(Looper.getMainLooper()), 0, transport);
        final Span span = mockSpan();
        final IRequestContext previousContext = DiagnosticContext.INSTANCE.getRequestContext();
        try (final MockedStatic<OTelUtility> telemetry = mockStatic(OTelUtility.class)) {
            final RequestContext otherRequestContext = new RequestContext();
            otherRequestContext.put(DiagnosticContext.CORRELATION_ID,
                    "22222222-2222-4222-8222-222222222222");
            DiagnosticContext.INSTANCE.setRequestContext(otherRequestContext);
            telemetry.when(() -> OTelUtility.createSpanFromParent(
                    SpanName.WebViewTargetBlankNavigation.name(), parentSpanContext)).thenReturn(span);

            assertTrue(client.onCreateWindow(mainWebView, false, false, message));
            telemetry.verifyNoInteractions();
            verify(span, never()).end();

            final WebView interceptor = transport.getWebView();
            // The test SDK predates this overload on the framework's WebViewClient.
            final boolean navigationHandled = ReflectionHelpers.callInstanceMethod(
                    Shadows.shadowOf(interceptor).getWebViewClient(), "shouldOverrideUrlLoading",
                    ReflectionHelpers.ClassParameter.from(WebView.class, interceptor),
                    ReflectionHelpers.ClassParameter.from(WebResourceRequest.class,
                            mockRequest(HTTPS_TARGET_URL)));
            assertTrue(navigationHandled);

            telemetry.verify(() -> OTelUtility.createSpanFromParent(
                    SpanName.WebViewTargetBlankNavigation.name(), parentSpanContext));
            verify(mainWebView).loadUrl(HTTPS_TARGET_URL);
            verify(span).setAttribute(AttributeName.correlation_id.name(), correlationId);
            verify(span).setAttribute(AttributeName.target_blank_navigation_is_user_gesture.name(), false);
            verify(span).setStatus(StatusCode.OK);
            verify(span).end();
            Shadows.shadowOf(Looper.getMainLooper()).idle();
        } finally {
            DiagnosticContext.INSTANCE.setRequestContext(previousContext);
        }
    }

    @Test
    public void testOnCreateWindow_noGestureOpenIdVc_refusesWalletDispatch() {
        mFragment = spy(mFragment);
        final AuthorizationActivity host = mock(AuthorizationActivity.class);
        final SpanContext parentSpanContext = SpanContext.getInvalid();
        when(host.getSpanContext()).thenReturn(parentSpanContext);
        when(mFragment.getActivity()).thenReturn(host);
        final WebView mainWebView = mock(WebView.class);
        final WebChromeClient client = setUpWindowClient(mainWebView);
        final AzureActiveDirectoryWebViewClient webViewClient =
                mock(AzureActiveDirectoryWebViewClient.class);
        ReflectionHelpers.setField(mFragment, "mAADWebViewClient", webViewClient);
        final WebView.WebViewTransport transport = mainWebView.new WebViewTransport();
        final Message message = Message.obtain(new Handler(Looper.getMainLooper()), 0, transport);
        final Span span = mockSpan();

        try (final MockedStatic<OTelUtility> telemetry = mockStatic(OTelUtility.class)) {
            telemetry.when(() -> OTelUtility.createSpanFromParent(
                    SpanName.WebViewTargetBlankNavigation.name(), parentSpanContext)).thenReturn(span);

            assertTrue(client.onCreateWindow(mainWebView, false, false, message));
            final WebView interceptor = transport.getWebView();
            final boolean navigationHandled = ReflectionHelpers.callInstanceMethod(
                    Shadows.shadowOf(interceptor).getWebViewClient(), "shouldOverrideUrlLoading",
                    ReflectionHelpers.ClassParameter.from(WebView.class, interceptor),
                    ReflectionHelpers.ClassParameter.from(WebResourceRequest.class,
                            mockRequest(OPENID_VC_TARGET_URL)));

            assertTrue(navigationHandled);
            telemetry.verify(() -> OTelUtility.createSpanFromParent(
                    SpanName.WebViewTargetBlankNavigation.name(), parentSpanContext));
            verify(webViewClient, never()).processOpenIdVcRequest(
                    mainWebView, OPENID_VC_TARGET_URL, span);
            verify(mainWebView, never()).loadUrl(ArgumentMatchers.anyString());
            verify(span).setAttribute(AttributeName.target_blank_navigation_is_user_gesture.name(), false);
            verify(span).setAttribute(AttributeName.target_blank_navigation_route.name(),
                    AuthenticationConstants.Broker.WEBVIEW_TARGET_BLANK_ROUTE_NO_USER_GESTURE);
            verify(span).setStatus(StatusCode.ERROR);
            verify(span).end();
        }
    }

    @Test
    public void testHandleInterceptedUrl_missingCorrelationId_doesNotSetAttribute() {
        for (final String correlationId : new String[]{null, ""}) {
            ReflectionHelpers.setField(mFragment, "mCorrelationId", correlationId);
            final WebView mainWebView = mock(WebView.class);
            final WebView interceptorWebView = mock(WebView.class);
            final Span span = mockSpan();

            mFragment.handleInterceptedUrlFromNewWindow(mainWebView, interceptorWebView,
                    mockRequest(HTTPS_TARGET_URL), span, false);

            verify(span, never()).setAttribute(eq(AttributeName.correlation_id.name()),
                    ArgumentMatchers.<String>any());
            verify(mainWebView).loadUrl(HTTPS_TARGET_URL);
            verify(span).end();
        }
    }

    private void setOpenIdVcRedirectFlightEnabled(final boolean enabled) {
        final IFlightsProvider flightsProvider = mock(IFlightsProvider.class);
        when(flightsProvider.isFlightEnabled(CommonFlight.ENABLE_OPEN_ID_VC_REDIRECT)).thenReturn(enabled);
        final MockCommonFlightsManager flightsManager = new MockCommonFlightsManager();
        flightsManager.setMockCommonFlightsProvider(flightsProvider);
        CommonFlightsManager.INSTANCE.initializeCommonFlightsManager(flightsManager);
    }

    @Test
    public void testHandleInterceptedUrl_nonUserGesture_loadsInline() {
        final WebView mainWebView = spy(new WebView(mContext));
        final WebView interceptorWebView = spy(new WebView(mContext));
        final Span span = mockSpan();
        final WebResourceRequest request = mockRequest(HTTPS_TARGET_URL);

        mFragment.handleInterceptedUrlFromNewWindow(mainWebView, interceptorWebView, request, span, false);

        // Should load URL inline in the main WebView
        verify(mainWebView).loadUrl(eq(HTTPS_TARGET_URL));
        verify(span).setAttribute(
                eq(AttributeName.target_blank_navigation_destination_host.name()),
                eq("terms.example.com"));
        verify(span).setAttribute(
                eq(AttributeName.target_blank_navigation_route.name()),
                eq(AuthenticationConstants.Broker.WEBVIEW_TARGET_BLANK_ROUTE_NO_USER_GESTURE));
        verify(span).setStatus(StatusCode.OK);
        verify(span).end();
    }

    @Test
    public void testHandleInterceptedUrl_nonHttpsWithoutUserGesture_refusesToOpen() {
        for (final String targetUrl : new String[]{OPENID_VC_TARGET_URL, FTP_URL, HTTP_TARGET_URL}) {
            final WebView mainWebView = mock(WebView.class);
            final WebView interceptorWebView = mock(WebView.class);
            final Span span = mockSpan();

            mFragment.handleInterceptedUrlFromNewWindow(
                    mainWebView, interceptorWebView, mockRequest(targetUrl), span, false);

            verify(mainWebView, never()).loadUrl(ArgumentMatchers.anyString());
            verify(span).setAttribute(AttributeName.target_blank_navigation_route.name(),
                    AuthenticationConstants.Broker.WEBVIEW_TARGET_BLANK_ROUTE_NO_USER_GESTURE);
            verify(span).setStatus(StatusCode.ERROR);
            verify(span).end();
        }
    }

    @Test
    public void testHandleInterceptedUrl_hostlessHttps_refusesToOpenWithOrWithoutGesture() {
        for (final String targetUrl : new String[]{
                "https://", "https:///path", "https://?query=value",
                "https:path", "HTTPS://#fragment", "https://:443/path"}) {
            for (final boolean isUserGesture : new boolean[]{false, true}) {
                final Context context = mock(Context.class);
                final WebView mainWebView = mock(WebView.class);
                final Span span = mockSpan();
                when(mainWebView.getContext()).thenReturn(context);

                mFragment.handleInterceptedUrlFromNewWindow(
                        mainWebView, mock(WebView.class), mockRequest(targetUrl), span, isUserGesture);

                verify(context, never()).startActivity(ArgumentMatchers.any(Intent.class));
                verify(mainWebView, never()).loadUrl(ArgumentMatchers.anyString());
                verify(span).setAttribute(AttributeName.target_blank_navigation_route.name(),
                        isUserGesture
                                ? AuthenticationConstants.Broker.WEBVIEW_TARGET_BLANK_ROUTE_NON_SSL
                                : AuthenticationConstants.Broker.WEBVIEW_TARGET_BLANK_ROUTE_NO_USER_GESTURE);
                verify(span).setStatus(StatusCode.ERROR);
                verify(span).end();
            }
        }
    }

    @Test
    public void testHandleInterceptedUrl_mixedCaseHttps_preservesOriginalUrlWithOrWithoutGesture() {
        final String targetUrl = "HtTpS://terms.example.com/CaseSensitive?Token=AbCd#Section";
        for (final boolean isUserGesture : new boolean[]{false, true}) {
            final Context context = mock(Context.class);
            final WebView mainWebView = mock(WebView.class);
            final Span span = mockSpan();
            when(mainWebView.getContext()).thenReturn(context);

            mFragment.handleInterceptedUrlFromNewWindow(
                    mainWebView, mock(WebView.class), mockRequest(targetUrl), span, isUserGesture);

            if (isUserGesture) {
                final ArgumentCaptor<Intent> intent = ArgumentCaptor.forClass(Intent.class);
                verify(context).startActivity(intent.capture());
                assertEquals(Intent.ACTION_VIEW, intent.getValue().getAction());
                assertEquals(targetUrl, intent.getValue().getDataString());
                verify(mainWebView, never()).loadUrl(ArgumentMatchers.anyString());
            } else {
                verify(mainWebView).loadUrl(targetUrl);
                verify(context, never()).startActivity(ArgumentMatchers.any(Intent.class));
            }
            verify(span).setStatus(StatusCode.OK);
            verify(span).end();
        }
    }

    @Test
    public void testHandleInterceptedUrl_nonSslUrl_refusesToOpen() {
        final WebView mainWebView = spy(new WebView(mContext));
        final WebView interceptorWebView = spy(new WebView(mContext));
        final Span span = mockSpan();
        final WebResourceRequest request = mockRequest(HTTP_TARGET_URL);

        // User gesture = true, but URL is http (not https)
        mFragment.handleInterceptedUrlFromNewWindow(mainWebView, interceptorWebView, request, span, true);

        // Should NOT load URL anywhere
        verify(mainWebView, never()).loadUrl(ArgumentMatchers.anyString());
        verify(span).setAttribute(
                eq(AttributeName.target_blank_navigation_route.name()),
                eq(AuthenticationConstants.Broker.WEBVIEW_TARGET_BLANK_ROUTE_NON_SSL));
        verify(span, never()).addEvent(ArgumentMatchers.anyString(), ArgumentMatchers.any(Attributes.class));
        verify(span).setStatus(StatusCode.ERROR);
        verify(span).end();
    }

    @Test
    public void testHandleInterceptedUrl_openIdVc_loadsInAuthenticationWebView() {
        setOpenIdVcRedirectFlightEnabled(true);
        final WebView mainWebView = spy(new WebView(mContext));
        final WebView interceptorWebView = spy(new WebView(mContext));
        final Span span = mockSpan();
        final WebResourceRequest request = mockRequest(OPENID_VC_TARGET_URL);
        final AzureActiveDirectoryWebViewClient webViewClient =
                mock(AzureActiveDirectoryWebViewClient.class);
        ReflectionHelpers.setField(mFragment, "mAADWebViewClient", webViewClient);
        when(webViewClient.processOpenIdVcRequest(mainWebView, OPENID_VC_TARGET_URL, span)).thenReturn(true);

        mFragment.handleInterceptedUrlFromNewWindow(mainWebView, interceptorWebView, request, span, true);

        verify(webViewClient).processOpenIdVcRequest(mainWebView, OPENID_VC_TARGET_URL, span);
        verify(mainWebView, never()).loadUrl(ArgumentMatchers.anyString());
        verify(span).setAttribute(
            eq(AttributeName.target_blank_navigation_destination_host.name()),
            eq("authorize"));
        verify(span).setAttribute(
                eq(AttributeName.target_blank_navigation_route.name()),
                eq(AuthenticationConstants.Broker.WEBVIEW_TARGET_BLANK_ROUTE_OPENID_VC));
        verify(span, never()).setStatus(ArgumentMatchers.any(StatusCode.class));
        verify(span).end();
    }

    @Test
    public void testHandleInterceptedUrl_openIdVcWithoutAuthority_launchesWallet() {
        setOpenIdVcRedirectFlightEnabled(true);
        for (final String targetUrl : new String[]{"openid-vc:authorize", "openid-vc:/authorize"}) {
            final WebView mainWebView = mock(WebView.class);
            final WebView interceptorWebView = mock(WebView.class);
            final Span span = mockSpan();
            final AzureActiveDirectoryWebViewClient webViewClient =
                    mock(AzureActiveDirectoryWebViewClient.class);
            ReflectionHelpers.setField(mFragment, "mAADWebViewClient", webViewClient);
            when(webViewClient.processOpenIdVcRequest(mainWebView, targetUrl, span)).thenReturn(true);

            mFragment.handleInterceptedUrlFromNewWindow(
                    mainWebView, interceptorWebView, mockRequest(targetUrl), span, true);

            verify(webViewClient).processOpenIdVcRequest(mainWebView, targetUrl, span);
            verify(mainWebView, never()).loadUrl(ArgumentMatchers.anyString());
            verify(span).setAttribute(
                    AttributeName.target_blank_navigation_route.name(),
                    AuthenticationConstants.Broker.WEBVIEW_TARGET_BLANK_ROUTE_OPENID_VC);
            verify(span).end();
        }
    }

    @Test
    public void testHandleInterceptedUrl_nonVcSchemes_doNotLaunchWallet() {
        setOpenIdVcRedirectFlightEnabled(true);
        for (final String targetUrl : new String[]{"openid-vc-extra://authorize", "authorize", "http://openid-vc"}) {
            final WebView mainWebView = mock(WebView.class);
            final WebView interceptorWebView = mock(WebView.class);
            final Span span = mockSpan();
            final AzureActiveDirectoryWebViewClient webViewClient =
                    mock(AzureActiveDirectoryWebViewClient.class);
            ReflectionHelpers.setField(mFragment, "mAADWebViewClient", webViewClient);

            mFragment.handleInterceptedUrlFromNewWindow(
                    mainWebView, interceptorWebView, mockRequest(targetUrl), span, true);

            verify(webViewClient, never()).processOpenIdVcRequest(
                    mainWebView, targetUrl, span);
            verify(mainWebView, never()).loadUrl(ArgumentMatchers.anyString());
            verify(span).setAttribute(
                    AttributeName.target_blank_navigation_route.name(),
                    AuthenticationConstants.Broker.WEBVIEW_TARGET_BLANK_ROUTE_NON_SSL);
            verify(span).end();
        }
    }

    @Test
    public void testHandleInterceptedUrl_uppercaseOpenIdVc_turkishLocale_launchesWallet() {
        final Locale originalLocale = Locale.getDefault();
        try {
            Locale.setDefault(new Locale("tr", "TR"));
            setOpenIdVcRedirectFlightEnabled(true);
            final String targetUrl = "OPENID-VC://authorize?request_uri=https%3A%2F%2Fexample.com%2FCaseSensitive";
            final WebView mainWebView = spy(new WebView(mContext));
            final WebView interceptorWebView = spy(new WebView(mContext));
            final Span span = mockSpan();
            final AzureActiveDirectoryWebViewClient webViewClient =
                    mock(AzureActiveDirectoryWebViewClient.class);
            ReflectionHelpers.setField(mFragment, "mAADWebViewClient", webViewClient);
            when(webViewClient.processOpenIdVcRequest(mainWebView, targetUrl, span)).thenReturn(true);

            mFragment.handleInterceptedUrlFromNewWindow(
                    mainWebView, interceptorWebView, mockRequest(targetUrl), span, true);

            verify(webViewClient).processOpenIdVcRequest(mainWebView, targetUrl, span);
            verify(mainWebView, never()).loadUrl(ArgumentMatchers.anyString());
            verify(span).setAttribute(
                    eq(AttributeName.target_blank_navigation_route.name()),
                    eq(AuthenticationConstants.Broker.WEBVIEW_TARGET_BLANK_ROUTE_OPENID_VC));
            verify(span, never()).setStatus(ArgumentMatchers.any(StatusCode.class));
            verify(span).end();
        } finally {
            Locale.setDefault(originalLocale);
        }
    }

    @Test
    public void testHandleInterceptedUrl_openIdVc_flightDisabled_launchesWallet() {
        setOpenIdVcRedirectFlightEnabled(false);
        final WebView mainWebView = spy(new WebView(mContext));
        final WebView interceptorWebView = spy(new WebView(mContext));
        final Span span = mockSpan();
        final WebResourceRequest request = mockRequest(OPENID_VC_TARGET_URL);
        final AzureActiveDirectoryWebViewClient webViewClient =
                mock(AzureActiveDirectoryWebViewClient.class);
        ReflectionHelpers.setField(mFragment, "mAADWebViewClient", webViewClient);

        when(webViewClient.processOpenIdVcRequest(mainWebView, OPENID_VC_TARGET_URL, span)).thenReturn(true);

        mFragment.handleInterceptedUrlFromNewWindow(mainWebView, interceptorWebView, request, span, true);

        verify(webViewClient, never()).processOpenIdVcRequest(
                ArgumentMatchers.any(WebView.class), ArgumentMatchers.anyString());
        verify(webViewClient).processOpenIdVcRequest(mainWebView, OPENID_VC_TARGET_URL, span);
        verify(mainWebView, never()).loadUrl(ArgumentMatchers.anyString());
        verify(span).setAttribute(
                eq(AttributeName.target_blank_navigation_route.name()),
                eq(AuthenticationConstants.Broker.WEBVIEW_TARGET_BLANK_ROUTE_OPENID_VC));
        verify(span, never()).addEvent(ArgumentMatchers.anyString(), ArgumentMatchers.any(Attributes.class));
        verify(span, never()).setStatus(ArgumentMatchers.any(StatusCode.class), ArgumentMatchers.anyString());
        verify(span).end();
    }

    @Test
    public void testHandleInterceptedUrl_httpsUrl_delegatesToBrowserRegardlessOfCurrentPage() {
        final Activity activity = Robolectric.buildActivity(Activity.class).get();
        final WebView mainWebView = mock(WebView.class);
        final WebView interceptorWebView = spy(new WebView(mContext));
        final Span span = mockSpan();
        final WebResourceRequest request = mockRequest(HTTPS_TARGET_URL);

        when(mainWebView.getContext()).thenReturn(activity);

        mFragment.handleInterceptedUrlFromNewWindow(mainWebView, interceptorWebView, request, span, true);

        final Intent launched = Shadows.shadowOf(activity).getNextStartedActivity();
        assertNotNull(launched);
        assertEquals(Intent.ACTION_VIEW, launched.getAction());
        assertEquals(HTTPS_TARGET_URL, launched.getDataString());
        verify(mainWebView, never()).getUrl();
        verify(mainWebView, never()).loadUrl(ArgumentMatchers.anyString());
        verify(span).setAttribute(
                eq(AttributeName.target_blank_navigation_route.name()),
            eq(AuthenticationConstants.Broker.WEBVIEW_TARGET_BLANK_ROUTE_BROWSER));
        verify(span).setStatus(StatusCode.OK);
        verify(span, never()).addEvent(ArgumentMatchers.anyString(), ArgumentMatchers.any(Attributes.class));
        verify(span).end();
    }

    @Test
    public void testHandleInterceptedUrl_hostlessUrl_omitsDestinationHost() {
        final WebView mainWebView = spy(new WebView(mContext));
        final WebView interceptorWebView = spy(new WebView(mContext));
        final Span span = mockSpan();
        final WebResourceRequest request = mockRequest(HOSTLESS_TARGET_URL);

        mFragment.handleInterceptedUrlFromNewWindow(mainWebView, interceptorWebView, request, span, true);

        verify(span, never()).setAttribute(
                eq(AttributeName.target_blank_navigation_destination_host.name()),
                ArgumentMatchers.anyString());
        verify(span).setAttribute(
                eq(AttributeName.target_blank_navigation_route.name()),
                eq(AuthenticationConstants.Broker.WEBVIEW_TARGET_BLANK_ROUTE_NON_SSL));
        verify(span).setStatus(StatusCode.ERROR);
        verify(span).end();
    }

    @Test
    public void testHandleInterceptedUrl_spanEndsInFinally() {
        final WebView mainWebView = spy(new WebView(mContext));
        final WebView interceptorWebView = spy(new WebView(mContext));
        final Span span = mockSpan();
        final WebResourceRequest request = mockRequest(HTTPS_TARGET_URL);

        mFragment.handleInterceptedUrlFromNewWindow(mainWebView, interceptorWebView, request, span, true);
        verify(span).end();
    }

    @Test
    public void testHandleInterceptedUrl_ftpScheme_refusesToOpen() {
        final WebView mainWebView = spy(new WebView(mContext));
        final WebView interceptorWebView = spy(new WebView(mContext));
        final Span span = mockSpan();
        final WebResourceRequest request = mockRequest(FTP_URL);

        mFragment.handleInterceptedUrlFromNewWindow(mainWebView, interceptorWebView, request, span, true);

        // FTP is not https, should be blocked
        verify(mainWebView, never()).loadUrl(ArgumentMatchers.anyString());
        verify(span).setAttribute(
                eq(AttributeName.target_blank_navigation_route.name()),
                eq(AuthenticationConstants.Broker.WEBVIEW_TARGET_BLANK_ROUTE_NON_SSL));
        verify(span).setStatus(StatusCode.ERROR);
        verify(span).end();
    }

    @Test
    public void testHandleInterceptedUrl_noBrowser_fallsBackInline() {
        verifyBrowserFallback(new ActivityNotFoundException("Sensitive URL must not enter telemetry."));
    }

    @Test
    public void testHandleInterceptedUrl_browserSecurityException_fallsBackInline() {
        verifyBrowserFallback(new SecurityException("Launch denied."));
    }

    @Test
    public void testHandleInterceptedUrl_browserError_propagatesWithoutFallback() {
        for (final Error error : new Error[]{new AssertionError(), new OutOfMemoryError()}) {
            final Context context = mock(Context.class);
            final WebView mainWebView = mock(WebView.class);
            final WebView interceptorWebView = mock(WebView.class);
            final Span span = mockSpan();
            when(mainWebView.getContext()).thenReturn(context);
            doThrow(error).when(context).startActivity(ArgumentMatchers.any(Intent.class));

            assertSame(error, assertThrows(Error.class, () ->
                    mFragment.handleInterceptedUrlFromNewWindow(
                            mainWebView, interceptorWebView, mockRequest(HTTPS_TARGET_URL), span, true)));

            verify(mainWebView, never()).loadUrl(ArgumentMatchers.anyString());
            verify(span, never()).addEvent(ArgumentMatchers.anyString(), ArgumentMatchers.any(Attributes.class));
            verify(span, never()).recordException(ArgumentMatchers.any(Throwable.class));
            verify(span, never()).setStatus(ArgumentMatchers.any(StatusCode.class));
            verify(span).end();
            verify(interceptorWebView).post(ArgumentMatchers.any(Runnable.class));
        }
    }

    @Test
    public void testHandleInterceptedUrl_browserSuccessTelemetryFails_doesNotFallback() {
        final Context context = mock(Context.class);
        final WebView mainWebView = mock(WebView.class);
        final Span span = mockSpan();
        final IllegalStateException failure = new IllegalStateException("Telemetry failed");
        when(mainWebView.getContext()).thenReturn(context);
        doThrow(failure).when(span).setStatus(StatusCode.OK);

        mFragment.handleInterceptedUrlFromNewWindow(
                mainWebView, mock(WebView.class), mockRequest(HTTPS_TARGET_URL), span, true);

        verify(context).startActivity(ArgumentMatchers.any(Intent.class));
        verify(mainWebView, never()).loadUrl(ArgumentMatchers.anyString());
        verify(span, never()).addEvent(ArgumentMatchers.anyString(), ArgumentMatchers.any(Attributes.class));
        verify(span, never()).setAttribute(AttributeName.target_blank_navigation_route.name(),
                AuthenticationConstants.Broker.WEBVIEW_TARGET_BLANK_ROUTE_BROWSER_FALLBACK);
        verify(span).recordException(failure);
        verify(span).setStatus(StatusCode.ERROR);
        verify(span).end();
    }

    private void verifyBrowserFallback(final Throwable exception) {
        final Context context = mock(Context.class);
        final WebView mainWebView = mock(WebView.class);
        final WebView interceptorWebView = spy(new WebView(mContext));
        final Span span = mockSpan();
        when(mainWebView.getContext()).thenReturn(context);
        doThrow(exception).when(context).startActivity(ArgumentMatchers.any(Intent.class));

        mFragment.handleInterceptedUrlFromNewWindow(
                mainWebView, interceptorWebView, mockRequest(HTTPS_TARGET_URL), span, true);

        verify(mainWebView).loadUrl(HTTPS_TARGET_URL);
        final InOrder decisions = org.mockito.Mockito.inOrder(span);
        decisions.verify(span).addEvent("browser_launch", failure(exception.getClass().getSimpleName()));
        decisions.verify(span).addEvent("browser_fallback", outcome("inline_fallback_requested"));
        verify(span).setAttribute(eq(AttributeName.target_blank_navigation_route.name()),
                eq(AuthenticationConstants.Broker.WEBVIEW_TARGET_BLANK_ROUTE_BROWSER_FALLBACK));
        verify(span, never()).setAttribute(eq(AttributeName.operation_outcome.name()), ArgumentMatchers.anyString());
        verify(span, never()).setStatus(eq(StatusCode.ERROR), ArgumentMatchers.anyString());
        verify(span).setStatus(StatusCode.OK);
        verify(span, never()).recordException(ArgumentMatchers.any(Throwable.class));
        verify(span).end();
    }

    @Test
    public void testHandleInterceptedUrl_browserAndFallbackFail_recordsBothFailures() {
        verifyBrowserAndFallbackFailure(new IllegalStateException());
    }

    @Test
    public void testHandleInterceptedUrl_fallbackError_propagates() {
        verifyInlineErrorPropagates(true);
    }

    private void verifyBrowserAndFallbackFailure(final Throwable fallbackFailure) {
        final Context context = mock(Context.class);
        final WebView mainWebView = mock(WebView.class);
        final WebView interceptorWebView = spy(new WebView(mContext));
        final Span span = mockSpan();
        when(mainWebView.getContext()).thenReturn(context);
        doThrow(new ActivityNotFoundException()).when(context).startActivity(ArgumentMatchers.any(Intent.class));
        doThrow(fallbackFailure).when(mainWebView).loadUrl(HTTPS_TARGET_URL);

        mFragment.handleInterceptedUrlFromNewWindow(
                mainWebView, interceptorWebView, mockRequest(HTTPS_TARGET_URL), span, true);

        final InOrder decisions = org.mockito.Mockito.inOrder(span);
        decisions.verify(span).addEvent("browser_launch", failure("ActivityNotFoundException"));
        decisions.verify(span).addEvent("browser_fallback", failure(fallbackFailure.getClass().getSimpleName()));
        verify(span).recordException(fallbackFailure);
        verify(span).setStatus(StatusCode.ERROR);
        verify(span).setAttribute(AttributeName.target_blank_navigation_route.name(),
                AuthenticationConstants.Broker.WEBVIEW_TARGET_BLANK_ROUTE_BROWSER_FALLBACK);
        verify(span, never()).setStatus(StatusCode.OK);
        verify(span).end();
    }

    @Test
    public void testHandleInterceptedUrl_nonUserGesture_inlineFailure_recordsError() {
        verifyInlineFailure(new IllegalStateException());
    }

    @Test
    public void testHandleInterceptedUrl_nonUserGesture_inlineError_propagates() {
        verifyInlineErrorPropagates(false);
    }

    private void verifyInlineErrorPropagates(final boolean isUserGesture) {
        final Context context = mock(Context.class);
        final WebView mainWebView = mock(WebView.class);
        final WebView interceptorWebView = mock(WebView.class);
        final Span span = mockSpan();
        final OutOfMemoryError failure = new OutOfMemoryError();
        when(mainWebView.getContext()).thenReturn(context);
        doThrow(new ActivityNotFoundException()).when(context).startActivity(ArgumentMatchers.any(Intent.class));
        doThrow(failure).when(mainWebView).loadUrl(HTTPS_TARGET_URL);

        assertSame(failure, assertThrows(OutOfMemoryError.class, () ->
                mFragment.handleInterceptedUrlFromNewWindow(
                        mainWebView, interceptorWebView, mockRequest(HTTPS_TARGET_URL), span, isUserGesture)));

        verify(span, never()).addEvent(eq("browser_fallback"), ArgumentMatchers.any(Attributes.class));
        verify(span, never()).recordException(ArgumentMatchers.any(Throwable.class));
        verify(span, never()).setStatus(ArgumentMatchers.any(StatusCode.class));
        verify(span).end();
        verify(interceptorWebView).post(ArgumentMatchers.any(Runnable.class));
    }

    private void verifyInlineFailure(final Throwable failure) {
        final WebView mainWebView = mock(WebView.class);
        final WebView interceptorWebView = spy(new WebView(mContext));
        final Span span = mockSpan();
        doThrow(failure).when(mainWebView).loadUrl(HTTPS_TARGET_URL);

        mFragment.handleInterceptedUrlFromNewWindow(
                mainWebView, interceptorWebView, mockRequest(HTTPS_TARGET_URL), span, false);

        verify(span, never()).addEvent(eq("browser_launch"), ArgumentMatchers.any(Attributes.class));
        verify(span, never()).addEvent(ArgumentMatchers.anyString(), ArgumentMatchers.any(Attributes.class));
        verify(span).recordException(failure);
        verify(span).setStatus(StatusCode.ERROR);
        verify(span).setAttribute(AttributeName.target_blank_navigation_route.name(),
                AuthenticationConstants.Broker.WEBVIEW_TARGET_BLANK_ROUTE_NO_USER_GESTURE);
        verify(span, never()).setStatus(StatusCode.OK);
        verify(span).end();
    }

    @Test
    public void testHandleInterceptedUrl_nullDestination_recordsError() {
        final WebView mainWebView = mock(WebView.class);
        final WebView interceptorWebView = spy(new WebView(mContext));
        final Span span = mockSpan();
        final WebResourceRequest request = mock(WebResourceRequest.class);

        mFragment.handleInterceptedUrlFromNewWindow(mainWebView, interceptorWebView, request, span, true);

        verify(mainWebView, never()).loadUrl(ArgumentMatchers.anyString());
        verify(span, never()).addEvent(ArgumentMatchers.anyString(), ArgumentMatchers.any(Attributes.class));
        verify(span).setStatus(StatusCode.ERROR, "Missing popup destination");
        verify(span).end();
    }

    @Test
    public void testHandleInterceptedUrl_walletFailure_doesNotFallbackOrReportSuccess() {
        setOpenIdVcRedirectFlightEnabled(true);
        final WebView mainWebView = mock(WebView.class);
        final WebView interceptorWebView = spy(new WebView(mContext));
        final Span span = mockSpan();
        final AzureActiveDirectoryWebViewClient webViewClient = mock(AzureActiveDirectoryWebViewClient.class);
        ReflectionHelpers.setField(mFragment, "mAADWebViewClient", webViewClient);
        when(webViewClient.processOpenIdVcRequest(mainWebView, OPENID_VC_TARGET_URL, span)).thenAnswer(invocation -> {
            span.setStatus(StatusCode.ERROR, "No handler found for openid-vc:// URI");
            return false;
        });

        mFragment.handleInterceptedUrlFromNewWindow(
                mainWebView, interceptorWebView, mockRequest(OPENID_VC_TARGET_URL), span, true);

        verify(span, never()).addEvent(eq("vc_dispatch"), ArgumentMatchers.any(Attributes.class));
        verify(mainWebView, never()).loadUrl(ArgumentMatchers.anyString());
        verify(span).setStatus(StatusCode.ERROR, "No handler found for openid-vc:// URI");
        verify(span, never()).setStatus(StatusCode.ERROR, "Wallet dispatch failed");
        verify(span, never()).setStatus(StatusCode.OK);
        verify(span).end();
    }

    @Test
    public void testHandleInterceptedUrl_missingWalletClient_recordsError() {
        setOpenIdVcRedirectFlightEnabled(true);
        final WebView mainWebView = mock(WebView.class);
        final WebView interceptorWebView = spy(new WebView(mContext));
        final Span span = mockSpan();

        mFragment.handleInterceptedUrlFromNewWindow(
                mainWebView, interceptorWebView, mockRequest(OPENID_VC_TARGET_URL), span, true);

        verify(span).recordException(ArgumentMatchers.any(IllegalStateException.class));
        verify(span).setStatus(StatusCode.ERROR);
        verify(span).setAttribute(AttributeName.target_blank_navigation_route.name(),
                AuthenticationConstants.Broker.WEBVIEW_TARGET_BLANK_ROUTE_OPENID_VC);
        verify(span, never()).addEvent(eq("vc_client_check"), ArgumentMatchers.any(Attributes.class));
        verify(mainWebView, never()).loadUrl(ArgumentMatchers.anyString());
        verify(span, never()).setStatus(StatusCode.OK);
        verify(span).end();
    }

    @Test
    public void testHandleInterceptedUrl_emptyDestination_recordsError() {
        final WebView mainWebView = mock(WebView.class);
        final Span span = mockSpan();

        mFragment.handleInterceptedUrlFromNewWindow(
                mainWebView, mock(WebView.class), mockRequest(""), span, true);

        verify(span, never()).addEvent(ArgumentMatchers.anyString(), ArgumentMatchers.any(Attributes.class));
        verify(span).setStatus(StatusCode.ERROR, "Missing popup destination");
        verify(mainWebView, never()).loadUrl(ArgumentMatchers.anyString());
        verify(span).end();
    }

    @Test
    public void testHandleInterceptedUrl_walletThrows_recordsFailureWithoutFallback() {
        setOpenIdVcRedirectFlightEnabled(true);
        final WebView mainWebView = mock(WebView.class);
        final Span span = mockSpan();
        final AzureActiveDirectoryWebViewClient client = mock(AzureActiveDirectoryWebViewClient.class);
        ReflectionHelpers.setField(mFragment, "mAADWebViewClient", client);
        when(client.processOpenIdVcRequest(mainWebView, OPENID_VC_TARGET_URL, span))
                .thenThrow(new IllegalStateException());

        mFragment.handleInterceptedUrlFromNewWindow(
                mainWebView, mock(WebView.class), mockRequest(OPENID_VC_TARGET_URL), span, true);

        verify(span).recordException(ArgumentMatchers.any(IllegalStateException.class));
        verify(span).setStatus(StatusCode.ERROR);
        verify(span).setAttribute(AttributeName.target_blank_navigation_route.name(),
                AuthenticationConstants.Broker.WEBVIEW_TARGET_BLANK_ROUTE_OPENID_VC);
        verify(span, never()).addEvent(eq("vc_dispatch"), ArgumentMatchers.any(Attributes.class));
        verify(mainWebView, never()).loadUrl(ArgumentMatchers.anyString());
        verify(span, never()).setStatus(StatusCode.OK);
        verify(span).end();
    }

    @Test
    public void testHandleInterceptedUrl_cleanupIsPostedToInterceptor() {
        final WebView interceptor = mock(WebView.class);
        final Span span = mockSpan();
        final ArgumentCaptor<Runnable> cleanup = ArgumentCaptor.forClass(Runnable.class);
        when(interceptor.post(ArgumentMatchers.any(Runnable.class))).thenReturn(true);

        mFragment.handleInterceptedUrlFromNewWindow(
                mock(WebView.class), interceptor, mockRequest(HTTPS_TARGET_URL), span, false);

        final InOrder order = org.mockito.Mockito.inOrder(span, interceptor);
        order.verify(span).end();
        order.verify(interceptor).post(cleanup.capture());
        verify(interceptor, never()).destroy();
        cleanup.getValue().run();
        verify(interceptor).destroy();
        verify(span).setStatus(StatusCode.OK);
        verify(span, never()).setStatus(eq(StatusCode.ERROR), ArgumentMatchers.anyString());
    }

    private static Attributes outcome(final String value) {
        return Attributes.builder().put(AttributeName.operation_outcome.name(), value).build();
    }

    private static Attributes failure(final String exceptionType) {
        return Attributes.builder()
                .put(AttributeName.operation_outcome.name(), "failed")
                .put(AttributeName.error_type.name(), exceptionType)
                .build();
    }
}
