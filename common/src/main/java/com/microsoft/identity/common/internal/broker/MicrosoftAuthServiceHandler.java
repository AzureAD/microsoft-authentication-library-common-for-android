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

package com.microsoft.identity.common.internal.broker;

import android.accounts.AccountManager;
import android.accounts.AuthenticatorDescription;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;

import com.microsoft.identity.common.adal.internal.AuthenticationConstants;
import com.microsoft.identity.common.exception.ClientException;
import com.microsoft.identity.common.exception.ErrorStrings;
import com.microsoft.identity.common.internal.logging.Logger;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

public class MicrosoftAuthServiceHandler {
    private static final String TAG = MicrosoftAuthServiceHandler.class.getSimpleName();
    private static final String MICROSOFT_AUTH_SERVICE_INTENT_FILTER = "com.microsoft.workaccount.brokeraccount.MicrosoftAuth";
    private static final String MICROSOFT_AUTH_SERVICE_CLASS_NAME = "com.microsoft.workaccount.brokeraccount.MicrosoftAuthService";

    private ConcurrentMap<MicrosoftAuthServiceConnection, CallbackExecutor<MicrosoftAuthServiceConnection>> mPendingConnections = new ConcurrentHashMap<>();
    private static ExecutorService sThreadExecutor = Executors.newCachedThreadPool();

    /**
     * Silently acquire the token from MicrosoftAuthService
     *
     * @param context       The application {@link Context}.
     * @param requestBundle The request data for the silent request.
     * @return The {@link Bundle} result from the MicrosoftAuthService.
     * @throws {@link ClientException} if failed to get token from the service.
     */
    public Bundle getAuthToken(final Context context, final Bundle requestBundle) throws ClientException {
        final String methodName = ":getAuthToken";
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        final AtomicReference<Bundle> bundleResult = new AtomicReference<>(null);
        final AtomicReference<Throwable> exception = new AtomicReference<>(null);

        performAsyncCallOnBound(context, new Callback<MicrosoftAuthServiceConnection>() {
            @Override
            public void onSuccess(MicrosoftAuthServiceConnection result) {
                final IMicrosoftAuthService authService = result.getMicrosoftAuthServiceProvider();
                try {
                    bundleResult.set(authService.acquireTokenSilently(prepareGetAuthTokenRequestData(context, requestBundle)));
                } catch (final RemoteException remoteException) {
                    exception.set(remoteException);
                }

                countDownLatch.countDown();
            }

            @Override
            public void onError(Throwable throwable) {
                exception.set(throwable);
                countDownLatch.countDown();
            }
        });

        try {
            countDownLatch.await();
        } catch (final InterruptedException e) {
            exception.set(e);
        }

        final Throwable throwable = exception.getAndSet(null);
        //ClientException with error code BROKER_APP_NOT_RESPONDING will be thrown if there is any exception thrown during binding the service.
        if (throwable != null) {
            if (throwable instanceof RemoteException) {
                Logger.error(TAG + methodName, null, "Get error when trying to get token from broker. " + throwable.getMessage(), throwable);
                throw new ClientException(ErrorStrings.BROKER_APP_NOT_RESPONDING, throwable.getMessage(), throwable);
            } else if (throwable instanceof InterruptedException) {
                Logger.error(TAG + methodName, null, "The MicrosoftAuthService binding call is interrupted. " + throwable.getMessage(), throwable);
                throw new ClientException(ErrorStrings.BROKER_APP_NOT_RESPONDING, throwable.getMessage(), throwable);
            } else {
                Logger.error(TAG + methodName, null, "Get error when trying to bind the MicrosoftAuthService. " + throwable.getMessage(), throwable);
                throw new ClientException(ErrorStrings.BROKER_APP_NOT_RESPONDING, throwable.getMessage(), throwable);
            }
        }

        return bundleResult.getAndSet(null);
    }

    /**
     * Get Broker users is a blocking call, cannot be executed on the main thread.
     *
     * @return A bundle in the broker. If no user exists in the broker, empty array will be returned.
     */
    public Bundle getBrokerUsers(final Context context) throws IOException {
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        final AtomicReference<Bundle> userBundle = new AtomicReference<>(null);
        final AtomicReference<Throwable> exception = new AtomicReference<>(null);

        performAsyncCallOnBound(context, new Callback<MicrosoftAuthServiceConnection>() {
            @Override
            public void onSuccess(MicrosoftAuthServiceConnection connection) {
                final IMicrosoftAuthService authService = connection.getMicrosoftAuthServiceProvider();
                try {
                    userBundle.set(authService.getBrokerUsers());
                } catch (final RemoteException ex) {
                    exception.set(ex);
                }

                countDownLatch.countDown();
            }

            @Override
            public void onError(Throwable throwable) {
                exception.set(throwable);
                countDownLatch.countDown();
            }
        });

        try {
            countDownLatch.await();
        } catch (final InterruptedException e) {
            exception.set(e);
        }

        final Throwable exceptionForRetrievingBrokerUsers = exception.getAndSet(null);
        if (exceptionForRetrievingBrokerUsers != null) {
            throw new IOException(exceptionForRetrievingBrokerUsers.getMessage(), exceptionForRetrievingBrokerUsers);
        }

        return userBundle.getAndSet(null);
    }

