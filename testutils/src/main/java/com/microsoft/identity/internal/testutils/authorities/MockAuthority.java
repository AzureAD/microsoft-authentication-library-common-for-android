package com.microsoft.identity.internal.testutils.authorities;

import com.microsoft.identity.common.internal.authorities.AnyOrganizationalAccount;
import com.microsoft.identity.common.internal.authorities.AzureActiveDirectoryAuthority;
import com.microsoft.identity.common.internal.logging.Logger;
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsOAuth2Configuration;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2Strategy;
import com.microsoft.identity.internal.testutils.strategies.MockTestStrategy;

import java.util.Map;

public class MockAuthority extends AzureActiveDirectoryAuthority {

    private static transient final String TAG = AADTestAuthority.class.getSimpleName();

    public MockAuthority() {
        super(new AnyOrganizationalAccount());
    }

    @Override
    public OAuth2Strategy createOAuth2Strategy() {
        final String methodName = ":createOAuth2Strategy";
        Logger.verbose(
                TAG + methodName,
                "Creating OAuth2Strategy"
        );
        MicrosoftStsOAuth2Configuration config = new MicrosoftStsOAuth2Configuration();
        config.setAuthorityUrl(this.getAuthorityURL());

        if (mSlice != null) {
            Logger.info(
                    TAG + methodName,
                    "Setting slice parameters..."
            );
            com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectory.AzureActiveDirectorySlice slice =
                    new com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectory.AzureActiveDirectorySlice();
            slice.setSlice(mSlice.getSlice());
            slice.setDataCenter(mSlice.getDC());
            config.setSlice(slice);
        }

        if (mFlightParameters != null) {
            Logger.info(
                    TAG + methodName,
                    "Setting flight parameters..."
            );
            //GSON Returns a LinkedTreeMap which implement AbstractMap....
            for (Map.Entry<String, String> entry : mFlightParameters.entrySet()) {
                config.getFlightParameters().put(entry.getKey(), entry.getValue());
            }
        }


        config.setMultipleCloudsSupported(mMultipleCloudsSupported);

        // return a mock strategy that returns mocked token responses when tokens are requested
        return new MockTestStrategy(config);
    }

}
