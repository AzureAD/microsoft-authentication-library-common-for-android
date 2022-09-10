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
package com.microsoft.identity.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import androidx.appcompat.app.AlertDialog;
import android.content.DialogInterface;
import android.webkit.ClientCertRequest;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.microsoft.identity.common.internal.ui.DualScreenActivity;
import com.microsoft.identity.common.internal.ui.webview.challengehandlers.ClientCertAuthChallengeHandler;
import com.microsoft.identity.common.internal.ui.webview.challengehandlers.ICertDetails;
import com.microsoft.identity.common.internal.ui.webview.challengehandlers.ISmartcardCertBasedAuthManager;
import com.microsoft.identity.common.internal.ui.webview.challengehandlers.ISmartcardSession;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.shadows.ShadowAlertDialog;
import org.robolectric.shadows.ShadowLooper;

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Set;

@RunWith(RobolectricTestRunner.class)
public class SmartcardCertBasedAuthTest {

    @Test
    public void testNoCertsOnSmartcard() {
        final Activity activity = setUpActivity();
        setUpClientCertAuthChallengeHandlerAndProcess(activity, new ArrayList<>());
        checkIfCorrectDialogIsShowing(
                getStringFromResource(activity, R.string.smartcard_error_dialog_positive_button)
        );
    }

    @Test
    public void testGetToCertPickerDialog() {
        final Activity activity = setUpActivity();
        final List<X509Certificate> certList = getMockCertList();
        setUpClientCertAuthChallengeHandlerAndProcess(activity, certList);
        goToPickerDialog(activity);
    }

    @Test
    public void testGetToPinDialog() {
        final Activity activity = setUpActivity();
        final List<X509Certificate> certList = getMockCertList();
        setUpClientCertAuthChallengeHandlerAndProcess(activity, certList);
        goToPinDialog(activity);
    }

    @Test
    public void testCancelAtPickerDialog() {
        final Activity activity = setUpActivity();
        final List<X509Certificate> certList = getMockCertList();
        setUpClientCertAuthChallengeHandlerAndProcess(activity, certList);
        AlertDialog pickerDialog = goToPickerDialog(activity);
        performClick(pickerDialog, DialogInterface.BUTTON_NEGATIVE);
        ensureNoDialogIsShowing();
    }

    @Test
    public void testCancelAtPinDialog() {
        final Activity activity = setUpActivity();
        final List<X509Certificate> certList = getMockCertList();
        setUpClientCertAuthChallengeHandlerAndProcess(activity, certList);
        AlertDialog pinDialog = goToPinDialog(activity);
        performClick(pinDialog, DialogInterface.BUTTON_NEGATIVE);
        ensureNoDialogIsShowing();
    }

    @Test
    public void testUnplugAtPickerDialog() {
        Activity activity = setUpActivity();
        List<X509Certificate> certList = getMockCertList();
        ClientCertAuthChallengeHandler clientCertAuthChallengeHandler = setUpClientCertAuthChallengeHandlerAndProcess(activity, certList);
        goToPickerDialog(activity);
        mockUnplugSmartcard(clientCertAuthChallengeHandler);
        checkIfCorrectDialogIsShowing(
                getStringFromResource(activity, R.string.smartcard_error_dialog_positive_button)
        );
    }

    @Test
    public void testUnplugAtPinDialog() {
        Activity activity = setUpActivity();
        List<X509Certificate> certList = getMockCertList();
        ClientCertAuthChallengeHandler clientCertAuthChallengeHandler = setUpClientCertAuthChallengeHandlerAndProcess(activity, certList);
        goToPinDialog(activity);
        mockUnplugSmartcard(clientCertAuthChallengeHandler);
        checkIfCorrectDialogIsShowing(
                getStringFromResource(activity, R.string.smartcard_error_dialog_positive_button)
        );
    }

