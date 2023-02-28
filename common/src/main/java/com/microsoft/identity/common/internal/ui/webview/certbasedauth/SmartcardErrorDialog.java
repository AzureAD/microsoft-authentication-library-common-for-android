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

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.microsoft.identity.common.R;

/**
 * Show simple dialog when the user has encountered an error or unexpected exception.
 */
public class SmartcardErrorDialog extends SmartcardDialog {

    private final int mTitleStringResourceId;
    private final int mMessageStringResourceId;
    private final IDismissCallback mDismissCallback;

    /**
     * Create new instance of SmartcardErrorDialog.
     * @param titleStringResourceId String resource id for text to be displayed as the title in dialog.
     * @param messageStringResourceId String resource id for text to be displayed as the message in dialog.
     * @param dismissCallback Implemented callback for when dialog is to be dismissed (positive button click or back button).
     * @param activity Host activity.
     */
    public SmartcardErrorDialog(final int titleStringResourceId,
                                final int messageStringResourceId,
                                @NonNull final IDismissCallback dismissCallback,
                                @NonNull final Activity activity) {
        super(activity);
        mTitleStringResourceId = titleStringResourceId;
        mMessageStringResourceId = messageStringResourceId;
        mDismissCallback = dismissCallback;
        createDialog();
    }

    /**
     * Builds an AlertDialog which informs users that they have encountered an error or unexpected exception.
     */
    protected void createDialog() {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //Start building dialog
                final AlertDialog.Builder builder = new AlertDialog.Builder(mActivity, R.style.ErrorAlertDialogTheme)
                        //Sets topmost text of dialog.
                        .setTitle(mTitleStringResourceId)
                        //Sets subtext of the title.
                        .setMessage(mMessageStringResourceId)
                        //In most cases, will set local dialog variable to null.
                        .setPositiveButton(R.string.smartcard_error_dialog_positive_button, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mDismissCallback.onAction();
                            }
                        });
                final AlertDialog dialog = builder.create();
                //If user touches outside dialog, the default behavior makes the dialog disappear without really doing anything.
                //Adding this line in disables this default behavior so that the user can only exit by hitting the positive button.
                dialog.setCanceledOnTouchOutside(false);
                //Handle back button the same as the positive button.
                dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        mDismissCallback.onAction();
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
        //Handle cancelling the same as the positive button.
        mDismissCallback.onAction();
    }
}
