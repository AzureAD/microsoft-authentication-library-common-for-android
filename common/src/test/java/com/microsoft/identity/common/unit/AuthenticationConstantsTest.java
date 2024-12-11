package com.microsoft.identity.common.unit;

import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.OAuth2.CODE;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.SWITCH_BROWSER.ACTION;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.SWITCH_BROWSER.ACTION_URI;

import android.net.Uri;

import com.microsoft.identity.common.adal.internal.AuthenticationConstants;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class AuthenticationConstantsTest {
    @Test
    public void testComputeMaxBrokerHostVersion() {
        Assert.assertEquals("5.0", AuthenticationConstants.Broker.computeMaxHostBrokerProtocol());
    }

    @Test
    public void testIsSwitchBrowserRequestMissingAllParams() {
        final Uri uri = Uri.parse("https://login.microsoftonline.com/");
        Assert.assertFalse(AuthenticationConstants.SWITCH_BROWSER.isSwitchBrowserRequest(uri));
    }

    @Test
    public void testIsSwitchBrowserRequestValid() {
        final Uri uri = Uri.parse("https://login.microsoftonline.com/switchbrowser/resume?" +
                ACTION_URI + "=action_uri&" +
                CODE + "=code&" +
                ACTION + "=action");
        Assert.assertTrue(AuthenticationConstants.SWITCH_BROWSER.isSwitchBrowserRequest(uri));
    }
}
