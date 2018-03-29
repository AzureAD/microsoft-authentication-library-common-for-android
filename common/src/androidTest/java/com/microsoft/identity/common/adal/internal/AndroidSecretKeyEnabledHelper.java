package com.microsoft.identity.common.adal.internal;


import android.os.Build;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public abstract class AndroidSecretKeyEnabledHelper extends AndroidTestHelper {

    private static final int MIN_SDK_VERSION = 18;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        if (AuthenticationSettings.INSTANCE.getSecretKeyData() == null && Build.VERSION.SDK_INT < MIN_SDK_VERSION) {
            setSecretKeyData();
        }
    }

    private void setSecretKeyData() throws NoSuchAlgorithmException, InvalidKeySpecException, UnsupportedEncodingException {
        // use same key for tests
        SecretKeyFactory keyFactory = SecretKeyFactory
                .getInstance("PBEWithSHA256And256BitAES-CBC-BC");
        final int iterations = 100;
        final int keySize = 256;
        SecretKey tempkey = keyFactory.generateSecret(new PBEKeySpec("test".toCharArray(),
                "abcdedfdfd".getBytes("UTF-8"), iterations, keySize));
        SecretKey secretKey = new SecretKeySpec(tempkey.getEncoded(), "AES");
        AuthenticationSettings.INSTANCE.setSecretKey(secretKey.getEncoded());
    }
}
