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
import com.microsoft.identity.common.java.crypto.IKeyAccessor;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.providers.oauth2.IStateGenerator;
import com.microsoft.identity.common.java.strategies.IAuthorizationStrategyFactory;
import com.microsoft.identity.common.java.util.IBroadcaster;
import com.microsoft.identity.common.java.util.IClockSkewManager;
import com.microsoft.identity.common.java.util.IPlatformUtil;

import edu.umd.cs.findbugs.annotations.Nullable;
import lombok.NonNull;

/**
 * An interface for providing components for each platforms.
 *
 * TODO: make getPopManagerLoader() and getStorageLoader() part of this interface.
 *       will do that in a separate PR to minimize PR size.
 */
public interface IPlatformComponents extends IPopManagerLoader, IStorageLoader {

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
     * Gets a {@link com.microsoft.identity.common.java.util.IBroadcaster} associated to the alias.
     *
     * @throws ClientException if it fails to initialize, or if the operation is not supported by the platform.
     */
    @NonNull
    IBroadcaster getBroadcaster() throws ClientException;

    /**
     * Gets {@link IAuthorizationStrategyFactory} of each platform.
     * Might be null for silent flow.
     */
    @SuppressWarnings(WarningType.rawtype_warning)
    @Nullable
    IAuthorizationStrategyFactory getAuthorizationStrategyFactory();

    /**
     * This generates a non-guessable value for the state parameter in an authorization request per the specification:
     * https://tools.ietf.org/html/rfc6749#section-10.10
     * Might be null for silent flow.
     */
    @Nullable
    IStateGenerator getStateGenerator();

    /**
     * Get a suite of platform-specific utility operations.
     */
    @NonNull
    IPlatformUtil getPlatformUtil();

    /**
     * Returns a wrapper of {@link com.microsoft.identity.common.java.net.HttpClient} objects.
     * This will allow test cases to interject an interceptor - to mock HTTP requests/responses.
     * */
    @NonNull
    IHttpClientWrapper getHttpClientWrapper();
}
