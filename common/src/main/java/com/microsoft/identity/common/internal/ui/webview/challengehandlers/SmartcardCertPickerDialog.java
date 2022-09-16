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
package com.microsoft.identity.common.internal.ui.webview.challengehandlers;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.microsoft.identity.common.R;
import com.microsoft.identity.common.logging.Logger;

import java.security.cert.X509Certificate;
import java.util.List;

/**
 * Builds and shows a dialog that allows the user to select a certificate they would like to use to authenticate.
 */
public class SmartcardCertPickerDialog extends SmartcardDialog {

    private static final String TAG = SmartcardCertPickerDialog.class.getSimpleName();
    private final List<ICertDetails> mCertList;
    private final PositiveButtonListener mPositiveButtonListener;
    private final CancelCbaCallback mCancelCbaCallback;

    /**
     * Creates new instance of SmartcardCertPickerDialog.
     * @param certList List of ClientCertAuthChallengeHandler.YubiKitCertDetails compiled from certificates on YubiKey.
     * @param positiveButtonListener Implemented Listener for a positive button click.
     * @param cancelCbaCallback Implemented Callback for when CBA is being cancelled.
     * @param activity Host activity.
     */
    public SmartcardCertPickerDialog(@NonNull final List<ICertDetails> certList,
                                     @NonNull final PositiveButtonListener positiveButtonListener,
                                     @NonNull final CancelCbaCallback cancelCbaCallback,
                                     @NonNull final Activity activity) {
        super(activity);
        mCertList = certList;
        mPositiveButtonListener = positiveButtonListener;
        mCancelCbaCallback = cancelCbaCallback;
        createDialog();
    }

    /**
     * Builds an AlertDialog that displays the details of the certificates in a single choice ListView and prompts the user to choose a certificate to proceed.
     */
    protected void createDialog() {
        final String methodTag = TAG + ":createDialog";
        //Create CertDetailsAdapter
        final CertDetailsAdapter certAdapter = new CertDetailsAdapter(mActivity, mCertList);
        //Must build dialog on UI thread
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //Start building the dialog.
                final AlertDialog.Builder builder = new AlertDialog.Builder(mActivity, R.style.CertAlertDialogTheme)
                        //Set topmost text of dialog.
                        .setTitle(R.string.smartcard_cert_dialog_title)
                        //Creates and sets a ListView which gets rows from the provided ICertDetails adapter. The first row is checked by default.
                        //We don't pass through a listener, as the radio button check logic is handled after dialog is created.
                        .setSingleChoiceItems(certAdapter, 0, null)
                        //Positive button will pass along the certDetails of the selected row.
                        .setPositiveButton(R.string.smartcard_cert_dialog_positive_button, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(final DialogInterface dialog, int which) {
                                //Get the certificate details of the checked row.
                                final int checkedPosition = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
                                final ICertDetails certDetails = certAdapter.getItem(checkedPosition);
                                //certAdapter.getItem could return null.
                                if (certDetails != null) {
                                    mPositiveButtonListener.onClick(certDetails);
                                } else {
                                    //Handle this by cancelling out of flow and logging.
                                    //Should add telemetry once telemetry process is updated.
                                    mCancelCbaCallback.onCancel();
                                    Logger.error(methodTag, "Could not retrieve info for selected certificate entry.", null);
                                }
                            }
                        })
                        //Negative button should end up cancelling flow.
                        .setNegativeButton(R.string.smartcard_cert_dialog_negative_button, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                //On request by user, cancel flow.
                                mCancelCbaCallback.onCancel();
                            }
                        });
                // Create dialog.
                final AlertDialog alertDialog = builder.create();
                // Set up single checked item logic for cert ListView within dialog.
                final ListView listView = alertDialog.getListView();
                listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        listView.setItemChecked(position,true);
                        certAdapter.notifyDataSetChanged();
                    }
                });

                //If user touches outside dialog, the default behavior makes the dialog disappear without really doing anything.
                //Adding this line in disables this default behavior so that the user can only exit by hitting the cancel button.
                alertDialog.setCanceledOnTouchOutside(false);
                //Handle back button the same as the negative button.
                alertDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        mCancelCbaCallback.onCancel();
                    }
                });
                mDialog = alertDialog;
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
     * Listener interface for a positive button click.
     */
    public interface PositiveButtonListener {
        void onClick(@NonNull final ICertDetails certDetails);
    }

    /**
     * Callback interface for when CBA is being cancelled.
     */
    public interface CancelCbaCallback {
        void onCancel();
    }

    /**
     * YubiKitCertDetails Adapter for ListView within smartcard certificate picker dialog.
     */
    public static class CertDetailsAdapter extends ArrayAdapter<ICertDetails> {

        public CertDetailsAdapter(@NonNull final Context context,
                                  @NonNull final List<ICertDetails> certs) {
            super(context, 0, certs);
        }

        @NonNull
        @Override
        public View getView(final int position,
                            @Nullable final View convertView,
                            @NonNull final ViewGroup parent) {
            View item = convertView;
            if (item == null) {
                item = LayoutInflater.from(getContext()).inflate(R.layout.certificate_row_layout, parent, false);
            }
            //Get references to the TextViews within the layout.
            final TextView subjectText = item.findViewById(R.id.subjectText);
            final TextView issuerText = item.findViewById(R.id.issuerText);
            // Fill in the TextViews with the subject and issuer values.
            final X509Certificate currentCert = getItem(position).getCertificate();
            subjectText.setText(currentCert.getSubjectDN().getName());
            issuerText.setText(currentCert.getIssuerDN().getName());
            //Set radio button to be checked/unchecked based on ListView.
            final ListView listView = (ListView) parent;
            final RadioButton radioButton = item.findViewById(R.id.radioButton);
            radioButton.setChecked(position == listView.getCheckedItemPosition());

            return item;
        }

    }
}
