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
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.microsoft.identity.common.internal.telemetry.Telemetry;
import com.microsoft.identity.common.internal.telemetry.events.UiEndEvent;
import com.microsoft.identity.common.internal.util.FindBugsConstants;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.exception.ErrorStrings;
import com.microsoft.identity.common.java.providers.RawAuthorizationResult;
import com.microsoft.identity.common.java.util.UrlUtil;
import com.microsoft.identity.common.logging.Logger;

import java.util.Map;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.AuthorizationIntentAction.REDIRECT_RETURNED_ACTION;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.AuthorizationIntentKey.AUTH_INTENT;
import static com.microsoft.identity.common.java.AuthenticationConstants.AAD.APP_LINK_KEY;

/**
 * Authorization fragment with customTabs or browsers.
 */
public class CurrentTaskBrowserAuthorizationFragment extends AuthorizationFragment {

    private static final String TAG = CurrentTaskBrowserAuthorizationFragment.class.getSimpleName();

    private static final String BROWSER_FLOW_STARTED = "browserFlowStarted";

    /**
     * Determines if the flow has started.
     */
    private boolean mBrowserFlowStarted = false;

    private Intent mAuthIntent;

    private boolean mResponseReceived = false;

    /**
     * Creates an intent to handle the completion of an authorization flow with browser.
     * This restores the activity that hosts AuthorizationFragment that was created at the start of the flow.
     *
     * @param context     the package context for the app.
     * @param responseUri the response URI, which carries the parameters describing the response.
     */
    @Nullable
    public static Intent createCustomTabResponseIntent(@NonNull final Context context,
                                                       @NonNull final String responseUri) {

        final Intent intent = new Intent(context, CurrentTaskAuthorizationActivity.class);
        intent.setAction(REDIRECT_RETURNED_ACTION);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NO_HISTORY);
        intent.putExtra("RESPONSE_URI", responseUri);
        return intent;
    }

    @SuppressFBWarnings(FindBugsConstants.NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE)
    @Override
    public void onCreate(final @Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle arguments = this.getArguments();
        if(arguments != null){
            mResponseReceived = arguments.getBoolean("RESPONSE", false);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(AUTH_INTENT, mAuthIntent);
        outState.putBoolean(BROWSER_FLOW_STARTED, mBrowserFlowStarted);
    }

    @Override
    void extractState(@NonNull final Bundle state) {
        super.extractState(state);
        mAuthIntent = state.getParcelable(AUTH_INTENT);
        mBrowserFlowStarted = state.getBoolean(BROWSER_FLOW_STARTED, false);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mResponseReceived) {
            // The custom tab was closed without getting a result.
            finish();
        }

        /*
         * If the Authorization Agent is set as Default or Browser,
         * and this is the first run of the activity, start the authorization intent with customTabs or browsers.
         *
         * When it returns back to this Activity from OAuth2 redirect, two scenarios would happen.
         * 1) The response uri is returned from BrowserTabActivity
         * 2) The authorization is cancelled by pressing the 'Back' button or the BrowserTabActivity is not launched.
         *
         * In the first case, generate the authorization result from the response uri.
         * In the second case, set the activity result intent with AUTH_CODE_CANCEL code.
         */
        //This check is needed when using customTabs or browser flow.
        if (!mBrowserFlowStarted) {
            mBrowserFlowStarted = true;
            if (mAuthIntent != null) {
                // We cannot start browser activity inside OnCreate().
                // Because the life cycle of the current activity will continue and onResume will be called before finishing the login in browser.
                // This is by design of Android OS.
                startActivity(mAuthIntent);
            } else {
                sendResult(RawAuthorizationResult.fromException(
                        new ClientException(ErrorStrings.AUTHORIZATION_INTENT_IS_NULL)));
                finish();
            }
        } else {
            cancelAuthorization(true);
        }
    }

    public void completeAuthorizationInBrowserFlow(@NonNull final String customTabResponseUri) {
        final String methodTag = TAG + ":completeAuthorizationInBrowserFlow";
        Logger.info(methodTag, null, "Received redirect from customTab/browser.");

        final RawAuthorizationResult data = RawAuthorizationResult.fromRedirectUri(customTabResponseUri);
        switch (data.getResultCode()){
            case BROKER_INSTALLATION_TRIGGERED:
                final Map<String, String> urlQueryParameters = UrlUtil.getParameters(data.getAuthorizationFinalUri());
                final String appLink = urlQueryParameters.get(APP_LINK_KEY);
                final Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(appLink));
                startActivity(browserIntent);
                break;

            case COMPLETED:
                Telemetry.emit(new UiEndEvent().isUiComplete());
                break;

            case CANCELLED:
                Telemetry.emit(new UiEndEvent().isUserCancelled());
                break;

            default:
                break;
        }

        sendResult(data);
        finish();
    }
}