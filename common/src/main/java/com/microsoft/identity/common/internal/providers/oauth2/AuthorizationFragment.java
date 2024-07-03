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
import android.view.View;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.microsoft.identity.common.internal.telemetry.Telemetry;
import com.microsoft.identity.common.internal.telemetry.events.UiEndEvent;
import com.microsoft.identity.common.java.logging.RequestContext;
import com.microsoft.identity.common.java.providers.RawAuthorizationResult;
import com.microsoft.identity.common.java.util.ported.PropertyBag;
import com.microsoft.identity.common.java.util.ported.LocalBroadcaster;
import com.microsoft.identity.common.java.logging.DiagnosticContext;
import com.microsoft.identity.common.logging.Logger;

import static com.microsoft.identity.common.java.AuthenticationConstants.LocalBroadcasterAliases.CANCEL_AUTHORIZATION_REQUEST;
import static com.microsoft.identity.common.java.AuthenticationConstants.LocalBroadcasterAliases.RETURN_AUTHORIZATION_REQUEST_RESULT;
import static com.microsoft.identity.common.java.AuthenticationConstants.LocalBroadcasterFields.REQUEST_CODE;
import static com.microsoft.identity.common.java.AuthenticationConstants.SdkPlatformFields.PRODUCT;
import static com.microsoft.identity.common.java.AuthenticationConstants.SdkPlatformFields.VERSION;
import static com.microsoft.identity.common.java.AuthenticationConstants.UIRequest.BROWSER_FLOW;

/**
 * This base classes
 * - handles how AuthorizationFragments communicates with the outside world.
 * - handles basic lifecycle operations.
 */
public abstract class AuthorizationFragment extends Fragment {

    private static final String TAG = AuthorizationFragment.class.getSimpleName();

    /**
     * The bundle containing values for initializing this fragment.
     */
    private Bundle mInstanceState;

    /**
     * Determines if authentication result has been sent.
     */
    protected boolean mAuthResultSent = false;

    /**
     * Listens to an operation cancellation event.
     */
    private final LocalBroadcaster.IReceiverCallback mCancelRequestReceiver = new LocalBroadcaster.IReceiverCallback() {
        @Override
        public void onReceive(@NonNull final PropertyBag propertyBag) {
            cancelAuthorization(propertyBag.getOrDefault(CANCEL_AUTHORIZATION_REQUEST, false));
        }
    };

    void setInstanceState(@NonNull final Bundle instanceStateBundle) {
        mInstanceState = instanceStateBundle;
    }

    @Override
    public void onCreate(@Nullable final Bundle savedInstanceState) {
        final String methodTag = TAG + ":onCreate";
        super.onCreate(savedInstanceState);

        // Register Broadcast receiver to cancel the auth request
        // if another incoming request is launched by the app
        LocalBroadcaster.INSTANCE.registerCallback(CANCEL_AUTHORIZATION_REQUEST, mCancelRequestReceiver);

        if (savedInstanceState == null && mInstanceState == null) {
            Logger.warn(methodTag, "No stored state. Unable to handle response");
            finish();
            return;
        }

        if (savedInstanceState == null) {
            Logger.verbose(methodTag, "Extract state from the intent bundle.");
            extractState(mInstanceState);
        } else {
            // If activity is killed by the os, savedInstance will be the saved bundle.
            Logger.verbose(methodTag, "Extract state from the saved bundle.");
            extractState(savedInstanceState);
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                handleBackButtonPressed();
            }
        });
    }

    void finish() {
        final String methodName = "#finish";
        LocalBroadcaster.INSTANCE.unregisterCallback(CANCEL_AUTHORIZATION_REQUEST);

        final FragmentActivity activity = getActivity();
        if (activity instanceof AuthorizationActivity) {
            activity.finish();
        } else {
            // The calling activity is not owned by MSAL/Broker.
            // Just remove this fragment.
            try {
                final FragmentManager fragmentManager = getFragmentManager();
                if (fragmentManager != null) {
                    fragmentManager
                            .beginTransaction()
                            .setTransitionStyle(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                            .remove(this)
                            .commitNow();
                }
            }catch(Exception e){
                /*
                MATS Telemetry indicated that the normal call to commit() which is async occasionally
                results in an IllegalStateException.  Current theory is that because we previously were
                user commit() rather than commitNow() that the fragment manager that we were removing
                ourselves from was already gone...

                Logging being added here to hopefully to make a more definitive determination of root cause.
                https://identitydivision.visualstudio.com/Engineering/_workitems/edit/1695851
                 */
                Logger.error(TAG + methodName, "Logged as error to capture 'cause'; Exception occurred when removing ourselves from provided FragmentManager", e);
            }
        }
    }

    /**
     * Get the state form the provided bundle and act on it as needed
     * @param state a bundle containing data provided when the activity was created
     */
    void extractState(@NonNull final Bundle state) {
        setDiagnosticContextForNewThread(state.getString(DiagnosticContext.CORRELATION_ID));
    }

    /**
     * When authorization fragment is launched.  It will be launched on a new thread. (TODO: verify this)
     * Initialize based on value provided in intent.
     */
    private static void setDiagnosticContextForNewThread(final String correlationId) {
        final String methodTag = TAG + ":setDiagnosticContextForAuthorizationActivity";
        final RequestContext rc = new RequestContext();
        rc.put(DiagnosticContext.CORRELATION_ID, correlationId);
        DiagnosticContext.INSTANCE.setRequestContext(rc);
        Logger.verbose(
                methodTag,
                "Initializing diagnostic context for AuthorizationActivity"
        );
    }

    @Override
    public void onStop() {
        final String methodTag = TAG + ":onStop";
        final FragmentActivity activity = getActivity();
        if (!mAuthResultSent && (activity == null || activity.isFinishing())) {
            Logger.info(methodTag,
                    "Hosting Activity is destroyed before Auth request is completed, sending request cancel"
            );
            Telemetry.emit(new UiEndEvent().isUserCancelled());
            sendResult(RawAuthorizationResult.ResultCode.SDK_CANCELLED);
        }
        super.onStop();
    }

    @Override
    public void onDestroy() {
        final String methodTag = TAG + ":onDestroy";
        Logger.info(methodTag, "");
        if (!mAuthResultSent) {
            Logger.info(methodTag,
                    "Hosting Activity is destroyed before Auth request is completed, sending request cancel"
            );
            Telemetry.emit(new UiEndEvent().isUserCancelled());
            sendResult(RawAuthorizationResult.ResultCode.SDK_CANCELLED);
        }

        LocalBroadcaster.INSTANCE.unregisterCallback(CANCEL_AUTHORIZATION_REQUEST);
        super.onDestroy();
    }

    public void handleBackButtonPressed() {
        cancelAuthorization(true);
    }

    void sendResult(final RawAuthorizationResult.ResultCode resultCode) {
        sendResult(RawAuthorizationResult.fromResultCode(resultCode));
    }

    void sendResult(@NonNull final RawAuthorizationResult result) {
        final String methodTag = TAG + ":sendResult";
        Logger.info(methodTag, "Sending result from Authorization Activity, resultCode: " + result.getResultCode());

        final PropertyBag propertyBag = RawAuthorizationResult.toPropertyBag(result);
        propertyBag.put(REQUEST_CODE, BROWSER_FLOW);

        LocalBroadcaster.INSTANCE.broadcast(RETURN_AUTHORIZATION_REQUEST_RESULT, propertyBag);
        mAuthResultSent = true;
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
