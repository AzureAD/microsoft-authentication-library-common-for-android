// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// This code is licensed under the MIT License.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files(the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions :
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.
package com.microsoft.identity.common.internal.encryption;

import android.content.Context;
import android.util.Base64;

import androidx.annotation.NonNull;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.microsoft.identity.common.adal.internal.AndroidSecretKeyEnabledHelper;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.util.Locale;


import javax.crypto.SecretKey;

import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.AZURE_AUTHENTICATOR_APP_PACKAGE_NAME;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public abstract class EncryptionManagerTests {

    @Before
    public void setUp() throws GeneralSecurityException, IOException {
        // Set everything, so that we can verify that everything works as expected.
        AndroidSecretKeyEnabledHelper.setAdalSecretKeyData();
        AndroidSecretKeyEnabledHelper.setLegacyBrokerSecretKeysData();
        BrokerEncryptionManager.setMockPackageName(AZURE_AUTHENTICATOR_APP_PACKAGE_NAME);

        final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        final BrokerEncryptionManager brokerEncryptionManager = new BrokerEncryptionManager(context);
        brokerEncryptionManager.generateKeyStoreEncryptedKey();

        final MsalEncryptionManager msalEncryptionManager = new MsalEncryptionManager(context);
        msalEncryptionManager.deleteKeyStoreEncryptedKey();
    }

    abstract IEncryptionManager getManager();

    abstract boolean verifyEncryptionType(@NonNull final String encryptedString) throws UnsupportedEncodingException;

    abstract void verifyEncryptingWithCorrectKey() throws IOException, GeneralSecurityException;

    abstract void verifyEncryptedValue() throws IOException, GeneralSecurityException;

    /**
     * Make sure the right key is being used to encrypt.
     */
    @Test
    public void testEncryptingWithCorrectKey() throws IOException, GeneralSecurityException {
        verifyEncryptingWithCorrectKey();
    }

    /**
     * Test encrypt and decrypt null and empty strings.
     */
    @Test
    public void testEncryptDecryptNullEmpty() {
        final IEncryptionManager manager = getManager();

        assertThrowsException(
                IllegalArgumentException.class,
                "Input is empty or null",
                new ThrowableRunnable() {
                    @Override
                    public void run() throws GeneralSecurityException, IOException {
                        manager.encrypt(null);
                    }
                });
        assertThrowsException(
                IllegalArgumentException.class,
                "Input is empty or null",
                new ThrowableRunnable() {
                    @Override
                    public void run() throws GeneralSecurityException, IOException {
                        manager.encrypt("");
                    }
                });

        assertThrowsException(
                IllegalArgumentException.class,
                "Input is empty or null",
                new ThrowableRunnable() {
                    @Override
                    public void run() throws GeneralSecurityException, IOException {
                        manager.decrypt(null);
                    }
                });

        assertThrowsException(
                IllegalArgumentException.class,
                "Input is empty or null",
                new ThrowableRunnable() {
                    @Override
                    public void run() throws GeneralSecurityException, IOException {
                        manager.decrypt("");
                    }
                });
    }

    /**
     * Test encrypt and decrypt a given value.
     */
    @Test
    public void testEncryptDecrypt() throws GeneralSecurityException, IOException {
        String clearText = "SomeValue1234";
        encryptDecrypt(getManager(), clearText);
    }

    /**
     * Given a known encrypted value, make sure it can be decrypted properly.
     * The encrypted values are taken from StorageHelper's test cases (before refactorization).
     */
    @Test
    public void testVerifyEncryptedValue() throws GeneralSecurityException, IOException {
        verifyEncryptedValue();
    }

    /**
     * test encrypt and decrypt messages with different size.
     */
    @Test
    public void testEncryptDecryptDifferentSizes() throws GeneralSecurityException, IOException {
        // try different block sizes
        final int sizeRange = 1000;
        final StringBuilder buf = new StringBuilder(sizeRange);
        final IEncryptionManager manager = getManager();
        for (int i = 0; i < sizeRange; i++) {
            encryptDecrypt(manager, buf.append("a").toString());
        }
    }

    private void encryptDecrypt(@NonNull final IEncryptionManager manager, @NonNull final String clearText) throws GeneralSecurityException, IOException {
        String encrypted = manager.encrypt(clearText);
        Assert.assertNotNull("encrypted string is not null", encrypted);
        Assert.assertFalse("encrypted string is not same as cleartext", encrypted.equals(clearText));
        Assert.assertTrue("encrypted string has a valid encryption type", verifyEncryptionType(encrypted));
        String decrypted = manager.decrypt(encrypted);
        Assert.assertEquals("Same as initial text", clearText, decrypted);
    }

    /**
     * Encrypt and decrypt same text should yield same results.
     */
    @Test
    public void testEncryptSameText() throws GeneralSecurityException, IOException {
        final IEncryptionManager manager = getManager();

        String clearText = "AAAAAAAA2pILN0mn3wlYIlWk7lqOZ5qjRWXHRnqDdzsq0s4aaUVgnMQo6oXfEUYL4fAxqVQ6dXh9sMAieFDjXVhTkp3mnL2gHSnAHJFwmj9mnlgaU7kVcoujXRA3Je23PEtoqEQMQPaurakVcEl7jOsjUGWD7JdaAHsYTujd1KHoTUdBJQQ-jz4t6Cish25zn9BPocJzN56rLUqgX3dnoA1z-hY4FS_EIn_Xdvqnil29t4etVHLDZD5RJbc5R3p5MaUKqPBF8sAQvJcgW-f9ebPHzO8L87RrsVNu4keagKmOnP139KSuORBhNaD57nmEvecJWtWTIAA&redirect_uri=https%3a%2f%2fworkaad.com%2fdemoclient1&client_id=dba19db4-53de-441d-9c63-da8d6f229e5a";
        String encrypted = manager.encrypt(clearText);
        String encrypted2 = manager.encrypt(clearText);
        String encrypted3 = manager.encrypt(clearText);

        Assert.assertNotNull("encrypted string is not null", encrypted);
        Assert.assertFalse("encrypted string is not same as cleartext", encrypted.equals(clearText));
        Assert.assertFalse("encrypted string is not same as another encrypted call",
                encrypted.equals(encrypted2));
        Assert.assertFalse("encrypted string is not same as another encrypted call",
                encrypted.equals(encrypted3));

        Assert.assertTrue("encrypted string has a valid encryption type", verifyEncryptionType(encrypted));
        Assert.assertTrue("encrypted string has a valid encryption type", verifyEncryptionType(encrypted2));
        Assert.assertTrue("encrypted string has a valid encryption type", verifyEncryptionType(encrypted3));

        String decrypted = manager.decrypt(encrypted);
        String decrypted2 = manager.decrypt(encrypted);
        String decrypted3 = manager.decrypt(encrypted);
        Assert.assertEquals("Same as initial text", clearText, decrypted);
        Assert.assertEquals("Same as initial text", decrypted, decrypted2);
        Assert.assertEquals("Same as initial text", decrypted, decrypted3);
    }

    /**
     * Tampered encrypted string should not be decrypt-able.
     */
    @Test
    public void testTampering() throws GeneralSecurityException, IOException {
        final IEncryptionManager manager = getManager();

        String clearText = "AAAAAAAA2pILN0mn3wlYIlWk7lqOZ5qjRWXH";
        String encrypted = manager.encrypt(clearText);
        Assert.assertNotNull("encrypted string is not null", encrypted);
        Assert.assertFalse("encrypted string is not same as cleartext", encrypted.equals(clearText));
        Assert.assertTrue("encrypted string has a valid encryption type", verifyEncryptionType(encrypted));

        String decrypted = manager.decrypt(encrypted);
        Assert.assertTrue("Same without Tampering", decrypted.equals(clearText));
        final String flagVersion = encrypted.substring(0, 3);
        final byte[] bytes = Base64.decode(encrypted.substring(3), Base64.DEFAULT);
        final int randomlyChosenByte = 15;
        bytes[randomlyChosenByte]++;
        final String modified = new String(Base64.encode(bytes, Base64.NO_WRAP), "UTF-8");
        assertThrowsException(GeneralSecurityException.class, null, new ThrowableRunnable() {
            @Override
            public void run() throws Exception {
                manager.decrypt(flagVersion + modified);
            }
        });
    }

    public void assertThrowsException(final Class<? extends Exception> expected, String hasMessage,
                                      final ThrowableRunnable testCode) {
        try {
            testCode.run();
            junit.framework.Assert.fail("This is expecting an exception, but it was not thrown.");
        } catch (final Throwable result) {
            if (!expected.isInstance(result)) {
                junit.framework.Assert.fail("Exception was not correct");
            }

            if (hasMessage != null && !hasMessage.isEmpty()) {
                assertTrue("Message has the text " + result.getMessage(),
                        (result.getMessage().toLowerCase(Locale.US)
                                .contains(hasMessage.toLowerCase(Locale.US))));
            }
        }
    }

    public interface ThrowableRunnable {
        void run() throws Exception;
    }
}