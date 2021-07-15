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

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;

import com.microsoft.identity.common.internal.telemetry.Telemetry;
import com.microsoft.identity.common.internal.telemetry.events.UiEndEvent;
import com.microsoft.identity.common.internal.util.FindBugsConstants;
import com.microsoft.identity.common.java.logging.RequestContext;
import com.microsoft.identity.common.java.providers.RawAuthorizationResult;
import com.microsoft.identity.common.java.util.ported.DataBag;
import com.microsoft.identity.common.java.util.ported.LocalBroadcaster;
import com.microsoft.identity.common.logging.DiagnosticContext;
import com.microsoft.identity.common.logging.Logger;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import static com.microsoft.identity.common.java.AuthenticationConstants.LobalBroadcasterAliases.CANCEL_AUTHORIZATION_REQUEST;
import static com.microsoft.identity.common.java.AuthenticationConstants.LobalBroadcasterAliases.RETURN_AUTHORIZATION_REQUEST_RESULT;
import static com.microsoft.identity.common.java.AuthenticationConstants.LocalBroadcasterFields.REQUEST_CODE;
import static com.microsoft.identity.common.java.AuthenticationConstants.UIRequest.BROWSER_FLOW;

public abstract class CurrentTaskAuthorizationFragment extends AuthorizationFragment {

    private static final String TAG = CurrentTaskAuthorizationFragment.class.getSimpleName();

    /**
     * The bundle containing values for initializing this fragment.
     */
    private Bundle mInstanceState;

    /**
     * Listens to an operation cancellation event.
     */
    private final LocalBroadcaster.IReceiverCallback mCancelRequestReceiver = new LocalBroadcaster.IReceiverCallback() {
        @Override
        public void onReceive(@NonNull final DataBag dataBag) {
            cancelAuthorization(dataBag.getBooleanMap().getOrDefault(CANCEL_AUTHORIZATION_REQUEST, false));
        }
    };

    void setInstanceState(@NonNull final Bundle instanceStateBundle) {
        mInstanceState = instanceStateBundle;
    }

    @SuppressFBWarnings(FindBugsConstants.NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE)
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        final String methodName = "#onCreate";
        super.onCreate(savedInstanceState);

        // Register Broadcast receiver to cancel the auth request
        // if another incoming request is launched by the app
        LocalBroadcaster.INSTANCE.registerCallback(CANCEL_AUTHORIZATION_REQUEST, mCancelRequestReceiver);

        if (savedInstanceState == null) {
            Logger.verbose(TAG + methodName, "Extract state from the intent bundle.");
            extractState(mInstanceState);
        } else {
            // If activity is killed by the os, savedInstance will be the saved bundle.
            Logger.verbose(TAG + methodName, "Extract state from the saved bundle.");
            extractState(savedInstanceState);
        }
    }

    @SuppressFBWarnings(FindBugsConstants.NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE)
    void finish() {
        final FragmentActivity activity = getActivity();
        if (activity instanceof AuthorizationActivity) {
            activity.finish();
        } else {
            // The calling activity is not owned by MSAL/Broker.
            // Just remove this fragment.
            if (getFragmentManager() != null) {
                getFragmentManager()
                        .beginTransaction()
                        .setTransitionStyle(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                        .remove(this)
                        .commit();
            }
        }
    }

    /**
     * Get the state form the provided bundle and act on it as needed
     *
     * @param state a bundle containing data provided when the activity was created
     */
    void extractState(@NonNull final Bundle state) {
        setDiagnosticContextForNewThread(state.getString(DiagnosticContext.CORRELATION_ID));
    }

    /**
     * When authorization fragment is launched.  It may be launched on a new thread.
     * Initialize based on value provided in intent.
     */
    private static String setDiagnosticContextForNewThread(@NonNull final String correlationId) {
        final String methodName = ":setDiagnosticContextForAuthorizationActivity";
        final RequestContext rc = new RequestContext();
        rc.put(DiagnosticContext.CORRELATION_ID, correlationId);
        DiagnosticContext.setRequestContext(rc);
        Logger.verbose(
                TAG + methodName,
                "Initializing diagnostic context for CurrentTaskAuthorizationActivity"
        );

        return correlationId;
    }

    @SuppressFBWarnings(FindBugsConstants.NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE)
    @Override
    public void onDestroy() {
        LocalBroadcaster.INSTANCE.unregisterCallback(CANCEL_AUTHORIZATION_REQUEST);
        super.onDestroy();
    }

    /**
     * NOTE: Fragment-only mode will not support this, as we don't own the activity.
     * This must be invoked by AuthorizationActivity.onBackPressed().
     */
    public boolean onBackPressed() {
        return false;
    }

    void sendResult(final RawAuthorizationResult.ResultCode resultCode) {
        sendResult(RawAuthorizationResult.fromResultCode(resultCode));
    }

    void sendResult(@NonNull final RawAuthorizationResult result) {
        Logger.info(TAG, "Sending result from Authorization Activity, resultCode: " + result.getResultCode());

        final DataBag dataBag = RawAuthorizationResult.toDataBag(result);
        dataBag.getIntMap().put(REQUEST_CODE, BROWSER_FLOW);

        LocalBroadcaster.INSTANCE.broadcast(RETURN_AUTHORIZATION_REQUEST_RESULT, dataBag);
    }

    void cancelAuthorization(final boolean isCancelledByUser) {
        if (isCancelledByUser) {
            Logger.info(TAG, "Received Authorization flow cancelled by the user");
            sendResult(RawAuthorizationResult.ResultCode.CANCELLED);
        } else {
            Logger.info(TAG, "Received Authorization flow cancel request from SDK");
            sendResult(RawAuthorizationResult.ResultCode.SDK_CANCELLED);
        }

        Telemetry.emit(new UiEndEvent().isUserCancelled());
        finish();
    }
}
