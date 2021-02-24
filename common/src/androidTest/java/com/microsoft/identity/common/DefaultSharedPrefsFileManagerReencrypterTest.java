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
package com.microsoft.identity.common;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.test.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.microsoft.identity.common.adal.internal.AuthenticationSettings;
import com.microsoft.identity.common.adal.internal.cache.StorageHelper;
import com.microsoft.identity.common.internal.cache.DefaultSharedPrefsFileManagerReencrypter;
import com.microsoft.identity.common.internal.cache.ISharedPreferencesFileManager;
import com.microsoft.identity.common.internal.cache.ISharedPrefsFileManagerReencrypter;
import com.microsoft.identity.common.internal.cache.SharedPreferencesFileManager;
import com.microsoft.identity.common.internal.controllers.TaskCompletedCallbackWithError;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

@RunWith(AndroidJUnit4.class)
public class DefaultSharedPrefsFileManagerReencrypterTest {

    private static final String TEST_CACHE_FILENAME = "com.msft.test-sharedprefs";
    private static final int MAX_WAIT = 5;
    private static final TimeUnit MAX_WAIT_UNIT = TimeUnit.SECONDS;

    private Context mContext;

    private ISharedPreferencesFileManager mTestCacheFile;

    private ISharedPrefsFileManagerReencrypter mFileManagerReencrypter;
    private TestEncrypterDecrypter mTestEncrypterDecrypter;
    private ISharedPrefsFileManagerReencrypter.IStringEncrypter mStringEncrypter;
    private ISharedPrefsFileManagerReencrypter.IStringDecrypter mStringDecrypter;

    private class TestEncrypterDecrypter implements
            ISharedPrefsFileManagerReencrypter.IStringEncrypter,
            ISharedPrefsFileManagerReencrypter.IStringDecrypter {

        private final Context mContext;

        // Decryption will use the legacy key
        private final byte[] mMockLegacyKey;

        TestEncrypterDecrypter(@NonNull final Context context,
                               @NonNull final byte[] secretKeyData) {
            mContext = context;
            mMockLegacyKey = secretKeyData;
        }

        @Override
        public String encrypt(@NonNull final String input) throws Exception {
            // Ensure the global keys are cleared, so we don't use them...
            AuthenticationSettings.INSTANCE.clearSecretKeysForTestCases();
            final StorageHelper storageHelper = new StorageHelper(mContext);
            return storageHelper.encrypt(input);
        }

        public String encryptWithLegacyKey(@NonNull final String input) throws Exception {
            try {
                AuthenticationSettings.INSTANCE.setSecretKey(mMockLegacyKey);
                final StorageHelper storageHelper = new StorageHelper(mContext);
                return storageHelper.encrypt(input);
            } finally {
                AuthenticationSettings.INSTANCE.clearSecretKeysForTestCases();
            }
        }

        @Override
        public String decrypt(@NonNull final String input) throws Exception {
            try {
                // This is a workaround for some really clunky global state management
                AuthenticationSettings.INSTANCE.setSecretKey(mMockLegacyKey);
                final StorageHelper storageHelper = new StorageHelper(mContext);
                return storageHelper.decrypt(input);
            } finally {
                // TODO You may need to rename this API! Not just tests anymore!
                AuthenticationSettings.INSTANCE.clearSecretKeysForTestCases();
            }
        }
    }

    @Before
    public void setUp() {
        mContext = InstrumentationRegistry.getTargetContext();
        mTestCacheFile = new SharedPreferencesFileManager(
                mContext,
                TEST_CACHE_FILENAME
        );
        mFileManagerReencrypter = new DefaultSharedPrefsFileManagerReencrypter();
        try {
            final byte[] mockKey = generateLegacyFormatKey();
            mTestEncrypterDecrypter = new TestEncrypterDecrypter(mContext, mockKey);
            mStringEncrypter = mTestEncrypterDecrypter;
            mStringDecrypter = mTestEncrypterDecrypter;
        } catch (NoSuchAlgorithmException
                | UnsupportedEncodingException
                | InvalidKeySpecException e) {
            e.printStackTrace();
        }
    }

    private byte[] generateLegacyFormatKey()
            throws NoSuchAlgorithmException, UnsupportedEncodingException, InvalidKeySpecException {
        SecretKeyFactory keyFactory = SecretKeyFactory
                .getInstance("PBEWithSHA256And256BitAES-CBC-BC");
        final int iterations = 100;
        final int keySize = 256;
        final SecretKey tempkey = keyFactory.generateSecret(new PBEKeySpec("test".toCharArray(),
                "abcdedfdfd".getBytes("UTF-8"), iterations, keySize));
        final SecretKey secretKey = new SecretKeySpec(tempkey.getEncoded(), "AES");
        return secretKey.getEncoded();
    }

    @After
    public void tearDown() {
        mTestCacheFile.clear();
        AuthenticationSettings.INSTANCE.clearSecretKeysForTestCases();
    }

