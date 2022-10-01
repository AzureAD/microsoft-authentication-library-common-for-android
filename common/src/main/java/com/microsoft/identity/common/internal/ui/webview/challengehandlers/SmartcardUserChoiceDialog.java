package com.microsoft.identity.common.internal.ui.webview.challengehandlers;

import android.app.Activity;
import androidx.appcompat.app.AlertDialog;

import androidx.annotation.NonNull;

public class SmartcardUserChoiceDialog extends SmartcardDialog {
    /**
     * Creates new instance of SmartcardDialog.
     *
     * @param activity Host activity.
     */
    public SmartcardUserChoiceDialog(@NonNull Activity activity) {
        super(activity);
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
                final AlertDialog.Builder builder = new AlertDialog.Builder(mActivity)
                        //Sets topmost text of dialog.
                        .setTitle("Tap your smartcard")
                        //Sets subtext of the title.
                        .setMessage("Plug in or tap smartcard to back of phone.");
                final androidx.appcompat.app.AlertDialog dialog = builder.create();
                //If user touches outside dialog, the default behavior makes the dialog disappear without really doing anything.
                //Adding this line in disables this default behavior so that the user can only exit by hitting the positive button.
                dialog.setCanceledOnTouchOutside(false);
                mDialog = dialog;
            }
        });
    }

    /**
     * Should dismiss dialog and call the appropriate methods to help cancel the CBA flow.
     */
    @Override
    void onCancelCba() {

    }
}
