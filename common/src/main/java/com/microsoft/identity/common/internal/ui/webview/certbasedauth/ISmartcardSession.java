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
package com.microsoft.identity.common.internal.ui.webview.certbasedauth;

import androidx.annotation.NonNull;

import java.security.PrivateKey;
import java.util.List;

/**
 * An abstract session that handles pulling information from a smartcard.
 */
public interface ISmartcardSession {

    /**
     * Gets a List of ICertDetails based on certificates stored on the smartcard.
     * @return a List of ICertDetails. If there are no certificates found, an empty List should be returned.
     * @throws Exception that could vary upon smartcard type.
     */
    @NonNull
    List<ICertDetails> getCertDetailsList() throws Exception;

    /**
     * Verifies PIN attempt provided by user.
     * @param pin Char array with pin.
     * @return true if PIN is verified; false otherwise.
     * @throws Exception that could vary upon smartcard type.
     */
    boolean verifyPin(@NonNull final char[] pin) throws Exception;

    /**
     * Gets number of PIN attempts remaining for smartcard.
     * @return number of PIN attempts remaining.
     * @throws Exception that could vary upon smartcard type.
     */
    int getPinAttemptsRemaining() throws Exception;

    /**
     * Instantiate a PrivateKey from the chosen certificate that can be passed to a ClientCertRequest proceed method for authentication.
     * @param certDetails ICertDetails of chosen certificate.
     * @param pin Char array containing verified PIN.
     * @return a PrivateKey.
     * @throws Exception that could vary upon smartcard type.
     */
    @NonNull
    PrivateKey getKeyForAuth(@NonNull final ICertDetails certDetails,
                             @NonNull final char[] pin) throws Exception;
}
