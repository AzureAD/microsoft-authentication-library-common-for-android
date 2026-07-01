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
package com.microsoft.identity.common.internal.providers;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.Nullable;

import com.microsoft.identity.common.java.logging.Logger;

/**
 * Common-provided deep-link receiver for broker-install request resume.
 *
 * <p>After the broker (Company Portal) is installed, its first launch deep-links back to
 * {@code msauth://<originPkg>/resume?resume=<cid>}. Because the deep-link host equals the origin
 * app's package (which equals the manifest placeholder {@code ${applicationId}}), this receiver
 * and its intent-filter are declared once in the shared common manifest and auto-merge into every
 * consuming app — so consumers (MSAL test app, OneAuth's 1P apps) register no receiver and no
 * intent-filter of their own.
 *
 * <p>The receiver is deliberately generic: it forwards only the single-use correlation id to the
 * consumer's own launcher activity (via {@link android.content.pm.PackageManager#getLaunchIntentForPackage}),
 * which then performs the consumer-specific resume (load the persisted request from the encrypted
 * store keyed by the id, adapt it to the consumer's request type, and re-invoke the interactive
 * API). Request parameters are never carried as extras — only the correlation id — so the full
 * request is always read from the encrypted {@code BrokerInstallResumeStore}.
 *
 * <p>Intentionally no-history and finishes immediately so it never becomes a UI dead-end.
 */
public class BrokerInstallResumeActivity extends Activity {

    private static final String TAG = BrokerInstallResumeActivity.class.getSimpleName();

    /**
     * Extra carrying the single-use broker-install resume correlation id from this receiver to the
     * consumer's launcher activity. The full request parameters are read from the encrypted store
     * keyed by this id — they are intentionally NOT passed as individual extras.
     */
    public static final String EXTRA_RESUME_CORRELATION_ID =
            "com.microsoft.identity.common.RESUME_CORRELATION_ID";

    /** Deep-link query parameter that carries the resume correlation id. */
    private static final String QUERY_PARAM_RESUME = "resume";

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Uri data = getIntent() != null ? getIntent().getData() : null;
        final String correlationId = data == null ? null : data.getQueryParameter(QUERY_PARAM_RESUME);

        if (correlationId == null) {
            Logger.warn(TAG, "Resume deep-link missing correlation id; nothing to resume.");
            finish();
            return;
        }

        final Intent launch = getPackageManager().getLaunchIntentForPackage(getPackageName());
        if (launch == null) {
            Logger.warn(TAG, "No launcher activity found for package; cannot resume.");
            finish();
            return;
        }

        Logger.info(TAG, "Forwarding broker-install resume for correlation id to launcher.");

        launch.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        launch.putExtra(EXTRA_RESUME_CORRELATION_ID, correlationId);
        startActivity(launch);
        finish();
    }
}
