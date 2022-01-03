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
package com.microsoft.identity.common.internal.util;

import android.text.TextUtils;

import androidx.annotation.Nullable;

import com.microsoft.identity.common.internal.logging.Logger;

import java.util.regex.Pattern;

public class AccountUtil {

    private static final String TAG = AccountUtil.class.getSimpleName();

    /**
     * Helper method to get uid from home account id
     * V2 home account format : <uid>.<utid>
     * V1 : it's stored as <uid>
     *
     * @param homeAccountId
     * @return valid uid or null if it's not in either of the format.
     */
    @Nullable
    public static String getUIdFromHomeAccountId(@Nullable String homeAccountId) {
        final String methodName = ":getUIdFromHomeAccountId";
        final String DELIMITER_TENANTED_USER_ID = ".";
        final int EXPECTED_ARGS_LEN = 2;
        final int INDEX_USER_ID = 0;

        if (!TextUtils.isEmpty(homeAccountId)) {
            final String[] homeAccountIdSplit = homeAccountId.split(
                    Pattern.quote(DELIMITER_TENANTED_USER_ID)
            );

            if (homeAccountIdSplit.length == EXPECTED_ARGS_LEN) {
                Logger.info(TAG + methodName,
                        "Home account id is tenanted, returning uid "
                );
                return homeAccountIdSplit[INDEX_USER_ID];
            } else if (homeAccountIdSplit.length == 1) {
                Logger.info(TAG + methodName,
                        "Home account id not tenanted, it's the uid added by v1 broker "
                );
                return homeAccountIdSplit[INDEX_USER_ID];
            }
        }

        Logger.warn(TAG + methodName,
                "Home Account id doesn't have uid or tenant id information, returning null "
        );

        return null;
    }
}
