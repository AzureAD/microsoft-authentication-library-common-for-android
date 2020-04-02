package com.microsoft.identity.common.internal.commands;

import android.content.Intent;

import androidx.annotation.NonNull;

import com.microsoft.identity.common.internal.commands.parameters.TokenCommandParameters;
import com.microsoft.identity.common.internal.controllers.BaseController;
import com.microsoft.identity.common.internal.result.AcquireTokenResult;

import java.util.List;

public abstract class TokenCommand extends BaseCommand<AcquireTokenResult> {

    public TokenCommand(@NonNull TokenCommandParameters parameters, @NonNull BaseController controller, @NonNull CommandCallback callback, @NonNull String publicApiId) {
        super(parameters, controller, callback, publicApiId);
    }

    public TokenCommand(@NonNull TokenCommandParameters parameters, @NonNull List<BaseController> controllers, @NonNull CommandCallback callback, @NonNull String publicApiId) {
        super(parameters, controllers, callback, publicApiId);
    }

    abstract void notify(int requestCode, int resultCode, final Intent data);
}
