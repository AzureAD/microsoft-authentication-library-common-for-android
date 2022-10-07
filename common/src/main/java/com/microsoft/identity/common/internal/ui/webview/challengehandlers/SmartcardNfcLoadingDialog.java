package com.microsoft.identity.common.internal.ui.webview.challengehandlers;

import android.app.Activity;
import android.content.DialogInterface;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.microsoft.identity.common.R;

public class SmartcardNfcLoadingDialog extends SmartcardDialog {
    /**
     * Creates new instance of SmartcardNfcLoadingDialog.
     *
     * @param activity Host activity.
     */
    public SmartcardNfcLoadingDialog(@NonNull Activity activity) {
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
                final View nfcLoadingLayout = mActivity.getLayoutInflater().inflate(R.layout.nfc_loading_layout, null);
                final AlertDialog.Builder builder = new AlertDialog.Builder(mActivity, R.style.UserChoiceAlertDialogTheme)
                        //Sets topmost text of dialog.
                        .setTitle("Please keep holding smartcard to device")
                        .setView(nfcLoadingLayout);
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
