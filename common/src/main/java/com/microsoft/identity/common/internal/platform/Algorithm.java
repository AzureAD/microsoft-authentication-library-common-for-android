package com.microsoft.identity.common.internal.platform;

/**
 * Marker interface for crypto algorithm names.
 */
public interface Algorithm {
    /**
     * @return the standard algorithm name;
     */
    public String name();
}
