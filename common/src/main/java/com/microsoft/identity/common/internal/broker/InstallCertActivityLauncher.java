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
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.microsoft.identity.common.internal.logging.Logger;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.util.StringUtil;

import lombok.AccessLevel;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * Base activity used to launch a Install WPJ Certificate request,
 * and get the result from the Activity using the API {@link AppCompatActivity#registerForActivityResult(ActivityResultContract, ActivityResultCallback)}
 */
@Accessors(prefix = "m")
public final class InstallCertActivityLauncher extends AppCompatActivity {
    private static final String TAG = InstallCertActivityLauncher.class.getSimpleName();
    private static final String INSTALL_CERT_INTENT_STARTED = "broker_intent_started";
    public static final String INSTALL_CERT_INTENT = "install_cert_intent";
    private Intent mInstallCertificateIntent;
    private Boolean mInstallCertificateIntentStarted = false;
    private Boolean mInstallCertificateResultReceived = false;

    @Setter(AccessLevel.PROTECTED)
    private static IInstallCertCallback mCallback;
    @Setter(AccessLevel.PROTECTED)
    private static String mResultKey;
    @Setter(AccessLevel.PROTECTED)
    private static String mErrorKey;

    // installCertActivityResultLauncher creates an ActivityResultLauncher<Intent>
    // to process the result form the install cert activity
    final ActivityResultLauncher<Intent> installCertActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            activityResult -> {
                final String methodTag = TAG + "#installCertActivityResultLauncher";
                Logger.info(methodTag, "Result received from Broker, Result code: " + activityResult.getResultCode());
                // Verify that the activity returned data
                final Intent intentData = activityResult.getData();
                if (intentData == null || intentData.getExtras() == null) {
                    onError("Certificate installation failed with an empty response");
                    mInstallCertificateResultReceived = true;
                    finish();
                    return;
                }
                // Get data
                final Bundle resultBundle = intentData.getExtras();
                final String result = resultBundle.getString(mResultKey, "false");
                final boolean isCertificateInstalled = Boolean.parseBoolean(result);
                final String errorMessage = resultBundle.getString(mErrorKey, null);
                // Send response to callback
                if (activityResult.getResultCode() == Activity.RESULT_OK) {
                    onSuccess(isCertificateInstalled);
                } else if (activityResult.getResultCode() == Activity.RESULT_CANCELED) {
                    onError("Certificate not Installed, user cancelled operation");
                } else {
                    onError(errorMessage);
                }
                mInstallCertificateResultReceived = true;
                finish();
            });

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            mInstallCertificateIntent = getIntent().getExtras().getParcelable(INSTALL_CERT_INTENT);
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
            onError("The activity is killed unexpectedly.");
        }
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(@NonNull final Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(INSTALL_CERT_INTENT, mInstallCertificateIntent);
        outState.putBoolean(INSTALL_CERT_INTENT_STARTED, mInstallCertificateIntentStarted);
    }

    private void onError(@Nullable final String errorMessage) {
        final String defaultErrorMessage = "Unexpected error: Certificate not installed";
        if (mCallback != null) {
            final String message = StringUtil.isNullOrEmpty(errorMessage) ? defaultErrorMessage : errorMessage;
            mCallback.onError(
                    new ClientException(ClientException.UNKNOWN_ERROR, message)
            );
        }
    }

    private void onSuccess(final boolean isCertificateInstalled) {
        if (mCallback != null) {
            mCallback.onSuccess(isCertificateInstalled);
        }
    }

    /**
     * Starts the InstallCertActivityLauncher, the activity in charge to launch the install certificate intent,
     * and extracts the result from this activity to pass it to the callback.
     *
     * @param activity calling activity
     * @param installCertificateIntent Install certificate intent
     * @param callback callback to run when the activity ends
     * @param keyToExtractResult key to extract the result from the install cert intent
     * @param keyToExtractError key to extract the error from the install cert intent
     */
    static public void installCertificate(@NonNull final Activity activity,
                                          @NonNull final Intent installCertificateIntent,
                                          @NonNull final IInstallCertCallback callback,
                                          @NonNull final String keyToExtractResult,
                                          @NonNull final String keyToExtractError) {
        final Intent installCertLauncher = new Intent(activity, InstallCertActivityLauncher.class);
        installCertLauncher.putExtra(INSTALL_CERT_INTENT, installCertificateIntent);
        mCallback = callback;
        mResultKey = keyToExtractResult;
        mErrorKey = keyToExtractError;
        activity.startActivity(installCertLauncher);
    }
}
