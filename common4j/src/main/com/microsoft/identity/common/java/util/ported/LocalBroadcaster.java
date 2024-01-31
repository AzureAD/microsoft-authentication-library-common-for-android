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
package com.microsoft.identity.common.java.util.ported;

import com.microsoft.identity.common.java.logging.Logger;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import lombok.NonNull;

public enum LocalBroadcaster {
    INSTANCE;

    private static final String TAG = LocalBroadcaster.class.getSimpleName();
    private static ExecutorService sBroadcastExecutor = Executors.newSingleThreadExecutor();

    public interface IReceiverCallback {
        void onReceive(@NonNull final PropertyBag propertyBag);
    }

    final ConcurrentHashMap<String, IReceiverCallback> mReceivers = new ConcurrentHashMap<>();
    final ConcurrentHashMap<String, Exception> mExceptions = new ConcurrentHashMap<>();

    public void registerCallback(@NonNull final String alias, @NonNull final IReceiverCallback callback){
        final String methodName = ":registerCallback";

        if (mReceivers.containsKey(alias)){
            Logger.warn(TAG + methodName, "The alias: " + alias + " has already been registered. " +
                    "It will be overwritten");
        }

        Logger.info(TAG + methodName, "Registering alias: " + alias);
        mReceivers.put(alias, callback);
    }

    /**
     * Register an exception for an exisiting receiver alias.
     * This should only be called inside the {@link IReceiverCallback#onReceive} method.
     * @param alias alias of the receiver to log the exception
     * @param e the exception being logged
     */
    public void registerExceptionDuringCallback(@NonNull final String alias, @NonNull final Exception e){
        final String methodName = ":registerExceptionDuringCallback";

        if (!mReceivers.containsKey(alias)){
            // Reaching this should be impossible, log
            Logger.warn(TAG + methodName, "No alias found in the local broadcaster when trying to register exception for: " + alias);
        } else {
            Logger.error(TAG + methodName, "Registering Exception for Alias: " + alias, e);
            mExceptions.put(alias, e);
        }
    }

    /**
     * Get the exception for receiver alias provided.
     * @param alias alias of the receiver to fetch exception for
     * @return the exception registered for the receiver, returns null if alias is not registered, or if no exception exists
     */
    public Exception getExceptionForAlias(@NonNull final String alias){
        final String methodName = ":getExceptionForAlias";

        if (mReceivers.containsKey(alias) && mExceptions.containsKey(alias)){
            Logger.info(TAG + methodName, "Returning exception for alias: " + alias);
            return mExceptions.get(alias);
        }

        // If there is no receiver for the given alias, or if there is no exception for that receiver,
        // return null.
        return null;
    }

    public void unregisterCallback(@NonNull final String alias){
        final String methodName = ":unregisterCallback";

        Logger.info(TAG + methodName, "Removing alias: " + alias);
        mReceivers.remove(alias);
        mExceptions.remove(alias);
    }

    public boolean hasReceivers(@NonNull final String alias) {
        return mReceivers.containsKey(alias);
    }

    public void broadcast(@NonNull final String alias, @NonNull final PropertyBag propertyBag) {
        final String methodName = ":broadcast";
        sBroadcastExecutor.execute(new Runnable() {
            public void run() {
                final IReceiverCallback receiver = mReceivers.get(alias);
                if (receiver != null) {
                    Logger.info(TAG + methodName, "broadcasting to alias: " + alias);
                    receiver.onReceive(propertyBag);
                } else {
                    Logger.info(TAG + methodName, "No callback is registered with alias: " + alias +
                            ". Do nothing.");
                }
            }
        });
    }

    /**
     * Clears the receivers associated with this instance.
     */
    public void clearReceivers() {
        mReceivers.clear();
        mExceptions.clear();
    }

    /**
     * Resets the broadcast executor service.
     */
    public static void resetBroadcast() {
        shutdownAndAwaitTerminationForBroadcasterService();
        sBroadcastExecutor = Executors.newSingleThreadExecutor();
    }

    private static void shutdownAndAwaitTerminationForBroadcasterService() {
        final String methodName = ":shutdownAndAwaitTerminationForBroadcasterService";
        sBroadcastExecutor.shutdown();
        try {
            if (!sBroadcastExecutor.awaitTermination(20, TimeUnit.SECONDS)) {
                sBroadcastExecutor.shutdownNow();
                if (!sBroadcastExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    Logger.info(TAG + methodName, "broadcastExecutor did not terminate");
                }
            }
        } catch (InterruptedException ex) {
            sBroadcastExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
