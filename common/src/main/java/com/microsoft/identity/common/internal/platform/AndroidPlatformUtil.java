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
package com.microsoft.identity.common.internal.platform;

import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.COMPANY_PORTAL_APP_PACKAGE_NAME;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import com.microsoft.identity.common.BuildConfig;
import com.microsoft.identity.common.adal.internal.AuthenticationConstants;
import com.microsoft.identity.common.adal.internal.net.DefaultConnectionService;
import com.microsoft.identity.common.internal.broker.BrokerData;
import com.microsoft.identity.common.internal.broker.IntuneMAMEnrollmentIdGateway;
import com.microsoft.identity.common.internal.broker.PackageHelper;
import com.microsoft.identity.common.internal.ui.webview.WebViewUtil;
import com.microsoft.identity.common.java.commands.ICommand;
import com.microsoft.identity.common.java.commands.InteractiveTokenCommand;
import com.microsoft.identity.common.java.commands.parameters.InteractiveTokenCommandParameters;
import com.microsoft.identity.common.java.constants.FidoConstants;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.exception.ErrorStrings;
import com.microsoft.identity.common.java.flighting.CommonFlight;
import com.microsoft.identity.common.java.flighting.CommonFlightsManager;
import com.microsoft.identity.common.java.logging.Logger;
import com.microsoft.identity.common.java.ui.BrowserDescriptor;
import com.microsoft.identity.common.java.util.IPlatformUtil;
import com.microsoft.identity.common.java.util.StringUtil;

import java.security.NoSuchAlgorithmException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.net.ssl.KeyManagerFactory;

import edu.umd.cs.findbugs.annotations.Nullable;
import lombok.AllArgsConstructor;
import lombok.NonNull;

@AllArgsConstructor
public class AndroidPlatformUtil implements IPlatformUtil {
    private static final String TAG = AndroidPlatformUtil.class.getSimpleName();

    @NonNull
    private final Context mContext;

    @Nullable
    private final Activity mActivity;

    /**
     * List of System Browsers which can be used from broker, currently only Chrome is supported.
     * This information here is populated from the default browser safelist in MSAL.
     *
     * @return
     */
    @Override
    public List<BrowserDescriptor> getBrowserSafeListForBroker() {
        List<BrowserDescriptor> browserDescriptors = new ArrayList<>();
        final HashSet<String> signatureHashes = new HashSet<String>();
        signatureHashes.add("7fmduHKTdHHrlMvldlEqAIlSfii1tl35bxj1OXN5Ve8c4lU6URVu4xtSHc3BVZxS6WWJnxMDhIfQN0N0K2NDJg==");
        final BrowserDescriptor chrome = new BrowserDescriptor(
                "com.android.chrome",
                signatureHashes,
                null,
                null
        );
        browserDescriptors.add(chrome);

        return browserDescriptors;
    }

    @Nullable
    @Override
    public String getInstalledCompanyPortalVersion() {
        try {
            final PackageInfo packageInfo =
                    mContext.getPackageManager().getPackageInfo(COMPANY_PORTAL_APP_PACKAGE_NAME, 0);
            return packageInfo.versionName;
        } catch (final PackageManager.NameNotFoundException e) {
            // CP is not installed. No need to do anything.
        }

        return null;
    }

    public void throwIfNetworkNotAvailable(final boolean performPowerOptimizationCheck)
            throws ClientException {

        final DefaultConnectionService connectionService = new DefaultConnectionService(mContext);

        if (performPowerOptimizationCheck && connectionService.isNetworkDisabledFromOptimizations()) {
            throw new ClientException(
                    ErrorStrings.NO_NETWORK_CONNECTION_POWER_OPTIMIZATION,
                    "Connection is not available to refresh token because power optimization is "
                            + "enabled. And the device is in doze mode or the app is standby");
        }

        if (!connectionService.isConnectionAvailable()) {
            throw new ClientException(
                    ErrorStrings.DEVICE_NETWORK_NOT_AVAILABLE,
                    "Connection is not available to refresh token");
        }
    }

    @Override
    public void removeCookiesFromWebView() {
        WebViewUtil.removeCookiesFromWebView(mContext);
    }

    @Override
    public boolean isValidCallingApp(@NonNull String redirectUri, @NonNull String packageName) {
        final String methodTag = TAG + ":isValidCallingApp";

        if (BuildConfig.bypassRedirectUriCheck || isValidHubRedirectURIForNAATests(redirectUri)) {
            Logger.warn(methodTag, "Bypassing RedirectUri Check. This should not be enabled in PROD. "+ redirectUri);
            return true;
        }

        final String expectedBrokerRedirectUri = PackageHelper.getBrokerRedirectUri(mContext, packageName);
        boolean isValidBrokerRedirect = StringUtil.equalsIgnoreCase(redirectUri, expectedBrokerRedirectUri);
        if (packageName.equals(AuthenticationConstants.Broker.AZURE_AUTHENTICATOR_APP_PACKAGE_NAME)) {
            final PackageHelper info = new PackageHelper(mContext.getPackageManager());
            //For merely verifying that the app is AuthApp, use a 512 hash.
            final String signatureDigest = info.getSha512SignatureForPackage(packageName);
            if (BrokerData.getProdMicrosoftAuthenticator().getSigningCertificateThumbprint().equals(signatureDigest)
                    || BrokerData.getDebugMicrosoftAuthenticator().getSigningCertificateThumbprint().equals(signatureDigest)) {
                // If the caller is the Authenticator, check if the redirect uri matches with either
                // the one generated with package name and signature or broker redirect uri.
                isValidBrokerRedirect |= StringUtil.equalsIgnoreCase(redirectUri, AuthenticationConstants.Broker.BROKER_REDIRECT_URI);
            }
        }

        if (!isValidBrokerRedirect) {
            com.microsoft.identity.common.logging.Logger.error(
                    methodTag,
                    "Broker redirect uri is invalid. Expected: "
                            + expectedBrokerRedirectUri
                            + " Actual: "
                            + redirectUri
                    ,
                    null
            );
        }

        return isValidBrokerRedirect;
    }

