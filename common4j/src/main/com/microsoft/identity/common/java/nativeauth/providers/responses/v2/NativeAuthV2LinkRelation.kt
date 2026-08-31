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
 * A HAL `_links` relation name used by V2 Native Auth responses.
 *
 * Deliberately not an enum: the server can send a link relation this SDK version does not yet
 * know about, and an enum would force that value to `null`, erasing the raw string needed for
 * diagnosis. [value] always preserves exactly what the server sent. An unrecognised relation is
 * simply not followed by the SDK; it does not need to fail parsing.
 *
 * Not [java.io.Serializable]: as a non-null, non-generic property it is always flattened to its
 * underlying `String`, so it is never boxed into the stream when [NativeAuthV2ContinuationState]
 * is serialized.
 */
@JvmInline
value class NativeAuthV2LinkRelation(val value: String) {
    companion object {
        val CHALLENGE = NativeAuthV2LinkRelation("challenge")
        val VERIFY = NativeAuthV2LinkRelation("verify")
        val RESEND = NativeAuthV2LinkRelation("resend")
        val UPDATE = NativeAuthV2LinkRelation("update")
        val POLL = NativeAuthV2LinkRelation("poll")
        val CONTINUE = NativeAuthV2LinkRelation("continue")
        val RESET_PASSWORD = NativeAuthV2LinkRelation("resetPassword")
        val SIGN_IN = NativeAuthV2LinkRelation("signIn")
        val SIGN_UP = NativeAuthV2LinkRelation("signUp")
    }
}
