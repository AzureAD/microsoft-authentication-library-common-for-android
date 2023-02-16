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

/**
 * Builds and shows dialog instances while keeping track of the current dialog being shown to the user.
 */
public interface IDialogHolder {

    /**
     * Build and show picker that prompts user to select a certificate for authentication.
     * @param certList List of ICertDetails that contains cert details only pertinent to the cert picker.
     * @param positiveButtonListener A PositiveButtonListener to be set for a SmartcardCertPickerDialog.
     * @param cancelCbaCallback      A Callback that holds code to be run when CBA is being cancelled.
     */
    void showCertPickerDialog(@NonNull final List<ICertDetails> certList,
                              @NonNull final SmartcardCertPickerDialog.PositiveButtonListener positiveButtonListener,
                              @NonNull final ICancelCbaCallback cancelCbaCallback);

    /**
     * Build and show PIN dialog that prompts user to type in their PIN for their YubiKey.
     * @param positiveButtonListener A PositiveButtonListener to be set for a SmartcardPinDialog.
     * @param cancelCbaCallback      A Callback that holds code to be run when CBA is being cancelled.
     */
    void showPinDialog(@NonNull final SmartcardPinDialog.PositiveButtonListener positiveButtonListener,
                       @NonNull final ICancelCbaCallback cancelCbaCallback);

    /**
     * Builds and shows dialog informing the user of an expected or unexpected error.
     * @param titleStringResourceId String resource id of the title text.
     * @param messageStringResourceId String resource id of the message text.
     */
    void showErrorDialog(final int titleStringResourceId,
                         final int messageStringResourceId);

    /**
     * Builds and shows dialog that prompts user to choose if they would like to proceed with on-device
     * or smartcard certificate based authentication.
     * @param positiveButtonListener A Listener containing code to be run upon a positive button click.
     * @param cancelCbaCallback A Callback that holds code to be run when CBA is being cancelled.
     */
    void showUserChoiceDialog(@NonNull final UserChoiceDialog.PositiveButtonListener positiveButtonListener,
                              @NonNull final ICancelCbaCallback cancelCbaCallback);

    /**
     * Builds and shows a SmartcardDialog that prompts the user to connect their smartcard,
     * either by plugging in (USB) or holding to back of phone (NFC).
     * @param cancelCbaCallback A Callback that holds code to be run when CBA is being cancelled.
     */
    void showSmartcardPromptDialog(@NonNull final ICancelCbaCallback cancelCbaCallback);

    /**
     * Builds and shows a SmartcardDialog that reminds the user to remain holding their smartcard device to their phone.
     */
    void showSmartcardNfcLoadingDialog();

    /**
     * Builds and shows a SmartcardDialog that prompts the user to connect their smartcard by holding it to the back of their phone.
     * @param cancelCbaCallback A Callback that holds code to be run when CBA is being cancelled.
     */
    void showSmartcardNfcPromptDialog(@NonNull final ICancelCbaCallback cancelCbaCallback);

    /**
     * Builds and shows a SmartcardDialog that notifies user that NFC is not on for their device.
     * @param dismissCallback a callback that holds logic to be run upon dismissal of the dialog.
     */
    void showSmartcardNfcReminderDialog(@NonNull final SmartcardNfcReminderDialog.DismissCallback dismissCallback);

    /**
     * TODO
     */
    void showSmartcardRemovalPromptDialog();

    /**
     * Dismisses current dialog, if one is showing.
     */
    void dismissDialog();

    /**
     * Shows provided SmartcardDialog if not null.
     * Automatically dismisses existing dialog (if showing).
     * @param dialog SmartcardDialog object to be shown.
     */
    void showDialog(@Nullable final SmartcardDialog dialog);

    /**
     * Informs if an existing dialog is currently showing.
     * @return True if a SmartcardDialog is currently showing. False otherwise.
     */
    boolean isDialogShowing();

    @Nullable
    String getDialogClassShowing();

        /**
         * Runs callback code for the current dialog when
         * a smartcard is disconnected from the device and a dialog is currently showing.
         */
    void onSmartcardRemoval();

    /**
     * Sets error mode for an existing SmartcardPinDialog.
     */
    void setPinDialogErrorMode();
}
