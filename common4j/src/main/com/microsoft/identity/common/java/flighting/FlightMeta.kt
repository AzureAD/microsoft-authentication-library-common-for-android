// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.identity.common.java.flighting

/**
 * Records governance information on a flight declaration without changing its runtime behavior.
 *
 * Completeness and premortem requirements are enforced by development tooling, not this annotation.
 *
 * @property owner Accountable person or team. Empty permits a tooling-resolved ownership candidate;
 * git authorship alone does not confirm current ownership.
 * @property type Intended lifecycle of the flight.
 * @property premortem Repository-relative premortem path. Required by policy for new or materially
 * changed release flights, regardless of their default value.
 */
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.BINARY)
annotation class FlightMeta(
    val owner: String = "",
    val type: FlagType,
    val premortem: String = ""
)
