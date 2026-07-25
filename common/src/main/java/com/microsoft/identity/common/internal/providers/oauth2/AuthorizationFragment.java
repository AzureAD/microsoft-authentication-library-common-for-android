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

import android.content.Context;
import android.os.Bundle;
import android.view.View;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.microsoft.identity.common.adal.internal.AuthenticationConstants;
import com.microsoft.identity.common.internal.telemetry.Telemetry;
import com.microsoft.identity.common.internal.telemetry.events.UiEndEvent;
import com.microsoft.identity.common.java.logging.RequestContext;
import com.microsoft.identity.common.java.providers.MamInstallReferrerBuilder;
import com.microsoft.identity.common.java.providers.RawAuthorizationResult;
import com.microsoft.identity.common.java.util.ported.PropertyBag;
import com.microsoft.identity.common.java.util.ported.LocalBroadcaster;
import com.microsoft.identity.common.java.logging.DiagnosticContext;
import com.microsoft.identity.common.logging.Logger;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.microsoft.identity.common.java.AuthenticationConstants.LocalBroadcasterAliases.CANCEL_AUTHORIZATION_REQUEST;
import static com.microsoft.identity.common.java.AuthenticationConstants.LocalBroadcasterAliases.RETURN_AUTHORIZATION_REQUEST_RESULT;
import static com.microsoft.identity.common.java.AuthenticationConstants.LocalBroadcasterFields.REQUEST_CODE;
import static com.microsoft.identity.common.java.AuthenticationConstants.UIRequest.BROWSER_FLOW;

