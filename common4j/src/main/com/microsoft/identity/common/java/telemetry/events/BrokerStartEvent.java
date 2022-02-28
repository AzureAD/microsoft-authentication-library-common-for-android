// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// This code is licensed under the MIT License.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files(the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions :
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.
package com.microsoft.identity.common.java.telemetry.events;

import com.microsoft.identity.common.java.commands.parameters.BrokerSilentTokenCommandParameters;
import com.microsoft.identity.common.java.commands.parameters.CommandParameters;

import static com.microsoft.identity.common.java.telemetry.TelemetryEventStrings.Event;
import static com.microsoft.identity.common.java.telemetry.TelemetryEventStrings.EventType;
import static com.microsoft.identity.common.java.telemetry.TelemetryEventStrings.Key;

public class BrokerStartEvent extends ApiStartEvent {
    public BrokerStartEvent() {
        super();
        names(Event.BROKER_START_EVENT);
        types(EventType.BROKER_EVENT);
    }

    public BrokerStartEvent putAction(final String actionName) {
        put(Key.BROKER_ACTION, actionName);
        return this;
    }

    public BrokerStartEvent putStrategy(final String strategyName) {
        put(Key.BROKER_STRATEGY, strategyName);
        return this;
    }

    @Override
    public BrokerStartEvent putProperties(CommandParameters parameters) {
        super.putProperties(parameters);

        if (parameters instanceof BrokerSilentTokenCommandParameters) {
            final BrokerSilentTokenCommandParameters tokenCommandParameters = (BrokerSilentTokenCommandParameters) parameters;

            put(Key.BROKER_PROTOCOL_VERSION, tokenCommandParameters.getBrokerVersion());
            put(Key.USER_ID, tokenCommandParameters.getHomeAccountId()); // pii
            put(Key.BROKER_CALLER_UID, String.valueOf(tokenCommandParameters.getCallerUid()));
            put(Key.BROKER_CALLER_APP_VERSION, String.valueOf(tokenCommandParameters.getCallerAppVersion()));
            put(Key.BROKER_CALLER_PACKAGE, String.valueOf(tokenCommandParameters.getCallerPackageName()));

            put(Key.BROKER_LOCAL_ACCOUNT_ID, tokenCommandParameters.getLocalAccountId()); // pii
            put(Key.NEGOTIATED_BROKER_VERSION, tokenCommandParameters.getNegotiatedBrokerProtocolVersion());

        }

        return this;
    }
}
