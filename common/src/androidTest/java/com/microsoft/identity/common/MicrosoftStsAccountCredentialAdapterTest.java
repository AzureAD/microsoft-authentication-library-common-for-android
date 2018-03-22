package com.microsoft.identity.common;

import android.support.test.runner.AndroidJUnit4;
import android.util.Base64;

import com.microsoft.identity.common.internal.cache.MicrosoftStsAccountCredentialAdapter;
import com.microsoft.identity.common.internal.dto.AccessToken;
import com.microsoft.identity.common.internal.dto.Account;
import com.microsoft.identity.common.internal.dto.RefreshToken;
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsAccount;
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsAuthorizationRequest;
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsOAuth2Strategy;
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsTokenResponse;
import com.microsoft.identity.common.internal.providers.oauth2.TokenResponse;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(AndroidJUnit4.class)
public class MicrosoftStsAccountCredentialAdapterTest {

    private static final String MOCK_GIVEN_NAME = "Brian";
    private static final String MOCK_FAMILY_NAME = "Melton-Grace";
    private static final String MOCK_PREFERRED_USERNAME = "bmelton";
    private static final String MOCK_OID = "1c1db626-0fcb-42bb-b39e-8e983dd92932";
    private static final String MOCK_TID = "7744ecc5-e130-4af1-ba81-749c395efc8c";
    private static final String MOCK_AUTHORITY = "https://login.microsoftonline.com";
    private static final String MOCK_ID_TOKEN_WITH_CLAIMS = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJNb2NrIElzc3VlciIsImlhdCI6MTUyMTQ5ODk1MCwiZXhwIjoxNTUzMDM1NjU2LCJhdWQiOiJ3d3cuZmFrZWRvbWFpbi5jb20iLCJzdWIiOiJmYWtlLmVtYWlsQGZha2Vkb21haW4uY29tIiwib2lkIjoiMWMxZGI2MjYtMGZjYi00MmJiLWIzOWUtOGU5ODNkZDkyOTMyIiwicHJlZmVycmVkX3VzZXJuYW1lIjoiYm1lbHRvbiIsImdpdmVuX25hbWUiOiJCcmlhbiIsImZhbWlseV9uYW1lIjoiTWVsdG9uLUdyYWNlIn0.5yImfq2TvQYPMBM49A1Dzoi2DnRPCUzFYe5hTrW9DxE";
    private static final String MOCK_UID = "mock_uid";
    private static final String MOCK_UTID = "mock_utid";
    private static final String MOCK_CLIENT_INFO = createRawClientInfo(MOCK_UID, MOCK_UTID);
    private static final String MOCK_SCOPE = "user.read";
    private static final String MOCK_FAMILY_ID = "1";
    private static final long MOCK_EXPIRES_IN = 3600L;

    @Mock
    MicrosoftStsOAuth2Strategy mockStrategy;

    @Mock
    MicrosoftStsAuthorizationRequest mockRequest;

    @Mock
    MicrosoftStsTokenResponse mockResponse;

    @Mock
    MicrosoftStsAccount mockAccount;

    private MicrosoftStsAccountCredentialAdapter mAccountFactory;

    @Before
    public void setUp() throws MalformedURLException {
        MockitoAnnotations.initMocks(this);
        when(mockStrategy.createAccount(any(TokenResponse.class))).thenReturn(mockAccount);
        when(mockRequest.getAuthority()).thenReturn(new URL(MOCK_AUTHORITY));
        when(mockResponse.getIdToken()).thenReturn(MOCK_ID_TOKEN_WITH_CLAIMS);
        when(mockResponse.getClientInfo()).thenReturn(MOCK_CLIENT_INFO);
        when(mockAccount.getTenantId()).thenReturn(MOCK_TID);
        when(mockRequest.getScope()).thenReturn(MOCK_SCOPE);
        when(mockResponse.getExpiresIn()).thenReturn(MOCK_EXPIRES_IN);
        when(mockResponse.getFamilyId()).thenReturn(MOCK_FAMILY_ID);
        mAccountFactory = new MicrosoftStsAccountCredentialAdapter();
    }

    @Test
    public void createAccount() throws Exception {
        final Account account = mAccountFactory.createAccount(mockStrategy, mockRequest, mockResponse);
        assertNotNull(account);
        assertEquals(MOCK_UID + "." + MOCK_UTID, account.getUniqueId());
        assertEquals(MOCK_AUTHORITY, account.getEnvironment());
        assertEquals(MOCK_TID, account.getRealm());
        assertEquals(MOCK_OID, account.getAuthorityAccountId());
        assertEquals(MOCK_PREFERRED_USERNAME, account.getUsername());
        assertEquals("MSSTS", account.getAuthorityType());
        assertEquals(MOCK_GIVEN_NAME, account.getFirstName());
        assertEquals(MOCK_FAMILY_NAME, account.getLastName());
    }

    @Test
    public void createAccessToken() throws Exception {
        final AccessToken accessToken = mAccountFactory.createAccessToken(mockStrategy, mockRequest, mockResponse);
        assertNotNull(accessToken);
        assertEquals(MOCK_SCOPE, accessToken.getTarget());
        assertNotNull(accessToken.getCachedAt());
        assertNotNull(accessToken.getExpiresOn());
        final long cachedAt = Long.valueOf(accessToken.getCachedAt());
        final long expiresOn = Long.valueOf(accessToken.getExpiresOn());
        assertEquals(cachedAt + MOCK_EXPIRES_IN, expiresOn);
        assertEquals(MOCK_CLIENT_INFO, accessToken.getClientInfo());
        assertEquals(MOCK_TID, accessToken.getRealm());
        assertEquals(MOCK_AUTHORITY, accessToken.getAuthority());
    }

    @Test
    public void createRefreshToken() throws Exception {
        final RefreshToken refreshToken = mAccountFactory.createRefreshToken(mockStrategy, mockRequest, mockResponse);
        assertNotNull(refreshToken);
        assertNotNull(refreshToken);
        assertEquals(MOCK_SCOPE, refreshToken.getTarget());
        assertNotNull(refreshToken.getCachedAt());
        assertNotNull(refreshToken.getExpiresOn());
        final long cachedAt = Long.valueOf(refreshToken.getCachedAt());
        final long expiresOn = Long.valueOf(refreshToken.getExpiresOn());
        assertEquals(cachedAt + MOCK_EXPIRES_IN, expiresOn);
        assertEquals(MOCK_CLIENT_INFO, refreshToken.getClientInfo());
        assertEquals(MOCK_FAMILY_ID, refreshToken.getFamilyId());
        assertEquals(MOCK_PREFERRED_USERNAME, refreshToken.getUsername());
    }

    static String createRawClientInfo(final String uid, final String utid) {
        final String claims = "{\"uid\":\"" + uid + "\",\"utid\":\"" + utid + "\"}";

        return new String(Base64.encode(claims.getBytes(
                Charset.forName("UTF-8")), Base64.NO_PADDING | Base64.NO_WRAP | Base64.URL_SAFE));
    }

}
