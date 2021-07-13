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
package com.microsoft.identity.common.migration;

import androidx.annotation.NonNull;

import com.microsoft.identity.common.internal.cache.ISharedPreferencesFileManager;
import com.microsoft.identity.common.internal.controllers.TaskCompletedCallback;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Default implementation of {@link ISharedPrefsFileManagerReencrypter}.
 */
public class DefaultSharedPrefsFileManagerReencrypter implements ISharedPrefsFileManagerReencrypter {

    @Override
    public IMigrationOperationResult reencrypt(@NonNull final ISharedPreferencesFileManager fileManager,
                                               @NonNull final IStringEncrypter encrypter,
                                               @NonNull final IStringDecrypter decrypter,
                                               @NonNull final ReencryptionParams params) {
        final Map<String, String> cacheEntries = new HashMap<>(fileManager.getAll());
        final MigrationOperationResult result = new MigrationOperationResult();
        result.setTotalRecords(cacheEntries.size());
        final Set<String> keysMarkedForRemoval = new HashSet<>();
        final AtomicBoolean shouldAbort = new AtomicBoolean(false);
        final Set<String> skipKeys = new HashSet<>();

        // Decrypt everything
        applyCacheMutation(
                cacheEntries,
                new Callable<Map.Entry<String, String>>() {
                    @Override
                    public void call(final Map.Entry<String, String> entry) throws Exception {
                        final String decryptedText = decrypter.decrypt(entry.getValue());
                        entry.setValue(decryptedText);
                    }
                },
                result,
                params,
                keysMarkedForRemoval,
                skipKeys,
                shouldAbort
        );

        // Clear any keys marked for removal...
        clearEntriesMarkedForRemoval(fileManager, cacheEntries, keysMarkedForRemoval);

        if (shouldAbort.get()) {
            return result;
        }

        // Reencrypt everything
        applyCacheMutation(
                cacheEntries,
                new Callable<Map.Entry<String, String>>() {
                    @Override
                    public void call(final Map.Entry<String, String> entry) throws Exception {
                        final String reencryptedText = encrypter.encrypt(entry.getValue());
                        entry.setValue(reencryptedText);
                    }
                },
                result,
                params,
                keysMarkedForRemoval,
                skipKeys,
                shouldAbort
        );

        // Clear any keys marked for removal...
        clearEntriesMarkedForRemoval(fileManager, cacheEntries, keysMarkedForRemoval);

        if (shouldAbort.get()) {
            return result;
        }

        // Write the newly encrypted records...
        for (final Map.Entry<String, String> cacheEntry : cacheEntries.entrySet()) {
            fileManager.putString(cacheEntry.getKey(), cacheEntry.getValue());
        }

        return result;
    }

    private interface Callable<T> {
        void call(T t) throws Exception;
    }

    private void applyCacheMutation(@NonNull final Map<String, String> cacheEntries,
                                    @NonNull final Callable<Map.Entry<String, String>> callable,
                                    @NonNull final MigrationOperationResult inputResult,
                                    @NonNull final ReencryptionParams params,
                                    @NonNull final Set<String> keysMarkedForRemoval,
                                    @NonNull final Set<String> skipKeys,
                                    @NonNull AtomicBoolean shouldAbort) {
        for (final Map.Entry<String, String> cacheEntry : cacheEntries.entrySet()) {
            try {
                if (skipKeys.contains(cacheEntry.getKey())) {
                    continue;
                }
                callable.call(cacheEntry);
            } catch (final Exception e) {
                inputResult.addFailure(e);
                skipKeys.add(cacheEntry.getKey());

                if (params.eraseEntryOnError()) {
                    keysMarkedForRemoval.add(cacheEntry.getKey());
                }

                if (params.eraseAllOnError()) {
                    keysMarkedForRemoval.addAll(cacheEntries.keySet());
                }

                if (params.abortOnError()) {
                    shouldAbort.set(true);
                    break;
                }
            }
        }
    }

    private void clearEntriesMarkedForRemoval(@NonNull final ISharedPreferencesFileManager fileManager,
                                              @NonNull final Map<String, String> cacheEntries,
                                              @NonNull final Set<String> keysMarkedForRemoval) {
        for (final String removedKey : keysMarkedForRemoval) {
            cacheEntries.remove(removedKey);
            fileManager.remove(removedKey);
        }
    }

    @Override
    public void reencryptAsync(@NonNull final ISharedPreferencesFileManager fileManager,
                               @NonNull final IStringEncrypter encrypter,
                               @NonNull final IStringDecrypter decrypter,
                               @NonNull final ReencryptionParams params,
                               @NonNull final TaskCompletedCallback<IMigrationOperationResult> callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                callback.onTaskCompleted(reencrypt(fileManager, encrypter, decrypter, params));
            }
        }).start();
    }
}