    /**
     * get the capabilities of the broker app from MicrosoftAuthService
     *
     * @param context The application {@link Context}.
     * @return A bundle in the broker.
     */
    public Bundle getCapabilities(final Context context) throws ClientException {
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        final AtomicReference<Bundle> capabilityBundle = new AtomicReference<>(null);
        final AtomicReference<Throwable> exception = new AtomicReference<>(null);

        performAsyncCallOnBound(context, new Callback<MicrosoftAuthServiceConnection>() {
            @Override
            public void onSuccess(MicrosoftAuthServiceConnection connection) {
                final IMicrosoftAuthService authService = connection.getMicrosoftAuthServiceProvider();
                try {
                    capabilityBundle.set(authService.getCapabilities());
                } catch (final RemoteException ex) {
                    exception.set(ex);
                }

                countDownLatch.countDown();
            }

            @Override
            public void onError(Throwable throwable) {
                exception.set(throwable);
                countDownLatch.countDown();
            }
        });

        try {
            countDownLatch.await();
        } catch (final InterruptedException e) {
            exception.set(e);
        }

        final Throwable exceptionGetCapabilities = exception.getAndSet(null);
        if (exceptionGetCapabilities != null) {
            throw new ClientException(ErrorStrings.FAILED_TO_GET_CAPABILITIES, exceptionGetCapabilities.getMessage(), exceptionGetCapabilities);
        }

        return capabilityBundle.getAndSet(null);
    }

    /**
     * Get the intent for launching the interactive request with broker.
     *
     * @param context The application {@link Context}.
     * @return The {@link Intent} to launch the interactive request.
     */
    public Intent getIntentForInteractiveRequest(final Context context) throws ClientException {
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        final AtomicReference<Intent> bundleResult = new AtomicReference<>(null);
        final AtomicReference<Throwable> exception = new AtomicReference<>(null);

        performAsyncCallOnBound(context, new Callback<MicrosoftAuthServiceConnection>() {
            @Override
            public void onSuccess(MicrosoftAuthServiceConnection result) {
                final IMicrosoftAuthService authService = result.getMicrosoftAuthServiceProvider();
                try {
                    bundleResult.set(authService.getIntentForInteractiveRequest());
                } catch (final RemoteException remoteException) {
                    exception.set(remoteException);
                }

                countDownLatch.countDown();
            }

            @Override
            public void onError(Throwable throwable) {
                exception.set(throwable);
                countDownLatch.countDown();
            }
        });

        try {
            countDownLatch.await();
        } catch (final InterruptedException e) {
            exception.set(e);
        }

        final Throwable throwable = exception.getAndSet(null);
        //ClientException with error code BROKER_APP_NOT_RESPONDING will be thrown if there is any exception thrown during binding the service.
        if (throwable != null) {
            if (throwable instanceof RemoteException) {
                Logger.error(TAG, null, "Get error when trying to get token from broker. " + throwable.getMessage(), throwable);
                throw new ClientException(ErrorStrings.BROKER_APP_NOT_RESPONDING,
                        throwable.getMessage(),
                        throwable);
            } else if (throwable instanceof InterruptedException) {
                Logger.error(TAG, null, "The MicrosoftAuthService binding call is interrupted. " + throwable.getMessage(), throwable);
                throw new ClientException(ErrorStrings.BROKER_APP_NOT_RESPONDING,
                        throwable.getMessage(),
                        throwable);
            } else {
                Logger.error(TAG, "Didn't receive the activity to launch from broker. " + throwable.getMessage(), throwable);
                throw new ClientException(ErrorStrings.BROKER_APP_NOT_RESPONDING,
                        "Didn't receive the activity to launch from broker: " + throwable.getMessage(),
                        throwable);
            }
        }

        return bundleResult.getAndSet(null);
    }


