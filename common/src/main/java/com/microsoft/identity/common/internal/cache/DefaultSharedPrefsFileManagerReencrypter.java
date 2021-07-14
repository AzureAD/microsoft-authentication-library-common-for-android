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
package com.microsoft.identity.common.internal.cache;

import androidx.annotation.NonNull;

import com.microsoft.identity.common.internal.controllers.TaskCompletedCallbackWithError;

import java.util.HashMap;
import java.util.Map;

/**
 * Default implementation of {@link ISharedPrefsFileManagerReencrypter}.
 */
@Deprecated
public class DefaultSharedPrefsFileManagerReencrypter implements ISharedPrefsFileManagerReencrypter {

    @Override
    public void reencrypt(@NonNull final ISharedPreferencesFileManager fileManager,
                          @NonNull final IStringEncrypter encrypter,
                          @NonNull final IStringDecrypter decrypter,
                          @NonNull final ReencryptionParams params) throws Exception {
        final Map<String, String> cacheEntries = new HashMap<>(fileManager.getAll());

        for (final Map.Entry<String, String> entry : cacheEntries.entrySet()) {
            final String clearTextKey = entry.getKey();
            final String encryptedTextValye = entry.getValue();

            try {
                // Decrypt the current entry
                final String decryptedTextValue = decrypter.decrypt(encryptedTextValye);

                // Reencrypt the entry
                final String reencryptedTextValue = encrypter.encrypt(decryptedTextValue);

                // Overwrite the existing value in-place
                fileManager.putString(clearTextKey, reencryptedTextValue);
            } catch (final Exception e) {
                if (params.eraseEntryOnError()) {
                    fileManager.remove(clearTextKey);
                }

                if (params.eraseAllOnError()) {
                    fileManager.clear();

                    if (params.abortOnError()) {
                        throw e;
                    } else {
                        break;
                    }
                }

                if (params.abortOnError()) {
                    throw e;
                }
            }
        }
    }

    @Override
    public void reencryptAsync(@NonNull final ISharedPreferencesFileManager fileManager,
                               @NonNull final IStringEncrypter encrypter,
                               @NonNull final IStringDecrypter decrypter,
                               @NonNull final ReencryptionParams params,
                               @NonNull final TaskCompletedCallbackWithError<Void, Exception> callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    reencrypt(fileManager, encrypter, decrypter, params);
                    callback.onTaskCompleted(null);
                } catch (final Exception e) {
                    callback.onError(e);
                }
            }
        }).start();
    }
}
