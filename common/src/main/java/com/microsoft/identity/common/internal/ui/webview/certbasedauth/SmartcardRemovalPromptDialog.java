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
import android.app.AlertDialog;

import androidx.annotation.NonNull;

import com.microsoft.identity.common.R;

/**
 * Builds a dialog that prompts the user to remove their smartcard in order to proceed.
 */
public class SmartcardRemovalPromptDialog extends SmartcardDialog {

    private final IDismissCallback mDismissCallback;

    /**
     * Creates new instance of SmartcardRemovalPromptDialog.
     * @param activity Host activity.
     */
    public SmartcardRemovalPromptDialog(@NonNull final IDismissCallback dismissCallback,
                                        @NonNull final Activity activity) {
        super(activity);
        mDismissCallback = dismissCallback;
        createDialog();
    }

    /**
     * Builds an AndroidDialog object and sets it to the local variable.
     */
    @Override
    void createDialog() {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final AlertDialog.Builder builder = new AlertDialog.Builder(mActivity, R.style.TitleOnlyAlertDialogTheme)
                        //Sets topmost text of dialog.
                        .setTitle(R.string.smartcard_removal_prompt_dialog_title);
                final AlertDialog dialog = builder.create();
                //If user touches outside dialog, the default behavior makes the dialog disappear without really doing anything.
                //Adding this line in disables this default behavior so that the user can only exit by hitting the positive button.
                dialog.setCanceledOnTouchOutside(false);
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
        //Unplugging is expected here... so let's actually dismiss the dialog.
        mDismissCallback.onAction();
    }
}
