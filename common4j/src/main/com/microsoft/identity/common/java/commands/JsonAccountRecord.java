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
package com.microsoft.identity.common.java.commands;

import com.google.gson.annotations.SerializedName;
import com.microsoft.identity.common.java.dto.IAccountRecord;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.Accessors;

/**
 * A DTO for the account object.
 */
@Getter
@Builder
@Accessors(prefix = "m")
public class JsonAccountRecord implements IAccountRecord {
    @SerializedName("homeAccountId")
    private String mHomeAccountId;
    @SerializedName("environment")
    private String mEnvironment;
    @SerializedName("realm")
    private String mRealm;
    @SerializedName("localAccountId")
    private String mLocalAccountId;
    @SerializedName("username")
    private String mUsername;
    @SerializedName("authorityType")
    private String mAuthorityType;
    @SerializedName("alternativeAccountId")
    private String mAlternativeAccountId;
    @SerializedName("firstName")
    private String mFirstName;
    @SerializedName("familyName")
    private String mFamilyName;
    @SerializedName("middleName")
    private String mMiddleName;
    @SerializedName("name")
    private String mName;
    @SerializedName("avatarUrl")
    private String mAvatarUrl;
    @SerializedName("clientInfo")
    private String mClientInfo;

    /**
     * Provide a translation to JsonAccountRecord for any IAccountRecord class.
     * @param record
     * @return
     */
    public static JsonAccountRecord of(final @NonNull IAccountRecord record) {
        // Don't make useless copies if we're sloppy.
        if (record instanceof JsonAccountRecord) {
            return (JsonAccountRecord) record;
        }
        return JsonAccountRecord.builder()
                .homeAccountId(record.getHomeAccountId())
                .environment(record.getEnvironment())
                .realm(record.getRealm())
                .localAccountId(record.getLocalAccountId())
                .username(record.getUsername())
                .authorityType(record.getAuthorityType())
                .alternativeAccountId(record.getAlternativeAccountId())
                .firstName(record.getFirstName())
                .familyName(record.getFamilyName())
                .middleName(record.getMiddleName())
                .name(record.getName())
                .avatarUrl(record.getAvatarUrl())
                .clientInfo(record.getClientInfo())
        .build();
    }
}
