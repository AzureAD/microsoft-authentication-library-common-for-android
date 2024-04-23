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
import com.microsoft.identity.common.components.AndroidPlatformComponentsFactory;
import com.microsoft.identity.common.crypto.AndroidAuthSdkStorageEncryptionManager;
import com.microsoft.identity.common.java.crypto.IKeyAccessorStringAdapter;
import com.microsoft.identity.common.java.crypto.KeyAccessorStringAdapter;
import com.microsoft.identity.common.java.interfaces.INameValueStorage;
import com.microsoft.identity.common.java.util.TaskCompletedCallback;
import com.microsoft.identity.common.migration.DefaultMultiTypeNameValueStorageReencrypter;
import com.microsoft.identity.common.migration.IMigrationOperationResult;
import com.microsoft.identity.common.migration.IMultiTypeNameValueStorageReencrypter;

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

    private INameValueStorage<String> mTestCacheFile;

    private IMultiTypeNameValueStorageReencrypter mFileManagerReencrypter;
    private TestEncrypterDecrypter mTestEncrypterDecrypter;
    private IMultiTypeNameValueStorageReencrypter.IStringEncrypter mStringEncrypter;
    private IMultiTypeNameValueStorageReencrypter.IStringDecrypter mStringDecrypter;

    private class TestEncrypterDecrypter implements
            IMultiTypeNameValueStorageReencrypter.IStringEncrypter,
            IMultiTypeNameValueStorageReencrypter.IStringDecrypter {

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
            final IKeyAccessorStringAdapter encryptionManager =
                    new KeyAccessorStringAdapter(
                            new AndroidAuthSdkStorageEncryptionManager(mContext));
            return encryptionManager.encrypt(input);
        }

        public String encryptWithLegacyKey(@NonNull final String input) throws Exception {
            try {
                AuthenticationSettings.INSTANCE.setSecretKey(mMockLegacyKey);
                final IKeyAccessorStringAdapter encryptionManager =
                        new KeyAccessorStringAdapter(
                                new AndroidAuthSdkStorageEncryptionManager(mContext));
                return encryptionManager.encrypt(input);
            } finally {
                AuthenticationSettings.INSTANCE.clearSecretKeysForTestCases();
            }
        }

        @Override
        public String decrypt(@NonNull final String input) throws Exception {
            try {
                AuthenticationSettings.INSTANCE.setSecretKey(mMockLegacyKey);
                final IKeyAccessorStringAdapter encryptionManager =
                        new KeyAccessorStringAdapter(
                                new AndroidAuthSdkStorageEncryptionManager(mContext));
                return encryptionManager.decrypt(input);
            } finally {
                // TODO You may need to rename this API! Not just tests anymore!
                AuthenticationSettings.INSTANCE.clearSecretKeysForTestCases();
            }
        }
    }

    @Before
    public void setUp() {
        mContext = InstrumentationRegistry.getTargetContext();
        mTestCacheFile = AndroidPlatformComponentsFactory.createFromContext(mContext)
                .getStorageSupplier()
                .getUnencryptedNameValueStore(TEST_CACHE_FILENAME, String.class);
        mFileManagerReencrypter = new DefaultMultiTypeNameValueStorageReencrypter();
        try {
            final byte[] mockKey = generateLegacyFormatKey("abcdedfdfd");
            mTestEncrypterDecrypter = new TestEncrypterDecrypter(mContext, mockKey);
            mStringEncrypter = mTestEncrypterDecrypter;
            mStringDecrypter = mTestEncrypterDecrypter;
        } catch (NoSuchAlgorithmException
                | UnsupportedEncodingException
                | InvalidKeySpecException e) {
            e.printStackTrace();
        }
    }

    private byte[] generateLegacyFormatKey(@NonNull final String salt)
            throws NoSuchAlgorithmException, UnsupportedEncodingException, InvalidKeySpecException {
        SecretKeyFactory keyFactory = SecretKeyFactory
                .getInstance("PBEWithSHA256And256BitAES-CBC-BC");
        final int iterations = 100;
        final int keySize = 256;
        final SecretKey tempkey = keyFactory.generateSecret(new PBEKeySpec("test".toCharArray(),
                salt.getBytes("UTF-8"), iterations, keySize));
        final SecretKey secretKey = new SecretKeySpec(tempkey.getEncoded(), "AES");
        return secretKey.getEncoded();
    }

    @After
    public void tearDown() {
        mTestCacheFile.clear();
        AuthenticationSettings.INSTANCE.clearSecretKeysForTestCases();
    }

    @Test
    public void testEmptyCacheReturnsSuccess() throws Exception {
        mFileManagerReencrypter.reencrypt(
                mTestCacheFile,
                mStringEncrypter,
                mStringDecrypter,
                new IMultiTypeNameValueStorageReencrypter.ReencryptionParams(
                        true,
                        false,
                        false
                )
        );
    }

    @Test
    public void testEmptyCacheReturnsSuccessAsync() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);

        mFileManagerReencrypter.reencryptAsync(
                mTestCacheFile,
                mStringEncrypter,
                mStringDecrypter,
                new IMultiTypeNameValueStorageReencrypter.ReencryptionParams(
                        true,
                        false,
                        false
                ),
                new TaskCompletedCallback<IMigrationOperationResult>() {
                    @Override
                    public void onTaskCompleted(final IMigrationOperationResult iMigrationOperationResult) {
                        Assert.assertEquals(0, iMigrationOperationResult.getCountOfTotalRecords());
                        Assert.assertEquals(0, iMigrationOperationResult.getCountOfFailedRecords());
                        Assert.assertEquals(0, iMigrationOperationResult.getFailures().size());
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
                mTestCacheFile.put(String.valueOf(ii), encryptedIntStr);
            } catch (Exception e) {
                Assert.assertNull("Should not throw", e);
            }
        }

        Assert.assertEquals(10, mTestCacheFile.getAll().size());

        // Ensure that the 'legacy' and new keys do not create the same value...
        final String legacyEncryptedZero = mTestCacheFile.get("0");
        final String newEncryptedZero = mTestEncrypterDecrypter.encrypt("0");

        Assert.assertNotEquals(legacyEncryptedZero, newEncryptedZero);

        // Reencrypt the cache

        mFileManagerReencrypter.reencrypt(
                mTestCacheFile,
                mStringEncrypter,
                mStringDecrypter,
                new IMultiTypeNameValueStorageReencrypter.ReencryptionParams(
                        true,
                        false,
                        false
                )
        );

        Assert.assertEquals(10, mTestCacheFile.getAll().size());
    }

    @Test
    public void testReencryptionSimpleAsync() throws Exception {
        // Add the numbers 0-9 to the cache, encrypted format...
        for (int ii = 0; ii < 10; ii++) {
            try {
                final String encryptedIntStr = mTestEncrypterDecrypter.encryptWithLegacyKey(String.valueOf(ii));
                mTestCacheFile.put(String.valueOf(ii), encryptedIntStr);
            } catch (Exception e) {
                Assert.assertNull("Should not throw", e);
            }
        }

        Assert.assertEquals(10, mTestCacheFile.getAll().size());

        // Ensure that the 'legacy' and new keys do not create the same value...
        final String legacyEncryptedZero = mTestCacheFile.get("0");
        final String newEncryptedZero = mTestEncrypterDecrypter.encrypt("0");

        Assert.assertNotEquals(legacyEncryptedZero, newEncryptedZero);

        // Reencrypt the cache
        final CountDownLatch latch = new CountDownLatch(1);

        mFileManagerReencrypter.reencryptAsync(
                mTestCacheFile,
                mStringEncrypter,
                mStringDecrypter,
                new IMultiTypeNameValueStorageReencrypter.ReencryptionParams(
                        true,
                        false,
                        false
                ),
                new TaskCompletedCallback<IMigrationOperationResult>() {
                    @Override
                    public void onTaskCompleted(IMigrationOperationResult iMigrationOperationResult) {
                        Assert.assertEquals(10, iMigrationOperationResult.getCountOfTotalRecords());
                        Assert.assertEquals(0, iMigrationOperationResult.getCountOfFailedRecords());
                        Assert.assertEquals(0, iMigrationOperationResult.getFailures().size());
                        latch.countDown();
                    }
                }
        );

        latch.await(MAX_WAIT, MAX_WAIT_UNIT);

        Assert.assertEquals(10, mTestCacheFile.getAll().size());
    }

    @Test
    public void testAbortOnError() throws Exception {
        final String sampleKey = "sample_key";
        final String sampleValue = "plaintext_value";
        mTestCacheFile.put(sampleKey, sampleValue);

        // Try to decrypt the unencrypted data, it will fail
        final IMigrationOperationResult result = mFileManagerReencrypter.reencrypt(
                mTestCacheFile,
                mStringEncrypter,
                new IMultiTypeNameValueStorageReencrypter.IStringDecrypter() {
                    @Override
                    public String decrypt(String input) throws Exception {
                        Assert.assertEquals(1, mTestCacheFile.getAll().size());
                        Assert.assertEquals(sampleValue, mTestCacheFile.get(sampleKey));
                        throw new IOException();
                    }
                },
                new IMultiTypeNameValueStorageReencrypter.ReencryptionParams(
                        true,
                        false,
                        false
                )
        );

        Assert.assertEquals(1, result.getCountOfTotalRecords());
        Assert.assertEquals(1, result.getCountOfFailedRecords());
        Assert.assertEquals(1, result.getFailures().size());
    }

    @Test
    public void testAbortOnErrorAsync() throws InterruptedException {
        final String sampleKey = "sample_key";
        final String sampleValue = "plaintext_value";
        mTestCacheFile.put(sampleKey, sampleValue);

        // Try to decrypt the unencrypted data, it will fail
        final CountDownLatch latch = new CountDownLatch(1);

        mFileManagerReencrypter.reencryptAsync(
                mTestCacheFile,
                mStringEncrypter,
                new IMultiTypeNameValueStorageReencrypter.IStringDecrypter() {
                    @Override
                    public String decrypt(String input) throws Exception {
                        throw new IOException();
                    }
                },
                new IMultiTypeNameValueStorageReencrypter.ReencryptionParams(
                        true,
                        false,
                        false
                ),
                new TaskCompletedCallback<IMigrationOperationResult>() {
                    @Override
                    public void onTaskCompleted(IMigrationOperationResult iMigrationOperationResult) {
                        Assert.assertEquals(1, iMigrationOperationResult.getCountOfTotalRecords());
                        Assert.assertEquals(1, iMigrationOperationResult.getCountOfFailedRecords());
                        Assert.assertEquals(1, iMigrationOperationResult.getFailures().size());
                        latch.countDown();
                    }
                }
        );

        latch.await(MAX_WAIT, MAX_WAIT_UNIT);
        Assert.assertEquals(1, mTestCacheFile.getAll().size());
        Assert.assertEquals(sampleValue, mTestCacheFile.get(sampleKey));
    }

    @Test
    public void testEraseEntryOnError() throws Exception {
        final String sampleKey = "sample_key";
        final String sampleValue = "plaintext_value";
        mTestCacheFile.put(sampleKey, sampleValue);

        // Try to decrypt the unencrypted data, it will fail
        mFileManagerReencrypter.reencrypt(
                mTestCacheFile,
                mStringEncrypter,
                new IMultiTypeNameValueStorageReencrypter.IStringDecrypter() {
                    @Override
                    public String decrypt(String input) throws Exception {
                        throw new IOException();
                    }
                },
                new IMultiTypeNameValueStorageReencrypter.ReencryptionParams(
                        false,
                        true,
                        false
                )
        );
        Assert.assertEquals(0, mTestCacheFile.getAll().size());
    }

    @Test
    public void testEraseEntryOnErrorAsync() throws InterruptedException {
        final String sampleKey = "sample_key";
        final String sampleValue = "plaintext_value";
        mTestCacheFile.put(sampleKey, sampleValue);

        // Try to decrypt the unencrypted data, it will fail
        final CountDownLatch latch = new CountDownLatch(1);

        mFileManagerReencrypter.reencryptAsync(
                mTestCacheFile,
                mStringEncrypter,
                new IMultiTypeNameValueStorageReencrypter.IStringDecrypter() {
                    @Override
                    public String decrypt(String input) throws Exception {
                        throw new IOException();
                    }
                },
                new IMultiTypeNameValueStorageReencrypter.ReencryptionParams(
                        false,
                        true,
                        false
                ),
                new TaskCompletedCallback<IMigrationOperationResult>() {
                    @Override
                    public void onTaskCompleted(IMigrationOperationResult iMigrationOperationResult) {
                        Assert.assertEquals(1, iMigrationOperationResult.getCountOfTotalRecords());
                        Assert.assertEquals(1, iMigrationOperationResult.getCountOfFailedRecords());
                        Assert.assertEquals(1, iMigrationOperationResult.getFailures().size());
                        latch.countDown();
                    }
                }
        );

        latch.await(MAX_WAIT, MAX_WAIT_UNIT);
        Assert.assertEquals(0, mTestCacheFile.getAll().size());
    }

    @Test
    public void testEraseAllOnError() throws Exception {
        final String sampleKey1 = "sample_key_1";
        final String sampleValue1 = "plaintext_value_1";

        final String sampleKey2 = "sample_key_2";
        final String sampleValue2 = "plaintext_value_2";

        mTestCacheFile.put(sampleKey1, sampleValue1);
        mTestCacheFile.put(sampleKey2, sampleValue2);

        // Try to decrypt the unencrypted data, it will fail
        mFileManagerReencrypter.reencrypt(
                mTestCacheFile,
                mStringEncrypter,
                new IMultiTypeNameValueStorageReencrypter.IStringDecrypter() {
                    @Override
                    public String decrypt(String input) throws Exception {
                        throw new IOException();
                    }
                },
                new IMultiTypeNameValueStorageReencrypter.ReencryptionParams(
                        false,
                        true,
                        false
                )
        );

        Assert.assertEquals(0, mTestCacheFile.getAll().size());
    }

    @Test
    public void testEraseAllOnErrorAsync() throws InterruptedException {
        final String sampleKey1 = "sample_key_1";
        final String sampleValue1 = "plaintext_value_1";

        final String sampleKey2 = "sample_key_2";
        final String sampleValue2 = "plaintext_value_2";

        mTestCacheFile.put(sampleKey1, sampleValue1);
        mTestCacheFile.put(sampleKey2, sampleValue2);

        // Try to decrypt the unencrypted data, it will fail
        final CountDownLatch latch = new CountDownLatch(1);

        mFileManagerReencrypter.reencryptAsync(
                mTestCacheFile,
                mStringEncrypter,
                new IMultiTypeNameValueStorageReencrypter.IStringDecrypter() {
                    @Override
                    public String decrypt(String input) throws Exception {
                        throw new IOException();
                    }
                },
                new IMultiTypeNameValueStorageReencrypter.ReencryptionParams(
                        false,
                        true,
                        false
                ),
                new TaskCompletedCallback<IMigrationOperationResult>() {
                    @Override
                    public void onTaskCompleted(IMigrationOperationResult iMigrationOperationResult) {
                        Assert.assertEquals(2, iMigrationOperationResult.getCountOfTotalRecords());
                        Assert.assertEquals(2, iMigrationOperationResult.getCountOfFailedRecords());
                        Assert.assertEquals(1, iMigrationOperationResult.getFailures().size());
                        Assert.assertEquals(0, mTestCacheFile.getAll().size());
                        latch.countDown();
                    }
                }
        );

        latch.await(MAX_WAIT, MAX_WAIT_UNIT);
        Assert.assertEquals(0, mTestCacheFile.getAll().size());
    }

    @Test
    public void testIncorrectKeyProvidedSkips() throws Exception {
        final byte[] mockKey = generateLegacyFormatKey("abcdefabcd");
        final TestEncrypterDecrypter origDelegate = new TestEncrypterDecrypter(mContext, mockKey);

        final String plaintextValue = "a_cool_value";

        final String key = "key";
        final String legacyEncryptedValue = origDelegate.encryptWithLegacyKey(plaintextValue);

        mTestCacheFile.put(key, legacyEncryptedValue);

        // Attempt to reencrypt the cache, but provide the wrong key intentionally
        mFileManagerReencrypter.reencrypt(
                mTestCacheFile,
                mTestEncrypterDecrypter,
                mTestEncrypterDecrypter,
                new IMultiTypeNameValueStorageReencrypter.ReencryptionParams(
                        false,
                        false,
                        false
                )
        );

        // Assert nothing was done
        Assert.assertEquals(legacyEncryptedValue, mTestCacheFile.get(key));
    }

    public void testIncorrectKeyProvidedThrows() throws Exception {
        final byte[] mockKey = generateLegacyFormatKey("abcdefabcd");
        final TestEncrypterDecrypter origDelegate = new TestEncrypterDecrypter(mContext, mockKey);

        final String plaintextValue = "a_cool_value";

        final String key = "key";
        final String legacyEncryptedValue = origDelegate.encryptWithLegacyKey(plaintextValue);

        mTestCacheFile.put(key, legacyEncryptedValue);

        // Attempt to reencrypt the cache, but provide the wrong key intentionally
        final IMigrationOperationResult result = mFileManagerReencrypter.reencrypt(
                mTestCacheFile,
                mTestEncrypterDecrypter,
                mTestEncrypterDecrypter,
                new IMultiTypeNameValueStorageReencrypter.ReencryptionParams(
                        true,
                        false,
                        false
                )
        );
    }

    @Test
    public void testIncorrectKeyProvidedClearsEntryMultiple() throws Exception {
        final byte[] mockKey = generateLegacyFormatKey("abcdefabcd");
        final TestEncrypterDecrypter origDelegate = new TestEncrypterDecrypter(mContext, mockKey);

        final String plainTextValue = "a_cool_value";
        final String anotherPlaintextValue = "a_cold_value";

        final String keyOne = "key_one";
        final String legacyEncryptedValue = origDelegate.encryptWithLegacyKey(plainTextValue);

        final String keyTwo = "key_two";
        final String anotherLegacyEncryptedValue = origDelegate.encryptWithLegacyKey(anotherPlaintextValue);

        mTestCacheFile.put(keyOne, legacyEncryptedValue);
        mTestCacheFile.put(keyTwo, anotherLegacyEncryptedValue);

        // Attempt to reencrypt the cache, but provide the wrong key intentionally
        final IMigrationOperationResult result = mFileManagerReencrypter.reencrypt(
                mTestCacheFile,
                mTestEncrypterDecrypter,
                mTestEncrypterDecrypter,
                new IMultiTypeNameValueStorageReencrypter.ReencryptionParams(
                        false,
                        true,
                        false
                )
        );

        // Assert entries removed
        Assert.assertNull(mTestCacheFile.get(keyOne));
        Assert.assertNull(mTestCacheFile.get(keyTwo));
        Assert.assertEquals(2, result.getCountOfTotalRecords());
        Assert.assertEquals(2, result.getCountOfFailedRecords());
        Assert.assertEquals(1, result.getFailures().size());
    }

    @Test
    public void testIncorrectKeyProvidedClearsEntryAndAbortsMultiple() throws Exception {
        final byte[] mockKey = generateLegacyFormatKey("abcdefabcd");
        final TestEncrypterDecrypter origDelegate = new TestEncrypterDecrypter(mContext, mockKey);

        final String plainTextValue = "a_cool_value";
        final String anotherPlaintextValue = "a_cold_value";

        final String keyOne = "key_one";
        final String legacyEncryptedValue = origDelegate.encryptWithLegacyKey(plainTextValue);

        final String keyTwo = "key_two";
        final String anotherLegacyEncryptedValue = origDelegate.encryptWithLegacyKey(anotherPlaintextValue);

        mTestCacheFile.put(keyOne, legacyEncryptedValue);
        mTestCacheFile.put(keyTwo, anotherLegacyEncryptedValue);

        // Attempt to reencrypt the cache, but provide the wrong key intentionally
        final IMigrationOperationResult result = mFileManagerReencrypter.reencrypt(
                mTestCacheFile,
                mTestEncrypterDecrypter,
                mTestEncrypterDecrypter,
                new IMultiTypeNameValueStorageReencrypter.ReencryptionParams(
                        true,
                        true,
                        false
                )
        );

        Assert.assertEquals(2, result.getCountOfTotalRecords());
        Assert.assertEquals(1, result.getCountOfFailedRecords());
        Assert.assertNull(mTestCacheFile.get(keyOne));
        Assert.assertEquals(anotherLegacyEncryptedValue, mTestCacheFile.get(keyTwo));
    }

    @Test
    public void testIncorrectKeyProvidedClearsAllEntries() throws Exception {
        final byte[] mockKey = generateLegacyFormatKey("abcdefabcd");
        final TestEncrypterDecrypter origDelegate = new TestEncrypterDecrypter(mContext, mockKey);

        final String plainTextValue = "a_cool_value";
        final String anotherPlaintextValue = "a_cold_value";

        final String keyOne = "key_one";
        final String legacyEncryptedValue = origDelegate.encryptWithLegacyKey(plainTextValue);

        final String keyTwo = "key_two";
        final String anotherLegacyEncryptedValue = origDelegate.encryptWithLegacyKey(anotherPlaintextValue);

        mTestCacheFile.put(keyOne, legacyEncryptedValue);
        mTestCacheFile.put(keyTwo, anotherLegacyEncryptedValue);

        // Attempt to reencrypt the cache, but provide the wrong key intentionally
        try {
            mFileManagerReencrypter.reencrypt(
                    mTestCacheFile,
                    mTestEncrypterDecrypter,
                    mTestEncrypterDecrypter,
                    new IMultiTypeNameValueStorageReencrypter.ReencryptionParams(
                            true,
                            false,
                            true
                    )
            );
        } catch (final Exception e) {
            Assert.assertEquals(0, mTestCacheFile.getAll().size());
        }
    }

    @Test
    public void testMultipleInvocationsAborts() throws Exception {
        // Add the numbers 0-9 to the cache, encrypted format...
        for (int ii = 0; ii < 10; ii++) {
            try {
                final String encryptedIntStr = mTestEncrypterDecrypter.encryptWithLegacyKey(String.valueOf(ii));
                mTestCacheFile.put(String.valueOf(ii), encryptedIntStr);
            } catch (Exception e) {
                // wont happen
            }
        }

        Assert.assertEquals(10, mTestCacheFile.getAll().size());

        // Ensure that the 'legacy' and new keys do not create the same value...
        final String legacyEncryptedZero = mTestCacheFile.get("0");
        final String newEncryptedZero = mTestEncrypterDecrypter.encrypt("0");

        Assert.assertNotEquals(legacyEncryptedZero, newEncryptedZero);

        // Reencrypt the cache

        mFileManagerReencrypter.reencrypt(
                mTestCacheFile,
                mStringEncrypter,
                mStringDecrypter,
                new IMultiTypeNameValueStorageReencrypter.ReencryptionParams(
                        true,
                        false,
                        false
                )
        );

        Assert.assertEquals(10, mTestCacheFile.getAll().size());

        // Do it again! (this is now the wrong key)
        try {
            mFileManagerReencrypter.reencrypt(
                    mTestCacheFile,
                    mStringEncrypter,
                    mStringDecrypter,
                    new IMultiTypeNameValueStorageReencrypter.ReencryptionParams(
                            true,
                            false,
                            false
                    )
            );
        } catch (final Exception e) {
            Assert.assertEquals(10, mTestCacheFile.getAll().size());
        }
    }

    @Test
    public void testMultipleInvocationsAbortsAndClears() throws Exception {
        // Add the numbers 0-9 to the cache, encrypted format...
        for (int ii = 0; ii < 10; ii++) {
            try {
                final String encryptedIntStr = mTestEncrypterDecrypter.encryptWithLegacyKey(String.valueOf(ii));
                mTestCacheFile.put(String.valueOf(ii), encryptedIntStr);
            } catch (Exception e) {
                // wont happen
            }
        }

        Assert.assertEquals(10, mTestCacheFile.getAll().size());

        // Ensure that the 'legacy' and new keys do not create the same value...
        final String legacyEncryptedZero = mTestCacheFile.get("0");
        final String newEncryptedZero = mTestEncrypterDecrypter.encrypt("0");

        Assert.assertNotEquals(legacyEncryptedZero, newEncryptedZero);

        // Reencrypt the cache

        mFileManagerReencrypter.reencrypt(
                mTestCacheFile,
                mStringEncrypter,
                mStringDecrypter,
                new IMultiTypeNameValueStorageReencrypter.ReencryptionParams(
                        true,
                        false,
                        false
                )
        );

        Assert.assertEquals(10, mTestCacheFile.getAll().size());

        // Do it again! (this is now the wrong key)
        try {
            mFileManagerReencrypter.reencrypt(
                    mTestCacheFile,
                    mStringEncrypter,
                    mStringDecrypter,
                    new IMultiTypeNameValueStorageReencrypter.ReencryptionParams(
                            true,
                            false,
                            true
                    )
            );
        } catch (final Exception e) {
            Assert.assertEquals(0, mTestCacheFile.getAll().size());
        }
    }

}
