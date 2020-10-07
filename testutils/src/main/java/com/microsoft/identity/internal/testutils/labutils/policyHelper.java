package com.microsoft.identity.internal.testutils.labutils;

import com.microsoft.identity.internal.test.labapi.ApiException;
import com.microsoft.identity.internal.test.labapi.api.DisablePolicyApi;
import com.microsoft.identity.internal.test.labapi.api.EnablePolicyApi;
import com.microsoft.identity.internal.test.labapi.model.CustomSuccessResponse;

public class policyHelper {
    public boolean enablePolicy(String upn, String policy)
    {
        LabAuthenticationHelper.getInstance().setupApiClientWithAccessToken();
        final EnablePolicyApi enablePolicyApi = new EnablePolicyApi();
        try{
            final CustomSuccessResponse customSuccessResponse;
            customSuccessResponse = enablePolicyApi.putPolicy(upn, policy);
            final String expectedResult = ("Enabled : " + policy + "policy for user : " + upn)
                    .toLowerCase();
            return customSuccessResponse.getResult().toLowerCase().contains(expectedResult);
        } catch (ApiException e) {
            throw new RuntimeException(e.getMessage());
        }
    }
    public boolean disablePolicy(String upn, String policy){
        LabAuthenticationHelper.getInstance().setupApiClientWithAccessToken();
        final DisablePolicyApi disablePolicyApi = new DisablePolicyApi();
        try{
            final CustomSuccessResponse customSuccessResponse;
            customSuccessResponse = disablePolicyApi.put(upn, policy);
            final String expectedResult = ("disabled : " + policy + " policy for user : " + upn)
                    .toLowerCase();
            return customSuccessResponse.getResult().toLowerCase().contains(expectedResult);
        } catch (ApiException e) {
            throw new RuntimeException(e.getMessage());
        }
    }
}
