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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.shadows.ShadowAlertDialog;
import org.robolectric.shadows.ShadowLooper;

import java.math.BigInteger;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Set;

@RunWith(RobolectricTestRunner.class)
//Tests basic cancellation and error dialog flows of smartcard CBA feature.
//Manual tests cover complete authentication flow.
public class SmartcardCertBasedAuthTest {
    //Build activity
    private static final ActivityController<DualScreenActivity> mController = Robolectric.buildActivity(DualScreenActivity.class);
    //Host activity
    private Activity mActivity;
    @Before
    public void setUp() {
        mController.restart();
        mActivity = mController.get();
    }

    //Should show error dialog.
    @Test
    public void testNoCertsOnSmartcard() {
        setUpClientCertAuthChallengeHandlerAndProcess(mActivity, new ArrayList<>());
        checkIfCorrectDialogIsShowing(
                getStringFromResource(mActivity, R.string.smartcard_error_dialog_positive_button)
        );
    }

    //Basic test to get to picker dialog.
    @Test
    public void testGetToCertPickerDialog() {
        final List<X509Certificate> certList = getMockCertList();
        setUpClientCertAuthChallengeHandlerAndProcess(mActivity, certList);
        goToPickerDialog(mActivity);
    }

    //Basic test to get to PIN dialog.
    @Test
    public void testGetToPinDialog() {
        final List<X509Certificate> certList = getMockCertList();
        setUpClientCertAuthChallengeHandlerAndProcess(mActivity, certList);
        goToPinDialog(mActivity);
    }

    //Clicking cancel button should result in no dialog showing.
    @Test
    public void testCancelAtPickerDialog() {
        final List<X509Certificate> certList = getMockCertList();
        setUpClientCertAuthChallengeHandlerAndProcess(mActivity, certList);
        final AlertDialog pickerDialog = goToPickerDialog(mActivity);
        performClick(pickerDialog, DialogInterface.BUTTON_NEGATIVE);
        ensureNoDialogIsShowing();
    }

    //Clicking cancel button should result in no dialog showing.
    @Test
    public void testCancelAtPinDialog() {
        final List<X509Certificate> certList = getMockCertList();
        setUpClientCertAuthChallengeHandlerAndProcess(mActivity, certList);
        final AlertDialog pinDialog = goToPinDialog(mActivity);
        performClick(pinDialog, DialogInterface.BUTTON_NEGATIVE);
        ensureNoDialogIsShowing();
    }

    //Should result in error dialog showing.
    @Test
    public void testUnplugAtPickerDialog() {
        final List<X509Certificate> certList = getMockCertList();
        final ClientCertAuthChallengeHandler clientCertAuthChallengeHandler = setUpClientCertAuthChallengeHandlerAndProcess(mActivity, certList);
        goToPickerDialog(mActivity);
        //Unplugging while dialog is showing should result in an error dialog.
        mockUnplugSmartcard(clientCertAuthChallengeHandler);
        checkIfCorrectDialogIsShowing(
                getStringFromResource(mActivity, R.string.smartcard_error_dialog_positive_button)
        );
    }

    //Should result in error dialog showing.
    @Test
    public void testUnplugAtPinDialog() {
        final List<X509Certificate> certList = getMockCertList();
        final ClientCertAuthChallengeHandler clientCertAuthChallengeHandler = setUpClientCertAuthChallengeHandlerAndProcess(mActivity, certList);
        goToPinDialog(mActivity);
        //Unplugging while dialog is showing should result in an error dialog.
        mockUnplugSmartcard(clientCertAuthChallengeHandler);
        checkIfCorrectDialogIsShowing(
                getStringFromResource(mActivity, R.string.smartcard_error_dialog_positive_button)
        );
    }

    //Incorrect PIN attempts before reaching limit should remain on same dialog with error message.
    //Upon no PIN attempts remaining, an error dialog should show instead.
    //Note: pin attempts remaining is initially set to 2.
    @Test
    public void testLockedOut() {
        final List<X509Certificate> certList = getMockCertList();
        final ClientCertAuthChallengeHandler challengeHandler = setUpClientCertAuthChallengeHandlerAndProcess(mActivity, certList);
        final AlertDialog pinDialog = goToPinDialog(mActivity);
        //Test typing in incorrect PIN.
        final char[] wrongPin = {'1', '2', '3'};
        enterPinAndClick(pinDialog, wrongPin);
        //Dialog should be the same.
        checkIfCorrectDialogIsShowing(
                getStringFromResource(mActivity, R.string.smartcard_pin_dialog_positive_button)
        );
        //Make sure error message is seen.
        final TextView errorMessage = pinDialog.findViewById(R.id.errorTextView);
        assertNotNull(errorMessage);
        final String expectedErrorMessage = getStringFromResource(mActivity, R.string.smartcard_pin_dialog_error_message);
        assertEquals(expectedErrorMessage, errorMessage.getText());
        //Upon click again, an error dialog should appear.
        enterPinAndClick(pinDialog, wrongPin);
        final AlertDialog errorDialog = checkIfCorrectDialogIsShowing(
                getStringFromResource(mActivity, R.string.smartcard_error_dialog_positive_button)
        );
        //Finally, we'll try to start the flow over again,
        // and it should block us with an error dialog.
        performClick(errorDialog, DialogInterface.BUTTON_POSITIVE);
        ensureNoDialogIsShowing();
        challengeHandler.processChallenge(getMockClientCertRequest());
        checkIfCorrectDialogIsShowing(
                getStringFromResource(mActivity, R.string.smartcard_error_dialog_positive_button)
        );
    }

