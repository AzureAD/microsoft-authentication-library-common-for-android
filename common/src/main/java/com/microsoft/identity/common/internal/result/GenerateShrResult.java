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
package com.microsoft.identity.common.internal.result;

import com.google.gson.annotations.SerializedName;
import com.microsoft.identity.common.java.dto.AccountRecord;
import com.microsoft.identity.common.java.exception.ErrorStrings;

import lombok.Getter;
import lombok.Setter;

/**
 * The result of a generateShr request.
 */
@Getter
@Setter
public class GenerateShrResult {

    /**
     * Errors that can be returned in this object. These values also used by OneAuth/MSAL CPP.
     */
    public static class Errors {

        /**
         * Indicates that the supplied home_account_id does not match any
         * {@link AccountRecord} in our [broker] local
         * cache.
         */
        public static final String NO_ACCOUNT_FOUND = ErrorStrings.NO_ACCOUNT_FOUND;

        /**
         * Indicates an error in client-side processing, review the contents of the error message
         * for additional info as to why this error could be thrown. Most likely, there was an
         * issue initializing the keystore to produce the requested SHR.
         */
        public static final String CLIENT_EXCEPTION = "client_exception";
    }

    @SerializedName("shr")
    private String shr;

    @SerializedName("error_code")
    private String errorCode;

    @SerializedName("error_msg")
    private String errorMessage;
}
