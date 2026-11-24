// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.identity.common.java.flighting

/**
 * Records governance information on a flight declaration without changing its runtime behavior.
 *
 * Completeness and premortem requirements are enforced by development tooling, not this annotation.
 *
 * @property owner Explicit accountable current person or team. The empty default preserves source
 * compatibility but does not satisfy governance checks for new or changed declarations.
 * @property type Intended lifecycle of the flight.
 * @property premortem Repository-relative governance document path, retaining the original field
 * name. Required for new or materially changed flights of every type, regardless of default value;
 * approved initial legacy backfill may retain a recorded historical document gap.
 */
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.BINARY)
annotation class FlightMeta(
    val owner: String = "",
    val type: FlagType,
    val premortem: String = ""
)