    //When an unexpected exception is thrown, a generic dialog should appear.
    @Test
    public void testExceptionThrownWhenGettingKey() {
        final List<X509Certificate> certList = getMockCertList();
        setUpClientCertAuthChallengeHandlerAndProcess(mActivity, certList);
        final AlertDialog pinDialog = goToPinDialog(mActivity);
        //Type in correct PIN, which (in this testing case only) should throw an exception
        // within the getKeyForAuth method. An error dialog should appear.
        final char[] correctPin = {'1', '2', '3', '4', '5', '6'};
        enterPinAndClick(pinDialog, correctPin);
        checkIfCorrectDialogIsShowing(
                getStringFromResource(mActivity, R.string.smartcard_error_dialog_positive_button)
        );
    }

    //Helper method... to get string from resource id.
    @NonNull
    private static String getStringFromResource(@NonNull final Activity activity, final int id) {
        return activity.getResources().getString(id);
    }

    //Return a list containing two mock certificates.
    @NonNull
    private List<X509Certificate> getMockCertList() {
        final X509Certificate cert1 = getMockCertificate("SomeIssuer1", "SomeSubject1");
        final X509Certificate cert2 = getMockCertificate("SomeIssuer2", "SomeSubject2");
        final List<X509Certificate> certList = new ArrayList<>();
        certList.add(cert1);
        certList.add(cert2);
        return certList;
    }

    //Returns the ClientCertAuthChallengeHandler set up with the TestSmartcardCertBasedAuthManager.
    //Calls processChallenge once.
    @NonNull
    private ClientCertAuthChallengeHandler setUpClientCertAuthChallengeHandlerAndProcess(@NonNull final Activity activity, @NonNull final List<X509Certificate> certList) {
        final TestSmartcardCertBasedAuthManager testSmartcardCertBasedAuthManager = new TestSmartcardCertBasedAuthManager(certList);
        final ClientCertAuthChallengeHandler clientCertAuthChallengeHandler = new ClientCertAuthChallengeHandler(activity, testSmartcardCertBasedAuthManager);
        clientCertAuthChallengeHandler.processChallenge(getMockClientCertRequest());
        return clientCertAuthChallengeHandler;
    }

    //Returns picker dialog if we successfully get there.
    @NonNull
    private AlertDialog goToPickerDialog(@NonNull final Activity activity) {
        return checkIfCorrectDialogIsShowing(
                getStringFromResource(activity, R.string.smartcard_cert_dialog_positive_button)
        );
    }

    //Returns PIN dialog if we successfully get there.
    @NonNull
    private AlertDialog goToPinDialog(@NonNull final Activity activity) {
        final AlertDialog pickerDialog = goToPickerDialog(activity);
        performClick(pickerDialog, DialogInterface.BUTTON_POSITIVE);
        return checkIfCorrectDialogIsShowing(
                getStringFromResource(activity, R.string.smartcard_pin_dialog_positive_button)
        );
    }

    //Enter PIN into dialog, check that components are correct, and click.
    private void enterPinAndClick(@NonNull final AlertDialog pinDialog, @NonNull char[] pin) {
        final EditText editText = pinDialog.findViewById(R.id.pinEditText);
        //Shouldn't be null if pinDialog is actually the PIN dialog.
        assertNotNull(editText);
        editText.setText(pin, 0, pin.length);
        //Make sure error messgae is not showing
        final TextView errorMessage = pinDialog.findViewById(R.id.errorTextView);
        assertNotNull(errorMessage);
        final String expectedErrorMessage = getStringFromResource(mActivity, R.string.smartcard_pin_dialog_error_message);
        assertNotEquals(expectedErrorMessage, errorMessage.getText());
        performClick(pinDialog, DialogInterface.BUTTON_POSITIVE);
    }

    //Perform a button click on the provided AlertDialog.
    private void performClick(@NonNull final AlertDialog dialog, final int whichButton) {
        dialog.getButton(whichButton).performClick();
        ShadowLooper.runUiThreadTasks();
    }

    //Check if dialog currently showing is correct based on the positive button text.
    //Note: Couldn't find a way to get title text from AlertDialog... so this seems
    // to be the next best option.
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

    //Check that no dialog is currently showing.
    private void ensureNoDialogIsShowing() {
        final AlertDialog dialog = (AlertDialog) ShadowAlertDialog.getLatestDialog();
        assertFalse(dialog.isShowing());
    }

