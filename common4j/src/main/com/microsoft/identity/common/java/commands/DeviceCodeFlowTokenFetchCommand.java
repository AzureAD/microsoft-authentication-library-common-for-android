package com.microsoft.identity.common.java.commands;

import com.microsoft.identity.common.java.WarningType;
import com.microsoft.identity.common.java.commands.parameters.DeviceCodeFlowCommandParameters;
import com.microsoft.identity.common.java.controllers.BaseController;
import com.microsoft.identity.common.java.logging.Logger;
import com.microsoft.identity.common.java.providers.microsoft.microsoftsts.MicrosoftStsAuthorizationResult;
import com.microsoft.identity.common.java.result.AcquireTokenResult;

import lombok.NonNull;

public class DeviceCodeFlowTokenFetchCommand extends  TokenCommand{

    MicrosoftStsAuthorizationResult authorizationResult;
    public DeviceCodeFlowTokenFetchCommand(@NonNull DeviceCodeFlowCommandParameters parameters,
                                         @NonNull MicrosoftStsAuthorizationResult authorizationResult,
                                         @NonNull BaseController controller,
                                         @SuppressWarnings(WarningType.rawtype_warning) @NonNull CommandCallback callback,
                                         @NonNull String publicApiId) {
        super(parameters, controller, callback, publicApiId);
        this.authorizationResult = authorizationResult;
    }
    @Override
    public AcquireTokenResult execute() throws Exception {
        final BaseController controller = getDefaultController();
        // Fetch the parameters
        final DeviceCodeFlowCommandParameters commandParameters = (DeviceCodeFlowCommandParameters) getParameters();
        final AcquireTokenResult tokenResult = controller.acquireDeviceCodeFlowToken(authorizationResult, commandParameters);

        return tokenResult;
    }

    @Override
    public boolean isEligibleForEstsTelemetry() {
        return false;
    }
}
