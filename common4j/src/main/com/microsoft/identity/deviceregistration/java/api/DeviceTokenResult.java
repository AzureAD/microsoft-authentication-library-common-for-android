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
package com.microsoft.identity.deviceregistration.java.api;

import edu.umd.cs.findbugs.annotations.Nullable;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.Accessors;

/**
 * Result of a device token request, carrying the full token response returned by the
 * token endpoint.
 */
@Builder
@Getter
@Accessors(prefix = "m")
public class DeviceTokenResult {

    /**
     * The device token.
     */
    @NonNull
    private final String mAccessToken;

    /**
     * Signed JWT containing device claims (device id, tenant id, object id).
     */
    @Nullable
    private final String mDeviceInfo;

    /**
     * Token scheme the access token was issued under, e.g. {@code Bearer}.
     */
    @Nullable
    private final String mTokenType;

    /**
     * Resource the token was issued for.
     */
    @Nullable
    private final String mResource;

    /**
     * Lifetime of the token in seconds from the time it was issued.
     * Prefer this over {@link #getExpiresOn()}, which is subject to client/server clock skew.
     */
    @Nullable
    private final Long mExpiresIn;

    /**
     * Extended lifetime of the token in seconds, used when the token service is unavailable.
     */
    @Nullable
    private final Long mExtExpiresIn;

    /**
     * Epoch seconds at which the token expires, as reported by the server.
     */
    @Nullable
    private final Long mExpiresOn;

    /**
     * Epoch seconds before which the token must not be accepted, as reported by the server.
     */
    @Nullable
    private final Long mNotBefore;
}
