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
package com.microsoft.identity.common.java.interfaces;

import com.microsoft.identity.common.java.WarningType;
import com.microsoft.identity.common.java.crypto.IDevicePopManager;
import com.microsoft.identity.common.java.crypto.IKeyAccessor;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.providers.oauth2.IStateGenerator;
import com.microsoft.identity.common.java.util.IClockSkewManager;
import com.microsoft.identity.common.java.util.IPlatformUtil;
import com.microsoft.identity.common.strategies.IAuthorizationStrategyFactory;

import edu.umd.cs.findbugs.annotations.Nullable;
import lombok.NonNull;

/**
 * Common components for each platforms.
 */
public interface IPlatformComponents {

    /**
     * Gets an {@link INameValueStorage<String>} associated to the given name.
     */
    @NonNull
    INameValueStorage<String> getNameValueStorage(@NonNull final String name);

    /**
     * Get an encryption manager for storage layer.
     */
    @NonNull
    IKeyAccessor getStorageEncryptionManager();

    /**
     * Gets clock skew manager.
     */
    @NonNull
    IClockSkewManager getClockSkewManager();

    /**
     * Gets the default {@link IDevicePopManager}
     *
     * @throws ClientException if it fails to initalize, or if the operation is not supported by the platform.
     */
    @NonNull
    IDevicePopManager getDefaultDevicePopManager() throws ClientException;

    /**
     * Gets a {@link IDevicePopManager} associated to the key alias.
     *
     * @param alias the alias of the key. Storage mechanisms can use this alias to provide access.
     * @throws ClientException if it fails to initalize, or if the operation is not supported by the platform.
     */
    @NonNull
    IDevicePopManager getDevicePopManager(@Nullable final String alias) throws ClientException;

    /**
     * Gets {@link IAuthorizationStrategyFactory} of each platform.
     */
    @SuppressWarnings(WarningType.rawtype_warning)
    @NonNull
    IAuthorizationStrategyFactory getAuthorizationStrategyFactory();

    /**
     * This generates a non-guessable value for the state parameter in an authorization request per the specification:
     * https://tools.ietf.org/html/rfc6749#section-10.10
     */
    @NonNull
    IStateGenerator getStateGenerator();

    /**
     * Get a suite of platform-specific utility operations.
     */
    @NonNull
    IPlatformUtil getPlatformUtil();
}
