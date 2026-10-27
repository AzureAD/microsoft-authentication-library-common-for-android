//  Copyright (c) Microsoft Corporation.
//  All rights reserved.
//
//  This code is licensed under the MIT License.
//
//  Permission is hereby granted, free of charge, to any person obtaining a copy
//  of this software and associated documentation files(the "Software"), to deal
//  in the Software without restriction, including without limitation the rights
//  to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
//  copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions :
//
//  The above copyright notice and this permission notice shall be included in
//  all copies or substantial portions of the Software.
//
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//  THE SOFTWARE.
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
