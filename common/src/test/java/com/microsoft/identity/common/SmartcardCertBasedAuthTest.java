package com.microsoft.identity.common;

import com.microsoft.identity.common.internal.ui.DualScreenActivity;
import com.microsoft.identity.common.internal.ui.webview.challengehandlers.ClientCertAuthChallengeHandler;
import com.microsoft.identity.common.internal.ui.webview.challengehandlers.DialogHolder;
import com.microsoft.identity.common.internal.ui.webview.challengehandlers.SmartcardCertPickerDialog;
import com.microsoft.identity.common.shadows.ShadowPivSession;
import com.microsoft.identity.common.shadows.ShadowUsbSmartCardConnection;
import com.microsoft.identity.common.shadows.ShadowUsbYubiKeyDevice;
import com.yubico.yubikit.android.transport.usb.UsbYubiKeyDevice;
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
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowAlertDialog;
import org.robolectric.shadows.ShadowUsbManager;

import java.io.IOException;
import java.lang.reflect.Field;
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
import androidx.test.core.app.ApplicationProvider;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.util.Log;
import android.view.View;
import android.webkit.ClientCertRequest;
import android.widget.TextView;

@RunWith(RobolectricTestRunner.class)
@Config(shadows={ShadowUsbManager.class, ShadowUsbSmartCardConnection.class, ShadowUsbYubiKeyDevice.class, ShadowPivSession.class})
public class SmartcardCertBasedAuthTest {

    final int YUBICO_VENDOR_ID = 0x1050;
    final int YUBICO_PRODUCT_ID = 0x0407;

    private UsbManager usbManager;

    @Mock
    UsbDevice usbDevice;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        usbManager = (UsbManager) ApplicationProvider.getApplicationContext().getSystemService(Context.USB_SERVICE);

        when(usbDevice.getDeviceName()).thenReturn("MockYubiKey");
        when(usbDevice.getVendorId()).thenReturn(YUBICO_VENDOR_ID);
        when(usbDevice.getProductId()).thenReturn(YUBICO_PRODUCT_ID);

        //For testing, always add device before initializing ClientCertAuthChallengeHandler.
        //Broadcasting that a Usb was connected (UsbManager.ACTION_USB_DEVICE_ATTACHED) doesn't work here for some reason.
        shadowOf(usbManager).addOrUpdateUsbDevice(usbDevice, true);
    }

    @Test
    public void testNoCertsOnYubiKey() {
        ActivityController<DualScreenActivity> controller = Robolectric.buildActivity(DualScreenActivity.class);
        controller.setup(); // Moves Activity to RESUMED state
        Activity activity = controller.get();

        ClientCertRequest clientCertRequest = getMockClientCertRequest();
        ClientCertAuthChallengeHandler clientCertAuthChallengeHandler = new ClientCertAuthChallengeHandler(activity);
        clientCertAuthChallengeHandler.processChallenge(clientCertRequest);

        Dialog dialog = ShadowAlertDialog.getLatestDialog();
        assertNotNull(dialog);
        assertTrue(dialog.isShowing());

        //close activity?
        //stop discovery
        clientCertAuthChallengeHandler.stopYubiKitManagerUsbDiscovery();
    }

    @Test
    public void testCertsOnYubKey() {
        ActivityController<DualScreenActivity> controller = Robolectric.buildActivity(DualScreenActivity.class);
        controller.setup(); // Moves Activity to RESUMED state
        Activity activity = controller.get();

        ClientCertRequest clientCertRequest = getMockClientCertRequest();
        ClientCertAuthChallengeHandler clientCertAuthChallengeHandler = new ClientCertAuthChallengeHandler(activity);

        //Access mDevice
        try {
            Field field = ClientCertAuthChallengeHandler.class.getDeclaredField("mDevice");
            field.setAccessible(true);
            UsbYubiKeyDevice device = (UsbYubiKeyDevice) field.get(clientCertAuthChallengeHandler);
            ShadowUsbYubiKeyDevice shadowDevice = (ShadowUsbYubiKeyDevice) Shadow.extract(device);
            //Create a fake cert
            X509Certificate authCert = getMockCert("Mock Issuer", "Mock Subject");
            shadowDevice.putCertificate(Slot.AUTHENTICATION, authCert);

            //try processing challenge
            clientCertAuthChallengeHandler.processChallenge(clientCertRequest);
            //should get cert picker dialog
            Dialog dialog = ShadowAlertDialog.getLatestDialog();
            assertNotNull(dialog);
            assertTrue(dialog.isShowing());

            Field dialogHolderField = ClientCertAuthChallengeHandler.class.getDeclaredField("mDialogHolder");
            dialogHolderField.setAccessible(true);
            DialogHolder dialogHolder = (DialogHolder) dialogHolderField.get(clientCertAuthChallengeHandler);
            assertNotNull(dialogHolder);
            assertNotNull(dialogHolder.getDialogSimpleName());
            assertEquals(SmartcardCertPickerDialog.class.getSimpleName(), dialogHolder.getDialogSimpleName());

            //stop discovery
            clientCertAuthChallengeHandler.stopYubiKitManagerUsbDiscovery();

        } catch (Exception e) {
            // do nothing for now
            Log.e("tag", "message", e);
        }
    }

    private X509Certificate getMockCert(@Nullable final String issuerDNName, @Nullable final String subjectDNName) {
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

}
