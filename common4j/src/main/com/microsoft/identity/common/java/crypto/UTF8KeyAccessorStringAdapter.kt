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
package com.microsoft.identity.common.java.crypto

import com.microsoft.identity.common.java.AuthenticationConstants
import com.microsoft.identity.common.java.exception.ClientException

/**
 * Add helper functions which takes in parameter or produce results in a ready-to-store (String) form.
 */
class UTF8KeyAccessorStringAdapter(private val mKeyAccessor: IKeyAccessor)
    : IKeyAccessorStringAdapter{

    /**
     * Encrypt a plaintext string, returning an encrypted UTF-8 encoded string.
     *
     * @param plainText the plaintext to encrypt.
     * @return the encrypted UTF-8 string.
     */
    @Throws(ClientException::class)
    override fun encrypt(plainText: String): String {
        val result = mKeyAccessor.encrypt(plainText.toByteArray(AuthenticationConstants.ENCODING_UTF8))
        return String(result, AuthenticationConstants.ENCODING_UTF8)
    }

    /**
     * Decrypt a UTF-8 ciphertext, returning the decrypted values.
     *
     * @param cipherText the UTF-8 ciphertext to decrypt.
     * @return the decrypted string.
     */
    @Throws(ClientException::class)
    override fun decrypt(cipherText: String): String {
        val result =
            mKeyAccessor.decrypt(cipherText.toByteArray(AuthenticationConstants.ENCODING_UTF8))
        return String(result, AuthenticationConstants.ENCODING_UTF8)
    }
}