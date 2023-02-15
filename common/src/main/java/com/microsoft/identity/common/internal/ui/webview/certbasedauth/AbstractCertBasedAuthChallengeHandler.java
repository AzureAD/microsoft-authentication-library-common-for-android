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

import android.webkit.ClientCertRequest;

import androidx.annotation.NonNull;

import com.microsoft.identity.common.internal.ui.webview.challengehandlers.IChallengeHandler;
import com.microsoft.identity.common.java.exception.BaseException;
import com.microsoft.identity.common.java.opentelemetry.ICertBasedAuthTelemetryHelper;
import com.microsoft.identity.common.java.providers.RawAuthorizationResult;

/**
 * ChallengeHandler extended abstract class specifically for certificate based authentication (CBA)
 *  implementations.
 */
public abstract class AbstractCertBasedAuthChallengeHandler implements IChallengeHandler<ClientCertRequest, Void> {
    protected boolean mIsCertBasedAuthProceeding;
    protected ICertBasedAuthTelemetryHelper mTelemetryHelper;
    /**
     * Emit telemetry for results from certificate based authentication (CBA) if CBA occurred.
     * @param response a RawAuthorizationResult object received upon a challenge response received.
     */
    public void emitTelemetryForCertBasedAuthResults(@NonNull final RawAuthorizationResult response) {
        if (!mIsCertBasedAuthProceeding) {
            return;
        }
        final RawAuthorizationResult.ResultCode resultCode = response.getResultCode();
        if (resultCode == RawAuthorizationResult.ResultCode.NON_OAUTH_ERROR
                || resultCode == RawAuthorizationResult.ResultCode.SDK_CANCELLED
                || resultCode == RawAuthorizationResult.ResultCode.CANCELLED) {
            final BaseException exception = response.getException();
            if (exception != null) {
                mTelemetryHelper.setResultFailure(exception);
                return;
            }
            //Putting result code as message.
            mTelemetryHelper.setResultFailure(resultCode.toString());
            return;
        }
        mTelemetryHelper.setResultSuccess();
    }

    /**
     * Clean up logic to run when AbstractCertBasedAuthChallengeHandler is no longer going to be used.
     */
    public abstract void cleanUp();
}
