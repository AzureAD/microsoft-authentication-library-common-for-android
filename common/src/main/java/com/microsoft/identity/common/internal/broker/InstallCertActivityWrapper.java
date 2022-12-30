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

import com.microsoft.identity.common.PropertyBagUtil;
import com.microsoft.identity.common.java.util.ported.PropertyBag;
import com.microsoft.identity.common.java.util.ported.LocalBroadcaster;
import com.microsoft.identity.common.logging.Logger;

import edu.umd.cs.findbugs.annotations.Nullable;

public final class InstallCertActivityWrapper extends Activity {
    private static final String TAG = InstallCertActivityWrapper.class.getSimpleName();
    private static final String INSTALL_CERT_INTENT_STARTED = "broker_intent_started";
    private static final int INSTALL_CERT_INTENT_REQUEST_CODE = 1;

    private Intent mInstallCertificateIntent;
    private Boolean mInstallCertificateIntentStarted = false;
    private Boolean mInstallCertificateResultReceived = false;

    public static final String INSTALL_CERT_INTENT = "install_cert_intent";
    public static final String INSTALL_CERT_BROADCAST_ALIAS = "install_cert_broadcast_alias";
    public static final String INSTALL_CERT_ACTIVITY_ERROR_KEY = "install_cert_error_key";

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            mInstallCertificateIntent = getIntent().getExtras().getParcelable(INSTALL_CERT_INTENT);
        } else {
            mInstallCertificateIntent = savedInstanceState.getParcelable(INSTALL_CERT_INTENT);
            mInstallCertificateIntentStarted = savedInstanceState.getBoolean(INSTALL_CERT_INTENT_STARTED);
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!mInstallCertificateIntentStarted) {
            mInstallCertificateIntentStarted = true;
            startActivityForResult(mInstallCertificateIntent, INSTALL_CERT_INTENT_REQUEST_CODE);
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
        if (!mInstallCertificateResultReceived) {
            final PropertyBag propertyBag = new PropertyBag();
            propertyBag.put(INSTALL_CERT_ACTIVITY_ERROR_KEY, "The activity is killed unexpectedly.");
            LocalBroadcaster.INSTANCE.broadcast(INSTALL_CERT_BROADCAST_ALIAS, propertyBag);
        }
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(INSTALL_CERT_INTENT, mInstallCertificateIntent);
        outState.putBoolean(INSTALL_CERT_INTENT_STARTED, mInstallCertificateIntentStarted);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        final String methodTag = TAG + ":onActivityResult";
        Logger.info(methodTag,
                "Result received from Broker "
                        + "Request code: " + requestCode
                        + " Result code: " + resultCode
        );
        mInstallCertificateResultReceived = true;
        final PropertyBag propertyBag = getPropertyBagFromBundle(data.getExtras());
        LocalBroadcaster.INSTANCE.broadcast(INSTALL_CERT_BROADCAST_ALIAS, propertyBag);
        finish();
    }

    private PropertyBag getPropertyBagFromBundle(@Nullable final Bundle resultBundle) {
        final PropertyBag propertyBag;
        if (resultBundle == null) {
            propertyBag = new PropertyBag();
            propertyBag.put(INSTALL_CERT_ACTIVITY_ERROR_KEY, "The activity responded with an empty bundle.");
        } else {
            propertyBag = PropertyBagUtil.fromBundle(resultBundle);
        }
        return propertyBag;
    }
}
