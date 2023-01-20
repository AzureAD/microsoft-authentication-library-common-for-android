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

import static com.microsoft.identity.common.java.AuthenticationConstants.ENCODING_UTF8;

import com.microsoft.identity.common.java.exception.ClientException;

import lombok.AllArgsConstructor;
import lombok.NonNull;

/**
 * Add helper functions which takes in parameter or produce results in a ready-to-store (String) form.
 * Kept for backward compatibilty. For post migration, use Base64KeyAccessorStringAdapter instead.
 */
@AllArgsConstructor
public class UTF8KeyAccessorStringAdapter implements IKeyAccessorStringAdapter {

    private final IKeyAccessor mKeyAcccesor;

    /**
     * Encrypt a plaintext string, returning an encrypted UTF-8 encoded string.
     *
     * @param plainText the plaintext to encrypt.
     * @return the encrypted UTF-8 string.
     */
    public String encrypt(@NonNull String plainText) throws ClientException {
        final byte[] result = mKeyAcccesor.encrypt(plainText.getBytes(ENCODING_UTF8));
        return new String(result, ENCODING_UTF8);
    }

    /**
     * Decrypt a UTF-8 ciphertext, returning the decrypted values.
     *
     * @param cipherText the UTF-8 ciphertext to decrypt.
     * @return the decrypted string.
     */
    public String decrypt(@NonNull String cipherText) throws ClientException {
        final byte[] result = mKeyAcccesor.decrypt(cipherText.getBytes(ENCODING_UTF8));
        return new String(result, ENCODING_UTF8);
    }
}
