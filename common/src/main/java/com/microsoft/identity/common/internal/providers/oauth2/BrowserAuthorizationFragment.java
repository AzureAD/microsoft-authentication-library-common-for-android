package com.microsoft.identity.common.internal.providers.oauth2;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.microsoft.identity.common.adal.internal.AuthenticationConstants;
import com.microsoft.identity.common.adal.internal.util.StringExtensions;
import com.microsoft.identity.common.exception.ClientException;
import com.microsoft.identity.common.exception.ErrorStrings;
import com.microsoft.identity.common.internal.logging.Logger;
import com.microsoft.identity.common.internal.telemetry.Telemetry;
import com.microsoft.identity.common.internal.telemetry.events.UiEndEvent;
import com.microsoft.identity.common.internal.util.StringUtil;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.AuthorizationIntentKey.AUTHORIZATION_FINAL_URL;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.AuthorizationIntentKey.AUTH_INTENT;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.DEVICE_REGISTRATION_REDIRECT_URI_HOSTNAME;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.INSTALL_URL_KEY;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.REDIRECT_PREFIX;
import static com.microsoft.identity.common.internal.providers.oauth2.AuthorizationResultFactory.ERROR;
import static com.microsoft.identity.common.internal.providers.oauth2.AuthorizationResultFactory.ERROR_DESCRIPTION;
import static com.microsoft.identity.common.internal.providers.oauth2.AuthorizationResultFactory.ERROR_SUBCODE;

/**
 * Authorization fragment with customTabs or browsers.
 * */
public class BrowserAuthorizationFragment extends AuthorizationFragment {

    private static final String TAG = BrowserAuthorizationFragment.class.getSimpleName();

    @VisibleForTesting
    private static final String BROWSER_FLOW_STARTED = "browserFlowStarted";

    /**
     * Class of the Activity that hosts this fragment.
     * This needs to be static - see createCustomTabResponseIntent() for more details.
     */
    private static Class<?> sCallingActivityClass;

    /**
     * Response URI of the browser flow.
     * As we might not have any control over the calling Activity,
     * we can't rely on the content of the launching intent to provide us this value.
     */
    private static String sCustomTabResponseUri;

    /**
     * Determines if the flow has started.
     */
    private boolean mBrowserFlowStarted = false;

    private Intent mAuthIntent;

    /**
     * Creates an intent to handle the completion of an authorization flow with browser.
     * This restores the activity that hosts AuthorizationFragment that was created at the start of the flow.
     *
     * @param context     the package context for the app.
     * @param responseUri the response URI, which carries the parameters describing the response.
     */
    public static Intent createCustomTabResponseIntent(final Context context,
                                                       final String responseUri) {
        // We cannot pass this as part of a new intent, because we might not have any control over the calling activity.
        sCustomTabResponseUri = responseUri;

        final Intent intent = new Intent(context, sCallingActivityClass);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return intent;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sCallingActivityClass = this.getActivity().getClass();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(AUTH_INTENT, mAuthIntent);
        outState.putBoolean(BROWSER_FLOW_STARTED, mBrowserFlowStarted);
    }

    @Override
    void extractState(final Bundle state){
        super.extractState(state);
        mAuthIntent = state.getParcelable(AUTH_INTENT);
        mBrowserFlowStarted = state.getBoolean(BROWSER_FLOW_STARTED, false);
    }

