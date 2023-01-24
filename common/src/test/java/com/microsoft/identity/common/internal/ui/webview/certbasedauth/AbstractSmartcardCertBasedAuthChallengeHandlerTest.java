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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.webkit.ClientCertRequest;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.microsoft.identity.common.java.opentelemetry.CertBasedAuthChoice;
import com.microsoft.identity.common.java.opentelemetry.ICertBasedAuthTelemetryHelper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

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

import lombok.Getter;

@RunWith(RobolectricTestRunner.class)
public abstract class AbstractSmartcardCertBasedAuthChallengeHandlerTest {
    protected Activity mActivity;
    protected TestDialogHolder mDialogHolder;

    @Before
    public void setUp() {
        mActivity = Robolectric.buildActivity(Activity.class).get();
        mDialogHolder = new TestDialogHolder();
    }

    @Test
    public void testNoCertsOnSmartcard() {
        setAndProcessChallengeHandler(new ArrayList<>());
        checkIfCorrectDialogIsShowing(TestDialog.error);
    }

    @Test
    public void testCancelAtPickerDialog() {
        setAndProcessChallengeHandler(getMockCertList());
        checkIfCorrectDialogIsShowing(TestDialog.cert_picker);
        final SmartcardCertPickerDialog.CancelCbaCallback callback =  mDialogHolder.getMCertPickerCancelCbaCallback();
        assertNotNull(callback);
        callback.onCancel();
        checkIfCorrectDialogIsShowing(null);
    }

    @Test
    public void testCancelAtPinDialog() {
        setAndProcessChallengeHandler(getMockCertList());
        checkIfCorrectDialogIsShowing(TestDialog.cert_picker);
        goToPinDialog();
        final SmartcardPinDialog.CancelCbaCallback callback = mDialogHolder.getMPinCancelCbaCallback();
        assertNotNull(callback);
        callback.onCancel();
        checkIfCorrectDialogIsShowing(null);
    }

    @Test
    public abstract void testLockedOut();

    @Test
    public void testExceptionThrownWhenGettingCertDetailsList() {
        final List<X509Certificate> certList = new ArrayList<>();
        certList.add(getMockCertificate("Exception", "Exception"));
        setAndProcessChallengeHandler(certList);
        checkIfCorrectDialogIsShowing(TestDialog.error);
    }

    @Test
    public abstract void testExceptionThrownWhenVerifyingPin();

    @Test
    public abstract void testExceptionThrownWhenGettingKey();

    protected abstract void setAndProcessChallengeHandler(@NonNull final List<X509Certificate> certList);

    protected void checkIfCorrectDialogIsShowing(@Nullable final TestDialog expectedDialog) {
        if (expectedDialog == null) {
            assertFalse(mDialogHolder.isDialogShowing());
            return;
        }
        assertNotNull(mDialogHolder.getMCurrentDialog());
        assertTrue(mDialogHolder.isDialogShowing());
        assertEquals(expectedDialog, mDialogHolder.getMCurrentDialog());
    }

    protected void goToPinDialog() {
        final SmartcardCertPickerDialog.PositiveButtonListener listener = mDialogHolder.getMCertPickerPositiveButtonListener();
        assertNotNull(listener);
        listener.onClick(mDialogHolder.getMCertList().get(0));
        checkIfCorrectDialogIsShowing(TestDialog.pin);
    }

    //Return a list containing two mock certificates.
    @NonNull
    protected List<X509Certificate> getMockCertList() {
        final X509Certificate cert1 = getMockCertificate("SomeIssuer1", "SomeSubject1");
        final X509Certificate cert2 = getMockCertificate("SomeIssuer2", "SomeSubject2");
        final List<X509Certificate> certList = new ArrayList<>();
        certList.add(cert1);
        certList.add(cert2);
        return certList;
    }

