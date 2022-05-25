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

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.microsoft.identity.common.internal.ui.DualScreenActivity;
import com.microsoft.identity.common.logging.Logger;

import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.AuthorizationIntentAction.DESTROY_REDIRECT_RECEIVING_ACTIVITY_ACTION;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.AuthorizationIntentAction.REDIRECT_RETURNED_ACTION;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.AuthorizationIntentAction.REFRESH_TO_CLOSE;

/**
 * Authorization activity for addressing authorization activities that are launched within the task
 * associated with the activity provided as a parameter to InteractiveTokenCommand.
 *
 * NOTE: this is only used when library configuration (set in MSAL) indicates that it should be used.
 * DEFAULT today is to create a new task for authorization; however this leads to problems if the user
 * navigates away during authorization (to home screen for example) or when using multi-window.
 */
public class CurrentTaskAuthorizationActivity extends DualScreenActivity {

    private static final String TAG = CurrentTaskAuthorizationActivity.class.getSimpleName();

    private CurrentTaskBrowserAuthorizationFragment mFragment;
    //Determines whether we should close this activity onResume.
    private boolean mCloseCustomTabs = true;
    private BroadcastReceiver redirectReceiver;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final String methodTag = TAG + ":onCreate";

        final Fragment fragment = AuthorizationActivityFactory.getAuthorizationFragmentFromStartIntent(getIntent());

        if(fragment instanceof CurrentTaskBrowserAuthorizationFragment){
            mFragment = (CurrentTaskBrowserAuthorizationFragment) fragment;
            mFragment.setInstanceState(getIntent().getExtras());
        }else{
            IllegalStateException ex = new IllegalStateException("Unexpected fragment type");
            Logger.error(methodTag, "Fragment provided was not of type CurrentTaskBrowserAuthorizationFragment", ex);
            throw ex;
        }

        //When the authorization response is received back to the library via an intent filter
        //That activity will launch this one to handle the processing of the authorization response.
        //It does that by invoked startActivityForResult.  If this activity is present in the same task stack/affinity
        //As the activity which received the authorization result and the onNewIntent method will be invoked.
        //If this activity is not in the same task stack/affinity then starting this activity will create a new instance
        //In that new instance scneario the following will be executed.
        if (REDIRECT_RETURNED_ACTION.equals(getIntent().getAction())) {
            if(CurrentTaskBrowserAuthorizationFragment.class.isInstance(mFragment)) {
                Bundle arguments = new Bundle();
                arguments.putBoolean("RESPONSE", true);
                mFragment.setArguments(arguments);
                mFragment.completeAuthorizationInBrowserFlow(getIntent().getStringExtra("RESPONSE_URI"));
                finish();
                return;
            }

            return;
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

    /**
     * This is invoked when an existing activity is re-used and provided with a new intent with additional information
     * NOTE: It's important that you use setIntent to update the intent associated with the activity.  Otherwise subsequent calls to
     * getIntent() in other event handlers will return the original intent or null
     * @param intent
     */
    @Override
    protected void onNewIntent(final Intent intent) {
        super.onNewIntent(intent);
        if (REFRESH_TO_CLOSE.equals(intent.getAction())) {
            final Intent broadcast = new Intent(DESTROY_REDIRECT_RECEIVING_ACTIVITY_ACTION);
            LocalBroadcastManager.getInstance(this).sendBroadcast(broadcast);
            unregisterAndFinish();
        }
        //IMPORTANT: If you don't call this...
        //then getIntent will return the original intent used to launch the activity or null
        setIntent(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();

        //onNewIntent will change the value of getIntent from what was used to create the activity
        //to the intent that was communicated to an existing activity
        if (REDIRECT_RETURNED_ACTION.equals(getIntent().getAction())) {

            Bundle arguments = new Bundle();
            arguments.putBoolean("RESPONSE", true);
            mFragment.setArguments(arguments);
            mFragment.completeAuthorizationInBrowserFlow(getIntent().getStringExtra("RESPONSE_URI"));
            setResult(RESULT_OK);
            unregisterAndFinish();

        }
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
