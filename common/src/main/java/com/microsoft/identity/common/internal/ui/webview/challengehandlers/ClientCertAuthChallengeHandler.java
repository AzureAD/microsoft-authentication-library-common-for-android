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
import static com.yubico.yubikit.piv.Slot.KEY_MANAGEMENT;
import static com.yubico.yubikit.piv.Slot.SIGNATURE;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.os.Build;
import android.security.KeyChain;
import android.security.KeyChainAliasCallback;
import android.security.KeyChainException;
import android.text.Editable;
import android.text.TextWatcher;
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
import com.yubico.yubikit.core.application.ApplicationNotAvailableException;
import com.yubico.yubikit.core.application.BadResponseException;
import com.yubico.yubikit.core.smartcard.ApduException;
import com.yubico.yubikit.core.smartcard.SmartCardConnection;
import com.yubico.yubikit.core.util.Callback;
import com.yubico.yubikit.core.util.Result;
import com.yubico.yubikit.piv.InvalidPinException;
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

    //creating nested class to hold details of a Certificate needed for the picker,
    // including subject, issuer, and slot.
    private class YubiKitCertDetails {
        private final String issuerText;
        private final String subjectText;
        private final Slot slot;

        public YubiKitCertDetails(String issuerText, String subjectText, Slot slot) {
            this.issuerText = issuerText;
            this.subjectText = subjectText;
            this.slot = slot;
        }

        public String getIssuerText() {
            return issuerText;
        }

        public String getSubjectText() {
            return subjectText;
        }

        public Slot getSlot() {
            return slot;
        }
    }

    public ClientCertAuthChallengeHandler(@NonNull final Activity activity) {
        mActivity = activity;
        //set mDevice to null
        mDevice = null;
        //Create and start YubiKitManager for UsbDiscovery mode.
        //When in Usb Discovery mode, Yubikeys that plug into the device will be accessible
        // once the user provides permission via the Android permission dialog.
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
        // Check if a YubiKey device has been discovered and connected.
        if (mDevice != null) {
            //A connection to the YubiKey needs to be made in order to read the certificates off it.
            mDevice.requestConnection(UsbSmartCardConnection.class, new Callback<Result<UsbSmartCardConnection, IOException>>() {
                @Override
                public void invoke(Result<UsbSmartCardConnection, IOException> value) {
                    try {
                        final SmartCardConnection c = value.getValue();
                        PivSession piv = new PivSession(c);
                        //Create ArrayList that contains cert details only pertinent to the cert picker.
                        final List<YubiKitCertDetails> certList = new ArrayList<>();
                        //We need to check every slot. If a slot is empty, an ApduException is called, in which case, we'll ignore it.
                        //AUTHENTICATION (9A)
                        getAndPutCertDetailsInList(AUTHENTICATION, piv, certList);
                        //SIGNATURE (9C)
                        getAndPutCertDetailsInList(SIGNATURE, piv, certList);
                        //KEY_MANAGEMENT (9D)
                        getAndPutCertDetailsInList(KEY_MANAGEMENT, piv, certList);
                        //CARD_AUTH (9E)
                        getAndPutCertDetailsInList(CARD_AUTH, piv, certList);

                        //Show Smartcard cert picker, which also handles the rest of the smartcard CBA flow.
                        mActivity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                buildAndShowSmartCardDialog(certList, request);
                            }
                        });

                    } catch(IOException e) {
                        Logger.errorPII(methodTag, "IOException", e);
                    } catch (ApduException e) {
                        Logger.errorPII(methodTag, "ApduException", e);
                    } catch (ApplicationNotAvailableException e) {
                        Logger.errorPII(methodTag, "ApplicationNotAvailableException", e);
                    }
                }
            });
            return null;
        }
        // Else, proceed with user certificates stored on device.
        else {
            return handleOnDeviceCertAuth(request);
        }
    }

    //Helper method created to handle reading certificates off YubiKey.
    private void getAndPutCertDetailsInList(Slot slot, PivSession piv, List<YubiKitCertDetails> certList) {
        final String methodTag = TAG + ":getCert";
        try {
            X509Certificate cert =  piv.getCertificate(slot);
            //If there are no exceptions, add details of this cert to our certList.
            certList.add(new YubiKitCertDetails(cert.getIssuerDN().getName(), cert.getSubjectDN().getName(), slot));
        } catch (IOException e) {
            Logger.errorPII(methodTag,"IOException", e);
        } catch (BadResponseException e) {
            Logger.errorPII(methodTag,"BadResponseException", e);
        } catch (ApduException e) {
            //If sw is 0x6a82 (27266), This is a FILE_NOT_FOUND error, which we should ignore since this means the slot is merely empty.
            if (e.getSw() == 0x6a82) {
                //Do nothing
            } else {
                Logger.errorPII(methodTag,"ApduException", e);
            }
        }
    }

    //Builds and shows the Smartcard Dialog given a non-empty CertDetailsAdapter and a ClientCertRequest
    private void buildAndShowSmartCardDialog(List<YubiKitCertDetails> certList, final ClientCertRequest request) {
        final CertDetailsAdapter certAdapter = new CertDetailsAdapter(mActivity, certList);
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
                            //If YubiKey has been taken out, cancel flow.
                            request.cancel();
                            return;
                        }
                        //Get the certificate details of the checked row.
                        int checkedPosition = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
                        final YubiKitCertDetails certDetails = certAdapter.getItem(checkedPosition);
                        //need to verify pin. Method also handles the rest of the CBA flow.
                        showPinPromptAndVerify(certDetails.getSlot(), request);
                        //call method to proceed
                        //request.cancel();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //On request by user, cancel flow.
                        request.cancel();
                    }
                });
        // Create dialog.
        final AlertDialog alertDialog = builder.create();
        // Set up logic for cert listview within dialog.
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

    }

    // Adapter for listview within certificate picker dialog.
    private class CertDetailsAdapter extends ArrayAdapter<YubiKitCertDetails> {

        public CertDetailsAdapter(@NonNull Context context, @NonNull List<YubiKitCertDetails> certs) {
            super(context, 0, certs);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            View item = convertView;
            if (item == null) {
                item = LayoutInflater.from(getContext()).inflate(R.layout.certificate_row_layout, parent, false);
            }
            //Get references to the textviews within the layout.
            TextView subjectText = item.findViewById(R.id.subjectText);
            TextView issuerText = item.findViewById(R.id.issuerText);
            // Fill in the textviews with the subject and issuer values.
            YubiKitCertDetails currentCert = getItem(position);
            subjectText.setText(currentCert.getSubjectText());
            issuerText.setText(currentCert.getIssuerText());
            //Set radio button to be checked/unchecked based on listview.
            ListView listView = (ListView) parent;
            RadioButton radioButton = item.findViewById(R.id.radioButton);
            radioButton.setChecked(position == listView.getCheckedItemPosition());

            return item;
        }

    }

    //Builds and shows pin dialog; verifies pin; and proceeds with authentication if verification is successful.
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void showPinPromptAndVerify(final Slot slot, final ClientCertRequest request) {
        final String methodTag = TAG + ":showPinPromptAndVerify";
        //Make sure that YubiKey is still plugged in before proceeding.
        if (mDevice == null) {
            request.cancel();
            return;
        }
        //Build pin dialog here.
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //Inflate Pin EditText layout
                final View pinLayout = mActivity.getLayoutInflater().inflate(R.layout.pin_textview_layout, null);
                //Start building dialog
                AlertDialog.Builder builder = new AlertDialog.Builder(mActivity, R.style.CertAlertDialogTheme)
                        .setTitle("Unlock smartcard")
                        .setMessage("Enter the smartcard PIN to access the certificate and sign in")
                        .setView(pinLayout)
                        .setPositiveButton("Unlock", null)
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                request.cancel();
                            }
                        });
                final AlertDialog dialog = builder.create();
                dialog.show();

                //Since I don't want to deal with adding the dependencies associated with TextInputLayout,
                // going to manually show the error message and color using a listener on the pin EditText.
                final EditText editText = pinLayout.findViewById(R.id.pinEditText);
                final TextView errorTextView = pinLayout.findViewById(R.id.errorTextView);
                editText.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                        if (start == 0) {
                            //Reset back to blue and no error text.
                            errorTextView.setText("");
                            editText.setBackgroundTintList(ColorStateList.valueOf(mActivity.getResources().getColor(R.color.dialogPinEditText)));
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

                //Need to show dialog before we can get a reference and override the positive button's behavior.
                dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //Make sure YubiKey is still plugged in.
                        if (mDevice == null) {
                            request.cancel();
                            dialog.dismiss();
                            return;
                        }
                        // Need to request a SmartCardConnection in order to access certs on YubiKey.
                        mDevice.requestConnection(UsbSmartCardConnection.class, new Callback<Result<UsbSmartCardConnection, IOException>>() {
                            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
                            @Override
                            public void invoke(Result<UsbSmartCardConnection, IOException> value) {
                                try {
                                    final SmartCardConnection c = value.getValue();
                                    final PivSession piv = new PivSession(c);
                                    // We need to retrieve the number of pin attempts before proceeding.
                                    // If the number of attempts is 0, no more attempts will be allowed,
                                    // and the user will need to figure out a way to reset their key outside of our library.
                                    int pinAttempts = piv.getPinAttempts();
                                    try {
                                        piv.verifyPin(editText.getText().toString().toCharArray());
                                        //If pin is successfully verified, we will get the certificate and perform the rest of the logic for authentication.
                                        X509Certificate cert = piv.getCertificate(slot);
                                        //NOTE: below is for testing. This would get replaced by the logic for actual authentication.
                                        Log.i(methodTag,  cert.getSubjectDN().getName());
                                        dialog.dismiss();
                                        request.cancel();
                                    } catch (BadResponseException e) {
                                        Logger.errorPII(methodTag,"BadResponseException", e);
                                    } catch (InvalidPinException e) {
                                        // An incorrect Pin attempt. Update Dialog to indicate that an incorrect attempt was made.
                                        // TODO: But if the number of pin attempts remaining is now 0, we should instead display the dialog informing the user that they have made too many incorrect attempts.
                                        Log.i(methodTag, "Incorrect. remaining" + --pinAttempts);
                                        editText.getText().clear();
                                        editText.setBackgroundTintList(ColorStateList.valueOf(mActivity.getResources().getColor(R.color.dialogPinErrorText)));
                                        errorTextView.setText("The PIN you entered was incorrect");

                                    }

                                } catch(IOException e) {
                                    Logger.errorPII(methodTag, "IOException", e);
                                } catch (ApduException e) {
                                    Logger.errorPII(methodTag, "ApduException", e);
                                } catch (ApplicationNotAvailableException e) {
                                    Logger.errorPII(methodTag, "ApplicationNotAvailableException", e);
                                }
                            }
                        });
                    }
                });
            }
        });

    }


    // Handles the logic for on-device certificate based authentication.
    // Makes use of Android's KeyChain.choosePrivateKeyAlias method,
    // which shows a cert picker that allows users to choose their on-device user certificate to authenticate with.
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private Void handleOnDeviceCertAuth(@NonNull final ClientCertRequest request) {
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
}