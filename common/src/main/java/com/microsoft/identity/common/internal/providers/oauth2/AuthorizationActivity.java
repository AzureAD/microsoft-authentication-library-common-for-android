package com.microsoft.identity.common.internal.providers.oauth2;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;

import com.microsoft.identity.common.R;
import com.microsoft.identity.common.internal.logging.DiagnosticContext;
import com.microsoft.identity.common.internal.ui.AuthorizationAgent;

import java.util.HashMap;

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
        intent.putExtra(AuthorizationFragment.KEY_AUTH_INTENT, authIntent);
        intent.putExtra(AuthorizationFragment.KEY_AUTH_REQUEST_URL, requestUrl);
        intent.putExtra(AuthorizationFragment.KEY_AUTH_REDIRECT_URI, redirectUri);
        intent.putExtra(AuthorizationFragment.KEY_REQUEST_HEADERS, requestHeaders);
        intent.putExtra(AuthorizationFragment.KEY_AUTH_AUTHORIZATION_AGENT, authorizationAgent);
        intent.putExtra(AuthorizationFragment.WEB_VIEW_ZOOM_CONTROLS_ENABLED, webViewZoomControlsEnabled);
        intent.putExtra(AuthorizationFragment.WEB_VIEW_ZOOM_ENABLED, webViewZoomEnabled);
        intent.putExtra(DiagnosticContext.CORRELATION_ID, DiagnosticContext.getRequestContext().get(DiagnosticContext.CORRELATION_ID));
        return intent;
    }
    
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.authorization_activity);
        mFragment = new AuthorizationFragment();
        mFragment.setInstanceState(getIntent().getExtras());

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