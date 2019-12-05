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

package com.microsoft.identity.common.internal.encryption;

import static com.microsoft.identity.common.internal.encryption.BaseEncryptionManager.VERSION_ANDROID_KEY_STORE;
import static com.microsoft.identity.common.internal.encryption.BaseEncryptionManager.VERSION_USER_DEFINED;

/**
 * Type of Secret key to be used.
 */
public enum KeyType {
    LEGACY_AUTHENTICATOR_APP_KEY,
    LEGACY_COMPANY_PORTAL_KEY,
    ADAL_USER_DEFINED_KEY,
    KEYSTORE_ENCRYPTED_KEY;

    String getBlobVersion(){
        switch (this) {
            case ADAL_USER_DEFINED_KEY:
            case LEGACY_COMPANY_PORTAL_KEY:
            case LEGACY_AUTHENTICATOR_APP_KEY:
                return VERSION_USER_DEFINED;

            case KEYSTORE_ENCRYPTED_KEY:
                return VERSION_ANDROID_KEY_STORE;

            default:
                throw new IllegalArgumentException("Unexpected KeyType");
        }
    }
}