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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;

import androidx.activity.result.ActivityResultLauncher;

import com.microsoft.identity.common.internal.mocks.MockCommonFlightsManager;
import com.microsoft.identity.common.java.flighting.CommonFlight;
import com.microsoft.identity.common.java.flighting.CommonFlightsManager;
import com.microsoft.identity.common.java.flighting.IFlightsProvider;
import com.microsoft.identity.common.java.opentelemetry.AttributeName;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;

/**
 * Tests for the WebView file upload feature in {@link WebViewAuthorizationFragment}.
 * Covers {@link WebViewAuthorizationFragment#handleFileUploadRequest} flight gating,
 * launch behavior, error handling, and the {@code @VisibleForTesting} accessors
 * used for cleanup verification.
 */
@RunWith(RobolectricTestRunner.class)
public class WebViewAuthorizationFragmentFileUploadTest {

    private WebViewAuthorizationFragment mFragment;
    private IFlightsProvider mMockFlightsProvider;
    private ActivityResultLauncher<Intent> mMockLauncher;

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() {
        mMockFlightsProvider = mock(IFlightsProvider.class);
        when(mMockFlightsProvider.isFlightEnabled(any(CommonFlight.class))).thenReturn(false);
        when(mMockFlightsProvider.isFlightEnabled(CommonFlight.ENABLE_WEBVIEW_FILE_UPLOAD)).thenReturn(true);

        final MockCommonFlightsManager mgr = new MockCommonFlightsManager();
        mgr.setMockCommonFlightsProvider(mMockFlightsProvider);
        CommonFlightsManager.INSTANCE.initializeCommonFlightsManager(mgr);

        mFragment = new WebViewAuthorizationFragment();

        mMockLauncher = mock(ActivityResultLauncher.class);
        mFragment.setFileChooserLauncher(mMockLauncher);
    }

