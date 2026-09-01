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
package com.microsoft.identity.common.java.nativeauth.providers.responses.v2

/**
 * SDK-issued Native Auth V2 operations. Unlike [NativeAuthV2HalAction] and
 * [NativeAuthV2LinkRelation], this set is entirely controlled by the SDK, not the server, so it is
 * closed and modeled as an enum.
 *
 * This is parser context used for operation-specific error mapping (for example, distinguishing an
 * invalid code entered during [VERIFY] from an invalid password submitted during
 * [UPDATE_PASSWORD]); it is not a telemetry enum and must not be used as one.
 */
internal enum class NativeAuthV2Operation {
    RESET_PASSWORD_START,
    CHALLENGE,
    RESEND,
    VERIFY,
    UPDATE_PASSWORD,
    POLL,

    /** Sign-in entry: `signIn` href posted with the username. */
    SIGN_IN_START,

    /** Challenge issued against the server-offered password first-factor method. */
    SIGN_IN_PASSWORD_CHALLENGE,

    /**
     * Password verification for a password supplied directly to the sign-in entry point. Kept
     * distinct from [SUBMIT_PASSWORD] so an invalid-credentials rejection can be attributed to the
     * entry point rather than to a deferred submission.
     */
    SIGN_IN_PASSWORD_VERIFY,

    /**
     * Password verification for a password submitted from the deferred password-required state.
     * Kept distinct from [SIGN_IN_PASSWORD_VERIFY]; see that value.
     */
    SUBMIT_PASSWORD,

    /** Challenge issued against an explicitly selected multi-factor method. */
    MFA_METHOD_CHALLENGE,

    /** One-time-code verification for a multi-factor challenge. */
    MFA_VERIFY,

    /** Sign-up entry: `signUp` href posted with just the continuation token. */
    SIGN_UP_START,

    /** Attribute submission during sign-up: `submitAttributes` href posted with a map of values. */
    SUBMIT_ATTRIBUTES;

    /**
     * `true` when this operation submits a password, in either the entry or the deferred form.
     */
    val isPasswordVerification: Boolean
        get() = this == SIGN_IN_PASSWORD_VERIFY || this == SUBMIT_PASSWORD

    /**
     * `true` when this operation belongs to the V2 sign-in flow rather than SSPR.
     */
    val isSignIn: Boolean
        get() = this == SIGN_IN_START ||
                this == SIGN_IN_PASSWORD_CHALLENGE ||
                this == SIGN_IN_PASSWORD_VERIFY ||
                this == SUBMIT_PASSWORD ||
                this == MFA_METHOD_CHALLENGE ||
                this == MFA_VERIFY

    /**
     * `true` when this operation belongs to the V2 sign-up flow.
     */
    val isSignUp: Boolean
        get() = this == SIGN_UP_START || this == SUBMIT_ATTRIBUTES
}
