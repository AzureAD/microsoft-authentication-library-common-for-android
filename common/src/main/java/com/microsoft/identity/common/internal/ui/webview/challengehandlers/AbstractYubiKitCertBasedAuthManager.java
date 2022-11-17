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

import androidx.annotation.NonNull;

import com.microsoft.identity.common.java.opentelemetry.CertBasedAuthTelemetryHelper;
import com.microsoft.identity.common.logging.Logger;
import com.yubico.yubikit.core.util.Callback;
import com.yubico.yubikit.core.util.Result;
import com.yubico.yubikit.piv.PivSession;
import com.yubico.yubikit.piv.jca.PivProvider;

import java.security.Security;

/**
 * Utilizes YubiKit in order to detect and interact with YubiKeys for smartcard certificate based authentication.
 */
public abstract class AbstractYubiKitCertBasedAuthManager extends AbstractSmartcardCertBasedAuthManager {

    private static final String TAG = AbstractYubiKitCertBasedAuthManager.class.getSimpleName();
    protected static final String YUBIKEY_PROVIDER = "YKPiv";

    /**
     * Adds a PivProvider instance to the Java static Security List (and emits relevant telemetry).
     * @param telemetryHelper CertBasedAuthTelemetryHelper instance.
     */
    @Override
    public void initBeforeProceedingWithRequest(@NonNull final CertBasedAuthTelemetryHelper telemetryHelper) {
        final String methodTag = TAG + ":initBeforeProceedingWithRequest";
        //Need to add a PivProvider instance to the beginning of the array of Security providers in order for signature logic to occur.
        //Note that this provider is removed when the UsbYubiKeyDevice connection is closed.
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
        Security.insertProviderAt(new PivProvider(getPivProviderCallback()), 1);
        Logger.info(methodTag, "An instance of PivProvider was added to Security static list.");
    }

    /**
     * Used to provide PivProvider constructor a Callback that will establish a new PivSession when it is needed.
     * @return A Callback which returns a Callback that will return a new PivSession instance.
     */
    abstract Callback<Callback<Result<PivSession, Exception>>> getPivProviderCallback();
}
