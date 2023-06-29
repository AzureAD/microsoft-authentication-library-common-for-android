package com.microsoft.identity.common.internal.ui.webview.fido;

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
import java.util.List;

import lombok.Builder;

/**
 * An object representing a RegFidoChallenge.
 */
@Builder
public class RegFidoChallenge extends AbstractFidoChallenge {

    /**
     * The authenticator uses this ID to associate a credential with the user.
     */
    private final String mUserId;
    /**
     * Usually the upn of the user.
     */
    private final String mUserName;
    /**
     * A user-visible credential name.
     */
    private final String mCredentialName;
    /**
     * Kind of attestation that the server wants to witness.
     */
    private final String mAttestationKind;
    /**
     * Array of strings indicating the public key types that are acceptable.
     */
    private final List<String> mPubKeyCredParams;
    /**
     * Array of credentials that should be... excluded.
     */
    private final List<String> mExcludedCredentials;


}
