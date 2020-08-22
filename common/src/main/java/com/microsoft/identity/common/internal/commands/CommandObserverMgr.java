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
package com.microsoft.identity.common.internal.commands;

import android.os.Handler;
import android.util.Pair;

import androidx.annotation.NonNull;

import com.microsoft.identity.common.internal.controllers.CommandResult;
import com.microsoft.identity.common.internal.logging.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.microsoft.identity.common.internal.controllers.CommandDispatcher.returnCommandResult;

/**
 * Singleton object used to track observers of a request. If multiple silent requests are received
 * in parallel, keep track of the callbacks and then invoke them once the operation is finished.
 */
public class CommandObserverMgr {

    /**
     * Tag for logging.
     */
    private static final String LOG_TAG = CommandObserverMgr.class.getSimpleName();

    /**
     * Our Singleton.
     */
    private static CommandObserverMgr INSTANCE;

    /**
     * A list of Observers to notify when this job finishes.
     */
    private final Map<BaseCommand<?>, List<Pair<CommandCallback<?, ?>, String>>> commandObservers = new HashMap<>();

    /**
     * Gets an instance of this Singleton.
     *
     * @return A reference to the CommandObserverMgr
     */
    public static synchronized CommandObserverMgr getInstance() {
        if (null == INSTANCE) {
            INSTANCE = new CommandObserverMgr();
        }

        return INSTANCE;
    }

    /**
     * Adds an observer to a request.
     *
     * @param command The Command we wish to observe.
     */
    public synchronized void addObserver(@NonNull final BaseCommand<?> command) {
        if (null == commandObservers.get(command)) {
            commandObservers.put(command, new ArrayList<Pair<CommandCallback<?, ?>, String>>());
        }

        final Pair<CommandCallback<?, ?>, String> cmdPair =
                new Pair<CommandCallback<?, ?>, String>(
                        command.getCallback(),
                        command.getParameters().getCorrelationId()
                );

        commandObservers.get(command).add(cmdPair);
    }

    /**
     * Invoked when a Command finishes to trigger callbacks.
     *
     * @param command The Command which has completed.
     * @param result  The result of the completed Command.
     * @param handler The handler to which we'll delegate posting the result.
     */
    public synchronized void onCommandCompleted(@NonNull final BaseCommand<?> command,
                                                @NonNull final CommandResult result,
                                                @NonNull final Handler handler) {
        // Get the list of observers for this Command
        final List<Pair<CommandCallback<?, ?>, String>> observers = commandObservers.get(command);

        if (null == observers || observers.isEmpty()) { // Just 1 observer, simply send the result to them
            returnCommandResult(command, result, handler);
        } else { // Multiple observers to notify
            for (final Pair<CommandCallback<?, ?>, String> subPair : observers) {
                // Get the callback to notify
                final CommandCallback callback = subPair.first;

                // Set it on the Command so they receive the result
                command.setCallback(callback);

                // This request began with the commandParam correlation_id
                final String originalCorrelationId = command.getParameters().getCorrelationId();

                // The correlation_id this callback expects
                final String callbackSpecificCorrelationId = subPair.second;

                if (!originalCorrelationId.equals(callbackSpecificCorrelationId)) {
                    // Correlation Ids don't match -- different threads expecting different ids
                    Logger.warn(
                            LOG_TAG,
                            "Multiple threads waiting on this result."
                                    + "\n"
                                    + "Original correlation ID: " + originalCorrelationId
                                    + "\n"
                                    + "Dropped correlation ID: " + callbackSpecificCorrelationId,
                            null
                    );

                    // If we could deep-copy the result, update the correlation_id and return
                    // doesn't look like we have a good way to do that?
                    //setCorrelationIdOnResult(result, callbackSpecificCorrelationId);
                }

                // Send the result to the callback
                returnCommandResult(command, result, handler);
            }
        }

        // We're done, remove the pair
        commandObservers.remove(command);
    }
}