    //Test PIN attempts
    //Below Test should probably be split into two!
    @Test
    public void testLockedOut() {
        Activity activity = setUpActivity();
        List<X509Certificate> certList = getMockCertList();
        ClientCertAuthChallengeHandler c = setUpClientCertAuthChallengeHandlerAndProcess(activity, certList);
        AlertDialog pinDialog = goToPinDialog(activity);
        EditText editText = pinDialog.findViewById(R.id.pinEditText);
        assertNotNull(editText);
        char[] wrongPin = {'1', '2', '3'};
        editText.setText(wrongPin, 0, wrongPin.length);
        performClick(pinDialog, DialogInterface.BUTTON_POSITIVE);
        checkIfCorrectDialogIsShowing(
                getStringFromResource(activity, R.string.smartcard_pin_dialog_positive_button)
        );
        TextView errorMessage = pinDialog.findViewById(R.id.errorTextView);
        assertNotNull(errorMessage);
        String expectedErrorMessage = getStringFromResource(activity, R.string.smartcard_pin_dialog_error_message);
        assertEquals(expectedErrorMessage, errorMessage.getText());

        editText.setText(wrongPin, 0, wrongPin.length);
        assertNotEquals(expectedErrorMessage, errorMessage.getText());

        performClick(pinDialog, DialogInterface.BUTTON_POSITIVE);
        checkIfCorrectDialogIsShowing(
                getStringFromResource(activity, R.string.smartcard_pin_dialog_positive_button)
        );
        assertEquals(expectedErrorMessage, errorMessage.getText());

        //This should show an error dialog next
        editText.setText(wrongPin, 0, wrongPin.length);
        performClick(pinDialog, DialogInterface.BUTTON_POSITIVE);

        AlertDialog errorDialog = checkIfCorrectDialogIsShowing(
                getStringFromResource(activity, R.string.smartcard_error_dialog_positive_button)
        );

        //Finally, we'll try to start again and it should block us.
        performClick(errorDialog, DialogInterface.BUTTON_POSITIVE);
        ensureNoDialogIsShowing();
        c.processChallenge(getMockClientCertRequest());
        //Should see error dialog again
        checkIfCorrectDialogIsShowing(
                getStringFromResource(activity, R.string.smartcard_error_dialog_positive_button)
        );
    }

    @NonNull
    private String getStringFromResource(@NonNull final Activity activity, final int id) {
        return activity.getResources().getString(id);
    }

    @NonNull
    private Activity setUpActivity() {
        ActivityController<DualScreenActivity> controller = Robolectric.buildActivity(DualScreenActivity.class);
        controller.setup(); // Moves Activity to RESUMED state
        return controller.get();
    }

    @NonNull
    private List<X509Certificate> getMockCertList() {
        X509Certificate cert1 = getMockCertificate("SomeIssuer1", "SomeSubject1");
        X509Certificate cert2 = getMockCertificate("SomeIssuer2", "SomeSubject2");
        List<X509Certificate> certList = new ArrayList<>();
        certList.add(cert1);
        certList.add(cert2);
        return certList;
    }

    @NonNull
    private ClientCertAuthChallengeHandler setUpClientCertAuthChallengeHandlerAndProcess(@NonNull final Activity activity, @NonNull final List<X509Certificate> certList) {
        TestSmartcardCertBasedAuthManager testSmartcardCertBasedAuthManager = new TestSmartcardCertBasedAuthManager(certList);
        ClientCertAuthChallengeHandler clientCertAuthChallengeHandler = new ClientCertAuthChallengeHandler(activity, testSmartcardCertBasedAuthManager);
        ClientCertRequest clientCertRequest = getMockClientCertRequest();
        clientCertAuthChallengeHandler.processChallenge(clientCertRequest);
        return clientCertAuthChallengeHandler;
    }

    @NonNull
    private AlertDialog goToPickerDialog(@NonNull final Activity activity) {
        return checkIfCorrectDialogIsShowing(
                getStringFromResource(activity, R.string.smartcard_cert_dialog_positive_button)
        );
    }

    @NonNull
    private AlertDialog goToPinDialog(@NonNull final Activity activity) {
        AlertDialog pickerDialog = goToPickerDialog(activity);
        performClick(pickerDialog, DialogInterface.BUTTON_POSITIVE);
        return checkIfCorrectDialogIsShowing(
                getStringFromResource(activity, R.string.smartcard_pin_dialog_positive_button)
        );
    }

