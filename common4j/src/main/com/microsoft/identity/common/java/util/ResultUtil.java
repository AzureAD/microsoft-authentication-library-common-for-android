/**
 * // Copyright (c) Microsoft Corporation.
 * // All rights reserved.
 * //
 * // This code is licensed under the MIT License.
 * //
 * // Permission is hereby granted, free of charge, to any person obtaining a copy
 * // of this software and associated documentation files(the "Software"), to deal
 * // in the Software without restriction, including without limitation the rights
 * // to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
 * // copies of the Software, and to permit persons to whom the Software is
 * // furnished to do so, subject to the following conditions :
 * //
 * // The above copyright notice and this permission notice shall be included in
 * // all copies or substantial portions of the Software.
 * //
 * // THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * // IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * // FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * // AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * // LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * // OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * // THE SOFTWARE.
 * */

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
package com.microsoft.identity.common.java.util;

import com.microsoft.identity.common.java.WarningType;
import com.microsoft.identity.common.java.logging.Logger;
import com.microsoft.identity.common.java.providers.oauth2.AuthorizationResult;
import com.microsoft.identity.common.java.providers.oauth2.IResult;

import lombok.NonNull;

public class ResultUtil {
    /**
     * Log IResult objects.  IResult objects are returned from Authorization and Token Requests
     *
     * @param tag    The log tag to use.
     * @param result The result object to log.
     */
    public static void logResult(@NonNull final String tag,
                                 @NonNull final IResult result) {
        final String TAG = tag + ":" + result.getClass().getSimpleName();

        if (result.getSuccess()) {
            Logger.info(
                    TAG,
                    "Success Result"
            );

            logExposedFieldsOfObject(TAG, result.getSuccessResponse());
        } else {
            Logger.warn(
                    TAG,
                    "Failure Result"
            );

            if (result.getErrorResponse() != null) {
                if (result.getErrorResponse().getError() != null) {
                    Logger.warn(
                            TAG,
                            "Error: " + result.getErrorResponse().getError()
                    );
                }

                if (result.getErrorResponse().getErrorDescription() != null) {
                    Logger.warnPII(
                            TAG,
                            "Description: " + result.getErrorResponse().getErrorDescription()
                    );
                }

                logExposedFieldsOfObject(TAG, result.getErrorResponse());
            }
        }

        if (result instanceof AuthorizationResult) {
            @SuppressWarnings(WarningType.rawtype_warning)
            AuthorizationResult authResult = (AuthorizationResult) result;

            if (authResult.getAuthorizationStatus() != null) {
                Logger.info(
                        TAG,
                        "Authorization Status: " + authResult.getAuthorizationStatus().toString()
                );
            }
        }
    }

    public static void logExposedFieldsOfObject(@NonNull final String tag,
                                                @NonNull final Object object) {
        final String TAG = tag + ":" + object.getClass().getSimpleName();
        Logger.info(TAG, ObjectMapper.serializeExposedFieldsOfObjectToJsonString(object));
    }
}
