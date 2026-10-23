// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.microsoft.identity.common.java.nativeauth.commands.parameters

import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.NativeAuthV2ContinuationState

/**
 * Activation input composed with existing token parameters. OTP validation belongs to the service.
 *
 * @property tokenParameters Existing dispatcher, strategy, and cache configuration.
 * @property continuationState Latest opaque activation state.
 * @property code OTP passed verbatim to the service without client-side format validation.
 */
class NativeAuthV2ActivateCommandParameters(
    val tokenParameters: BaseSignInTokenCommandParameters,
    val continuationState: NativeAuthV2ContinuationState,
    val code: String
) {
    override fun toString(): String = "NativeAuthV2ActivateCommandParameters(<redacted>)"
}
