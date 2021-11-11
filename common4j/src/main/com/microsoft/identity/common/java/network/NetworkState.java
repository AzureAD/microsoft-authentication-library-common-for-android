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

    public static NetworkState newInstance(String networkStateString) {
        String[] values = networkStateString.split("\\s+");
        long delay = 0, duration = 0;
        NetworkInterface networkInterface = null;
        try {
            if (values.length == 3) {
                delay = Integer.parseInt(values[0]);
                networkInterface = NetworkInterface.fromValue(values[1]);
                duration = Integer.parseInt(values[2]);
            } else if (values.length == 2) {
                networkInterface = NetworkInterface.fromValue(values[0]);
                duration = Integer.parseInt(values[1]);
            } else if (values.length == 1) {
                networkInterface = NetworkInterface.fromValue(values[0]);
            }
        } catch (Exception exception) {
            throw new IllegalArgumentException("Error parsing network state from \"" + networkStateString + "\"", exception);
        }

        if (networkInterface == null) {
            throw new IllegalArgumentException("Unable to get network interface from \"" + networkStateString + "\"");
        }

        return new NetworkState(delay, networkInterface, duration);
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
