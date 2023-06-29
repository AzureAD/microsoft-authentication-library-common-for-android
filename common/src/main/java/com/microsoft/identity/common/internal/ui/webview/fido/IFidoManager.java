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
package com.microsoft.identity.common.internal.ui.webview.fido;

import java.util.Map;

import lombok.NonNull;

/**
 * Representation of a manager that handles interactions with a passkey provider (usually through an API).
 */
public interface IFidoManager {
    /**
     * Interacts with the FIDO credential provider and puts the authentication result in a header format.
     * @param challenge AuthFidoChallenge received from the server.
     * @return header fields for response.
     */
    Map<String, String> getAuthResponseHeader(@NonNull AuthFidoChallenge challenge);

    /**
     *Interacts with the FIDO credential provider and puts the registration result in a header format.
     * @param challenge RegFidoChallenge received from the server.
     * @return header fields for response.
     */
    Map<String, String> getRegResponseHeader(@NonNull RegFidoChallenge challenge);
}