    @Test
    public void testEmptyCacheReturnsSuccess() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);

        mFileManagerReencrypter.reencryptAsync(
                mTestCacheFile,
                mStringEncrypter,
                mStringDecrypter,
                new ISharedPrefsFileManagerReencrypter.ReencryptionParams(
                        true,
                        false,
                        false
                ),
                new TaskCompletedCallbackWithError<Void, Exception>() {
                    @Override
                    public void onError(Exception e) {
                        e.printStackTrace();
                        Assert.fail();
                        latch.countDown();
                    }

                    @Override
                    public void onTaskCompleted(Void aVoid) {
                        latch.countDown();
                    }
                }
        );

        latch.await(MAX_WAIT, MAX_WAIT_UNIT);
    }

    @Test
    public void testReencryptionSimple() throws Exception {
        // Add the numbers 0-9 to the cache, encrypted format...
        for (int ii = 0; ii < 10; ii++) {
            try {
                final String encryptedIntStr = mTestEncrypterDecrypter.encryptWithLegacyKey(String.valueOf(ii));
                mTestCacheFile.putString(String.valueOf(ii), encryptedIntStr);
            } catch (Exception e) {
                // wont happen
            }
        }

        Assert.assertEquals(10, mTestCacheFile.getAll().size());

        // Ensure that the 'legacy' and new keys do not create the same value...
        final String legacyEncryptedZero = mTestCacheFile.getString("0");
        final String newEncryptedZero = mTestEncrypterDecrypter.encrypt("0");

        Assert.assertNotEquals(legacyEncryptedZero, newEncryptedZero);

        // Reencrypt the cache
        final CountDownLatch latch = new CountDownLatch(1);

        mFileManagerReencrypter.reencryptAsync(
                mTestCacheFile,
                mStringEncrypter,
                mStringDecrypter,
                new ISharedPrefsFileManagerReencrypter.ReencryptionParams(
                        true,
                        false,
                        false
                ),
                new TaskCompletedCallbackWithError<Void, Exception>() {
                    @Override
                    public void onError(Exception e) {
                        e.printStackTrace();
                        Assert.fail();
                        latch.countDown();
                    }

                    @Override
                    public void onTaskCompleted(Void aVoid) {
                        latch.countDown();
                    }
                }
        );

        latch.await(MAX_WAIT, MAX_WAIT_UNIT);

        Assert.assertEquals(10, mTestCacheFile.getAll().size());
    }

    @Test
    public void testAbortOnError() throws InterruptedException {
        final String sampleKey = "sample_key";
        final String sampleValue = "plaintext_value";
        mTestCacheFile.putString(sampleKey, sampleValue);

        // Try to decrypt the unencrypted data, it will fail
        final CountDownLatch latch = new CountDownLatch(1);

        mFileManagerReencrypter.reencryptAsync(
                mTestCacheFile,
                mStringEncrypter,
                new ISharedPrefsFileManagerReencrypter.IStringDecrypter() {
                    @Override
                    public String decrypt(String input) throws Exception {
                        throw new IOException();
                    }
                },
                new ISharedPrefsFileManagerReencrypter.ReencryptionParams(
                        true,
                        false,
                        false
                ),
                new TaskCompletedCallbackWithError<Void, Exception>() {
                    @Override
                    public void onError(Exception error) {
                        latch.countDown();
                    }

                    @Override
                    public void onTaskCompleted(Void aVoid) {
                        Assert.fail();
                        latch.countDown();
                    }
                }
        );

        latch.await(MAX_WAIT, MAX_WAIT_UNIT);
        Assert.assertEquals(1, mTestCacheFile.getAll().size());
        Assert.assertEquals(sampleValue, mTestCacheFile.getString(sampleKey));
    }

    @Test
    public void testEraseEntryOnError() throws InterruptedException {
        final String sampleKey = "sample_key";
        final String sampleValue = "plaintext_value";
        mTestCacheFile.putString(sampleKey, sampleValue);

        // Try to decrypt the unencrypted data, it will fail
        final CountDownLatch latch = new CountDownLatch(1);

        mFileManagerReencrypter.reencryptAsync(
                mTestCacheFile,
                mStringEncrypter,
                new ISharedPrefsFileManagerReencrypter.IStringDecrypter() {
                    @Override
                    public String decrypt(String input) throws Exception {
                        throw new IOException();
                    }
                },
                new ISharedPrefsFileManagerReencrypter.ReencryptionParams(
                        false,
                        true,
                        false
                ),
                new TaskCompletedCallbackWithError<Void, Exception>() {
                    @Override
                    public void onError(Exception error) {
                        Assert.fail();
                        latch.countDown();
                    }

                    @Override
                    public void onTaskCompleted(Void aVoid) {
                        latch.countDown();
                    }
                }
        );

        latch.await(MAX_WAIT, MAX_WAIT_UNIT);
        Assert.assertEquals(0, mTestCacheFile.getAll().size());
    }

    @Test
    public void testEraseAllOnError() throws InterruptedException {
        final String sampleKey1 = "sample_key_1";
        final String sampleValue1 = "plaintext_value_1";

        final String sampleKey2 = "sample_key_2";
        final String sampleValue2 = "plaintext_value_2";

        mTestCacheFile.putString(sampleKey1, sampleValue1);
        mTestCacheFile.putString(sampleKey2, sampleValue2);

        // Try to decrypt the unencrypted data, it will fail
        final CountDownLatch latch = new CountDownLatch(1);

        mFileManagerReencrypter.reencryptAsync(
                mTestCacheFile,
                mStringEncrypter,
                new ISharedPrefsFileManagerReencrypter.IStringDecrypter() {
                    @Override
                    public String decrypt(String input) throws Exception {
                        throw new IOException();
                    }
                },
                new ISharedPrefsFileManagerReencrypter.ReencryptionParams(
                        false,
                        true,
                        false
                ),
                new TaskCompletedCallbackWithError<Void, Exception>() {
                    @Override
                    public void onError(Exception error) {
                        Assert.fail();
                        latch.countDown();
                    }

                    @Override
                    public void onTaskCompleted(Void aVoid) {
                        latch.countDown();
                    }
                }
        );

        latch.await(MAX_WAIT, MAX_WAIT_UNIT);
        Assert.assertEquals(0, mTestCacheFile.getAll().size());
    }

}