    @Override
    public void onResume() {
        super.onResume();

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
                final Intent resultIntent = new Intent();
                resultIntent.putExtra(AuthenticationConstants.Browser.RESPONSE_AUTHENTICATION_EXCEPTION, new ClientException(ErrorStrings.AUTHORIZATION_INTENT_IS_NULL));
                sendResult(AuthenticationConstants.UIResponse.BROWSER_CODE_AUTHENTICATION_EXCEPTION, resultIntent);
                finish();
            }
        } else {
            if (!StringUtil.isEmpty(sCustomTabResponseUri)) {
                completeAuthorizationInBrowserFlow(sCustomTabResponseUri);
            } else {
                cancelAuthorization(true);
            }
        }
    }

    private void completeAuthorizationInBrowserFlow(@NonNull final String customTabResponseUri) {
        Logger.info(TAG, null, "Received redirect from customTab/browser.");
        final Intent resultIntent = createResultIntent(customTabResponseUri);
        final Map<String, String> urlQueryParameters = StringExtensions.getUrlParameters(customTabResponseUri);
        final String userName = urlQueryParameters.get(AuthenticationConstants.Broker.INSTALL_UPN_KEY);

        if (isDeviceRegisterRedirect(customTabResponseUri) && !TextUtils.isEmpty(userName)) {
            Logger.info(TAG, " Device needs to be registered, sending BROWSER_CODE_DEVICE_REGISTER");
            Logger.infoPII(TAG, "Device Registration triggered for user: " + userName);
            resultIntent.putExtra(AuthenticationConstants.Broker.INSTALL_UPN_KEY, userName);
            sendResult(
                    AuthenticationConstants.UIResponse.BROWSER_CODE_DEVICE_REGISTER,
                    resultIntent
            );
            return;
        }

        if (urlQueryParameters.containsKey(INSTALL_URL_KEY)) {
            final String appLink = urlQueryParameters.get(INSTALL_URL_KEY);
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(appLink));
            startActivity(browserIntent);
            Logger.info(TAG, "Return to caller with BROKER_REQUEST_RESUME, and waiting for result.");
            sendResult(AuthenticationConstants.UIResponse.BROKER_REQUEST_RESUME, resultIntent);
        } else if (!StringUtil.isEmpty(resultIntent.getStringExtra(AUTHORIZATION_FINAL_URL))) {
            sendResult(AuthenticationConstants.UIResponse.BROWSER_CODE_COMPLETE, resultIntent);
            Telemetry.emit(new UiEndEvent().isUiComplete());
        } else if (!StringUtil.isEmpty(resultIntent.getStringExtra(AuthenticationConstants.Browser.RESPONSE_ERROR_SUBCODE))
                && resultIntent.getStringExtra(AuthenticationConstants.Browser.RESPONSE_ERROR_SUBCODE).equalsIgnoreCase("cancel")) {
            //when the user click the "cancel" button in the UI, server will send the the redirect uri with "cancel" error sub-code and redirects back to the calling app
            Telemetry.emit(new UiEndEvent().isUserCancelled());
            sendResult(AuthenticationConstants.UIResponse.BROWSER_CODE_SDK_CANCEL, resultIntent);
        } else {
            Telemetry.emit(new UiEndEvent().isUiCancelled());
            sendResult(AuthenticationConstants.UIResponse.BROWSER_CODE_ERROR, resultIntent);
        }

        finish();
    }

    private Intent createResultIntent(@NonNull final String url) {
        Intent resultIntent = new Intent();
        final Map<String, String> parameters = StringExtensions.getUrlParameters(url);
        if (!StringExtensions.isNullOrBlank(parameters.get(ERROR))) {
            Logger.info(TAG, "Sending intent to cancel authentication activity");

            resultIntent.putExtra(AuthenticationConstants.Browser.RESPONSE_ERROR_CODE, parameters.get(ERROR));
            resultIntent.putExtra(AuthenticationConstants.Browser.RESPONSE_ERROR_SUBCODE, parameters.get(ERROR_SUBCODE));

            // Fallback logic on error_subcode when error_description is not provided.
            // When error is "login_required", redirect url has error_description.
            // When error is  "access_denied", redirect url has  error_subcode.
            if (!StringUtil.isEmpty(parameters.get(ERROR_DESCRIPTION))) {
                resultIntent.putExtra(AuthenticationConstants.Browser.RESPONSE_ERROR_MESSAGE, parameters.get(ERROR_DESCRIPTION));
            } else {
                resultIntent.putExtra(AuthenticationConstants.Browser.RESPONSE_ERROR_MESSAGE, parameters.get(ERROR_SUBCODE));
            }
        } else {
            Logger.info(TAG, "It is pointing to redirect. Final url can be processed to get the code or error.");
            resultIntent.putExtra(AUTHORIZATION_FINAL_URL, url);
        }

        return resultIntent;
    }

    private boolean isDeviceRegisterRedirect(@NonNull final String redirectUrl) {
        try {
            URI uri = new URI(redirectUrl);
            return uri.getScheme().equalsIgnoreCase(REDIRECT_PREFIX) &&
                    uri.getHost().equalsIgnoreCase(DEVICE_REGISTRATION_REDIRECT_URI_HOSTNAME);
        } catch (URISyntaxException e) {
            Logger.error(TAG, "Uri construction failed", e);
            return false;
        }
    }
}
