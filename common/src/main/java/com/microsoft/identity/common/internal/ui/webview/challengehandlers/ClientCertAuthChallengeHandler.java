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

import static com.yubico.yubikit.piv.Slot.AUTHENTICATION;
import static com.yubico.yubikit.piv.Slot.CARD_AUTH;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Build;
import android.security.KeyChain;
import android.security.KeyChainAliasCallback;
import android.security.KeyChainException;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.ClientCertRequest;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;

import com.microsoft.identity.common.R;
import com.microsoft.identity.common.logging.Logger;
import com.yubico.yubikit.android.YubiKitManager;
import com.yubico.yubikit.android.transport.usb.UsbConfiguration;
import com.yubico.yubikit.android.transport.usb.UsbYubiKeyDevice;
import com.yubico.yubikit.android.transport.usb.connection.UsbSmartCardConnection;
import com.yubico.yubikit.core.smartcard.SmartCardConnection;
import com.yubico.yubikit.core.util.Callback;
import com.yubico.yubikit.core.util.Result;
import com.yubico.yubikit.piv.PivSession;
import com.yubico.yubikit.piv.Slot;

import java.io.IOException;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

//Handles Certificate Based Authentication by means of certificates provisioned onto YubiKeys or the devices themselves.
//Note that CBA requires API >= 21 (YubiKit SDK min API = 19; ClientCertRequest class available with API > 21)
public final class ClientCertAuthChallengeHandler implements IChallengeHandler<ClientCertRequest, Void> {
    private static final String TAG = ClientCertAuthChallengeHandler.class.getSimpleName();
    private static final String ACCEPTABLE_ISSUER = "CN=MS-Organization-Access";
    private Activity mActivity;
    private final YubiKitManager mYubiKitManager;
    private UsbYubiKeyDevice mDevice;

