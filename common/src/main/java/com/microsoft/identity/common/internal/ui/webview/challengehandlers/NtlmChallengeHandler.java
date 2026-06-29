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
     * Upper bound on the length of a displayed origin value (host/realm). The realm in particular is
     * server-controlled, so it is capped to keep the dialog readable and bounded.
     */
    private static final int MAX_ORIGIN_VALUE_LENGTH = 256;

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
     *
     * @param ntlmChallenge the challenge containing request origin details
     * @return the formatted origin text
     */
    String getOriginText(final NtlmChallenge ntlmChallenge) {
        final StringBuilder originText = new StringBuilder();
        final String host = sanitizeOriginValue(ntlmChallenge.getHost());
        if (!TextUtils.isEmpty(host)) {
            originText.append(mActivity.getString(R.string.http_auth_dialog_origin_host, host));
        }

        final String realm = sanitizeOriginValue(ntlmChallenge.getRealm());
        if (!TextUtils.isEmpty(realm)) {
            if (originText.length() > 0) {
                originText.append('\n');
            }
            originText.append(mActivity.getString(R.string.http_auth_dialog_origin_realm, realm));
        }

        return originText.toString();
    }

    /**
     * Sanitizes a server-supplied origin value (host or realm) before it is rendered in the dialog.
     * <p>
     * The realm is taken verbatim from the {@code WWW-Authenticate} response header and is therefore
     * fully attacker-controlled. Without sanitization a malicious server could embed CR/LF (or other
     * control characters) in the realm to inject additional lines into the credential dialog and spoof
     * its content — turning this transparency feature into a phishing surface. Control characters and
     * line/paragraph separators are collapsed to single spaces so the value stays on one visual line,
     * and the result is length-capped.
     *
     * @param value the raw, untrusted origin value
     * @return a single-line, length-bounded value safe to display
     */
    static String sanitizeOriginValue(final String value) {
        if (TextUtils.isEmpty(value)) {
            return "";
        }

        String sanitized = value
                .replaceAll("[\\p{Cc}\\p{Zl}\\p{Zp}]", " ")
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
