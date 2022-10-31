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

import android.content.Context;

import androidx.annotation.NonNull;

import com.microsoft.identity.common.components.AndroidPlatformComponentsFactory;
import com.microsoft.identity.common.java.exception.ClientException;

/**
 * Factory class for constructing asymmetric keys.
 */
public class AndroidKeystoreAsymmetricRsaKeyFactory implements AsymmetricRsaKeyFactory {

    private final Context mContext;

    /**
     * Constructs a new key factory.
     *
     * @param context The current application Context.
     */
    public AndroidKeystoreAsymmetricRsaKeyFactory(@NonNull final Context context) {
        mContext = context;
    }

    @Override
    public synchronized AsymmetricRsaKey generateAsymmetricKey(@NonNull final String alias) throws ClientException {
        return new AndroidKeystoreAsymmetricRsaKey(
                AndroidPlatformComponentsFactory.createFromContext(mContext).getDevicePopManager(alias),
                alias
        );
    }

    @Override
    public synchronized AsymmetricRsaKey loadAsymmetricKey(@NonNull final String alias) throws ClientException {
        // We can just call generate.... same thing... it will be created if it doesn't exist.
        return generateAsymmetricKey(alias);
    }

    @Override
    public synchronized boolean clearAsymmetricKey(@NonNull final String alias) throws ClientException {
        return AndroidPlatformComponentsFactory.createFromContext(mContext).getDevicePopManager(alias).clearAsymmetricKey();
    }
}
