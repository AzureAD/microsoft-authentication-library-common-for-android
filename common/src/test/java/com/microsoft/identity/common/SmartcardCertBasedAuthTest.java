package com.microsoft.identity.common;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.app.Dialog;
import android.webkit.ClientCertRequest;

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
        ActivityController<DualScreenActivity> controller = Robolectric.buildActivity(DualScreenActivity.class);
        controller.setup(); // Moves Activity to RESUMED state
        Activity activity = controller.get();

        ClientCertRequest clientCertRequest = getMockClientCertRequest();

        TestSmartcardCertBasedAuthManager testSmartcardCertBasedAuthManager = new TestSmartcardCertBasedAuthManager(
                new ArrayList<>()
        );
        ClientCertAuthChallengeHandler clientCertAuthChallengeHandler = new ClientCertAuthChallengeHandler(activity,
                testSmartcardCertBasedAuthManager);
        clientCertAuthChallengeHandler.processChallenge(clientCertRequest);

        Dialog dialog = ShadowAlertDialog.getLatestDialog();
        assertNotNull(dialog);
        assertTrue(dialog.isShowing());
    }

    @Test
    public void testCertsOnSmartcard() {
        ActivityController<DualScreenActivity> controller = Robolectric.buildActivity(DualScreenActivity.class);
        controller.setup(); // Moves Activity to RESUMED state
        Activity activity = controller.get();

        ClientCertRequest clientCertRequest = getMockClientCertRequest();

        //Create mock certificates.
        X509Certificate cert1 = getMockCertificate("SomeIssuer1", "SomeSubject1");
        X509Certificate cert2 = getMockCertificate("SomeIssuer2", "SomeSubject2");

        List<X509Certificate> certList = new ArrayList<>();
        certList.add(cert1);
        certList.add(cert2);

        TestSmartcardCertBasedAuthManager testSmartcardCertBasedAuthManager = new TestSmartcardCertBasedAuthManager(certList);
        ClientCertAuthChallengeHandler clientCertAuthChallengeHandler = new ClientCertAuthChallengeHandler(activity, testSmartcardCertBasedAuthManager);
        clientCertAuthChallengeHandler.processChallenge(clientCertRequest);

        Dialog dialog = ShadowAlertDialog.getLatestDialog();
        assertNotNull(dialog);
        assertTrue(dialog.isShowing());

    }

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

        public TestSmartcardCertBasedAuthManager(@NonNull final List<X509Certificate> certList) {
            mIsConnected = false;
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
                callback.onGetSession(new TestSmartcardSession(mCertDetailsList));
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

        public TestSmartcardSession(List<ICertDetails> certDetailsList) {
            mCertDetailsList = certDetailsList;
            mPin = new char[]{'1', '2', '3', '4', '5', '6'};
            mPinAttemptsRemaining = 3;
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

        public void resetPinAttemptsRemaining() {
            mPinAttemptsRemaining = 3;
        }
    }
}