    @Override
    @Nullable
    public String getEnrollmentId(@NonNull final String userId, @NonNull final String packageName) {
        return IntuneMAMEnrollmentIdGateway
                .getInstance().getEnrollmentId(
                        mContext,
                        userId,
                        packageName
                );
    }

    @Override
    public void onReturnCommandResult(@NonNull ICommand<?> command) {
        optionallyReorderTasks(command);
    }

    @Override
    public long getNanosecondTime() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1) {
            return SystemClock.elapsedRealtimeNanos();
        } else {
            return System.nanoTime();
        }
    }

    @Override
    public void postCommandResult(@NonNull Runnable runnable) {
        final Handler handler = new Handler(Looper.getMainLooper());
        handler.post(runnable);
    }

    @Override
    public KeyManagerFactory getSslContextKeyManagerFactory() throws NoSuchAlgorithmException {
        return KeyManagerFactory.getInstance("X509");
    }

    @Nullable
    @Override
    public String getPackageNameFromUid(int uid) {
        return mContext.getPackageManager().getNameForUid(uid);
    }

    @Override
    public List<Map.Entry<String, String>> updateWithAndGetPlatformSpecificExtraQueryParameters(@Nullable List<Map.Entry<String, String>> originalList) {
        List<Map.Entry<String, String>> queryParams = originalList != null ?  new ArrayList<>(originalList) : new ArrayList<>();

        // Passkey feature support is only for Android at the moment.
        final  Map.Entry<String, String> webauthnParam = new AbstractMap.SimpleEntry<>(FidoConstants.WEBAUTHN_QUERY_PARAMETER_FIELD, FidoConstants.WEBAUTHN_QUERY_PARAMETER_VALUE);
        if (CommonFlightsManager.INSTANCE.getFlightsProvider().isFlightEnabled(CommonFlight.ENABLE_PASSKEY_FEATURE)) {
            if (!queryParams.contains(webauthnParam)) {
                queryParams.add(webauthnParam);
            }
        } else {
            // If we don't want to add this query string param, then we should also remove other instances of it that might be already present from MSAL/OneAuth-MSAL.
            queryParams.remove(webauthnParam);
        }
        return queryParams;
    }

    /**
     * This method optionally re-orders tasks to bring the task that launched
     * the interactive activity to the foreground. This is useful when the activity provided
     * to us does not have a taskAffinity and as a result it's possible that other apps or the home
     * screen could be in the task stack ahead of the app that launched the interactive
     * authorization UI.
     *
     * @param command The BaseCommand.
     */
    private void optionallyReorderTasks(@NonNull final ICommand<?> command) {
        final String methodTag = TAG + ":optionallyReorderTasks";
        if (command instanceof InteractiveTokenCommand) {
            if (mActivity == null) {
                throw new IllegalStateException("Activity cannot be null in an interactive session.");
            }

            final InteractiveTokenCommand interactiveTokenCommand = (InteractiveTokenCommand) command;
            final InteractiveTokenCommandParameters interactiveTokenCommandParameters = (InteractiveTokenCommandParameters) interactiveTokenCommand.getParameters();
            if (interactiveTokenCommandParameters.getHandleNullTaskAffinity() && !hasTaskAffinity(mActivity)) {
                //If an interactive command doesn't have a task affinity bring the
                //task that launched the command to the foreground
                //In order for this to work the app has to have requested the re-order tasks permission
                //https://developer.android.com/reference/android/Manifest.permission#REORDER_TASKS
                //if the permission has not been granted nothing will happen if you just invoke the method
                final ActivityManager activityManager = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
                if (activityManager != null) {
                    activityManager.moveTaskToFront(mActivity.getTaskId(), 0);
                } else {
                    Logger.warn(methodTag, "ActivityManager was null; Unable to bring task for the foreground.");
                }
            }
        }
    }

    private static boolean hasTaskAffinity(@NonNull final Activity activity) {
        final String methodTag = TAG + ":hasTaskAffinity";
        final PackageManager packageManager = activity.getPackageManager();
        try {
            final ComponentName componentName = activity.getComponentName();
            final ActivityInfo startActivityInfo = componentName != null ? packageManager.getActivityInfo(componentName, 0) : null;
            if (startActivityInfo == null) {
                return false;
            }
            return startActivityInfo.taskAffinity != null;
        } catch (final PackageManager.NameNotFoundException e) {
            Logger.warn(
                    methodTag,
                    "Unable to get ActivityInfo for activity provided to start authorization."
            );

            //Normally all tasks have an affinity unless configured explicitly for multi-window support to not have one
            return true;
        }
    }

    private boolean isValidHubRedirectURIForNAATests(String redirectUri) {
        // The only allow-listed hub app on ESTS is Teams app. We cannot use our test app's clientId/redirecrURI for testing NAA scenarios
        // Below redirectURI is being used in our automation tests and also by OneAuth tests for NAA
        return BuildConfig.DEBUG && (redirectUri.equals("msauth://com.microsoft.teams/VCpKgbYCXucoq1mZ4BZPsh5taNE=")
                || redirectUri.equals("msauth://com.microsoft.teams/fcg80qvoM1YMKJZibjBwQcDfOno=")
                || redirectUri.equals("https://login.microsoftonline.com/common/oauth2/nativeclient"));
    }
}