import lombok.Getter;
import lombok.Setter;

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

            // Log hosting activity destruction in the url tracker
            updateLatestUrlStatus(null, "SDK_CANCELLED: Activity destroyed before auth completion");
        }

        LocalBroadcaster.INSTANCE.unregisterCallback(CANCEL_AUTHORIZATION_REQUEST);
        super.onDestroy();
    }

    public void handleBackButtonPressed() {
        cancelAuthorization(true);
    }

    /**
     * MAM broker-install request-resume: when the {@code ENABLE_BROKER_INSTALL_RESUME} flight is on, tag
     * the Company Portal install link with this app's package as the Play install referrer so Company Portal
     * can redirect back to us after install (CP-confirmed {@code &referrer=<originPkg>} pattern).
     * <p>
     * Shared by every {@link AuthorizationFragment} subclass that launches the broker install so the
     * flight-gate is evaluated in exactly one place ({@link MamInstallReferrerBuilder}). Null-safe: with the
     * flight off (or no attached context) the original link is returned unchanged.
     *
     * @param appLink the server-provided Play Store install link.
     * @return the decorated link when the flight is on, otherwise the original {@code appLink}.
     */
    protected String decorateInstallLinkWithReferrer(final String appLink) {
        final Context context = getContext();
        return MamInstallReferrerBuilder.decorateAppLinkWithOriginReferrerIfEnabled(
                appLink, context == null ? null : context.getPackageName());
    }

    void sendResult(final RawAuthorizationResult.ResultCode resultCode) {
        sendResult(RawAuthorizationResult.fromResultCode(resultCode));
    }

    void sendResult(@NonNull final RawAuthorizationResult result) {
        final String methodTag = TAG + ":sendResult";
        Logger.info(methodTag, "Sending result from Authorization Activity, resultCode: " + result.getResultCode());

        // Track the final result code we got for this authorization flow
        mFinalResultCode = result.getResultCode();

        final PropertyBag propertyBag = propertyBagFromAuthorizationResult(result);

        LocalBroadcaster.INSTANCE.broadcast(RETURN_AUTHORIZATION_REQUEST_RESULT, propertyBag);
        mAuthResultSent = true;
    }

    /**
     * Creates a {@link PropertyBag} from the given authorization result.
     * Subclasses may override to add additional fields.
     */
    @NonNull
    protected PropertyBag propertyBagFromAuthorizationResult(@NonNull final RawAuthorizationResult result) {
        final PropertyBag propertyBag = RawAuthorizationResult.toPropertyBag(result);
        propertyBag.put(REQUEST_CODE, BROWSER_FLOW);
        return propertyBag;
    }

    void cancelAuthorization(final boolean isCancelledByUser) {
        final String methodTag = TAG + ":cancelAuthorization";
        if (isCancelledByUser) {
            Logger.info(methodTag, "Received Authorization flow cancelled by the user");
            sendResult(RawAuthorizationResult.ResultCode.CANCELLED);

            // Log this in the url load status tracker
            updateLatestUrlStatus(null, "CANCELLED: Authorization cancelled by user.");
        } else {
            Logger.info(methodTag, "Received Authorization flow cancel request from SDK");
            sendResult(RawAuthorizationResult.ResultCode.SDK_CANCELLED);

            // Log this in the url load status tracker
            updateLatestUrlStatus(null, "SDK_CANCELLED: Authorization cancelled by SDK.");
        }

        Telemetry.emit(new UiEndEvent().isUserCancelled());
        finish();
    }

    /**
     * Tracks the URLs loaded in the WebView along with their load status.
     * Key: Load order (int), Value: URL Status object
     */
    private final Map<Integer, UrlStatus> mUrlStatusTracker = new LinkedHashMap<>();

    @Getter
    private RawAuthorizationResult.ResultCode mFinalResultCode;
    private int mUrlLoadCounter = 0;

    /**
     * Class to represent the URL loaded and whether or not it received a loading error or a server error
     */
    public static class UrlStatus {
        @Getter
        private final String url;

        /**
         * Error encountered during loading
         */
        @Getter
        @Setter
        private String loadingError;

        /**
         * Error returned from server
         */
        @Getter
        @Setter
        private String authError;

        UrlStatus(final String url, final String loadingError, final String authError) {
            this.url = sanitizeUrl(url);
            this.loadingError = loadingError;
            this.authError = authError;
        }

        @NonNull
        public String toString() {
            final StringBuilder sb = new StringBuilder();
            sb.append("url=").append(url);
            if (loadingError != null) {
                sb.append(", loadingError=").append(loadingError);
            }
            if (authError != null) {
                sb.append(", authError=").append(authError);
            }
            return sb.toString();
        }

        /**
         * Sanitize the URL to ensure no sensitive data is tracked.
         * Only allows URLs with known AAD/MSA host suffixes.
         * All query parameters are stripped for privacy.
         *
         * @param url The URL to sanitize.
         * @return The sanitized URL (scheme + host + path only) or "[REDACTED]" if host is not allowed.
         */
        private static String sanitizeUrl(final String url) {
            if (url == null || url.isEmpty()) {
                return url;
            }

            try {
                final URI uri = new URI(url);
                final String host = uri.getHost();

                if (host == null || host.isEmpty()) {
                    return host;
                }

                // Only allow URLs with known AAD/MSA host suffixes
                final boolean isAllowedHost =
                        host.endsWith(AuthenticationConstants.Broker.AAD_GLOBAL_URL_HOST_SUFFIX) ||
                        host.endsWith(AuthenticationConstants.Broker.AAD_INTUNE_MDM_URL_HOST_SUFFIX) ||
                        host.endsWith(AuthenticationConstants.Broker.AAD_US_URL_HOST_SUFFIX) ||
                        host.endsWith(AuthenticationConstants.Broker.AAD_CHINA_URL_HOST_SUFFIX) ||
                        host.endsWith(AuthenticationConstants.Broker.MSA_URL_HOST_SUFFIX);

                if (!isAllowedHost) {
                    return "[REDACTED]";
                }

                // Build sanitized URL: scheme + host + path only (no query params or fragments)
                final StringBuilder sanitizedUrl = new StringBuilder();
                final String scheme = uri.getScheme();
                if (scheme != null) {
                    sanitizedUrl.append(scheme).append("://");
                }
                sanitizedUrl.append(host);

                final String path = uri.getPath();
                if (path != null && !path.isEmpty()) {
                    sanitizedUrl.append(path);
                }

                return sanitizedUrl.toString();
            } catch (final Exception e) {
                // If URL parsing fails, redact the entire URL for safety
                return "[PARSING_ERROR]";
            }
        }
    }

    /**
     * Tracks a URL load event. Returns the index it was added at.
     *
     * @param url       The URL being loaded.
     * @param loadingError The error if the load failed (null if successful).
     * @param authError The error received from server-side.
     */
    protected void trackUrlStatus(final String url, final String loadingError, final String authError) {
        mUrlStatusTracker.put(++mUrlLoadCounter, new UrlStatus(url, loadingError, authError));
    }

    /**
     * Updates the most recent URL load event with new status information.
     *
     * @param loadingError The error if the load failed (null if successful).
     * @param authError The error received from server-side.
     */
    protected void updateLatestUrlStatus(final String loadingError, final String authError) {
        final UrlStatus latestStatus = mUrlStatusTracker.get(mUrlLoadCounter);

        if (latestStatus == null) {
            Logger.warn(TAG, "No URL load status to update.");
            return;
        }

        latestStatus.setLoadingError(loadingError);
        latestStatus.setAuthError(authError);

        mUrlStatusTracker.put(mUrlLoadCounter, latestStatus);
    }

    /**
     * Retrieves the tracked URL load events.
     *
     * @return A copy of the URL load tracker map.
     */
    public Map<Integer, UrlStatus> getUrlLoadTracker() {
        return new LinkedHashMap<>(mUrlStatusTracker);
    }
}

