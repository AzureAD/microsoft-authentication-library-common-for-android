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
package com.microsoft.identity.common.internal.ui.webview.certbasedauth;

import android.app.Activity;
import android.content.DialogInterface;

import androidx.appcompat.app.AlertDialog;

import com.microsoft.identity.common.R;

import lombok.NonNull;

/**
 * Builds a dialog that prompts the user to connect their smartcard, either by plugging in (USB) or holding to back of phone (NFC).
 */
public class SmartcardPromptDialog extends SmartcardDialog {

    private final ICancelCbaCallback mCancelCbaCallback;

    /**
     * Creates new instance of SmartcardPromptDialog.
     *
     * @param activity Host activity.
     */
    public SmartcardPromptDialog(@NonNull final ICancelCbaCallback cancelCbaCallback,
                                 @NonNull final Activity activity) {
        super(activity);
        mCancelCbaCallback = cancelCbaCallback;
        createDialog();
    }

    /**
     * Builds an Android Dialog object and set it to mDialog.
     */
    @Override
    void createDialog() {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final AlertDialog.Builder builder = new AlertDialog.Builder(mActivity, R.style.UserChoiceAlertDialogTheme)
                        //Sets topmost text of dialog.
                        .setTitle(R.string.smartcard_prompt_dialog_title)
                        //Sets subtext of the title.
                        .setMessage(R.string.smartcard_prompt_dialog_message)
                        .setNegativeButton(R.string.smartcard_prompt_dialog_negative_button, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                mCancelCbaCallback.onCancel();
                            }
                        });
                final androidx.appcompat.app.AlertDialog dialog = builder.create();
                //If user touches outside dialog, the default behavior makes the dialog disappear without really doing anything.
                //Adding this line in disables this default behavior so that the user can only exit by hitting the positive button.
                dialog.setCanceledOnTouchOutside(false);
                //Handle back button the same as the negative button.
                dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        mCancelCbaCallback.onCancel();
                    }
                });
                mDialog = dialog;
            }
        });
    }

    /**
     * Called when smartcard is unexpectedly disconnected via USB from device.
     * Used to run any cancellation logic needed (without the cancel button needing to be pressed).
     */
    @Override
    void onUnexpectedUnplug() {
        mCancelCbaCallback.onCancel();
    }
}
