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
package com.microsoft.identity.common.internal.ui.webview.certbasedauth;

import androidx.annotation.NonNull;

import com.microsoft.identity.common.java.opentelemetry.CertBasedAuthTelemetryHelper;
import com.microsoft.identity.common.logging.Logger;
import com.yubico.yubikit.core.util.Callback;
import com.yubico.yubikit.core.util.Result;
import com.yubico.yubikit.piv.PivSession;
import com.yubico.yubikit.piv.jca.PivProvider;

import java.security.Security;

/**
 * Manages the addition and removal of YubiKey PIV provider instances in the Java Security static list.
 */
public class YubiKeyPivProviderManager {

    private static final String TAG = YubiKeyPivProviderManager.class.getSimpleName();
    protected static final String YUBIKEY_PROVIDER = "YKPiv";

    /**
     * Add a YubiKey PIV provider, and remove any existing copies of the provider.
     * @param telemetryHelper CertBasedAuthTelemetryHelper instance.
     * @param pivProviderCallback A Callback which returns a Callback that will return a new PivSession instance.
     */
    public static void addPivProvider(@NonNull final CertBasedAuthTelemetryHelper telemetryHelper,
                                      @NonNull final Callback<Callback<Result<PivSession, Exception>>> pivProviderCallback) {
        final String methodTag = TAG + ":addPivProvider";
        if (Security.getProvider(YUBIKEY_PROVIDER) != null) {
            Security.removeProvider(YUBIKEY_PROVIDER);
            //The PivProvider instance is either unexpectedly being added elsewhere
            // or it isn't being removed properly upon CBA flow termination.
            telemetryHelper.setExistingPivProviderPresent(true);
            Logger.info(methodTag, "Existing PivProvider was present in Security static list.");
        } else {
            telemetryHelper.setExistingPivProviderPresent(false);
            Logger.info(methodTag, "Security static list does not have existing PivProvider.");
        }
        //The position parameter is 1-based (1 maps to index 0).
        Security.insertProviderAt(new PivProvider(pivProviderCallback), 1);
        Logger.info(methodTag, "An instance of PivProvider was added to Security static list.");
    }

    /**
     * Removes existing YubiKey PIV provider from static list, if present.
     */
    public static void removePivProvider() {
        if (Security.getProvider(YUBIKEY_PROVIDER) == null) {
            return;
        }
        Security.removeProvider(YUBIKEY_PROVIDER);
        Logger.info(TAG, "An instance of PivProvider was removed from Security static list.");
    }
}
