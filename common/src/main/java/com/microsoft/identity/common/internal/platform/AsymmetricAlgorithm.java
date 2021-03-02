package com.microsoft.identity.common.internal.platform;

import lombok.NonNull;

/**
 * Marker interface for asymmetric algorithms.
 */
public interface AsymmetricAlgorithm extends Algorithm {
    /**
     * return the name of the algorithm in question.
     */
    String name();

    static AsymmetricAlgorithm of(@NonNull final String name) {
        return new AsymmetricAlgorithm() {
            @Override
            public String name() {
                return name;
            }
        };
    }
}
