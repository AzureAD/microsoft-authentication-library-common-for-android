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
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;

import androidx.test.core.app.ApplicationProvider;

import com.microsoft.identity.common.adal.internal.AuthenticationConstants;
import com.microsoft.identity.common.java.opentelemetry.AttributeName;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;

/**
 * Tests for the multi-window (target=_blank) URL handling logic and
 * TLR URL detection in {@link WebViewAuthorizationFragment}.
 */
@RunWith(RobolectricTestRunner.class)
public class WebViewAuthorizationFragmentMultiWindowTest {

    private WebViewAuthorizationFragment mFragment;
    private Context mContext;

    // TLR URLs
    private static final String TLR_URL = "https://login.microsoftonline.com/tlr/start?param=1";
    private static final String TLR_URL_UPPER_CASE = "HTTPS://LOGIN.MICROSOFTONLINE.COM/TLR/START?PARAM=1";
    private static final String TLR_URL_MIXED_CASE = "https://Login.Microsoftonline.com/Tlr/Start?param=1";

    // Non-TLR URLs
    private static final String NON_TLR_HTTPS_URL = "https://login.microsoftonline.com/common/oauth2/authorize";
    private static final String HTTP_URL = "http://example.com/tlr/start";
    private static final String FTP_URL = "ftp://files.example.com/document.pdf";

    // Target URLs for handleInterceptedUrlFromNewWindow
    private static final String HTTPS_TARGET_URL = "https://terms.example.com/privacy";
    private static final String HTTP_TARGET_URL = "http://terms.example.com/privacy";

    @Before
    public void setUp() throws Exception {
        mContext = ApplicationProvider.getApplicationContext();
        mFragment = new WebViewAuthorizationFragment();
    }

    // -----------------------------------------------------------------------
    // isTlrUrl tests
    // -----------------------------------------------------------------------

    @Test
    public void testIsTlrUrl_nullUrl_returnsFalse() {
        assertFalse(mFragment.isTlrUrl(null));
    }

    @Test
    public void testIsTlrUrl_emptyString_returnsFalse() {
        assertFalse(mFragment.isTlrUrl(""));
    }

    @Test
    public void testIsTlrUrl_validTlrUrl_returnsTrue() {
        assertTrue(mFragment.isTlrUrl(TLR_URL));
    }

    @Test
    public void testIsTlrUrl_upperCaseTlrUrl_returnsTrue() {
        assertTrue(mFragment.isTlrUrl(TLR_URL_UPPER_CASE));
    }

    @Test
    public void testIsTlrUrl_mixedCaseTlrUrl_returnsTrue() {
        assertTrue(mFragment.isTlrUrl(TLR_URL_MIXED_CASE));
    }

    @Test
    public void testIsTlrUrl_httpsNonTlrPath_returnsFalse() {
        assertFalse(mFragment.isTlrUrl(NON_TLR_HTTPS_URL));
    }

    @Test
    public void testIsTlrUrl_httpWithTlrPath_returnsFalse() {
        // Must be HTTPS to be considered a TLR URL
        assertFalse(mFragment.isTlrUrl(HTTP_URL));
    }

    @Test
    public void testIsTlrUrl_ftpScheme_returnsFalse() {
        assertFalse(mFragment.isTlrUrl(FTP_URL));
    }

    @Test
    public void testIsTlrUrl_tlrPathOnly_returnsFalse() {
        // No scheme prefix, just path
        assertFalse(mFragment.isTlrUrl("/tlr/start"));
    }

    // -----------------------------------------------------------------------
    // handleInterceptedUrlFromNewWindow tests
    // -----------------------------------------------------------------------

    private WebResourceRequest mockRequest(final String url) {
        final WebResourceRequest request = mock(WebResourceRequest.class);
        when(request.getUrl()).thenReturn(Uri.parse(url));
        return request;
    }

