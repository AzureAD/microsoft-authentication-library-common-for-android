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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

import lombok.Getter;
import lombok.experimental.Accessors;

@Getter @Accessors(prefix = "m")
class TestDialogHolder implements IDialogHolder {

    private TestDialog mCurrentDialog;
    private SmartcardCertPickerDialog.PositiveButtonListener mCertPickerPositiveButtonListener;
    private ICancelCbaCallback mCertPickerCancelCbaCallback;
    private SmartcardPinDialog.PositiveButtonListener mPinPositiveButtonListener;
    private ICancelCbaCallback mPinCancelCbaCallback;
    private UserChoiceDialog.PositiveButtonListener mUserChoicePositiveButtonListener;
    private ICancelCbaCallback mUserChoiceCancelCbaCallback;
    private ICancelCbaCallback mPromptCancelCbaCallback;
    private ICancelCbaCallback mNfcPromptCancelCbaCallback;
    private SmartcardNfcReminderDialog.DismissCallback mNfcReminderDismissCallback;
    private List<ICertDetails> mCertList;

    @Override
    public void showCertPickerDialog(@NonNull final List<ICertDetails> certList,
                                     @NonNull final SmartcardCertPickerDialog.PositiveButtonListener positiveButtonListener,
                                     @NonNull final ICancelCbaCallback cancelCbaCallback) {
        mCurrentDialog = TestDialog.cert_picker;
        mCertPickerPositiveButtonListener = positiveButtonListener;
        mCertPickerCancelCbaCallback = cancelCbaCallback;
        mCertList = certList;
    }

    @Override
    public void showPinDialog(@NonNull final SmartcardPinDialog.PositiveButtonListener positiveButtonListener,
                              @NonNull final ICancelCbaCallback cancelCbaCallback) {
        mCurrentDialog = TestDialog.pin;
        mPinPositiveButtonListener = positiveButtonListener;
        mPinCancelCbaCallback = cancelCbaCallback;
    }

    @Override
    public void showErrorDialog(final int titleStringResourceId,
                                final int messageStringResourceId) {
        mCurrentDialog = TestDialog.error;
    }

    @Override
    public void showUserChoiceDialog(@NonNull final UserChoiceDialog.PositiveButtonListener positiveButtonListener,
                                     @NonNull final ICancelCbaCallback cancelCbaCallback) {
        mCurrentDialog = TestDialog.user_choice;
        mUserChoicePositiveButtonListener = positiveButtonListener;
        mUserChoiceCancelCbaCallback = cancelCbaCallback;
    }

    @Override
    public void showSmartcardPromptDialog(@NonNull final ICancelCbaCallback cancelCbaCallback) {
        mCurrentDialog = TestDialog.prompt;
        mPromptCancelCbaCallback = cancelCbaCallback;
    }

    @Override
    public void showSmartcardNfcLoadingDialog() {
        mCurrentDialog = TestDialog.nfc_loading;
    }

    @Override
    public void showSmartcardNfcPromptDialog(@NonNull final ICancelCbaCallback cancelCbaCallback) {
        mCurrentDialog = TestDialog.nfc_prompt;
        mNfcPromptCancelCbaCallback = cancelCbaCallback;
    }

    @Override
    public void showSmartcardNfcReminderDialog(@NonNull final SmartcardNfcReminderDialog.DismissCallback dismissCallback) {
        mCurrentDialog = TestDialog.nfc_reminder;
        mNfcReminderDismissCallback = dismissCallback;
    }

    @Override
    public void dismissDialog() {
        mCurrentDialog = null;
    }

    @Override
    public void showDialog(@Nullable SmartcardDialog dialog) {}

    @Override
    public boolean isDialogShowing() {
        return mCurrentDialog != null;
    }

    @Override
    public void onCancelCba() {
        switch (mCurrentDialog) {
            case cert_picker:
                mCertPickerCancelCbaCallback.onCancel();
                break;
            case pin:
                mPinCancelCbaCallback.onCancel();
                break;
            case user_choice:
                mUserChoiceCancelCbaCallback.onCancel();
                break;
            case prompt:
                mPromptCancelCbaCallback.onCancel();
                break;
            case nfc_prompt:
                mNfcPromptCancelCbaCallback.onCancel();
                break;
        }
    }

    @Override
    public void setPinDialogErrorMode() {}
}
