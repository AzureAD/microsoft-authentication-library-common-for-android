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

/**
 * Cache the authority validation metadata that is only used in MSAL CPP.
 */
public interface IDefaultAuthorityValidationMetadataCache {

    /**
     * Generate cache Key for authority validation metadata.
     *
     * @param environment Environment
     * @return String
     */
    public String generateAuthorityValidationMetadataKey(final String environment);

    /**
     * This function is only used in the authority validation in MSAL CPP
     * Save the key/cacheValue to the cache.
     *
     * @param environment Environment.
     * @param cacheValue Value.
     */
    public void saveAuthorityValidationMetadata(final String environment, final String cacheValue);

    /**
     * This function is only used in the authority validation in MSAL CPP
     * Returns the saved string value from the cache.
     *
     * @param environment Environment.
     * @return The string of the cached value, or null if not exist.
     */
    public String getAuthorityValidationMetadata(final String environment);

    /**
     * API to clear all cache.
     * Note: This method is intended to be only used for testing purposes.
     */
    public void clearCache();
}
