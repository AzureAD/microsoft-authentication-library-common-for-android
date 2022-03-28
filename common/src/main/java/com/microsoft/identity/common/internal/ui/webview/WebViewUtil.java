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

package com.microsoft.identity.common.internal.ui.webview;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.webkit.CookieManager;
import android.webkit.WebView;

import androidx.annotation.NonNull;

import com.microsoft.identity.common.logging.Logger;

import static com.microsoft.identity.common.internal.ui.webview.ProcessUtil.AuthServiceProcess;

public class WebViewUtil {
    private static final String TAG = WebViewUtil.class.getSimpleName();

    /**
     * Must be invoked before WebView or CookieManager is invoked in the process.
     * See https://developer.android.com/about/versions/pie/android-9.0-changes-28#web-data-dirs for more info.
     */
    @SuppressLint("NewApi")
    public static void setDataDirectorySuffix(@NonNull final Context context) {
        final String methodTag = TAG + ":setDataDirectorySuffix";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                if (ProcessUtil.isRunningOnAuthService(context)) {
                    WebView.setDataDirectorySuffix(AuthServiceProcess);
                }
            } catch (final IllegalStateException e) {
                Logger.warn(methodTag, "WebView is already initialized. IllegalStateException is expected when setDataDirectorySuffix() is invoked");
            }
        }
    }

    /**
     * Sets whether WebView should send and accept cookies.
     */
    public static void setAcceptCookie(final boolean acceptCookie, final Context context) {
        final CookieManager cookieManager = getCookieManager(context);
        cookieManager.setAcceptCookie(acceptCookie);
    }

    /**
     * Clear all cookies from embedded webview.
     * This is a blocking call and so should not be called on UI thread.
     */
    @SuppressWarnings("deprecation")
    public static void removeCookiesFromWebView(final Context context) {
        final CookieManager cookieManager = getCookieManager(context);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            cookieManager.removeAllCookies(null);
            cookieManager.flush();
        } else {
            final android.webkit.CookieSyncManager syncManager = android.webkit.CookieSyncManager.createInstance(context);
            cookieManager.removeAllCookie();
            syncManager.sync();
        }
    }

    /**
     * Clear session cookies from embedded webview.
     * This is a blocking call and so should not be called on UI thread.
     */
    @SuppressWarnings("deprecation")
    public static void removeSessionCookiesFromWebView(final Context context) {
        final CookieManager cookieManager = getCookieManager(context);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            cookieManager.removeAllCookies(null);
            cookieManager.flush();
        } else {
            final android.webkit.CookieSyncManager syncManager = android.webkit.CookieSyncManager.createInstance(context);
            cookieManager.removeSessionCookie();
            syncManager.sync();
        }
    }

    private static CookieManager getCookieManager(final Context context) {
        setDataDirectorySuffix(context);
        return CookieManager.getInstance();
    }
}
