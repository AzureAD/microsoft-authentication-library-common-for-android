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

package com.microsoft.identity.internal.testutils.labutils;

import com.microsoft.identity.common.internal.logging.Logger;
import com.microsoft.identity.internal.test.labapi.ApiException;
import com.microsoft.identity.internal.test.labapi.api.DisablePolicyApi;
import com.microsoft.identity.internal.test.labapi.api.EnablePolicyApi;
import com.microsoft.identity.internal.test.labapi.model.CustomSuccessResponse;

import org.junit.Assert;

import lombok.NonNull;

/**
 * PolicyHelper is a class which lets you to enable/disable CA/Special Policies for any Locked user.
 */
public class PolicyHelper {

    private static final String TAG = PolicyHelper.class.getName();
    /**
     * lets you enable CA/Special Policies for any Locked User.
     * Enable Policy can be used for GlobalMFA, MAMCA, MDMCA, MFAONSPO, MFAONEXO.   Also test users can have more than 1 policy assigned to the same user.
     *
     * @param upn    Enter a valid Locked User UPN (optional)
     * @param policy Enable Policy can be used for GlobalMFA, MAMCA, MDMCA, MFAONSPO, MFAONEXO. (optional)
     * @return boolean value indicating policy enabled or not.
     */
    public boolean enablePolicy(@NonNull final String upn, @NonNull final String policy) {
        LabAuthenticationHelper.getInstance().setupApiClientWithAccessToken();
        final EnablePolicyApi enablePolicyApi = new EnablePolicyApi();
        try {
            final CustomSuccessResponse customSuccessResponse = enablePolicyApi.putPolicy(upn, policy);
            final String expectedResult = (policy +" Enabled for user : " + upn).toLowerCase();
            final String result = customSuccessResponse.getResult();
            Assert.assertNotNull(result);
            return result.toLowerCase().contains(expectedResult);
        } catch (final ApiException e) {
            Logger.error(TAG,"Bad Request : Enable Policy can be used only for Locked users.",e);
            throw new AssertionError(e);
        }
    }

    /**
     * lets you disable CA/Special Policies for any Locked User.
     * Disable Policy can be used for GlobalMFA, MAMCA, MDMCA, MFAONSPO, MFAONEXO.   Also test users can have more than 1 policy assigned to the same user.
     *
     * @param upn    Enter a valid Locked User UPN (optional)
     * @param policy Disable Policy can be used for GlobalMFA, MAMCA, MDMCA, MFAONSPO, MFAONEXO. (optional)
     * @return boolean value indicating policy is disabled or not for the upn.
     */
    public boolean disablePolicy(@NonNull final String upn, @NonNull final String policy) {
        LabAuthenticationHelper.getInstance().setupApiClientWithAccessToken();
        final DisablePolicyApi disablePolicyApi = new DisablePolicyApi();
        try {
            final CustomSuccessResponse customSuccessResponse = disablePolicyApi.put(upn, policy);
            final String expectedResult = (policy + " Disabled for user : " + upn).toLowerCase();
            final String result = customSuccessResponse.getResult();
            Assert.assertNotNull(result);
            return result.toLowerCase().contains(expectedResult);
        } catch (final ApiException e) {
            Logger.error(TAG," Bad Request : Disable Policy can be used only for Locked users. ",e);
            throw new AssertionError(e);
        }
    }
}
