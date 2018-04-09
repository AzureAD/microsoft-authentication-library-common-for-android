package com.microsoft.identity.common;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.microsoft.identity.common.adal.internal.AndroidSecretKeyEnabledHelper;
import com.microsoft.identity.common.adal.internal.cache.StorageHelper;
import com.microsoft.identity.common.internal.cache.AccountCredentialCache;
import com.microsoft.identity.common.internal.cache.AccountCredentialCacheKeyValueDelegate;
import com.microsoft.identity.common.internal.cache.IAccountCredentialAdapter;
import com.microsoft.identity.common.internal.cache.IAccountCredentialCache;
import com.microsoft.identity.common.internal.cache.IAccountCredentialCacheKeyValueDelegate;
import com.microsoft.identity.common.internal.cache.IShareSingleSignOnState;
import com.microsoft.identity.common.internal.cache.ISharedPreferencesFileManager;
import com.microsoft.identity.common.internal.cache.MsalOAuth2TokenCache;
import com.microsoft.identity.common.internal.cache.SharedPreferencesFileManager;
import com.microsoft.identity.common.internal.dto.AccessToken;
import com.microsoft.identity.common.internal.dto.Account;
import com.microsoft.identity.common.internal.dto.Credential;
import com.microsoft.identity.common.internal.dto.CredentialType;
import com.microsoft.identity.common.internal.dto.RefreshToken;
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsAuthorizationRequest;
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsOAuth2Strategy;
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsTokenResponse;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;

import static com.microsoft.identity.common.AccountCredentialCacheTest.CLIENT_ID;
import static com.microsoft.identity.common.AccountCredentialCacheTest.ENVIRONMENT;
import static com.microsoft.identity.common.AccountCredentialCacheTest.REALM;
import static com.microsoft.identity.common.AccountCredentialCacheTest.TARGET;
import static com.microsoft.identity.common.AccountCredentialCacheTest.UNIQUE_ID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

@RunWith(AndroidJUnit4.class)
public class MsalOAuth2TokenCacheTest extends AndroidSecretKeyEnabledHelper {

    private MsalOAuth2TokenCache mOauth2TokenCache;
    private ISharedPreferencesFileManager mSharedPreferencesFileManager;

    @Mock
    MicrosoftStsOAuth2Strategy mockStrategy;

    @Mock
    MicrosoftStsAuthorizationRequest mockRequest;

    @Mock
    MicrosoftStsTokenResponse mockResponse;

    @Mock
    IAccountCredentialAdapter mockCredentialAdapter;

    private Account account;
    private AccessToken accessToken;
    private RefreshToken refreshToken;
    private IAccountCredentialCacheKeyValueDelegate keyValueDelegate;
    private IAccountCredentialCache accountCredentialCache;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        MockitoAnnotations.initMocks(this);

        // Used by mocks
        account = new Account();
        account.setUniqueId(UNIQUE_ID);
        account.setEnvironment(ENVIRONMENT);
        account.setRealm(REALM);

        accessToken = new AccessToken();
        accessToken.setUniqueId(UNIQUE_ID);
        accessToken.setEnvironment(ENVIRONMENT);
        accessToken.setCredentialType(CredentialType.AccessToken.name());
        accessToken.setClientId(CLIENT_ID);

        refreshToken = new RefreshToken();
        refreshToken.setUniqueId(UNIQUE_ID);
        refreshToken.setEnvironment(ENVIRONMENT);
        refreshToken.setCredentialType(CredentialType.RefreshToken.name());
        refreshToken.setClientId(CLIENT_ID);

        // Mocks
        when(
                mockCredentialAdapter.createAccount(
                        mockStrategy,
                        mockRequest,
                        mockResponse
                )
        ).thenReturn(account);

        when(
                mockCredentialAdapter.createAccessToken(
                        mockStrategy,
                        mockRequest,
                        mockResponse
                )
        ).thenReturn(accessToken);

        when(
                mockCredentialAdapter.createRefreshToken(
                        mockStrategy,
                        mockRequest,
                        mockResponse
                )
        ).thenReturn(refreshToken);

        // Context and related init
        final Context context = InstrumentationRegistry.getTargetContext();
        mSharedPreferencesFileManager = new SharedPreferencesFileManager(
                context,
                "test_prefs",
                new StorageHelper(context)
        );

        keyValueDelegate = new AccountCredentialCacheKeyValueDelegate();

        accountCredentialCache = new AccountCredentialCache(
                context,
                keyValueDelegate,
                mSharedPreferencesFileManager
        );

        mOauth2TokenCache = new MsalOAuth2TokenCache(
                context,
                accountCredentialCache,
                mockCredentialAdapter,
                new ArrayList<IShareSingleSignOnState>()
        );
    }

    @After
    public void tearDown() {
        mSharedPreferencesFileManager.clear();
    }

    @Test
    public void saveTokens() throws Exception {
        mOauth2TokenCache.saveTokens(
                mockStrategy,
                mockRequest,
                mockResponse
        );

        final List<Account> accounts = accountCredentialCache.getAccounts();
        assertEquals(1, accounts.size());
        assertEquals(account, accounts.get(0));

        final List<Credential> credentials = accountCredentialCache.getCredentials();
        assertEquals(2, credentials.size());

        final List<Credential> rts = new ArrayList<>();
        final List<Credential> ats = new ArrayList<>();

        for (final Credential credential : credentials) {
            if (credential.getCredentialType().equalsIgnoreCase(CredentialType.AccessToken.name())) {
                ats.add(credential);
            } else if (credential.getCredentialType().equalsIgnoreCase(CredentialType.RefreshToken.name())) {
                rts.add(credential);
            } else {
                fail();
            }
        }

        assertEquals(accessToken, ats.get(0));
        assertEquals(refreshToken, rts.get(0));
    }

    @Test
    public void saveTokensWithIntersect() throws Exception {
        // Manually insert an AT with a ltd scope into the cache
        final String extendedScopes = "calendar.modify user.read user.write https://graph.windows.net";

        AccessToken accessTokenToClear = new AccessToken();
        accessTokenToClear.setUniqueId(UNIQUE_ID);
        accessTokenToClear.setEnvironment(ENVIRONMENT);
        accessTokenToClear.setCredentialType(CredentialType.AccessToken.name().toLowerCase());
        accessTokenToClear.setClientId(CLIENT_ID);
        accessTokenToClear.setTarget(TARGET);

        // Save this dummy AT
        accountCredentialCache.saveCredential(accessTokenToClear);

        // Set the wider target on the new AT to write...
        accessToken.setTarget(extendedScopes);

        mOauth2TokenCache.saveTokens(
                mockStrategy,
                mockRequest,
                mockResponse
        );

        final List<Account> accounts = accountCredentialCache.getAccounts();
        assertEquals(1, accounts.size());
        assertEquals(account, accounts.get(0));

        final List<Credential> credentials = accountCredentialCache.getCredentials();
        assertEquals(2, credentials.size());

        final List<Credential> rts = new ArrayList<>();
        final List<Credential> ats = new ArrayList<>();

        for (final Credential credential : credentials) {
            if (credential.getCredentialType().equalsIgnoreCase(CredentialType.AccessToken.name())) {
                ats.add(credential);
            } else if (credential.getCredentialType().equalsIgnoreCase(CredentialType.RefreshToken.name())) {
                rts.add(credential);
            } else {
                fail();
            }
        }

        assertEquals(accessToken, ats.get(0));
        assertEquals(refreshToken, rts.get(0));
    }

}
