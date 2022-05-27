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
package com.microsoft.identity.labapi.utilities.client;

import java.util.List;

/**
 * A Class to facilitate writing tests for Guest Accounts.
 */
public class LabGuestAccount {
    private final String homeUpn;
    private final String homeTenantId;
    private final String homeDomain;
    private final List<String> guestLabTenants;

    public LabGuestAccount(String homeUpn, String homeDomain, String homeTenantId, List<String> guestLabTenants) {
        this.homeUpn = homeUpn;
        this.homeDomain = homeDomain;
        this.homeTenantId = homeTenantId;
        this.guestLabTenants = guestLabTenants;
    }

    public String getHomeUpn() {
        return homeUpn;
    }

    public String getHomeDomain() {
        return homeDomain;
    }

    public String getHomeTenantId() {
        return homeTenantId;
    }

    public List<String> getGuestLabTenants() {
        return guestLabTenants;
    }
}
