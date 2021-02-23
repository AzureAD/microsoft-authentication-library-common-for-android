package com.microsoft.identity.common.internal.platform;

import androidx.test.runner.AndroidJUnitRunner;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.security.KeyStore;

public class RawKeyAccessorTest {
    @Test
    public void testEncryptDecrypt() throws Exception {
        RawKeyAccessor a = new RawKeyAccessor(new CryptoSuite() {
            @Override
            public String cipherName() {
                return "AES/GCM/NoPadding";
            }

            @Override
            public String macName() {
                return "HmacSHA256";
            }

            @Override
            public boolean isAsymmetric() {
                return false;
            }

            @Override
            public Class<? extends KeyStore.Entry> keyClass() {
                return null;
            }

            @Override
            public int keySize() {
                return 256;
            }
        }, "12345678123456781234567812345678".getBytes("UTF-8"));

        final byte[] in = "ABCDEFGHABCDEFGHABCDEFGHABCDEFGH".getBytes();
        byte[] out = a.encrypt(in);
        byte[] around = a.decrypt(out);
        Assert.assertArrayEquals(in, around);
    }

    @Test
    public void testSignAndVerify() throws Exception {
        RawKeyAccessor a = new RawKeyAccessor(new CryptoSuite() {
            @Override
            public String cipherName() {
                return "AES/GCM/NoPadding";
            }

            @Override
            public String macName() {
                return "HmacSHA256";
            }

            @Override
            public boolean isAsymmetric() {
                return false;
            }

            @Override
            public Class<? extends KeyStore.Entry> keyClass() {
                return null;
            }

            @Override
            public int keySize() {
                return 256;
            }
        }, "12345678123456781234567812345678".getBytes("UTF-8"));

        final byte[] in = "ABCDEFGHABCDEFGHABCDEFGHABCDEFGH".getBytes();
        byte[] out = a.sign(in, IDevicePopManager.SigningAlgorithm.SHA_256_WITH_RSA);
        Assert.assertTrue(a.verify(in, IDevicePopManager.SigningAlgorithm.SHA_256_WITH_RSA, out));
    }
}
