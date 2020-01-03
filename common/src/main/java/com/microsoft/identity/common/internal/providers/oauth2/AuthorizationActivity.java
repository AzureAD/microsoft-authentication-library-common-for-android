package com.microsoft.identity.common.internal.providers.oauth2;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;

import com.microsoft.identity.common.R;

public final class AuthorizationActivity extends FragmentActivity {

    private AuthorizationFragment mFragment;

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