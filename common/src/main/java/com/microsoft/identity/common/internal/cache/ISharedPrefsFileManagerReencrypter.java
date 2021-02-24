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
package com.microsoft.identity.common.internal.cache;

import com.microsoft.identity.common.internal.controllers.TaskCompletedCallbackWithError;

/**
 * Interface describing an object that can reencrypt instances of
 * {@link ISharedPreferencesFileManager}.
 */
public interface ISharedPrefsFileManagerReencrypter {

    interface IStringEncrypter {
        String encrypt(String input) throws Exception;
    }

    interface IStringDecrypter {
        String decrypt(String input) throws Exception;
    }

    class ReencryptionParams {

        private final boolean mAbortOnError;
        private final boolean mEraseEntryOnError;
        private final boolean mEraseAllOnError;

        public ReencryptionParams(final boolean abortOnError,
                                  final boolean eraseEntryOnError,
                                  final boolean eraseAllOnError) {
            mAbortOnError = abortOnError;
            mEraseEntryOnError = eraseEntryOnError;
            mEraseAllOnError = eraseAllOnError;
        }

        boolean abortOnError() {
            return mAbortOnError;
        }

        boolean eraseEntryOnError() {
            return mEraseEntryOnError;
        }

        boolean eraseAllOnError() {
            return mEraseAllOnError;
        }
    }

    void reencrypt(ISharedPreferencesFileManager fileManager,
                   IStringEncrypter encrypter,
                   IStringDecrypter decrypter,
                   ReencryptionParams params,
                   TaskCompletedCallbackWithError<Void, Exception> callback
    );
}