    @After
    public void tearDown() {
        CommonFlightsManager.INSTANCE.resetFlightsManager();
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private ValueCallback<Uri[]> mockFilePathCallback() {
        return mock(ValueCallback.class);
    }

    private WebChromeClient.FileChooserParams mockFileChooserParams() {
        final WebChromeClient.FileChooserParams params = mock(WebChromeClient.FileChooserParams.class);
        when(params.createIntent()).thenReturn(new Intent(Intent.ACTION_GET_CONTENT));
        return params;
    }

    // -----------------------------------------------------------------------
    // handleFileUploadRequest — flight gating
    // -----------------------------------------------------------------------

    @Test
    public void testHandleFileUploadRequest_flightDisabled_returnsFalse() {
        when(mMockFlightsProvider.isFlightEnabled(CommonFlight.ENABLE_WEBVIEW_FILE_UPLOAD)).thenReturn(false);

        final ValueCallback<Uri[]> callback = mockFilePathCallback();
        final WebChromeClient.FileChooserParams params = mockFileChooserParams();

        final boolean result = mFragment.handleFileUploadRequest(callback, params, null);

        assertFalse(result);
        verify(callback, never()).onReceiveValue(any());
        verify(mMockLauncher, never()).launch(any());
    }

    // -----------------------------------------------------------------------
    // handleFileUploadRequest — launch success
    // -----------------------------------------------------------------------

    @Test
    public void testHandleFileUploadRequest_launchSucceeds_returnsTrue() {
        final ValueCallback<Uri[]> callback = mockFilePathCallback();
        final WebChromeClient.FileChooserParams params = mockFileChooserParams();

        final boolean result = mFragment.handleFileUploadRequest(callback, params, null);

        assertTrue(result);
        verify(mMockLauncher).launch(any(Intent.class));
    }

    @Test
    public void testHandleFileUploadRequest_storesCallback() {
        final ValueCallback<Uri[]> callback = mockFilePathCallback();
        final WebChromeClient.FileChooserParams params = mockFileChooserParams();

        mFragment.handleFileUploadRequest(callback, params, null);

        assertEquals(callback, mFragment.getFileUploadCallback());
    }

    // -----------------------------------------------------------------------
    // handleFileUploadRequest — cancels existing callback
    // -----------------------------------------------------------------------

    @Test
    public void testHandleFileUploadRequest_cancelsExistingCallback() {
        final ValueCallback<Uri[]> existingCallback = mockFilePathCallback();
        final Span existingSpan = mock(Span.class);
        mFragment.setFileUploadCallback(existingCallback);
        mFragment.setFileUploadSpan(existingSpan);

        final ValueCallback<Uri[]> newCallback = mockFilePathCallback();
        final WebChromeClient.FileChooserParams params = mockFileChooserParams();

        mFragment.handleFileUploadRequest(newCallback, params, null);

        verify(existingCallback).onReceiveValue(null);
        verify(existingSpan).setAttribute(
                AttributeName.webview_file_upload_result.name(), "superseded");
        verify(existingSpan).setAttribute(
                AttributeName.webview_file_upload_file_count_bucket.name(), "0");
        verify(existingSpan).setStatus(StatusCode.ERROR);
        verify(existingSpan).end();
    }

    // -----------------------------------------------------------------------
    // handleFileUploadRequest — launch failure
    // -----------------------------------------------------------------------

    @Test
    public void testHandleFileUploadRequest_createIntentThrows_returnsFalse() {
        final ValueCallback<Uri[]> callback = mockFilePathCallback();
        final WebChromeClient.FileChooserParams params = mock(WebChromeClient.FileChooserParams.class);
        when(params.createIntent()).thenThrow(new RuntimeException("No activity found"));

        final boolean result = mFragment.handleFileUploadRequest(callback, params, null);

        assertFalse(result);
        verify(callback).onReceiveValue(null);
    }

    @Test
    public void testHandleFileUploadRequest_createIntentThrows_clearsCallback() {
        final ValueCallback<Uri[]> callback = mockFilePathCallback();
        final WebChromeClient.FileChooserParams params = mock(WebChromeClient.FileChooserParams.class);
        when(params.createIntent()).thenThrow(new RuntimeException("No activity found"));

        mFragment.handleFileUploadRequest(callback, params, null);

        assertNull(mFragment.getFileUploadCallback());
    }

    @Test
    public void testHandleFileUploadRequest_launcherThrows_returnsFalse() {
        doThrow(new IllegalStateException("Launcher not initialized"))
                .when(mMockLauncher).launch(any(Intent.class));

        final ValueCallback<Uri[]> callback = mockFilePathCallback();
        final WebChromeClient.FileChooserParams params = mockFileChooserParams();

        final boolean result = mFragment.handleFileUploadRequest(callback, params, null);

        assertFalse(result);
        verify(callback).onReceiveValue(null);
    }

    @Test
    public void testHandleFileUploadRequest_launcherThrows_clearsCallback() {
        doThrow(new IllegalStateException("Launcher not initialized"))
                .when(mMockLauncher).launch(any(Intent.class));

        final ValueCallback<Uri[]> callback = mockFilePathCallback();
        final WebChromeClient.FileChooserParams params = mockFileChooserParams();

        mFragment.handleFileUploadRequest(callback, params, null);

        assertNull(mFragment.getFileUploadCallback());
    }

    @Test
    public void testHandleFileUploadResult_singleFile_recordsSelected() {
        final ValueCallback<Uri[]> callback = mockFilePathCallback();
        final Span span = mock(Span.class);
        final Intent data = new Intent().setData(Uri.parse("content://picker/file"));
        mFragment.setFileUploadCallback(callback);
        mFragment.setFileUploadSpan(span);

        mFragment.handleFileUploadResult(Activity.RESULT_OK, data);

        verify(span).setAttribute(AttributeName.webview_file_upload_result.name(), "selected");
        verify(span).setAttribute(
                AttributeName.webview_file_upload_file_count_bucket.name(), "1");
        verify(span).setStatus(StatusCode.OK);
        verify(span).end();
        verify(callback).onReceiveValue(any(Uri[].class));
        assertNull(mFragment.getFileUploadCallback());
    }

    @Test
    public void testHandleFileUploadResult_multipleFiles_recordsBucketedCount() {
        final ValueCallback<Uri[]> callback = mockFilePathCallback();
        final Span span = mock(Span.class);
        final Intent data = new Intent();
        final ClipData clipData = new ClipData(
                "files",
                new String[]{"text/uri-list"},
                new ClipData.Item(Uri.parse("content://picker/1")));
        clipData.addItem(new ClipData.Item(Uri.parse("content://picker/2")));
        clipData.addItem(new ClipData.Item(Uri.parse("content://picker/3")));
        data.setClipData(clipData);
        mFragment.setFileUploadCallback(callback);
        mFragment.setFileUploadSpan(span);

        mFragment.handleFileUploadResult(Activity.RESULT_OK, data);

        verify(span).setAttribute(AttributeName.webview_file_upload_result.name(), "selected");
        verify(span).setAttribute(
                AttributeName.webview_file_upload_file_count_bucket.name(), "2-5");
        verify(span).setStatus(StatusCode.OK);
        verify(span).end();
    }

    @Test
    public void testHandleFileUploadResult_cancelled_recordsNormalCompletion() {
        final ValueCallback<Uri[]> callback = mockFilePathCallback();
        final Span span = mock(Span.class);
        mFragment.setFileUploadCallback(callback);
        mFragment.setFileUploadSpan(span);

        mFragment.handleFileUploadResult(Activity.RESULT_CANCELED, null);

        verify(span).setAttribute(AttributeName.webview_file_upload_result.name(), "cancelled");
        verify(span).setAttribute(
                AttributeName.webview_file_upload_file_count_bucket.name(), "0");
        verify(span).setStatus(StatusCode.OK);
        verify(span).end();
        verify(callback).onReceiveValue(null);
    }

    @Test
    public void testHandleFileUploadResult_okWithoutData_recordsNoData() {
        final ValueCallback<Uri[]> callback = mockFilePathCallback();
        final Span span = mock(Span.class);
        mFragment.setFileUploadCallback(callback);
        mFragment.setFileUploadSpan(span);

        mFragment.handleFileUploadResult(Activity.RESULT_OK, null);

        verify(span).setAttribute(AttributeName.webview_file_upload_result.name(), "no_data");
        verify(span).setAttribute(
                AttributeName.webview_file_upload_file_count_bucket.name(), "0");
        verify(span).setStatus(StatusCode.OK);
        verify(span).end();
        verify(callback).onReceiveValue(null);
    }
}
