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

import com.microsoft.identity.common.exception.ClientException;

import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

public class AndroidKeystoreAsymmetricKeyFactory implements AsymmetricKeyFactory {

    private final Context mContext;

    public AndroidKeystoreAsymmetricKeyFactory(@NonNull final Context context) {
        mContext = context;
    }

    @Override
    public AsymmetricKey generateAsymmetricKey(@NonNull final String alias) throws ClientException {
        try {
            return new AndroidKeystoreAsymmetricKey(
                    mContext,
                    new DevicePopManager(alias)
            );
        } catch (KeyStoreException e) {
            e.printStackTrace();
        } catch (CertificateException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // TODO catch, wrap, and rethrow Exception
        return null;
    }

    @Override
    public AsymmetricKey loadAsymmetricKey(String alias) throws ClientException {
        // We can just call generate.... same thing... it will be created if it doesn't exist.
        return generateAsymmetricKey(alias);
    }

    @Override
    public boolean clearAsymmetricKey(String alias) {
        try {
            return new DevicePopManager(alias).clearAsymmetricKey();
        } catch (KeyStoreException e) {
            e.printStackTrace();
        } catch (CertificateException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // TODO catch, wrap, and rethrow Exception
        return false;
    }
}
