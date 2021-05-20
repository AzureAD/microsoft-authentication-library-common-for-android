package com.microsoft.identity.common.unit.internal.request;

import android.app.Activity;
import android.content.Context;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.microsoft.identity.common.exception.ClientException;
import com.microsoft.identity.common.internal.authorities.AccountsInOneOrganization;
import com.microsoft.identity.common.internal.authorities.AllAccounts;
import com.microsoft.identity.common.internal.authorities.AnyOrganizationalAccount;
import com.microsoft.identity.common.internal.authorities.Authority;
import com.microsoft.identity.common.internal.authorities.AzureActiveDirectoryAudience;
import com.microsoft.identity.common.internal.authorities.AzureActiveDirectoryAuthority;
import com.microsoft.identity.common.internal.authscheme.AbstractAuthenticationScheme;
import com.microsoft.identity.common.internal.authscheme.BearerAuthenticationSchemeInternal;
import com.microsoft.identity.common.internal.broker.BrokerRequest;
import com.microsoft.identity.common.internal.cache.AccountDeletionRecord;
import com.microsoft.identity.common.internal.cache.ICacheRecord;
import com.microsoft.identity.common.internal.commands.parameters.BrokerInteractiveTokenCommandParameters;
import com.microsoft.identity.common.internal.commands.parameters.InteractiveTokenCommandParameters;
import com.microsoft.identity.common.internal.dto.AccountRecord;
import com.microsoft.identity.common.internal.dto.Credential;
import com.microsoft.identity.common.internal.dto.CredentialType;
import com.microsoft.identity.common.internal.dto.IAccountRecord;
import com.microsoft.identity.common.internal.dto.IdTokenRecord;
import com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectory.AzureActiveDirectoryCloud;
import com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectory.AzureActiveDirectorySlice;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationRequest;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2Strategy;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2TokenCache;
import com.microsoft.identity.common.internal.providers.oauth2.OpenIdConnectPromptParameter;
import com.microsoft.identity.common.internal.providers.oauth2.TokenResponse;
import com.microsoft.identity.common.internal.request.MsalBrokerRequestAdapter;
import com.microsoft.identity.common.internal.request.SdkType;
import com.microsoft.identity.common.internal.ui.AuthorizationAgent;
import com.microsoft.identity.common.internal.ui.browser.BrowserDescriptor;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.MockitoRule;
import org.mockito.junit.MockitoTestRule;
import org.robolectric.RobolectricTestRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

@RunWith(RobolectricTestRunner.class)
public class MsalBrokerRequestAdapterTest {

    public static final String TEST_APPLICATION_NAME = "application";
    public static final String TEST_APPLICATION_VERSION = "version";
    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    public static final boolean CLOUD_IS_VALIDATES = true;
    public static final String CACHE_HOST_NAME = "cacheHostName";
    public static final String NETWORK_HOST_NAME = "networkHostName";
    public static final String TEST_TENANT_ID = "aTenantId";
    public static final String TEST_CLOUD_URL = "https://login.fabrikam.com";
    public static final AzureActiveDirectoryAudience TEST_AUDIENCE = AccountsInOneOrganization.builder().tenantId(TEST_TENANT_ID).cloudUrl(TEST_CLOUD_URL)
            .build();
    public static final AzureActiveDirectorySlice TEST_SLICE = AzureActiveDirectorySlice.builder().slice("aSlice").dataCenter("dataCenter").build();
    public static Context mockContext = Mockito.mock(Context.class);

