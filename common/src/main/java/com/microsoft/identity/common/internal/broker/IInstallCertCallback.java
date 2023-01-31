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
package com.microsoft.identity.common.internal.broker;

import android.app.Activity;

import androidx.annotation.NonNull;

import com.microsoft.identity.common.java.exception.BaseException;

/**
 * Interface for the callback
 * {@link DeviceRegistrationClientApplication#installCert(IDeviceRegistrationRecord, Activity, InstallCertCallback)}.
 * If certificate is installed successfully, it will be returned through onSuccess.
 * Otherwise, a {@link BaseException} will be returned through onError.
 */
public interface IInstallCertCallback {
    /**
     * This method will be called if the install WPJ certificate activity returns a status and no error message.
     *
     * @param isCertInstalled  it is true if the operation succeeds and the certificate was installed, otherwise false.
     */
    void onSuccess(final boolean isCertInstalled);

    /**
     * This method will be called if the install WPJ certificate activity returns a error message.
     *
     * @param exception the error that causes the installation of the WPJ certificate to fail
     */
    void onError(@NonNull final BaseException exception);
}
