package com.microsoft.identity.common.java.commands;

import com.microsoft.identity.common.java.WarningType;
import com.microsoft.identity.common.java.commands.parameters.DeviceCodeFlowCommandParameters;
import com.microsoft.identity.common.java.controllers.BaseController;
import com.microsoft.identity.common.java.controllers.CommandDispatcher;
import com.microsoft.identity.common.java.logging.Logger;
import com.microsoft.identity.common.java.providers.microsoft.microsoftsts.MicrosoftStsAuthorizationResponse;
import com.microsoft.identity.common.java.providers.microsoft.microsoftsts.MicrosoftStsAuthorizationResult;
import com.microsoft.identity.common.java.providers.oauth2.AuthorizationResult;
import com.microsoft.identity.common.java.result.AcquireTokenResult;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import lombok.NonNull;

public class DeviceCodeFlowUserCodeCommand extends BaseCommand<AuthorizationResult> {
    public DeviceCodeFlowUserCodeCommand(@NonNull DeviceCodeFlowCommandParameters parameters,
                                 @NonNull BaseController controller,
                                 @SuppressWarnings(WarningType.rawtype_warning) @NonNull CommandCallback callback,
                                 @NonNull String publicApiId) {
        super(parameters, controller, callback, publicApiId);
    }

    @Override
    public MicrosoftStsAuthorizationResult execute() throws Exception {
        // Get the controller used to execute the command
        final BaseController controller = getDefaultController();

        // Fetch the parameters
        final DeviceCodeFlowCommandParameters commandParameters = (DeviceCodeFlowCommandParameters) getParameters();

        // Call deviceCodeFlowAuthRequest to get authorization result (Part 1 of DCF)
        @SuppressWarnings(WarningType.rawtype_warning) final AuthorizationResult authorizationResult = controller.deviceCodeFlowAuthRequest(commandParameters);

        // Fetch the authorization response
        final MicrosoftStsAuthorizationResponse authorizationResponse =
                (MicrosoftStsAuthorizationResponse) authorizationResult.getAuthorizationResponse();

        final Date expiredDate = new Date();
        try {
            long expiredInInMilliseconds = TimeUnit.SECONDS.toMillis(Long.parseLong(authorizationResponse.getExpiresIn()));
            expiredDate.setTime(expiredDate.getTime() + expiredInInMilliseconds);
        } catch (final NumberFormatException e) {
            // Shouldn't happen, but if it does, we don't want to fail the request because of this.
           // Logger.error(methodTag, "Failed to parse authorizationResponse.getExpiresIn()", e);
        }

        // Communicate with user app and provide authentication information
        return (MicrosoftStsAuthorizationResult)authorizationResult;

    }

    @Override
    public boolean isEligibleForEstsTelemetry() {
        return false;
    }
}
