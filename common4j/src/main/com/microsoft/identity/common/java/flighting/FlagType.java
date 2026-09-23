// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// This code is licensed under the MIT License.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files(the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions :
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.
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
     * release cleanup, but still require an owner and pre-mortem when they are not marked legacy.
     */
    CONFIG
}