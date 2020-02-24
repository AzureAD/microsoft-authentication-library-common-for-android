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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.microsoft.identity.common.adal.internal.AuthenticationConstants;
import com.microsoft.identity.common.internal.logging.DiagnosticContext;
import com.microsoft.identity.common.internal.logging.Logger;
import com.microsoft.identity.common.internal.telemetry.Telemetry;
import com.microsoft.identity.common.internal.telemetry.events.UiEndEvent;

import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.AuthorizationIntentAction.CANCEL_INTERACTIVE_REQUEST;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.AuthorizationIntentAction.RETURN_INTERACTIVE_REQUEST_RESULT;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.AuthorizationIntentKey.REQUEST_CANCELLED_BY_USER;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.AuthorizationIntentKey.REQUEST_CODE;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.AuthorizationIntentKey.RESULT_CODE;

/**
 * This base classes
 *  - handles how AuthorizationFragments communicates with the outside world.
 *  - handles basic lifecycle operations.
 * */
public abstract class AuthorizationFragment extends Fragment {

    private static final String TAG = AuthorizationFragment.class.getSimpleName();

    /**
     * The bundle containing values for initializing this fragment.
     */
    private Bundle mInstanceState;

    /**
     * Determines if authentication result has been sent.
     */
    private boolean mAuthResultSent = false;

    /**
     * Listens to an operation cancellation event.
     */
    private BroadcastReceiver mCancelRequestReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            cancelAuthorization(intent.getBooleanExtra(REQUEST_CANCELLED_BY_USER, false));
        }
    };

    void setInstanceState(@NonNull final Bundle instanceStateBundle) {
        mInstanceState = instanceStateBundle;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        final String methodName = "#onCreate";
        super.onCreate(savedInstanceState);

        // Register Broadcast receiver to cancel the auth request
        // if another incoming request is launched by the app
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(mCancelRequestReceiver,
                new IntentFilter(CANCEL_INTERACTIVE_REQUEST));

        if (savedInstanceState == null) {
            Logger.verbose(TAG + methodName, "Extract state from the intent bundle.");
            extractState(mInstanceState);
        } else {
            // If activity is killed by the os, savedInstance will be the saved bundle.
            Logger.verbose(TAG + methodName, "Extract state from the saved bundle.");
            extractState(savedInstanceState);
        }
    }

    void finish() {
        final FragmentActivity activity = getActivity();
        if (activity instanceof AuthorizationActivity) {
            activity.finish();
        } else {
            // The calling activity is not owned by MSAL/Broker.
            // Just remove this fragment.
            getFragmentManager()
                    .beginTransaction()
                    .setTransitionStyle(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                    .remove(this)
                    .commit();
        }
    }

    void extractState(final Bundle state) {
        if (state == null) {
            Logger.warn(TAG, "No stored state. Unable to handle response");
            finish();
            return;
        }

        setDiagnosticContextForNewThread(state.getString(DiagnosticContext.CORRELATION_ID));
    }

    /**
     * When authorization fragment is launched.  It will be launched on a new thread. (TODO: verify this)
     * Initialize based on value provided in intent.
     */
    private static String setDiagnosticContextForNewThread(String correlationId) {
        final String methodName = ":setDiagnosticContextForAuthorizationActivity";
        final com.microsoft.identity.common.internal.logging.RequestContext rc =
                new com.microsoft.identity.common.internal.logging.RequestContext();
        rc.put(DiagnosticContext.CORRELATION_ID, correlationId);
        DiagnosticContext.setRequestContext(rc);
        Logger.verbose(
                TAG + methodName,
                "Initializing diagnostic context for AuthorizationActivity"
        );

        return correlationId;
    }

    @Override
    public void onStop() {
        final String methodName = ":onStop";
        if (!mAuthResultSent && getActivity().isFinishing()) {
            Logger.info(TAG + methodName,
                    "Hosting Activity is destroyed before Auth request is completed, sending request cancel"
            );
            Telemetry.emit(new UiEndEvent().isUserCancelled());
            sendResult(AuthenticationConstants.UIResponse.BROWSER_CODE_SDK_CANCEL, new Intent());
        }
        super.onStop();
    }

    @Override
    public void onDestroy() {
        final String methodName = "#onDestroy";
        Logger.info(TAG + methodName, "");
        if (!mAuthResultSent) {
            Logger.info(TAG + methodName,
                    "Hosting Activity is destroyed before Auth request is completed, sending request cancel"
            );
            Telemetry.emit(new UiEndEvent().isUserCancelled());
            sendResult(AuthenticationConstants.UIResponse.BROWSER_CODE_SDK_CANCEL, new Intent());
        }

        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(mCancelRequestReceiver);
        super.onDestroy();
    }

    /**
     * NOTE: Fragment-only mode will not support this, as we don't own the activity.
     * This must be invoked by AuthorizationActivity.onBackPressed().
     */
    public boolean onBackPressed() {
        return false;
    }

    void sendResult(int resultCode, final Intent resultIntent) {
        Logger.info(TAG, "Sending result from Authorization Activity, resultCode: " + resultCode);

        resultIntent.setAction(RETURN_INTERACTIVE_REQUEST_RESULT);
        resultIntent.putExtra(REQUEST_CODE, AuthenticationConstants.UIRequest.BROWSER_FLOW);
        resultIntent.putExtra(RESULT_CODE, resultCode);

        LocalBroadcastManager.getInstance(getContext()).sendBroadcast(resultIntent);
        mAuthResultSent = true;
    }

    void cancelAuthorization(boolean isCancelledByUser) {
        final Intent resultIntent = new Intent();
        resultIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        if (isCancelledByUser){
            Logger.info(TAG, "Received Authorization flow cancelled by the user");
            sendResult(AuthenticationConstants.UIResponse.BROWSER_CODE_CANCEL, resultIntent);
        } else {
            Logger.info(TAG, "Received Authorization flow cancel request from SDK");
            sendResult(AuthenticationConstants.UIResponse.BROWSER_CODE_SDK_CANCEL, resultIntent);
        }

        Telemetry.emit(new UiEndEvent().isUserCancelled());
        finish();
    }
}
