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
package com.microsoft.identity.common.java.crypto;

import com.microsoft.identity.common.java.crypto.key.AES256KeyLoader;
import com.microsoft.identity.common.java.exception.ClientException;

import java.util.ArrayList;
import java.util.List;

import edu.umd.cs.findbugs.annotations.Nullable;
import lombok.NonNull;

public class MockStorageEncryptionManager extends StorageEncryptionManager {

    private final AES256KeyLoader mEncryptKey;
    private final List<AES256KeyLoader> mDecryptKey;

    MockStorageEncryptionManager(@NonNull final byte[] iv,
                                 @Nullable final AES256KeyLoader key) throws ClientException {
        this(iv,
                key,
                new ArrayList<AES256KeyLoader>() {{
                    add(key);
                }});
    }

    MockStorageEncryptionManager(@NonNull final byte[] iv,
                                 @Nullable final AES256KeyLoader encryptKey,
                                 @Nullable final List<AES256KeyLoader> decryptKey) throws ClientException {
        super(new IVGenerator() {
            @Override
            public byte[] generate() {
                return iv.clone();
            }
        });
        mEncryptKey = encryptKey;
        mDecryptKey = decryptKey;
    }

    @Override
    public @NonNull AES256KeyLoader getKeyLoaderForEncryption() throws ClientException {
        return mEncryptKey;
    }

    @Override
    public @NonNull List<AES256KeyLoader> getKeyLoaderForDecryption(@NonNull byte[] cipherText) throws ClientException {
        return mDecryptKey;
    }
}
