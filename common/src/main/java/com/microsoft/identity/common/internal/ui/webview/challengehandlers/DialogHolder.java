package com.microsoft.identity.common.internal.ui.webview.challengehandlers;

import android.app.Activity;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import net.jcip.annotations.ThreadSafe;

import java.util.List;

/**
 * Builds and shows SmartcardDialog instances while keeping track of the current dialog being shown to the user.
 */
@ThreadSafe
public class DialogHolder {
    //Current host activity.
    private final Activity mActivity;
    //The current dialog that is showing, if any.
    private SmartcardDialog mCurrentDialog;

    /**
     * Creates new instance of DialogHolder.
     * @param activity Current host activity.
     */
    DialogHolder(Activity activity) {
        mActivity = activity;
        mCurrentDialog = null;
    }

    /**
     * Build and show picker that prompts user to select a certificate for authentication.
     * @param certList List of YubiKitCertDetails that contains cert details only pertinent to the cert picker.
     * @param positiveButtonListener A PositiveButtonListener to be set for a SmartcardCertPickerDialog.
     * @param negativeButtonListener A NegativeButtonListener to be set for a SmartcardCertPickerDialog.
     * @param cancelCbaCallback      A Callback that holds code to be run when a YubiKey unexpectedly unplugs.
     */
    public synchronized void showCertPickerDialog(@NonNull final List<ClientCertAuthChallengeHandler.YubiKitCertDetails> certList,
                                                  @NonNull final SmartcardCertPickerDialog.PositiveButtonListener positiveButtonListener,
                                                  @NonNull final SmartcardCertPickerDialog.NegativeButtonListener negativeButtonListener,
                                                  @NonNull final SmartcardCertPickerDialog.CancelCbaCallback cancelCbaCallback) {
        final SmartcardCertPickerDialog certPickerDialog = new SmartcardCertPickerDialog(
                certList,
                positiveButtonListener,
                negativeButtonListener,
                cancelCbaCallback,
                mActivity);
        //Show cert picker dialog.
        showDialog(certPickerDialog);
    }

    /**
     * Build and show PIN dialog that prompts user to type in their PIN for their YubiKey.
     * @param positiveButtonListener A PositiveButtonListener to be set for a SmartcardPinDialog.
     * @param negativeButtonListener A NegativeButtonListener to be set for a SmartcardPinDialog.
     * @param cancelCbaCallback      A Callback that holds code to be run when a YubiKey unexpectedly unplugs.
     */
    public synchronized void showPinDialog(@NonNull final SmartcardPinDialog.PositiveButtonListener positiveButtonListener,
                                           @NonNull final SmartcardPinDialog.NegativeButtonListener negativeButtonListener,
                                           @NonNull final SmartcardPinDialog.CancelCbaCallback cancelCbaCallback) {
        final SmartcardPinDialog pinDialog = new SmartcardPinDialog(
                positiveButtonListener,
                negativeButtonListener,
                cancelCbaCallback,
                mActivity);
        //Show PinDialog, which should always be called after a positive button press.
        showDialog(pinDialog);
    }

    /**
     * Builds and shows dialog informing the user of an expected or unexpected error.
     * @param titleStringResourceId String resource id of the title text.
     * @param messageStringResourceId String resource id of the message text.
     */
    public synchronized void showErrorDialog(final int titleStringResourceId, final int messageStringResourceId) {
        showDialog(new SmartcardErrorDialog(
                titleStringResourceId,
                messageStringResourceId,
                new SmartcardErrorDialog.PositiveButtonListener() {
                    @Override
                    public void onClick() {
                        //Reset currentDialog to null
                        mCurrentDialog = null;
                    }
                },
                mActivity));
    }

    /**
     * Dismisses current dialog, if one is showing.
     */
    public synchronized void dismissDialog() {
        showDialog(null);
    }

    /**
     * Shows provided SmartcardDialog if not null.
     * Automatically dismisses existing dialog (if showing).
     * @param dialog SmartcardDialog object to be shown.
     */
    public synchronized void showDialog(@Nullable final SmartcardDialog dialog) {
        //Dismiss current dialog, if one is currently showing.
        if (mCurrentDialog != null) {
            mCurrentDialog.dismiss();
        }
        //Set current dialog, which could be null.
        mCurrentDialog = dialog;
        //If current dialog is not null, show.
        if (mCurrentDialog != null) {
            // dispatch to main thread if not on main thread
            mCurrentDialog.show();
        }
    }

    /**
     * Informs if an existing dialog is currently showing.
     * @return True if a SmartcardDialog is currently showing. False otherwise.
     */
    public synchronized boolean isDialogShowing() {
        return (mCurrentDialog != null);
    }

    /**
     * Runs the onCancelCbaCallback code for the current dialog.
     * Used when YubiKey is unexpectedly disconnected from device.
     */
    public synchronized void onCancelCba() {
        if (mCurrentDialog != null) {
            mCurrentDialog.onCancelCba();
        }
    }

    /**
     * Sets error mode for an existing SmartcardPinDialog.
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public synchronized void setPinDialogErrorMode() {
        if (mCurrentDialog instanceof SmartcardPinDialog) {
            ((SmartcardPinDialog) mCurrentDialog).setErrorMode();
        }
    }
}