    //"Unplug" a smartcard.
    private void mockUnplugSmartcard(@NonNull final ClientCertAuthChallengeHandler clientCertAuthChallengeHandler) {
        //This stops usb discovery, which should automatically disconnect a connected smartcard.
        clientCertAuthChallengeHandler.stopSmartcardUsbDiscovery();
        ShadowLooper.runUiThreadTasks();
    }

    //Return an empty ClientCertRequest only to be used for testing.
    @NonNull
    private ClientCertRequest getMockClientCertRequest() {
        return new ClientCertRequest() {
            @Override
            public String[] getKeyTypes() {
                return new String[0];
            }

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

    //Return a mock certificate only to be used for testing.
    @NonNull
    private X509Certificate getMockCertificate(@Nullable final String issuerDNName, @Nullable final String subjectDNName) {
        return new X509Certificate() {
            @Override
            public void checkValidity() {

            }

            @Override
            public void checkValidity(Date date) {

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
            public byte[] getTBSCertificate() {
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
            public byte[] getEncoded() {
                return new byte[0];
            }

            @Override
            public void verify(PublicKey publicKey) {

            }

            @Override
            public void verify(PublicKey publicKey, String s) {

            }

            @NonNull
            @Override
            public String toString() {
                return issuerDNName + " " + subjectDNName;
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

    //Implements ISmartcardCertBasedAuthManager in order to carry out testing of dialogs.
    //Only meant to be used for testing purposes.
    private static class TestSmartcardCertBasedAuthManager implements ISmartcardCertBasedAuthManager {

        private IStartDiscoveryCallback mStartDiscoveryCallback;
        private boolean mIsConnected;
        private final List<ICertDetails> mCertDetailsList;
        private int mPinAttemptsRemaining;

        public TestSmartcardCertBasedAuthManager(@NonNull final List<X509Certificate> certList) {
            mIsConnected = false;
            //Attempts remaining is usually 3, but 2 attempts is all that's necessary for testing.
            mPinAttemptsRemaining = 2;
            //Convert cert list into certDetails list.
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
        public void startDiscovery(@NonNull final IStartDiscoveryCallback startDiscoveryCallback) {
            mStartDiscoveryCallback = startDiscoveryCallback;
            mockConnect();
        }

        @Override
        public void stopDiscovery() {
            mockDisconnect();
        }

        @Override
        public void attemptDeviceSession(@NonNull final ISessionCallback callback) {
            try {
                callback.onGetSession(new TestSmartcardSession(mCertDetailsList, mPinAttemptsRemaining, new TestSmartcardSession.ITestSessionCallback() {
                    @Override
                    public void onIncorrectAttempt() {
                        mPinAttemptsRemaining--;
                    }
                }));
            } catch (@NonNull final Exception e) {
                callback.onException(e);
            }
        }

        @Override
        public boolean isDeviceConnected() {
            return mIsConnected;
        }

        @Override
        public void prepareForAuth() {
            //Since we don't go through with authentication for testing,
            // we don't need any logic here.
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

    //Implements ISmartcardSession in order to carry out testing of dialogs.
    //Only meant to be used for testing purposes.
    private static class TestSmartcardSession implements ISmartcardSession {

        private final List<ICertDetails> mCertDetailsList;
        private final char[] mPin;
        private int mPinAttemptsRemaining;

        private final ITestSessionCallback mCallback;

        //Used to keep the pinAttemptsRemaining variable consistent between the manager and session.
        interface ITestSessionCallback {
            void onIncorrectAttempt();
        }

        public TestSmartcardSession(@NonNull final List<ICertDetails> certDetailsList,
                                    final int pinAttemptsRemaining,
                                    @NonNull final ITestSessionCallback callback) {
            mCertDetailsList = certDetailsList;
            mPin = new char[]{'1', '2', '3', '4', '5', '6'};
            mPinAttemptsRemaining = pinAttemptsRemaining;
            mCallback = callback;
        }

        @NonNull
        @Override
        public List<ICertDetails> getCertDetailsList() {
            return mCertDetailsList;
        }

        @Override
        public boolean verifyPin(final char[] pin) {
            if (Arrays.equals(mPin, pin)) {
                return true;
            } else {
                mPinAttemptsRemaining = mPinAttemptsRemaining > 0 ? mPinAttemptsRemaining - 1 : 0;
                mCallback.onIncorrectAttempt();
                return false;
            }
        }

        @Override
        public int getPinAttemptsRemaining() {
            return mPinAttemptsRemaining;
        }

        //This method is going to be used to test handling a thrown exception,
        // so we should never get to the return statement.
        @NonNull
        @Override
        public PrivateKey getKeyForAuth(@NonNull final ICertDetails certDetails, final char[] pin) throws Exception {
            if (Arrays.equals(mPin, pin)) {
                throw new Exception("Testing, 1,2,3");
            }
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
    }
}
