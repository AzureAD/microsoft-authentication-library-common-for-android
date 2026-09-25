// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.microsoft.identity.common.internal.providers.oauth2

import android.os.Build
import android.webkit.WebView
import com.microsoft.identity.common.internal.ui.webview.AzureActiveDirectoryWebViewClient
import io.mockk.justRun
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify as verifyMockK
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers

/** View-scoped Auth UX cleanup must coexist with passkey and activity-scoped cleanup. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P])
class WebViewAuthorizationFragmentAuthUxTest {
    /** Cleans both registrations without invoking activity/certificate teardown. */
    @Test
    fun onDestroyViewCleansAuthUxAndPasskeyIndependentlyOfActivityTeardown() {
        val fragment = WebViewAuthorizationFragment()
        val client: AzureActiveDirectoryWebViewClient = mock()
        val view: WebView = mock()
        ReflectionHelpers.setField(fragment, "mAADWebViewClient", client)
        ReflectionHelpers.setField(fragment, "mWebView", view)
        ReflectionHelpers.setField(fragment, "mPasskeyWebListenerHooked", true)
        mockkObject(PasskeyWebListener.Companion)
        try {
            justRun { PasskeyWebListener.unhook(view) }
            fragment.onDestroyView()
            verifyMockK(exactly = 1) { PasskeyWebListener.unhook(view) }
        } finally {
            unmockkObject(PasskeyWebListener.Companion)
        }
        verify(client).removeAuthUxDocumentStartScript()
        verify(client, never()).onDestroy()
        assertFalse(ReflectionHelpers.getField(fragment, "mPasskeyWebListenerHooked"))
    }

    /** Partial initialization is safe even if there is no client or WebView. */
    @Test
    fun onDestroyViewBeforeInitializationIsSafe() {
        WebViewAuthorizationFragment().onDestroyView()
    }

    /** A constructed client is cleaned even when the view has not been assigned. */
    @Test
    fun onDestroyViewCleansClientWithoutWebView() {
        val fragment = WebViewAuthorizationFragment()
        val client: AzureActiveDirectoryWebViewClient = mock()
        ReflectionHelpers.setField(fragment, "mAADWebViewClient", client)
        fragment.onDestroyView()
        verify(client).removeAuthUxDocumentStartScript()
    }

    /** Repeated teardown can safely invoke the client's idempotent registration cleanup. */
    @Test
    fun repeatedViewTeardownUsesRegistrationCleanupOnly() {
        val fragment = WebViewAuthorizationFragment()
        val client: AzureActiveDirectoryWebViewClient = mock()
        ReflectionHelpers.setField(fragment, "mAADWebViewClient", client)
        fragment.onDestroyView()
        fragment.onDestroyView()
        verify(client, times(2)).removeAuthUxDocumentStartScript()
        verify(client, never()).onDestroy()
    }

    /** The hidden authorization fragment inherits the same view-scoped cleanup. */
    @Test
    fun silentFragmentInheritsAuthUxCleanup() {
        val fragment = SilentWebViewAuthorizationFragment()
        val client: AzureActiveDirectoryWebViewClient = mock()
        ReflectionHelpers.setField(fragment, "mAADWebViewClient", client)
        fragment.onDestroyView()
        verify(client).removeAuthUxDocumentStartScript()
        verify(client, never()).onDestroy()
    }
}
