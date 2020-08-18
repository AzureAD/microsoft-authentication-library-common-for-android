package com.microsoft.identity.common.internal.commands;

import android.os.Handler;

import com.microsoft.identity.common.internal.controllers.CommandResult;

public interface ICommandSubscriptionMgr {

    void addSubscriber(BaseCommand<?> command);

    void onCommandCompleted(BaseCommand<?> command, CommandResult result, Handler handler);
}
