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

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.microsoft.identity.common.internal.util.StringUtil;

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

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

@RunWith(AndroidJUnit4.class)
public class KeystoreEncryptedKeyManagerTest {

    final String keyFileName = "testKeyFile";

    private SecretKey generateMockSecretKey() throws NoSuchAlgorithmException, UnsupportedEncodingException, InvalidKeySpecException {
        final SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("PBEWithSHA256And256BitAES-CBC-BC");
        final SecretKey tempKey = keyFactory.generateSecret(new PBEKeySpec("mock-password".toCharArray(), "MOCK_SALT".getBytes("UTF-8"), 100, 256));
        return new SecretKeySpec(tempKey.getEncoded(), "AES");
    }

    @Before
    public void setUp() throws Exception {
        final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        final File file = new File(context.getDir(context.getPackageName(), Context.MODE_PRIVATE),
                keyFileName);

        if (file.exists()) {
            file.delete();
        }
    }

    @Test
    public void testKeyPairAndroidKeyStore() throws GeneralSecurityException, IOException {
        final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        final KeystoreEncryptedKeyManager manager = new KeystoreEncryptedKeyManager(context, keyFileName);

        manager.createKeyAndSave();

        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);
        Assert.assertTrue("Keystore has the alias", keyStore.containsAlias("AdalKey"));

    }

    @Test
    public void testSaveKey() throws GeneralSecurityException, IOException {
        final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        final KeystoreEncryptedKeyManager manager = new KeystoreEncryptedKeyManager(context, keyFileName);

        final File file = new File(context.getDir(context.getPackageName(), Context.MODE_PRIVATE),
                keyFileName);

        Assert.assertFalse(file.exists());

        manager.saveKey(generateMockSecretKey());

        Assert.assertTrue(file.exists());
    }

    @Test
    public void testReadAndWriteKey() throws GeneralSecurityException, IOException {
        final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        final KeystoreEncryptedKeyManager manager = new KeystoreEncryptedKeyManager(context, keyFileName);

        final SecretKey writtenKey = generateMockSecretKey();
        final String serializedWrittenKey = BaseEncryptionManager.serializeSecretKey(writtenKey);

        manager.saveKey(writtenKey);

        final SecretKey readKey = manager.loadKey();
        final String serializedReadKey = BaseEncryptionManager.serializeSecretKey(readKey);
        Assert.assertTrue(!StringUtil.isEmpty(serializedReadKey));

        Assert.assertTrue(serializedWrittenKey.equals(serializedReadKey));
    }

    @Test
    public void testCreateAndReadKey() throws GeneralSecurityException, IOException {
        final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        final KeystoreEncryptedKeyManager manager = new KeystoreEncryptedKeyManager(context, keyFileName);

        final SecretKey generatedKey = manager.createKeyAndSave();
        final String serializedGeneratedKey = BaseEncryptionManager.serializeSecretKey(generatedKey);
        Assert.assertTrue(!StringUtil.isEmpty(serializedGeneratedKey));

        final SecretKey readKey = manager.loadKey();
        final String serializedReadKey = BaseEncryptionManager.serializeSecretKey(readKey);
        Assert.assertTrue(!StringUtil.isEmpty(serializedReadKey));

        Assert.assertTrue(serializedGeneratedKey.equals(serializedReadKey));
    }

    @Test
    public void testDeleteKey() throws GeneralSecurityException, IOException {
        final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        final KeystoreEncryptedKeyManager manager = new KeystoreEncryptedKeyManager(context, keyFileName);

        final SecretKey writtenKey = generateMockSecretKey();
        manager.saveKey(writtenKey);

        final File file = new File(context.getDir(context.getPackageName(), Context.MODE_PRIVATE),
                keyFileName);

        Assert.assertTrue(file.exists());

        manager.deleteKeyFile();

        Assert.assertFalse(file.exists());
        Assert.assertTrue(manager.loadKey() == null);
        Assert.assertTrue(manager.loadKey() == null);
    }

    @Test
    public void testUpdateKey() throws GeneralSecurityException, IOException {
        final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        final KeystoreEncryptedKeyManager manager = new KeystoreEncryptedKeyManager(context, keyFileName);

        final SecretKey firstKey = generateMockSecretKey();
        final String serializedFirstKey = BaseEncryptionManager.serializeSecretKey(firstKey);

        manager.saveKey(firstKey);

        final String serializedLoadedFirstKey = BaseEncryptionManager.serializeSecretKey(manager.loadKey());
        Assert.assertTrue(serializedLoadedFirstKey.equalsIgnoreCase(serializedFirstKey));

        final SecretKey updateKey = generateMockSecretKey();
        final String serializedUpdateKey = BaseEncryptionManager.serializeSecretKey(updateKey);

        manager.saveKey(updateKey);

        final String serializedLoadedUpdatedKey = BaseEncryptionManager.serializeSecretKey(manager.loadKey());
        Assert.assertTrue(serializedLoadedUpdatedKey.equalsIgnoreCase(serializedUpdateKey));
    }

    @Test
    public void testKeyShouldNotBeCached() throws GeneralSecurityException, IOException {
        final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        final KeystoreEncryptedKeyManager manager = new KeystoreEncryptedKeyManager(context, keyFileName);

        manager.saveKey(generateMockSecretKey());

        final File file = new File(context.getDir(context.getPackageName(), Context.MODE_PRIVATE),
                keyFileName);

        Assert.assertTrue(file.exists());

        file.delete();

        Assert.assertTrue(manager.loadKey() == null);
    }

}
