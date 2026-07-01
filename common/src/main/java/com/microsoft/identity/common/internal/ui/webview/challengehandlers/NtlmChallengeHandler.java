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
package com.microsoft.identity.common.internal.ui.webview.challengehandlers;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.microsoft.identity.common.R;
import com.microsoft.identity.common.java.flighting.CommonFlight;
import com.microsoft.identity.common.java.flighting.CommonFlightsManager;
import com.microsoft.identity.common.java.opentelemetry.OTelUtility;
import com.microsoft.identity.common.java.providers.RawAuthorizationResult;
import com.microsoft.identity.common.java.ui.webview.authorization.IAuthorizationCompletionCallback;
import com.microsoft.identity.common.logging.Logger;

import io.opentelemetry.api.metrics.LongCounter;

/**
 * Http authorization handler for NTLM challenge on web view.
 */
public final class NtlmChallengeHandler implements IChallengeHandler<NtlmChallenge, Void> {
    private static final String TAG = NtlmChallengeHandler.class.getSimpleName();

    /**
     * Upper bound on the length of a displayed origin value (the host). The value is server-supplied,
     * so it is capped to keep the dialog readable and bounded.
     */
    private static final int MAX_ORIGIN_VALUE_LENGTH = 256;

    /**
     * Multiple of {@link #MAX_ORIGIN_VALUE_LENGTH} to which an untrusted value is truncated <em>before</em>
     * the sanitization regexes run, so a pathologically large header value cannot cause unnecessary
     * regex work on the UI thread. The final display cap is applied after sanitization.
     */
    private static final int PRE_SANITIZE_CAP = MAX_ORIGIN_VALUE_LENGTH * 4;

    /**
     * Counts how many times the request-origin row was successfully shown in the HTTP auth dialog.
     * Emitted only when the {@link CommonFlight#ENABLE_HTTP_AUTH_ORIGIN_DISPLAY} flight is on, this is
     * the signal used to confirm the flighted path is exercising correctly after the flight is ramped.
     */
    private static final LongCounter sHttpAuthOriginDisplayedCount = OTelUtility.createLongCounter(
            "http_auth_origin_displayed_count",
            "Number of times the request origin row was shown in the HTTP auth dialog"
    );

    private final Activity mActivity;
    private final IAuthorizationCompletionCallback mChallengeCallback;

    /**
     * Constructor of NtlmChallengeHandler.
     *
     * @param activity activity to place the UI
     * @param callback challenge completion callback which will process the challenge result.
     */
    public NtlmChallengeHandler(final Activity activity,
                                final IAuthorizationCompletionCallback callback) {
        mActivity = activity;
        mChallengeCallback = callback;
    }

    /**
     * Process the NTLM Challenge. If the credentials stored for the current host exists, use the
     * users credentials to resolve the NTLM challenge. Otherwise, show the http auth dialog on UI,
     * user will need to type in the username and password to resolve the NTML challenge.
     */
    @Override
    public Void processChallenge(final NtlmChallenge ntlmChallenge) {
        showHttpAuthDialog(ntlmChallenge);
        return null;
    }

