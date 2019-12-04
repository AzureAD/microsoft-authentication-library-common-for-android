package com.microsoft.identity.common.internal.encryption;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.test.platform.app.InstrumentationRegistry;

import com.microsoft.identity.common.internal.util.StringUtil;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;

import javax.crypto.SecretKey;

public class BrokerEncryptionManagerTests_WithKeyStoreKeys extends EncryptionManagerTests {

    @Override
    IEncryptionManager getManager() {
        final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        return new BrokerEncryptionManager(context, true);
    }

    @Override
    boolean verifyEncryptionType(@NonNull String encryptedString) throws UnsupportedEncodingException {
        return BaseEncryptionManager.getEncryptionType(encryptedString) == IEncryptionManager.EncryptionType.ANDROID_KEY_STORE;
    }

    @Override
    void verifyEncryptingWithCorrectKey() throws IOException, GeneralSecurityException {
        final BrokerEncryptionManager manager = (BrokerEncryptionManager) getManager();
        final SecretKey brokerKeyStoreKey = manager.generateKeyStoreEncryptedKey();
        final String serializedKeyStoreEncryptedSecretKey = BaseEncryptionManager.serializeSecretKey(brokerKeyStoreKey);

        final SecretKey managerSecretKey = manager.loadKeyForEncryption().getKey();
        final String serializedManagerSecretKey = BaseEncryptionManager.serializeSecretKey(managerSecretKey);

        Assert.assertTrue(serializedKeyStoreEncryptedSecretKey.equalsIgnoreCase(serializedManagerSecretKey));
    }

    @Test
    public void testDecryptingDataEncryptedByLegacyKeys() throws GeneralSecurityException, IOException {
        final String clearText = "testText";
        final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        final BrokerEncryptionManager legacyKeyManager = new BrokerEncryptionManager(context, false);

        final String legacyKeyText = legacyKeyManager.encrypt(clearText);

        Assert.assertTrue(clearText.equals(getManager().decrypt(legacyKeyText)));
    }

    /**
     * If both are set, each subclasses should be able to get different keys.
     * This would happen if MSAL/ADAL is invoked by the broker app with the 'SkipBroker' option.
     * */
    @Test
    public void testReadBrokerAndMsalKeyStoreKeys() throws IOException, GeneralSecurityException {
        final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        final BrokerEncryptionManager brokerKeyManager = new BrokerEncryptionManager(context, true);
        final MsalEncryptionManager msalKeyManager = new MsalEncryptionManager(context);

        final SecretKey brokerSecretKey = brokerKeyManager.loadKeyForEncryption().getKey();
        final String serializedBrokerSecretKey = BaseEncryptionManager.serializeSecretKey(brokerSecretKey);

        final SecretKey msalSecretKey = msalKeyManager.loadKeyForEncryption().getKey();
        final String serializedMsalSecretKey = BaseEncryptionManager.serializeSecretKey(msalSecretKey);

        Assert.assertFalse(StringUtil.isEmpty(serializedBrokerSecretKey));
        Assert.assertFalse(StringUtil.isEmpty(serializedMsalSecretKey));
        Assert.assertFalse(serializedBrokerSecretKey.equalsIgnoreCase(serializedMsalSecretKey));
    }

    @Override
    void verifyEncryptedValue() throws IOException, GeneralSecurityException {
        final String clearText = "SomeValue1234";
        final String knownEncryptedValue = "cE1QTAwMewa18HbKfYStXy1rMDFSMb6wcl85AwsZA5wrLT7UqaMGTGp+Rs/cVuaPjqL/PrgbpLsa583TZqINuHiZFz8Jbk=";
        final SecretKey knownSecretKey = BaseEncryptionManager.deserializeSecretKey("M01Gv93WQ9vqtEYzjr5ca18xbufrsmskjwOkRB+U+iQ=\n");

        ((BrokerEncryptionManager)getManager()).saveKeyStoreEncryptedKey(knownSecretKey);
        final String decryptedValue = getManager().decrypt(knownEncryptedValue);

        Assert.assertTrue("decrypted string should match with the expected value", decryptedValue.equals(clearText));
    }
}
