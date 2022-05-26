package com.microsoft.identity.common.internal.ui.webview.challengehandlers;

import android.app.Activity;
import android.app.Dialog;
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

//Builds and shows a dialog that prompts the user to provide a PIN in order to verify ownership of the YubiKey.
public class SmartcardPinDialog {

    private Activity mActivity;
    private Dialog mDialog;
    private View mPinLayout;
    private PositiveButtonListener mPositiveButtonListener;
    private NegativeButtonListener mNegativeButtonListener;

    public SmartcardPinDialog(Activity activity) {
        mActivity = activity;
        createDialog();
    }

    private void createDialog() {
        //Must build dialog on UI thread
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //Inflate Pin EditText layout
                mPinLayout = mActivity.getLayoutInflater().inflate(R.layout.pin_textview_layout, null);
                //Start building dialog
                AlertDialog.Builder builder = new AlertDialog.Builder(mActivity, R.style.CertAlertDialogTheme)
                        .setTitle(R.string.smartcard_pin_dialog_title)
                        .setMessage(R.string.smartcard_pin_dialog_message)
                        .setView(mPinLayout)
                        //Setting positive button listener to null for now, but will override to handle custom UI behavior after dialog is shown (in the show method).
                        .setPositiveButton(R.string.smartcard_pin_dialog_positive_button, null)
                        //Negative button should cancel flow.
                        .setNegativeButton(R.string.smartcard_pin_dialog_negative_button, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (mNegativeButtonListener != null) {
                                    mNegativeButtonListener.onClick();
                                }
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
                        if (mNegativeButtonListener != null) {
                            mNegativeButtonListener.onClick();
                        }
                    }
                });
                mDialog = dialog;
            }
        });
    }

    //In order to add custom UI for errors, the positive button must be overwritten.
    //But the dialog needs to be shown before we can get a reference and override the positive button's behavior.
    public void show() {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mDialog.show();
                //Need to show appropriate error UI when user enters an incorrect PIN.
                //Since TextInputLayout requires extra dependencies, we're going to manually show the error message and color using a listener on the PIN's EditText.
                final EditText pinEditText = mPinLayout.findViewById(R.id.pinEditText);
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
                        if (mPositiveButtonListener != null) {
                            mPositiveButtonListener.onClick(pinEditText.getText().toString());
                        }
                    }
                });
            }
        });
    }

    public void dismiss() {
        mDialog.dismiss();
    }

    //Update Dialog to indicate that an incorrect attempt was made.
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void setErrorMode() {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //Clear edittext, turn edittext bar red, and show red error message.
                EditText pinEditText = mPinLayout.findViewById(R.id.pinEditText);
                pinEditText.getText().clear();
                pinEditText.setBackgroundTintList(ColorStateList.valueOf(mActivity.getResources().getColor(R.color.dialogErrorText)));

                TextView errorTextView = mPinLayout.findViewById(R.id.errorTextView);
                errorTextView.setText(R.string.smartcard_pin_dialog_error_message);
            }
        });
    }

    //Reset Dialog's UI to original non-error state.
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void resetErrorMode() {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //Reset back to blue and don't show error text.
                TextView errorTextView = mPinLayout.findViewById(R.id.errorTextView);
                errorTextView.setText("");
                EditText pinEditText = mPinLayout.findViewById(R.id.pinEditText);
                pinEditText.setBackgroundTintList(ColorStateList.valueOf(mActivity.getResources().getColor(R.color.dialogPinEditText)));
            }
        });
    }

    //Listener interfaces and setters for the dialog buttons.
    public void setPositiveButtonListener(PositiveButtonListener listener) {
        mPositiveButtonListener = listener;
    }

    public void setNegativeButtonListener(NegativeButtonListener listener) {
        mNegativeButtonListener = listener;
    }

    public interface PositiveButtonListener {
        void onClick(String pin);
    }

    public interface NegativeButtonListener {
        void onClick();
    }

}
