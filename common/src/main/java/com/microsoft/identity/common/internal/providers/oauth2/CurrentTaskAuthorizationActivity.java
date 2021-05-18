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
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.microsoft.identity.common.internal.telemetry.Telemetry;
import com.microsoft.identity.common.internal.telemetry.events.UiStartEvent;
import com.microsoft.identity.common.internal.ui.AuthorizationAgent;
import com.microsoft.identity.common.internal.ui.DualScreenActivity;
import com.microsoft.identity.common.logging.Logger;

import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.AuthorizationIntentKey.AUTHORIZATION_AGENT;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.AuthorizationIntentAction.DESTROY_REDIRECT_RECEIVING_ACTIVITY;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.AuthorizationIntentAction.REDIRECT_RETURNED_ACTION;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.AuthorizationIntentAction.REFRESH_TO_CLOSE;

public class CurrentTaskAuthorizationActivity extends DualScreenActivity {

    private static final String TAG = CurrentTaskAuthorizationActivity.class.getSimpleName();

    private CurrentTaskBrowserAuthorizationFragment mFragment;
    private boolean mCustomTabs = false;
    private boolean mCloseCustomTabs = true;
    private BroadcastReceiver redirectReceiver;

     @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Fragment fragment = AuthorizationActivityFactory.getAuthorizationFragmentFromStartIntent(getIntent());

        if(fragment instanceof CurrentTaskBrowserAuthorizationFragment){
            mFragment = (CurrentTaskBrowserAuthorizationFragment) fragment;
            mFragment.setInstanceState(getIntent().getExtras());
        }else{
            IllegalStateException ex = new IllegalStateException("Unexpected fragment type");
            Logger.error(TAG, "Fragment provided was not of type CurrentTaskBrowserAuthorizationFragment", ex);
            throw ex;
        }

        // Custom Tab Redirects should not be creating a new instance of this activity
        if (REDIRECT_RETURNED_ACTION.equals(getIntent().getAction())) {
            if(CurrentTaskBrowserAuthorizationFragment.class.isInstance(mFragment)) {
                Bundle arguments = new Bundle();
                arguments.putBoolean("RESPONSE", true);
                mFragment.setArguments(arguments);
                mFragment.completeAuthorizationInBrowserFlow(getIntent().getStringExtra("RESPONSE_URI"));
                finish();
                return;
            }
        }

        setFragment(mFragment);

        if (savedInstanceState == null) {

            mCloseCustomTabs = false;

            // This activity will receive a broadcast if it can't be opened from the back stack
            redirectReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    // Remove the custom tab on top of this activity.
                    Intent newIntent = new Intent(CurrentTaskAuthorizationActivity.this, CurrentTaskAuthorizationActivity.class);
                    newIntent.setAction(REFRESH_TO_CLOSE);
                    newIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(newIntent);
                }
            };
            LocalBroadcastManager.getInstance(this).registerReceiver(redirectReceiver,
                    new IntentFilter(REDIRECT_RETURNED_ACTION)
            );
        }
    }

    @Override
    public void onBackPressed() {
        if (!mFragment.onBackPressed()) {
            super.onBackPressed();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (REFRESH_TO_CLOSE.equals(intent.getAction())) {
            // The custom tab is now destroyed so we can finish the redirect activity
            Intent broadcast = new Intent(DESTROY_REDIRECT_RECEIVING_ACTIVITY);
            LocalBroadcastManager.getInstance(this).sendBroadcast(broadcast);
            unregisterAndFinish();
        } else if (REDIRECT_RETURNED_ACTION.equals(intent.getAction())) {
            // We have successfully redirected back to this activity. Return the result and close.
            unregisterAndFinish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mCloseCustomTabs) {
            // The custom tab was closed without getting a result.
            unregisterAndFinish();
        }
        mCloseCustomTabs = true;
    }

    private void unregisterAndFinish() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(redirectReceiver);
        finish();
    }
}