    private void showHttpAuthDialog(final NtlmChallenge ntlmChallenge) {
        final String methodTag = TAG + ":showHttpAuthDialog";

        final LayoutInflater factory = LayoutInflater.from(mActivity);
        final View v = factory.inflate(mActivity.getResources().getLayout(R.layout.http_auth_dialog), null);
        final EditText usernameView = (EditText) v.findViewById(R.id.editUserName);
        final EditText passwordView = (EditText) v.findViewById(R.id.editPassword);
        setOriginTextIfEnabled(v, ntlmChallenge);
        final String title = mActivity.getText(R.string.http_auth_dialog_title).toString();
        final AlertDialog.Builder httpAuthDialog = new AlertDialog.Builder(mActivity);
        httpAuthDialog.setTitle(title)
                .setView(v)
                .setPositiveButton(R.string.http_auth_dialog_login,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                Logger.info(methodTag,"Proceeding with user supplied username and password.");
                                ntlmChallenge.getHandler().proceed(usernameView.getText().toString(), passwordView.getText().toString());
                            }
                        })
                .setNegativeButton(R.string.http_auth_dialog_cancel,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                ntlmChallenge.getHandler().cancel();
                                cancelRequest();
                            }
                        })
                .setOnCancelListener(
                        new DialogInterface.OnCancelListener() {
                            public void onCancel(DialogInterface dialog) {
                                ntlmChallenge.getHandler().cancel();
                                cancelRequest();
                            }
                        }).create().show();
    }

    /**
     * Sets the request origin details on the dialog when the flight is enabled.
     *
     * @param dialogView    the inflated dialog view
     * @param ntlmChallenge the challenge containing request origin details
     */
    void setOriginTextIfEnabled(final View dialogView, final NtlmChallenge ntlmChallenge) {
        if (!isHttpAuthOriginDisplayEnabled()) {
            return;
        }

        final String originText = getOriginText(ntlmChallenge);
        if (TextUtils.isEmpty(originText)) {
            return;
        }

        final TextView originView = (TextView) dialogView.findViewById(R.id.httpAuthOriginText);
        originView.setText(originText);
        originView.setVisibility(View.VISIBLE);
        sHttpAuthOriginDisplayedCount.add(1);
    }

    /**
     * Gets the request origin text displayed in the dialog.
     * <p>
     * v1 displays the <strong>host only</strong>. The realm is fully server-controlled free text, so a
     * malicious origin could pair a suspicious host with a reassuring realm line (e.g. "Microsoft
     * Corporate Login") sitting right next to it; displaying the realm is deferred pending security
     * review (see PR #3171 discussion). The {@link NtlmChallenge#getRealm() realm} is intentionally
     * not read here.
     *
     * @param ntlmChallenge the challenge containing request origin details
     * @return the formatted origin text, or an empty string when no host is available
     */
    String getOriginText(final NtlmChallenge ntlmChallenge) {
        final String host = sanitizeOriginValue(ntlmChallenge.getHost());
        if (TextUtils.isEmpty(host)) {
            return "";
        }

        return mActivity.getString(R.string.http_auth_dialog_origin_host, host);
    }

    /**
     * Sanitizes a server-supplied origin value (the host) before it is rendered in the dialog.
     * <p>
     * The host comes from the challenge and is <em>not</em> fully trusted — a WebView may have been
     * navigated to a malicious origin, which is exactly the case this transparency feature exists to
     * expose. Without sanitization a hostile value could embed CR/LF or other control characters to
     * inject additional lines into the credential dialog and spoof its content, or use invisible
     * Unicode <em>format</em> characters (category {@code \p{Cf}}, e.g. U+202E RIGHT-TO-LEFT OVERRIDE)
     * to visually reorder the rendered text — turning this feature into a phishing surface. Control
     * characters, Unicode format characters, and line/paragraph separators are collapsed to single
     * spaces so the value stays on one visual line, and the result is length-capped. The input is
     * pre-capped ({@link #PRE_SANITIZE_CAP}) before the regex passes to bound UI-thread work on very
     * large values.
     *
     * @param value the raw, untrusted origin value
     * @return a single-line, length-bounded value safe to display
     */
    static String sanitizeOriginValue(final String value) {
        if (TextUtils.isEmpty(value)) {
            return "";
        }

        // Pre-cap the untrusted input before running the regexes so a very large header value can't
        // cause unnecessary UI-thread work. A small multiple of the display cap leaves room for the
        // whitespace-collapse step before the final cap is applied.
        String sanitized = value.length() > PRE_SANITIZE_CAP
                ? value.substring(0, PRE_SANITIZE_CAP)
                : value;

        sanitized = sanitized
                .replaceAll("[\\p{Cc}\\p{Cf}\\p{Zl}\\p{Zp}]", " ")
                .replaceAll("\\s+", " ")
                .trim();

        if (sanitized.length() > MAX_ORIGIN_VALUE_LENGTH) {
            sanitized = sanitized.substring(0, MAX_ORIGIN_VALUE_LENGTH);
        }

        return sanitized;
    }

    /**
     * Checks whether the request origin display flight is enabled.
     *
     * @return true if the request origin should be shown
     */
    boolean isHttpAuthOriginDisplayEnabled() {
        return CommonFlightsManager.INSTANCE.getFlightsProvider()
                .isFlightEnabled(CommonFlight.ENABLE_HTTP_AUTH_ORIGIN_DISPLAY);
    }

    private void cancelRequest() {
        final String methodTag = TAG + ":cancelRequest";
        Logger.info(methodTag,"Sending intent to cancel authentication activity");
        mChallengeCallback.onChallengeResponseReceived(
                RawAuthorizationResult.fromResultCode(RawAuthorizationResult.ResultCode.CANCELLED));
    }
}
