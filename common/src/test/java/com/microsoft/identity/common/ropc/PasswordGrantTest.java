package com.microsoft.identity.common.ropc;

import com.microsoft.identity.common.exception.ClientException;
import com.microsoft.identity.common.internal.authorities.RopcTestAuthority;
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsTokenRequest;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2Strategy;
import com.microsoft.identity.common.internal.providers.oauth2.TokenRequest;
import com.microsoft.identity.common.internal.providers.oauth2.TokenResult;
import com.microsoft.identity.common.internal.util.StringUtil;
import com.microsoft.identity.common.utilities.Credential;
import com.microsoft.identity.common.utilities.Scenario;
import com.microsoft.identity.common.utilities.TestConfigurationQuery;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

@RunWith(RobolectricTestRunner.class)
public final class PasswordGrantTest {

    private static final String[] SCOPES = {"user.read", "openid", "offline_access", "profile"};
    private static final String CLIENT_ID = "4b0db8c2-9f26-4417-8bde-3f0e3656f8e0";

    private Scenario getDefaultTestScenario() {
        TestConfigurationQuery query = new TestConfigurationQuery();
        query.userType = "Member";
        query.isFederated = false;
        query.federationProvider = "ADFSv4";

        Scenario scenario = Scenario.GetScenario(query);
        return scenario;
    }

    private boolean isEmpty(String[] arr) {
        return arr == null || arr.length == 0;
    }

    private String convertScopesArrayToString(final String[] scopes) {
        if (isEmpty(scopes)) {
            return null;
        }
        final Set<String> scopesInSet = new HashSet<>(Arrays.asList(scopes));
        return StringUtil.convertSetToString(scopesInSet, " ");
    }

    private TokenRequest createTokenRequest(String[] scopes, String username, String password) {
        String scope = convertScopesArrayToString(scopes);

        TokenRequest tokenRequest = new MicrosoftStsTokenRequest();
        tokenRequest.setClientId(CLIENT_ID);
        tokenRequest.setScope(scope);
        tokenRequest.setUsername(username);
        tokenRequest.setPassword(password);

        return tokenRequest;
    }

    @Test
    public void canPerformROPC() throws IOException {

        Scenario scenario = getDefaultTestScenario();
        Credential credential = scenario.getCredential();

        RopcTestAuthority ropcTestAuthority = new RopcTestAuthority();
        OAuth2Strategy testStrategy = ropcTestAuthority.createOAuth2Strategy();

        TokenRequest tokenRequest = createTokenRequest(SCOPES, credential.userName, credential.password);

        try {
            final TokenResult tokenResult = testStrategy.requestToken(tokenRequest);

            assertEquals(true, tokenResult.getSuccess());
        } catch (ClientException exception) {
            fail("Unexpected exception.");
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void usernameNotProvided() throws IOException {

        Scenario scenario = getDefaultTestScenario();
        Credential credential = scenario.getCredential();;

        RopcTestAuthority ropcTestAuthority = new RopcTestAuthority();
        OAuth2Strategy testStrategy = ropcTestAuthority.createOAuth2Strategy();

        TokenRequest tokenRequest = createTokenRequest(SCOPES, null, credential.password);

        try {
            final TokenResult tokenResult = testStrategy.requestToken(tokenRequest);

            assertEquals(true, tokenResult.getSuccess());
        } catch (ClientException exception) {
            fail("Unexpected exception.");
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void passwordNotProvided() throws IOException {

        Scenario scenario = getDefaultTestScenario();
        Credential credential = scenario.getCredential();

        RopcTestAuthority ropcTestAuthority = new RopcTestAuthority();
        OAuth2Strategy testStrategy = ropcTestAuthority.createOAuth2Strategy();

        TokenRequest tokenRequest = createTokenRequest(SCOPES, credential.userName, null);

        try {
            final TokenResult tokenResult = testStrategy.requestToken(tokenRequest);

            assertEquals(true, tokenResult.getSuccess());
        } catch (ClientException exception) {
            fail("Unexpected exception.");
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void scopeNotProvided() throws IOException {

        Scenario scenario = getDefaultTestScenario();
        Credential credential = scenario.getCredential();

        RopcTestAuthority ropcTestAuthority = new RopcTestAuthority();
        OAuth2Strategy testStrategy = ropcTestAuthority.createOAuth2Strategy();

        TokenRequest tokenRequest = createTokenRequest(null, credential.userName, credential.password);

        try {
            final TokenResult tokenResult = testStrategy.requestToken(tokenRequest);

            assertEquals(true, tokenResult.getSuccess());
        } catch (ClientException exception) {
            fail("Unexpected exception.");
        }
    }

}
