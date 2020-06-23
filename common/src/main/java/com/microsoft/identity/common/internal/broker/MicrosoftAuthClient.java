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

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.text.TextUtils;

import com.microsoft.identity.common.exception.ClientException;
import com.microsoft.identity.common.internal.logging.Logger;

import java.util.List;

import androidx.annotation.NonNull;

/**
 * Client that wraps the code necessary to bind to the MicrosoftAuthService (Android Bound Service)
 */
public class MicrosoftAuthClient {

    private static final String TAG = MicrosoftAuthClient.class.getSimpleName();
    private static final String MICROSOFT_AUTH_SERVICE_INTENT_FILTER = "com.microsoft.identity.client.MicrosoftAuth";
    private static final String MICROSOFT_AUTH_SERVICE_CLASS_NAME = "com.microsoft.identity.client.MicrosoftAuthService";

    private Context mContext;
    private MicrosoftAuthServiceConnection mMicrosoftAuthServiceConnection;
    private Intent mMicrosoftAuthServiceIntent;
    private Boolean mBound = false;

    /**
     * Constructor for the Microsoft Auth Client
     *
     * @param context
     */
    public MicrosoftAuthClient(Context context) {
        mContext = context;
        mMicrosoftAuthServiceIntent = getIntentForAuthService(mContext);
    }

    /**
     * Binds to the service and returns a future that provides the proxy for the calling the Microsoft auth service
     *
     * @return MicrosoftAuthServiceFuture
     */
    public MicrosoftAuthServiceFuture connect() throws ClientException {

        MicrosoftAuthServiceFuture future = new MicrosoftAuthServiceFuture();
        mMicrosoftAuthServiceConnection = new MicrosoftAuthServiceConnection(future);

        mBound = mContext.bindService(mMicrosoftAuthServiceIntent, mMicrosoftAuthServiceConnection, Context.BIND_AUTO_CREATE);
        Logger.info(TAG + "connect", "The status for MicrosoftAuthService bindService call is: " + Boolean.valueOf(mBound));

        if (!mBound) {
            throw new ClientException("Service is unavailable or does not support binding.  Microsoft Auth Service.");
        }

        return future;
    }

    /**
     * Disconnects (unbinds) from the bound Microsoft Auth Service
     */
    public void disconnect() {
        if(mBound) {
            mContext.unbindService(mMicrosoftAuthServiceConnection);
            mBound = false;
        }
    }


    /**
     * Gets the intent that points to the bound service on the device... if available
     * You shouldn't get this far if it's not available
     *
     * @param context
     * @return Intent
     */
    public Intent getIntentForAuthService(final Context context) {
        final String currentActiveBrokerPackageName = new BrokerValidator(context).
                getCurrentActiveBrokerPackageName();

        if (TextUtils.isEmpty(currentActiveBrokerPackageName) ||
                !isMicrosoftAuthServiceSupported(context.getPackageManager(), currentActiveBrokerPackageName)) {
            return null;
        }
        final Intent authServiceToBind = new Intent(MICROSOFT_AUTH_SERVICE_INTENT_FILTER);
        authServiceToBind.setPackage(currentActiveBrokerPackageName);
        authServiceToBind.setClassName(currentActiveBrokerPackageName, MICROSOFT_AUTH_SERVICE_CLASS_NAME);

        return authServiceToBind;
    }

    private boolean isMicrosoftAuthServiceSupported(@NonNull final PackageManager packageManager,
                                                    @NonNull final String packageName) {
        final Intent microsoftAuthServiceIntent = new Intent(MICROSOFT_AUTH_SERVICE_INTENT_FILTER);
        microsoftAuthServiceIntent.setPackage(packageName);
        microsoftAuthServiceIntent.setClassName(packageName, MICROSOFT_AUTH_SERVICE_CLASS_NAME);

        final List<ResolveInfo> infos = packageManager.queryIntentServices(microsoftAuthServiceIntent, 0);
        return infos != null && infos.size() > 0;
    }

}
