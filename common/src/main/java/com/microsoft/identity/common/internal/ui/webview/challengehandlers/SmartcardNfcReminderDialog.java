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

public class SmartcardNfcReminderDialog extends SmartcardDialog {

    private final PositiveButtonListener mPositiveButtonListener;

    public SmartcardNfcReminderDialog(@NonNull final PositiveButtonListener positiveButtonListener,
                                      @NonNull final Activity activity) {
        super(activity);
        mPositiveButtonListener = positiveButtonListener;
        createDialog();
    }

    /**
     * Should build an Android Dialog object and set it to mDialog.
     * Dialog objects must be built/interacted with on the UI thread.
     */
    @Override
    void createDialog() {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final AlertDialog.Builder builder = new AlertDialog.Builder(mActivity, R.style.UserChoiceAlertDialogTheme)
                        //Sets topmost text of dialog.
                        .setTitle(R.string.smartcard_nfc_reminder_dialog_title)
                        //Sets subtext of the title.
                        .setMessage(R.string.smartcard_nfc_reminder_dialog_message)
                        .setPositiveButton(R.string.smartcard_nfc_reminder_dialog_positive_button, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mPositiveButtonListener.onClick();
                            }
                        });
                final AlertDialog dialog = builder.create();
                //If user touches outside dialog, the default behavior makes the dialog disappear without really doing anything.
                //Adding this line in disables this default behavior so that the user can only exit by hitting the cancel button.
                dialog.setCanceledOnTouchOutside(false);
                //Handle back button the same as the positive button.
                dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        mPositiveButtonListener.onClick();
                    }
                });
                mDialog = dialog;
            }
        });
    }

    /**
     * Should dismiss dialog and call the appropriate methods to help cancel the CBA flow.
     */
    @Override
    void onCancelCba() {
        //This method will never be called on this dialog, so no logic needed.
    }

    /**
     * Listener interface for a positive button click.
     */
    public interface PositiveButtonListener {
        void onClick();
    }
}
