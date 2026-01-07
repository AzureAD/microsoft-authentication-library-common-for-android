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

package com.microsoft.identity.labapi.utilities.client;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;

import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.Map;

/**
 * Represents a lab account entry parsed from a JSON string.
 */
public class LabJsonStringAccountEntry implements Serializable {

    @SerializedName("Upn")
    private String upn;
    @SerializedName("HomeObjectId")
    private String homeObjectId;
    @SerializedName("HomeTenantId")
    private String homeTenantId;
    @SerializedName("KeyVaultEntry")
    private String keyVaultEntry;

    public String getUpn() {
        return upn;
    }

    public String getHomeObjectId() {
        return homeObjectId;
    }

    public String getHomeTenantId() {
        return homeTenantId;
    }

    public String getKeyVaultEntry() {
        return keyVaultEntry;
    }


    /**
     * Parses a JSON string into a Map<String, LabJsonStringAccountEntry> using Gson.
     * @param json the JSON string to parse
     * @return a map of key to LabJsonStringAccountEntry
     */
    public static Map<String, LabJsonStringAccountEntry> parseJsonToMap(String json) {
        Gson gson = new Gson();
        Type type = new TypeToken<Map<String, LabJsonStringAccountEntry>>(){}.getType();
        return gson.fromJson(json, type);
    }
}
