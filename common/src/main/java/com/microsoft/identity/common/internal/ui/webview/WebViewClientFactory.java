package com.microsoft.identity.common.internal.ui.webview;

import android.app.Activity;

import com.microsoft.identity.common.internal.logging.Logger;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationConfiguration;
import com.microsoft.identity.common.internal.ui.webview.challengehandlers.IChallengeCompletionCallback;

/** Not sure if we should create a OAuth2StrategyFactory class and move the getWebViewClient into each OAuth2Strategy subclasses.
 *  TODO
 */
public class WebViewClientFactory <GenericOAuth2WebViewClient extends OAuth2WebViewClient>{
    static final String TAG = WebViewClientFactory.class.getSimpleName();

    private static WebViewClientFactory sInstance = null;

    public static WebViewClientFactory getInstance() {
        if (sInstance == null) {
            sInstance = new WebViewClientFactory();
        }
        return sInstance;
    }

    public GenericOAuth2WebViewClient getWebViewClient(AuthorizationConfiguration config, Activity activity, IChallengeCompletionCallback callback) {
        //Currently for all microsoft idp, the webView client's behavior is the same.
        if (config.getIdpType().equals("Microsoft")) {
            return (GenericOAuth2WebViewClient) new AzureActiveDirectoryWebViewClient(activity, callback, config.getRedirectUrl());
        } else {
            Logger.warn(TAG, "Not implemented.");
            return null;
        }
    }
}