    private void performClick(@NonNull final AlertDialog dialog, final int whichButton) {
        dialog.getButton(whichButton).performClick();
        ShadowLooper.runUiThreadTasks();
    }

    @NonNull
    private AlertDialog checkIfCorrectDialogIsShowing(@NonNull final String expectedPositiveButtonText) {
        final AlertDialog dialog = (AlertDialog) ShadowAlertDialog.getLatestDialog();
        assertNotNull(dialog);
        assertTrue(dialog.isShowing());

        final Button positiveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
        assertNotNull(positiveButton);

        final String positiveButtonText = positiveButton.getText().toString();
        assertEquals(expectedPositiveButtonText, positiveButtonText);
        return dialog;
    }

    private void ensureNoDialogIsShowing() {
        final AlertDialog dialog = (AlertDialog) ShadowAlertDialog.getLatestDialog();
        assertFalse(dialog.isShowing());
    }

    private void mockUnplugSmartcard(ClientCertAuthChallengeHandler clientCertAuthChallengeHandler) {
        //Try unplugging
        clientCertAuthChallengeHandler.stopSmartcardUsbDiscovery();
        ShadowLooper.runUiThreadTasks();
    }

    @NonNull
    private ClientCertRequest getMockClientCertRequest() {
        return new ClientCertRequest() {
            @Nullable
            @Override
            public String[] getKeyTypes() {
                return new String[0];
            }

            @Nullable
            @Override
            public Principal[] getPrincipals() {
                return new Principal[0];
            }

            @Override
            public String getHost() {
                return null;
            }

            @Override
            public int getPort() {
                return 0;
            }

            @Override
            public void proceed(PrivateKey privateKey, X509Certificate[] x509Certificates) {

            }

            @Override
            public void ignore() {

            }

            @Override
            public void cancel() {

            }
        };
    }

    @NonNull
    private X509Certificate getMockCertificate(@Nullable final String issuerDNName, @Nullable final String subjectDNName) {
        return new X509Certificate() {
            @Override
            public void checkValidity() throws CertificateExpiredException, CertificateNotYetValidException {

            }

            @Override
            public void checkValidity(Date date) throws CertificateExpiredException, CertificateNotYetValidException {

            }

            @Override
            public int getVersion() {
                return 0;
            }

            @Override
            public BigInteger getSerialNumber() {
                return null;
            }

            @Override
            public Principal getIssuerDN() {
                return new Principal() {
                    @Override
                    public String getName() {
                        return issuerDNName;
                    }
                };
            }

            @Override
            public Principal getSubjectDN() {
                return new Principal() {
                    @Override
                    public String getName() {
                        return subjectDNName;
                    }
                };
            }

            @Override
            public Date getNotBefore() {
                return null;
            }

            @Override
            public Date getNotAfter() {
                return null;
            }

            @Override
            public byte[] getTBSCertificate() throws CertificateEncodingException {
                return new byte[0];
            }

            @Override
            public byte[] getSignature() {
                return new byte[0];
            }

            @Override
            public String getSigAlgName() {
                return null;
            }

            @Override
            public String getSigAlgOID() {
                return null;
            }

            @Override
            public byte[] getSigAlgParams() {
                return new byte[0];
            }

            @Override
            public boolean[] getIssuerUniqueID() {
                return new boolean[0];
            }

            @Override
            public boolean[] getSubjectUniqueID() {
                return new boolean[0];
            }

            @Override
            public boolean[] getKeyUsage() {
                return new boolean[0];
            }

            @Override
            public int getBasicConstraints() {
                return 0;
            }

            @Override
            public byte[] getEncoded() throws CertificateEncodingException {
                return new byte[0];
            }

            @Override
            public void verify(PublicKey publicKey) throws CertificateException, InvalidKeyException, NoSuchAlgorithmException, NoSuchProviderException, SignatureException {

            }

            @Override
            public void verify(PublicKey publicKey, String s) throws CertificateException, InvalidKeyException, NoSuchAlgorithmException, NoSuchProviderException, SignatureException {

            }

            @Override
            public String toString() {
                return null;
            }

            @Override
            public PublicKey getPublicKey() {
                return null;
            }

            @Override
            public boolean hasUnsupportedCriticalExtension() {
                return false;
            }

            @Override
            public Set<String> getCriticalExtensionOIDs() {
                return null;
            }

            @Override
            public Set<String> getNonCriticalExtensionOIDs() {
                return null;
            }

            @Override
            public byte[] getExtensionValue(String s) {
                return new byte[0];
            }
        };
    }

