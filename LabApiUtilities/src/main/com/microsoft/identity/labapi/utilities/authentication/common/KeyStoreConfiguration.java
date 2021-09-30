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
package com.microsoft.identity.labapi.utilities.authentication.common;

import java.util.Arrays;

import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.Accessors;

/**
 * Class holds information necessary to instantiate a keystore in order to retrieve and access
 * a ClientCertificateConfiguration and the private key associated with that ClientCertificateConfiguration.
 * <p>
 * Lomboked from original source located at:
 * https://github.com/AzureAD/microsoft-authentication-library-common-for-android/blob/dev/common/src/main/java/com/microsoft/identity/common/internal/providers/keys/KeyStoreConfiguration.java
 */
@Getter
@Accessors(prefix = "m")
public class KeyStoreConfiguration {
    private final String mKeyStoreType;
    private final String mKeyStoreProvider;
    private final char[] mKeyStorePassword;

    public char[] getKeyStorePassword() {
        return mKeyStorePassword == null ? null : Arrays.copyOf(mKeyStorePassword, mKeyStorePassword.length);
    }

    public KeyStoreConfiguration(@NonNull final String keyStoreType,
                                 @NonNull final String keyStoreProvider,
                                 final char[] keyStorePassword) {
        this.mKeyStoreType = keyStoreType;
        this.mKeyStoreProvider = keyStoreProvider;
        this.mKeyStorePassword = keyStorePassword == null ? null : Arrays.copyOf(keyStorePassword, keyStorePassword.length);
    }
}
