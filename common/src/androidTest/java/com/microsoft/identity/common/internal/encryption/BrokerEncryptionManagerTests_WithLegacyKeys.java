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

import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.AZURE_AUTHENTICATOR_APP_PACKAGE_NAME;

public class BrokerEncryptionManagerTests_WithLegacyKeys extends EncryptionManagerTests {
    @Override
    IEncryptionManager getManager() {
        final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        return new BrokerEncryptionManager(context, false);
    }

    @Override
    boolean verifyEncryptionType(@NonNull String encryptedString) throws UnsupportedEncodingException {
        return BaseEncryptionManager.getEncryptionType(encryptedString) == IEncryptionManager.EncryptionType.USER_DEFINED;
    }

    @Override
    void verifyEncryptingWithCorrectKey() throws IOException, GeneralSecurityException {
        final SecretKey legacySecretKey = BaseEncryptionManager.getSecretKeyFromRawByteArray(AuthenticationSettings.INSTANCE.getBrokerSecretKeys().get(AZURE_AUTHENTICATOR_APP_PACKAGE_NAME));
        final String serializedLegacySecretKey = BaseEncryptionManager.serializeSecretKey(legacySecretKey);

        final SecretKey managerSecretKey = getManager().loadKeyForEncryption().getKey();
        final String serializedManagerSecretKey = BaseEncryptionManager.serializeSecretKey(managerSecretKey);

        Assert.assertTrue(serializedLegacySecretKey.equalsIgnoreCase(serializedManagerSecretKey));
    }

    @Override
    void verifyEncryptedValue() throws IOException, GeneralSecurityException {
        final String clearText = "SomeValue1234";
        final String knownEncryptedValue = "cE1VTAwMfnJI6hPWtgbiRb20C8wtQggsRita3v+kyot0VHGcZvrX2ST6fvIdB9l3/MMOH72NSErCyukG7yFrq95g3obBbc=";
        final String decryptedValue = getManager().decrypt(knownEncryptedValue);

        Assert.assertTrue("decrypted string should match with the expected value", decryptedValue.equals(clearText));
    }
}
