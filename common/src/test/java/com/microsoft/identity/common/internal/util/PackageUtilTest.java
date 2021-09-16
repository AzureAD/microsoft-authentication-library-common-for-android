package com.microsoft.identity.common.internal.util;

import android.util.Base64;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class PackageUtilTest {

    @Test
    public void testHexStringConversion() {
        String string = PackageUtils.convertToBase64("DE:AD:BE:EF");
        Assert.assertEquals(Base64.encodeToString(new byte[]{(byte) (0xde & 0xff), (byte) (0xad &0xff), (byte) (0xbe & 0xff), (byte) (0xef & 0xff)}, Base64.NO_WRAP),
                string);
    }
}
