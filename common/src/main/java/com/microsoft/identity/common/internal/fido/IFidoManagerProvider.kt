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
package com.microsoft.identity.common.internal.fido

import android.app.Activity

/**
 * Supplies a host-specific [IFidoManager] for a WebView passkey challenge.
 *
 * Implemented by hosts that can fulfil a ceremony through a mechanism this library has no
 * knowledge of, and registered with [FidoManagerFactory.setProvider] during host initialization.
 * Not a general extension point: registration is refused for any app that is not signed as a
 * broker, so a library embedded in some other app cannot be made to hand over passkey handling.
 */
interface IFidoManagerProvider {

    /**
     * @param activity live foreground Activity hosting the WebView auth flow.
     * @return a manager able to fulfil the challenge, or null to use this library's default.
     */
    fun getFidoManager(activity: Activity): IFidoManager?
}
