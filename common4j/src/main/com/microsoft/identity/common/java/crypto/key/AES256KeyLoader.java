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
package com.microsoft.identity.common.java.crypto.key;

import org.jetbrains.annotations.NotNull;

/**
 * Abstract base class for loading AES-256 secret keys.
 * <p>
 * This class implements the {@link ISecretKeyLoader} interface and provides a default
 * implementation for retrieving an AES-256 key generator. It serves as a base class
 * for concrete implementations that handle the loading of AES-256 secret keys from
 * various sources.
 */
public abstract class AES256KeyLoader implements ISecretKeyLoader {

    /**
     * Shared instance of AES256SecretKeyGenerator.
     * Created once and reused across all instances to avoid unnecessary object creation.
     */
    private static final ISecretKeyGenerator AES_256_KEY_GENERATOR = new AES256SecretKeyGenerator();

    /**
     * Returns an AES-256 secret key generator.
     * <p>
     * This implementation returns a shared instance of {@link AES256SecretKeyGenerator}
     * to avoid unnecessary object creation, as the generator is stateless.
     *
     * @return A shared instance of {@link AES256SecretKeyGenerator}
     */
    @Override
    @NotNull
    public ISecretKeyGenerator getSecretKeyGenerator() {
        return AES_256_KEY_GENERATOR;
    }
}
