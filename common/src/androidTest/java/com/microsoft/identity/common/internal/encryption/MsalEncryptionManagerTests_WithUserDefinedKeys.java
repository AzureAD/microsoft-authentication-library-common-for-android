package com.microsoft.identity.common.internal.encryption;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.test.platform.app.InstrumentationRegistry;

import com.microsoft.identity.common.adal.internal.AuthenticationSettings;

import org.junit.Assert;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;

import javax.crypto.SecretKey;

/**
 * Tests for MsalEncryptionManagerTests, but with user defined key.
 */
public class MsalEncryptionManagerTests_WithUserDefinedKeys extends EncryptionManagerTests {
    @Override
    IEncryptionManager getManager() {
        final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        return new MsalEncryptionManager(context);
    }

    @Override
    boolean verifyEncryptionType(@NonNull String encryptedString) throws UnsupportedEncodingException {
        return BaseEncryptionManager.getEncryptionType(encryptedString) == IEncryptionManager.EncryptionType.USER_DEFINED;
    }

    @Override
    void verifyEncryptingWithCorrectKey() throws IOException, GeneralSecurityException {
        final SecretKey adalSecretKey = BaseEncryptionManager.getSecretKeyFromRawByteArray(AuthenticationSettings.INSTANCE.getSecretKeyData());
        final String serializedAdalSecretKey = BaseEncryptionManager.serializeSecretKey(adalSecretKey);

        final SecretKey managerSecretKey = getManager().loadKeyForEncryption().getKey();
        final String serializedManagerSecretKey = BaseEncryptionManager.serializeSecretKey(managerSecretKey);

        Assert.assertTrue(serializedAdalSecretKey.equalsIgnoreCase(serializedManagerSecretKey));
    }

    @Override
    void verifyEncryptedValue() throws IOException, GeneralSecurityException {
        final String clearText = "SomeValue1234";
        final String knownEncryptedValue = "cE1VTAwMdk3Vd+r0Px/tr/0fRs62y+C6vLR2/SwsHH8l8d2D+rRyzhq1QXzWumfJlxNyMw7jrvOuNJg/p1BkqfyE6KZC9M=";
        final String decryptedValue = getManager().decrypt(knownEncryptedValue);

        Assert.assertTrue("decrypted string should match with the expected value", decryptedValue.equals(clearText));
    }
}
