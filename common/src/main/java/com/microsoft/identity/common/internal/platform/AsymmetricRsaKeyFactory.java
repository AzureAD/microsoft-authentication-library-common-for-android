//  Copyright (c) Microsoft Corporation.
//  All rights reserved.
//
//  This code is licensed under the MIT License.
//
//  Permission is hereby granted, free of charge, to any person obtaining a copy
//  of this software and associated documentation files(the "Software"), to deal
//  in the Software without restriction, including without limitation the rights
//  to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
//  copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions :
//
//  The above copyright notice and this permission notice shall be included in
//  all copies or substantial portions of the Software.
//
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//  THE SOFTWARE.
package com.microsoft.identity.common.internal.platform;

import com.microsoft.identity.common.java.exception.ClientException;

/**
 * Interface defining a factory for instances of {@link AsymmetricRsaKey}.
 */
public interface AsymmetricRsaKeyFactory extends AsymmetricKeyFactory {

    /**
     * Generates a new {@link AsymmetricRsaKey}, retrievable by the provided alias. API 18 is assumed.
     * API 23+ affords best security due to improved AndroidKeystore APIs.
     * <p>
     * Keys will be:
     * - 2048+ bit key length (per NIST 800-57 Pt 3 Rev 1).
     * - Software or hardware backed, depending on compat with device OS, hardware, API level.
     *
     * @param alias The name of the key to create.
     * @return The newly-created asymmetric key.
     * @throws ClientException If the key cannot be created.
     */
    @Override
    AsymmetricRsaKey generateAsymmetricKey(String alias) throws ClientException;

    /**
     * Retrieves an asymmetric key by name. If no key can be found for the provided alias, it will
     * be created.
     *
     * @param alias The alias for the sought asymmetric key.
     * @return The asymmetric key.
     * @throws ClientException If the key cannot be retrieved/created.
     */
    AsymmetricRsaKey loadAsymmetricKey(String alias) throws ClientException;

}