    public static final IAccountRecord TEST_ACCOUNT_RECORD = new IAccountRecord() {
        @Override
        public String getHomeAccountId() {
            return null;
        }

        @Override
        public String getEnvironment() {
            return null;
        }

        @Override
        public String getRealm() {
            return null;
        }

        @Override
        public String getLocalAccountId() {
            return null;
        }

        @Override
        public String getUsername() {
            return null;
        }

        @Override
        public String getAuthorityType() {
            return null;
        }

        @Override
        public String getAlternativeAccountId() {
            return null;
        }

        @Override
        public String getFirstName() {
            return null;
        }

        @Override
        public String getFamilyName() {
            return null;
        }

        @Override
        public String getMiddleName() {
            return null;
        }

        @Override
        public String getName() {
            return null;
        }

        @Override
        public String getAvatarUrl() {
            return null;
        }

        @Override
        public String getClientInfo() {
            return null;
        }
    };
    public static final List<BrowserDescriptor> TEST_BROWSER_SAFE_LIST = Arrays.asList(
            BrowserDescriptor.builder()
                    .packageName("aBrowser")
                    .signatureHashes(Collections.singleton("browserHash"))
                    .versionLowerBound("1")
                    .versionUpperBound("2")
                    .build());
    public static final String TEST_CLAIMS_JSON = "{ \"claims\": \"something\"";
    public static final String TEST_CLIENT_ID = "aClientId";
    public static final String TEST_CORRELATION_ID = "aCorrelationId";
    public static final List<Pair<String, String>> TEST_EXTRA_OPTIONS = Arrays.asList(new Pair<String, String>("one", "two"));
    public static final List<Pair<String, String>> TEST_EXTRA_QUERY_STRING_PARAMETERS = Arrays.asList(new Pair<String, String>("QPone", "QPtwo"));
    public static final List<String> TEST_EXTRA_SCOPE = Arrays.asList("extraScope");
    public static final String TEST_LOGIN_HINT = "aLoginHint";
    public static final OAuth2TokenCache TEST_OAUTH_2_TOKEN_CACHE = new OAuth2TokenCache<OAuth2Strategy, AuthorizationRequest, TokenResponse>(mockContext) {
        @Override
        public ICacheRecord save(OAuth2Strategy oAuth2Strategy, AuthorizationRequest request, TokenResponse response) throws ClientException {
            return null;
        }

        @Override
        public List<ICacheRecord> saveAndLoadAggregatedAccountData(OAuth2Strategy oAuth2Strategy, AuthorizationRequest request, TokenResponse response) throws ClientException {
            return null;
        }

        @Override
        public ICacheRecord save(AccountRecord accountRecord, IdTokenRecord idTokenRecord) {
            return null;
        }

        @Override
        public ICacheRecord load(String clientId, String target, AccountRecord account, AbstractAuthenticationScheme authScheme) {
            return null;
        }

        @Override
        public List<ICacheRecord> loadWithAggregatedAccountData(String clientId, String target, AccountRecord account, AbstractAuthenticationScheme authenticationScheme) {
            return null;
        }

        @Override
        public boolean removeCredential(Credential credential) {
            return false;
        }

        @Override
        public AccountRecord getAccount(String environment, String clientId, String homeAccountId, String realm) {
            return null;
        }

        @Override
        public List<ICacheRecord> getAccountsWithAggregatedAccountData(String environment, String clientId, String homeAccountId) {
            return null;
        }

        @Override
        public AccountRecord getAccountByLocalAccountId(String environment, String clientId, String localAccountId) {
            return null;
        }

        @Override
        public ICacheRecord getAccountWithAggregatedAccountDataByLocalAccountId(String environment, String clientId, String localAccountId) {
            return null;
        }

        @Override
        public List<AccountRecord> getAccounts(String environment, String clientId) {
            return null;
        }

        @Override
        public List<AccountRecord> getAllTenantAccountsForAccountByClientId(String clientId, AccountRecord accountRecord) {
            return null;
        }

        @Override
        public List<ICacheRecord> getAccountsWithAggregatedAccountData(String environment, String clientId) {
            return null;
        }

        @Override
        public List<IdTokenRecord> getIdTokensForAccountRecord(String clientId, AccountRecord accountRecord) {
            return null;
        }

        @Override
        public AccountDeletionRecord removeAccount(String environment, String clientId, String homeAccountId, String realm) {
            return null;
        }

        @Override
        public AccountDeletionRecord removeAccount(String environment, String clientId, String homeAccountId, String realm, CredentialType... typesToRemove) {
            return null;
        }

        @Override
        public void clearAll() {

        }

        @Override
        protected Set<String> getAllClientIds() {
            return null;
        }

        @Override
        public AccountRecord getAccountByHomeAccountId(@Nullable String environment, @NonNull String clientId, @NonNull String homeAccountId) {
            return null;
        }
    };
    public static final String TEST_REDIRECT_URI = "aRedirectUri";
    public static final HashMap<String, String> TEST_REQUEST_HEADERS = new HashMap<>(Collections.singletonMap("aHeader", "aValue"));
    public static final String REQUIRED_BROKER_PROTOCOL_VERSION = "3.0";
    public static final String SDK_VERSION = "sdkVersion";
    public static final Set<String> TEST_SCOPES = Collections.singleton("aScope");
    public static final List<String> CLOUD_ALIASES = Arrays.asList("common");
    public static final String TEST_AUTHORITY_TYPE = "AAD";
    public static final AzureActiveDirectoryCloud TEST_CLOUD = AzureActiveDirectoryCloud.builder()
            .cloudHostAliases(CLOUD_ALIASES)
            .isValidated(CLOUD_IS_VALIDATES)
            .preferredCacheHostName(CACHE_HOST_NAME)
            .preferredNetworkHostName(NETWORK_HOST_NAME)
            .build();
    public static final AzureActiveDirectoryAuthority TEST_AUTHORITY = AzureActiveDirectoryAuthority.builder()
            .audience(TEST_AUDIENCE)
            .slice(TEST_SLICE)
            .authorityTypeString(TEST_AUTHORITY_TYPE)
            .azureActiveDirectoryCloud(TEST_CLOUD)
            .authorityUrl("https://an.authority.url/")
            .build();

