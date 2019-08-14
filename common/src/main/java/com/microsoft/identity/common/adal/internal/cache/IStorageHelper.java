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
package com.microsoft.identity.common.adal.internal.cache;

import java.io.IOException;
import java.security.GeneralSecurityException;

import javax.crypto.SecretKey;

public interface IStorageHelper {
    /**
     * Encrypt text with current key based on API level.
     *
     * @param clearText Clear text to encrypt.
     * @return Encrypted blob.
     * @throws GeneralSecurityException for key related exceptions.
     * @throws IOException              For general IO related exceptions.
     */
    String encrypt(String clearText) throws GeneralSecurityException, IOException;

    /**
     * Decrypt encrypted blob with either user provided key or key persisted in AndroidKeyStore.
     *
     * @param encryptedBlob The blob to decrypt
     * @return Decrypted clear text.
     * @throws GeneralSecurityException for key related exceptions.
     * @throws IOException              For general IO related exceptions.
     */
    String decrypt(String encryptedBlob) throws GeneralSecurityException, IOException;

    /**
     * Get Secret Key based on API level to use in encryption. Decryption key
     * depends on version# since user can migrate to new Android.OS
     *
     * @return SecretKey Get Secret Key based on API level to use in encryption.
     * @throws GeneralSecurityException throws if general security error happens.
     * @throws IOException              throws if I/O error happens.
     */
    SecretKey loadSecretKeyForEncryption() throws IOException, GeneralSecurityException;
}
