package com.microsoft.identity.common.internal.platform;

import lombok.NonNull;

/**
 * Marker interface for Symmetric algortihm classes.
 */
public interface SymmetricAlgorithm extends Algorithm {
    String name();
    static SymmetricAlgorithm of(@NonNull final String name) {
        return new SymmetricAlgorithm() {
            @Override
            public String name() {
                return name;
            }
        };
    }

}