    private static class TestSmartcardCertBasedAuthManager implements ISmartcardCertBasedAuthManager {

        private IStartDiscoveryCallback mStartDiscoveryCallback;
        private boolean mIsConnected;
        private final List<ICertDetails> mCertDetailsList;
        private int mPinAttemptsRemaining;

        public TestSmartcardCertBasedAuthManager(@NonNull final List<X509Certificate> certList) {
            mIsConnected = false;
            mPinAttemptsRemaining = 3;
            //Convert cert list into certDetails list
            mCertDetailsList = new ArrayList<>();
            for (X509Certificate cert : certList) {
                mCertDetailsList.add(new ICertDetails() {
                    @NonNull
                    @Override
                    public X509Certificate getCertificate() {
                        return cert;
                    }
                });
            }
        }

        @Override
        public void startDiscovery(IStartDiscoveryCallback startDiscoveryCallback) {
            mStartDiscoveryCallback = startDiscoveryCallback;
            mockConnect();
        }

        @Override
        public void stopDiscovery() {
            mockDisconnect();
        }

        @Override
        public void attemptDeviceSession(@NonNull ISessionCallback callback) {
            try {
                callback.onGetSession(new TestSmartcardSession(mCertDetailsList, mPinAttemptsRemaining, new TestSmartcardSession.ITestSessionCallback() {
                    @Override
                    public void onIncorrectAttempt() {
                        mPinAttemptsRemaining--;
                    }
                }));
            } catch (Exception e) {
                callback.onException(e);
            }
        }

        @Override
        public boolean isDeviceConnected() {
            return mIsConnected;
        }

        @Override
        public void prepareForAuth() {
            //Don't need anything
        }

        public void mockConnect() {
            if (mStartDiscoveryCallback != null) {
                mStartDiscoveryCallback.onStartDiscovery();
                mIsConnected = true;
            }
        }

        public void mockDisconnect() {
            if (mStartDiscoveryCallback != null) {
                mStartDiscoveryCallback.onClosedConnection();
                mIsConnected = false;
            }
        }
    }

    private static class TestSmartcardSession implements ISmartcardSession {

        private final List<ICertDetails> mCertDetailsList;
        private final char[] mPin;
        private int mPinAttemptsRemaining;

        private ITestSessionCallback mCallback;

        public TestSmartcardSession(@NonNull final List<ICertDetails> certDetailsList, final int pinAttemptsRemaining, @NonNull final ITestSessionCallback callback) {
            mCertDetailsList = certDetailsList;
            mPin = new char[]{'1', '2', '3', '4', '5', '6'};
            mPinAttemptsRemaining = pinAttemptsRemaining;
            mCallback = callback;
        }

        @NonNull
        @Override
        public List<ICertDetails> getCertDetailsList() throws Exception {
            return mCertDetailsList;
        }

        @Override
        public boolean verifyPin(char[] pin) throws Exception {
            if (Arrays.equals(mPin, pin)) {
                return true;
            } else {
                mPinAttemptsRemaining = mPinAttemptsRemaining > 0 ? mPinAttemptsRemaining - 1 : 0;
                mCallback.onIncorrectAttempt();
                return false;
            }
        }

        @Override
        public int getPinAttemptsRemaining() throws Exception {
            return mPinAttemptsRemaining;
        }

        @NonNull
        @Override
        public PrivateKey getKeyForAuth(ICertDetails certDetails, char[] pin) throws Exception {
            return new PrivateKey() {
                @Override
                public String getAlgorithm() {
                    return null;
                }

                @Override
                public String getFormat() {
                    return null;
                }

                @Override
                public byte[] getEncoded() {
                    return new byte[0];
                }
            };
        }

        interface ITestSessionCallback {
            void onIncorrectAttempt();
        }
    }
}