    public ClientCertAuthChallengeHandler(@NonNull final Activity activity) {
        mActivity = activity;
        //set mDevice to null
        mDevice = null;
        //Create and start YubiKitManager
        mYubiKitManager = new YubiKitManager(mActivity.getApplicationContext());
        mYubiKitManager.startUsbDiscovery(new UsbConfiguration(), new Callback<UsbYubiKeyDevice>() {
            @Override
            public void invoke(UsbYubiKeyDevice device) {
                Logger.info(TAG, "A YubiKey device was connected");
                //set device
                mDevice = device;
                mDevice.setOnClosed(new Runnable() {
                    @Override
                    public void run() {
                        Logger.info(TAG, "A YubiKey device was disconnected");
                        mDevice = null;
                    }
                });
            }
        });
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public Void processChallenge(@NonNull final ClientCertRequest request) {
        final String methodTag = TAG + ":processChallenge";
        if (mDevice != null) {
            //Try to make a connection
            mDevice.requestConnection(UsbSmartCardConnection.class, new Callback<Result<UsbSmartCardConnection, IOException>>() {
                @Override
                public void invoke(Result<UsbSmartCardConnection, IOException> value) {
                    try {
                        final SmartCardConnection c = value.getValue();
                        PivSession piv = new PivSession(c);
                        //create X509Certificate arraylist
                        final List<YubiKitCertDetails> certList = new ArrayList<>();
                        //get certificate in slot
                        final X509Certificate cert = piv.getCertificate(AUTHENTICATION);
                        certList.add(new YubiKitCertDetails(cert.getIssuerDN().getName(), cert.getSubjectDN().getName(), AUTHENTICATION));
                        final X509Certificate cert2 = piv.getCertificate(CARD_AUTH);
                        certList.add(new YubiKitCertDetails(cert2.getIssuerDN().getName(), cert2.getSubjectDN().getName(), CARD_AUTH));

                        mActivity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                buildAndShowSmartCardDialog(certList, request);
                            }
                        });

                    } catch(Exception e) {
                        Log.i(methodTag, e.getMessage());
                    }
                }
            });
            return null;
        }
        else {
            return handleOnDeviceCertAuth(request);
        }
    }

    // Handles the logic for on-device certificate based authentication.
    // Makes use of Android's KeyChain.choosePrivateKeyAlias method,
    // which shows a cert picker that allows users to choose their on-device user certificate to authenticate with.
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public Void handleOnDeviceCertAuth(@NonNull final ClientCertRequest request) {
        final String methodTag = TAG + ":handleOnDeviceCertAuth";
        final Principal[] acceptableCertIssuers = request.getPrincipals();

        // When ADFS server sends null or empty issuers, we'll continue with cert prompt.
        if (acceptableCertIssuers != null) {
            for (Principal issuer : acceptableCertIssuers) {
                if (issuer.getName().contains(ACCEPTABLE_ISSUER)) {
                    //Checking if received acceptable issuers contain "CN=MS-Organization-Access"
                    Logger.info(methodTag,"Cancelling the TLS request, not respond to TLS challenge triggered by device authentication.");
                    request.cancel();
                    return null;
                }
            }
        }

        KeyChain.choosePrivateKeyAlias(mActivity, new KeyChainAliasCallback() {
                    @Override
                    public void alias(String alias) {
                        if (alias == null) {
                            Logger.info(methodTag,"No certificate chosen by user, cancelling the TLS request.");
                            request.cancel();
                            return;
                        }

                        try {
                            final X509Certificate[] certChain = KeyChain.getCertificateChain(
                                    mActivity.getApplicationContext(), alias);
                            final PrivateKey privateKey = KeyChain.getPrivateKey(
                                    mActivity, alias);

                            Logger.info(methodTag,"Certificate is chosen by user, proceed with TLS request.");
                            request.proceed(privateKey, certChain);
                            return;
                        } catch (final KeyChainException e) {
                            Logger.errorPII(methodTag,"KeyChain exception", e);
                        } catch (final InterruptedException e) {
                            Logger.errorPII(methodTag,"InterruptedException exception", e);
                        }

                        request.cancel();
                    }
                },
                request.getKeyTypes(),
                request.getPrincipals(),
                request.getHost(),
                request.getPort(),
                null);

        return null;
    }

    //Allows AzureActiveDirectoryWebViewCLient to stop mYubiKitManager's discovery mode.
    public void stopYubiKitManagerUsbDiscovery() {
        //Stop UsbDiscovery for YubiKitManager
        //Should be called when host fragment is destroyed.
        mYubiKitManager.stopUsbDiscovery();
    }

    //Builds and shows the Smartcard Dialog given a non-empty CertificateAdapter and a ClientCertRequest
    private void buildAndShowSmartCardDialog(List<YubiKitCertDetails> certList, final ClientCertRequest request) {
        try {
            final CertificateAdapter certAdapter = new CertificateAdapter(mActivity, certList);
            //Make sure that certAdapter is not empty.
            if (certAdapter.getCount() == 0) {
                return;
            }
            //Start building the dialog.
            AlertDialog.Builder builder = new AlertDialog.Builder(mActivity, R.style.CertAlertDialogTheme)
                    .setTitle("Choose a smartcard certificate to sign in")
                    .setSingleChoiceItems(certAdapter, 0, null)
                    .setPositiveButton("Continue", new DialogInterface.OnClickListener() {
                        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
                        @Override
                        public void onClick(final DialogInterface dialog, int which) {
                            //Ensure that YubiKey has not been removed prematurely by checking if mDevice is null.
                            if (mDevice == null) {
                                request.cancel();
                                return;
                            }
                            //Get checked position.
                            int checkedPosition = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
                            //Here is where we should bring up the PIN prompt
                            final YubiKitCertDetails certDetails = certAdapter.getItem(checkedPosition);
                            //need to verify pin and call method on request to finish
                            //https://stackoverflow.com/questions/32225009/continue-displaying-alertdialog-if-entered-wrong-password use this later when we have to do multiple password attempts
                            showPinPromptAndVerify(certDetails.slot, request);
                            //call method to proceed
                            //request.cancel();
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Toast.makeText(mActivity, "Cancelled", Toast.LENGTH_LONG).show();
                            request.cancel();
                            dialog.cancel();
                        }
                    });
            // Create dialog
            final AlertDialog alertDialog = builder.create();

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
            //Show the dialog.
            alertDialog.show();
            //alertDialog.getWindow().setLayout(1000, 1000);
        } catch (Exception e) {
            //log here
        }

    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void showPinPromptAndVerify(final Slot slot, final ClientCertRequest request) {
        if (mDevice == null) {
            request.cancel();
            return;
        }
        mDevice.requestConnection(UsbSmartCardConnection.class, new Callback<Result<UsbSmartCardConnection, IOException>>() {
            @Override
            public void invoke(Result<UsbSmartCardConnection, IOException> value) {
                try {
                    final SmartCardConnection c = value.getValue();
                    final PivSession piv = new PivSession(c);
                    Log.i(TAG, "invoke: " + piv.getPinAttempts());
                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //build dialog here
                            final EditText editText = new EditText(mActivity);
                            AlertDialog.Builder builder = new AlertDialog.Builder(mActivity, R.style.CertAlertDialogTheme)
                                    .setTitle("Unlock smartcard")
                                    .setMessage("Enter the smartcard PIN to access the certificate and sign in")
                                    .setView(editText)
                                    .setPositiveButton("Unlock", null)
                                    .setNegativeButton("Cancel", null);
                            final AlertDialog dialog = builder.create();
                            dialog.show();
                            dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    if (mDevice == null) {
                                        request.cancel();
                                        dialog.dismiss();
                                        return;
                                    }
                                    mDevice.requestConnection(UsbSmartCardConnection.class, new Callback<Result<UsbSmartCardConnection, IOException>>() {
                                        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
                                        @Override
                                        public void invoke(Result<UsbSmartCardConnection, IOException> value) {
                                            try {
                                                final SmartCardConnection c = value.getValue();
                                                final PivSession piv = new PivSession(c);
                                                int pinAttempts = piv.getPinAttempts();
                                                try {
                                                    piv.verifyPin(editText.getText().toString().toCharArray());
                                                    X509Certificate cert = piv.getCertificate(slot);
                                                    Log.i(TAG, "buildandshow: " + cert.getSubjectDN().getName());
                                                    dialog.dismiss();
                                                    request.cancel();
                                                } catch (Exception e) {
                                                    Log.i(TAG, "onClick: Incorrect. remaining" + --pinAttempts);
                                                }

                                            } catch (Exception e) {
                                                //log?
                                            }
                                        }
                                    });
                                }
                            });
                        }
                    });

                    //need to verify pin here
                    //piv.verifyPin(pass.toCharArray());
                } catch (Exception e) {
                    Log.i(TAG, "buildandshow: " + e);
                }
            }
        });

    }

    //creating class to hold details of a Certificate needed for the picker
    //including subject, issuer, and slot
    private class YubiKitCertDetails {
        String issuerText;
        String subjectText;
        Slot slot;

        YubiKitCertDetails(String issuerText, String subjectText, Slot slot) {
            this.issuerText = issuerText;
            this.subjectText = subjectText;
            this.slot = slot;
        }
    }

    public class CertificateAdapter extends ArrayAdapter<YubiKitCertDetails> {

        public CertificateAdapter(@NonNull Context context, @NonNull List<YubiKitCertDetails> certs) {
            super(context, 0, certs);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            View item = convertView;
            if (item == null) {
                item = LayoutInflater.from(getContext()).inflate(R.layout.certificate_row_layout, parent, false);
            }

            YubiKitCertDetails currentCert = getItem(position);
            TextView subjectText = item.findViewById(R.id.subjectText);
            TextView issuerText = item.findViewById(R.id.issuerText);

            subjectText.setText(currentCert.subjectText);
            issuerText.setText(currentCert.issuerText);

            //set checking
            RadioButton radioButton = item.findViewById(R.id.radioButton);
            ListView listView = (ListView) parent;
            radioButton.setChecked(position == listView.getCheckedItemPosition());

            return item;
        }

    }
}