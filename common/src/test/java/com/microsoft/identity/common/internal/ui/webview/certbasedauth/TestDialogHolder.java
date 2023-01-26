package com.microsoft.identity.common.internal.ui.webview.certbasedauth;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

import lombok.Getter;

@Getter
class TestDialogHolder implements IDialogHolder {

    private TestDialog mCurrentDialog;
    private SmartcardCertPickerDialog.PositiveButtonListener mCertPickerPositiveButtonListener;
    private SmartcardCertPickerDialog.CancelCbaCallback mCertPickerCancelCbaCallback;
    private SmartcardPinDialog.PositiveButtonListener mPinPositiveButtonListener;
    private SmartcardPinDialog.CancelCbaCallback mPinCancelCbaCallback;
    private UserChoiceDialog.PositiveButtonListener mUserChoicePositiveButtonListener;
    private UserChoiceDialog.CancelCbaCallback mUserChoiceCancelCbaCallback;
    private SmartcardPromptDialog.CancelCbaCallback mPromptCancelCbaCallback;
    private SmartcardNfcPromptDialog.CancelCbaCallback mNfcPromptCancelCbaCallback;
    private SmartcardNfcReminderDialog.DismissCallback mNfcReminderDismissCallback;
    private List<ICertDetails> mCertList;


    public TestDialogHolder() {
        mCurrentDialog = null;
        mCertPickerPositiveButtonListener = null;
        mCertPickerCancelCbaCallback = null;
        mPinPositiveButtonListener = null;
        mPinCancelCbaCallback = null;
    }

    @Override
    public void showCertPickerDialog(@NonNull List<ICertDetails> certList, @NonNull SmartcardCertPickerDialog.PositiveButtonListener positiveButtonListener, @NonNull SmartcardCertPickerDialog.CancelCbaCallback cancelCbaCallback) {
        mCurrentDialog = TestDialog.cert_picker;
        mCertPickerPositiveButtonListener = positiveButtonListener;
        mCertPickerCancelCbaCallback = cancelCbaCallback;
        mCertList = certList;
    }

    @Override
    public void showPinDialog(@NonNull SmartcardPinDialog.PositiveButtonListener positiveButtonListener, @NonNull SmartcardPinDialog.CancelCbaCallback cancelCbaCallback) {
        mCurrentDialog = TestDialog.pin;
        mPinPositiveButtonListener = positiveButtonListener;
        mPinCancelCbaCallback = cancelCbaCallback;
    }

    @Override
    public void showErrorDialog(int titleStringResourceId, int messageStringResourceId) {
        mCurrentDialog = TestDialog.error;
    }

    @Override
    public void showUserChoiceDialog(@NonNull UserChoiceDialog.PositiveButtonListener positiveButtonListener, @NonNull UserChoiceDialog.CancelCbaCallback cancelCbaCallback) {
        mCurrentDialog = TestDialog.user_choice;
        mUserChoicePositiveButtonListener = positiveButtonListener;
        mUserChoiceCancelCbaCallback = cancelCbaCallback;
    }

    @Override
    public void showSmartcardPromptDialog(@NonNull SmartcardPromptDialog.CancelCbaCallback cancelCbaCallback) {
        mCurrentDialog = TestDialog.prompt;
        mPromptCancelCbaCallback = cancelCbaCallback;
    }

    @Override
    public void showSmartcardNfcLoadingDialog() {
        mCurrentDialog = TestDialog.nfc_loading;
    }

    @Override
    public void showSmartcardNfcPromptDialog(@NonNull SmartcardNfcPromptDialog.CancelCbaCallback cancelCbaCallback) {
        mCurrentDialog = TestDialog.nfc_prompt;
        mNfcPromptCancelCbaCallback = cancelCbaCallback;
    }

    @Override
    public void showSmartcardNfcReminderDialog(@NonNull SmartcardNfcReminderDialog.DismissCallback dismissCallback) {
        mCurrentDialog = TestDialog.nfc_reminder;
        mNfcReminderDismissCallback = dismissCallback;
    }

    @Override
    public void dismissDialog() {
        mCurrentDialog = null;
    }

    @Override
    public void showDialog(@Nullable SmartcardDialog dialog) {

    }

    @Override
    public boolean isDialogShowing() {
        return mCurrentDialog != null;
    }

    @Override
    public void onCancelCba() {

    }

    @Override
    public void setPinDialogErrorMode() {

    }
}
