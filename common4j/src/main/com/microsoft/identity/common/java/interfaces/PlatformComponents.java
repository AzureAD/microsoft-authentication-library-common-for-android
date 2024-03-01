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
import com.microsoft.identity.common.java.cache.IMultiTypeNameValueStorage;
import com.microsoft.identity.common.java.crypto.IDevicePopManager;
import com.microsoft.identity.common.java.crypto.IKeyAccessor;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.providers.oauth2.IStateGenerator;
import com.microsoft.identity.common.java.strategies.IAuthorizationStrategyFactory;
import com.microsoft.identity.common.java.util.IBroadcaster;
import com.microsoft.identity.common.java.util.IClockSkewManager;
import com.microsoft.identity.common.java.util.IPlatformUtil;

import edu.umd.cs.findbugs.annotations.Nullable;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

/**
 * An class which provide components for each platforms.
 */
@Getter
@SuperBuilder
@Accessors(prefix = "m")
public class PlatformComponents implements IPlatformComponents {

   // @NonNull
    private final IClockSkewManager mClockSkewManager;

   // @NonNull
    private final IBroadcaster mBroadcaster;

   // @NonNull
    private final IPopManagerSupplier mPopManagerLoader;

    @Nullable
    private final IStorageSupplier mStorageSupplier;

    @SuppressWarnings(WarningType.rawtype_warning)
    @Nullable
    private final IAuthorizationStrategyFactory mAuthorizationStrategyFactory;

    @Nullable
    private final IStateGenerator mStateGenerator;

    // @NonNull
    private final IPlatformUtil mPlatformUtil;

    // @NonNull
    private final IHttpClientWrapper mHttpClientWrapper;

    // TODO: Remove these methods and have the caller invoke IPopManagerSupplier directly.
    // Keeping this for now to minimize the PR size.

    /**
     * Gets the default {@link IDevicePopManager}
     *
     * @throws ClientException if it fails to initialize, or if the operation is not supported by the platform.
     */
    @Override
    @NonNull
    public IDevicePopManager getDefaultDevicePopManager() throws ClientException {
        return mPopManagerLoader.getDefaultDevicePopManager();
    }

    /**
     * Gets a {@link IDevicePopManager} associated to the alias.
     *
     * @throws ClientException if it fails to initialize, or if the operation is not supported by the platform.
     */
    @Override
    @NonNull
    public IDevicePopManager getDevicePopManager(@Nullable final String alias) throws ClientException {
        return mPopManagerLoader.getDevicePopManager(alias);
    }
}
