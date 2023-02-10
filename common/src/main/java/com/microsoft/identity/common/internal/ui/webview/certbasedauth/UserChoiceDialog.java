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

import java.util.ArrayList;
import java.util.Arrays;

public class UserChoiceDialog extends SmartcardDialog {

    private final PositiveButtonListener mPositiveButtonListener;
    private final CancelCbaCallback mCancelCbaCallback;

    /**
     * Creates new instance of SmartcardDialog.
     *
     * @param activity Host activity.
     */
    public UserChoiceDialog(@NonNull final PositiveButtonListener positiveButtonListener,
                            @NonNull final CancelCbaCallback cancelCbaCallback,
                            @NonNull final Activity activity) {
        super(activity);
        mPositiveButtonListener = positiveButtonListener;
        mCancelCbaCallback = cancelCbaCallback;
        createDialog();
    }

    /**
     * Builds an Android Dialog object and set it to mDialog.
     */
    @Override
    void createDialog() {
        //Create array for auth choices to pass into the AlertDialog.
        final ArrayList<String> choicesList = new ArrayList<>();
        choicesList.add(mActivity.getResources().getString(R.string.user_choice_dialog_on_device_name));
        choicesList.add(mActivity.getResources().getString(R.string.user_choice_dialog_smartcard_name));
        String[] choicesArray = Arrays.copyOf(choicesList.toArray(), choicesList.size(), String[].class);
        final AlertDialog.Builder builder = new AlertDialog.Builder(mActivity, R.style.UserChoiceAlertDialogTheme)
                .setTitle(R.string.user_choice_dialog_title)
                .setSingleChoiceItems(choicesArray, 0, null)
                .setPositiveButton(R.string.user_choice_dialog_positive_button, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        final int checkedPosition = ((AlertDialog) dialogInterface).getListView().getCheckedItemPosition();
                        //Position 0 -> On-device
                        //Position 1 -> Smartcard
                        mPositiveButtonListener.onClick(checkedPosition);
                    }
                })
                .setNegativeButton(R.string.user_choice_dialog_negative_button, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        mCancelCbaCallback.onCancel();
                    }
                });
        final AlertDialog dialog = builder.create();
        //If user touches outside dialog, the default behavior makes the dialog disappear without really doing anything.
        //Adding this line in disables this default behavior so that the user can only exit by hitting the cancel button.
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

    /**
     * Should dismiss dialog and call the appropriate methods to help cancel the CBA flow.
     */
    @Override
    void onSmartcardRemoval() {
        mCancelCbaCallback.onCancel();
    }

    /**
     * Listener interface for a positive button click.
     */
    public interface PositiveButtonListener {
        void onClick(final int checkedPosition);
    }

    /**
     * Callback interface for when CBA is being cancelled.
     */
    public interface CancelCbaCallback {
        void onCancel();
    }
}
