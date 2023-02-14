package com.microsoft.identity.common.internal.ui.webview.certbasedauth;

import android.app.Activity;
import android.app.AlertDialog;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.microsoft.identity.common.R;

public class SmartcardRemovalPromptDialog extends SmartcardDialog {

    private final RemovalCallback mRemovalCallback;

    public SmartcardRemovalPromptDialog(@Nullable RemovalCallback removalCallback,
                                        @NonNull Activity activity) {
        super(activity);
        mRemovalCallback = removalCallback;
        createDialog();
    }

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

    @Override
    void onSmartcardRemoval() {
        if (mRemovalCallback != null) {
            mRemovalCallback.onRemoved();
        }
    }

    public interface RemovalCallback {
        void onRemoved();
    }
}
