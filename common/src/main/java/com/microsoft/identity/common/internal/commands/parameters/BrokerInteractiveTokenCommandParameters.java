package com.microsoft.identity.common.internal.commands.parameters;

import com.microsoft.identity.common.internal.request.BrokerAcquireTokenOperationParameters;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder(toBuilder = true)
public class BrokerInteractiveTokenCommandParameters extends InteractiveTokenCommandParameters {

    private String callerPackageName;
    private int callerUid;
    private String callerAppVersion;
    private String brokerVersion;

    private boolean shouldResolveInterrupt;
    private BrokerAcquireTokenOperationParameters.RequestType requestType;

    /**
     * Helper method to identify if the request originated from Broker itself or from client libraries.
     *
     * @return : true if request is the request is originated from Broker, false otherwise
     */
    public boolean isRequestFromBroker() {
        return requestType == BrokerAcquireTokenOperationParameters.RequestType.BROKER_RT_REQUEST ||
                requestType == BrokerAcquireTokenOperationParameters.RequestType.RESOLVE_INTERRUPT;
    }
}
