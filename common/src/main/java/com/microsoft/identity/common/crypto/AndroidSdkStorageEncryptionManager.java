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
package com.microsoft.identity.common.crypto;

import android.content.Context;

import com.microsoft.identity.common.adal.internal.AuthenticationSettings;
import com.microsoft.identity.common.java.crypto.StorageEncryptionManager;
import com.microsoft.identity.common.java.crypto.key.AES256KeyLoader;
import com.microsoft.identity.common.java.telemetry.ITelemetryCallback;
import com.microsoft.identity.common.logging.Logger;

import java.util.ArrayList;
import java.util.List;

import edu.umd.cs.findbugs.annotations.Nullable;
import lombok.NonNull;

/**
 * Key Encryption Manager for ADAL & MSAL.
 * */
public class AndroidSdkStorageEncryptionManager extends StorageEncryptionManager {
    private static final String TAG = AndroidSdkStorageEncryptionManager.class.getSimpleName();

    private final UserDefinedKeyLoader mUserDefinedKey;
    private final AndroidWrappedKeyLoader mKeyStoreKeyLoader;

    public AndroidSdkStorageEncryptionManager(@NonNull final Context context,
                                              @Nullable final ITelemetryCallback telemetryCallback) {
        if (AuthenticationSettings.INSTANCE.getSecretKeyData() == null) {
            mUserDefinedKey = null;
        } else {
            mUserDefinedKey = new UserDefinedKeyLoader("USER_DEFINED_KEY",
                    AuthenticationSettings.INSTANCE.getSecretKeyData());
        }

        mKeyStoreKeyLoader = new AndroidWrappedKeyLoader(context, telemetryCallback);
    }

    @Override
    public @NonNull AES256KeyLoader getKeyLoaderForEncryption() {
        if (mUserDefinedKey != null) {
            return mUserDefinedKey;
        }

        return mKeyStoreKeyLoader;
    }

    @Override
    public @NonNull List<AES256KeyLoader> getKeyLoaderForDecryption(@NonNull byte[] cipherText) {
        final String methodName = "getKeyLoaderForDecryption";

        if (mUserDefinedKey != null &&
                isEncryptedByThisKeyIdentifier(cipherText, UserDefinedKeyLoader.KEY_IDENTIFIER)) {
            return new ArrayList<AES256KeyLoader>() {{
                add(mUserDefinedKey);
            }};
        }

        if (isEncryptedByThisKeyIdentifier(cipherText, AndroidWrappedKeyLoader.KEY_IDENTIFIER)) {
            return new ArrayList<AES256KeyLoader>() {{
                add(mKeyStoreKeyLoader);
            }};
        }

        Logger.warn(TAG + methodName, "Cannot find a matching key to decrypt the given blob");
        return new ArrayList<>();
    }
}
