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
package com.microsoft.identity.common.adal.internal.cache;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.microsoft.identity.common.adal.internal.AndroidSecretKeyEnabledHelper;
import com.microsoft.identity.common.adal.internal.AndroidTestHelper;
import com.microsoft.identity.common.adal.internal.AuthenticationConstants;
import com.microsoft.identity.common.adal.internal.AuthenticationSettings;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import static androidx.test.InstrumentationRegistry.getInstrumentation;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class StorageHelperTests extends AndroidSecretKeyEnabledHelper {

    private static final String TAG = "StorageHelperTests";

    private static final int MIN_SDK_VERSION = 18;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

        // Everything is on clean slate.
        final Context context = getInstrumentation().getTargetContext();
        final StorageHelper storageHelper = new StorageHelper(context);
        storageHelper.deleteKeyFile();
        storageHelper.resetKeyPairFromAndroidKeyStore();

        AuthenticationSettings.INSTANCE.clearSecretKeysForTestCases();
    }

    @Test
    public void testEncryptDecrypt() throws GeneralSecurityException, IOException {
        String clearText = "SomeValue1234";
        encryptDecrypt(clearText);
    }

    @Test
    public void testEncryptDecryptNullEmpty() {

        final Context context = getInstrumentation().getTargetContext();
        final StorageHelper storageHelper = new StorageHelper(context);
        assertThrowsException(
                IllegalArgumentException.class,
                "Input is empty or null",
                new AndroidTestHelper.ThrowableRunnable() {
                    @Override
                    public void run() throws GeneralSecurityException, IOException {
                        storageHelper.encrypt(null);
                    }
                });
        assertThrowsException(
                IllegalArgumentException.class,
                "Input is empty or null",
                new AndroidTestHelper.ThrowableRunnable() {
                    @Override
                    public void run() throws GeneralSecurityException, IOException {
                        storageHelper.encrypt("");
                    }
                });

        assertThrowsException(
                IllegalArgumentException.class,
                "Input is empty or null",
                new AndroidTestHelper.ThrowableRunnable() {
                    @Override
                    public void run() throws GeneralSecurityException, IOException {
                        storageHelper.decrypt(null);
                    }
                });

        assertThrowsException(
                IllegalArgumentException.class,
                "Input is empty or null",
                new AndroidTestHelper.ThrowableRunnable() {
                    @Override
                    public void run() throws GeneralSecurityException, IOException {
                        storageHelper.decrypt("");
                    }
                });
    }

    /**
     * test different size messages
     */
    @Test
    public void testEncryptDecryptDifferentSizes() throws GeneralSecurityException, IOException {
        Log.d(TAG, "Starting testEncryptDecrypt_differentSizes");
        // try different block sizes
        final int sizeRange = 1000;
        StringBuilder buf = new StringBuilder(sizeRange);
        for (int i = 0; i < sizeRange; i+=20) {
            encryptDecrypt(buf.append("a").toString());
        }
        Log.d(TAG, "Finished testEncryptDecrypt_differentSizes");
    }

    private void encryptDecrypt(String clearText) throws GeneralSecurityException, IOException {
        Context context = getInstrumentation().getTargetContext();
        final StorageHelper storageHelper = new StorageHelper(context);
        String encrypted = storageHelper.encrypt(clearText);
        assertNotNull("encrypted string is not null", encrypted);
        assertFalse("encrypted string is not same as cleartext", encrypted.equals(clearText));
        String decrypted = storageHelper.decrypt(encrypted);
        assertEquals("Same as initial text", clearText, decrypted);
    }

    @Test
    public void testEncryptSameText() throws GeneralSecurityException, IOException {
        // access code
        Context context = getInstrumentation().getTargetContext();
        final StorageHelper storageHelper = new StorageHelper(context);
        String clearText = "AAAAAAAA2pILN0mn3wlYIlWk7lqOZ5qjRWXHRnqDdzsq0s4aaUVgnMQo6oXfEUYL4fAxqVQ6dXh9sMAieFDjXVhTkp3mnL2gHSnAHJFwmj9mnlgaU7kVcoujXRA3Je23PEtoqEQMQPaurakVcEl7jOsjUGWD7JdaAHsYTujd1KHoTUdBJQQ-jz4t6Cish25zn9BPocJzN56rLUqgX3dnoA1z-hY4FS_EIn_Xdvqnil29t4etVHLDZD5RJbc5R3p5MaUKqPBF8sAQvJcgW-f9ebPHzO8L87RrsVNu4keagKmOnP139KSuORBhNaD57nmEvecJWtWTIAA&redirect_uri=https%3a%2f%2fworkaad.com%2fdemoclient1&client_id=dba19db4-53de-441d-9c63-da8d6f229e5a";
        Log.d(TAG, "Starting testEncryptSameText");
        String encrypted = storageHelper.encrypt(clearText);
        String encrypted2 = storageHelper.encrypt(clearText);
        String encrypted3 = storageHelper.encrypt(clearText);

        assertNotNull("encrypted string is not null", encrypted);
        assertFalse("encrypted string is not same as cleartext", encrypted.equals(clearText));
        assertFalse("encrypted string is not same as another encrypted call",
                encrypted.equals(encrypted2));
        assertFalse("encrypted string is not same as another encrypted call",
                encrypted.equals(encrypted3));

        String decrypted = storageHelper.decrypt(encrypted);
        String decrypted2 = storageHelper.decrypt(encrypted);
        String decrypted3 = storageHelper.decrypt(encrypted);
        assertEquals("Same as initial text", clearText, decrypted);
        assertEquals("Same as initial text", decrypted, decrypted2);
        assertEquals("Same as initial text", decrypted, decrypted3);
        Log.d(TAG, "Finished testEncryptSameText");
    }

    @Test
    public void testKeyThumbprint() throws GeneralSecurityException, IOException {
        Context context = getInstrumentation().getTargetContext();
        final StorageHelper storageHelper = new StorageHelper(context);
        Assert.assertEquals(storageHelper.testThumbprint(), storageHelper.testThumbprint());
    }

    @Test
    public void testKeyChange() throws GeneralSecurityException, IOException {
        Context context = getInstrumentation().getTargetContext();
        StorageHelper.LAST_KNOWN_THUMBPRINT.set("");
        StorageHelper.FIRST_TIME.set(true);
        final StorageHelper storageHelper = new StorageHelper(context);
        Assert.assertTrue(storageHelper.testKeyChange());
        for (int i = 0; i < 500; i++) {
            Assert.assertFalse("None of these things should change the key", storageHelper.testKeyChange());
        }
        StorageHelper.LAST_KNOWN_THUMBPRINT.set("foo");
        Assert.assertTrue("We altered the key here", storageHelper.testKeyChange());
        for (int i = 0; i < 500; i++) {
            Assert.assertFalse("The key should remain unchanged", storageHelper.testKeyChange());
        }
    }


    @Test
    public void testTampering() throws GeneralSecurityException, IOException {
        final Context context = getInstrumentation().getTargetContext();
        final StorageHelper storageHelper = new StorageHelper(context);
        String clearText = "AAAAAAAA2pILN0mn3wlYIlWk7lqOZ5qjRWXH";
        String encrypted = storageHelper.encrypt(clearText);
        assertNotNull("encrypted string is not null", encrypted);
        assertFalse("encrypted string is not same as cleartext", encrypted.equals(clearText));

        String decrypted = storageHelper.decrypt(encrypted);
        assertTrue("Same without Tampering", decrypted.equals(clearText));
        final String flagVersion = encrypted.substring(0, 3);
        final byte[] bytes = Base64.decode(encrypted.substring(3), Base64.DEFAULT);
        final int randomlyChosenByte = 15;
        bytes[randomlyChosenByte]++;
        final String modified = new String(Base64.encode(bytes, Base64.NO_WRAP), "UTF-8");
        assertThrowsException(GeneralSecurityException.class, null, new ThrowableRunnable() {
            @Override
            public void run() throws Exception {
                storageHelper.decrypt(flagVersion + modified);
            }
        });
    }

    /**
     * Make sure that version sets correctly. It needs to be tested at different
     * emulator(18 and before 18).
     */
    @Test
    public void testVersion() throws GeneralSecurityException, IOException {

        final Context context = getInstrumentation().getTargetContext();
        final StorageHelper storageHelper = new StorageHelper(context);
        String value = "anvaERSgvhdfgkhrebgagagfdgadfgaadfgadfgadfg435gerhawdeADFGb #$%#gf3$%1234";
        String encrypted = storageHelper.encrypt(value);
        final int knownEncryptedSubstringStart = 1;
        final int knownEncryptedSubstringEnd = 3;
        String encodeVersion = encrypted.substring(knownEncryptedSubstringStart, knownEncryptedSubstringEnd);
        assertEquals("Encode version is same", "E1", encodeVersion);
        final byte[] bytes = Base64.decode(encrypted.substring(3), Base64.DEFAULT);

        // get key version used for this data. If user upgraded to different
        // API level, data needs to be updated
        final int keyVersionLength = 4;
        String keyVersionCheck = new String(bytes, 0, keyVersionLength, "UTF-8");
        Log.v(TAG, "Key version check:" + keyVersionCheck);
        if (Build.VERSION.SDK_INT < MIN_SDK_VERSION || AuthenticationSettings.INSTANCE.getSecretKeyData() != null) {
            assertEquals("It should use user defined", "U001", keyVersionCheck);
        } else {
            assertEquals("It should use user defined", "A001", keyVersionCheck);
        }
    }

    @TargetApi(MIN_SDK_VERSION)
    @Test
    public void testKeyPairAndroidKeyStore() throws
            GeneralSecurityException, IOException {
        if (Build.VERSION.SDK_INT < MIN_SDK_VERSION) {
            return;
        }
        final Context context = getInstrumentation().getTargetContext();
        final StorageHelper storageHelper = new StorageHelper(context);
        SecretKey kp = storageHelper.generateKeyStoreEncryptedKey();

        assertNotNull("Keypair is not null", kp);

        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);
        assertTrue("Keystore has the alias", keyStore.containsAlias("AdalKey"));
    }

    @TargetApi(MIN_SDK_VERSION)
    @Test
    public void testMigration() throws
            GeneralSecurityException, IOException {
        if (Build.VERSION.SDK_INT < MIN_SDK_VERSION) {
            return;
        }

        final Context context = getInstrumentation().getTargetContext();
        final StorageHelper storageHelper = new StorageHelper(context);
        setSecretKeyData();
        String expectedDecrypted = "SomeValue1234";
        String encryptedInAPI17 = "cE1VTAwMb4ChefrTHHblCg0DYaK1UR456nW3q6+hqA9Cs2uB+bqcfsLzukiI+KOCdBGJV+JqhRJHBIDCOl68TYkLQAz65g=";
        String decrypted = storageHelper.decrypt(encryptedInAPI17);
        assertEquals("Expected clear text as same", expectedDecrypted, decrypted);
    }

    @TargetApi(MIN_SDK_VERSION)
    @Test
    public void testGetSecretKeyFromAndroidKeyStore() throws IOException, GeneralSecurityException {

        if (Build.VERSION.SDK_INT < MIN_SDK_VERSION) {
            return;
        }

        final Context context = getInstrumentation().getTargetContext();
        final StorageHelper storageHelper = new StorageHelper(context);

        File keyFile = new File(context.getDir(context.getPackageName(),
                Context.MODE_PRIVATE), "adalks");
        if (keyFile.exists()) {
            keyFile.delete();
        }

        SecretKey key = storageHelper.loadSecretKeyForEncryption();
        assertNotNull("Key is not null", key);

        SecretKey key2 = storageHelper.loadSecretKeyForEncryption();
        Log.d(TAG, "Key1:" + key.toString());
        Log.d(TAG, "Key1:" + key2.toString());
        assertTrue("Key info is same", key.toString().equals(key2.toString()));
    }

    private void setMockBrokerSecretKeys() throws NoSuchAlgorithmException, UnsupportedEncodingException, InvalidKeySpecException {
        final Map<String, byte[]> secretKeys = new HashMap<String, byte[]>(2);
        final SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("PBEWithSHA256And256BitAES-CBC-BC");

        final SecretKey authAppTempkey = keyFactory.generateSecret(new PBEKeySpec("mock-password".toCharArray(), "AZURE_AUTHENTICATOR_APP_SALT".getBytes("UTF-8"), 100, 256));
        final SecretKey authAppSecretKey = new SecretKeySpec(authAppTempkey.getEncoded(), "AES");
        secretKeys.put(AuthenticationConstants.Broker.AZURE_AUTHENTICATOR_APP_PACKAGE_NAME, authAppSecretKey.getEncoded());

        final SecretKey cpTempkey = keyFactory.generateSecret(new PBEKeySpec("mock-password".toCharArray(), "COMPANY_PORTAL_APP_SALT".getBytes("UTF-8"), 100, 256));
        final SecretKey cpSecretKey = new SecretKeySpec(cpTempkey.getEncoded(), "AES");
        secretKeys.put(AuthenticationConstants.Broker.COMPANY_PORTAL_APP_PACKAGE_NAME, cpSecretKey.getEncoded());

        AuthenticationSettings.INSTANCE.setBrokerSecretKeys(secretKeys);
    }

    // Encrypt with legacy key, then try decrypting. The decryption code should be smart enough to figure that out.
    @Test
    public void testDecryptingLegacyAuthAppKey() throws GeneralSecurityException, IOException {

        setMockBrokerSecretKeys();

        class LegacyStorageHelperMock extends StorageHelper {
            public LegacyStorageHelperMock(@NonNull Context context) {
                super(context);
            }

            @Override
            protected String getPackageName() {
                return AuthenticationConstants.Broker.AZURE_AUTHENTICATOR_APP_PACKAGE_NAME;
            }

            @Override
            protected boolean isBrokerProcess() {
                return true;
            }
        }

        final Context context = getInstrumentation().getTargetContext();
        final LegacyStorageHelperMock legacyStorageHelperMock = new LegacyStorageHelperMock(context);

        final String authAppKey = Base64.encodeToString(new SecretKeySpec(AuthenticationSettings.INSTANCE.getBrokerSecretKeys().get(AuthenticationConstants.Broker.AZURE_AUTHENTICATOR_APP_PACKAGE_NAME), "AES").getEncoded(), Base64.DEFAULT);
        final String encryptionKey = Base64.encodeToString(legacyStorageHelperMock.loadSecretKeyForEncryption().getEncoded(), Base64.DEFAULT);
        assertTrue("AuthApp key is used for encryption.", authAppKey.equals(encryptionKey));

        String expectedDecrypted = "SomeValue1234";
        assertTrue("Data is not encrypted", legacyStorageHelperMock.getEncryptionType(expectedDecrypted) == StorageHelper.EncryptionType.UNENCRYPTED);

        String legacyEncryptedKey = legacyStorageHelperMock.encrypt(expectedDecrypted);
        assertTrue("Data is encrypted with legacy key", legacyStorageHelperMock.getEncryptionType(legacyEncryptedKey) == StorageHelper.EncryptionType.USER_DEFINED);

        String decryptedLegacyKey = legacyStorageHelperMock.decrypt(legacyEncryptedKey);
        assertTrue("Decrypted data is same", expectedDecrypted.equals(decryptedLegacyKey));
    }

    @Test
    public void testDecryptingLegacyCPKey() throws GeneralSecurityException, IOException {

        setMockBrokerSecretKeys();

        class LegacyStorageHelperMock extends StorageHelper {
            public LegacyStorageHelperMock(@NonNull Context context) {
                super(context);
            }

            @Override
            protected String getPackageName() {
                return AuthenticationConstants.Broker.COMPANY_PORTAL_APP_PACKAGE_NAME;
            }

            @Override
            protected boolean isBrokerProcess() {
                return true;
            }
        }

        final Context context = getInstrumentation().getTargetContext();
        final LegacyStorageHelperMock legacyStorageHelperMock = new LegacyStorageHelperMock(context);

        final String authAppKey = Base64.encodeToString(new SecretKeySpec(AuthenticationSettings.INSTANCE.getBrokerSecretKeys().get(AuthenticationConstants.Broker.COMPANY_PORTAL_APP_PACKAGE_NAME), "AES").getEncoded(), Base64.DEFAULT);
        final String encryptionKey = Base64.encodeToString(legacyStorageHelperMock.loadSecretKeyForEncryption().getEncoded(), Base64.DEFAULT);
        assertTrue("CP key is used for encryption.", authAppKey.equals(encryptionKey));

        String expectedDecrypted = "SomeValue1234";
        assertTrue("Data is not encrypted", legacyStorageHelperMock.getEncryptionType(expectedDecrypted) == StorageHelper.EncryptionType.UNENCRYPTED);

        String legacyEncryptedKey = legacyStorageHelperMock.encrypt(expectedDecrypted);
        assertTrue("Data is encrypted with legacy key", legacyStorageHelperMock.getEncryptionType(legacyEncryptedKey) == StorageHelper.EncryptionType.USER_DEFINED);

        String decryptedLegacyKey = legacyStorageHelperMock.decrypt(legacyEncryptedKey);
        assertTrue("Decrypted data is same", expectedDecrypted.equals(decryptedLegacyKey));
    }

    @Test
    public void testDecryptingWithADALUserDefinedKey() throws IOException, GeneralSecurityException {

        final SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("PBEWithSHA256And256BitAES-CBC-BC");
        final SecretKey tempkey = keyFactory.generateSecret(new PBEKeySpec("mock-password".toCharArray(), "mock-byte-code-for-salt".getBytes("UTF-8"), 100, 256));
        final SecretKey secretKey = new SecretKeySpec(tempkey.getEncoded(), "AES");
        AuthenticationSettings.INSTANCE.setSecretKey(secretKey.getEncoded());

        class StorageHelperMock extends StorageHelper {
            public StorageHelperMock(@NonNull Context context) {
                super(context);
            }

            @Override
            protected String getPackageName() {
                // Simulate the case where CP is doing Local ADAL auth.
                return AuthenticationConstants.Broker.COMPANY_PORTAL_APP_PACKAGE_NAME;
            }
        }

        final Context context = getInstrumentation().getTargetContext();
        final StorageHelperMock storageHelperMock = new StorageHelperMock(context);

        String unencryptedValue = "SomeValue1234";
        String encryptedValue = storageHelperMock.encrypt(unencryptedValue);

        assertTrue("Encrypted with user defined key", storageHelperMock.getEncryptionType(encryptedValue) == StorageHelper.EncryptionType.USER_DEFINED);

        List<StorageHelper.KeyType> keyTypeList = storageHelperMock.getKeysForDecryptionType(encryptedValue, AuthenticationConstants.Broker.COMPANY_PORTAL_APP_PACKAGE_NAME);

        assertTrue("Expected 1 keyType", keyTypeList.size() == 1);
        assertTrue("The first key should be user defined key", keyTypeList.get(0) == StorageHelper.KeyType.ADAL_USER_DEFINED_KEY);

        String decryptedValue = storageHelperMock.decrypt(encryptedValue);

        assertTrue("Decrypted data is same", decryptedValue.equals(unencryptedValue));
    }

    @Test
    public void testDecryptingWithADALKeyStoreKey() throws IOException, GeneralSecurityException {

        final Context context = getInstrumentation().getTargetContext();
        final StorageHelper storageHelper = new StorageHelper(context);

        String unencryptedValue = "SomeValue1234";
        String encryptedValue = storageHelper.encrypt(unencryptedValue);

        assertTrue("Encrypted with keystore key", storageHelper.getEncryptionType(encryptedValue) == StorageHelper.EncryptionType.ANDROID_KEY_STORE);

        List<StorageHelper.KeyType> keyTypeList = storageHelper.getKeysForDecryptionType(encryptedValue, "MOCK_ADAL_APP");

        assertTrue("Expected 1 keyType", keyTypeList.size() == 1);
        assertTrue("The first key should be keystore encrypted key", keyTypeList.get(0) == StorageHelper.KeyType.KEYSTORE_ENCRYPTED_KEY);

        String decryptedValue = storageHelper.decrypt(encryptedValue);

        assertTrue("Decrypted data is same", decryptedValue.equals(unencryptedValue));
    }

    @Test
    public void testSecretKeySerialization() throws UnsupportedEncodingException {
        final Context context = getInstrumentation().getTargetContext();
        final StorageHelper storageHelper = new StorageHelper(context);

        final String keyString = "ABCDEFGH";
        final SecretKey key = new SecretKeySpec(Base64.decode(keyString.getBytes(AuthenticationConstants.CHARSET_UTF8), Base64.DEFAULT), "AES");
        final SecretKey anotherKey = new SecretKeySpec(Base64.decode("RANDOM".getBytes(AuthenticationConstants.CHARSET_UTF8), Base64.DEFAULT), "AES");

        final String serializedKey = storageHelper.serializeSecretKey(key);
        final SecretKey deserializedKey = storageHelper.deserializeSecretKey(serializedKey);

        assertTrue("keys are matching.", deserializedKey.equals(key));
        assertFalse("keys should not be matching.", deserializedKey.equals(anotherKey));
    }
}
