package com.microsoft.identity.common.internal.commands;

import android.os.Handler;
import android.util.Pair;

import androidx.annotation.NonNull;

import com.microsoft.identity.common.internal.controllers.CommandResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.microsoft.identity.common.internal.controllers.CommandDispatcher.setCorrelationIdOnResult;

public class CommandSubscriptionMgr implements ICommandSubscriptionMgr {

    private static CommandSubscriptionMgr INSTANCE;

    private final Map<BaseCommand<?>, List<CommandCallback<?, ?>>> commandSubscribers = new HashMap<>();
    private final Map<BaseCommand<?>, List<Pair<CommandCallback<?,?>, String>>> commandSubs = new HashMap<>();

    public static synchronized CommandSubscriptionMgr getInstance() {
        if (null == INSTANCE) {
            INSTANCE = new CommandSubscriptionMgr();
        }

        return INSTANCE;
    }

    @Override
    public synchronized void addSubscriber(@NonNull final BaseCommand<?> command) {
        if (null == commandSubscribers.get(command)) {
            commandSubs.put(command, new ArrayList<Pair<CommandCallback<?, ?>, String>>());
        }

        final Pair<CommandCallback<?,?>, String> cmdPair =
                new Pair<CommandCallback<?, ?>, String>(
                        command.getCallback(),
                        command.getParameters().getCorrelationId()
                );
    }

    @Override
    public synchronized void onCommandCompleted(@NonNull final BaseCommand<?> command,
                                                @NonNull final CommandResult result,
                                                @NonNull final Handler handler) {
        final List<Pair<CommandCallback<?,?>, String>> subs = commandSubs.get(command);
        handler.post(new Runnable() {
            @Override
            public void run() {
                for (Pair<CommandCallback<?,?>, String> subPair : subs) {
                    final CommandCallback callback = subPair.first;

                    // Copy the object
                    final CommandResult resultClone = new CommandResult(result);

                    // TODO This is a potential bug? The copy ctor above is shallow
                    // What do we expect, should the correlation id change if we serve a result from
                    // another thread?

                    // Set the correct correlationId on the result - do not reuse across threads.
                    final String threadCorrelationId = subPair.second;
                    setCorrelationIdOnResult(result, threadCorrelationId);

                    switch (result.getStatus()) {
                        case ERROR:
                            callback.onError(result.getResult());
                            break;

                        case COMPLETED:
                            callback.onTaskCompleted(result.getResult());
                            break;

                        case CANCEL:
                            callback.onCancel();
                            break;
                    }
                }
            }
        });

        //
        final List<CommandCallback<?, ?>> subscribers = commandSubscribers.get(command);
        handler.post(
                new Runnable() {
                    @Override
                    public void run() {
                        for (final CommandCallback callback : subscribers) {
                            switch (result.getStatus()) {
                                case ERROR:
                                    callback.onError(result.getResult());
                                    break;
                                case COMPLETED:
                                    callback.onTaskCompleted(result.getResult());
                                    break;
                                case CANCEL:
                                    callback.onCancel();
                                default:
                                    // TODO something?
                            }
                        }
                    }
                }
        );

        // We're done, remove the pair
        commandSubscribers.remove(command);
    }
}
