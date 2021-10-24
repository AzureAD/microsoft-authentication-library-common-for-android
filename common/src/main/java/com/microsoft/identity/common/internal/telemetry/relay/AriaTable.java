package com.microsoft.identity.common.internal.telemetry.relay;

public enum AriaTable {
    SESSION("android_session"),

    EVENT("android_event");

    private final String mName;

    AriaTable(final String name) {
        mName = name;
    }

    public String getName() {
        return mName;
    }
}
