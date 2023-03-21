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

    public void registerCallback(@NonNull final String alias, @NonNull final IReceiverCallback callback){
        final String methodName = ":registerCallback";

        if (mReceivers.containsKey(alias)){
            Logger.warn(TAG + methodName, "The alias: " + alias + " has already been registered. " +
                    "It will be overwritten");
        }

        Logger.info(TAG + methodName, "Registering alias: " + alias);
        mReceivers.put(alias, callback);
    }

    public void unregisterCallback(@NonNull final String alias){
        final String methodName = ":unregisterCallback";

        Logger.info(TAG + methodName, "Removing alias: " + alias);
        mReceivers.remove(alias);
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
     * Resets the broadcast executor service.
     */
    public static void resetBroadcast() {
        mReceivers.clear();
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
