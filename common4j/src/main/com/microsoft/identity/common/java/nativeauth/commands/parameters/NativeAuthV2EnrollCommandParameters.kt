// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.microsoft.identity.common.java.nativeauth.commands.parameters

import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.NativeAuthV2ContinuationState

/**
 * Enrollment input, composed with existing token parameters for command and cache plumbing.
 * Neither contact nor continuation state is included in log strings.
 *
 * @property tokenParameters Existing dispatcher, strategy, and cache configuration.
 * @property continuationState Latest opaque registration state.
 * @property methodId Identifier of an offered supported enrollment method.
 * @property contact Explicit email address or phone number, never inferred from the username.
 */
class NativeAuthV2EnrollCommandParameters(
    val tokenParameters: BaseSignInTokenCommandParameters,
    val continuationState: NativeAuthV2ContinuationState,
    val methodId: String,
    val contact: String
) {
    override fun toString(): String = "NativeAuthV2EnrollCommandParameters(<redacted>)"
}
