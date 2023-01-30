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

import android.app.Activity;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.os.Build;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;

import com.microsoft.identity.common.R;
import com.microsoft.identity.common.logging.Logger;

import lombok.NonNull;

/**
 * Builds and shows a dialog that prompts the user to provide a PIN in order to verify ownership of the YubiKey.
 */
public class SmartcardPinDialog extends SmartcardDialog {

    private static final String TAG = SmartcardPinDialog.class.getSimpleName();
    private View mPinLayout;
    private final PositiveButtonListener mPositiveButtonListener;
    private final CancelCbaCallback mCancelCbaCallback;

    /**
     * Creates new instance of SmartcardPinDialog.
     * @param positiveButtonListener Implemented Listener for a positive button click.
     * @param cancelCbaCallback Implemented Callback for when CBA is being cancelled.
     * @param activity Host activity.
     */
    public SmartcardPinDialog(@NonNull final PositiveButtonListener positiveButtonListener,
                              @NonNull final CancelCbaCallback cancelCbaCallback,
                              @NonNull final Activity activity) {
        super(activity);
        mPositiveButtonListener = positiveButtonListener;
        mCancelCbaCallback = cancelCbaCallback;
        createDialog();
    }

    /**
     * Builds an AlertDialog that prompts the user to enter their YubiKey PIN.
     * Note that the positive button listener is set after the dialog is shown in the overridden show method.
     */
    protected void createDialog() {
        //Must build dialog on UI thread
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //Inflate Pin EditText layout
                mPinLayout = mActivity.getLayoutInflater().inflate(R.layout.pin_textview_layout, null);
                //Start building dialog
                final AlertDialog.Builder builder = new AlertDialog.Builder(mActivity, R.style.CertAlertDialogTheme)
                        //Sets topmost text of dialog.
                        .setTitle(R.string.smartcard_pin_dialog_title)
                        //Sets subtext of the title.
                        .setMessage(R.string.smartcard_pin_dialog_message)
                        //Sets custom layout for body of dialog.
                        .setView(mPinLayout)
                        //Setting positive button listener to null for now, but will override to handle custom UI behavior after dialog is shown (in the show method).
                        .setPositiveButton(R.string.smartcard_pin_dialog_positive_button, null)
                        //Negative button should cancel flow.
                        .setNegativeButton(R.string.smartcard_pin_dialog_negative_button, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mCancelCbaCallback.onCancel();
                            }
                        });
                //Create dialog
                final AlertDialog dialog = builder.create();
                //If user touches outside dialog, the default behavior makes the dialog disappear without really doing anything.
                //Adding this line in disables this default behavior so that the user can only exit by hitting the cancel button.
                dialog.setCanceledOnTouchOutside(false);
                //Handle back button the same as the negative button.
                dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        mCancelCbaCallback.onCancel();
                    }
                });
                mDialog = dialog;
            }
        });
    }

    /**
     * Handles scenario when CBA is canceled unexpectedly (for example. when a YubiKey is unplugged while a dialog is showing).
     */
    @Override
    public void onCancelCba() {
        //Call CancelCbaCallback's onCancel
        mCancelCbaCallback.onCancel();
    }

    /**
     * In order to add custom UI for errors, the positive button must be overwritten.
     * Note that the dialog needs to be shown before we can get a reference and override the positive button's behavior.
     */
    @Override
    public void show() {
        final String methodTag = TAG + ":show";
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mDialog.show();
                //Need to show appropriate error UI when user enters an incorrect PIN.
                //Since TextInputLayout requires extra dependencies, we're going to manually show the error message and color using a listener on the PIN's EditText.
                final EditText pinEditText = mPinLayout.findViewById(R.id.pinEditText);
                if (pinEditText == null) {
                    //Log error and cancel out of flow.
                    mCancelCbaCallback.onCancel();
                    Logger.error(methodTag, "Error while retrieving dialog EditText component.", null);
                    return;
                }
                pinEditText.addTextChangedListener(new TextWatcher() {
                    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                        if (start == 0) {
                            resetErrorMode();
                        }
                    }
                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        //Do nothing
                    }
                    @Override
                    public void afterTextChanged(Editable s) {
                        //Do nothing
                    }
                });

                //Since we aren't using AlertDialog's setPositionButton method this time, it is important to remember to manually dismiss the dialog when it isn't needed anymore.
                ((AlertDialog)mDialog).getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //Avoiding the use of strings for pin.
                        final char[] pin = new char[pinEditText.length()];
                        pinEditText.getText().getChars(0, pinEditText.length(), pin, 0);
                        mPositiveButtonListener.onClick(pin);
                    }
                });
            }
        });
    }

    /**
     * Update Dialog to indicate that an incorrect attempt was made.
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void setErrorMode() {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //Clear edittext, turn edittext bar red, and show red error message.
                final EditText pinEditText = mPinLayout.findViewById(R.id.pinEditText);
                pinEditText.getText().clear();
                pinEditText.setBackgroundTintList(ColorStateList.valueOf(mActivity.getResources().getColor(R.color.dialogErrorText)));

                final TextView errorTextView = mPinLayout.findViewById(R.id.errorTextView);
                errorTextView.setText(R.string.smartcard_pin_dialog_error_message);
            }
        });
    }

    /**
     * Reset Dialog's UI to original non-error state.
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void resetErrorMode() {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //Reset back to blue and don't show error text.
                final TextView errorTextView = mPinLayout.findViewById(R.id.errorTextView);
                errorTextView.setText("");
                final EditText pinEditText = mPinLayout.findViewById(R.id.pinEditText);
                pinEditText.setBackgroundTintList(ColorStateList.valueOf(mActivity.getResources().getColor(R.color.dialogPinEditText)));
            }
        });
    }

    /**
     * Listener interface for a positive button click.
     */
    public interface PositiveButtonListener {
        void onClick(@NonNull final char[] pin);
    }

    /**
     * Callback interface for when CBA is being cancelled.
     */
    public interface CancelCbaCallback {
        void onCancel();
    }

}
