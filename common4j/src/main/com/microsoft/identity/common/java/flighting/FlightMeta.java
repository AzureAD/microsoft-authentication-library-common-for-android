// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// This code is licensed under the MIT License.

package com.microsoft.identity.common.java.flighting;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares governance metadata for an enum flight constant.
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.FIELD)
public @interface FlightMeta {
    /**
     * DRI or team alias.
     */
    String owner();

    /**
     * Lifecycle intent of the flight.
     */
    FlagType type();

    /**
     * Repository-relative path to the flight pre-mortem document.
     */
    String premortem();
}