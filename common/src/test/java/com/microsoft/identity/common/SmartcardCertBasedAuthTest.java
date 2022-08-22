package com.microsoft.identity.common;

import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationActivity;
import com.microsoft.identity.common.internal.providers.oauth2.WebViewAuthorizationFragment;
import com.microsoft.identity.common.internal.ui.webview.challengehandlers.ClientCertAuthChallengeHandler;
import com.microsoft.identity.common.shadows.ShadowPivSession;
import com.yubico.yubikit.core.Transport;
import com.yubico.yubikit.core.application.ApplicationNotAvailableException;
import com.yubico.yubikit.core.application.BadResponseException;
import com.yubico.yubikit.core.smartcard.ApduException;
import com.yubico.yubikit.core.smartcard.SmartCardConnection;
import com.yubico.yubikit.piv.PivSession;
import com.yubico.yubikit.piv.Slot;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlertDialog;
import org.robolectric.shadows.ShadowUsbManager;
import static org.robolectric.RuntimeEnvironment.application;

import java.io.IOException;
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
import java.util.Date;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import androidx.annotation.Nullable;
import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.webkit.ClientCertRequest;
import android.webkit.WebView;

@RunWith(RobolectricTestRunner.class)
@Config(shadows={ShadowUsbManager.class, ShadowPivSession.class})
public class SmartcardCertBasedAuthTest {

    final int YUBICO_VENDOR_ID = 0x1050;

    private UsbManager usbManager;

    @Mock
    UsbDevice usbDevice;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        usbManager = (UsbManager) ApplicationProvider.getApplicationContext().getSystemService(Context.USB_SERVICE);

        when(usbDevice.getDeviceName()).thenReturn("MockYubiKey");
        when(usbDevice.getVendorId()).thenReturn(YUBICO_VENDOR_ID);
    }

    @Test
    public void testAddUsbYubiKeyDevice() {
        //ActivityController<AuthorizationActivity> controller = Robolectric.buildActivity(AuthorizationActivity.class);
        //controller.setup();
        //AuthorizationActivity activity = controller.get();

        //set up ClientCertAuthChallengeHandler
/*        try(ActivityScenario<AuthorizationActivity> scenario = ActivityScenario.launch(AuthorizationActivity.class)) {
            scenario.moveToState(Lifecycle.State.RESUMED);
            scenario.onActivity(new ActivityScenario.ActivityAction<AuthorizationActivity>() {
                @Override
                public void perform(AuthorizationActivity activity) {
                    ClientCertAuthChallengeHandler clientCertAuthChallengeHandler = new ClientCertAuthChallengeHandler(activity);
                    assertFalse(usbManager.getDeviceList().values().isEmpty());
                }
            });
        }*/
        //ClientCertAuthChallengeHandler clientCertAuthChallengeHandler = new ClientCertAuthChallengeHandler(Robolectric.setupActivity(Activity.class));
        ActivityController<Activity> controller = Robolectric.buildActivity(Activity.class);
        controller.setup(); // Moves Activity to RESUMED state
        Activity activity = controller.get();
        ClientCertAuthChallengeHandler clientCertAuthChallengeHandler = new ClientCertAuthChallengeHandler(activity);
        //add to usbmanager?
        //application.sendBroadcast(new Intent(UsbManager.ACTION_USB_PORT_CHANGED));
        shadowOf(usbManager).addOrUpdateUsbDevice(usbDevice, true);
        application.sendBroadcast(new Intent(UsbManager.ACTION_USB_DEVICE_ATTACHED));
        ClientCertRequest clientCertRequest = new ClientCertRequest() {
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
        clientCertAuthChallengeHandler.processChallenge(clientCertRequest);
        //assertFalse(usbManager.getDeviceList().values().isEmpty());
        List<Dialog> dialogs = ShadowAlertDialog.getShownDialogs();
        assertEquals(1, dialogs.size());


    }

    @Test
    public void testPivSession() throws ApduException, IOException, ApplicationNotAvailableException, BadResponseException {
        PivSession ps  = new PivSession(new SmartCardConnection() {
            @Override
            public byte[] sendAndReceive(byte[] apdu) throws IOException {
                return new byte[0];
            }

            @Override
            public Transport getTransport() {
                return null;
            }

            @Override
            public boolean isExtendedLengthApduSupported() {
                return false;
            }

            @Override
            public void close() throws IOException {

            }
        });

        ps.putCertificate(Slot.AUTHENTICATION, new X509Certificate() {
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
                return null;
            }

            @Override
            public Principal getSubjectDN() {
                return null;
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
        });

        assertNotNull(ps.getCertificate(Slot.AUTHENTICATION));

    }

}
