package com.microsoft.identity.common.internal.platform;

import org.junit.Assert;
import org.junit.Test;

import java.security.SecureRandom;

public class KeyStoreAccessorTests {
    private static final SecureRandom RANDOM = new SecureRandom();

    @Test
    public void testBasicFunctionality() throws Exception {
        KeyAccessor accessor = KeyStoreAccessor.newInstance(SymmetricCipher.AES_GCM_NONE_HMACSHA256);
        byte[] in = new byte[1024];
        RANDOM.nextBytes(in);
        byte[] out = accessor.encrypt(in);
        byte[] around = accessor.decrypt(out);
        Assert.assertArrayEquals(in, around);

        byte[] signature = accessor.sign(in, IDevicePopManager.SigningAlgorithm.SHA_256_WITH_RSA);
        boolean verified = accessor.verify(in, IDevicePopManager.SigningAlgorithm.SHA_256_WITH_RSA, signature);
        Assert.assertTrue(verified);
    }
}
