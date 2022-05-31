package com.microsoft.identity.common.internal.ui.webview.challengehandlers;

import android.app.Activity;

import androidx.appcompat.app.AlertDialog;

import com.microsoft.identity.common.R;

//Show simple dialog when the user has incorrectly attempted to enter their PIN the maximum amount of times allowed.
public class SmartcardMaxFailedAttemptsDialog extends SmartcardDialog {

    public SmartcardMaxFailedAttemptsDialog(Activity activity) {
        super(activity);
        createDialog();
    }

    protected void createDialog() {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //Start building dialog
                AlertDialog.Builder builder = new AlertDialog.Builder(mActivity, R.style.ErrorAlertDialogTheme)
                        .setTitle(R.string.smartcard_max_attempt_dialog_title)
                        .setMessage(R.string.smartcard_max_attempt_dialog_message)
                        .setPositiveButton(R.string.smartcard_max_attempt_dialog_positive_button, null);
                AlertDialog dialog = builder.create();
                mDialog = dialog;
            }
        });
    }
}
