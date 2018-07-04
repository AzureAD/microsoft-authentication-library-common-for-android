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
package com.microsoft.identity.common.internal.ui.embeddedwebview.challengehandlers;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.HttpAuthHandler;
import android.webkit.WebView;
import android.widget.EditText;

import com.microsoft.identity.common.R;
import com.microsoft.identity.common.adal.internal.AuthenticationConstants;
import com.microsoft.identity.common.internal.logging.Logger;

/**
 * Http authorization handler for NTLM challenge on web view.
 */
public final class NtlmChallengeHandler implements IChallengeHandler {
    private static final String TAG = NtlmChallengeHandler.class.getSimpleName();
    private NtlmChallenge mNtlmChallenge;
    private Context mContext;
    private IChallengeCompletionCallback mChallengeCallback;

    /**
     * Constructor of NtlmChallengeHandler.
     * @param view
     * @param handler
     * @param host
     * @param realm
     * @param context
     * @param callback
     */
    public NtlmChallengeHandler(final WebView view,
                                final HttpAuthHandler handler,
                                final String host,
                                final String realm,
                                final Context context,
                                final IChallengeCompletionCallback callback) {
        mNtlmChallenge = new NtlmChallenge(view, handler, host, realm);
        mContext = context;
        mChallengeCallback = callback;
    }

    /**
     * Process the NTLM Challenge. If the credentials stored for the current host exists, use the
     * users credentials to resolve the NTML challenge. Otherwise, show the http auth dialog on UI,
     * user will need to type in the username and password to resolve the NTML challenge.
     */
    public void process() {
        if (mNtlmChallenge.getHandler().useHttpAuthUsernamePassword()
                && mNtlmChallenge.getView() != null) {
            final String[] haup = mNtlmChallenge.getView()
                    .getHttpAuthUsernamePassword(mNtlmChallenge.getHost(), mNtlmChallenge.mRealm);
            if (haup != null && haup.length == 2) {
                final String userName = haup[0];
                final String password = haup[1];
                if (userName != null && password != null) {
                    mNtlmChallenge.getHandler().proceed(userName, password);
                }
            }
        } else {
            showHttpAuthDialog();
        }
    }

    private void showHttpAuthDialog() {
        final LayoutInflater factory = LayoutInflater.from(mContext);
        final View v = factory.inflate(mContext.getResources().getLayout(R.layout.http_auth_dialog), null);
        final EditText usernameView = (EditText) v.findViewById(R.id.editUserName);
        final EditText passwordView = (EditText) v.findViewById(R.id.editPassword);
        final String title = mContext.getText(R.string.http_auth_dialog_title).toString();
        final AlertDialog.Builder httpAuthDialog = new AlertDialog.Builder(mContext);
        httpAuthDialog.setTitle(title)
                .setView(v)
                .setPositiveButton(R.string.http_auth_dialog_login,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                mNtlmChallenge.getHandler().proceed(usernameView.getText().toString(), passwordView.getText().toString());
                            }
                        })
                .setNegativeButton(R.string.http_auth_dialog_cancel,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                mNtlmChallenge.getHandler().cancel();
                                cancelRequest();
                            }
                        })
                .setOnCancelListener(
                        new DialogInterface.OnCancelListener() {
                            public void onCancel(DialogInterface dialog) {
                                mNtlmChallenge.getHandler().cancel();
                                cancelRequest();
                            }
                        }).create().show();
    }

    private void cancelRequest() {
        Logger.verbose(TAG, "Sending intent to cancel authentication activity");
        mChallengeCallback.sendResponse(AuthenticationConstants.UIResponse.BROWSER_CODE_CANCEL, new Intent());
    }

    class NtlmChallenge {
        private HttpAuthHandler mHandler;
        private WebView mView;
        private String mHost;
        private String mRealm;

        NtlmChallenge(final WebView view,
                      final HttpAuthHandler handler,
                      final String host,
                      final String realm) {
            mHandler = handler;
            mView = view;
            mHost = host;
            mRealm = realm;
        }

        HttpAuthHandler getHandler() {
            return mHandler;
        }

        WebView getView() {
            return mView;
        }

        String getHost() {
            return mHost;
        }

        String getRealm() {
            return mRealm;
        }
    }
}