    @Test
    public void testRequestGeneration() {
        final MsalBrokerRequestAdapter adapter = new MsalBrokerRequestAdapter();
        final boolean forceRefresh = true;
        final Fragment TEST_FRAGMENT = new Fragment();
        final boolean handleNullTaskAffinity = true;
        final boolean isSharedDevice = true;
        final boolean isWebViewZoomControlsEnabled = true;
        final boolean isWebViewZoomEnabled = true;
        final boolean powerOptCheckEnabled = true;
        final OpenIdConnectPromptParameter promptParameter = OpenIdConnectPromptParameter.LOGIN;
        final SdkType sdkType = SdkType.ADAL;
        InteractiveTokenCommandParameters params = InteractiveTokenCommandParameters.builder()
                .authority(TEST_AUTHORITY)
                .activity(new Activity())
                .applicationName(TEST_APPLICATION_NAME)
                .applicationVersion(TEST_APPLICATION_VERSION)
                .authenticationScheme(BearerAuthenticationSchemeInternal.builder().build())
                .authorizationAgent(AuthorizationAgent.BROWSER)
                .brokerBrowserSupportEnabled(false)
                .browserSafeList(TEST_BROWSER_SAFE_LIST)
                .account(TEST_ACCOUNT_RECORD)
                .claimsRequestJson(TEST_CLAIMS_JSON)
                .clientId(TEST_CLIENT_ID)
                .correlationId(TEST_CORRELATION_ID)
                .extraOptions(TEST_EXTRA_OPTIONS)
                .extraQueryStringParameters(TEST_EXTRA_QUERY_STRING_PARAMETERS)
                .extraScopesToConsent(TEST_EXTRA_SCOPE)
                .forceRefresh(forceRefresh)
                .fragment(TEST_FRAGMENT)
                .handleNullTaskAffinity(handleNullTaskAffinity)
                .isSharedDevice(isSharedDevice)
                .isWebViewZoomControlsEnabled(isWebViewZoomControlsEnabled)
                .isWebViewZoomEnabled(isWebViewZoomEnabled)
                .loginHint(TEST_LOGIN_HINT)
                .oAuth2TokenCache(TEST_OAUTH_2_TOKEN_CACHE)
                .powerOptCheckEnabled(powerOptCheckEnabled)
                .prompt(promptParameter)
                .redirectUri(TEST_REDIRECT_URI)
                .requestHeaders(TEST_REQUEST_HEADERS)
                .requiredBrokerProtocolVersion(REQUIRED_BROKER_PROTOCOL_VERSION)
                .sdkType(sdkType)
                .sdkVersion(SDK_VERSION)
                .scopes(TEST_SCOPES)
                .build();
        BrokerRequest brokerRequest = adapter.brokerRequestFromAcquireTokenParameters(params);
        Activity mockActivity = Mockito.mock(Activity.class);
        BrokerInteractiveTokenCommandParameters out = adapter.BrokerInteactiveParametersFromBrokerRequest(mockActivity, 0, "3.0",
                brokerRequest);
        Assert.assertEquals(TEST_SCOPES, out.getScopes());
        Assert.assertEquals(null, out.getFragment());
        Assert.assertEquals(TEST_AUTHORITY, out.getAuthority());
        Assert.assertEquals(sdkType, out.getSdkType());
        Assert.assertEquals(SDK_VERSION, out.getSdkVersion());
        Assert.assertEquals(TEST_APPLICATION_NAME, out.getApplicationName());
        Assert.assertEquals(TEST_APPLICATION_VERSION, out.getApplicationVersion());
        Assert.assertEquals(TEST_EXTRA_OPTIONS, out.getExtraOptions());
        Assert.assertEquals(null, out.getBrowserSafeList());
        Assert.assertEquals(TEST_CLAIMS_JSON, out.getClaimsRequestJson());
        Assert.assertEquals(TEST_CLIENT_ID, out.getClientId());
        Assert.assertEquals(TEST_CORRELATION_ID, out.getCorrelationId());
        Assert.assertEquals(TEST_LOGIN_HINT, out.getLoginHint());
        Assert.assertEquals(null, out.getAccount());
    }

    @Test
    public void authorityFromUrlTest() {
        AzureActiveDirectoryAuthority a = (AzureActiveDirectoryAuthority) Authority.getAuthorityFromAuthorityUrl("https://login.fabrikam.com/aTenantId");
        Assert.assertEquals("https://login.fabrikam.com/aTenantId", a.getAuthorityURL().toString());
    }
}
