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
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.microsoft.identity.common.exception.ClientException;

import java.util.Date;

public class AndroidKeystoreAsymmetricKey implements AsymmetricKey {

    private final IDevicePopManager mDevicePopManager;

    AndroidKeystoreAsymmetricKey(@NonNull final Context context,
                                 @NonNull final IDevicePopManager popManager)
            throws ClientException {
        mDevicePopManager = popManager;

        if (!mDevicePopManager.asymmetricKeyExists()) {
            mDevicePopManager.generateAsymmetricKey(context);
        }
    }

    @Override
    public Date getCreatedOn() throws ClientException {
        return mDevicePopManager.getAsymmetricKeyCreationDate();
    }

    @Override
    public String getThumbprint() throws ClientException {
        return mDevicePopManager.getAsymmetricKeyThumbprint();
    }

    @Override
    public String getPublicKey() throws ClientException {
        return mDevicePopManager.getPublicKey(IDevicePopManager.PublicKeyFormat.JWK);
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    public String sign(@NonNull final String data) throws ClientException {
        return mDevicePopManager.sign(
                DevicePopManager.SigningAlgorithms.SHA_256_WITH_RSA,
                data
        );
    }
}
