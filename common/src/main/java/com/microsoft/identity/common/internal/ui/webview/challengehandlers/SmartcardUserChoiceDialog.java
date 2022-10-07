package com.microsoft.identity.common.internal.ui.webview.challengehandlers;

import android.app.Activity;
import android.content.DialogInterface;

import androidx.appcompat.app.AlertDialog;

import com.microsoft.identity.common.R;

import lombok.NonNull;

public class SmartcardUserChoiceDialog extends SmartcardDialog {

    private static final String TAG = SmartcardUserChoiceDialog.class.getSimpleName();
    private final PositiveButtonListener mPositiveButtonListener;
    private final CancelCbaCallback mCancelCbaCallback;
    /**
     * Creates new instance of SmartcardDialog.
     *
     * @param activity Host activity.
     */
    public SmartcardUserChoiceDialog(@NonNull final PositiveButtonListener positiveButtonListener,
                                     @NonNull final CancelCbaCallback cancelCbaCallback,
                                     @NonNull final Activity activity) {
        super(activity);
        mPositiveButtonListener = positiveButtonListener;
        mCancelCbaCallback = cancelCbaCallback;
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
                        .setTitle("Prepare your smartcard")
                        //Sets subtext of the title.
                        .setMessage("Plug in or hold smartcard to back of phone.")
                        .setPositiveButton("See certificates on device", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                mPositiveButtonListener.onClick();
                            }
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                mCancelCbaCallback.onCancel();
                            }
                        });
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
        mCancelCbaCallback.onCancel();
    }

    /**
     * Listener interface for a positive button click.
     */
    public interface PositiveButtonListener {
        void onClick();
    }

    /**
     * Callback interface for when CBA is being cancelled.
     */
    public interface CancelCbaCallback {
        void onCancel();
    }
}
