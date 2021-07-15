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
package com.microsoft.identity.common.migration;

import com.microsoft.identity.common.internal.cache.ISharedPreferencesFileManager;
import com.microsoft.identity.common.internal.controllers.TaskCompletedCallback;

/**
 * Interface describing an object that can reencrypt instances of
 * {@link ISharedPreferencesFileManager}.
 */
public interface ISharedPrefsFileManagerReencrypter {

    /**
     * The object to which this class delegates reencryption of the
     * {@link ISharedPreferencesFileManager}.
     */
    interface IStringEncrypter {
        String encrypt(String input) throws Exception;
    }

    /**
     * The object to which this class delegates decryption of the input
     * {@link ISharedPreferencesFileManager}.
     */
    interface IStringDecrypter {
        String decrypt(String input) throws Exception;
    }

    /**
     * Encapsulates error handling switches for controlling reencryption.
     */
    class ReencryptionParams {

        private final boolean mAbortOnError;
        private final boolean mEraseEntryOnError;
        private final boolean mEraseAllOnError;

        /**
         * Constructs a new {@link ReencryptionParams}.
         *
         * @param abortOnError      True if the operation should abort upon errors.
         * @param eraseEntryOnError True, if the operation should delete the entry that caused the error.
         * @param eraseAllOnError   True, if the operation should delete all entries if an error occurs.
         */
        public ReencryptionParams(final boolean abortOnError,
                                  final boolean eraseEntryOnError,
                                  final boolean eraseAllOnError) {
            mAbortOnError = abortOnError;
            mEraseEntryOnError = eraseEntryOnError;
            mEraseAllOnError = eraseAllOnError;
        }

        /**
         * Gets the abortOnError value.
         *
         * @return True, if the operation should abort upon error.
         */
        boolean abortOnError() {
            return mAbortOnError;
        }

        /**
         * Gets the eraseEntryOnError value.
         *
         * @return True, if the operation should erase the problematic entry upon error.
         */
        boolean eraseEntryOnError() {
            return mEraseEntryOnError;
        }

        /**
         * Gets the eraseAllOnError value.
         *
         * @return True, if the operation should erase all entries upon error.
         */
        boolean eraseAllOnError() {
            return mEraseAllOnError;
        }
    }

    /**
     * Performs reencryption of the provided {@link ISharedPreferencesFileManager}, delegating to
     * the suppplied {@link IStringEncrypter} and {@link IStringDecrypter} to perform content
     * transformations.
     * <p>
     * Please note: this method does not lock the underlying store during reencryption. Users of
     * this API are advised to ensure the designated store is not mutated during the reencryption
     * process otherwise undefined behavior/results may occur.
     *
     * @param fileManager The {@link ISharedPreferencesFileManager} to reencrypt.
     * @param encrypter   The delegate object to handle reencryption.
     * @param decrypter   The delegate object to handle decryption of the existing data.
     * @param params      Params to control error handling behavior.
     */
    IMigrationOperationResult reencrypt(ISharedPreferencesFileManager fileManager,
                                        IStringEncrypter encrypter,
                                        IStringDecrypter decrypter,
                                        ReencryptionParams params
    );

    /**
     * Performs reencryption of the provided {@link ISharedPreferencesFileManager} asynchronously,
     * delegating to the suppplied {@link IStringEncrypter} and {@link IStringDecrypter} to perform
     * content transformations.
     * <p>
     * Please note: this method does not lock the underlying store during reencryption. Users of
     * this API are advised to ensure the designated store is not mutated during the reencryption
     * process otherwise undefined behavior/results may occur.
     *
     * @param fileManager The {@link ISharedPreferencesFileManager} to reencrypt.
     * @param encrypter   The delegate object to handle reencryption.
     * @param decrypter   The delegate object to handle decryption of the existing data.
     * @param params      Params to control error handling behavior.
     * @param callback    Callback to receive any error/completion callbacks.
     */
    void reencryptAsync(ISharedPreferencesFileManager fileManager,
                        IStringEncrypter encrypter,
                        IStringDecrypter decrypter,
                        ReencryptionParams params,
                        TaskCompletedCallback<IMigrationOperationResult> callback
    );
}
