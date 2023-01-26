package com.microsoft.identity.common.internal.ui.webview.certbasedauth;

import androidx.annotation.NonNull;

import java.security.PrivateKey;
import java.util.Arrays;
import java.util.List;

//Implements ISmartcardSession in order to carry out testing of dialogs.
//Only meant to be used for testing purposes.
class TestSmartcardSession implements ISmartcardSession {

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
