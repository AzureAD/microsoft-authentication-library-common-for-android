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
package com.microsoft.identity.common.internal.providers.oauth2;

import static com.microsoft.identity.common.java.AuthenticationConstants.LocalBroadcasterAliases.RETURN_AUTHORIZATION_REQUEST_RESULT;
import static com.microsoft.identity.common.java.AuthenticationConstants.LocalBroadcasterFields.REQUEST_CODE;
import static com.microsoft.identity.common.java.AuthenticationConstants.UIRequest.BROWSER_FLOW;

import androidx.annotation.NonNull;

import com.microsoft.identity.common.internal.telemetry.Telemetry;
import com.microsoft.identity.common.internal.telemetry.events.UiEndEvent;
import com.microsoft.identity.common.java.providers.RawAuthorizationResult;
import com.microsoft.identity.common.java.util.ported.LocalBroadcaster;
import com.microsoft.identity.common.java.util.ported.PropertyBag;
import com.microsoft.identity.common.logging.Logger;

public abstract class CurrentTaskAuthorizationFragment extends AuthorizationFragment {

    private static final String TAG = CurrentTaskAuthorizationFragment.class.getSimpleName();

    void sendResult(final RawAuthorizationResult.ResultCode resultCode) {
        sendResult(RawAuthorizationResult.fromResultCode(resultCode));
    }

    void sendResult(@NonNull final RawAuthorizationResult result) {
        final String methodTag = TAG + ":sendResult";
        Logger.info(methodTag, "Sending result from Authorization Activity, resultCode: " + result.getResultCode());

        final PropertyBag propertyBag = RawAuthorizationResult.toPropertyBag(result);
        propertyBag.put(REQUEST_CODE, BROWSER_FLOW);

        LocalBroadcaster.INSTANCE.broadcast(RETURN_AUTHORIZATION_REQUEST_RESULT, propertyBag);
    }

    void cancelAuthorization(final boolean isCancelledByUser) {
        final String methodTag = TAG + ":cancelAuthorization";
        if (isCancelledByUser) {
            Logger.info(methodTag, "Received Authorization flow cancelled by the user");
            sendResult(RawAuthorizationResult.ResultCode.CANCELLED);
        } else {
            Logger.info(methodTag, "Received Authorization flow cancel request from SDK");
            sendResult(RawAuthorizationResult.ResultCode.SDK_CANCELLED);
        }

        Telemetry.emit(new UiEndEvent().isUserCancelled());
        finish();
    }
}
