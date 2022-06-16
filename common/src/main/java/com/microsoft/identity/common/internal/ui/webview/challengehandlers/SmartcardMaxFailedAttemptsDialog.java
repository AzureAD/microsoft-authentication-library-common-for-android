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
import android.content.DialogInterface;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.microsoft.identity.common.R;

/**
 * Show simple dialog when the user has incorrectly entered their PIN the maximum amount of times allowed.
 */
public class SmartcardMaxFailedAttemptsDialog extends SmartcardDialog {

    private final PositiveButtonListener mPositiveButtonListener;

    /**
     * Create new instance of SmartcardMaxFailedAttemptsDialog.
     * @param activity Host activity.
     */
    public SmartcardMaxFailedAttemptsDialog(@NonNull final PositiveButtonListener positiveButtonListener,
                                            @NonNull final Activity activity) {
        super(activity);
        mPositiveButtonListener = positiveButtonListener;
        createDialog();
    }

    /**
     * Builds an AlertDialog that informs user that they have incorrectly entered their PIN the maximum amount of times allowed..
     */
    protected void createDialog() {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //Start building dialog
                AlertDialog.Builder builder = new AlertDialog.Builder(mActivity, R.style.ErrorAlertDialogTheme)
                        //Sets topmost text of dialog.
                        .setTitle(R.string.smartcard_max_attempt_dialog_title)
                        //Sets subtext of the title.
                        .setMessage(R.string.smartcard_max_attempt_dialog_message)
                        //In most cases, will set local dialog variable to null.
                        .setPositiveButton(R.string.smartcard_max_attempt_dialog_positive_button, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (mPositiveButtonListener != null) {
                                    mPositiveButtonListener.onClick();
                                }
                            }
                        });
                AlertDialog dialog = builder.create();
                //If user touches outside dialog, the default behavior makes the dialog disappear without really doing anything.
                //Adding this line in disables this default behavior so that the user can only exit by hitting the positive button.
                dialog.setCanceledOnTouchOutside(false);
                //Handle back button the same as the positive button.
                dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        if (mPositiveButtonListener != null) {
                            mPositiveButtonListener.onClick();
                        }
                    }
                });
                mDialog = dialog;
            }
        });
    }

    /**
     * Handles scenario when CBA is canceled unexpectedly (for example. when a YubiKey is unplugged while a dialog is showing).
     */
    @Override
    void onCancelCba() {
        dismiss();
    }

    /**
     * Listener interface for a positive button click.
     */
    public interface PositiveButtonListener {
        void onClick();
    }
}
