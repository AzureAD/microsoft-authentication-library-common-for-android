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

package com.microsoft.identity.internal.testutils.labutils;

import com.microsoft.identity.internal.test.labapi.model.ConfigInfo;
import com.microsoft.identity.internal.test.labapi.model.UserInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Class to facilitate loading guest accounts from the lab api
 * Takes a lab config object and creates a {@link LabGuest} object to facilitate performing
 * operations on guest accounts
 */
public class LabGuestAccountHelper {

    public static LabGuest loadGuestAccountFromLab(final LabUserQuery query) {
        final List<LabConfig> labConfigs = LabUserHelper.loadUsersForTest(query);

        // set one as the current config; doesn't matter which one
        LabConfig.setCurrentLabConfig(labConfigs.get(0));

        List<String> guestLabTenants = new ArrayList<>();

        for (LabConfig labConfig : labConfigs) {
            guestLabTenants.add(labConfig.getConfigInfo().getUserInfo().getTenantID());
        }

        // pick one config info object to obtain home tenant information
        // doesn't matter which one as all have the same home tenant
        final ConfigInfo configInfo = labConfigs.get(0).getConfigInfo();

        final UserInfo userInfo = configInfo.getUserInfo();

        return new LabGuest(
                userInfo.getHomeUPN(),
                userInfo.getHomeDomain(),
                userInfo.getHomeTenantID(),
                guestLabTenants
        );
    }

    public static String getPasswordForGuestUser(final LabGuest guestUser) {
        final String labName = guestUser.getHomeDomain().split("\\.")[0];
        return LabHelper.getSecret(labName);
    }
}
