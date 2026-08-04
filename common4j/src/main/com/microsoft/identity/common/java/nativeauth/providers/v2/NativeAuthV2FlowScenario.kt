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
package com.microsoft.identity.common.java.nativeauth.providers.v2

/**
 * The Native Auth V2 flow that produced a [com.microsoft.identity.common.java.nativeauth.providers.responses.v2.NativeAuthV2ContinuationState].
 *
 * This is SDK-internal parser/request context (for example, to help select the right entry link
 * relation on the next request); it is not exposed as part of any public result or telemetry
 * surface. Only [RESET_PASSWORD] is wired up in this round; additional values are added, one flow
 * at a time, as sign-in and sign-up V2 support lands. `Enum` already implements `Serializable`, so
 * this type participates in [com.microsoft.identity.common.java.nativeauth.providers.responses.v2.NativeAuthV2ContinuationState]'s
 * serialization without any extra declaration here.
 */
internal enum class NativeAuthV2FlowScenario {
    RESET_PASSWORD
}
