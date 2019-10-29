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

package com.microsoft.identity.internal.testutils.labutils;

import com.microsoft.identity.internal.test.labapi.model.UserInfo;

/**
 * This class contains methods necessary to obtain a Scenario for a given test case
 * A Scenario is defined by a {@link UserInfo} object, and a {@link Credential} object
 */
public class Scenario {

    private UserInfo mUserInfo;
    private Credential mCredential;

    public UserInfo getUserInfo() {
        return mUserInfo;
    }

    public void setUserInfo(UserInfo userInfo) {
        this.mUserInfo = userInfo;
    }

    public Credential getCredential() {
        return mCredential;
    }

    public void setCredential(Credential credential) {
        this.mCredential = credential;
    }

    public static String getPasswordForUser(String upn) {
        LabUserQuery query = new LabUserQuery();
        query.homeUpn = upn;
        Scenario scenario = GetScenario(query);
        String password = scenario.getCredential().password;
        return password;
    }

    public static String getUserFromUpn(final String upn) {
        return upn;
    }

    public static Scenario GetScenario(LabUserQuery query) {
        return null;
        /**UserInfo userInfo = LabUserHelper.getUserInfo(query);
         String keyVaultLocation = userInfo.getCredentialVaultKeyName();
         String secretName = keyVaultLocation.substring(keyVaultLocation.lastIndexOf('/') + 1);

         Credential credential = null;
         try {
         credential = Secrets.GetCredential(tc.getUsers().getUpn(), secretName);
         } catch (Exception e) {
         e.printStackTrace();
         }

         Scenario scenario = new Scenario();
         scenario.setTestConfiguration(tc);
         scenario.setCredential(credential);

         return scenario;**/
    }
}
