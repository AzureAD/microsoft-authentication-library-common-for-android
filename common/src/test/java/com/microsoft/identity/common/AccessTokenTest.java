package com.microsoft.identity.common;

import com.microsoft.identity.common.adal.internal.AuthenticationSettings;
import com.microsoft.identity.common.internal.dto.AccessToken;

import junit.framework.Assert;

import org.junit.Test;

import java.util.Calendar;

public class AccessTokenTest {

    private static final int ONE_MINUTE = 60;
    private static final int NEGATIVE_FIVE_MINUTES = -ONE_MINUTE * 5;

    @Test
    public void testExpiry() {
        // Set a 0 min expiry buffer
        AuthenticationSettings.INSTANCE.setExpirationBuffer(0);

        final AccessToken accessToken = new AccessToken();
        accessToken.setExpiresOn(getCurrentTimeStr());
        Assert.assertTrue(accessToken.isExpired());
    }

    @Test
    public void testExpiryWithExtExpiresOn() {
        // Set a 0 min expiry buffer
        AuthenticationSettings.INSTANCE.setExpirationBuffer(0);

        final AccessToken accessToken = new AccessToken();
        final String currentTime = getCurrentTimeStr();
        final String currentTimePlus5Min = String.valueOf(Long.valueOf(currentTime) + (5 * ONE_MINUTE));
        accessToken.setExpiresOn(currentTime);
        accessToken.setExtendedExpiresOn(currentTimePlus5Min);
        Assert.assertFalse(accessToken.isExpired());
    }

    @Test
    public void testExpiryWithBuffer() {
        // Set a 5 min expiry buffer
        AuthenticationSettings.INSTANCE.setExpirationBuffer(NEGATIVE_FIVE_MINUTES);

        final AccessToken accessToken = new AccessToken();
        accessToken.setExpiresOn(getCurrentTimeStr());
        Assert.assertFalse(accessToken.isExpired());
    }

    private final String getCurrentTimeStr() {
        return String.valueOf(
                Calendar
                        .getInstance()
                        .getTime()
                        .getTime() / 1000
        );
    }
}
