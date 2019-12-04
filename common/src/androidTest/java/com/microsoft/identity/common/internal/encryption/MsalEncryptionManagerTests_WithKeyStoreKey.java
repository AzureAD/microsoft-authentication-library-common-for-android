package com.microsoft.identity.common.internal.encryption;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.test.platform.app.InstrumentationRegistry;

import com.microsoft.identity.common.adal.internal.AuthenticationSettings;

import org.junit.Assert;
import org.junit.Before;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;

import javax.crypto.SecretKey;

/**
 * Tests for MsalEncryptionManagerTests, with the default (Keystore-encrypted) key.
 */
public class MsalEncryptionManagerTests_WithKeyStoreKey extends EncryptionManagerTests {

    @Before
    @Override
    public void setUp() throws GeneralSecurityException, IOException {
        super.setUp();

        // Also clear secret key, so that it uses keystore key.
        AuthenticationSettings.INSTANCE.clearSecretKeysForTestCases();
    }

    @Override
    IEncryptionManager getManager() {
        final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        return new MsalEncryptionManager(context);
    }

    @Override
    boolean verifyEncryptionType(@NonNull String encryptedString) throws UnsupportedEncodingException {
        return BaseEncryptionManager.getEncryptionType(encryptedString) == IEncryptionManager.EncryptionType.ANDROID_KEY_STORE;
    }

    @Override
    void verifyEncryptingWithCorrectKey() throws IOException, GeneralSecurityException {
        final MsalEncryptionManager manager = (MsalEncryptionManager) getManager();
        final SecretKey msalKeyStoreKey = manager.generateKeyStoreEncryptedKey();
        final String serializedKeyStoreEncryptedSecretKey = BaseEncryptionManager.serializeSecretKey(msalKeyStoreKey);

        final SecretKey managerSecretKey = manager.loadKeyForEncryption().getKey();
        final String serializedManagerSecretKey = BaseEncryptionManager.serializeSecretKey(managerSecretKey);

        Assert.assertTrue(serializedKeyStoreEncryptedSecretKey.equalsIgnoreCase(serializedManagerSecretKey));
    }

    @Override
    void verifyEncryptedValue() throws IOException, GeneralSecurityException {
        final String clearText = "SomeValue1234";
        final String knownEncryptedValue = "cE1QTAwMewa18HbKfYStXy1rMDFSMb6wcl85AwsZA5wrLT7UqaMGTGp+Rs/cVuaPjqL/PrgbpLsa583TZqINuHiZFz8Jbk=";
        final SecretKey knownSecretKey = BaseEncryptionManager.deserializeSecretKey("M01Gv93WQ9vqtEYzjr5ca18xbufrsmskjwOkRB+U+iQ=\n");

        ((MsalEncryptionManager)getManager()).saveKeyStoreEncryptedKey(knownSecretKey);
        final String decryptedValue = getManager().decrypt(knownEncryptedValue);

        Assert.assertTrue("decrypted string should match with the expected value", decryptedValue.equals(clearText));
    }
}
