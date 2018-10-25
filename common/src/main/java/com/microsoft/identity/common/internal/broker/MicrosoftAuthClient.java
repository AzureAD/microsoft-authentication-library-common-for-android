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

import android.accounts.AccountManager;
import android.accounts.AuthenticatorDescription;
import android.content.Context;
import android.content.Intent;

import com.microsoft.identity.common.adal.internal.AuthenticationConstants;
import com.microsoft.identity.common.internal.logging.Logger;

public class MicrosoftAuthClient {

    private static final String TAG = MicrosoftAuthClient.class.getSimpleName();
    private static final String MICROSOFT_AUTH_SERVICE_INTENT_FILTER = "com.microsoft.workaccount.brokeraccount.MicrosoftAuth";
    private static final String MICROSOFT_AUTH_SERVICE_CLASS_NAME = "com.microsoft.workaccount.brokeraccount.MicrosoftAuthService";

    private Context mContext;
    private MicrosoftAuthServiceConnection mMicrosoftAuthServiceConnection;
    private Intent mMicrosoftAuthServiceIntent;

    public MicrosoftAuthClient(Context context){
        mContext = context;
        mMicrosoftAuthServiceIntent = getIntentForAuthService(mContext);
    }

    public MicrosoftAuthServiceFuture connect(){

        MicrosoftAuthServiceFuture future = new MicrosoftAuthServiceFuture();
        mMicrosoftAuthServiceConnection = new MicrosoftAuthServiceConnection(future);

        final boolean serviceBound = mContext.bindService(mMicrosoftAuthServiceIntent, mMicrosoftAuthServiceConnection, Context.BIND_AUTO_CREATE);
        Logger.verbose(TAG + "connect", "The status for MicrosoftAuthService bindService call is: " + Boolean.valueOf(serviceBound));

        if (!serviceBound) {
            disconnect();
            throw new RuntimeException("Unable to bind to service");
        }

        return future;
    }

    public void disconnect(){
        mContext.unbindService(mMicrosoftAuthServiceConnection);
    }

    private Intent getIntentForAuthService(final Context context) {
        String currentActiveBrokerPackageName = getCurrentActiveBrokerPackageName(context);
        if (currentActiveBrokerPackageName == null || currentActiveBrokerPackageName.length() == 0) {
            return null;
        }
        final Intent authServiceToBind = new Intent(MICROSOFT_AUTH_SERVICE_INTENT_FILTER);
        authServiceToBind.setPackage(currentActiveBrokerPackageName);
        authServiceToBind.setClassName(currentActiveBrokerPackageName, MICROSOFT_AUTH_SERVICE_CLASS_NAME);

        return authServiceToBind;
    }


    private String getCurrentActiveBrokerPackageName(final Context context) {
        AuthenticatorDescription[] authenticators = AccountManager.get(context).getAuthenticatorTypes();
        for (AuthenticatorDescription authenticator : authenticators) {
            if (authenticator.type.equals(AuthenticationConstants.Broker.BROKER_ACCOUNT_TYPE)) {
                return authenticator.packageName;
            }
        }

        return null;
    }







}
