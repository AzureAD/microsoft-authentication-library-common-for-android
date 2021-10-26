package com.microsoft.identity.common.java.network;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;

@Data
public class NetworkState {
    private long delay;
    private NetworkInterface networkInterface;
    private long duration;

    private long timeApplied;
    private long timeCompleted;

    @Getter(value = AccessLevel.NONE)
    private boolean applied = false;

    public NetworkState(final NetworkInterface networkInterface) {
        this(0, networkInterface, 0);
    }

    public NetworkState(final long delaySeconds, final NetworkInterface networkInterface) {
        this(delaySeconds, networkInterface, 0);
    }

    public NetworkState(final NetworkInterface networkInterface, final long durationSeconds) {
        this(0, networkInterface, durationSeconds);
    }

    public NetworkState(final long delaySeconds, final NetworkInterface networkInterface, final long durationSeconds) {
        this.delay = TimeUnit.SECONDS.toMillis(delaySeconds);
        this.duration = TimeUnit.SECONDS.toMillis(durationSeconds);
        this.networkInterface = networkInterface;
    }

    @Override
    public String toString() {
        return String.format(
                Locale.getDefault(),
                "[interface=%s, delay=%ds, duration=%ds]",
                networkInterface.getKey(), delay, duration
        );
    }

    public boolean wasApplied() {
        return applied;
    }
}
