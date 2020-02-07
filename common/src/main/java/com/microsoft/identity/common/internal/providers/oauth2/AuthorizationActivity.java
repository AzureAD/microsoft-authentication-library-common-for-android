package com.microsoft.identity.common.internal.providers.oauth2;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;

import com.microsoft.identity.common.R;
import com.microsoft.identity.common.internal.logging.DiagnosticContext;
import com.microsoft.identity.common.internal.telemetry.Telemetry;
import com.microsoft.identity.common.internal.telemetry.events.UiStartEvent;
import com.microsoft.identity.common.internal.ui.AuthorizationAgent;

import java.util.HashMap;

import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.AuthorizationIntentKey.AUTHORIZATION_AGENT;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.AuthorizationIntentKey.AUTH_INTENT;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.AuthorizationIntentKey.REDIRECT_URI;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.AuthorizationIntentKey.REQUEST_URL;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.AuthorizationIntentKey.REQUEST_HEADERS;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.AuthorizationIntentKey.WEB_VIEW_ZOOM_CONTROLS_ENABLED;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.AuthorizationIntentKey.WEB_VIEW_ZOOM_ENABLED;

public final class AuthorizationActivity extends FragmentActivity {

    private AuthorizationFragment mFragment;

    public static Intent createStartIntent(final Context context,
                                           final Intent authIntent,
                                           final String requestUrl,
                                           final String redirectUri,
                                           final HashMap<String, String> requestHeaders,
                                           final AuthorizationAgent authorizationAgent,
                                           final boolean webViewZoomEnabled,
                                           final boolean webViewZoomControlsEnabled) {
        final Intent intent = new Intent(context, AuthorizationActivity.class);
        intent.putExtra(AUTH_INTENT, authIntent);
        intent.putExtra(REQUEST_URL, requestUrl);
        intent.putExtra(REDIRECT_URI, redirectUri);
        intent.putExtra(REQUEST_HEADERS, requestHeaders);
        intent.putExtra(AUTHORIZATION_AGENT, authorizationAgent);
        intent.putExtra(WEB_VIEW_ZOOM_CONTROLS_ENABLED, webViewZoomControlsEnabled);
        intent.putExtra(WEB_VIEW_ZOOM_ENABLED, webViewZoomEnabled);
        intent.putExtra(DiagnosticContext.CORRELATION_ID, DiagnosticContext.getRequestContext().get(DiagnosticContext.CORRELATION_ID));
        return intent;
    }

    public static AuthorizationFragment getAuthorizationFragmentFromStartIntent(@NonNull final Intent intent){
        AuthorizationFragment fragment;
        final AuthorizationAgent authorizationAgent = (AuthorizationAgent)intent.getSerializableExtra(AUTHORIZATION_AGENT);
        Telemetry.emit(new UiStartEvent().putUserAgent(authorizationAgent));

        if (authorizationAgent == AuthorizationAgent.WEBVIEW) {
            fragment = new WebViewAuthorizationFragment();
        } else {
            fragment = new BrowserAuthorizationFragment();
        }

        fragment.setInstanceState(intent.getExtras());
        return fragment;
    }
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.authorization_activity);

        mFragment = getAuthorizationFragmentFromStartIntent(getIntent());
        getSupportFragmentManager()
                .beginTransaction()
                .setTransitionStyle(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .replace(R.id.authorization_activity_content, mFragment)
                .commit();
    }

    @Override
    public void onBackPressed() {
        if (!mFragment.onBackPressed()){
            super.onBackPressed();
        }
    }
}