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
package com.microsoft.identity.common.crypto;

import com.microsoft.identity.common.java.crypto.key.AES256KeyLoader;

import javax.crypto.SecretKey;

import lombok.NonNull;

/**
 * For loading an AES-256 key from a provided rawbytes array.
 */
public class PredefinedKeyLoader extends AES256KeyLoader {

    /**
     * Indicate that the token item is encrypted with the user provided key.
     */
    public static final String USER_PROVIDED_KEY_IDENTIFIER = "U001";

    private final String mAlias;
    private final SecretKey mKey;

    public PredefinedKeyLoader(@NonNull final String alias,
                               @NonNull final byte[] rawBytes){
        mAlias = alias;
        mKey = generateKeyFromRawBytes(rawBytes);
    }

    @Override
    @NonNull
    public String getAlias() {
        return mAlias;
    }

    @Override
    @NonNull
    public SecretKey getKey() {
        return mKey;
    }

    @Override
    @NonNull
    public String getKeyTypeIdentifier() {
        return USER_PROVIDED_KEY_IDENTIFIER;
    }
}
