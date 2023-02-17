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
package com.microsoft.identity.common.internal.controllers;

import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.CLIENT_ADVERTISED_MAXIMUM_BP_VERSION_KEY;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.CLIENT_CONFIGURED_MINIMUM_BP_VERSION_KEY;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.MSAL_TO_BROKER_PROTOCOL_NAME;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.MSAL_TO_BROKER_PROTOCOL_VERSION_CODE;
import static com.microsoft.identity.common.internal.broker.ipc.BrokerOperationBundle.Operation.MSAL_ACQUIRE_TOKEN_DCF;
import static com.microsoft.identity.common.internal.broker.ipc.BrokerOperationBundle.Operation.MSAL_FETCH_DCF_AUTH_RESULT;
import static com.microsoft.identity.common.internal.broker.ipc.BrokerOperationBundle.Operation.MSAL_ACQUIRE_TOKEN_SILENT;
import static com.microsoft.identity.common.internal.broker.ipc.BrokerOperationBundle.Operation.MSAL_GENERATE_SHR;
import static com.microsoft.identity.common.internal.broker.ipc.BrokerOperationBundle.Operation.MSAL_GET_ACCOUNTS;
import static com.microsoft.identity.common.internal.broker.ipc.BrokerOperationBundle.Operation.MSAL_GET_CURRENT_ACCOUNT_IN_SHARED_DEVICE;
import static com.microsoft.identity.common.internal.broker.ipc.BrokerOperationBundle.Operation.MSAL_GET_DEVICE_MODE;
import static com.microsoft.identity.common.internal.broker.ipc.BrokerOperationBundle.Operation.MSAL_GET_INTENT_FOR_INTERACTIVE_REQUEST;
import static com.microsoft.identity.common.internal.broker.ipc.BrokerOperationBundle.Operation.MSAL_REMOVE_ACCOUNT;
import static com.microsoft.identity.common.internal.broker.ipc.BrokerOperationBundle.Operation.MSAL_SIGN_OUT_FROM_SHARED_DEVICE;
import static com.microsoft.identity.common.internal.broker.ipc.BrokerOperationBundle.Operation.MSAL_SSO_TOKEN;
import static com.microsoft.identity.common.internal.controllers.BrokerOperationExecutor.BrokerOperation;
import static com.microsoft.identity.common.java.AuthenticationConstants.LocalBroadcasterAliases.RETURN_BROKER_INTERACTIVE_ACQUIRE_TOKEN_RESULT;
import static com.microsoft.identity.common.java.AuthenticationConstants.LocalBroadcasterFields.REQUEST_CODE;
import static com.microsoft.identity.common.java.AuthenticationConstants.LocalBroadcasterFields.RESULT_CODE;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.microsoft.identity.common.components.AndroidPlatformComponentsFactory;
import com.microsoft.identity.common.PropertyBagUtil;
import com.microsoft.identity.common.adal.internal.AuthenticationConstants;
import com.microsoft.identity.common.internal.broker.BrokerActivity;
import com.microsoft.identity.common.internal.broker.BrokerResult;
import com.microsoft.identity.common.internal.broker.BrokerValidator;
import com.microsoft.identity.common.internal.broker.MicrosoftAuthClient;
import com.microsoft.identity.common.internal.broker.ipc.AccountManagerAddAccountStrategy;
import com.microsoft.identity.common.internal.broker.ipc.BoundServiceStrategy;
import com.microsoft.identity.common.internal.broker.ipc.BrokerOperationBundle;
import com.microsoft.identity.common.internal.broker.ipc.ContentProviderStrategy;
import com.microsoft.identity.common.internal.broker.ipc.IIpcStrategy;
import com.microsoft.identity.common.internal.cache.HelloCache;
import com.microsoft.identity.common.internal.commands.parameters.AndroidActivityInteractiveTokenCommandParameters;
import com.microsoft.identity.common.internal.request.MsalBrokerRequestAdapter;
import com.microsoft.identity.common.internal.result.MsalBrokerResultAdapter;
import com.microsoft.identity.common.internal.telemetry.Telemetry;
import com.microsoft.identity.common.internal.telemetry.TelemetryEventStrings;
import com.microsoft.identity.common.internal.telemetry.events.ApiEndEvent;
import com.microsoft.identity.common.internal.telemetry.events.ApiStartEvent;
import com.microsoft.identity.common.internal.util.AccountManagerUtil;
import com.microsoft.identity.common.internal.util.StringUtil;
import com.microsoft.identity.common.java.WarningType;
import com.microsoft.identity.common.java.authorities.AzureActiveDirectoryAudience;
import com.microsoft.identity.common.java.authscheme.PopAuthenticationSchemeWithClientKeyInternal;
import com.microsoft.identity.common.java.cache.ICacheRecord;
import com.microsoft.identity.common.java.cache.MsalOAuth2TokenCache;
import com.microsoft.identity.common.java.commands.AcquirePrtSsoTokenResult;
import com.microsoft.identity.common.java.commands.parameters.AcquirePrtSsoTokenCommandParameters;
import com.microsoft.identity.common.java.commands.parameters.CommandParameters;
import com.microsoft.identity.common.java.commands.parameters.DeviceCodeFlowCommandParameters;
import com.microsoft.identity.common.java.commands.parameters.GenerateShrCommandParameters;
import com.microsoft.identity.common.java.commands.parameters.InteractiveTokenCommandParameters;
import com.microsoft.identity.common.java.commands.parameters.RemoveAccountCommandParameters;
import com.microsoft.identity.common.java.commands.parameters.RopcTokenCommandParameters;
import com.microsoft.identity.common.java.commands.parameters.SilentTokenCommandParameters;
import com.microsoft.identity.common.java.commands.parameters.TokenCommandParameters;
import com.microsoft.identity.common.java.controllers.BaseController;
import com.microsoft.identity.common.java.exception.BaseException;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.exception.ErrorStrings;
import com.microsoft.identity.common.java.exception.ServiceException;
import com.microsoft.identity.common.java.interfaces.IPlatformComponents;
import com.microsoft.identity.common.java.providers.microsoft.MicrosoftRefreshToken;
import com.microsoft.identity.common.java.providers.microsoft.azureactivedirectory.ClientInfo;
import com.microsoft.identity.common.java.providers.microsoft.microsoftsts.MicrosoftStsAccount;
import com.microsoft.identity.common.java.providers.oauth2.AuthorizationResult;
import com.microsoft.identity.common.java.providers.oauth2.IDToken;
import com.microsoft.identity.common.java.result.AcquireTokenResult;
import com.microsoft.identity.common.java.result.GenerateShrResult;
import com.microsoft.identity.common.java.util.BrokerProtocolVersionUtil;
import com.microsoft.identity.common.java.util.ResultFuture;
import com.microsoft.identity.common.java.util.ThreadUtils;
import com.microsoft.identity.common.java.util.ported.LocalBroadcaster;
import com.microsoft.identity.common.java.util.ported.PropertyBag;
import com.microsoft.identity.common.logging.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import lombok.EqualsAndHashCode;

