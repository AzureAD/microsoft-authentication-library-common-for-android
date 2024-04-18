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
package com.microsoft.identity.common.java.providers.microsoft.azureactivedirectory;

import com.microsoft.identity.common.java.exception.ErrorStrings;
import com.microsoft.identity.common.java.exception.ServiceException;
import com.microsoft.identity.common.java.util.Base64;
import com.microsoft.identity.common.java.util.JsonUtil;
import com.microsoft.identity.common.java.util.StringUtil;
import com.microsoft.identity.common.java.util.base64.Base64Flags;

import org.json.JSONException;

import java.io.Serializable;
import java.util.EnumSet;
import java.util.Map;

import lombok.NonNull;

/**
 * Object representation of client_info returned by AAD's Token Endpoint.
 */
public class ClientInfo implements Serializable {

    private static final String UNIQUE_IDENTIFIER = "uid";
    private static final String UNIQUE_TENANT_IDENTIFIER = "utid";
    private static final long serialVersionUID = 3326461566190095403L;

    /**
     * Unique identifier for a user in the current tenant.
     */
    private String mUid;

    /**
     * Unique identifier for a tenant.
     */
    private String mUtid;

    private String mRawClientInfo;

    /**
     * Constructor for ClientInfo object.
     *
     * @param rawClientInfo raw client info
     * @throws ServiceException if rawIdToken is malformed in JSON format.
     */
    public ClientInfo(@NonNull String rawClientInfo) throws ServiceException {
        if (StringUtil.isNullOrEmpty(rawClientInfo)) {
            throw new IllegalArgumentException("ClientInfo cannot be null or blank.");
        }

        // decode the client info first
        final String decodedClientInfo = StringUtil.fromByteArray(
                Base64.decode(rawClientInfo, EnumSet.of(Base64Flags.URL_SAFE)));
        final Map<String, String> clientInfoItems;
        try {
            clientInfoItems = JsonUtil.extractJsonObjectIntoMap(decodedClientInfo);
        } catch (final JSONException e) {
            throw new ServiceException("", ErrorStrings.INVALID_JWT, e);
        }

        mUid = clientInfoItems.get(ClientInfo.UNIQUE_IDENTIFIER);
        mUtid = clientInfoItems.get(ClientInfo.UNIQUE_TENANT_IDENTIFIER);
        mRawClientInfo = rawClientInfo;
    }

    /**
     * Gets the user unique id.
     *
     * @return The user unique id to get.
     */
    public String getUid() {
        return mUid;
    }

    /**
     * Gets the tenant unique id.
     *
     * @return The tenant unique id to get.
     */
    public String getUtid() {
        return mUtid;
    }

    /**
     * Returns the raw String underlying this object.
     *
     * @return the raw ClientInfo String.
     */
    public String getRawClientInfo() {
        return mRawClientInfo;
    }

}
