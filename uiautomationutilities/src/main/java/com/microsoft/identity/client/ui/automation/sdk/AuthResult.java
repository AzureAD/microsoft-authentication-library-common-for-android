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
package com.microsoft.identity.client.ui.automation.sdk;

import android.text.TextUtils;
import org.junit.Assert;
import androidx.annotation.NonNull;

import com.microsoft.identity.client.ui.automation.logging.Logger;

import lombok.Getter;

/**
 * An abstract wrapper class for the result obtained from acquire token interactively
 * or silently, it wraps all the parameters obtained from token and also
 * the exception, with methods to assert success or failures.
 */
@Getter
public abstract class AuthResult {

    private final static String TAG = AuthResult.class.getSimpleName();

    private String accessToken;
    private String idToken;
    private String userId;
    private String username;
    private String authority;
    private Exception exception;

    public AuthResult(@NonNull final String accessToken, @NonNull final String idToken, @NonNull final String userId, @NonNull final String username, @NonNull final String authority) {
        Logger.i(TAG, "Initializing the Result Object..");
        this.accessToken = accessToken;
        this.idToken = idToken;
        this.userId = userId;
        this.username = username;
        this.authority = authority;
    }

    public AuthResult(@NonNull final Exception exception) {
        this.exception = exception;
    }

    public void assertSuccess() {
        Logger.i(TAG, "Assert Success if Result Parameters are empty or not..");
        if (exception != null) {
            throw new AssertionError(exception);
        }
        Assert.assertFalse(TextUtils.isEmpty(accessToken));
        Assert.assertFalse(TextUtils.isEmpty(idToken));
        Assert.assertFalse(TextUtils.isEmpty(userId));
        Assert.assertFalse(TextUtils.isEmpty(username));
    }

    public void assertFailure() {
        Logger.i(TAG, "Assert Failure if there is exception being set..");
        Assert.assertNotNull(exception);
    }

    public boolean isAccessTokenEqual(String accessToken){
        return accessToken.equals(this.accessToken);
    }

    public String getAccessToken() {
        return accessToken;
    }
}
