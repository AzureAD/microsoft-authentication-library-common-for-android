package com.microsoft.identity.common.internal.ui.webview.certbasedauth;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import androidx.annotation.Nullable;

import com.microsoft.identity.common.shadows.ShadowCertBasedAuthTelemetryHelper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;

@RunWith(RobolectricTestRunner.class)
@Config(shadows={ShadowCertBasedAuthTelemetryHelper.class})
public class CertBasedAuthFactoryTest extends AbstractCertBasedAuthTest {
    private CertBasedAuthFactory mFactory;
    protected TestUsbSmartcardCertBasedAuthManager mUsbManager;
    protected TestNfcSmartcardCertBasedAuthManager mNfcManager;
    final TestCertBasedAuthTelemetryHelper mTestCertBasedAuthTelemetryHelper = new TestCertBasedAuthTelemetryHelper();


    @Before
    public void factorySetUp() {
        mUsbManager = new TestUsbSmartcardCertBasedAuthManager(new ArrayList<>());
        mNfcManager = new TestNfcSmartcardCertBasedAuthManager(new ArrayList<>());
        mFactory = new CertBasedAuthFactory(mActivity, mUsbManager, mNfcManager, mDialogHolder);
    }

    @Test
    public void testInitiallyUsbConnected() {
        mUsbManager.mockConnect();
        mFactory.createCertBasedAuthChallengeHandler(new CertBasedAuthFactory.CertBasedAuthChallengeHandlerCallback() {
            @Override
            public void onReceived(@Nullable ICertBasedAuthChallengeHandler challengeHandler) {
                assertTrue(challengeHandler instanceof UsbSmartcardCertBasedAuthChallengeHandler);
            }
        });
    }

    @Test
    public void testCancelAtUserChoiceDialog() {
        mFactory.createCertBasedAuthChallengeHandler(new CertBasedAuthFactory.CertBasedAuthChallengeHandlerCallback() {
            @Override
            public void onReceived(@Nullable ICertBasedAuthChallengeHandler challengeHandler) {
                //nothing needed
            }
        });
        final UserChoiceDialog.CancelCbaCallback callback = mDialogHolder.getMUserChoiceCancelCbaCallback();
        assertNotNull(callback);
        callback.onCancel();
        checkIfCorrectDialogIsShowing(null);
    }

    @Test
    public void testCancelAtPromptDialog() {

    }

    @Test
    public void testChooseSmartcardAndProceedWithUsb() {

    }

    @Test
    public void testChooseSmartcardAndProceedWithNfc() {

    }
}
