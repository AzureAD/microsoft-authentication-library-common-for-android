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
package com.microsoft.identity.common.adal.internal.tokensharing;

public interface ITokenShareInternal {

    /**
     * For the supplied user unique identifier (OID/upn/preferred_username), return the
     * corresponding refresh token for that account if the current application is a member of FoCI
     * (family of clientIds). The token will be wrapped inside an opaque self-serializing object
     * and cannot be used directly against an STS.
     *
     * @param identifier The identifier of the sought user's FRT.
     * @return The {@link com.microsoft.identity.common.internal.cache.ADALTokenCacheItem}
     * serialized to JSON.
     * @throws Exception If the requested token cannot be found.
     */
    String getOrgIdFamilyRefreshToken(String identifier) throws Exception;

    /**
     * Saves the supplied SsoStateSerializer blob into the cache.
     *
     * @param ssoStateSerializerBlob The blob to save.
     * @throws Exception If an error is encountered while saving the supplied blob.
     */
    void saveOrgIdFamilyRefreshToken(String ssoStateSerializerBlob) throws Exception;

    /**
     * For the provided MSA user id, retrieve the FRT belonging to this user (if exists).
     *
     * @param identifier The identifier of the sought user's FRT.
     * @return The raw FRT for the provided identifier.
     * @throws Exception If the requested token cannot be found.
     */
    String getMsaFamilyRefreshToken(String identifier) throws Exception;

    /**
     * Saves the supplied FRT to the cache.
     *
     * @param refreshToken The FRT to save.
     * @throws Exception If an Exception is encountered while saving the FRT.
     */
    void saveMsaFamilyRefreshToken(String refreshToken) throws Exception;
}
