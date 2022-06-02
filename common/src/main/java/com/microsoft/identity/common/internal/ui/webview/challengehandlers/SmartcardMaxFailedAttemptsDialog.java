package com.microsoft.identity.common.internal.ui.webview.challengehandlers;

import android.app.Activity;
import android.content.DialogInterface;

import androidx.appcompat.app.AlertDialog;

import com.microsoft.identity.common.R;

//Show simple dialog when the user has incorrectly attempted to enter their PIN the maximum amount of times allowed.
public class SmartcardMaxFailedAttemptsDialog extends SmartcardDialog {

    private PositiveButtonListener mPositiveButtonListener;

    public SmartcardMaxFailedAttemptsDialog(Activity activity) {
        super(activity);
        mPositiveButtonListener = null;
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

    @Override
    void onCancelCba() {
        dismiss();
    }

    //Listener interfaces and setters for the dialog buttons.
    public void setPositiveButtonListener(PositiveButtonListener listener) {
        mPositiveButtonListener = listener;
    }

    public interface PositiveButtonListener {
        void onClick();
    }
}