    //Return an empty ClientCertRequest only to be used for testing.
    @NonNull
    protected ClientCertRequest getMockClientCertRequest() {
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
    protected X509Certificate getMockCertificate(@Nullable final String issuerDNName, @Nullable final String subjectDNName) {
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

    //Implements ISmartcardSession in order to carry out testing of dialogs.
    //Only meant to be used for testing purposes.
    protected static class TestSmartcardSession implements ISmartcardSession {

        private final List<ICertDetails> mCertDetailsList;
        private final char[] mPin;
        private int mPinAttemptsRemaining;

        private final TestSmartcardSession.ITestSessionCallback mCallback;

        //Used to keep the pinAttemptsRemaining variable consistent between the manager and session.
        interface ITestSessionCallback {
            void onIncorrectAttempt();
        }

        public TestSmartcardSession(@NonNull final List<ICertDetails> certDetailsList,
                                    final int pinAttemptsRemaining,
                                    @NonNull final TestSmartcardSession.ITestSessionCallback callback) {
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

    enum TestDialog {
        cert_picker,
        pin,
        error,
        user_choice,
        prompt,
        nfc_loading,
        nfc_prompt,
        nfc_reminder
    }

    @Getter
    protected static class TestDialogHolder implements IDialogHolder {

        private TestDialog mCurrentDialog;
        private SmartcardCertPickerDialog.PositiveButtonListener mCertPickerPositiveButtonListener;
        private SmartcardCertPickerDialog.CancelCbaCallback mCertPickerCancelCbaCallback;
        private SmartcardPinDialog.PositiveButtonListener mPinPositiveButtonListener;
        private SmartcardPinDialog.CancelCbaCallback mPinCancelCbaCallback;
        private SmartcardNfcPromptDialog.CancelCbaCallback mNfcPromptCancelCbaCallback;
        private List<ICertDetails> mCertList;


        TestDialogHolder() {
            mCurrentDialog = null;
            mCertPickerPositiveButtonListener = null;
            mCertPickerCancelCbaCallback = null;
            mPinPositiveButtonListener = null;
            mPinCancelCbaCallback = null;
        }

        @Override
        public void showCertPickerDialog(@NonNull List<ICertDetails> certList, @NonNull SmartcardCertPickerDialog.PositiveButtonListener positiveButtonListener, @NonNull SmartcardCertPickerDialog.CancelCbaCallback cancelCbaCallback) {
            mCurrentDialog = TestDialog.cert_picker;
            mCertPickerPositiveButtonListener = positiveButtonListener;
            mCertPickerCancelCbaCallback = cancelCbaCallback;
            mCertList = certList;
        }

        @Override
        public void showPinDialog(@NonNull SmartcardPinDialog.PositiveButtonListener positiveButtonListener, @NonNull SmartcardPinDialog.CancelCbaCallback cancelCbaCallback) {
            mCurrentDialog = TestDialog.pin;
            mPinPositiveButtonListener = positiveButtonListener;
            mPinCancelCbaCallback = cancelCbaCallback;
        }

        @Override
        public void showErrorDialog(int titleStringResourceId, int messageStringResourceId) {
            mCurrentDialog = TestDialog.error;
        }

        @Override
        public void showUserChoiceDialog(@NonNull UserChoiceDialog.PositiveButtonListener positiveButtonListener, @NonNull UserChoiceDialog.CancelCbaCallback cancelCbaCallback) {
            mCurrentDialog = TestDialog.user_choice;
        }

        @Override
        public void showSmartcardPromptDialog(@NonNull SmartcardPromptDialog.CancelCbaCallback cancelCbaCallback) {
            mCurrentDialog = TestDialog.prompt;
        }

        @Override
        public void showSmartcardNfcLoadingDialog() {
            mCurrentDialog = TestDialog.nfc_loading;
        }

        @Override
        public void showSmartcardNfcPromptDialog(@NonNull SmartcardNfcPromptDialog.CancelCbaCallback cancelCbaCallback) {
            mCurrentDialog = TestDialog.nfc_prompt;
            mNfcPromptCancelCbaCallback = cancelCbaCallback;
        }

        @Override
        public void showSmartcardNfcReminderDialog(@NonNull SmartcardNfcReminderDialog.DismissCallback dismissCallback) {
            mCurrentDialog = TestDialog.nfc_reminder;
        }

        @Override
        public void dismissDialog() {
            mCurrentDialog = null;
        }

        @Override
        public void showDialog(@Nullable SmartcardDialog dialog) {

        }

        @Override
        public boolean isDialogShowing() {
            return mCurrentDialog != null;
        }

        @Override
        public void onCancelCba() {

        }

        @Override
        public void setPinDialogErrorMode() {

        }
    }

    protected static class TestCertBasedAuthTelemetryHelper implements ICertBasedAuthTelemetryHelper {

        @Override
        public void setCertBasedAuthChallengeHandler(String challengeHandlerName) {

        }

        @Override
        public void setExistingPivProviderPresent(boolean present) {

        }

        @Override
        public void setResultSuccess() {

        }

        @Override
        public void setResultFailure(String message) {

        }

        @Override
        public void setResultFailure(Exception exception) {

        }

        @Override
        public void setUserChoice(CertBasedAuthChoice choice) {

        }
    }
}
