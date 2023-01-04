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
package com.microsoft.identity.common.internal.ui.webview.challengehandlers;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.microsoft.identity.common.logging.Logger;
import com.yubico.yubikit.android.transport.nfc.NfcNotAvailable;

/**
 * Instantiates managers for certificate based authentication.
 */
public class SmartcardCertBasedAuthManagerFactory {

    private static final String TAG = SmartcardCertBasedAuthManagerFactory.class.getSimpleName();

    /**
     * Creates and returns an applicable instance of AbstractUsbSmartcardCertBasedAuthManager that handles USB connections.
     * @param context current Context.
     * @return An AbstractUsbSmartcardCertBasedAuthManager if the USB_SERVICE is supported by the device; null otherwise.
     */
    @Nullable
    static AbstractUsbSmartcardCertBasedAuthManager createUsbSmartcardCertBasedAuthManager(@NonNull final Context context) {
        final String methodTag = TAG + ":createUsbSmartcardCertBasedAuthManager";
        if (context.getSystemService(Context.USB_SERVICE) == null) {
            Logger.info(methodTag, "Certificate Based Authentication via YubiKey not enabled due to device not supporting the USB_SERVICE system service.");
            return null;
        }
        return new YubiKitUsbSmartcardCertBasedAuthManager(context);
    }

    /**
     * Creates and returns an applicable instance of AbstractNfcSmartcardCertBasedAuthManager that handles NFC connections.
     * @param context current Context.
     * @return An AbstractNfcSmartcardCertBasedAuthManager if NFC is supported on the device; null otherwise.
     */
    @Nullable
    static AbstractNfcSmartcardCertBasedAuthManager createNfcSmartcardCertBasedAuthManager(@NonNull final Context context) {
        final String methodTag = TAG + ":createNfcSmartcardCertBasedAuthManager";
        try {
            return new YubiKitNfcSmartcardCertBasedAuthManager(context);
        } catch (final NfcNotAvailable e) {
            //This means that the device does not have an NFC reader.
            Logger.info(methodTag, "Device does not support NFC capabilities.");
            return null;
        }
    }
}
