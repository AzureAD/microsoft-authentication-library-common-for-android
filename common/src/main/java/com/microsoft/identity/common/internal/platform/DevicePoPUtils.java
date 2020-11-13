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
import com.microsoft.identity.common.internal.authscheme.IPoPAuthenticationSchemeParams;
import com.microsoft.identity.common.internal.result.GenerateShrResult;
import com.microsoft.identity.common.internal.util.IClockSkewManager;

import java.net.URL;

public class DevicePoPUtils {

    private DevicePoPUtils() {
        // Utility class.
    }

    /**
     * Generates an AT-less SHR using the PoPMgr's internal signing key.
     *
     * @param context          The current application's {@link Context}.
     * @param clockSkewManager An instance of {@link IClockSkewManager}, used to mitigate
     *                         clock-skew/drift.
     * @param popSchemeParams  The input params used to create the resulting SHR.
     * @return The {@link GenerateShrResult} containing the resulint SHR.
     * @throws ClientException If an error is encountered.
     */
    public static synchronized GenerateShrResult generateSignedHttpRequest(
            @NonNull final Context context,
            @NonNull final IClockSkewManager clockSkewManager,
            @NonNull final IPoPAuthenticationSchemeParams popSchemeParams) throws ClientException {
        // Clock-skew correction values
        final long ONE_SECOND_MILLIS = 1000L;
        final long timestampMillis = clockSkewManager.getAdjustedReferenceTime().getTime();

        final String httpMethodStr = popSchemeParams.getHttpMethod();
        final URL resourceUrl = popSchemeParams.getUrl();
        final String nonce = popSchemeParams.getNonce();
        final String clientClaims = popSchemeParams.getClientClaims();
        final IDevicePopManager popMgr = Device.getDevicePoPManagerInstance();

        // Generate keys, if none exist (should already be initialized)
        if (!popMgr.asymmetricKeyExists()) {
            popMgr.generateAsymmetricKey(context);
        }

        final String shr = popMgr.mintSignedHttpRequest(
                httpMethodStr,
                timestampMillis / ONE_SECOND_MILLIS,
                resourceUrl,
                nonce,
                clientClaims
        );

        // Create our result object
        final GenerateShrResult result = new GenerateShrResult();
        result.setShr(shr);

        return result;
    }
}
