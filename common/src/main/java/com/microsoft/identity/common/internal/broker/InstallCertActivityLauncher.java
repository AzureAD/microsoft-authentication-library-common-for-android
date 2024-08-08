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

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.microsoft.identity.common.PropertyBagUtil;
import com.microsoft.identity.common.internal.logging.Logger;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.util.StringUtil;
import com.microsoft.identity.common.java.util.ported.LocalBroadcaster;
import com.microsoft.identity.common.java.util.ported.PropertyBag;


import lombok.experimental.Accessors;

/**
 * Base activity used to launch a Install WPJ Certificate request,
 * and get the result from the Activity using the API {@link AppCompatActivity#registerForActivityResult(ActivityResultContract, ActivityResultCallback)}
 */
@Accessors(prefix = "m")
public final class InstallCertActivityLauncher extends AppCompatActivity {
    private static final String TAG = InstallCertActivityLauncher.class.getSimpleName();
    private static final String INSTALL_CERT_INTENT_STARTED = "broker_intent_started";
    private static final String INSTALL_CERT_INTENT = "install_cert_intent";
    private static final String INSTALL_CERT_BROADCAST_ALIAS = "install_cert_broadcast_alias";
    private static final String CERT_INSTALLATION_FAILED_WITH_EMPTY_RESPONSE = "Certificate installation failed with an empty response";
    private Intent mInstallCertificateIntent;
    private Boolean mInstallCertificateIntentStarted = false;
    private Boolean mInstallCertificateResultReceived = false;

    // installCertActivityResultLauncher creates an ActivityResultLauncher<Intent>
    // to process the result form the install cert activity
    final ActivityResultLauncher<Intent> installCertActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            activityResult -> {
                final String methodTag = TAG + "#installCertActivityResultLauncher";
                Logger.info(methodTag, "Result received from Broker, Result code: " + activityResult.getResultCode());
                final Intent intentWithData = activityResult.getData();
                final PropertyBag resultBag;
                if (intentWithData == null || intentWithData.getExtras() == null) {
                    Logger.error(methodTag, CERT_INSTALLATION_FAILED_WITH_EMPTY_RESPONSE, null);
                    resultBag = new PropertyBag();
                } else {
                    resultBag = PropertyBagUtil.fromBundle(intentWithData.getExtras());
                }
                mInstallCertificateResultReceived = true;
                LocalBroadcaster.INSTANCE.broadcast(INSTALL_CERT_BROADCAST_ALIAS, resultBag);
                finish();
            });


    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final String methodTag = TAG + ":onCreate";
        if (savedInstanceState == null) {
            Bundle extras = getIntent().getExtras();
            if(extras != null) {
                mInstallCertificateIntent = extras.getParcelable(INSTALL_CERT_INTENT);
            } else {
                Logger.warn(methodTag, "Extras is null.");
            }
        } else {
            // If the activity is being re-initialized after previously being shut down
            // then this Bundle contains the data it most recently supplied.
            mInstallCertificateIntent = savedInstanceState.getParcelable(INSTALL_CERT_INTENT);
            mInstallCertificateIntentStarted = savedInstanceState.getBoolean(INSTALL_CERT_INTENT_STARTED);
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!mInstallCertificateIntentStarted) {
            mInstallCertificateIntentStarted = true;
            // Launch intent to Install the certificate.
            installCertActivityResultLauncher.launch(mInstallCertificateIntent);
        }
    }

    @Override
    protected void onDestroy() {
        // If the broker process crashes, registerForActivityResult will not be triggered.
        if (!mInstallCertificateResultReceived) {
            Logger.error(TAG, "The activity is killed unexpectedly.", null);
            LocalBroadcaster.INSTANCE.broadcast(INSTALL_CERT_BROADCAST_ALIAS, new PropertyBag());
        }
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(@NonNull final Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(INSTALL_CERT_INTENT, mInstallCertificateIntent);
        outState.putBoolean(INSTALL_CERT_INTENT_STARTED, mInstallCertificateIntentStarted);
    }

    /**
     * Starts the InstallCertActivityLauncher, the activity in charge to launch the install certificate intent,
     * and extracts the result from this activity to pass it to the callback.
     *
     * @param activity                 calling activity
     * @param installCertificateIntent Install certificate intent
     * @param callback                 callback to run when the activity ends
     * @param keyToExtractResult       key to extract the result from the install cert intent
     * @param keyToExtractError        key to extract the error from the install cert intent
     */
    static public void installCertificate(@NonNull final Activity activity,
                                          @NonNull final Intent installCertificateIntent,
                                          @NonNull final IInstallCertCallback callback,
                                          @NonNull final String keyToExtractResult,
                                          @NonNull final String keyToExtractError) {
        final Intent installCertLauncher = new Intent(activity, InstallCertActivityLauncher.class);
        installCertLauncher.putExtra(INSTALL_CERT_INTENT, installCertificateIntent);
        registerCallbackAndParseResult(callback, keyToExtractResult, keyToExtractError);
        activity.startActivity(installCertLauncher);
    }

    /**
     * Registers a listener in a local broadcaster to get a propertyBag from the install cert activity,
     * the registered callback parse the result and invokes the callback provided by the API caller.
     *
     * @param callback           callback to run when the activity ends
     * @param keyToExtractResult key to extract the result from the install cert intent
     * @param keyToExtractError  key to extract the error from the install cert intent
     */
    private static void registerCallbackAndParseResult(@NonNull final IInstallCertCallback callback,
                                                       @NonNull final String keyToExtractResult,
                                                       @NonNull final String keyToExtractError) {
        LocalBroadcaster.INSTANCE.registerCallback(
                INSTALL_CERT_BROADCAST_ALIAS,
                propertyBag -> {
                    final String isCertInstalled = propertyBag.get(keyToExtractResult);
                    final String exceptionMessage = propertyBag.get(keyToExtractError);
                    if (StringUtil.isNullOrEmpty(exceptionMessage) && !StringUtil.isNullOrEmpty(isCertInstalled)) {
                        callback.onSuccess(Boolean.parseBoolean(isCertInstalled));
                    } else {
                        final String errorMessage = StringUtil.isNullOrEmpty(exceptionMessage) ?
                                CERT_INSTALLATION_FAILED_WITH_EMPTY_RESPONSE : exceptionMessage;
                        callback.onError(new ClientException(ClientException.INSTALL_CERT_ERROR, errorMessage));
                    }
                    LocalBroadcaster.INSTANCE.unregisterCallback(INSTALL_CERT_BROADCAST_ALIAS);
                });
    }
}
