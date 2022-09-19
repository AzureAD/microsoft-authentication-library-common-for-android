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
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import com.microsoft.identity.common.R;
import com.microsoft.identity.common.java.providers.RawAuthorizationResult;
import com.microsoft.identity.common.java.ui.webview.authorization.IAuthorizationCompletionCallback;
import com.microsoft.identity.common.logging.Logger;

/**
 * Http authorization handler for NTLM challenge on web view.
 */
public final class NtlmChallengeHandler implements IChallengeHandler<NtlmChallenge, Void> {
    private static final String TAG = NtlmChallengeHandler.class.getSimpleName();
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

    private void cancelRequest() {
        final String methodTag = TAG + ":cancelRequest";
        Logger.info(methodTag,"Sending intent to cancel authentication activity");
        mChallengeCallback.onChallengeResponseReceived(
                RawAuthorizationResult.fromResultCode(RawAuthorizationResult.ResultCode.CANCELLED));
    }
}
