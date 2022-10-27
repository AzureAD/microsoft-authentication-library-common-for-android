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
package com.microsoft.identity.common.components;

import static com.microsoft.identity.common.java.exception.ClientException.CERTIFICATE_LOAD_FAILURE;
import static com.microsoft.identity.common.java.exception.ClientException.IO_ERROR;
import static com.microsoft.identity.common.java.exception.ClientException.KEYSTORE_NOT_INITIALIZED;
import static com.microsoft.identity.common.java.exception.ClientException.NO_SUCH_ALGORITHM;

import android.content.Context;

import androidx.annotation.Nullable;

import com.microsoft.identity.common.internal.platform.AndroidDevicePopManager;
import com.microsoft.identity.common.java.crypto.IDevicePopManager;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.interfaces.IPopManagerLoader;

import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.HashMap;

import lombok.AllArgsConstructor;
import lombok.NonNull;

@AllArgsConstructor
public class AndroidPopManagerLoader implements IPopManagerLoader {
    private final Context mContext;

    private final HashMap<String, IDevicePopManager> mCachedPopManager = new HashMap<>();

    @Override
    public @NonNull IDevicePopManager getDefaultDevicePopManager() throws ClientException {
        return getDevicePopManager(null);
    }

    @Override
    public @NonNull IDevicePopManager getDevicePopManager(@Nullable String alias) throws ClientException {
        final IDevicePopManager cachedManager = mCachedPopManager.get(alias);
        if (cachedManager != null) {
            return cachedManager;
        }

        final IDevicePopManager popManager = initializeDevicePopManager(alias);
        mCachedPopManager.put(alias, popManager);
        return popManager;
    }

    @NonNull
    private IDevicePopManager initializeDevicePopManager(@Nullable final String alias) throws ClientException {
        final Exception exception;
        final String errCode;

        try {
            if (alias == null) {
                return new AndroidDevicePopManager(mContext);
            } else {
                return new AndroidDevicePopManager(mContext, alias);
            }
        } catch (final KeyStoreException e) {
            exception = e;
            errCode = KEYSTORE_NOT_INITIALIZED;
        } catch (final CertificateException e) {
            exception = e;
            errCode = CERTIFICATE_LOAD_FAILURE;
        } catch (final NoSuchAlgorithmException e) {
            exception = e;
            errCode = NO_SUCH_ALGORITHM;
        } catch (final IOException e) {
            exception = e;
            errCode = IO_ERROR;
        }

        throw new ClientException(
                errCode,
                "Failed to initialize DevicePoPManager = " + exception.getMessage(),
                exception
        );
    }
}
