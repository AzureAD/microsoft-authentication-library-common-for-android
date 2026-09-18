// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// This code is licensed under the MIT License.

package com.microsoft.identity.common.java.flighting;

/**
 * Describes what a flight is for and determines the governance rules for its lifecycle.
 *
 * <p>The type cannot be inferred reliably from the code default or the current ECS state. It
 * provides the expected age tolerance and indicates whether the flight should be removed after
 * the feature reaches general availability:</p>
 *
 */
public enum FlagType {
    /**
     * Temporary rollout flight. Once the feature is generally available, the default should be
     * updated, the flight should be retired, and its code branch should be removed.
     */
    RELEASE,

    /**
     * Safety-revert flight that allows a production behavior to be disabled quickly. It may live
     * longer than a release flight, but it should still be removed when the protected behavior is
     * proven stable and no longer needs an emergency control.
     */
    KILL_SWITCH,

    /**
     * Persistent tuning or behavior configuration, such as a timeout, protocol version, retry
     * count, or telemetry sampling setting. Configuration flights are exempt from age-based
     * release cleanup, but still require an owner and pre-mortem.
     */
    CONFIG
}