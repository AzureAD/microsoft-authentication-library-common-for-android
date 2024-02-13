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
package com.microsoft.identity.common.java.storage;

import com.microsoft.identity.common.java.crypto.IKeyAccessor;
import com.microsoft.identity.common.java.crypto.KeyAccessorStringAdapter;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.interfaces.INameValueStorage;
import com.microsoft.identity.common.java.logging.Logger;
import com.microsoft.identity.common.java.util.StringUtil;
import com.microsoft.identity.common.java.util.ported.Predicate;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import edu.umd.cs.findbugs.annotations.Nullable;
import lombok.NonNull;

/**
 * An encrypted implementation of a {@link INameValueStorage}. Each value stored in this storage is
 * encrypted using the encryption manager provided to this storage via its constructor.
 * <p>
 * Please note that internal raw storage here is backed by a String based {@link INameValueStorage}
 * that is provided to the constructor here. Any value that we intend to put into the
 * {@link EncryptedNameValueStorage} based on the type parameter T is first adapted into a String
 * based on the provided {@link IGenericTypeStringAdapter} for that type and then the String is
 * encrypted and persisted into the raw String based name value storage.
 * <p>
 * Doing the above gives us a design very similar to the SharedPreferencesFileManager that we have
 * in the Android Broker (that one actually only supports Strings and Long...this one should
 * support any type).
 *
 * @param <T> the type parameter denoting the Type of data that we intend to store
 */
public class EncryptedNameValueStorage<T> implements INameValueStorage<T> {

    private static final String TAG = EncryptedNameValueStorage.class.getSimpleName();

    @NonNull
    private final INameValueStorage<String> mRawNameValueStorage;

    @NonNull
    private final KeyAccessorStringAdapter mEncryptionManager;

    @NonNull
    private final IGenericTypeStringAdapter<T> mStringAdapter;

    /**
     * Creates an instance of an {@link EncryptedNameValueStorage}.
     *
     * @param rawNameValueStringStorage the raw String based {@link INameValueStorage} where the
     *                                  encrypted data is stored
     * @param encryptionManager         the {@link IKeyAccessor} responsible for encrypting the data
     * @param stringAdapter             the {@link IGenericTypeStringAdapter} that will be used to
     *                                  adapt the values to their String representation before
     *                                  encrypting and persisting them
     */
    public EncryptedNameValueStorage(@NonNull final INameValueStorage<String> rawNameValueStringStorage,
                                     @NonNull final IKeyAccessor encryptionManager,
                                     @NonNull final IGenericTypeStringAdapter<T> stringAdapter) {
        this.mRawNameValueStorage = rawNameValueStringStorage;
        this.mEncryptionManager = new KeyAccessorStringAdapter(encryptionManager);
        this.mStringAdapter = stringAdapter;
    }

    @Nullable
    @Override
    public T get(@NonNull final String name) {
        final String methodTag = TAG + ":get";

        final String encryptedString = mRawNameValueStorage.get(name);
        if (StringUtil.isNullOrEmpty(encryptedString)) {
            Logger.info(methodTag, "Data associated to the given key is null or empty", null);
            remove(name);
            return null;
        }

        try {
            final String decryptedString = mEncryptionManager.decrypt(encryptedString);
            return mStringAdapter.adapt(decryptedString);
        } catch (final ClientException e){
            Logger.error(methodTag, "Failed to read encrypted value", null);
            return null;
        }
    }

    @Override
    public @NonNull Map<String, T> getAll() {
        final Map<String, String> stringEntries = mRawNameValueStorage.getAll();
        final Map<String, T> decryptedEntries = new HashMap<>();

        for (final Map.Entry<String, String> entry : stringEntries.entrySet()) {
            final T decryptedValue = get(entry.getKey());

            if (decryptedValue != null) {
                decryptedEntries.put(entry.getKey(), decryptedValue);
            }
        }

        return decryptedEntries;
    }

    @Override
    public void put(@NonNull final String name, @Nullable final T value) {
        final String methodTag = TAG + ":put";

        if (value == null) {
            mRawNameValueStorage.put(name, null);
            return;
        }

        final String adaptedValue = mStringAdapter.adapt(value);
        if (StringUtil.isNullOrEmpty(adaptedValue)) {
            mRawNameValueStorage.put(name, adaptedValue);
            return;
        }

        // If the encryption failed, we won't be storing anything.
        try {
            final String encryptedValue = mEncryptionManager.encrypt(adaptedValue);
            mRawNameValueStorage.put(name, encryptedValue);
        } catch (final ClientException e) {
            Logger.error(methodTag, "Failed to store encrypted value", null);
        }
    }

    @Override
    public void remove(@NonNull final String name) {
        mRawNameValueStorage.remove(name);
    }

    @Override
    public void clear() {
        mRawNameValueStorage.clear();
    }

    @Override
    public @NonNull Set<String> keySet() {
        return mRawNameValueStorage.keySet();
    }

    @Override
    public Iterator<Map.Entry<String, T>> getAllFilteredByKey(@NonNull final Predicate<String> keyFilter) {
        final Map<String, T> newMap = new HashMap<>();
        for (final Map.Entry<String, T> entry : getAll().entrySet()) {
            if (keyFilter.test(entry.getKey())) {
                newMap.put(entry.getKey(), entry.getValue());
            }
        }
        return newMap.entrySet().iterator();
    }
}