    private Span mockSpan() {
        return mock(Span.class);
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
                eq(AttributeName.target_blank_navigation_route.name()),
                eq(AuthenticationConstants.Broker.WEBVIEW_TARGET_BLANK_ROUTE_NO_USER_GESTURE));
        verify(span).setStatus(StatusCode.OK);
        verify(span).end();
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
        verify(span).setStatus(StatusCode.OK);
        verify(span).end();
    }

    @Test
    public void testHandleInterceptedUrl_nonTlrPage_loadsInline() {
        final WebView mainWebView = spy(new WebView(mContext));
        final WebView interceptorWebView = spy(new WebView(mContext));
        final Span span = mockSpan();
        final WebResourceRequest request = mockRequest(HTTPS_TARGET_URL);

        // mainWebView is on a non-TLR page
        // Robolectric WebView.getUrl() returns null by default (no page loaded), which is non-TLR
        mFragment.handleInterceptedUrlFromNewWindow(mainWebView, interceptorWebView, request, span, true);

        // Should load URL inline
        verify(mainWebView).loadUrl(eq(HTTPS_TARGET_URL));
        verify(span).setAttribute(
                eq(AttributeName.target_blank_navigation_route.name()),
                eq(AuthenticationConstants.Broker.WEBVIEW_TARGET_BLANK_ROUTE_NON_TLR));
        verify(span).setStatus(StatusCode.OK);
        verify(span).end();
    }

    @Test
    public void testHandleInterceptedUrl_tlrPage_delegatesToBrowser() {
        final Activity activity = Robolectric.buildActivity(Activity.class).get();
        final WebView mainWebView = mock(WebView.class);
        final WebView interceptorWebView = spy(new WebView(mContext));
        final Span span = mockSpan();
        final WebResourceRequest request = mockRequest(HTTPS_TARGET_URL);

        // Simulate main WebView being on a TLR page
        when(mainWebView.getUrl()).thenReturn(TLR_URL);
        when(mainWebView.getContext()).thenReturn(activity);

        mFragment.handleInterceptedUrlFromNewWindow(mainWebView, interceptorWebView, request, span, true);

        // Should NOT load inline
        verify(mainWebView, never()).loadUrl(ArgumentMatchers.anyString());
        verify(span).setAttribute(
                eq(AttributeName.target_blank_navigation_route.name()),
                eq(AuthenticationConstants.Broker.WEBVIEW_TARGET_BLANK_ROUTE_TLR));
        verify(span).setStatus(StatusCode.OK);
        verify(span).end();
    }

    @Test
    public void testHandleInterceptedUrl_spanEndsInFinally() {
        final WebView mainWebView = spy(new WebView(mContext));
        final WebView interceptorWebView = spy(new WebView(mContext));
        final Span span = mockSpan();
        final WebResourceRequest request = mockRequest(HTTPS_TARGET_URL);

        // Even for a normal non-TLR case, span.end() should always be called
        mFragment.handleInterceptedUrlFromNewWindow(mainWebView, interceptorWebView, request, span, true);
        verify(span).end();
    }

    @Test
    public void testHandleInterceptedUrl_nonUserGesture_nonSslUrl_loadsInline() {
        // Non-user-gesture takes priority over non-SSL check
        final WebView mainWebView = spy(new WebView(mContext));
        final WebView interceptorWebView = spy(new WebView(mContext));
        final Span span = mockSpan();
        final WebResourceRequest request = mockRequest(HTTP_TARGET_URL);

        mFragment.handleInterceptedUrlFromNewWindow(mainWebView, interceptorWebView, request, span, false);

        // Even though URL is http, non-user-gesture check comes first and loads inline
        verify(mainWebView).loadUrl(eq(HTTP_TARGET_URL));
        verify(span).setAttribute(
                eq(AttributeName.target_blank_navigation_route.name()),
                eq(AuthenticationConstants.Broker.WEBVIEW_TARGET_BLANK_ROUTE_NO_USER_GESTURE));
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
        verify(span).setStatus(StatusCode.OK);
        verify(span).end();
    }
}