    /**
     * Removing all the accounts from broker.
     *
     * @param context The application {@link Context}.
     */
    public void removeAccounts(final Context context) {
        final String methodName = ":removeAccounts";
        performAsyncCallOnBound(context, new Callback<MicrosoftAuthServiceConnection>() {
            @Override
            public void onSuccess(MicrosoftAuthServiceConnection result) {
                try {
                    result.getMicrosoftAuthServiceProvider().removeAccounts();
                } catch (final RemoteException remoteException) {
                    Logger.error(TAG + methodName, null, remoteException.getMessage(), remoteException);
                }
            }

            @Override
            public void onError(Throwable throwable) {
                Logger.error(TAG + methodName, null, throwable.getMessage(), throwable);
            }
        });
    }


    private void performAsyncCallOnBound(final Context context, final Callback<MicrosoftAuthServiceConnection> callback) {
        bindToAuthService(context, new Callback<MicrosoftAuthServiceConnection>() {
            @Override
            public void onSuccess(final MicrosoftAuthServiceConnection result) {
                if (Looper.myLooper() != Looper.getMainLooper()) {
                    callback.onSuccess(result);
                    result.unBindService(context);
                } else {
                    sThreadExecutor.execute(new Runnable() {
                        @Override
                        public void run() {
                            callback.onSuccess(result);
                            result.unBindService(context);
                        }
                    });
                }
            }

            @Override
            public void onError(Throwable throwable) {
                callback.onError(throwable);
            }
        });
    }

    private void bindToAuthService(final Context context, final Callback<MicrosoftAuthServiceConnection> callback) {
        final String methodName = ":bindToAuthService";
        Logger.verbose(TAG + methodName, "Binding to MicrosoftAuthService for caller uid. ", "uid: " + android.os.Process.myUid());
        final Intent authServiceToBind = getIntentForAuthService(context);

        final MicrosoftAuthServiceConnection connection = new MicrosoftAuthServiceConnection();
        final CallbackExecutor<MicrosoftAuthServiceConnection> callbackExecutor = new CallbackExecutor<>(callback);
        mPendingConnections.put(connection, callbackExecutor);
        final boolean serviceBound = context.bindService(authServiceToBind, connection, Context.BIND_AUTO_CREATE);
        Logger.verbose(TAG + methodName, "The status for MicrosoftAuthService bindService call is: " + Boolean.valueOf(serviceBound));

        if (!serviceBound) {
            connection.unBindService(context);
            callback.onError(new ClientException(ErrorStrings.BROKER_BIND_SERVICE_FAILED));
        }
    }

    private Map<String, String> prepareGetAuthTokenRequestData(final Context context, final Bundle requestBundle) {
        final Set<String> requestBundleKeys = requestBundle.keySet();

        final Map<String, String> requestData = new HashMap<>();
        for (final String key : requestBundleKeys) {
            if (key.equals(AuthenticationConstants.Browser.REQUEST_ID)
                    || key.equals(AuthenticationConstants.Broker.EXPIRATION_BUFFER)) {
                requestData.put(key, String.valueOf(requestBundle.getInt(key)));
                continue;
            }
            requestData.put(key, requestBundle.getString(key));
        }
        requestData.put(AuthenticationConstants.Broker.CALLER_INFO_PACKAGE, context.getPackageName());

        return requestData;
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

    private class MicrosoftAuthServiceConnection implements android.content.ServiceConnection {
        private IMicrosoftAuthService mMicrosoftAuthService;
        private boolean mBound;

        public IMicrosoftAuthService getMicrosoftAuthServiceProvider() {
            return mMicrosoftAuthService;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Logger.verbose(TAG, "MicrosoftAuthService is connected.");
            mMicrosoftAuthService = IMicrosoftAuthService.Stub.asInterface(service);
            mBound = true;

            final CallbackExecutor<MicrosoftAuthServiceConnection> callbackExecutor = mPendingConnections.remove(this);
            if (callbackExecutor != null) {
                callbackExecutor.onSuccess(this);
            } else {
                Logger.verbose(TAG, "No callback is found.");
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Logger.verbose(TAG, "MicrosoftAuthService is disconnected.");
            mBound = false;
        }

        public void unBindService(final Context context) {
            // Service disconnect is async operation, in case of race condition, having the service binding check queued up
            // in main message looper and unbind it.
            final Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (mBound) {
                        try {
                            context.unbindService(MicrosoftAuthServiceConnection.this);
                        } catch (final IllegalArgumentException exception) {
                            Logger.error(TAG, null, "Unbind threw IllegalArgumentException", exception);
                        } finally {
                            mBound = false;
                        }
                    }
                }
            });
        }
    }

}
