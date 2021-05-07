package com.microsoft.identity.common.internal.providers.oauth2;

import java.util.UUID;

public class DefaultStateGenerator extends StateGenerator {
    @Override
    public String generate() {
        final UUID stateUUID1 = UUID.randomUUID();
        final UUID stateUUID2 = UUID.randomUUID();
        final String state = stateUUID1.toString() + "-" + stateUUID2.toString();

        return state;
    }

}
