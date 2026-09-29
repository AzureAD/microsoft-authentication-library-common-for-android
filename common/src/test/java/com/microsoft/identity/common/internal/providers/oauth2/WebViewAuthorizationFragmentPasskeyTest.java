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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import android.os.Build;
import android.webkit.WebView;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;

/**
 * Tests passkey listener cleanup during the authorization WebView lifecycle.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.P)
public class WebViewAuthorizationFragmentPasskeyTest {

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
}
