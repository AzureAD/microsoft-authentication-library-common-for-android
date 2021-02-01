package com.microsoft.identity.common.internal.commands;

import com.microsoft.identity.common.internal.commands.parameters.CalculationCommandParameters;
import com.microsoft.identity.common.internal.controllers.BaseController;
import com.microsoft.identity.common.internal.controllers.BrokerMsalController;
import com.microsoft.identity.common.internal.controllers.LocalMSALController;
import com.microsoft.identity.common.internal.logging.Logger;

import java.util.Collections;
import java.util.List;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;

@Getter
public class CalculationCommand extends BaseCommand<String> {
    private static final String TAG = CalculationCommand.class.getSimpleName();

    private final CalculationCommandParameters parameters;

    @EqualsAndHashCode.Exclude
    private final CommandCallback callback;

    private final List<BaseController> controllers;

    public CalculationCommand(@NonNull final CalculationCommandParameters parameters, @NonNull final BaseController controller, @NonNull final CommandCallback callback, @NonNull final String publicApiId) {
        super(parameters, controller, callback, publicApiId);
        this.parameters = parameters;
        this.callback = callback;
        this.controllers = Collections.unmodifiableList(Collections.singletonList(controller));
    }

    @Override
    public String execute() throws Exception {
        final String methodName = ":execute";

        Logger.info(TAG + methodName, "Executing calculation command...");

        return getDefaultController().calculate(parameters.getFirst(), parameters.getSecond(), parameters.getOperator());
    }

    @Override
    public boolean isEligibleForEstsTelemetry() {
        return false;
    }

    @Override
    public BaseController getDefaultController() {
        return controllers.get(0);
    }

    @Override
    public boolean isEligibleForCaching() {
        return false;
    }
}
