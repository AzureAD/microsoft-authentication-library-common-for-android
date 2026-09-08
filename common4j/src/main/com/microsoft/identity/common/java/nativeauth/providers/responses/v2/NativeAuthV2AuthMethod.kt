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

import com.microsoft.identity.common.java.nativeauth.util.ILoggable
import java.io.Serializable

/**
 * A server-offered Native Auth V2 authentication method, safe to hand to layers above common4j.
 *
 * Deliberately carries no href: the method-specific links stay inside the opaque
 * [NativeAuthV2ContinuationState] and are followed only by common4j protocol code once the caller
 * selects a method by [id]. [type] is normalized to lower case by the parser so callers can match
 * it without repeating case handling, while [hint] is the server-supplied, possibly PII-bearing,
 * target label (for example a partially-masked email address) and is therefore excluded from
 * [toString].
 *
 * @property id Opaque server identifier for this method, used to select it.
 * @property type Normalized (lower-case) method type, for example `password` or `email`.
 * @property hint Optional server-supplied target label; may be `null`.
 */
data class NativeAuthV2AuthMethod(
    val id: String,
    val type: String,
    val hint: String?
) : ILoggable, Serializable {

    override fun toUnsanitizedString(): String =
        "NativeAuthV2AuthMethod(id=$id, type=$type, hint=$hint)"

    override fun toString(): String = "NativeAuthV2AuthMethod(id=$id, type=$type)"

    companion object {
        private const val serialVersionUID = 1L
    }
}
