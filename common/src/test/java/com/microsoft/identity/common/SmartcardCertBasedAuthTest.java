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
import com.microsoft.identity.common.internal.ui.webview.challengehandlers.CertBasedAuthFactory;
import com.microsoft.identity.common.internal.ui.webview.challengehandlers.ICertBasedAuthChallengeHandler;
import com.microsoft.identity.common.internal.ui.webview.challengehandlers.ICertDetails;
import com.microsoft.identity.common.internal.ui.webview.challengehandlers.AbstractSmartcardCertBasedAuthManager;
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
    //Controller to build, get, and restart activity
    private static final ActivityController<DualScreenActivity> mController = Robolectric.buildActivity(DualScreenActivity.class);
    //Host activity
    private Activity mActivity = mController.get();
    //Resource strings
    private final String SMARTCARD_CERT_DIALOG_POSITIVE_BUTTON = mActivity.getResources().getString(R.string.smartcard_cert_dialog_positive_button);
    private final String SMARTCARD_PIN_DIALOG_POSITIVE_BUTTON = mActivity.getResources().getString(R.string.smartcard_pin_dialog_positive_button);
    private final String SMARTCARD_PIN_DIALOG_ERROR_MESSAGE = mActivity.getResources().getString(R.string.smartcard_pin_dialog_error_message);
    private final String SMARTCARD_ERROR_DIALOG_POSITIVE_BUTTON = mActivity.getResources().getString(R.string.smartcard_error_dialog_positive_button);

    //Need to restart activity for every test.
    @Before
    public void setUp() {
        mController.restart();
        mActivity = mController.get();
    }

    //Should show error dialog.
    @Test
    public void testNoCertsOnSmartcard() {
        final AbstractSmartcardCertBasedAuthManager smartcardCertBasedAuthManager = new TestSmartcardCertBasedAuthManager(new ArrayList<>());
        setUpSmartcardCertBasedAuthChallengeHandlerAndProcess(mActivity, smartcardCertBasedAuthManager);
        checkIfCorrectDialogIsShowing(SMARTCARD_ERROR_DIALOG_POSITIVE_BUTTON);
    }

    //Basic test to get to picker dialog.
    @Test
    public void testGetToCertPickerDialog() {
        final AbstractSmartcardCertBasedAuthManager smartcardCertBasedAuthManager = new TestSmartcardCertBasedAuthManager(getMockCertList());
        setUpSmartcardCertBasedAuthChallengeHandlerAndProcess(mActivity, smartcardCertBasedAuthManager);
        goToPickerDialog();
    }

    //Basic test to get to PIN dialog.
    @Test
    public void testGetToPinDialog() {
        final AbstractSmartcardCertBasedAuthManager smartcardCertBasedAuthManager = new TestSmartcardCertBasedAuthManager(getMockCertList());
        setUpSmartcardCertBasedAuthChallengeHandlerAndProcess(mActivity, smartcardCertBasedAuthManager);
        goToPinDialog();
    }

    //Clicking cancel button should result in no dialog showing.
    @Test
    public void testCancelAtPickerDialog() {
        final AbstractSmartcardCertBasedAuthManager smartcardCertBasedAuthManager = new TestSmartcardCertBasedAuthManager(getMockCertList());
        setUpSmartcardCertBasedAuthChallengeHandlerAndProcess(mActivity, smartcardCertBasedAuthManager);
        final AlertDialog pickerDialog = goToPickerDialog();
        performClick(pickerDialog, DialogInterface.BUTTON_NEGATIVE);
        ensureNoDialogIsShowing();
    }

    //Clicking cancel button should result in no dialog showing.
    @Test
    public void testCancelAtPinDialog() {
        final AbstractSmartcardCertBasedAuthManager smartcardCertBasedAuthManager = new TestSmartcardCertBasedAuthManager(getMockCertList());
        setUpSmartcardCertBasedAuthChallengeHandlerAndProcess(mActivity, smartcardCertBasedAuthManager);
        final AlertDialog pinDialog = goToPinDialog();
        performClick(pinDialog, DialogInterface.BUTTON_NEGATIVE);
        ensureNoDialogIsShowing();
    }

    //Should result in error dialog showing.
    @Test
    public void testUnplugAtPickerDialog() {
        final AbstractSmartcardCertBasedAuthManager smartcardCertBasedAuthManager = new TestSmartcardCertBasedAuthManager(getMockCertList());
        setUpSmartcardCertBasedAuthChallengeHandlerAndProcess(mActivity, smartcardCertBasedAuthManager);
        goToPickerDialog();
        //Unplugging while dialog is showing should result in an error dialog.
        mockUnplugSmartcard(smartcardCertBasedAuthManager);
        checkIfCorrectDialogIsShowing(SMARTCARD_ERROR_DIALOG_POSITIVE_BUTTON);
    }

    //Should result in error dialog showing.
    @Test
    public void testUnplugAtPinDialog() {
        final AbstractSmartcardCertBasedAuthManager smartcardCertBasedAuthManager = new TestSmartcardCertBasedAuthManager(getMockCertList());
        setUpSmartcardCertBasedAuthChallengeHandlerAndProcess(mActivity, smartcardCertBasedAuthManager);
        goToPinDialog();
        //Unplugging while dialog is showing should result in an error dialog.
        mockUnplugSmartcard(smartcardCertBasedAuthManager);
        checkIfCorrectDialogIsShowing(SMARTCARD_ERROR_DIALOG_POSITIVE_BUTTON);
    }

    //Incorrect PIN attempts before reaching limit should remain on same dialog with error message.
    //Upon no PIN attempts remaining, an error dialog should show instead.
    //Note: pin attempts remaining is initially set to 2.
    @Test
    public void testLockedOut() {
        final AbstractSmartcardCertBasedAuthManager smartcardCertBasedAuthManager = new TestSmartcardCertBasedAuthManager(getMockCertList());
        ICertBasedAuthChallengeHandler certBasedAuthChallengeHandler = setUpSmartcardCertBasedAuthChallengeHandlerAndProcess(mActivity, smartcardCertBasedAuthManager);
        final AlertDialog pinDialog = goToPinDialog();
        //Test typing in incorrect PIN.
        final char[] wrongPin = {'1', '2', '3'};
        enterPinAndClick(pinDialog, wrongPin);
        //Dialog should be the same.
        checkIfCorrectDialogIsShowing(SMARTCARD_PIN_DIALOG_POSITIVE_BUTTON);
        //Make sure error message is seen.
        final TextView errorMessage = pinDialog.findViewById(R.id.errorTextView);
        assertNotNull(errorMessage);
        assertEquals(SMARTCARD_PIN_DIALOG_ERROR_MESSAGE, errorMessage.getText());
        //Upon click again, an error dialog should appear.
        enterPinAndClick(pinDialog, wrongPin);
        final AlertDialog errorDialog = checkIfCorrectDialogIsShowing(SMARTCARD_ERROR_DIALOG_POSITIVE_BUTTON);
        //Finally, we'll try to start the flow over again,
        // and it should block us with an error dialog.
        performClick(errorDialog, DialogInterface.BUTTON_POSITIVE);
        ensureNoDialogIsShowing();
        certBasedAuthChallengeHandler.processChallenge(getMockClientCertRequest());
        checkIfCorrectDialogIsShowing(SMARTCARD_ERROR_DIALOG_POSITIVE_BUTTON);
    }

    //When getting the cert details list for the picker, should show a generic error dialog
    // upon unexpected exception thrown.
    @Test
    public void testExceptionThrownWhenGettingCertDetailsList() {
        //Create a specific certList with one cert that has "Exception" as the issuer name.
        //The Test session is set up so that in this specific case, it will throw an exception
        // for testing purposes.
        final List<X509Certificate> certList = new ArrayList<>();
        certList.add(getMockCertificate("Exception", "Exception"));
        final AbstractSmartcardCertBasedAuthManager smartcardCertBasedAuthManager = new TestSmartcardCertBasedAuthManager(certList);
        setUpSmartcardCertBasedAuthChallengeHandlerAndProcess(mActivity, smartcardCertBasedAuthManager);
        checkIfCorrectDialogIsShowing(SMARTCARD_ERROR_DIALOG_POSITIVE_BUTTON);
    }

    //In pin dialog, should show a generic error dialog upon unexpected exception thrown
    // while interacting with a session.
    @Test
    public void testExceptionThrownWhenVerifyingPin() {
        final AbstractSmartcardCertBasedAuthManager smartcardCertBasedAuthManager = new TestSmartcardCertBasedAuthManager(getMockCertList());
        setUpSmartcardCertBasedAuthChallengeHandlerAndProcess(mActivity, smartcardCertBasedAuthManager);
        final AlertDialog pinDialog = goToPinDialog();
        //Type in PIN of "exc", which (in this testing case only) should throw an exception
        // within the verifyPin method. An error dialog should appear.
        final char[] exceptionPin = {'e', 'x', 'c'};
        enterPinAndClick(pinDialog, exceptionPin);
        checkIfCorrectDialogIsShowing(SMARTCARD_ERROR_DIALOG_POSITIVE_BUTTON);
    }

    //When an unexpected exception is thrown, a generic error dialog should appear.
    @Test
    public void testExceptionThrownWhenGettingKey() {
        final AbstractSmartcardCertBasedAuthManager smartcardCertBasedAuthManager = new TestSmartcardCertBasedAuthManager(getMockCertList());
        setUpSmartcardCertBasedAuthChallengeHandlerAndProcess(mActivity, smartcardCertBasedAuthManager);
        final AlertDialog pinDialog = goToPinDialog();
        //Type in correct PIN, which (in this testing case only) should throw an exception
        // within the getKeyForAuth method. An error dialog should appear.
        final char[] correctPin = {'1', '2', '3', '4', '5', '6'};
        enterPinAndClick(pinDialog, correctPin);
        checkIfCorrectDialogIsShowing(SMARTCARD_ERROR_DIALOG_POSITIVE_BUTTON);
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

    //Returns the SmartcardCertBasedAuthChallengeHandler set up with the TestSmartcardCertBasedAuthManager.
    //Calls processChallenge once.
    @NonNull
    private ICertBasedAuthChallengeHandler setUpSmartcardCertBasedAuthChallengeHandlerAndProcess(@NonNull final Activity activity,
                                                                                                 @NonNull final AbstractSmartcardCertBasedAuthManager smartcardCertBasedAuthManager) {
        smartcardCertBasedAuthManager.setConnectionCallback(new AbstractSmartcardCertBasedAuthManager.IConnectionCallback() {
            @Override
            public void onCreateConnection() {
                //Nothing needed
            }

            @Override
            public void onClosedConnection() {
                //Nothing needed
            }
        });
        final CertBasedAuthFactory certBasedAuthFactory = new TestCertBasedAuthFactory(activity, smartcardCertBasedAuthManager);
        final ICertBasedAuthChallengeHandler certBasedAuthChallengeHandler = certBasedAuthFactory.createCertBasedAuthChallengeHandler();
        certBasedAuthChallengeHandler.processChallenge(getMockClientCertRequest());
        return certBasedAuthChallengeHandler;
    }

    //Returns picker dialog if we successfully get there.
    @NonNull
    private AlertDialog goToPickerDialog() {
        return checkIfCorrectDialogIsShowing(SMARTCARD_CERT_DIALOG_POSITIVE_BUTTON);
    }

    //Returns PIN dialog if we successfully get there.
    @NonNull
    private AlertDialog goToPinDialog() {
        final AlertDialog pickerDialog = goToPickerDialog();
        performClick(pickerDialog, DialogInterface.BUTTON_POSITIVE);
        return checkIfCorrectDialogIsShowing(SMARTCARD_PIN_DIALOG_POSITIVE_BUTTON);
    }

    //Enter PIN into dialog, check that components are correct, and click.
    private void enterPinAndClick(@NonNull final AlertDialog pinDialog, @NonNull char[] pin) {
        final EditText editText = pinDialog.findViewById(R.id.pinEditText);
        //Shouldn't be null if pinDialog is actually the PIN dialog.
        assertNotNull(editText);
        editText.setText(pin, 0, pin.length);
        //Make sure error message is not showing
        final TextView errorMessage = pinDialog.findViewById(R.id.errorTextView);
        assertNotNull(errorMessage);
        assertNotEquals(SMARTCARD_PIN_DIALOG_ERROR_MESSAGE, errorMessage.getText());
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
    private void mockUnplugSmartcard(@NonNull final AbstractSmartcardCertBasedAuthManager smartcardCertBasedAuthManager) {
        //This stops usb discovery, which should automatically disconnect a connected smartcard.
        smartcardCertBasedAuthManager.stopDiscovery();
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

    //For testing purposes only.
    //Uses protected constructor of CertBasedAuthFactory in order to inject test manager.
    private static class TestCertBasedAuthFactory extends CertBasedAuthFactory {

        protected TestCertBasedAuthFactory(@NonNull Activity activity,
                                           @NonNull AbstractSmartcardCertBasedAuthManager manager) {
            super(activity, manager);
        }
    }

    //Implements AbstractSmartcardCertBasedAuthManager in order to carry out testing of dialogs.
    //Only meant to be used for testing purposes.
    private static class TestSmartcardCertBasedAuthManager extends AbstractSmartcardCertBasedAuthManager {

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
        public void startDiscovery() {
            mockConnect();
        }

        @Override
        public void stopDiscovery() {
            mockDisconnect();
        }

        @Override
        public void requestDeviceSession(@NonNull final ISessionCallback callback) {
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
        public void initBeforeProceedingWithRequest() {
            //Since we don't go through with authentication for testing,
            // we don't need any logic here.
        }

        @Override
        public void onDestroy() {
            stopDiscovery();
        }

        public void mockConnect() {
            if (mConnectionCallback != null) {
                mConnectionCallback.onCreateConnection();
                mIsConnected = true;
            }
        }

        public void mockDisconnect() {
            if (mConnectionCallback != null) {
                mConnectionCallback.onClosedConnection();
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
        public List<ICertDetails> getCertDetailsList() throws Exception {
            //Check for a specific testing case where if there's only one cert
            // and it has a subject value of "Exception", throw an Exception.
            if (mCertDetailsList.size() == 1 &&
                    mCertDetailsList.get(0).getCertificate().getIssuerDN().getName().equals("Exception")) {
                throw new Exception();
            }
            return mCertDetailsList;
        }

        @Override
        public boolean verifyPin(@NonNull final char[] pin) throws Exception {
            if (Arrays.equals(mPin, pin)) {
                return true;
            }
            else if (Arrays.equals(new char[]{'e', 'x', 'c'}, pin)) {
                //This is a special case where we want to test handling an exception.
                throw new Exception();
            }
            else {
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
        public PrivateKey getKeyForAuth(@NonNull final ICertDetails certDetails, @NonNull final char[] pin) throws Exception {
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
