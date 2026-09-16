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
 * An account attribute the server requested during a Native Auth V2 sign-up flow, safe to hand to
 * layers above common4j.
 *
 * @property name The wire name of the attribute (for example `email` or `displayName`).
 * @property type Optional input type the server declared for the attribute (for example `text` or
 * `password`).
 * @property required Whether the server marked the attribute as required.
 */
data class NativeAuthV2RequiredAttribute(
    val name: String,
    val type: String?,
    val required: Boolean?
) : ILoggable, Serializable {

    override fun toUnsanitizedString(): String =
        "NativeAuthV2RequiredAttribute(name=$name, type=$type, required=$required)"

    override fun toString(): String = toUnsanitizedString()

    companion object {
        private const val serialVersionUID = 1L
    }
}
