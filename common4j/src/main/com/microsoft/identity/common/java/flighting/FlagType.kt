// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.identity.common.java.flighting

/** Describes a flight's intended lifecycle, not its current rollout state. */
enum class FlagType {
    /** Temporary release control intended for eventual retirement. */
    RELEASE,

    /** Safety control retained while its rollback capability is justified. */
    KILL_SWITCH,

    /** Configuration parameter retained while its tuning capability is needed. */
    CONFIG
}
