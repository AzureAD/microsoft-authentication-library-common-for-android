//  Copyright (c) Microsoft Corporation.
//  All rights reserved.
//
//  This code is licensed under the MIT License.
//
//  Permission is hereby granted, free of charge, to any person obtaining a copy
//  of this software and associated documentation files(the "Software"), to deal
//  in the Software without restriction, including without limitation the rights
//  to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
//  copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions :
//
//  The above copyright notice and this permission notice shall be included in
//  all copies or substantial portions of the Software.
//
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//  THE SOFTWARE.
package com.microsoft.identity.common.internal.broker;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.microsoft.identity.common.adal.internal.AuthenticationConstants;
import com.microsoft.identity.common.exception.ClientException;
import com.microsoft.identity.common.exception.ErrorStrings;
import com.microsoft.identity.common.internal.request.SdkType;
import com.microsoft.identity.common.internal.result.BrokerResultAdapterFactory;
import com.microsoft.identity.common.internal.result.IBrokerResultAdapter;
import com.microsoft.identity.common.logging.Logger;

import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.AuthorizationIntentAction.RETURN_INTERACTIVE_REQUEST_RESULT;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.AuthorizationIntentKey.REQUEST_CODE;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.AuthorizationIntentKey.RESULT_CODE;

public final class BrokerActivity extends Activity {

    public static final String BROKER_INTENT = "broker_intent";
    static final String BROKER_INTENT_STARTED = "broker_intent_started";
    static final int BROKER_INTENT_REQUEST_CODE = 1001;

    private static final String TAG = BrokerActivity.class.getSimpleName();

    private Intent mBrokerInteractiveRequestIntent;
    private Boolean mBrokerIntentStarted = false;
    private Boolean mBrokerResultReceived = false;


    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            mBrokerInteractiveRequestIntent = getIntent().getExtras().getParcelable(BROKER_INTENT);
        } else {
            mBrokerInteractiveRequestIntent = savedInstanceState.getParcelable(BROKER_INTENT);
            mBrokerIntentStarted = savedInstanceState.getBoolean(BROKER_INTENT_STARTED);
        }

    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!mBrokerIntentStarted) {
            mBrokerIntentStarted = true;
            startActivityForResult(mBrokerInteractiveRequestIntent, BROKER_INTENT_REQUEST_CODE);
        }

    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        // If the broker process crashes, onActivityResult() will not be triggered.
        // (tested by throwing an exception in AccountChooserActivity, and by killing the activity via App Switcher).
        if (!mBrokerResultReceived) {
            returnsExceptionOnActivityUnexpectedlyKilled();
        }

        super.onDestroy();
    }

    private void returnsExceptionOnActivityUnexpectedlyKilled() {
        final IBrokerResultAdapter resultAdapter = BrokerResultAdapterFactory.getBrokerResultAdapter(SdkType.MSAL);
        final Bundle resultBundle = resultAdapter.bundleFromBaseException(
                new ClientException(ErrorStrings.BROKER_REQUEST_CANCELLED,
                        "The activity is killed unexpectedly."), null);

        final Intent data = new Intent();
        data.putExtras(resultBundle);

        data.setAction(RETURN_INTERACTIVE_REQUEST_RESULT);
        data.putExtra(REQUEST_CODE, AuthenticationConstants.UIRequest.BROWSER_FLOW);
        data.putExtra(RESULT_CODE, AuthenticationConstants.UIResponse.BROWSER_CODE_CANCEL);

        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(data);
    }

    @Override
    protected void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(BROKER_INTENT, mBrokerInteractiveRequestIntent);
        outState.putBoolean(BROKER_INTENT_STARTED, mBrokerIntentStarted);
    }

    /**
     * Receive result from broker intent
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        final String methodName = ":onActivityResult";
        Logger.info(TAG + methodName,
                "Result received from Broker "
                        + "Request code: " + requestCode
                        + " Result code: " + requestCode
        );

        mBrokerResultReceived = true;

        if (resultCode == AuthenticationConstants.UIResponse.TOKEN_BROKER_RESPONSE ||
                resultCode == AuthenticationConstants.UIResponse.BROWSER_CODE_CANCEL
                || resultCode == AuthenticationConstants.UIResponse.BROWSER_CODE_ERROR) {

            Logger.verbose(TAG + methodName, "Completing interactive request ");

            data.setAction(RETURN_INTERACTIVE_REQUEST_RESULT);
            data.putExtra(REQUEST_CODE, AuthenticationConstants.UIRequest.BROWSER_FLOW);
            data.putExtra(RESULT_CODE, resultCode);

            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(data);
        } else {
            // This means the broker is unexpectedly killed. (tested by killing the broker process via adb).
            returnsExceptionOnActivityUnexpectedlyKilled();
        }

        finish();
    }
}