/**
 * The implementation of MSAL Controller for Broker.
 */
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class BrokerMsalController extends BaseController {

    private static final String TAG = BrokerMsalController.class.getSimpleName();
    private static final long WAIT_BETWEEN_DCF_POLLING_MILLISECONDS = TimeUnit.SECONDS.toMillis(5);

    protected final MsalBrokerRequestAdapter mRequestAdapter = new MsalBrokerRequestAdapter();
    protected final MsalBrokerResultAdapter mResultAdapter = new MsalBrokerResultAdapter();

    private ResultFuture<Bundle> mBrokerResultFuture;
    private final String mActiveBrokerPackageName;
    private final BrokerOperationExecutor mBrokerOperationExecutor;
    private final HelloCache mHelloCache;
    private final IPlatformComponents mComponents;

    private final Context mApplicationContext;

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    public BrokerMsalController(@NonNull final Context applicationContext, @NonNull final IPlatformComponents components) {
        mComponents = components;
        mApplicationContext = applicationContext;
        mActiveBrokerPackageName = getActiveBrokerPackageName();
        if (StringUtil.isEmpty(mActiveBrokerPackageName)) {
            throw new IllegalStateException("Active Broker not found. This class should not be initialized.");
        }

        mBrokerOperationExecutor = new BrokerOperationExecutor(getIpcStrategies(mApplicationContext, mActiveBrokerPackageName));
        mHelloCache = getHelloCache();
    }

    public BrokerMsalController(@NonNull final Context applicationContext) {
        mComponents = AndroidPlatformComponentsFactory.createFromContext(applicationContext);
        mApplicationContext = applicationContext;
        mActiveBrokerPackageName = getActiveBrokerPackageName();
        if (TextUtils.isEmpty(mActiveBrokerPackageName)) {
            throw new IllegalStateException("Active Broker not found. This class should not be initialized.");
        }

        mBrokerOperationExecutor = new BrokerOperationExecutor(getIpcStrategies(mApplicationContext, mActiveBrokerPackageName));
        mHelloCache = getHelloCache();
    }

    @VisibleForTesting
    public HelloCache getHelloCache() {
        return new HelloCache(mApplicationContext, MSAL_TO_BROKER_PROTOCOL_NAME, mActiveBrokerPackageName,
                mComponents);
    }

    @VisibleForTesting
    public String getActiveBrokerPackageName() {
        return new BrokerValidator(mApplicationContext).getCurrentActiveBrokerPackageName();
    }

    /**
     * Gets a list of communication strategies.
     * Order of objects in the list will reflects the order of strategies that will be used.
     */
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    @NonNull
    protected List<IIpcStrategy> getIpcStrategies(final Context applicationContext, final String activeBrokerPackageName) {
        final String methodTag = TAG + ":getIpcStrategies";
        final List<IIpcStrategy> strategies = new ArrayList<>();
        final StringBuilder sb = new StringBuilder(100);
        sb.append("Broker Strategies added : ");

        final ContentProviderStrategy contentProviderStrategy = new ContentProviderStrategy(applicationContext);
        if (contentProviderStrategy.isBrokerContentProviderAvailable(activeBrokerPackageName)) {
            sb.append("ContentProviderStrategy, ");
            strategies.add(contentProviderStrategy);
        }

        final MicrosoftAuthClient client = new MicrosoftAuthClient(applicationContext);
        if (client.isBoundServiceSupported(activeBrokerPackageName)) {
            sb.append("BoundServiceStrategy, ");
            strategies.add(new BoundServiceStrategy<>(client));
        }

        if (AccountManagerUtil.canUseAccountManagerOperation(applicationContext)) {
            sb.append("AccountManagerStrategy.");
            strategies.add(new AccountManagerAddAccountStrategy(applicationContext));
        }

        Logger.info(methodTag, sb.toString());

        return strategies;
    }

    /**
     * MSAL-Broker handshake operation.
     *
     * @param strategy            an {@link IIpcStrategy}
     * @param minRequestedVersion the minimum allowed broker protocol version, may be null.
     * @return a protocol version negotiated by MSAL and Broker.
     */
    @VisibleForTesting
    public @NonNull
    String hello(final @NonNull IIpcStrategy strategy,
                 final @Nullable String minRequestedVersion) throws BaseException {

        final String cachedProtocolVersion = mHelloCache.tryGetNegotiatedProtocolVersion(
                minRequestedVersion, MSAL_TO_BROKER_PROTOCOL_VERSION_CODE);

        if (!StringUtil.isEmpty(cachedProtocolVersion)) {
            return cachedProtocolVersion;
        }

        final Bundle bundle = new Bundle();
        bundle.putString(
                CLIENT_ADVERTISED_MAXIMUM_BP_VERSION_KEY,
                MSAL_TO_BROKER_PROTOCOL_VERSION_CODE
        );

        if (!StringUtil.isEmpty(minRequestedVersion)) {
            bundle.putString(
                    CLIENT_CONFIGURED_MINIMUM_BP_VERSION_KEY,
                    minRequestedVersion
            );
        }

        final BrokerOperationBundle helloBundle = new BrokerOperationBundle(
                BrokerOperationBundle.Operation.MSAL_HELLO,
                mActiveBrokerPackageName,
                bundle);

        final String negotiatedProtocolVersion = mResultAdapter.verifyHelloFromResultBundle(
                mActiveBrokerPackageName,
                strategy.communicateToBroker(helloBundle)
        );

        mHelloCache.saveNegotiatedProtocolVersion(
                minRequestedVersion,
                MSAL_TO_BROKER_PROTOCOL_VERSION_CODE,
                negotiatedProtocolVersion);

        return negotiatedProtocolVersion;
    }

    /**
     * Performs interactive acquire token with Broker.
     *
     * @param parameters a {@link InteractiveTokenCommandParameters}
     * @return an {@link AcquireTokenResult}.
     */
    @Override
    public AcquireTokenResult acquireToken(final @NonNull InteractiveTokenCommandParameters parameters)
            throws BaseException, InterruptedException, ExecutionException {
        final String methodTag = TAG + ":acquireToken";

        Telemetry.emit(
                new ApiStartEvent()
                        .putProperties(parameters)
                        .putApiId(TelemetryEventStrings.Api.BROKER_ACQUIRE_TOKEN_INTERACTIVE)
        );

        //Create BrokerResultFuture to block on response from the broker... response will be return as an activity result
        //BrokerActivity will receive the result and ask the API dispatcher to complete the request
        //In completeAcquireToken below we will set the result on the future and unblock the flow.
        mBrokerResultFuture = new ResultFuture<>();

        //Get the broker interactive parameters intent
        final Intent interactiveRequestIntent = getBrokerAuthorizationIntent(parameters);

        Activity activity = null;
        if (parameters instanceof AndroidActivityInteractiveTokenCommandParameters) {
            activity = ((AndroidActivityInteractiveTokenCommandParameters) parameters).getActivity();
        }

        //Pass this intent to the BrokerActivity which will be used to start this activity
        final Intent brokerActivityIntent = new Intent(mApplicationContext, BrokerActivity.class);
        brokerActivityIntent.putExtra(BrokerActivity.BROKER_INTENT, interactiveRequestIntent);

        LocalBroadcaster.INSTANCE.registerCallback(RETURN_BROKER_INTERACTIVE_ACQUIRE_TOKEN_RESULT,
                new LocalBroadcaster.IReceiverCallback() {
                    @Override
                    public void onReceive(@NonNull PropertyBag propertyBag) {
                        /**
                         * Get the response from the Broker captured by BrokerActivity.
                         * BrokerActivity will pass along the response to the broker controller.
                         * The Broker controller will map the response into the broker result
                         * and signal the future with the broker result to unblock the request.
                         */

                        Logger.verbose(
                                methodTag,
                                "Received result from Broker..."
                        );

                        Telemetry.emit(
                                new ApiStartEvent()
                                        .putApiId(TelemetryEventStrings.Api.BROKER_COMPLETE_ACQUIRE_TOKEN_INTERACTIVE)
                                        .put(TelemetryEventStrings.Key.REQUEST_CODE, propertyBag.<Integer>getOrDefault(REQUEST_CODE, -1).toString())
                                        .put(TelemetryEventStrings.Key.RESULT_CODE, propertyBag.<Integer>getOrDefault(RESULT_CODE, -1).toString())
                        );

                        mBrokerResultFuture.setResult(PropertyBagUtil.toBundle(propertyBag));

                        Telemetry.emit(
                                new ApiEndEvent()
                                        .putApiId(TelemetryEventStrings.Api.BROKER_COMPLETE_ACQUIRE_TOKEN_INTERACTIVE)
                        );

                        LocalBroadcaster.INSTANCE.unregisterCallback(RETURN_BROKER_INTERACTIVE_ACQUIRE_TOKEN_RESULT);
                    }
                });

        if (null == activity) {
            // To support calling from OneAuth-MSAL, which may be initialized without an Activity
            // add Flags to start as a NEW_TASK if we are launching from an application Context
            brokerActivityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mApplicationContext.startActivity(brokerActivityIntent);
        } else {
            // Start the BrokerActivity using our existing Activity
            activity.startActivity(brokerActivityIntent);
        }

        final AcquireTokenResult result;
        try {
            //Wait to be notified of the result being returned... we could add a timeout here if we want to
            final Bundle resultBundle = mBrokerResultFuture.get();

            // For MSA Accounts Broker doesn't save the accounts, instead it just passes the result along,
            // MSAL needs to save this account locally for future token calls.
            // parameters.getOAuth2TokenCache() will be non-null only in case of MSAL native
            // If the request is from MSALCPP , OAuth2TokenCache will be null.
            if (parameters.getOAuth2TokenCache() != null) {
                saveMsaAccountToCache(resultBundle, (MsalOAuth2TokenCache) parameters.getOAuth2TokenCache());
            }

            result = new MsalBrokerResultAdapter().getAcquireTokenResultFromResultBundle(resultBundle);
        } catch (final BaseException | ExecutionException e) {
            Telemetry.emit(
                    new ApiEndEvent()
                            .putException(e)
                            .putApiId(TelemetryEventStrings.Api.BROKER_ACQUIRE_TOKEN_INTERACTIVE)
            );
            throw e;
        }

        Telemetry.emit(
                new ApiEndEvent()
                        .putResult(result)
                        .putApiId(TelemetryEventStrings.Api.BROKER_ACQUIRE_TOKEN_INTERACTIVE)
        );

        return result;
    }

    @Override
    public void onFinishAuthorizationSession(int requestCode,
                                             int resultCode,
                                             @NonNull final PropertyBag data) {
        throw new UnsupportedOperationException();
    }

    /**
     * Get the intent for the broker interactive request
     *
     * @param parameters a {@link InteractiveTokenCommandParameters}
     * @return an {@link Intent} for initiating Broker interactive activity.
     */
    private @NonNull
    Intent getBrokerAuthorizationIntent(
            final @NonNull InteractiveTokenCommandParameters parameters) throws BaseException {
        return mBrokerOperationExecutor.execute(parameters,
                new BrokerOperation<Intent>() {
                    private String negotiatedBrokerProtocolVersion;

                    @Override
                    public void performPrerequisites(final @NonNull IIpcStrategy strategy) throws BaseException {
                        verifyTokenParametersAreSupported(parameters);
                        negotiatedBrokerProtocolVersion = hello(strategy, parameters.getRequiredBrokerProtocolVersion());
                    }

                    @Override
                    public @NonNull
                    BrokerOperationBundle getBundle() {
                        return new BrokerOperationBundle(
                                MSAL_GET_INTENT_FOR_INTERACTIVE_REQUEST,
                                mActiveBrokerPackageName,
                                null);
                    }

                    @Override
                    public @NonNull
                    Intent extractResultBundle(final @Nullable Bundle resultBundle) throws BaseException {
                        if (resultBundle == null) {
                            throw mResultAdapter.getExceptionForEmptyResultBundle();
                        }

                        final Intent intent = mResultAdapter.getIntentForInteractiveRequestFromResultBundle(
                                resultBundle,
                                negotiatedBrokerProtocolVersion);
                        intent.putExtras(
                                mRequestAdapter.getRequestBundleForAcquireTokenInteractive(parameters, negotiatedBrokerProtocolVersion)
                        );
                        return intent;
                    }

                    @Override
                    public @NonNull
                    String getMethodName() {
                        return ":getBrokerAuthorizationIntent";
                    }

                    @Override
                    public @Nullable
                    String getTelemetryApiId() {
                        return null;
                    }

                    @Override
                    public void putValueInSuccessEvent(final @NonNull ApiEndEvent event, final @NonNull Intent result) {
                    }
                });
    }

    // Suppressing rawtype warnings due to the generic type AuthorizationResult
    @SuppressWarnings(WarningType.rawtype_warning)
    @Override
    public AuthorizationResult deviceCodeFlowAuthRequest(final DeviceCodeFlowCommandParameters parameters)
            throws BaseException, ClientException {
        // IPC to Broker : fetch DCF auth result
        return mBrokerOperationExecutor.execute(parameters,
                new BrokerOperation<AuthorizationResult>() {
                    private String negotiatedBrokerProtocolVersion;

                    @Override
                    public void performPrerequisites(final @NonNull IIpcStrategy strategy) throws BaseException {
                        negotiatedBrokerProtocolVersion = hello(strategy, parameters.getRequiredBrokerProtocolVersion());
                    }

                    @Override
                    public BrokerOperationBundle getBundle() {
                        return new BrokerOperationBundle(MSAL_FETCH_DCF_AUTH_RESULT,
                                mActiveBrokerPackageName,
                                mRequestAdapter.getRequestBundleForDeviceCodeFlowAuthRequest(
                                        mApplicationContext,
                                        parameters,
                                        negotiatedBrokerProtocolVersion));
                    }

                    @Override
                    public AuthorizationResult extractResultBundle(final @Nullable Bundle resultBundle) throws BaseException {
                        if (resultBundle == null) {
                            throw mResultAdapter.getExceptionForEmptyResultBundle();
                        }

                        return mResultAdapter.getDeviceCodeFlowAuthResultFromResultBundle(resultBundle);
                    }

                    @Override
                    public @NonNull
                    String getMethodName() {
                        return ":deviceCodeFlowAuthRequest";
                    }

                    @Override
                    public @Nullable
                    String getTelemetryApiId() {
                        return null;
                    }

                    @Override
                    public void putValueInSuccessEvent(final @NonNull ApiEndEvent event, final @NonNull AuthorizationResult result) {
                    }

                });
    }

    public AcquireTokenResult acquireDeviceCodeFlowToken(
            @SuppressWarnings(WarningType.rawtype_warning) final AuthorizationResult authorizationResult,
            final DeviceCodeFlowCommandParameters parameters)
            throws BaseException, ClientException {

        // IPC to Broker : AcquireTokenWithDCF API in Broker
        return mBrokerOperationExecutor.execute(parameters,
                new BrokerOperation<AcquireTokenResult>() {
                    private String negotiatedBrokerProtocolVersion;

                    @Override
                    public void performPrerequisites(final @NonNull IIpcStrategy strategy) throws BaseException {
                        // hello ipc
                        negotiatedBrokerProtocolVersion = hello(strategy, parameters.getRequiredBrokerProtocolVersion());
                    }

                    @Override
                    public BrokerOperationBundle getBundle() {
                        // Call Broker to make a request to fetch DCF authorization result
                        // Note : Broker API here is to only fetch the authorization result which has the verificationUri, userCode, expiration time and message.
                        return new BrokerOperationBundle(MSAL_ACQUIRE_TOKEN_DCF,
                                mActiveBrokerPackageName,
                                mRequestAdapter.getRequestBundleForDeviceCodeFlowTokenRequest(
                                        mApplicationContext,
                                        parameters,
                                        authorizationResult,
                                        negotiatedBrokerProtocolVersion));
                    }

                    @Override
                    public AcquireTokenResult extractResultBundle(final @Nullable Bundle resultBundle) throws BaseException {
                        if (resultBundle == null) {
                            throw mResultAdapter.getExceptionForEmptyResultBundle();
                        }

                        AcquireTokenResult acquireTokenResult = mResultAdapter.getDeviceCodeFlowTokenResultFromResultBundle(resultBundle);
                        // If authorization_pending continue polling for token
                        if (acquireTokenResult == null) {
                            // Wait between polls for 5 secs
                            ThreadUtils.sleepSafely((int) WAIT_BETWEEN_DCF_POLLING_MILLISECONDS, TAG,
                                    "Attempting to sleep thread during Device Code Flow token polling...");
                            return acquireDeviceCodeFlowToken(authorizationResult, parameters);
                        } else {
                            return acquireTokenResult;
                        }
                    }

                    @Override
                    public @NonNull
                    String getMethodName() {
                        return ":deviceCodeFlowAuthRequest";
                    }

                    @Override
                    public @Nullable
                    String getTelemetryApiId() {
                        return null;
                    }

                    @Override
                    public void putValueInSuccessEvent(final @NonNull ApiEndEvent event, final @NonNull AcquireTokenResult result) {
                        event.putResult(result);
                    }

                });
    }

    /**
     * Performs acquire token silent with Broker.
     *
     * @param parameters a {@link SilentTokenCommandParameters}
     * @return an {@link AcquireTokenResult}.
     */
    @Override
    public @NonNull
    AcquireTokenResult acquireTokenSilent(final @NonNull SilentTokenCommandParameters parameters) throws BaseException {
        return mBrokerOperationExecutor.execute(parameters,
                new BrokerOperation<AcquireTokenResult>() {
                    private String negotiatedBrokerProtocolVersion;

                    @Override
                    public void performPrerequisites(final @NonNull IIpcStrategy strategy) throws BaseException {
                        verifyTokenParametersAreSupported(parameters);
                        negotiatedBrokerProtocolVersion = hello(strategy, parameters.getRequiredBrokerProtocolVersion());
                    }

                    @Override
                    public @NonNull
                    BrokerOperationBundle getBundle() {
                        return new BrokerOperationBundle(MSAL_ACQUIRE_TOKEN_SILENT,
                                mActiveBrokerPackageName,
                                mRequestAdapter.getRequestBundleForAcquireTokenSilent(
                                        mApplicationContext,
                                        parameters,
                                        negotiatedBrokerProtocolVersion
                                ));
                    }

                    @Override
                    public @NonNull
                    AcquireTokenResult extractResultBundle(final @Nullable Bundle resultBundle) throws BaseException {
                        if (resultBundle == null) {
                            throw mResultAdapter.getExceptionForEmptyResultBundle();
                        }
                        return mResultAdapter.getAcquireTokenResultFromResultBundle(resultBundle);
                    }

                    @Override
                    public @NonNull
                    String getMethodName() {
                        return ":acquireTokenSilent";
                    }

                    @Override
                    public @NonNull
                    String getTelemetryApiId() {
                        return TelemetryEventStrings.Api.BROKER_ACQUIRE_TOKEN_SILENT;
                    }

                    @Override
                    public void putValueInSuccessEvent(final @NonNull ApiEndEvent event, final @NonNull AcquireTokenResult result) {
                        event.putResult(result);
                    }
                });
    }

    /**
     * Returns account(s) that has previously been used to acquire token with broker through the calling app.
     * This only works when getBrokerAccountMode() is BROKER_ACCOUNT_MODE_MULTIPLE_ACCOUNT.
     *
     * @param parameters a {@link CommandParameters}
     * @return a list of {@link ICacheRecord}.
     */
    @Override
    public @NonNull
    List<ICacheRecord> getAccounts(final @NonNull CommandParameters parameters) throws BaseException {
        return mBrokerOperationExecutor.execute(parameters,
                new BrokerOperation<List<ICacheRecord>>() {
                    private String negotiatedBrokerProtocolVersion;

                    @Override
                    public void performPrerequisites(final @NonNull IIpcStrategy strategy) throws BaseException {
                        negotiatedBrokerProtocolVersion = hello(strategy, parameters.getRequiredBrokerProtocolVersion());
                    }

                    @Override
                    public @NonNull
                    BrokerOperationBundle getBundle() {
                        return new BrokerOperationBundle(
                                MSAL_GET_ACCOUNTS,
                                mActiveBrokerPackageName,
                                mRequestAdapter.getRequestBundleForGetAccounts(
                                        parameters,
                                        negotiatedBrokerProtocolVersion
                                ));
                    }

                    @Override
                    public @NonNull
                    List<ICacheRecord> extractResultBundle(final @Nullable Bundle resultBundle) throws BaseException {
                        if (resultBundle == null) {
                            throw mResultAdapter.getExceptionForEmptyResultBundle();
                        }
                        return mResultAdapter.getAccountsFromResultBundle(resultBundle);
                    }

                    @Override
                    public @NonNull
                    String getMethodName() {
                        return ":getAccounts";
                    }

                    @Override
                    public @NonNull
                    String getTelemetryApiId() {
                        return TelemetryEventStrings.Api.BROKER_GET_ACCOUNTS;
                    }

                    @Override
                    public void putValueInSuccessEvent(final @NonNull ApiEndEvent event, final @NonNull List<ICacheRecord> result) {
                        event.put(TelemetryEventStrings.Key.ACCOUNTS_NUMBER, Integer.toString(result.size()));
                    }
                });
    }

    /**
     * Remove a given account from broker.
     *
     * @param parameters a {@link RemoveAccountCommandParameters}
     * @return true if the account is successfully removed.
     */
    @Override
    public boolean removeAccount(final @NonNull RemoveAccountCommandParameters parameters) throws BaseException {
        return mBrokerOperationExecutor.execute(parameters,
                new BrokerOperation<Boolean>() {
                    private String negotiatedBrokerProtocolVersion;

                    @Override
                    public void performPrerequisites(final @NonNull IIpcStrategy strategy) throws BaseException {
                        negotiatedBrokerProtocolVersion = hello(strategy, parameters.getRequiredBrokerProtocolVersion());
                    }

                    @Override
                    public @NonNull
                    BrokerOperationBundle getBundle() {
                        return new BrokerOperationBundle(
                                MSAL_REMOVE_ACCOUNT,
                                mActiveBrokerPackageName,
                                mRequestAdapter.getRequestBundleForRemoveAccount(
                                        parameters,
                                        negotiatedBrokerProtocolVersion
                                ));
                    }

                    @Override
                    public @NonNull
                    Boolean extractResultBundle(final @Nullable Bundle resultBundle) throws BaseException {
                        mResultAdapter.verifyRemoveAccountResultFromBundle(resultBundle);
                        return true;
                    }

                    @Override
                    public @NonNull
                    String getMethodName() {
                        return ":removeAccount";
                    }

                    @Override
                    public @NonNull
                    String getTelemetryApiId() {
                        return TelemetryEventStrings.Api.BROKER_REMOVE_ACCOUNT;
                    }

                    @Override
                    public void putValueInSuccessEvent(final @NonNull ApiEndEvent event, final @NonNull Boolean result) {
                    }
                });
    }

    /**
     * Get device mode from broker.
     *
     * @param parameters a {@link CommandParameters}
     * @return true if the device is in as shared mode. False otherwise.
     */
    @Override
    public boolean getDeviceMode(final @NonNull CommandParameters parameters) throws BaseException {
        return mBrokerOperationExecutor.execute(parameters,
                new BrokerOperation<Boolean>() {
                    @Override
                    public void performPrerequisites(final @NonNull IIpcStrategy strategy) {
                    }

                    @Override
                    public @NonNull
                    BrokerOperationBundle getBundle() {
                        return new BrokerOperationBundle(
                                MSAL_GET_DEVICE_MODE,
                                mActiveBrokerPackageName,
                                null);
                    }

                    @Override
                    public @NonNull
                    Boolean extractResultBundle(final @Nullable Bundle resultBundle) throws BaseException {
                        if (resultBundle == null) {
                            throw mResultAdapter.getExceptionForEmptyResultBundle();
                        }
                        return mResultAdapter.getDeviceModeFromResultBundle(resultBundle);
                    }

                    @Override
                    public @NonNull
                    String getMethodName() {
                        return ":getDeviceMode";
                    }

                    @Override
                    public @NonNull
                    String getTelemetryApiId() {
                        return TelemetryEventStrings.Api.GET_BROKER_DEVICE_MODE;
                    }

                    @Override
                    public void putValueInSuccessEvent(final @NonNull ApiEndEvent event, final @NonNull Boolean result) {
                        event.put(TelemetryEventStrings.Key.IS_DEVICE_SHARED, Boolean.toString(result));
                    }
                });
    }

    /**
     * If the device is in shared mode, returns the account that is currently signed into the device.
     * Otherwise, this will be the same as getAccounts().
     *
     * @param parameters a {@link CommandParameters}
     * @return a list of {@link ICacheRecord}.
     */
    @Override
    public @NonNull
    List<ICacheRecord> getCurrentAccount(final @NonNull CommandParameters parameters) throws BaseException {
        final String methodName = ":getCurrentAccount";

        if (!parameters.isSharedDevice()) {
            Logger.verbose(TAG + methodName, "Not a shared device, invoke getAccounts() instead of getCurrentAccount()");
            return getAccounts(parameters);
        }

        return mBrokerOperationExecutor.execute(parameters,
                new BrokerOperation<List<ICacheRecord>>() {
                    private String negotiatedBrokerProtocolVersion;

                    @Override
                    public void performPrerequisites(final @NonNull IIpcStrategy strategy) throws BaseException {
                        negotiatedBrokerProtocolVersion = hello(strategy, parameters.getRequiredBrokerProtocolVersion());
                    }

                    @Override
                    public @NonNull
                    BrokerOperationBundle getBundle() {
                        return new BrokerOperationBundle(
                                MSAL_GET_CURRENT_ACCOUNT_IN_SHARED_DEVICE,
                                mActiveBrokerPackageName,
                                mRequestAdapter.getRequestBundleForGetAccounts(
                                        parameters,
                                        negotiatedBrokerProtocolVersion
                                ));
                    }

                    @Override
                    public @NonNull
                    List<ICacheRecord> extractResultBundle(final @Nullable Bundle resultBundle) throws BaseException {
                        if (resultBundle == null) {
                            throw mResultAdapter.getExceptionForEmptyResultBundle();
                        }
                        return mResultAdapter.getAccountsFromResultBundle(resultBundle);
                    }

                    @Override
                    public @NonNull
                    String getMethodName() {
                        return methodName;
                    }

                    @Override
                    public @NonNull
                    String getTelemetryApiId() {
                        return TelemetryEventStrings.Api.BROKER_GET_CURRENT_ACCOUNT;
                    }

                    @Override
                    public void putValueInSuccessEvent(final @NonNull ApiEndEvent event, final @NonNull List<ICacheRecord> result) {
                        event.put(TelemetryEventStrings.Key.ACCOUNTS_NUMBER, Integer.toString(result.size()));
                    }
                });
    }

    /**
     * If the device is in shared mode, remove the account that is currently signed into the device.
     * Otherwise, this will be the same as removeAccount().
     *
     * @param parameters a {@link RemoveAccountCommandParameters}
     * @return a list of {@link ICacheRecord}.
     */
    @Override
    public boolean removeCurrentAccount(final @NonNull RemoveAccountCommandParameters parameters) throws BaseException {
        final String methodName = ":removeCurrentAccount";

        if (!parameters.isSharedDevice()) {
            Logger.verbose(methodName, "Not a shared device, invoke removeAccount() instead of removeCurrentAccount()");
            return removeAccount(parameters);
        }

        /*
         * Given an account, perform a global sign-out from this shared device (End my shift capability).
         * This will invoke Broker and
         * 1. Remove account from token cache.
         * 2. Remove account from AccountManager.
         * 3. Clear WebView cookies.
         *
         * If everything succeeds on the broker side, it will then
         * 4. Sign out from default browser.
         */
        return mBrokerOperationExecutor.execute(parameters,
                new BrokerOperation<Boolean>() {
                    private String negotiatedBrokerProtocolVersion;

                    @Override
                    public void performPrerequisites(final @NonNull IIpcStrategy strategy) throws BaseException {
                        negotiatedBrokerProtocolVersion = hello(strategy, parameters.getRequiredBrokerProtocolVersion());
                    }

                    @Override
                    public @NonNull
                    BrokerOperationBundle getBundle() {
                        return new BrokerOperationBundle(
                                MSAL_SIGN_OUT_FROM_SHARED_DEVICE,
                                mActiveBrokerPackageName,
                                mRequestAdapter.getRequestBundleForRemoveAccountFromSharedDevice(
                                        parameters,
                                        negotiatedBrokerProtocolVersion
                                ));
                    }

                    @Override
                    public @NonNull
                    Boolean extractResultBundle(final @Nullable Bundle resultBundle) throws BaseException {
                        mResultAdapter.verifyRemoveAccountResultFromBundle(resultBundle);
                        return true;
                    }

                    @Override
                    public @NonNull
                    String getMethodName() {
                        return methodName;
                    }

                    @Override
                    public @NonNull
                    String getTelemetryApiId() {
                        return TelemetryEventStrings.Api.BROKER_REMOVE_ACCOUNT_FROM_SHARED_DEVICE;
                    }

                    @Override
                    public void putValueInSuccessEvent(final @NonNull ApiEndEvent event, final @NonNull Boolean result) {
                    }
                });
    }

    @Override
    public AcquireTokenResult acquireTokenWithPassword(@lombok.NonNull RopcTokenCommandParameters parameters) throws Exception {
        throw new ClientException("acquireTokenWithPassword() not supported in BrokerMsalController");
    }

    @Override
    public GenerateShrResult generateSignedHttpRequest(@NonNull final GenerateShrCommandParameters parameters) throws Exception {
        return mBrokerOperationExecutor.execute(parameters, new BrokerOperation<GenerateShrResult>() {

            private String negotiatedBrokerProtocolVersion;

            @Override
            public void performPrerequisites(final @NonNull IIpcStrategy strategy) throws BaseException {
                negotiatedBrokerProtocolVersion = hello(strategy, parameters.getRequiredBrokerProtocolVersion());
            }

            @NonNull
            @Override
            public BrokerOperationBundle getBundle() throws ClientException {
                return new BrokerOperationBundle(
                        MSAL_GENERATE_SHR,
                        mActiveBrokerPackageName,
                        mRequestAdapter.getRequestBundleForGenerateShr(
                                parameters,
                                negotiatedBrokerProtocolVersion
                        )
                );
            }

            @NonNull
            @Override
            public GenerateShrResult extractResultBundle(@Nullable final Bundle resultBundle) throws BaseException {
                if (null == resultBundle) {
                    throw mResultAdapter.getExceptionForEmptyResultBundle();
                }

                return mResultAdapter.getGenerateShrResultFromResultBundle(resultBundle);
            }

            @NonNull
            @Override
            public String getMethodName() {
                return ":generateSignedHttpRequest";
            }

            @Nullable
            @Override
            public String getTelemetryApiId() {
                // TODO Needed?
                return null;
            }

            @Override
            public void putValueInSuccessEvent(@NonNull final ApiEndEvent event,
                                               @NonNull final GenerateShrResult result) {
                // TODO Needed?
            }
        });
    }

    public AcquirePrtSsoTokenResult getSsoToken(final @NonNull AcquirePrtSsoTokenCommandParameters parameters) throws BaseException {
        return mBrokerOperationExecutor.execute(parameters, new BrokerOperation<AcquirePrtSsoTokenResult>() {

            private String negotiatedBrokerProtocolVersion;

            @Override
            public void performPrerequisites(final @NonNull IIpcStrategy strategy) throws BaseException {
                negotiatedBrokerProtocolVersion = hello(strategy, parameters.getRequiredBrokerProtocolVersion());
            }

            @NonNull
            @Override
            public BrokerOperationBundle getBundle() throws ClientException {
                return new BrokerOperationBundle(
                        MSAL_SSO_TOKEN,
                        mActiveBrokerPackageName,
                        mRequestAdapter.getRequestBundleForSsoToken(
                                parameters,
                                negotiatedBrokerProtocolVersion
                        )
                );
            }

            @NonNull
            @Override
            public AcquirePrtSsoTokenResult extractResultBundle(@Nullable final Bundle resultBundle) throws BaseException {
                if (null == resultBundle) {
                    throw mResultAdapter.getExceptionForEmptyResultBundle();
                }

                return mResultAdapter.getAcquirePrtSsoTokenResultFromBundle(resultBundle);
            }

            @NonNull
            @Override
            public String getMethodName() {
                return ":getSsoToken";
            }

            @Nullable
            @Override
            public String getTelemetryApiId() {
                // TODO Needed?
                return null;
            }

            @Override
            public void putValueInSuccessEvent(@NonNull final ApiEndEvent event,
                                               @NonNull final AcquirePrtSsoTokenResult result) {
                // TODO Needed?
            }
        });

    }

    /**
     * Checks if the account returns is a MSA Account and sets single on state in cache
     */
    private void saveMsaAccountToCache(final @NonNull Bundle resultBundle,
                                       @SuppressWarnings(WarningType.rawtype_warning) final @NonNull MsalOAuth2TokenCache msalOAuth2TokenCache) throws BaseException {
        final String methodTag = TAG + ":saveMsaAccountToCache";

        final BrokerResult brokerResult = new MsalBrokerResultAdapter().brokerResultFromBundle(resultBundle);

        if (resultBundle.getBoolean(AuthenticationConstants.Broker.BROKER_REQUEST_V2_SUCCESS) &&
                AzureActiveDirectoryAudience.MSA_MEGA_TENANT_ID.equalsIgnoreCase(brokerResult.getTenantId())) {
            Logger.info(methodTag, "Result returned for MSA Account, saving to cache");

            if (StringUtil.isEmpty(brokerResult.getClientInfo())) {
                Logger.error(methodTag, "ClientInfo is empty.", null);
                throw new ClientException(ErrorStrings.UNKNOWN_ERROR, "ClientInfo is empty.");
            }

            try {
                final ClientInfo clientInfo = new ClientInfo(brokerResult.getClientInfo());
                final MicrosoftStsAccount microsoftStsAccount = new MicrosoftStsAccount(
                        new IDToken(brokerResult.getIdToken()),
                        clientInfo
                );
                microsoftStsAccount.setEnvironment(brokerResult.getEnvironment());

                final MicrosoftRefreshToken microsoftRefreshToken = new MicrosoftRefreshToken(
                        brokerResult.getRefreshToken(),
                        clientInfo,
                        brokerResult.getScope(),
                        brokerResult.getClientId(),
                        brokerResult.getEnvironment(),
                        brokerResult.getFamilyId()
                );

                msalOAuth2TokenCacheSetSingleSignOnState(msalOAuth2TokenCache, microsoftStsAccount, microsoftRefreshToken);
            } catch (ServiceException e) {
                Logger.errorPII(methodTag, "Exception while creating Idtoken or ClientInfo," +
                        " cannot save MSA account tokens", e
                );
                throw new ClientException(ErrorStrings.INVALID_JWT, e.getMessage(), e);
            }
        }

    }

    // Suppressing unchecked warnings due to casting of MicrosoftStsAccount to GenericAccount and MicrosoftRefreshToken to GenericRefreshToken in the call to setSingleSignOnState method
    @SuppressWarnings(WarningType.unchecked_warning)
    private void msalOAuth2TokenCacheSetSingleSignOnState(@SuppressWarnings(WarningType.rawtype_warning) @NonNull MsalOAuth2TokenCache msalOAuth2TokenCache, MicrosoftStsAccount microsoftStsAccount, MicrosoftRefreshToken microsoftRefreshToken) throws ClientException {
        msalOAuth2TokenCache.setSingleSignOnState(microsoftStsAccount, microsoftRefreshToken);
    }

    /**
     * Verifies if the token parameters are supported by the required broker protocol version
     * @param parameters Token Parameters for verify
     * @throws ClientException if the token parameters are not supported
     */
    private void verifyTokenParametersAreSupported(@NonNull final TokenCommandParameters parameters) throws ClientException {
        final String requiredProtocolVersion = parameters.getRequiredBrokerProtocolVersion();
        if (parameters.getAuthenticationScheme() instanceof PopAuthenticationSchemeWithClientKeyInternal
                && !BrokerProtocolVersionUtil.canSupportPopAuthenticationSchemeWithClientKey(requiredProtocolVersion)){
            throw new ClientException(ClientException.AUTH_SCHEME_NOT_SUPPORTED,
                    "The min broker protocol version for PopAuthenticationSchemeWithClientKey should be equal or more than 11.0."
            + " Current required version is set to: " + parameters.getRequiredBrokerProtocolVersion());
        }
    }
}
