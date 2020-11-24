package com.microsoft.identity.common.unit.adal.internal;

import com.microsoft.identity.common.adal.internal.AuthenticationSettings;

import org.junit.Assert;
import org.junit.Test;

public class AuthenticationSettingsTest {
    @Test
    public void testSecretKeyStorage() {
        byte [] testKey = new byte[32];
        for (int i = 0; i < 32; i++) {
            testKey[i] = Double.valueOf(Math.random()).byteValue();
        }
        AuthenticationSettings.INSTANCE.setSecretKey(testKey);
        byte [] testKey2 = AuthenticationSettings.INSTANCE.getSecretKeyData();
        byte [] testKey3 = AuthenticationSettings.INSTANCE.getSecretKeyData();
        Assert.assertArrayEquals(testKey, testKey2);
        Assert.assertArrayEquals(testKey, testKey3);
        Assert.assertNotSame(testKey, testKey2);
        Assert.assertNotSame(testKey, testKey3);
        Assert.assertNotSame(testKey2, testKey3);
    }
}
