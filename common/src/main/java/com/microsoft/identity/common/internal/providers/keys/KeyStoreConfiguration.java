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
package com.microsoft.identity.common.internal.providers.keys;

/**
 * Class holds information necessary to instantiate a keystore in order to retrieve and access
 * a ClientCertificateConfiguration and the private key associated with that ClientCertificateConfiguration.
 * NOTE: This class should move to library configuration
 */
public class KeyStoreConfiguration {

    private final String mKeyStoreType;
    private final String mKeyStoreProvider;
    private final char[] mKeyStorePassword;

    /**
     * Constructor of KeyStoreConfiguration.
     *
     * @param keyStoreType     String
     * @param keyStoreProvider String
     * @param keyStorePassword char[]
     */
    public KeyStoreConfiguration(String keyStoreType, String keyStoreProvider, char[] keyStorePassword) {
        mKeyStoreType = keyStoreType;
        mKeyStoreProvider = keyStoreProvider;
        mKeyStorePassword = keyStorePassword;
    }

    /**
     * Get the key store type.
     *
     * @return String
     */
    public String getKeyStoreType() {
        return mKeyStoreType;
    }

    /**
     * Get the key store provider.
     *
     * @return String
     */
    public String getKeyStoreProvider() {
        return mKeyStoreProvider;
    }

    /**
     * Get the key store password.
     *
     * @return String
     */
    public char[] getKeyStorePassword() {
        return mKeyStorePassword;
    }


}
