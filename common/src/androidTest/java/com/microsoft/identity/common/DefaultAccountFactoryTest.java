package com.microsoft.identity.common;

import android.support.test.runner.AndroidJUnit4;
import android.util.Base64;

import com.microsoft.identity.common.internal.cache.DefaultAccountFactory;
import com.microsoft.identity.common.internal.dto.Account;
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

import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(AndroidJUnit4.class)
public class DefaultAccountFactoryTest {

    private static final String MOCK_TID = "7744ecc5-e130-4af1-ba81-749c395efc8c";
    private static final String MOCK_AUTHORITY = "https://login.microsoftonline.com";
    private static final String MOCK_ID_TOKEN_WITH_CLAIMS = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJNb2NrIElzc3VlciIsImlhdCI6MTUyMTQ5ODk1MCwiZXhwIjoxNTUzMDM1NjU2LCJhdWQiOiJ3d3cuZmFrZWRvbWFpbi5jb20iLCJzdWIiOiJmYWtlLmVtYWlsQGZha2Vkb21haW4uY29tIiwib2lkIjoiMWMxZGI2MjYtMGZjYi00MmJiLWIzOWUtOGU5ODNkZDkyOTMyIiwicHJlZmVycmVkX3VzZXJuYW1lIjoiYm1lbHRvbiIsImdpdmVuX25hbWUiOiJCcmlhbiIsImZhbWlseV9uYW1lIjoiTWVsdG9uLUdyYWNlIn0.5yImfq2TvQYPMBM49A1Dzoi2DnRPCUzFYe5hTrW9DxE";
    private static final String MOCK_CLIENT_INFO = createRawClientInfo("test_uid", "test_utid");

    @Mock
    MicrosoftStsOAuth2Strategy mockStrategy;

    @Mock
    MicrosoftStsAuthorizationRequest mockRequest;

    @Mock
    MicrosoftStsTokenResponse mockResponse;

    @Mock
    MicrosoftStsAccount mockAccount;

    private DefaultAccountFactory mAccountFactory;

    @Before
    public void setUp() throws MalformedURLException {
        MockitoAnnotations.initMocks(this);
        when(mockStrategy.createAccount(any(TokenResponse.class))).thenReturn(mockAccount);
        when(mockRequest.getAuthority()).thenReturn(new URL(MOCK_AUTHORITY));
        when(mockResponse.getIdToken()).thenReturn(MOCK_ID_TOKEN_WITH_CLAIMS);
        when(mockResponse.getClientInfo()).thenReturn(MOCK_CLIENT_INFO);
        when(mockAccount.getTenantId()).thenReturn(MOCK_TID);
        mAccountFactory = new DefaultAccountFactory();
    }

    @Test
    public void createAccount() throws Exception {
        final Account account = mAccountFactory.createAccount(mockStrategy, mockRequest, mockResponse);
        // TODO verify Account contents...
        assertNotNull(account);
    }

    static String createRawClientInfo(final String uid, final String utid) {
        final String claims = "{\"uid\":\"" + uid + "\",\"utid\":\"" + utid + "\"}";

        return new String(Base64.encode(claims.getBytes(
                Charset.forName("UTF-8")), Base64.NO_PADDING | Base64.NO_WRAP | Base64.URL_SAFE));
    }

}
