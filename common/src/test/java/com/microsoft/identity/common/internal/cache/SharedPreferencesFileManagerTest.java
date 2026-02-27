// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// This code is licensed under the MIT License.

package com.microsoft.identity.common.internal.cache;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

import androidx.test.core.app.ApplicationProvider;

import com.microsoft.identity.common.java.crypto.IKeyAccessor;
import com.microsoft.identity.common.java.exception.ClientException;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Iterator;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.nio.charset.StandardCharsets;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = {Build.VERSION_CODES.N})
public class SharedPreferencesFileManagerTest {

    private final Context mContext = ApplicationProvider.getApplicationContext();
    private final Set<String> mCreatedSharedPreferencesFiles = new HashSet<>();

    private static final String PREFIX = "enc:";

    private IKeyAccessor createMockKeyAccessor() throws Exception {
        final IKeyAccessor keyAccessor = mock(IKeyAccessor.class);

        when(keyAccessor.encrypt(any(byte[].class))).thenAnswer(invocation -> {
            final byte[] plaintext = invocation.getArgument(0);
            final String payload = PREFIX + new String(plaintext, StandardCharsets.UTF_8);
            return payload.getBytes(StandardCharsets.UTF_8);
        });

        when(keyAccessor.decrypt(any(byte[].class))).thenAnswer(invocation -> {
            final byte[] ciphertext = invocation.getArgument(0);
            final String payload = new String(ciphertext, StandardCharsets.UTF_8);
            if (!payload.startsWith(PREFIX)) {
                throw new ClientException("invalid_ciphertext", "Missing expected prefix");
            }

            return payload.substring(PREFIX.length()).getBytes(StandardCharsets.UTF_8);
        });

        return keyAccessor;
    }

    private IKeyAccessor createMockKeyAccessorWithDecryptFailure() throws Exception {
        final IKeyAccessor keyAccessor = createMockKeyAccessor();
        when(keyAccessor.decrypt(any(byte[].class))).thenThrow(
                new ClientException("mock_decrypt_error", "Simulated decrypt failure")
        );
        return keyAccessor;
    }

    private String createTestFileName() {
        final String fileName = "spfm-test-" + UUID.randomUUID();
        mCreatedSharedPreferencesFiles.add(fileName);
        return fileName;
    }

    @After
    public void cleanup() {
        for (final String fileName : mCreatedSharedPreferencesFiles) {
            final SharedPreferences sharedPreferences = mContext.getSharedPreferences(fileName, Context.MODE_PRIVATE);
            sharedPreferences.edit().clear().commit();
            mContext.deleteSharedPreferences(fileName);
        }
        mCreatedSharedPreferencesFiles.clear();
        SharedPreferencesFileManager.clearSingletonCache();
    }

    @Test
    public void getAll_withEncryptionManager_returnsPlaintextValues() throws Exception {
        final String fileName = createTestFileName();
        final String key = "k1";
        final String plaintext = "value-1";

        final IKeyAccessor encryptionManager = createMockKeyAccessor();
        final SharedPreferencesFileManager manager = new SharedPreferencesFileManager(mContext, fileName, encryptionManager);

        manager.putString(key, plaintext);

        final Map<String, String> all = manager.getAll();
        assertEquals(plaintext, all.get(key));
    }

    @Test
    public void getAll_whenDecryptionFails_keepsStoredCiphertextValue() throws Exception {
        final String fileName = createTestFileName();
        final String key = "k2";
        final String plaintext = "value-2";

        final SharedPreferencesFileManager writer = new SharedPreferencesFileManager(
                mContext,
                fileName,
            createMockKeyAccessor()
        );
        writer.putString(key, plaintext);

        final SharedPreferences preferences = mContext.getSharedPreferences(fileName, Context.MODE_PRIVATE);
        final String storedCiphertext = preferences.getString(key, null);
        assertNotNull(storedCiphertext);

        final SharedPreferencesFileManager readerWithDecryptFailure = new SharedPreferencesFileManager(
                mContext,
                fileName,
            createMockKeyAccessorWithDecryptFailure()
        );

        final Map<String, String> all = readerWithDecryptFailure.getAll();
        assertTrue(all.containsKey(key));
        assertEquals(storedCiphertext, all.get(key));
    }

    @Test
    public void getAllFilteredByKey_withEncryptionManager_returnsDecryptedFilteredValues() throws Exception {
        final String fileName = createTestFileName();
        final IKeyAccessor encryptionManager = createMockKeyAccessor();
        final SharedPreferencesFileManager manager = new SharedPreferencesFileManager(mContext, fileName, encryptionManager);

        manager.putString("keep-1", "plain-1");
        manager.putString("drop-1", "plain-2");
        manager.putString("keep-2", "plain-3");

        final Iterator<Map.Entry<String, String>> iterator =
                manager.getAllFilteredByKey(key -> key.startsWith("keep-"));

        int count = 0;
        while (iterator.hasNext()) {
            final Map.Entry<String, String> entry = iterator.next();
            assertTrue(entry.getKey().startsWith("keep-"));

            if ("keep-1".equals(entry.getKey())) {
                assertEquals("plain-1", entry.getValue());
            }

            if ("keep-2".equals(entry.getKey())) {
                assertEquals("plain-3", entry.getValue());
            }

            count++;
        }

        assertEquals(2, count);
    }

    @Test
    public void getAllFilteredByKey_whenDecryptFailsOrValueEmpty_skipsEntry() throws Exception {
        final String fileName = createTestFileName();

        final SharedPreferencesFileManager writer = new SharedPreferencesFileManager(
                mContext,
                fileName,
                createMockKeyAccessor()
        );
        writer.putString("will-fail", "plain-value");
        writer.putString("empty-value", "");

        final SharedPreferencesFileManager readerWithDecryptFailure = new SharedPreferencesFileManager(
                mContext,
                fileName,
            createMockKeyAccessorWithDecryptFailure()
        );

        final Iterator<Map.Entry<String, String>> iterator =
                readerWithDecryptFailure.getAllFilteredByKey(
                        key -> "will-fail".equals(key) || "empty-value".equals(key)
                );

        assertFalse(iterator.hasNext());
    }
}
