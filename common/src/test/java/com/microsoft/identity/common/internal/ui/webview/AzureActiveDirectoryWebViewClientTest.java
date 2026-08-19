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
package com.microsoft.identity.common.internal.ui.webview;

import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.AUTHENTICATOR_MFA_LINKING_PREFIX;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.COMPANY_PORTAL_APP_PACKAGE_NAME;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.PLAY_STORE_INSTALL_PREFIX;
import static com.microsoft.identity.common.java.providers.RawAuthorizationResult.ResultCode.CANCELLED;
import static com.microsoft.identity.common.java.providers.RawAuthorizationResult.ResultCode.MDM_FLOW;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Looper;
import android.webkit.SslErrorHandler;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;

import com.microsoft.identity.common.adal.internal.AuthenticationConstants;
import com.microsoft.identity.common.internal.broker.BrokerValidator;
import com.microsoft.identity.common.internal.mocks.MockCommonFlightsManager;
import com.microsoft.identity.common.internal.telemetry.OnboardingTelemetryRecorder;
import com.microsoft.identity.common.internal.ui.DualScreenActivity;
import com.microsoft.identity.common.internal.ui.OpenIdVcReturnActivity;
import com.microsoft.identity.common.internal.ui.webview.challengehandlers.NonceRedirectHandler;
import com.microsoft.identity.common.internal.ui.webview.challengehandlers.PKeyAuthChallengeHandler;
import com.microsoft.identity.common.internal.ui.webview.challengehandlers.ReAttachPrtHeaderHandler;
import com.microsoft.identity.common.internal.ui.webview.switchbrowser.SwitchBrowserProtocolCoordinator;
import com.microsoft.identity.common.java.challengehandlers.PKeyAuthChallengeFactory;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.exception.ErrorStrings;
import com.microsoft.identity.common.java.flighting.CommonFlight;
import com.microsoft.identity.common.java.flighting.CommonFlightsManager;
import com.microsoft.identity.common.java.flighting.IFlightConfig;
import com.microsoft.identity.common.java.flighting.IFlightsManager;
import com.microsoft.identity.common.java.flighting.IFlightsProvider;
import com.microsoft.identity.common.java.providers.MamInstallReferrerBuilder;
import com.microsoft.identity.common.java.providers.RawAuthorizationResult;
import com.microsoft.identity.common.java.providers.microsoft.azureactivedirectory.AzureActiveDirectory;
import com.microsoft.identity.common.java.providers.microsoft.azureactivedirectory.AzureActiveDirectoryCloud;
import com.microsoft.identity.common.java.ui.webview.authorization.IAuthorizationCompletionCallback;
import com.microsoft.identity.common.shadows.ShadowProcessUtil;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowPackageManager;
import java.net.URL;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import io.opentelemetry.api.trace.Span;
import com.microsoft.identity.common.java.opentelemetry.AttributeName;
import com.microsoft.identity.common.java.opentelemetry.SpanExtension;

/**
 * Tests for {@link AzureActiveDirectoryWebViewClient}.
 */
@RunWith(RobolectricTestRunner.class)
public class AzureActiveDirectoryWebViewClientTest {
    private WebView mMockWebView;
    private AzureActiveDirectoryWebViewClient mWebViewClient;
    private Context mContext;
    private Activity mActivity;
    private static final String TEST_REDIRECT_URI = "msauth://com.example.app/somehash=";

    // Test strings initialized.
    private static final String TEST_PLAY_STORE_INSTALL_AUTH_APP_URL =
            PLAY_STORE_INSTALL_PREFIX + AuthenticationConstants.Broker.AZURE_AUTHENTICATOR_APP_PACKAGE_NAME;
    private static final String TEST_PLAY_STORE_INSTALL_CP_URL =
            PLAY_STORE_INSTALL_PREFIX + COMPANY_PORTAL_APP_PACKAGE_NAME;
    private static final String TEST_PLAY_STORE_INSTALL_INVALID_APP =
            PLAY_STORE_INSTALL_PREFIX + "com.azure.xyz";
    private static final String AUTHENTICATOR_MFA_LINKING_INVALID_URI =
            AUTHENTICATOR_MFA_LINKING_PREFIX + "xyz";
    private static final String TEST_SSL_PROTECTION_HTTP_URL = "http://foo";
    private static final String TEST_SSL_PROTECTION_FTP_URL = "ftp://foo";
    private static final String TEST_REDIRECT_URL = "msauth://com.example.app/somehash=?code=AUTH_CODE&state=xyz";
    private static final String TEST_REDIRECT_URL_WITH_FRAGMENT =
            "msauth://com.example.app/somehash=?code=AUTH_CODE#fragment";

    // isRedirectUrl spoofing-attack vectors (FireWatch c1bf88bd). These use an https://
    // redirect URI on purpose: any msauth:// URL is also caught downstream by
    // isInstallRequestUrl, so https:// isolates the isRedirectUrl behavior. The msauth://
    // install-path gap is tracked separately (see PR description).
    private static final String HTTPS_REDIRECT_URI = "https://login.contoso.com/auth";
    private static final String HTTPS_REDIRECT_LEGIT =
            "https://login.contoso.com/auth?code=AUTH_CODE&state=xyz";
    // Suffixes that would pass a startsWith() check but differ in path.
    private static final String HTTPS_REDIRECT_SPOOFED_SUFFIX_HOST =
            "https://login.contoso.com/auth.attacker.com/x?code=STOLEN&state=xyz";
    private static final String HTTPS_REDIRECT_SPOOFED_PATH_SUFFIX =
            "https://login.contoso.com/authstolen?code=STOLEN&state=xyz";
    // Differs only by a trailing slash — must still match.
    private static final String HTTPS_REDIRECT_TRAILING_SLASH =
            "https://login.contoso.com/auth/?code=AUTH_CODE&state=xyz";
    // Path-less registered redirect URI, and an incoming redirect that adds a root "/" before
    // the query. The empty configured path and the incoming "/" must normalize equal.
    private static final String HTTPS_REDIRECT_URI_NO_PATH = "https://login.contoso.com";
    private static final String HTTPS_REDIRECT_NO_PATH_ROOT_SLASH =
            "https://login.contoso.com/?code=AUTH_CODE&state=xyz";
    // Scheme-less registered redirect URI (defensive branch; not used in practice). The incoming
    // URL still carries the auth code in the query, which must be stripped before comparison.
    private static final String SCHEMELESS_REDIRECT_URI = "login.contoso.com/auth";
    private static final String SCHEMELESS_REDIRECT_LEGIT =
            "login.contoso.com/auth?code=AUTH_CODE&state=xyz";
    // Opaque (urn:) redirect URI — the broker OOB redirect. Authority/path are
    // null, so the matcher must compare the scheme-specific part.
    private static final String OOB_REDIRECT_URI = "urn:ietf:wg:oauth:2.0:oob";
    private static final String OOB_REDIRECT_LEGIT =
            "urn:ietf:wg:oauth:2.0:oob?code=AUTH_CODE&state=xyz";
    private static final String OOB_REDIRECT_SPOOFED_SSP_SUFFIX =
            "urn:ietf:wg:oauth:2.0:oobstolen?code=STOLEN&state=xyz";
    // Hierarchical urn (authority "evil", path "/oob") spoofing the opaque OOB redirect.
    // The registered URI is opaque, this one is not, so the opaque/hierarchical mismatch
    // must be rejected rather than compared on scheme alone.
    private static final String OOB_REDIRECT_SPOOFED_HIERARCHICAL =
            "urn://evil/oob?code=STOLEN&state=xyz";
    private static final String TEST_WEBSITE_REQUEST_URL = "browser://abcxyz/a";
    private static final String TEST_BROWSER_DEVICE_CA_URL_QUERY_STRING_PARAMETER = "browser://abcxyz/xyz&ismdmurl=1";

    private static final String TEST_HTTPS_DEVICE_CA_URL_QUERY_STRING_PARAMETER = "https://abcxyz/xyz&ismdmurl=1";
    private static final String TEST_INSTALL_REQUEST_URL = "msauth://wpj/?username=someusername%somedomain.onmicrosoft.com&app_link=https%3a%2f%2fplay.google.com%2fstore%2fapps%2fdetails%3fid%3dcom.azure.authenticator%26referrer%3dcom.msft.identity.client.sample.local";
    private static final String TEST_DEVICE_REGISTRATION_URL = "msauth://wpj/?username=someusername%somedomain.onmicrosoft.com";
    private static final String TEST_BLANK_PAGE_REQUEST_URL = "about:blank";
    private static final String TEST_PKEY_AUTH_URL = "urn:http-auth:PKeyAuth/xyz";
    private static final String TEST_WEB_CP_URL = "companyportal://abc/123";
    private static final String TEST_PLAYSTORE_FOR_BROKER_APP_URL = "https://play.google.com/store/apps/details?id=com.azure.authenticator";
    private static final String TEST_INVALID_URL = "https://some.invalid.url";
    // Sentinel returned by the mocked WebView.getUrl() so that origin-tracking tests can distinguish
    // "the recorded main-frame URL was used" from "we fell back to view.getUrl()" (AB#3706623).
    private static final String FALLBACK_ORIGIN_URL = "https://fallback.contoso.com/authorize";
    private static final String TEST_MSA_HEADER_FORWARDING_POSITIVE_URL = "https://login.live.com/oauth20_authorize.srf";
    private static final String TEST_MSA_HEADER_FORWARDING_NEGATIVE_URL = "https://login.blah.com/oauth20_authorize.srf";

    private static final String TEST_NONCE_REDIRECT_URL = "https://login.microsoftonline.com/organizations/oAuth2/v2.0/authorize?&sso_nonce=ABCD";
    private static final String TEST_CROSS_CLOUD_REDIRECT_URL = "https://login.microsoftonline.us/organizations/oAuth2/v2.0/authorize?x=10";
    private static final String TEST_PUBLIC_CLOUD_REDIRECT_URL = "https://login.microsoftonline.com/organizations/oAuth2/v2.0/authorize?x=10";
    private static final String TEST_PASSKEY_REDIRECT_URL = "http-auth:PassKey?challenge=challenge&version=1.0&submitUrl=https://login.microsoftonline.com/common/credential?passKeyAuth=1.0%2fpasskey&context=&relyingPartyIdentifier=login.microsoft.com&allowedCredentials=somevalue";
    private static final String TEST_INTENT_INSTALL_BROKER_REDIRECT_URL = "intent://play.google.com/store/apps/details?id=com.azure.authenticator&referrer=%20adjust_reftag%3Dc6f1p4ErudH2C%26utm_source%3DLanding%2BPage%2BOrganic%2B-%2Bapp%2Bstore%2Bbadges%26utm_campaign%3Dappstore_android&pcampaignid=web_auto_redirect&web_logged_in=0&redirect_entry_point=dp#Intent;scheme=https;action=android.intent.action.VIEW;package=com.android.vending;end";

    // Intent fixtures whose effective parsed target differs from the allow-listed Play Store package,
    // used to verify the post-parse validation step.
    private static final String TEST_INTENT_WITH_NON_ALLOWLISTED_PACKAGE = "intent://play.google.com/store/apps/details?referrer=;package=com.android.vending;&id=com.azure.authenticator#Intent;scheme=https;action=android.intent.action.VIEW;package=com.example.unrelatedapp;end";
    private static final String TEST_INTENT_WITH_EXPLICIT_COMPONENT = "intent://play.google.com/store/apps/details?id=com.azure.authenticator#Intent;scheme=https;action=android.intent.action.VIEW;package=com.android.vending;component=com.example.unrelatedapp/.SampleActivity;end";
    private static final String GOOGLE_PLAY_STORE_PACKAGE_NAME = "com.android.vending";

    private static final String TEST_WEB_CP_ENROLLMENT_URL = "https://enterprise.google.com/android/enroll";

    // ---------------------------------------------------------------------------
    // MAM Conditional Access broker-install fixtures.
    //
    // All four are the same msauth://wpj broker-install redirect; they differ only in whether the
    // server marked the install as MAM-CA (intuneAppProtection=1) and whether the app_link already
    // names an install referrer. The app_link is percent-encoded exactly as it arrives on the wire.
    // ---------------------------------------------------------------------------

    /** Play link for Company Portal with no referrer of its own; the client-side decoration applies. */
    private static final String SERVER_SUPPLIED_REFERRER = "com.contoso.serverpicked";
    private static final String ENCODED_CP_APP_LINK =
            "https%3a%2f%2fplay.google.com%2fstore%2fapps%2fdetails%3fid%3dcom.microsoft.windowsintune.companyportal";
    /** The same link, but the server already named an install referrer on it. */
    private static final String ENCODED_CP_APP_LINK_WITH_SERVER_REFERRER =
            ENCODED_CP_APP_LINK + "%26referrer%3d" + SERVER_SUPPLIED_REFERRER;
    /** The same link delivered over the browser:// extension prefix, which the allowlist rejects. */
    private static final String ENCODED_CP_APP_LINK_BROWSER_PREFIX =
            "browser%3a%2f%2fplay.google.com%2fstore%2fapps%2fdetails%3fid%3dcom.microsoft.windowsintune.companyportal";

    private static final String MAM_CA_REDIRECT_PREFIX =
            "msauth://wpj/?username=someuser%40contoso.onmicrosoft.com&intuneAppProtection=1&app_link=";

    /** MAM-CA install: marked by the server, app_link carries no referrer. */
    private static final String TEST_MAM_CA_INSTALL_REQUEST_URL =
            MAM_CA_REDIRECT_PREFIX + ENCODED_CP_APP_LINK;
    /** MAM-CA install where the server already picked the referrer. */
    private static final String TEST_MAM_CA_INSTALL_REQUEST_URL_SERVER_REFERRER =
            MAM_CA_REDIRECT_PREFIX + ENCODED_CP_APP_LINK_WITH_SERVER_REFERRER;
    /** MAM-CA install whose app_link arrives over the browser:// extension prefix. */
    private static final String TEST_MAM_CA_INSTALL_REQUEST_URL_BROWSER_PREFIX =
            MAM_CA_REDIRECT_PREFIX + ENCODED_CP_APP_LINK_BROWSER_PREFIX;
    /** Ordinary device-registration install: no MAM-CA marker, so it must keep its existing behavior. */
    private static final String TEST_PLAIN_INSTALL_REQUEST_URL =
            "msauth://wpj/?username=someuser%40contoso.onmicrosoft.com&app_link=" + ENCODED_CP_APP_LINK;

    private static final String TEST_PLAYSTORE_REDIRECT_WITH_BROWSER_PROTOCOL = "browser://play.app.goo.gl/?link=https://play.google.com/store/apps/details?id=com.microsoft.windowsintune.companyportal";
    private static final String TEST_OPENID_VC_URL = "openid-vc://credential-offer?credential_issuer=https%3A%2F%2Fexample.com&credential_configuration_ids=VerifiedEmployee";

    // Authenticator activation app link test URLs
    private static final String TEST_AUTHENTICATOR_ACTIVATION_GLOBAL =
            "https://login.microsoftonline.com/authenticatorApp/activateAccount?accountType=mfa&source=qrCode&accountType=msa&code=demo&uaid=0022d4c4141444b484dd38026d312794&expires=3971458484";
    private static final String TEST_AUTHENTICATOR_ACTIVATION_CHINA =
            "https://login.chinacloudapi.cn/authenticatorApp/activateAccount?accountType=mfa&source=qrCode&accountType=msa&code=demo&uaid=0022d4c4141444b484dd38026d312794&expires=3971458484";
    private static final String TEST_AUTHENTICATOR_ACTIVATION_US_GOV =
            "https://login.microsoftonline.us/authenticatorApp/activateAccount?accountType=mfa&source=qrCode&accountType=msa&code=demo&uaid=0022d4c4141444b484dd38026d312794&expires=3971458484";
    private static final String TEST_AUTHENTICATOR_ACTIVATION_INVALID_HOST =
            "https://login.evil.com/authenticatorApp/activateAccount?accountType=mfa&code=123";
    private static final String TEST_AUTHENTICATOR_ACTIVATION_INVALID_PATH =
            "https://login.microsoftonline.com/some/other/path?accountType=mfa&code=123";

    @Before
    public void setup() throws ClientException {
        mContext = ApplicationProvider.getApplicationContext();
        mMockWebView = new WebView(mContext);
        mActivity = Robolectric.buildActivity(Activity.class).get();
        mWebViewClient = new AzureActiveDirectoryWebViewClient(
                mActivity,
                new IAuthorizationCompletionCallback() {
                    @Override
                    public void onChallengeResponseReceived(@NonNull RawAuthorizationResult response) {

                    }

                    @Override
                    public void setPKeyAuthStatus(boolean status) {
                        return;
                    }
                },
                new OnPageLoadedCallback() {
                    @Override
                    public void onPageLoaded(final String url) {
                        return;
                    }
                },
                TEST_REDIRECT_URI,
                Mockito.mock(SwitchBrowserProtocolCoordinator.class),
                "homeTenantId",
                false);
        HashMap<String, String> dummyHeaders = new HashMap<>();
        dummyHeaders.put("key", "value");
        mWebViewClient.setRequestHeaders(dummyHeaders);
        mWebViewClient.setRequestUrl(TEST_PUBLIC_CLOUD_REDIRECT_URL);
        AzureActiveDirectory.ensureCloudDiscovery();
    }

    @After
    public void cleanUp(){
        CommonFlightsManager.INSTANCE.resetFlightsManager();
        // Clear onboarding session-correlation SharedPreferences to keep tests isolated;
        // OnboardingTelemetryRecorder.addBlockingError persists to this store.
        if (mContext != null) {
            new com.microsoft.identity.common.internal.telemetry.OnboardingSessionCorrelationStore(mContext)
                    .save("");
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUrlOverrideHandlesEmptyString() {
        assertTrue(mWebViewClient.shouldOverrideUrlLoading(mMockWebView, ""));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUrlOverrideHandlesNullString() {
        assertTrue(mWebViewClient.shouldOverrideUrlLoading(mMockWebView, (String) null));
    }

    @Test
    public void testUrlOverrideHandlesPkeyAuthUrl() {
        assertTrue(mWebViewClient.shouldOverrideUrlLoading(mMockWebView, TEST_PKEY_AUTH_URL));
    }

    @Test
    public void testUrlOverrideHandlesWebsiteRequestUrl() {
        assertTrue(mWebViewClient.shouldOverrideUrlLoading(mMockWebView, TEST_WEBSITE_REQUEST_URL));
        assertTrue(mWebViewClient.shouldOverrideUrlLoading(mMockWebView, TEST_BROWSER_DEVICE_CA_URL_QUERY_STRING_PARAMETER));
        assertTrue(mWebViewClient.shouldOverrideUrlLoading(mMockWebView, TEST_PLAYSTORE_REDIRECT_WITH_BROWSER_PROTOCOL));
    }

    @Test
    public void testUrlOverrideHandlesOpenIdVcUrl() {
        assertTrue(mWebViewClient.shouldOverrideUrlLoading(mMockWebView, TEST_OPENID_VC_URL));
    }

    @Test
    @Config(shadows = {ShadowProcessUtil.class})
    public void testOpenIdVcUrl_TrustedAuthenticator_AttachesReturnPendingIntent() {
        // Arrange: the return-to-caller flight is on, and Microsoft Authenticator is the resolved handler...
        enableOpenIdVcReturnToCallerFlight();
        registerOpenIdVcHandler(AuthenticationConstants.Broker.AZURE_AUTHENTICATOR_APP_PACKAGE_NAME);

        // ...and it passes BrokerValidator signature-pinning.
        try (final MockedConstruction<BrokerValidator> ignored = mockConstruction(
                BrokerValidator.class,
                (mock, ctx) -> when(mock.isValidBrokerPackage(anyString())).thenReturn(true))) {
            final boolean result = mWebViewClient.shouldOverrideUrlLoading(mMockWebView, TEST_OPENID_VC_URL);
            assertTrue("shouldOverrideUrlLoading must return true for openid-vc:// URLs", result);
        }

        // Assert: the launch intent is pinned to Authenticator and carries the return PendingIntent.
        final Intent started = Shadows.shadowOf(mActivity).getNextStartedActivity();
        assertNotNull("Expected the openid-vc handler to be started", started);
        assertEquals("Launch intent must be pinned to Authenticator",
                AuthenticationConstants.Broker.AZURE_AUTHENTICATOR_APP_PACKAGE_NAME, started.getPackage());
        assertTrue("Trusted wallet must receive the return-to-caller PendingIntent",
                started.hasExtra(OpenIdVcReturnActivity.RETURN_PENDING_INTENT_EXTRA));
    }

    @Test
    @Config(shadows = {ShadowProcessUtil.class})
    public void testOpenIdVcUrl_UntrustedHandler_DoesNotAttachReturnPendingIntent() {
        // Arrange: the return-to-caller flight is on, but a non-Authenticator app claims the scheme.
        enableOpenIdVcReturnToCallerFlight();
        registerOpenIdVcHandler("com.example.malicious");

        // Act: the untrusted package fails the Authenticator check before BrokerValidator is even consulted.
        final boolean result = mWebViewClient.shouldOverrideUrlLoading(mMockWebView, TEST_OPENID_VC_URL);
        assertTrue("shouldOverrideUrlLoading must return true for openid-vc:// URLs", result);

        // Assert: the handler is still launched (existing dispatch behavior) but WITHOUT the return PendingIntent.
        final Intent started = Shadows.shadowOf(mActivity).getNextStartedActivity();
        assertNotNull("Expected the openid-vc handler to be started", started);
        assertFalse("Untrusted handler must NOT receive the return-to-caller PendingIntent",
                started.hasExtra(OpenIdVcReturnActivity.RETURN_PENDING_INTENT_EXTRA));
    }

    @Test
    @Config(shadows = {ShadowProcessUtil.class})
    public void testOpenIdVcUrl_AuthenticatorFailsSignatureVerification_DoesNotPinOrAttach() {
        // Arrange: the return-to-caller flight is on and the resolved handler IS Microsoft
        // Authenticator by package name, but it FAILS BrokerValidator signature verification -
        // e.g. a re-signed / repackaged look-alike claiming com.azure.authenticator. The signature
        // gate (not just the package-name check) must prevent both package-pinning and attaching.
        enableOpenIdVcReturnToCallerFlight();
        registerOpenIdVcHandler(AuthenticationConstants.Broker.AZURE_AUTHENTICATOR_APP_PACKAGE_NAME);

        try (final MockedConstruction<BrokerValidator> ignored = mockConstruction(
                BrokerValidator.class,
                (mock, ctx) -> when(mock.isValidBrokerPackage(anyString())).thenReturn(false))) {
            final boolean result = mWebViewClient.shouldOverrideUrlLoading(mMockWebView, TEST_OPENID_VC_URL);
            assertTrue("shouldOverrideUrlLoading must return true for openid-vc:// URLs", result);
        }

        // Assert: the handler is still launched, but the intent is neither pinned to Authenticator
        // nor carries the return PendingIntent - the signature gate rejected it.
        final Intent started = Shadows.shadowOf(mActivity).getNextStartedActivity();
        assertNotNull("Expected the openid-vc handler to be started", started);
        assertNotEquals("Signature-failing Authenticator must NOT be package-pinned",
                AuthenticationConstants.Broker.AZURE_AUTHENTICATOR_APP_PACKAGE_NAME, started.getPackage());
        assertFalse("Signature-failing Authenticator must NOT receive the return-to-caller PendingIntent",
                started.hasExtra(OpenIdVcReturnActivity.RETURN_PENDING_INTENT_EXTRA));
    }

    @Test
    @Config(shadows = {ShadowProcessUtil.class})
    public void testOpenIdVcUrl_MultipleHandlers_PinsToAuthenticatorAndAttaches() {
        // Arrange: the return-to-caller flight is on, and TWO apps claim openid-vc:// - a
        // third-party wallet and Microsoft Authenticator. resolveActivity() would return the
        // system chooser here; the implementation must instead target Authenticator directly.
        enableOpenIdVcReturnToCallerFlight();
        registerOpenIdVcHandler("com.example.otherwallet");
        registerOpenIdVcHandler(AuthenticationConstants.Broker.AZURE_AUTHENTICATOR_APP_PACKAGE_NAME);

        try (final MockedConstruction<BrokerValidator> ignored = mockConstruction(
                BrokerValidator.class,
                (mock, ctx) -> when(mock.isValidBrokerPackage(anyString())).thenReturn(true))) {
            final boolean result = mWebViewClient.shouldOverrideUrlLoading(mMockWebView, TEST_OPENID_VC_URL);
            assertTrue("shouldOverrideUrlLoading must return true for openid-vc:// URLs", result);
        }

        // Assert: no chooser - the launch is pinned to Authenticator and carries the return PendingIntent.
        final Intent started = Shadows.shadowOf(mActivity).getNextStartedActivity();
        assertNotNull("Expected the openid-vc handler to be started", started);
        assertEquals("Multi-handler launch must be pinned to Authenticator (no chooser)",
                AuthenticationConstants.Broker.AZURE_AUTHENTICATOR_APP_PACKAGE_NAME, started.getPackage());
        assertTrue("Pinned Authenticator must receive the return PendingIntent",
                started.hasExtra(OpenIdVcReturnActivity.RETURN_PENDING_INTENT_EXTRA));
    }

    @Test
    public void testOpenIdVcUrl_BrokerlessHost_DoesNotAttachReturnPendingIntent() {
        // Arrange: return-to-caller flight is on and Microsoft Authenticator is the verified
        // handler, but this WebView is NOT hosted in the broker's auth-service process (brokerless
        // / embedded case - no ShadowProcessUtil applied, so ProcessUtil.isRunningOnAuthService is
        // false). Return-to-caller must be wired only for the brokered flow.
        enableOpenIdVcReturnToCallerFlight();
        registerOpenIdVcHandler(AuthenticationConstants.Broker.AZURE_AUTHENTICATOR_APP_PACKAGE_NAME);

        try (final MockedConstruction<BrokerValidator> ignored = mockConstruction(
                BrokerValidator.class,
                (mock, ctx) -> when(mock.isValidBrokerPackage(anyString())).thenReturn(true))) {
            final boolean result = mWebViewClient.shouldOverrideUrlLoading(mMockWebView, TEST_OPENID_VC_URL);
            assertTrue("shouldOverrideUrlLoading must return true for openid-vc:// URLs", result);
        }

        // Assert: brokerless host falls back to the pre-existing dispatch - no return PendingIntent.
        final Intent started = Shadows.shadowOf(mActivity).getNextStartedActivity();
        assertNotNull("Expected the openid-vc handler to be started", started);
        assertFalse("Brokerless host must NOT attach the return-to-caller PendingIntent",
                started.hasExtra(OpenIdVcReturnActivity.RETURN_PENDING_INTENT_EXTRA));
    }

    private void registerOpenIdVcHandler(final String packageName) {
        final ResolveInfo resolveInfo = new ResolveInfo();
        resolveInfo.activityInfo = new ActivityInfo();
        resolveInfo.activityInfo.packageName = packageName;
        resolveInfo.activityInfo.name = packageName + ".OpenIdVcActivity";
        final ShadowPackageManager shadowPackageManager = Shadows.shadowOf(mContext.getPackageManager());
        // Register for the implicit intent (used by queryIntentActivities to discover handlers) and
        // for the package-pinned intent (used by resolveActivity after the launch is pinned to a
        // specific handler, which is how the production code resolves the final intent).
        shadowPackageManager.addResolveInfoForIntent(
                new Intent(Intent.ACTION_VIEW, Uri.parse(TEST_OPENID_VC_URL)), resolveInfo);
        shadowPackageManager.addResolveInfoForIntent(
                new Intent(Intent.ACTION_VIEW, Uri.parse(TEST_OPENID_VC_URL)).setPackage(packageName), resolveInfo);
    }

    @Test
    @Config(shadows = {ShadowProcessUtil.class})
    public void testOpenIdVcUrl_ReturnToCallerFlightDisabled_DoesNotAttachReturnPendingIntent() {
        // Arrange: openid-vc redirect handling is on, but the return-to-caller flight is OFF.
        final IFlightsProvider mockFlightsProvider = Mockito.mock(IFlightsProvider.class);
        when(mockFlightsProvider.isFlightEnabled(CommonFlight.ENABLE_OPEN_ID_VC_REDIRECT)).thenReturn(true);
        when(mockFlightsProvider.isFlightEnabled(CommonFlight.ENABLE_OPEN_ID_VC_RETURN_TO_CALLER)).thenReturn(false);
        final MockCommonFlightsManager mockCommonFlightsManager = new MockCommonFlightsManager();
        mockCommonFlightsManager.setMockCommonFlightsProvider(mockFlightsProvider);
        CommonFlightsManager.INSTANCE.initializeCommonFlightsManager(mockCommonFlightsManager);

        // Even with Authenticator as the resolved handler, the flight being off means no attach.
        registerOpenIdVcHandler(AuthenticationConstants.Broker.AZURE_AUTHENTICATOR_APP_PACKAGE_NAME);

        final boolean result = mWebViewClient.shouldOverrideUrlLoading(mMockWebView, TEST_OPENID_VC_URL);
        assertTrue("shouldOverrideUrlLoading must return true for openid-vc:// URLs", result);

        final Intent started = Shadows.shadowOf(mActivity).getNextStartedActivity();
        assertNotNull("Expected the openid-vc handler to be started", started);
        assertFalse("Return-to-caller flight OFF must not attach the return PendingIntent",
                started.hasExtra(OpenIdVcReturnActivity.RETURN_PENDING_INTENT_EXTRA));
    }

    private void enableOpenIdVcReturnToCallerFlight() {
        final IFlightsProvider mockFlightsProvider = Mockito.mock(IFlightsProvider.class);
        when(mockFlightsProvider.isFlightEnabled(CommonFlight.ENABLE_OPEN_ID_VC_REDIRECT)).thenReturn(true);
        when(mockFlightsProvider.isFlightEnabled(CommonFlight.ENABLE_OPEN_ID_VC_RETURN_TO_CALLER)).thenReturn(true);
        final MockCommonFlightsManager mockCommonFlightsManager = new MockCommonFlightsManager();
        mockCommonFlightsManager.setMockCommonFlightsProvider(mockFlightsProvider);
        CommonFlightsManager.INSTANCE.initializeCommonFlightsManager(mockCommonFlightsManager);
    }

    @Test
    public void testOpenIdVcUrl_StopsWebViewAndReturnsError_WhenNoHandlerFound() {
        // Arrange
        final IAuthorizationCompletionCallback mockCallback = Mockito.mock(IAuthorizationCompletionCallback.class);
        final ArgumentCaptor<RawAuthorizationResult> resultCaptor = ArgumentCaptor.forClass(RawAuthorizationResult.class);
        final AzureActiveDirectoryWebViewClient webViewClient = new AzureActiveDirectoryWebViewClient(
                mActivity,
                mockCallback,
                url -> {},
                TEST_REDIRECT_URI,
                Mockito.mock(SwitchBrowserProtocolCoordinator.class),
                "homeTenantId",
                false
        );
        final WebView mockWebView = Mockito.mock(WebView.class);

        // Act - Robolectric has no handler registered for openid-vc://, so the no-handler path executes
        final boolean result = webViewClient.shouldOverrideUrlLoading(mockWebView, TEST_OPENID_VC_URL);

        // Assert
        assertTrue("shouldOverrideUrlLoading must return true for openid-vc:// URLs", result);
        Mockito.verify(mockWebView).stopLoading();

        // Verify the callback received an error result
        Mockito.verify(mockCallback).onChallengeResponseReceived(resultCaptor.capture());
        final RawAuthorizationResult capturedResult = resultCaptor.getValue();
        assertEquals("Expected ACTIVITY_NOT_FOUND error code",
                ErrorStrings.ACTIVITY_NOT_FOUND,
                ((ClientException) capturedResult.getException()).getErrorCode());
        assertTrue("Expected error message about no application found",
                capturedResult.getException().getMessage().contains("No application found"));
    }

    @Test
    public void testUrlOverrideHandlesOpenIdVcUrl_FlightDisabled() {
        // When the flight is disabled, the openid-vc:// URL bypasses the VC handler
        // and is caught by the SSL protection check instead (non-https URL).
        final IAuthorizationCompletionCallback mockCallback = Mockito.mock(IAuthorizationCompletionCallback.class);
        final ArgumentCaptor<RawAuthorizationResult> resultCaptor = ArgumentCaptor.forClass(RawAuthorizationResult.class);
        final AzureActiveDirectoryWebViewClient webViewClient = new AzureActiveDirectoryWebViewClient(
                mActivity,
                mockCallback,
                url -> {},
                TEST_REDIRECT_URI,
                Mockito.mock(SwitchBrowserProtocolCoordinator.class),
                "homeTenantId",
                false
        );
        final WebView mockWebView = Mockito.mock(WebView.class);

        final IFlightsProvider mockFlightsProvider = Mockito.mock(IFlightsProvider.class);
        when(mockFlightsProvider.isFlightEnabled(CommonFlight.ENABLE_OPEN_ID_VC_REDIRECT)).thenReturn(false);

        final MockCommonFlightsManager mockCommonFlightsManager = new MockCommonFlightsManager();
        mockCommonFlightsManager.setMockCommonFlightsProvider(mockFlightsProvider);
        CommonFlightsManager.INSTANCE.initializeCommonFlightsManager(mockCommonFlightsManager);

        final boolean result = webViewClient.shouldOverrideUrlLoading(mockWebView, TEST_OPENID_VC_URL);

        assertTrue("shouldOverrideUrlLoading must return true (intercepted by SSL check)", result);
        Mockito.verify(mockWebView).stopLoading();

        // Verify the error is SSL protection, NOT the VC-specific ACTIVITY_NOT_FOUND.
        Mockito.verify(mockCallback).onChallengeResponseReceived(resultCaptor.capture());
        final RawAuthorizationResult capturedResult = resultCaptor.getValue();
        assertEquals("Expected SSL protection error, not VC handler error",
                ErrorStrings.WEBVIEW_REDIRECTURL_NOT_SSL_PROTECTED,
                ((ClientException) capturedResult.getException()).getErrorCode());

        CommonFlightsManager.INSTANCE.resetFlightsManager();
    }

    @Test
    @Config(shadows = {
            ShadowProcessUtil.class})
    public void testUrlOverrideHandlesHttpsDeviceCARequestUrl() {
        final IFlightsProvider mockFlightsProvider = Mockito.mock(IFlightsProvider.class);
        when(mockFlightsProvider.isFlightEnabled(CommonFlight.ENABLE_WEB_CP_IN_WEBVIEW)).thenReturn(true);

        final MockCommonFlightsManager mockCommonFlightsManager = new MockCommonFlightsManager();
        mockCommonFlightsManager.setMockCommonFlightsProvider(mockFlightsProvider);
        CommonFlightsManager.INSTANCE.initializeCommonFlightsManager(mockCommonFlightsManager);
        assertTrue(mWebViewClient.shouldOverrideUrlLoading(mMockWebView, TEST_HTTPS_DEVICE_CA_URL_QUERY_STRING_PARAMETER));
        CommonFlightsManager.INSTANCE.resetFlightsManager();
    }

    @Test
    @Config(shadows = {
            ShadowProcessUtil.class})
    public void testUrlHandlesHttpsDeviceCARequestUrlFlightOff() {
        final IFlightsProvider mockFlightsProvider = Mockito.mock(IFlightsProvider.class);
        when(mockFlightsProvider.isFlightEnabled(CommonFlight.ENABLE_WEB_CP_IN_WEBVIEW)).thenReturn(false);

        final MockCommonFlightsManager mockCommonFlightsManager = new MockCommonFlightsManager();
        mockCommonFlightsManager.setMockCommonFlightsProvider(mockFlightsProvider);
        CommonFlightsManager.INSTANCE.initializeCommonFlightsManager(mockCommonFlightsManager);
        assertFalse(mWebViewClient.shouldOverrideUrlLoading(mMockWebView, TEST_HTTPS_DEVICE_CA_URL_QUERY_STRING_PARAMETER));
        CommonFlightsManager.INSTANCE.resetFlightsManager();
    }

    @Test
    public void testUrlOverrideHandlesInstallRequest() {
        assertTrue(mWebViewClient.shouldOverrideUrlLoading(mMockWebView, TEST_INSTALL_REQUEST_URL));
    }

    @Test
    public void testUrlOverrideHandlesPlayStoreRequest() {
        final IFlightsProvider mockFlightsProvider = Mockito.mock(IFlightsProvider.class);
        when(mockFlightsProvider.isFlightEnabled(CommonFlight.ENABLE_PLAYSTORE_URL_LAUNCH)).thenReturn(true);

        final MockCommonFlightsManager mockCommonFlightsManager = new MockCommonFlightsManager();
        mockCommonFlightsManager.setMockCommonFlightsProvider(mockFlightsProvider);
        CommonFlightsManager.INSTANCE.initializeCommonFlightsManager(mockCommonFlightsManager);
        assertTrue(mWebViewClient.shouldOverrideUrlLoading(mMockWebView, TEST_PLAYSTORE_FOR_BROKER_APP_URL));
        CommonFlightsManager.INSTANCE.resetFlightsManager();
    }

    @Test
    public void testUrlOverrideHandlesDeviceRegistrationRequest() {
        assertTrue(mWebViewClient.shouldOverrideUrlLoading(mMockWebView, TEST_DEVICE_REGISTRATION_URL));
    }

    @Test
    public void testUrlOverrideHandlesWebCpUrl() {
        assertTrue(mWebViewClient.shouldOverrideUrlLoading(mMockWebView, TEST_WEB_CP_URL));
    }

    /**
     * A legitimate msauth:// redirect (auth code in the query string) must be delivered through
     * the redirect path (processRedirectUrl), not the broker-install fallthrough.
     * <p>
     * Asserting only that shouldOverrideUrlLoading() returns true is insufficient: any msauth://
     * URL is also matched by isInstallRequestUrl, so even a broken isRedirectUrl would return true
     * via processInstallRequest. We therefore pin the redirect path — the completion callback
     * receives a COMPLETED result AND the onboarding recorder is not marked with the broker-install
     * step that processInstallRequest would record.
     */
    @Test
    public void testUrlOverrideHandlesRedirectUriString() throws ClientException, org.json.JSONException {
        final IAuthorizationCompletionCallback mockCallback =
                Mockito.mock(IAuthorizationCompletionCallback.class);
        final ArgumentCaptor<RawAuthorizationResult> captor =
                ArgumentCaptor.forClass(RawAuthorizationResult.class);
        final AzureActiveDirectoryWebViewClient client =
                buildClientWithRedirectUri(mockCallback, TEST_REDIRECT_URI);
        final OnboardingTelemetryRecorder recorder = newOnboardingRecorder();
        client.setOnboardingTelemetryRecorder(recorder);
        final WebView mockWebView = Mockito.mock(WebView.class);

        final boolean handled = client.shouldOverrideUrlLoading(mockWebView, TEST_REDIRECT_URL);

        assertTrue("msauth redirect must be handled", handled);
        // Delivered as a completed auth result...
        Mockito.verify(mockCallback).onChallengeResponseReceived(captor.capture());
        assertEquals(RawAuthorizationResult.ResultCode.COMPLETED, captor.getValue().getResultCode());
        // ...through the redirect path, not processInstallRequest (which would have recorded the
        // broker-install onboarding step).
        assertFalse("msauth redirect must not be handled via the broker-install path",
                onboardingHasBrokerInstallStep(recorder));
    }

    /**
     * Regression test for the switch_browser routing gap that strict redirect matching would
     * otherwise introduce (raised in PR #3136 review). A switch_browser request arrives as
     * {redirectUrl}/switch_browser?code=...&action_uri=..., whose path no longer equals the
     * registered redirect URI. It must still be routed to the switch_browser handler and must NOT
     * fall through to processRedirectUrl/processInstallRequest, which would deliver the
     * switch_browser continuation code to the completion callback as if it were the final auth code.
     */
    @Test
    public void testSwitchBrowserRequest_isRoutedToSwitchBrowser_notRedirectOrInstall()
            throws ClientException, org.json.JSONException {
        final SwitchBrowserProtocolCoordinator mockCoordinator =
                Mockito.mock(SwitchBrowserProtocolCoordinator.class);
        final String switchBrowserUrl = TEST_REDIRECT_URI
                + "/switch_browser?code=sb_code&action_uri=https://login.microsoftonline.com/x";
        when(mockCoordinator.isSwitchBrowserRequest(switchBrowserUrl, TEST_REDIRECT_URI))
                .thenReturn(true);

        final IAuthorizationCompletionCallback mockCallback =
                Mockito.mock(IAuthorizationCompletionCallback.class);
        final AzureActiveDirectoryWebViewClient client = new AzureActiveDirectoryWebViewClient(
                mActivity, mockCallback, url -> { }, TEST_REDIRECT_URI, mockCoordinator, "homeTenantId", false);
        final OnboardingTelemetryRecorder recorder = newOnboardingRecorder();
        client.setOnboardingTelemetryRecorder(recorder);
        final WebView mockWebView = Mockito.mock(WebView.class);

        final boolean handled = client.shouldOverrideUrlLoading(mockWebView, switchBrowserUrl);

        assertTrue("switch_browser request must be handled", handled);
        // Routed to the switch_browser handler...
        Mockito.verify(mockCoordinator).processSwitchBrowserRedirectAsync(
                Mockito.eq(switchBrowserUrl), Mockito.any(), Mockito.eq(TEST_REDIRECT_URI));
        // ...and never delivered as a final auth result nor recorded as a broker-install step.
        Mockito.verify(mockCallback, Mockito.never()).onChallengeResponseReceived(Mockito.any());
        assertFalse("switch_browser must not be handled via the broker-install path",
                onboardingHasBrokerInstallStep(recorder));
    }

    /**
     * URL with auth code in the fragment instead of query string is still a
     * legitimate redirect — only scheme, authority and path are matched.
     */
    @Test
    public void testUrlOverrideHandlesRedirectUriWithFragment() {
        assertTrue(mWebViewClient.shouldOverrideUrlLoading(mMockWebView, TEST_REDIRECT_URL_WITH_FRAGMENT));
    }

    /**
     * Positive control for the strict-matching change: a legitimate redirect to
     * the configured https redirect URI (auth code in the query string) is still
     * recognized and delivered to the completion callback.
     */
    @Test
    public void testStrictMatching_acceptsLegitimateHttpsRedirect() throws ClientException {
        final IAuthorizationCompletionCallback mockCallback =
                Mockito.mock(IAuthorizationCompletionCallback.class);
        final AzureActiveDirectoryWebViewClient client =
                buildClientWithRedirectUri(mockCallback, HTTPS_REDIRECT_URI);
        final WebView mockWebView = Mockito.mock(WebView.class);

        final boolean handled = client.shouldOverrideUrlLoading(mockWebView, HTTPS_REDIRECT_LEGIT);

        assertTrue("Legitimate redirect must be handled", handled);
        // The auth code is delivered exactly once via the redirect path.
        Mockito.verify(mockCallback, Mockito.times(1))
                .onChallengeResponseReceived(Mockito.any());
    }

    /**
     * A redirect that differs from the registered URI only by a single trailing
     * slash on the path (registered "/auth" vs incoming "/auth/") denotes the
     * same resource and must still be accepted. normalizePath() collapses the
     * single trailing slash before comparison.
     */
    @Test
    public void testStrictMatching_acceptsTrailingSlashPathDifference() throws ClientException {
        final IAuthorizationCompletionCallback mockCallback =
                Mockito.mock(IAuthorizationCompletionCallback.class);
        final AzureActiveDirectoryWebViewClient client =
                buildClientWithRedirectUri(mockCallback, HTTPS_REDIRECT_URI);
        final WebView mockWebView = Mockito.mock(WebView.class);

        final boolean handled = client.shouldOverrideUrlLoading(mockWebView, HTTPS_REDIRECT_TRAILING_SLASH);

        assertTrue("Trailing-slash redirect must be handled", handled);
        Mockito.verify(mockCallback, Mockito.times(1))
                .onChallengeResponseReceived(Mockito.any());
    }

    /**
     * A path-less registered redirect URI (no path component) must still match an incoming
     * redirect that carries a root "/" before the query string. The configured empty path and
     * the incoming "/" normalize to the same value, so the auth code is delivered via the
     * redirect path. Guards against normalizePath rejecting the "" vs "/" difference.
     */
    @Test
    public void testStrictMatching_acceptsRootSlashForPathLessRedirect() throws ClientException {
        final IAuthorizationCompletionCallback mockCallback =
                Mockito.mock(IAuthorizationCompletionCallback.class);
        final AzureActiveDirectoryWebViewClient client =
                buildClientWithRedirectUri(mockCallback, HTTPS_REDIRECT_URI_NO_PATH);
        final WebView mockWebView = Mockito.mock(WebView.class);

        final boolean handled = client.shouldOverrideUrlLoading(mockWebView, HTTPS_REDIRECT_NO_PATH_ROOT_SLASH);

        assertTrue("Path-less redirect with root slash must be handled", handled);
        Mockito.verify(mockCallback, Mockito.times(1))
                .onChallengeResponseReceived(Mockito.any());
    }

    /**
     * Scheme-less registered redirect URI (defensive branch, unused in practice since all
     * redirect URIs carry a scheme): the incoming URL still carries the auth code in the query,
     * so the query/fragment must be stripped before the equality check — mirroring the scheme-less
     * branch of the Kotlin isSwitchBrowserRedirectUrl. Without stripping, the full url (with
     * ?code=...) would not equal the registered URI and the redirect would fall through to the
     * SSL-protection path instead of being delivered as a completed auth result.
     */
    @Test
    public void testStrictMatching_acceptsSchemelessRedirectStrippingQuery() throws ClientException {
        final IAuthorizationCompletionCallback mockCallback =
                Mockito.mock(IAuthorizationCompletionCallback.class);
        final ArgumentCaptor<RawAuthorizationResult> captor =
                ArgumentCaptor.forClass(RawAuthorizationResult.class);
        final AzureActiveDirectoryWebViewClient client =
                buildClientWithRedirectUri(mockCallback, SCHEMELESS_REDIRECT_URI);
        final WebView mockWebView = Mockito.mock(WebView.class);

        final boolean handled = client.shouldOverrideUrlLoading(mockWebView, SCHEMELESS_REDIRECT_LEGIT);

        assertTrue("Scheme-less redirect must be handled", handled);
        // Delivered via the redirect path as a completed auth result (not the SSL-protection path
        // that a non-match would take).
        Mockito.verify(mockCallback).onChallengeResponseReceived(captor.capture());
        assertEquals(RawAuthorizationResult.ResultCode.COMPLETED, captor.getValue().getResultCode());
    }

    /**
     * Opaque redirect URI (broker OOB, urn:ietf:wg:oauth:2.0:oob): a legitimate
     * redirect with the same scheme-specific part (auth code in query) is
     * accepted and delivered as a completed auth result.
     */
    @Test
    public void testStrictMatching_acceptsLegitimateOpaqueOobRedirect() throws ClientException {
        final IAuthorizationCompletionCallback mockCallback =
                Mockito.mock(IAuthorizationCompletionCallback.class);
        final ArgumentCaptor<RawAuthorizationResult> captor =
                ArgumentCaptor.forClass(RawAuthorizationResult.class);
        final AzureActiveDirectoryWebViewClient client =
                buildClientWithRedirectUri(mockCallback, OOB_REDIRECT_URI);
        final WebView mockWebView = Mockito.mock(WebView.class);

        final boolean handled = client.shouldOverrideUrlLoading(mockWebView, OOB_REDIRECT_LEGIT);

        assertTrue("Legitimate OOB redirect must be handled", handled);
        Mockito.verify(mockCallback).onChallengeResponseReceived(captor.capture());
        assertEquals(RawAuthorizationResult.ResultCode.COMPLETED, captor.getValue().getResultCode());
    }

    /**
     * Opaque redirect URI: an attacker-controlled urn with an extra suffix on the
     * scheme-specific part (urn:...:oobstolen) must NOT be accepted as the OOB
     * redirect. Without comparing the scheme-specific part, the null authority /
     * path would make this match on scheme alone. The auth code must never be
     * delivered as a completed result.
     */
    @Test
    public void testStrictMatching_rejectsSpoofedOpaqueOobRedirect() throws ClientException {
        final IAuthorizationCompletionCallback mockCallback =
                Mockito.mock(IAuthorizationCompletionCallback.class);
        final ArgumentCaptor<RawAuthorizationResult> captor =
                ArgumentCaptor.forClass(RawAuthorizationResult.class);
        final AzureActiveDirectoryWebViewClient client =
                buildClientWithRedirectUri(mockCallback, OOB_REDIRECT_URI);
        final WebView mockWebView = Mockito.mock(WebView.class);

        client.shouldOverrideUrlLoading(mockWebView, OOB_REDIRECT_SPOOFED_SSP_SUFFIX);

        // The spoofed urn is not the redirect; it falls through to the SSL-protection
        // error path, so any delivered result must NOT be a completed auth result.
        for (final RawAuthorizationResult result : captureAllResults(mockCallback, captor)) {
            assertNotEquals("Spoofed opaque redirect must not deliver an auth code",
                    RawAuthorizationResult.ResultCode.COMPLETED, result.getResultCode());
        }
    }

    /**
     * Opaque vs hierarchical mismatch: the registered redirect URI is opaque
     * (urn:ietf:wg:oauth:2.0:oob) but the incoming URL is a hierarchical urn
     * (urn://evil/oob). Comparing only the scheme would accept it; the matcher must
     * reject the mismatch so the auth code is never delivered as a completed result.
     */
    @Test
    public void testStrictMatching_rejectsHierarchicalUrnSpoofingOpaqueOob() throws ClientException {
        final IAuthorizationCompletionCallback mockCallback =
                Mockito.mock(IAuthorizationCompletionCallback.class);
        final ArgumentCaptor<RawAuthorizationResult> captor =
                ArgumentCaptor.forClass(RawAuthorizationResult.class);
        final AzureActiveDirectoryWebViewClient client =
                buildClientWithRedirectUri(mockCallback, OOB_REDIRECT_URI);
        final WebView mockWebView = Mockito.mock(WebView.class);

        client.shouldOverrideUrlLoading(mockWebView, OOB_REDIRECT_SPOOFED_HIERARCHICAL);

        for (final RawAuthorizationResult result : captureAllResults(mockCallback, captor)) {
            assertNotEquals("Hierarchical urn spoofing the opaque OOB redirect must not deliver an auth code",
                    RawAuthorizationResult.ResultCode.COMPLETED, result.getResultCode());
        }
    }

    private static java.util.List<RawAuthorizationResult> captureAllResults(
            final IAuthorizationCompletionCallback mockCallback,
            final ArgumentCaptor<RawAuthorizationResult> captor) {
        Mockito.verify(mockCallback, Mockito.atLeast(0)).onChallengeResponseReceived(captor.capture());
        return captor.getAllValues();
    }

    /**
     * Regression test for the FireWatch finding c1bf88bd-5fce-454c-a028-cbfe176639e0.
     * <p>
     * Historically {@code isRedirectUrl} used {@code String#startsWith}, so a URL
     * that contained the registered redirect URI as a prefix but had an
     * attacker-controlled suffix would be accepted as a redirect and the auth code
     * delivered to the completion callback. The strict scheme + authority + path
     * comparison must reject these, so the completion callback is never invoked
     * with the spoofed result.
     */
    @Test
    public void testStrictMatching_rejectsSpoofedRedirectWithSuffixHost() throws ClientException {
        assertSpoofedRedirectNotDelivered(HTTPS_REDIRECT_SPOOFED_SUFFIX_HOST);
    }

    @Test
    public void testStrictMatching_rejectsSpoofedRedirectWithPathSuffix() throws ClientException {
        assertSpoofedRedirectNotDelivered(HTTPS_REDIRECT_SPOOFED_PATH_SUFFIX);
    }

    private void assertSpoofedRedirectNotDelivered(@NonNull final String spoofedUrl)
            throws ClientException {
        final IAuthorizationCompletionCallback mockCallback =
                Mockito.mock(IAuthorizationCompletionCallback.class);
        final AzureActiveDirectoryWebViewClient client =
                buildClientWithRedirectUri(mockCallback, HTTPS_REDIRECT_URI);
        final WebView mockWebView = Mockito.mock(WebView.class);

        client.shouldOverrideUrlLoading(mockWebView, spoofedUrl);

        // The spoofed URL must NOT be treated as a redirect that delivers an auth code.
        Mockito.verify(mockCallback, Mockito.never())
                .onChallengeResponseReceived(Mockito.any());
    }

    /**
     * Kill-switch test: with ENABLE_STRICT_REDIRECT_URI_MATCHING disabled via ECS,
     * isRedirectUrl falls back to the historical prefix match, so the spoofed
     * suffix URL is (incorrectly) accepted again and its code delivered. This
     * proves the flag fully disables the new behavior, allowing a config-only
     * rollback.
     */
    @Test
    public void testKillSwitch_disablesStrictMatching_revertsToPrefixMatch() throws ClientException {
        final IFlightsProvider mockFlightsProvider = Mockito.mock(IFlightsProvider.class);
        when(mockFlightsProvider.isFlightEnabled(CommonFlight.ENABLE_STRICT_REDIRECT_URI_MATCHING))
                .thenReturn(false);
        final MockCommonFlightsManager mockCommonFlightsManager = new MockCommonFlightsManager();
        mockCommonFlightsManager.setMockCommonFlightsProvider(mockFlightsProvider);
        CommonFlightsManager.INSTANCE.initializeCommonFlightsManager(mockCommonFlightsManager);

        final IAuthorizationCompletionCallback mockCallback =
                Mockito.mock(IAuthorizationCompletionCallback.class);
        final AzureActiveDirectoryWebViewClient client =
                buildClientWithRedirectUri(mockCallback, HTTPS_REDIRECT_URI);
        final WebView mockWebView = Mockito.mock(WebView.class);

        client.shouldOverrideUrlLoading(mockWebView, HTTPS_REDIRECT_SPOOFED_PATH_SUFFIX);

        // Prefix match is back: spoofed URL is treated as a redirect and delivered.
        Mockito.verify(mockCallback, Mockito.times(1))
                .onChallengeResponseReceived(Mockito.any());
    }

    private AzureActiveDirectoryWebViewClient buildClientWithRedirectUri(
            @NonNull final IAuthorizationCompletionCallback completionCallback,
            @NonNull final String redirectUri) throws ClientException {
        return new AzureActiveDirectoryWebViewClient(
                mActivity,
                completionCallback,
                url -> { },
                redirectUri,
                Mockito.mock(SwitchBrowserProtocolCoordinator.class),
                "homeTenantId",
                false);
    }

    private OnboardingTelemetryRecorder newOnboardingRecorder() {
        final String seedJson = "{\"schema_version\":\"1.0.0\","
                + "\"session_correlation_id\":\"abc-123\","
                + "\"onboarding_mode\":\"non-brokered\"}";
        return new OnboardingTelemetryRecorder(seedJson, "client-id", "scope1", mContext);
    }

    /**
     * Reads the finalized onboarding blob and reports whether the broker-install step was recorded.
     * processInstallRequest records this step; processRedirectUrl and processSwitchBrowserRequest do
     * not, so its presence distinguishes which routing path handleUrl took.
     */
    private static boolean onboardingHasBrokerInstallStep(final OnboardingTelemetryRecorder recorder)
            throws org.json.JSONException {
        final org.json.JSONObject blob = new org.json.JSONObject(recorder.finalizeBlob());
        final org.json.JSONArray steps = blob.getJSONArray("steps_list");
        for (int i = 0; i < steps.length(); i++) {
            if (com.microsoft.identity.common.java.telemetry.OnboardingTelemetryConstants
                    .STEP_BROKER_INSTALL_PROMPTED.equals(steps.getJSONObject(i).getString("step_id"))) {
                return true;
            }
        }
        return false;
    }

    @Test
    public void testUrlOverrideHandlesPlayStoreRedirect() {
        assertTrue(mWebViewClient.shouldOverrideUrlLoading(mMockWebView, TEST_PLAY_STORE_INSTALL_AUTH_APP_URL));
        assertTrue(mWebViewClient.shouldOverrideUrlLoading(mMockWebView, TEST_PLAY_STORE_INSTALL_CP_URL));
        assertFalse(mWebViewClient.shouldOverrideUrlLoading(mMockWebView, TEST_PLAY_STORE_INSTALL_INVALID_APP));
    }

    @Test
    public void testUrlOverrideHandlesAuthAppMFAUrl() {
        assertTrue(mWebViewClient.shouldOverrideUrlLoading(mMockWebView, AUTHENTICATOR_MFA_LINKING_INVALID_URI));
    }

    @Test
    public void testUrlOverrideHandlesSSLProtectionCheck() {
        assertTrue(mWebViewClient.shouldOverrideUrlLoading(mMockWebView, TEST_SSL_PROTECTION_HTTP_URL));
        assertTrue(mWebViewClient.shouldOverrideUrlLoading(mMockWebView, TEST_SSL_PROTECTION_FTP_URL));
    }

    @Test
    public void testUrlOverrideHandlesBlankPageRequest() {
        assertTrue(mWebViewClient.shouldOverrideUrlLoading(mMockWebView, TEST_BLANK_PAGE_REQUEST_URL));
    }

    @Test
    public void testUrlOverrideHandlesInvalidUrl() {
        assertFalse(mWebViewClient.shouldOverrideUrlLoading(mMockWebView, TEST_INVALID_URL));
    }

    @Test
    public void testUrlOverrideHandlesHeaderForwardingRequiredUrl() {
        assertTrue(mWebViewClient.shouldOverrideUrlLoading(mMockWebView, TEST_MSA_HEADER_FORWARDING_POSITIVE_URL));
        assertFalse(mWebViewClient.shouldOverrideUrlLoading(mMockWebView, TEST_MSA_HEADER_FORWARDING_NEGATIVE_URL));
    }

    @Test
    public void testUrlOverrideHandlesNonceRedirectUrl() {
        assertTrue(mWebViewClient.shouldOverrideUrlLoading(mMockWebView, TEST_NONCE_REDIRECT_URL));
    }

    @Test
    public void testUrlOverrideHandlesCrossCloudRedirectUrl() {
        assertTrue(mWebViewClient.shouldOverrideUrlLoading(mMockWebView, TEST_CROSS_CLOUD_REDIRECT_URL));
    }

    // ---------------------------------------------------------------------------------------------
    // CWE-918: caller-side (catch(Throwable)) fallback gate in processNonceAndReAttachHeaders.
    //
    // The 7 NonceRedirectHandlerTest cases cover the handler's PRIMARY gate. The tests below cover
    // the MIRRORED gate that lives in this class: when NonceRedirectHandler.processChallenge throws
    // mid-processing, the catch(Throwable) fallback still navigates and must apply the same trust
    // check so the PRT credential header (x-ms-RefreshTokenCredential) is not forwarded to an
    // untrusted or cleartext host. NonceRedirectHandler construction is mocked so processChallenge
    // throws, deterministically forcing the fallback branch, and the header map handed to
    // WebView.loadUrl is captured to assert whether the credential survived.
    // ---------------------------------------------------------------------------------------------

    private static final String CWE918_TRUSTED_NONCE_HOST = "trusted.contoso.example";
    private static final String CWE918_UNTRUSTED_NONCE_HOST = "malicious.contoso.example";
    private static final String CWE918_NON_CREDENTIAL_HEADER_KEY = "x-ms-PasskeyProtocol";
    private static final String CWE918_NON_CREDENTIAL_HEADER_VALUE = "passkey-protocol-v1";
    private static final String CWE918_PRT_HEADER_VALUE = "original-aad-bound-prt-credential";

    /**
     * @param credentialHeaderValidationEnabled value returned for
     *                                          {@link CommonFlight#ENABLE_NONCE_REDIRECT_CREDENTIAL_HEADER_VALIDATION}.
     *                                          The attach-nonce feature flight is always stubbed on so
     *                                          the isNonceRedirect branch in handleUrl is reached.
     */
    private void installNonceRedirectFlights(final boolean credentialHeaderValidationEnabled) {
        final IFlightsProvider mockFlightsProvider = Mockito.mock(IFlightsProvider.class);
        when(mockFlightsProvider.isFlightEnabled(
                CommonFlight.ENABLE_ATTACH_NEW_PRT_HEADER_WHEN_NONCE_EXPIRED)).thenReturn(true);
        when(mockFlightsProvider.isFlightEnabled(
                CommonFlight.ENABLE_NONCE_REDIRECT_CREDENTIAL_HEADER_VALIDATION))
                .thenReturn(credentialHeaderValidationEnabled);
        final MockCommonFlightsManager mockCommonFlightsManager = new MockCommonFlightsManager();
        mockCommonFlightsManager.setMockCommonFlightsProvider(mockFlightsProvider);
        CommonFlightsManager.INSTANCE.initializeCommonFlightsManager(mockCommonFlightsManager);
    }

    // ===== PKeyAuth SubmitUrl same-origin: challenging-origin tracking (AB#3706623) =====
    // These tests pin the WebView-side derivation of the "challenging origin" that the factory
    // validates a PKeyAuth SubmitUrl against. They intercept the factory construction and capture the
    // second argument (the derived origin) rather than driving the full signing path, so they observe
    // exactly which navigation became the origin. They cover both the shouldOverrideUrlLoading path
    // and the onPageStarted path (the sole origin source pre-API-24 and for redirect targets that
    // never commit), with the origin-validation flight both on and off.

    /**
     * Turns on the {@link CommonFlight#ENABLE_PKEYAUTH_SUBMIT_URL_ORIGIN_VALIDATION} kill-switch so the
     * WebView-side origin recording/derivation added for AB#3706623 actually runs. {@code @After}
     * {@link #cleanUp()} resets the flights manager.
     */
    private void enablePKeyAuthOriginValidationFlight() {
        setPKeyAuthOriginValidationFlight(true);
    }

    /**
     * Turns the {@link CommonFlight#ENABLE_PKEYAUTH_SUBMIT_URL_ORIGIN_VALIDATION} kill-switch off so we
     * can assert the WebView-side origin recording/derivation is a complete no-op with the flight off.
     */
    private void disablePKeyAuthOriginValidationFlight() {
        setPKeyAuthOriginValidationFlight(false);
    }

    private void setPKeyAuthOriginValidationFlight(final boolean enabled) {
        final IFlightsProvider mockFlightsProvider = Mockito.mock(IFlightsProvider.class);
        when(mockFlightsProvider.isFlightEnabled(CommonFlight.ENABLE_PKEYAUTH_SUBMIT_URL_ORIGIN_VALIDATION))
                .thenReturn(enabled);
        final MockCommonFlightsManager mockCommonFlightsManager = new MockCommonFlightsManager();
        mockCommonFlightsManager.setMockCommonFlightsProvider(mockFlightsProvider);
        CommonFlightsManager.INSTANCE.initializeCommonFlightsManager(mockCommonFlightsManager);
    }

    private HashMap<String, String> nonceRequestHeadersWithPrt() {
        final HashMap<String, String> headers = new HashMap<>();
        headers.put(AuthenticationConstants.Broker.PRT_RESPONSE_HEADER, CWE918_PRT_HEADER_VALUE);
        headers.put(CWE918_NON_CREDENTIAL_HEADER_KEY, CWE918_NON_CREDENTIAL_HEADER_VALUE);
        return headers;
    }

    /**
     * Drives shouldOverrideUrlLoading with an sso_nonce redirect while forcing
     * NonceRedirectHandler.processChallenge to throw, and returns the header map that the
     * catch(Throwable) fallback passes to WebView.loadUrl for the given url.
     */
    private Map<String, String> captureFallbackLoadUrlHeaders(final String url) throws Exception {
        final WebView mockWebView = Mockito.mock(WebView.class);
        try (final MockedConstruction<NonceRedirectHandler> ignored = mockConstruction(
                NonceRedirectHandler.class,
                (mock, ctx) -> when(mock.processChallenge(any(URL.class)))
                        .thenThrow(new RuntimeException("forced failure to exercise catch(Throwable)")))) {
            mWebViewClient.shouldOverrideUrlLoading(mockWebView, url);
        }

        final ArgumentCaptor<Map> headersCaptor = ArgumentCaptor.forClass(Map.class);
        Mockito.verify(mockWebView).loadUrl(eq(url), headersCaptor.capture());
        //noinspection unchecked
        return headersCaptor.getValue();
    }

    @Test
    public void testNonceFallbackStripsPrtForUntrustedHostWhenFlightOn() throws Exception {
        installNonceRedirectFlights(true);
        mWebViewClient.setRequestHeaders(nonceRequestHeadersWithPrt());
        final String url = "https://" + CWE918_UNTRUSTED_NONCE_HOST + "/authorize?sso_nonce=ABCD";

        final Map<String, String> loadedHeaders = captureFallbackLoadUrlHeaders(url);

        assertFalse("PRT credential header must be stripped on the untrusted fallback path",
                loadedHeaders.containsKey(AuthenticationConstants.Broker.PRT_RESPONSE_HEADER));
        assertEquals("Non-credential headers must survive the strip",
                CWE918_NON_CREDENTIAL_HEADER_VALUE,
                loadedHeaders.get(CWE918_NON_CREDENTIAL_HEADER_KEY));
    }

    @Test
    public void testNonceFallbackForwardsPrtForUntrustedHostWhenFlightOff() throws Exception {
        installNonceRedirectFlights(false);
        mWebViewClient.setRequestHeaders(nonceRequestHeadersWithPrt());
        final String url = "http://" + CWE918_UNTRUSTED_NONCE_HOST + "/authorize?sso_nonce=ABCD";

        final Map<String, String> loadedHeaders = captureFallbackLoadUrlHeaders(url);

        // Kill-switch off is a complete revert to pre-fix behavior: the full header map, PRT included,
        // is forwarded even to an untrusted cleartext host. This proves the flight short-circuits the
        // trust check (the right operand of the || is never evaluated).
        assertEquals("Flight-off must forward the original PRT credential header unchanged",
                CWE918_PRT_HEADER_VALUE,
                loadedHeaders.get(AuthenticationConstants.Broker.PRT_RESPONSE_HEADER));
        assertEquals(CWE918_NON_CREDENTIAL_HEADER_VALUE,
                loadedHeaders.get(CWE918_NON_CREDENTIAL_HEADER_KEY));
    }

    @Test
    public void testNonceFallbackForwardsPrtForTrustedHostWhenFlightOn() throws Exception {
        // Seed a synthetic validated cloud host so isValidCloudHost runs for real (not mocked). A
        // test-only host is used deliberately: putCloud writes into the JVM-global sAadClouds, so a
        // real production host would stay validated for the rest of the module's tests.
        AzureActiveDirectory.putCloud(CWE918_TRUSTED_NONCE_HOST, new AzureActiveDirectoryCloud(true));
        installNonceRedirectFlights(true);
        mWebViewClient.setRequestHeaders(nonceRequestHeadersWithPrt());
        final String url = "https://" + CWE918_TRUSTED_NONCE_HOST + "/authorize?sso_nonce=ABCD";

        final Map<String, String> loadedHeaders = captureFallbackLoadUrlHeaders(url);

        assertEquals("Trusted HTTPS AAD host must keep the PRT credential header",
                CWE918_PRT_HEADER_VALUE,
                loadedHeaders.get(AuthenticationConstants.Broker.PRT_RESPONSE_HEADER));
        assertEquals(CWE918_NON_CREDENTIAL_HEADER_VALUE,
                loadedHeaders.get(CWE918_NON_CREDENTIAL_HEADER_KEY));
    }

    /**
     * CWE-918 (Finding A): the sso_nonce branch in handleUrl is evaluated before the SSL hard block,
     * and isNonceRedirect is a bare substring match, so a cleartext URL merely containing "sso_nonce"
     * used to take the nonce branch and never reach the SSL check. With enforcement on, a non-HTTPS
     * nonce URL must instead fall through every intermediate branch to processSSLProtectionCheck,
     * which hard-blocks it (stopLoading + WEBVIEW_REDIRECTURL_NOT_SSL_PROTECTED). This proves the
     * cleartext URL is not swallowed by any intermediate branch and never reaches the credential sink.
     */
    @Test
    public void testCleartextNonceUrlIsSslBlockedWhenFlightOn() {
        final IAuthorizationCompletionCallback mockCallback =
                Mockito.mock(IAuthorizationCompletionCallback.class);
        final ArgumentCaptor<RawAuthorizationResult> resultCaptor =
                ArgumentCaptor.forClass(RawAuthorizationResult.class);
        final AzureActiveDirectoryWebViewClient webViewClient = new AzureActiveDirectoryWebViewClient(
                mActivity,
                mockCallback,
                url -> {},
                TEST_REDIRECT_URI,
                Mockito.mock(SwitchBrowserProtocolCoordinator.class),
                "homeTenantId",
                false
        );
        final WebView mockWebView = Mockito.mock(WebView.class);
        installNonceRedirectFlights(true);
        final String url = "http://" + CWE918_UNTRUSTED_NONCE_HOST + "/authorize?sso_nonce=ABCD";

        final boolean result = webViewClient.shouldOverrideUrlLoading(mockWebView, url);

        assertTrue("shouldOverrideUrlLoading must return true (intercepted by SSL check)", result);
        // The nonce branch must NOT be taken: the cleartext URL falls through to the SSL hard block.
        Mockito.verify(mockWebView).stopLoading();
        Mockito.verify(mockCallback).onChallengeResponseReceived(resultCaptor.capture());
        assertEquals("Cleartext sso_nonce URL must be rejected by the SSL protection check",
                ErrorStrings.WEBVIEW_REDIRECTURL_NOT_SSL_PROTECTED,
                ((ClientException) resultCaptor.getValue().getException()).getErrorCode());
        // And it must never reach the credential-bearing loadUrl path.
        Mockito.verify(mockWebView, Mockito.never())
                .loadUrl(Mockito.anyString(), Mockito.anyMap());
    }

    private WebResourceRequest mockNavigationRequest(final String url, final boolean isForMainFrame) {
        final WebResourceRequest request = Mockito.mock(WebResourceRequest.class);
        when(request.getUrl()).thenReturn(Uri.parse(url));
        when(request.isForMainFrame()).thenReturn(isForMainFrame);
        return request;
    }

    /**
     * A main-frame https navigation that we defer to the WebView (returns {@code false}) is recorded as
     * the challenging origin, and a subsequent PKeyAuth challenge validates its SubmitUrl against that
     * recorded URL — taking precedence over {@link WebView#getUrl()}.
     */
    @Test
    public void testPKeyAuthOriginTracking_MainFrameHttpsNavigation_BecomesChallengingOrigin() throws ClientException {
        enablePKeyAuthOriginValidationFlight();
        final WebView mockWebView = Mockito.mock(WebView.class);
        when(mockWebView.getUrl()).thenReturn(FALLBACK_ORIGIN_URL);

        try (final MockedConstruction<PKeyAuthChallengeFactory> factoryCtor =
                     mockConstruction(PKeyAuthChallengeFactory.class);
             final MockedConstruction<PKeyAuthChallengeHandler> handlerCtor =
                     mockConstruction(PKeyAuthChallengeHandler.class)) {

            // Main-frame https navigation deferred to the WebView -> recorded as the challenging origin.
            assertFalse(mWebViewClient.shouldOverrideUrlLoading(
                    mockWebView, mockNavigationRequest(TEST_INVALID_URL, true)));

            // Subsequent PKeyAuth challenge derives its challenging origin from the recorded URL.
            assertTrue(mWebViewClient.shouldOverrideUrlLoading(
                    mockWebView, mockNavigationRequest(TEST_PKEY_AUTH_URL, true)));

            final PKeyAuthChallengeFactory factory = factoryCtor.constructed().get(0);
            final ArgumentCaptor<String> originCaptor = ArgumentCaptor.forClass(String.class);
            Mockito.verify(factory).getPKeyAuthChallengeFromWebViewRedirect(
                    eq(TEST_PKEY_AUTH_URL), originCaptor.capture());
            assertEquals("Recorded main-frame https URL must be the challenging origin",
                    TEST_INVALID_URL, originCaptor.getValue());
        }
    }

    /**
     * A subframe https navigation must NOT become the challenging origin (subframe poisoning guard).
     * With nothing recorded, the factory receives the {@link WebView#getUrl()} fallback instead.
     */
    @Test
    public void testPKeyAuthOriginTracking_SubframeHttpsNavigation_DoesNotBecomeOrigin() throws ClientException {
        enablePKeyAuthOriginValidationFlight();
        final WebView mockWebView = Mockito.mock(WebView.class);
        when(mockWebView.getUrl()).thenReturn(FALLBACK_ORIGIN_URL);

        try (final MockedConstruction<PKeyAuthChallengeFactory> factoryCtor =
                     mockConstruction(PKeyAuthChallengeFactory.class);
             final MockedConstruction<PKeyAuthChallengeHandler> handlerCtor =
                     mockConstruction(PKeyAuthChallengeHandler.class)) {

            // Subframe navigation (isForMainFrame == false) must not be recorded.
            mWebViewClient.shouldOverrideUrlLoading(
                    mockWebView, mockNavigationRequest(TEST_INVALID_URL, false));

            assertTrue(mWebViewClient.shouldOverrideUrlLoading(
                    mockWebView, mockNavigationRequest(TEST_PKEY_AUTH_URL, true)));

            final PKeyAuthChallengeFactory factory = factoryCtor.constructed().get(0);
            final ArgumentCaptor<String> originCaptor = ArgumentCaptor.forClass(String.class);
            Mockito.verify(factory).getPKeyAuthChallengeFromWebViewRedirect(
                    eq(TEST_PKEY_AUTH_URL), originCaptor.capture());
            assertEquals("Subframe navigation must not become the challenging origin; expected view.getUrl() fallback",
                    FALLBACK_ORIGIN_URL, originCaptor.getValue());
        }
    }

    /**
     * A main-frame https URL that we OVERRIDE (handled, returns {@code true}) must NOT be recorded,
     * because the WebView never actually loads it. Recording it would false-reject the next legitimate
     * challenge on the real page. Here the derived origin falls back to {@link WebView#getUrl()}.
     */
    @Test
    public void testPKeyAuthOriginTracking_OverriddenMainFrameUrl_DoesNotBecomeOrigin() throws ClientException {
        enablePKeyAuthOriginValidationFlight();
        final WebView mockWebView = Mockito.mock(WebView.class);
        when(mockWebView.getUrl()).thenReturn(FALLBACK_ORIGIN_URL);

        try (final MockedConstruction<PKeyAuthChallengeFactory> factoryCtor =
                     mockConstruction(PKeyAuthChallengeFactory.class);
             final MockedConstruction<PKeyAuthChallengeHandler> handlerCtor =
                     mockConstruction(PKeyAuthChallengeHandler.class)) {

            // Main-frame https URL we override (header-forwarding branch, returns true) -> not recorded.
            assertTrue(mWebViewClient.shouldOverrideUrlLoading(
                    mockWebView, mockNavigationRequest(TEST_MSA_HEADER_FORWARDING_POSITIVE_URL, true)));

            assertTrue(mWebViewClient.shouldOverrideUrlLoading(
                    mockWebView, mockNavigationRequest(TEST_PKEY_AUTH_URL, true)));

            final PKeyAuthChallengeFactory factory = factoryCtor.constructed().get(0);
            final ArgumentCaptor<String> originCaptor = ArgumentCaptor.forClass(String.class);
            Mockito.verify(factory).getPKeyAuthChallengeFromWebViewRedirect(
                    eq(TEST_PKEY_AUTH_URL), originCaptor.capture());
            assertEquals("An overridden (never-loaded) URL must not become the challenging origin; expected view.getUrl() fallback",
                    FALLBACK_ORIGIN_URL, originCaptor.getValue());
        }
    }

    /**
     * onPageStarted is the sole origin source on pre-API-24 devices (the deprecated String overload
     * of shouldOverrideUrlLoading carries no frame info) and for redirect targets that never commit
     * via shouldOverrideUrlLoading. An https URL delivered through onPageStarted must be recorded as
     * the challenging origin and take precedence over {@link WebView#getUrl()}.
     */
    @Test
    public void testPKeyAuthOriginTracking_OnPageStartedHttpsRedirect_BecomesChallengingOrigin() throws ClientException {
        enablePKeyAuthOriginValidationFlight();
        final WebView mockWebView = Mockito.mock(WebView.class);
        when(mockWebView.getUrl()).thenReturn(FALLBACK_ORIGIN_URL);

        try (final MockedConstruction<PKeyAuthChallengeFactory> factoryCtor =
                     mockConstruction(PKeyAuthChallengeFactory.class);
             final MockedConstruction<PKeyAuthChallengeHandler> handlerCtor =
                     mockConstruction(PKeyAuthChallengeHandler.class)) {

            // Redirect target delivered via onPageStarted (main-frame-only by Android contract).
            mWebViewClient.onPageStarted(mockWebView, TEST_INVALID_URL, null);

            assertTrue(mWebViewClient.shouldOverrideUrlLoading(
                    mockWebView, mockNavigationRequest(TEST_PKEY_AUTH_URL, true)));

            final PKeyAuthChallengeFactory factory = factoryCtor.constructed().get(0);
            final ArgumentCaptor<String> originCaptor = ArgumentCaptor.forClass(String.class);
            Mockito.verify(factory).getPKeyAuthChallengeFromWebViewRedirect(
                    eq(TEST_PKEY_AUTH_URL), originCaptor.capture());
            assertEquals("onPageStarted https URL must be recorded as the challenging origin",
                    TEST_INVALID_URL, originCaptor.getValue());
        }
    }

    /**
     * A non-https onPageStarted callback must NOT replace an already-recorded https origin (pins the
     * https-only recording from round 4). A cleartext detour cannot demote the trusted origin.
     */
    @Test
    public void testPKeyAuthOriginTracking_OnPageStartedNonHttps_DoesNotReplaceOrigin() throws ClientException {
        enablePKeyAuthOriginValidationFlight();
        final WebView mockWebView = Mockito.mock(WebView.class);
        when(mockWebView.getUrl()).thenReturn(FALLBACK_ORIGIN_URL);

        try (final MockedConstruction<PKeyAuthChallengeFactory> factoryCtor =
                     mockConstruction(PKeyAuthChallengeFactory.class);
             final MockedConstruction<PKeyAuthChallengeHandler> handlerCtor =
                     mockConstruction(PKeyAuthChallengeHandler.class)) {

            // First an https page is recorded, then a cleartext page must not overwrite it.
            mWebViewClient.onPageStarted(mockWebView, TEST_INVALID_URL, null);
            mWebViewClient.onPageStarted(mockWebView, TEST_SSL_PROTECTION_HTTP_URL, null);

            assertTrue(mWebViewClient.shouldOverrideUrlLoading(
                    mockWebView, mockNavigationRequest(TEST_PKEY_AUTH_URL, true)));

            final PKeyAuthChallengeFactory factory = factoryCtor.constructed().get(0);
            final ArgumentCaptor<String> originCaptor = ArgumentCaptor.forClass(String.class);
            Mockito.verify(factory).getPKeyAuthChallengeFromWebViewRedirect(
                    eq(TEST_PKEY_AUTH_URL), originCaptor.capture());
            assertEquals("A non-https onPageStarted must not replace the recorded https origin",
                    TEST_INVALID_URL, originCaptor.getValue());
        }
    }

    /**
     * With the flight off, onPageStarted recording and challenging-origin derivation are a complete
     * no-op: the factory receives {@code null} regardless of what onPageStarted saw.
     */
    @Test
    public void testPKeyAuthOriginTracking_OnPageStartedFlightOff_NoOp() throws ClientException {
        disablePKeyAuthOriginValidationFlight();
        final WebView mockWebView = Mockito.mock(WebView.class);
        when(mockWebView.getUrl()).thenReturn(FALLBACK_ORIGIN_URL);

        try (final MockedConstruction<PKeyAuthChallengeFactory> factoryCtor =
                     mockConstruction(PKeyAuthChallengeFactory.class);
             final MockedConstruction<PKeyAuthChallengeHandler> handlerCtor =
                     mockConstruction(PKeyAuthChallengeHandler.class)) {

            mWebViewClient.onPageStarted(mockWebView, TEST_INVALID_URL, null);

            assertTrue(mWebViewClient.shouldOverrideUrlLoading(
                    mockWebView, mockNavigationRequest(TEST_PKEY_AUTH_URL, true)));

            final PKeyAuthChallengeFactory factory = factoryCtor.constructed().get(0);
            final ArgumentCaptor<String> originCaptor = ArgumentCaptor.forClass(String.class);
            Mockito.verify(factory).getPKeyAuthChallengeFromWebViewRedirect(
                    eq(TEST_PKEY_AUTH_URL), originCaptor.capture());
            assertNull("With the flight off, no challenging origin is derived",
                    originCaptor.getValue());
        }
    }
    // ===== PKeyAuth SubmitUrl same-origin: navigation-context telemetry (AB#3706623, round 8) =====
    // The WebView client annotates the current span with two non-PII navigation-context attributes it
    // alone knows: whether the challenge is on the main frame, and where the challenging origin was
    // derived from (recorded https URL vs a view.getUrl() fallback vs none). These are set only when
    // the master flight is on. We mock SpanExtension.current() to capture the attributes.

    /**
     * A main-frame PKeyAuth challenge with a recorded https origin annotates the current span with
     * {@code pkeyauth_challenge_is_main_frame=true} and {@code pkeyauth_challenging_origin_source=recorded}.
     */
    @Test
    public void testPKeyAuthContextTelemetry_MainFrameRecordedOrigin_Emitted() {
        enablePKeyAuthOriginValidationFlight();
        final WebView mockWebView = Mockito.mock(WebView.class);
        when(mockWebView.getUrl()).thenReturn(FALLBACK_ORIGIN_URL);
        final Span mockSpan = Mockito.mock(Span.class);

        try (final MockedStatic<SpanExtension> spanExtension = Mockito.mockStatic(SpanExtension.class);
             final MockedConstruction<PKeyAuthChallengeFactory> factoryCtor =
                     mockConstruction(PKeyAuthChallengeFactory.class);
             final MockedConstruction<PKeyAuthChallengeHandler> handlerCtor =
                     mockConstruction(PKeyAuthChallengeHandler.class)) {
            spanExtension.when(SpanExtension::current).thenReturn(mockSpan);

            // Record an https main-frame origin, then dispatch a main-frame PKeyAuth challenge.
            mWebViewClient.onPageStarted(mockWebView, TEST_INVALID_URL, null);
            assertTrue(mWebViewClient.shouldOverrideUrlLoading(
                    mockWebView, mockNavigationRequest(TEST_PKEY_AUTH_URL, true)));

            Mockito.verify(mockSpan).setAttribute(
                    AttributeName.pkeyauth_challenge_is_main_frame.name(), true);
            Mockito.verify(mockSpan).setAttribute(
                    AttributeName.pkeyauth_challenging_origin_source.name(), "recorded");
        }
    }

    /**
     * A subframe PKeyAuth challenge with nothing recorded annotates the span with
     * {@code pkeyauth_challenge_is_main_frame=false} and {@code pkeyauth_challenging_origin_source=webview_url}
     * (the {@link WebView#getUrl()} fallback). Validation is not relaxed for subframes; only the
     * main-frame flag is recorded so a cross-origin iframe challenge can be measured.
     */
    @Test
    public void testPKeyAuthContextTelemetry_SubframeFallbackOrigin_Emitted() {
        enablePKeyAuthOriginValidationFlight();
        final WebView mockWebView = Mockito.mock(WebView.class);
        when(mockWebView.getUrl()).thenReturn(FALLBACK_ORIGIN_URL);
        final Span mockSpan = Mockito.mock(Span.class);

        try (final MockedStatic<SpanExtension> spanExtension = Mockito.mockStatic(SpanExtension.class);
             final MockedConstruction<PKeyAuthChallengeFactory> factoryCtor =
                     mockConstruction(PKeyAuthChallengeFactory.class);
             final MockedConstruction<PKeyAuthChallengeHandler> handlerCtor =
                     mockConstruction(PKeyAuthChallengeHandler.class)) {
            spanExtension.when(SpanExtension::current).thenReturn(mockSpan);

            assertTrue(mWebViewClient.shouldOverrideUrlLoading(
                    mockWebView, mockNavigationRequest(TEST_PKEY_AUTH_URL, false)));

            Mockito.verify(mockSpan).setAttribute(
                    AttributeName.pkeyauth_challenge_is_main_frame.name(), false);
            Mockito.verify(mockSpan).setAttribute(
                    AttributeName.pkeyauth_challenging_origin_source.name(), "webview_url");
        }
    }

    /**
     * With the master flight off, no navigation-context telemetry is emitted — the span is never
     * touched, matching the end-to-end no-op guarantee.
     */
    @Test
    public void testPKeyAuthContextTelemetry_FlightOff_NotEmitted() {
        disablePKeyAuthOriginValidationFlight();
        final WebView mockWebView = Mockito.mock(WebView.class);
        when(mockWebView.getUrl()).thenReturn(FALLBACK_ORIGIN_URL);
        final Span mockSpan = Mockito.mock(Span.class);

        try (final MockedStatic<SpanExtension> spanExtension = Mockito.mockStatic(SpanExtension.class);
             final MockedConstruction<PKeyAuthChallengeFactory> factoryCtor =
                     mockConstruction(PKeyAuthChallengeFactory.class);
             final MockedConstruction<PKeyAuthChallengeHandler> handlerCtor =
                     mockConstruction(PKeyAuthChallengeHandler.class)) {
            spanExtension.when(SpanExtension::current).thenReturn(mockSpan);

            assertTrue(mWebViewClient.shouldOverrideUrlLoading(
                    mockWebView, mockNavigationRequest(TEST_PKEY_AUTH_URL, true)));

            Mockito.verifyNoInteractions(mockSpan);
        }
    }


    @Test
    @Config(shadows = {
            ShadowProcessUtil.class})
    public void testUrlOverrideHandleWebCPEnrollmentUrlEnabled() {
        final AzureActiveDirectoryWebViewClient mockWebViewClient = Mockito.spy(mWebViewClient);
        final IFlightsProvider mockFlightsProvider = Mockito.mock(IFlightsProvider.class);
        when(mockFlightsProvider.isFlightEnabled(CommonFlight.ENABLE_WEB_CP_IN_WEBVIEW)).thenReturn(true);

        final MockCommonFlightsManager mockCommonFlightsManager = new MockCommonFlightsManager();
        mockCommonFlightsManager.setMockCommonFlightsProvider(mockFlightsProvider);
        CommonFlightsManager.INSTANCE.initializeCommonFlightsManager(mockCommonFlightsManager);

        assertTrue(mockWebViewClient.isWebCpInWebviewFeatureEnabled(TEST_WEB_CP_ENROLLMENT_URL));
        assertTrue(mockWebViewClient.shouldOverrideUrlLoading(mMockWebView, TEST_WEB_CP_ENROLLMENT_URL));
        CommonFlightsManager.INSTANCE.resetFlightsManager();
    }

    @Test
    @Config(shadows = {
            ShadowProcessUtil.class})
    public void testUrlOverrideHandleWebCPEnrollmentUrlDisabled() {
        final AzureActiveDirectoryWebViewClient mockWebViewClient = Mockito.spy(mWebViewClient);
        final IFlightsProvider mockFlightsProvider = Mockito.mock(IFlightsProvider.class);
        when(mockFlightsProvider.isFlightEnabled(CommonFlight.ENABLE_WEB_CP_IN_WEBVIEW)).thenReturn(false);

        final MockCommonFlightsManager mockCommonFlightsManager = new MockCommonFlightsManager();
        mockCommonFlightsManager.setMockCommonFlightsProvider(mockFlightsProvider);
        CommonFlightsManager.INSTANCE.initializeCommonFlightsManager(mockCommonFlightsManager);

        assertFalse(mockWebViewClient.isWebCpInWebviewFeatureEnabled(TEST_WEB_CP_ENROLLMENT_URL));
        assertFalse(mockWebViewClient.shouldOverrideUrlLoading(mMockWebView, TEST_WEB_CP_ENROLLMENT_URL));
    }

    @Test
    @Config(shadows = {
            ShadowProcessUtil.class})
    public void testLoadDeviceCaUrlInWebView() {
        // Mocks
        final WebView mockWebview = Mockito.mock(WebView.class);
        final AzureActiveDirectoryWebViewClient mockWebViewClient = Mockito.spy(mWebViewClient);
        final IFlightsProvider mockFlightsProvider = Mockito.mock(IFlightsProvider.class);
        when(mockFlightsProvider.isFlightEnabled(CommonFlight.ENABLE_WEB_CP_IN_WEBVIEW)).thenReturn(true);

        final MockCommonFlightsManager mockCommonFlightsManager = new MockCommonFlightsManager();
        mockCommonFlightsManager.setMockCommonFlightsProvider(mockFlightsProvider);
        CommonFlightsManager.INSTANCE.initializeCommonFlightsManager(mockCommonFlightsManager);
        // Actual call
        mockWebViewClient.loadDeviceCaUrl(TEST_BROWSER_DEVICE_CA_URL_QUERY_STRING_PARAMETER, mockWebview);
        // Verify
        Mockito.verify(mockWebview).loadUrl(Mockito.anyString(), Mockito.any());
    }

    @Test
    @Config(shadows = {
            ShadowProcessUtil.class})
    public void testLoadDeviceCaUrlInBrowser() {
        // Mocks
        final WebView mockWebview = Mockito.mock(WebView.class);
        final AzureActiveDirectoryWebViewClient mockWebViewClient = Mockito.spy(mWebViewClient);
        final IFlightsProvider mockFlightsProvider = Mockito.mock(IFlightsProvider.class);
        when(mockFlightsProvider.isFlightEnabled(CommonFlight.ENABLE_WEB_CP_IN_WEBVIEW)).thenReturn(false);

        final MockCommonFlightsManager mockCommonFlightsManager = new MockCommonFlightsManager();
        mockCommonFlightsManager.setMockCommonFlightsProvider(mockFlightsProvider);
        CommonFlightsManager.INSTANCE.initializeCommonFlightsManager(mockCommonFlightsManager);
        // Actual call
        mockWebViewClient.loadDeviceCaUrl(TEST_BROWSER_DEVICE_CA_URL_QUERY_STRING_PARAMETER, mockWebview);
        // Verify
        Mockito.verify(mockWebview, Mockito.never()).loadUrl(Mockito.anyString(), Mockito.any());
    }

    @Test
    public void testLoadDeviceCaUrlInBrowserInBrokelessFlow() {
        // Mocks
        final WebView mockWebview = Mockito.mock(WebView.class);
        final AzureActiveDirectoryWebViewClient mockWebViewClient = Mockito.spy(mWebViewClient);
        final IFlightsProvider mockFlightsProvider = Mockito.mock(IFlightsProvider.class);
        when(mockFlightsProvider.isFlightEnabled(CommonFlight.ENABLE_WEB_CP_IN_WEBVIEW)).thenReturn(true);

        final MockCommonFlightsManager mockCommonFlightsManager = new MockCommonFlightsManager();
        mockCommonFlightsManager.setMockCommonFlightsProvider(mockFlightsProvider);
        CommonFlightsManager.INSTANCE.initializeCommonFlightsManager(mockCommonFlightsManager);
        // Actual call
        mockWebViewClient.loadDeviceCaUrl(TEST_BROWSER_DEVICE_CA_URL_QUERY_STRING_PARAMETER, mockWebview);
        // Verify
        Mockito.verify(mockFlightsProvider, Mockito.never()).isFlightEnabled(Mockito.any());
        Mockito.verify(mockWebview, Mockito.never()).loadUrl(Mockito.anyString(), Mockito.any());
    }

    @Test
    public void testLoadDeviceCaUrlInWebviewInBrokelessFlow() {
        // Mocks
        final WebView mockWebview = Mockito.mock(WebView.class);
        final AzureActiveDirectoryWebViewClient mockWebViewClient  = new AzureActiveDirectoryWebViewClient(
                mActivity,
                new IAuthorizationCompletionCallback() {
                    @Override
                    public void onChallengeResponseReceived(@NonNull RawAuthorizationResult response) {

                    }

                    @Override
                    public void setPKeyAuthStatus(boolean status) {
                        return;
                    }
                },
                new OnPageLoadedCallback() {
                    @Override
                    public void onPageLoaded(final String url) {
                        return;
                    }
                },
                TEST_REDIRECT_URI,
                Mockito.mock(SwitchBrowserProtocolCoordinator.class),
                "homeTenantId",
                true);
        final IFlightsProvider mockFlightsProvider = Mockito.mock(IFlightsProvider.class);
        when(mockFlightsProvider.isFlightEnabled(CommonFlight.ENABLE_WEB_CP_IN_WEBVIEW)).thenReturn(true);

        final MockCommonFlightsManager mockCommonFlightsManager = new MockCommonFlightsManager();
        mockCommonFlightsManager.setMockCommonFlightsProvider(mockFlightsProvider);
        CommonFlightsManager.INSTANCE.initializeCommonFlightsManager(mockCommonFlightsManager);
        // Actual call
        mockWebViewClient.loadDeviceCaUrl(TEST_BROWSER_DEVICE_CA_URL_QUERY_STRING_PARAMETER, mockWebview);
        // Verify
        Mockito.verify(mockFlightsProvider, Mockito.never()).isFlightEnabled(Mockito.any());
        Mockito.verify(mockWebview).loadUrl(Mockito.anyString(), Mockito.any());
    }

    @Test
    public void testProcessCloudRedirectAndPrtHeaderInternalSuccess() {
        ReAttachPrtHeaderHandler mockCrossCloudChallengeHandler = Mockito.mock(ReAttachPrtHeaderHandler.class);
        try {
            mWebViewClient.reAttachPrtHeader(TEST_CROSS_CLOUD_REDIRECT_URL, mockCrossCloudChallengeHandler, mMockWebView, "methodTag", Span.current());
        } catch (Exception e) {
            Assert.fail("Unexpected exception occured " + e);
        }
    }

    @Test
    public void testProcessCloudRedirectAndPrtHeaderInternalException() {
        ReAttachPrtHeaderHandler mockReAttachPrtHandler = Mockito.mock(ReAttachPrtHeaderHandler.class);
        WebView mockWebView = Mockito.mock(WebView.class);
        Mockito.doThrow(new RuntimeException("Test Exception")).when(mockReAttachPrtHandler).processChallenge(TEST_CROSS_CLOUD_REDIRECT_URL);
        try {
            mWebViewClient.reAttachPrtHeader(TEST_CROSS_CLOUD_REDIRECT_URL, mockReAttachPrtHandler, mockWebView, "methodTag", Span.current());
            Mockito.verify(mockReAttachPrtHandler, Mockito.times(1)).processChallenge(TEST_CROSS_CLOUD_REDIRECT_URL);
            Mockito.verify(mockWebView).loadUrl(Mockito.anyString());
        } catch (Exception e) {
            Assert.fail("Failure is not expected. We should have caught the exception and ignored it. " + e);
        }
    }

    @Test
    public void testUrlOverrideHandlesIntentRedirectUrl() {
        setBrokerInstallIntentValidationFlight(true);
        final Context mockContext = Mockito.mock(Context.class);
        final WebView mockWebView = Mockito.mock(WebView.class);
        when(mockWebView.getContext()).thenReturn(mockContext);

        assertTrue(mWebViewClient.shouldOverrideUrlLoading(mockWebView, TEST_INTENT_INSTALL_BROKER_REDIRECT_URL));

        final ArgumentCaptor<Intent> intentCaptor = ArgumentCaptor.forClass(Intent.class);
        Mockito.verify(mockContext).startActivity(intentCaptor.capture());
        assertEquals(GOOGLE_PLAY_STORE_PACKAGE_NAME, intentCaptor.getValue().getPackage());
        assertNull(intentCaptor.getValue().getComponent());
        CommonFlightsManager.INSTANCE.resetFlightsManager();
    }

    @Test
    public void testIntentToInstallBroker_blocksNonAllowlistedPackage_whenValidationEnabled() {
        setBrokerInstallIntentValidationFlight(true);
        final Context mockContext = Mockito.mock(Context.class);
        final WebView mockWebView = Mockito.mock(WebView.class);
        when(mockWebView.getContext()).thenReturn(mockContext);

        // The request passes the install-intent gate (so it is "handled") ...
        assertTrue(mWebViewClient.shouldOverrideUrlLoading(mockWebView, TEST_INTENT_WITH_NON_ALLOWLISTED_PACKAGE));
        // ... but a parsed target package that is not on the allow-list must not be launched.
        Mockito.verify(mockContext, never()).startActivity(any(Intent.class));
        CommonFlightsManager.INSTANCE.resetFlightsManager();
    }

    @Test
    public void testIntentToInstallBroker_clearsExplicitComponent_whenValidationEnabled() {
        setBrokerInstallIntentValidationFlight(true);
        final Context mockContext = Mockito.mock(Context.class);
        final WebView mockWebView = Mockito.mock(WebView.class);
        when(mockWebView.getContext()).thenReturn(mockContext);

        assertTrue(mWebViewClient.shouldOverrideUrlLoading(mockWebView, TEST_INTENT_WITH_EXPLICIT_COMPONENT));

        final ArgumentCaptor<Intent> intentCaptor = ArgumentCaptor.forClass(Intent.class);
        Mockito.verify(mockContext).startActivity(intentCaptor.capture());
        // Any explicit component is cleared; only the allow-listed package remains.
        assertNull(intentCaptor.getValue().getComponent());
        assertEquals(GOOGLE_PLAY_STORE_PACKAGE_NAME, intentCaptor.getValue().getPackage());
        CommonFlightsManager.INSTANCE.resetFlightsManager();
    }

    @Test
    public void testIntentToInstallBroker_legacyBehavior_whenValidationDisabled() {
        setBrokerInstallIntentValidationFlight(false);
        final Context mockContext = Mockito.mock(Context.class);
        final WebView mockWebView = Mockito.mock(WebView.class);
        when(mockWebView.getContext()).thenReturn(mockContext);

        // With the validation flight off, the legacy launch behavior is preserved (rollback switch).
        assertTrue(mWebViewClient.shouldOverrideUrlLoading(mockWebView, TEST_INTENT_WITH_NON_ALLOWLISTED_PACKAGE));
        Mockito.verify(mockContext).startActivity(any(Intent.class));
        CommonFlightsManager.INSTANCE.resetFlightsManager();
    }

    /**
     * A selector cannot be injected through the {@code intent://} URL scheme (Android does not
     * (de)serialize a selector via parseUri/toUri), so the selector-clearing defense is exercised
     * directly on the sanitizer. On Android a top-level package and a selector are mutually
     * exclusive, so an intent that smuggles the store package inside a selector has a {@code null}
     * top-level package: the sanitizer nulls the selector (verified on the mutated intent) and then
     * blocks the intent because the validated package is null.
     */
    @Test
    public void testSanitizeAndValidateBrokerInstallIntent_clearsSelectorAndBlocks() {
        final Intent intent = new Intent(Intent.ACTION_VIEW);
        final Intent selector = new Intent(Intent.ACTION_VIEW);
        selector.setPackage(GOOGLE_PLAY_STORE_PACKAGE_NAME);
        intent.setSelector(selector);

        final Intent result = mWebViewClient.sanitizeAndValidateBrokerInstallIntent(intent);

        assertNull(result);
        // The selector was cleared before the null-package block, so it can never redirect resolution.
        assertNull(intent.getSelector());
    }

    /**
     * When the validated (top-level) package is not allow-listed, the intent must be blocked
     * (returns {@code null}) so it is never launched.
     */
    @Test
    public void testSanitizeAndValidateBrokerInstallIntent_returnsNullForNonAllowlistedPackage() {
        final Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setPackage("com.example.unrelatedapp");

        assertNull(mWebViewClient.sanitizeAndValidateBrokerInstallIntent(intent));
    }

    /**
     * For an allow-listed target, URI-permission grant flags are stripped and CATEGORY_BROWSABLE is
     * added, while unrelated flags (e.g. FLAG_ACTIVITY_NEW_TASK) are preserved.
     */
    @Test
    public void testSanitizeAndValidateBrokerInstallIntent_stripsGrantFlagsAndAddsBrowsable() {
        final Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setPackage(GOOGLE_PLAY_STORE_PACKAGE_NAME);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                | Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
                | Intent.FLAG_ACTIVITY_NEW_TASK);

        final Intent result = mWebViewClient.sanitizeAndValidateBrokerInstallIntent(intent);

        assertNotNull(result);
        assertEquals(0, result.getFlags() & Intent.FLAG_GRANT_READ_URI_PERMISSION);
        assertEquals(0, result.getFlags() & Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        assertEquals(0, result.getFlags() & Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        assertEquals(0, result.getFlags() & Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
        assertNotEquals(0, result.getFlags() & Intent.FLAG_ACTIVITY_NEW_TASK);
        assertTrue(result.hasCategory(Intent.CATEGORY_BROWSABLE));
    }

    private void setBrokerInstallIntentValidationFlight(final boolean enabled) {
        final IFlightsProvider mockFlightsProvider = Mockito.mock(IFlightsProvider.class);
        // Default any unstubbed flight to its real default value so these tests don't silently
        // depend on Mockito's boolean default (false) as the WebViewClient evolves.
        when(mockFlightsProvider.isFlightEnabled(any(IFlightConfig.class)))
                .thenAnswer(invocation -> {
                    final IFlightConfig config = invocation.getArgument(0);
                    final Object defaultValue = config.getDefaultValue();
                    return defaultValue instanceof Boolean && (Boolean) defaultValue;
                });
        // Override only the flight under test.
        when(mockFlightsProvider.isFlightEnabled(CommonFlight.ENABLE_BROKER_INSTALL_INTENT_VALIDATION))
                .thenReturn(enabled);
        final MockCommonFlightsManager mockCommonFlightsManager = new MockCommonFlightsManager();
        mockCommonFlightsManager.setMockCommonFlightsProvider(mockFlightsProvider);
        CommonFlightsManager.INSTANCE.initializeCommonFlightsManager(mockCommonFlightsManager);
    }

    public void setTestPasskeyRedirectUrl() {
        assertTrue(mWebViewClient.shouldOverrideUrlLoading(mMockWebView, TEST_PASSKEY_REDIRECT_URL));
    }

    @Test
    public void testPasskeyActivityCastingNoException() {
        try {
            // Putting an explicit non-AuthorizationActivity activity here to test that an exception won't be thrown.
            mActivity = Robolectric.buildActivity(DualScreenActivity.class).get();
            mWebViewClient = new AzureActiveDirectoryWebViewClient(
                    mActivity,
                    new IAuthorizationCompletionCallback() {
                        @Override
                        public void onChallengeResponseReceived(@NonNull RawAuthorizationResult response) {

                        }

                        @Override
                        public void setPKeyAuthStatus(boolean status) {
                            return;
                        }
                    },
                    new OnPageLoadedCallback() {
                        @Override
                        public void onPageLoaded(final String url) {
                            return;
                        }
                    },
                    TEST_REDIRECT_URI,
                    Mockito.mock(SwitchBrowserProtocolCoordinator.class),
                    "homeTenantId",
                    false);
            mWebViewClient.shouldOverrideUrlLoading(mMockWebView, TEST_PASSKEY_REDIRECT_URL);
        } catch (ClassCastException e) {
            Assert.fail("Failure is not expected. The class checks should have prevented this." + e);
        } catch (Exception e) {
            Assert.fail("Failure is not expected." + e);
        }
    }

    @Test
    public void testOnReceivedSslError_Legacy() {
        final String mockActiveUrl = "https://login.microsoftonline.com/organizations/oAuth2/v2.0/authorize";
        final SslErrorHandler mockHandler = Mockito.mock(android.webkit.SslErrorHandler.class);
        final SslError mockError = Mockito.mock(android.net.http.SslError.class);
        final IAuthorizationCompletionCallback mockCallback = Mockito.mock(IAuthorizationCompletionCallback.class);
        when(mockError.getUrl()).thenReturn("https://example.com");
        final IFlightsManager mockFlightsManager = Mockito.mock(IFlightsManager.class);
        final IFlightsProvider mockFlightsProvider = Mockito.mock(IFlightsProvider.class);
        when(mockFlightsProvider.isFlightEnabled(eq(CommonFlight.SHOULD_PRESERVE_WEBVIEW_FLOW_ON_SSL_ERROR))).thenReturn(false);
        when(mockFlightsManager.getFlightsProvider(anyLong())).thenReturn(mockFlightsProvider);
        CommonFlightsManager.INSTANCE.initializeCommonFlightsManager(mockFlightsManager);
        final AzureActiveDirectoryWebViewClient mockWebViewClient = new AzureActiveDirectoryWebViewClient(
                mActivity,
                mockCallback,
                url -> {},
                TEST_REDIRECT_URI,
                Mockito.mock(SwitchBrowserProtocolCoordinator.class),
                "homeTenantId",
                false);
        final WebView mockWebView = new WebView(mContext);
        mockWebView.setWebViewClient(mockWebViewClient);

        // act
        mockWebViewClient.onReceivedSslError(mockWebView, mockHandler, mockError);

        Mockito.verify(mockHandler, Mockito.times(1)).cancel();
        Mockito.verify(mockCallback, Mockito.times(1)).onChallengeResponseReceived(any());
    }

    @Test
    public void testOnReceivedSslError() {
        final IFlightsManager mockFlightsManager = Mockito.mock(IFlightsManager.class);
        final IFlightsProvider mockFlightsProvider = Mockito.mock(IFlightsProvider.class);
        when(mockFlightsProvider.isFlightEnabled(eq(CommonFlight.SHOULD_PRESERVE_WEBVIEW_FLOW_ON_SSL_ERROR))).thenReturn(true);
        when(mockFlightsManager.getFlightsProvider(anyLong())).thenReturn(mockFlightsProvider);
        CommonFlightsManager.INSTANCE.initializeCommonFlightsManager(mockFlightsManager);
        final SslErrorHandler mockHandler = Mockito.mock(android.webkit.SslErrorHandler.class);
        final SslError mockError = Mockito.mock(android.net.http.SslError.class);
        final IAuthorizationCompletionCallback mockCallback = Mockito.mock(IAuthorizationCompletionCallback.class);
        when(mockError.getUrl()).thenReturn("https://example.com");
        final AzureActiveDirectoryWebViewClient mockWebViewClient = new AzureActiveDirectoryWebViewClient(
                mActivity,
                mockCallback,
                url -> {},
                TEST_REDIRECT_URI,
                Mockito.mock(SwitchBrowserProtocolCoordinator.class),
                "homeTenantId",
                false
        );
        final WebView mockWebView = new WebView(mContext);
        mockWebView.setWebViewClient(mockWebViewClient);

        // act
        mockWebViewClient.onReceivedSslError(mockWebView, mockHandler, mockError);

        // verify that the handler is cancelled and the callback is invoked
        Mockito.verify(mockHandler, Mockito.times(1)).cancel();
        Mockito.verify(mockCallback, never()).onChallengeResponseReceived(any());

        CommonFlightsManager.INSTANCE.resetFlightsManager();
    }

    @Test
    @Config(shadows = {ShadowProcessUtil.class})
    public void testProcessWebsiteRequest() {
        // Test case 1: General browser redirect (default case)
        testProcessWebsiteRequest_BrowserRedirect();

        // Test case 2: Device CA request
        testProcessWebsiteRequest_DeviceCaRequest();

        // Test case 3: WebCP playstore redirect when feature is enabled
        testProcessWebsiteRequest_WebCpPlaystoreRedirect();

        // Test case 4: Exception handling
        testProcessWebsiteRequest_ExceptionHandling();
    }

    private void testProcessWebsiteRequest_BrowserRedirect() {
        // Arrange
        final IAuthorizationCompletionCallback mockCallback = Mockito.mock(IAuthorizationCompletionCallback.class);
        final ArgumentCaptor<RawAuthorizationResult> resultCaptor = ArgumentCaptor.forClass(RawAuthorizationResult.class);
        final AzureActiveDirectoryWebViewClient webViewClient = Mockito.spy(new AzureActiveDirectoryWebViewClient(
                mActivity,
                mockCallback,
                url -> {},
                TEST_REDIRECT_URI,
                Mockito.mock(SwitchBrowserProtocolCoordinator.class),
                "homeTenantId",
                false
        ));
        final WebView mockWebView = Mockito.mock(WebView.class);

        // Mock openLinkInBrowser to simulate successful browser launch
        Mockito.doNothing().when(webViewClient).openLinkInBrowser(any());

        // Act
        webViewClient.processWebsiteRequest(mockWebView, TEST_WEBSITE_REQUEST_URL);

        // Verify
        Mockito.verify(webViewClient).openLinkInBrowser(TEST_WEBSITE_REQUEST_URL);
        Mockito.verify(mockCallback).onChallengeResponseReceived(resultCaptor.capture());
        final RawAuthorizationResult capturedResult = resultCaptor.getValue();
        assertEquals(CANCELLED, capturedResult.getResultCode());
    }

    private void testProcessWebsiteRequest_DeviceCaRequest() {
        // Arrange
        final IAuthorizationCompletionCallback mockCallback = Mockito.mock(IAuthorizationCompletionCallback.class);
        final ArgumentCaptor<RawAuthorizationResult> resultCaptor = ArgumentCaptor.forClass(RawAuthorizationResult.class);
        final AzureActiveDirectoryWebViewClient webViewClient = Mockito.spy(new AzureActiveDirectoryWebViewClient(
                mActivity,
                mockCallback,
                url -> {},
                TEST_REDIRECT_URI,
                Mockito.mock(SwitchBrowserProtocolCoordinator.class),
                "homeTenantId",
                false
        ));
        final WebView mockWebView = Mockito.mock(WebView.class);


        // Mock openLinkInBrowser to simulate successful browser launch
        Mockito.doNothing().when(webViewClient).openLinkInBrowser(any());

        // Act
        webViewClient.processWebsiteRequest(mockWebView, TEST_BROWSER_DEVICE_CA_URL_QUERY_STRING_PARAMETER);

        // Assert
        Mockito.verify(mockWebView).stopLoading();
        Mockito.verify(webViewClient).openLinkInBrowser(TEST_BROWSER_DEVICE_CA_URL_QUERY_STRING_PARAMETER);

        // Capture and verify the specific result received in the callback
        Mockito.verify(mockCallback).onChallengeResponseReceived(resultCaptor.capture());

        final RawAuthorizationResult capturedResult = resultCaptor.getValue();
        assertEquals(MDM_FLOW, capturedResult.getResultCode());
    }

    private void testProcessWebsiteRequest_WebCpPlaystoreRedirect() {
        // Arrange
        final IAuthorizationCompletionCallback mockCallback = Mockito.mock(IAuthorizationCompletionCallback.class);
        final ArgumentCaptor<RawAuthorizationResult> resultCaptor = ArgumentCaptor.forClass(RawAuthorizationResult.class);
        final AzureActiveDirectoryWebViewClient webViewClient = new AzureActiveDirectoryWebViewClient(
                mActivity,
                mockCallback,
                url -> {},
                TEST_REDIRECT_URI,
                Mockito.mock(SwitchBrowserProtocolCoordinator.class),
                "homeTenantId",
                false
        );
        final WebView mockWebView = Mockito.mock(WebView.class);

        // Mock flight manager to enable WebCP in WebView feature
        final IFlightsProvider mockFlightsProvider = Mockito.mock(IFlightsProvider.class);
        when(mockFlightsProvider.isFlightEnabled(eq(CommonFlight.ENABLE_WEB_CP_IN_WEBVIEW))).thenReturn(true);

        final MockCommonFlightsManager mockCommonFlightsManager = new MockCommonFlightsManager();
        mockCommonFlightsManager.setMockCommonFlightsProvider(mockFlightsProvider);
        CommonFlightsManager.INSTANCE.initializeCommonFlightsManager(mockCommonFlightsManager);

        // Mock the isWebCpInWebviewFeatureEnabled method to return true
        webViewClient.isWebCpInWebviewFeatureEnabled(TEST_PLAYSTORE_REDIRECT_WITH_BROWSER_PROTOCOL);

        // Act
        webViewClient.processWebsiteRequest(mockWebView, TEST_PLAYSTORE_REDIRECT_WITH_BROWSER_PROTOCOL);

        // Capture and verify that the callback is invoked with success result
        Mockito.verify(mockCallback).onChallengeResponseReceived(resultCaptor.capture());

        final RawAuthorizationResult capturedResult = resultCaptor.getValue();
        // For WebCP playstore redirects, we expect MDM_FLOW result code when successful
        assertEquals(MDM_FLOW, capturedResult.getResultCode());

        // Cleanup
        CommonFlightsManager.INSTANCE.resetFlightsManager();
    }

    private void testProcessWebsiteRequest_ExceptionHandling() {
        // Arrange
        final IAuthorizationCompletionCallback mockCallback = Mockito.mock(IAuthorizationCompletionCallback.class);
        final ArgumentCaptor<RawAuthorizationResult> resultCaptor = ArgumentCaptor.forClass(RawAuthorizationResult.class);
        final AzureActiveDirectoryWebViewClient webViewClient = Mockito.spy(new AzureActiveDirectoryWebViewClient(
                mActivity,
                mockCallback,
                url -> {},
                TEST_REDIRECT_URI,
                Mockito.mock(SwitchBrowserProtocolCoordinator.class),
                "homeTenantId",
                false
        ));
        final WebView mockWebView = Mockito.mock(WebView.class);

        // Mock openLinkInBrowser to throw ActivityNotFoundException to simulate failure
        Mockito.doThrow(new ActivityNotFoundException("Test: No browser found")).when(webViewClient).openLinkInBrowser(any());

        // Act
        webViewClient.processWebsiteRequest(mockWebView, TEST_WEBSITE_REQUEST_URL);

        // Verify that openLinkInBrowser was called
        Mockito.verify(webViewClient).openLinkInBrowser(TEST_WEBSITE_REQUEST_URL);

        // Capture and verify the specific error received in the callback
        Mockito.verify(mockCallback).onChallengeResponseReceived(resultCaptor.capture());

        final RawAuthorizationResult capturedResult = resultCaptor.getValue();
        // Verify that the error contains the expected error code when browser launch fails
        assertEquals("Expected UNEXPECTED_ERROR error code",
                ErrorStrings.UNEXPECTED_ERROR,
                ((ClientException) capturedResult.getException()).getErrorCode());

        // Verify that the error message is about browser not found
        assertTrue("Expected error message about browser not found",
                capturedResult.getException().getMessage().contains("No browser found to open the link"));
    }

    // -----------------------------------------------------------------------
    // URL tracking tests (IUrlLoadTracker integration)
    // -----------------------------------------------------------------------

    private AzureActiveDirectoryWebViewClient createWebViewClientWithTracker(
            final IUrlLoadTracker tracker) throws ClientException {
        return new AzureActiveDirectoryWebViewClient(
                mActivity,
                Mockito.mock(IAuthorizationCompletionCallback.class),
                url -> {},
                TEST_REDIRECT_URI,
                Mockito.mock(SwitchBrowserProtocolCoordinator.class),
                "homeTenantId",
                false,
                tracker);
    }

    @Test
    public void testUrlTracker_onPageStarted_callsTrackNewUrlStatus() throws ClientException {
        final IUrlLoadTracker mockTracker = Mockito.mock(IUrlLoadTracker.class);
        final AzureActiveDirectoryWebViewClient client = createWebViewClientWithTracker(mockTracker);
        final String url = "https://login.microsoftonline.com/common/oauth2/v2.0/authorize";

        client.onPageStarted(mMockWebView, url, null);

        Mockito.verify(mockTracker).trackNewUrlStatus(url, null, null);
    }

    @Test
    public void testUrlTracker_onPageStarted_nullTracker_noException() throws ClientException {
        // Uses constructor without IUrlLoadTracker; should not throw
        final AzureActiveDirectoryWebViewClient client = new AzureActiveDirectoryWebViewClient(
                mActivity,
                Mockito.mock(IAuthorizationCompletionCallback.class),
                url -> {},
                TEST_REDIRECT_URI,
                Mockito.mock(SwitchBrowserProtocolCoordinator.class),
                "homeTenantId",
                false);

        // Should complete without NullPointerException
        client.onPageStarted(mMockWebView, "https://login.microsoftonline.com/common/oauth2/v2.0/authorize", null);
    }

    @Test
    public void testUrlTracker_onReceivedError_deprecated_callsUpdateLatestUrlStatus() throws ClientException {
        final IUrlLoadTracker mockTracker = Mockito.mock(IUrlLoadTracker.class);
        final AzureActiveDirectoryWebViewClient client = createWebViewClientWithTracker(mockTracker);
        final int errorCode = -2;
        final String description = "net::ERR_NAME_NOT_RESOLVED";

        client.onReceivedError(mMockWebView, errorCode, description,
                "https://login.microsoftonline.com/common/oauth2/v2.0/authorize");

        Mockito.verify(mockTracker).updateLatestUrlStatus("Code:" + errorCode + ", " + description, null);
    }

    @Test
    public void testUrlTracker_onReceivedError_mainFrame_callsUpdateLatestUrlStatus() throws ClientException {
        final IUrlLoadTracker mockTracker = Mockito.mock(IUrlLoadTracker.class);
        final AzureActiveDirectoryWebViewClient client = createWebViewClientWithTracker(mockTracker);

        final WebResourceRequest mockRequest = Mockito.mock(WebResourceRequest.class);
        final WebResourceError mockError = Mockito.mock(WebResourceError.class);
        Mockito.when(mockRequest.isForMainFrame()).thenReturn(true);
        Mockito.when(mockError.getErrorCode()).thenReturn(-2);
        Mockito.when(mockError.getDescription()).thenReturn("net::ERR_NAME_NOT_RESOLVED");

        client.onReceivedError(mMockWebView, mockRequest, mockError);

        Mockito.verify(mockTracker).updateLatestUrlStatus("Code:-2, net::ERR_NAME_NOT_RESOLVED", null);
    }

    @Test
    public void testUrlTracker_onReceivedError_subResource_doesNotCallUpdateLatestUrlStatus() throws ClientException {
        final IUrlLoadTracker mockTracker = Mockito.mock(IUrlLoadTracker.class);
        final AzureActiveDirectoryWebViewClient client = createWebViewClientWithTracker(mockTracker);

        final WebResourceRequest mockRequest = Mockito.mock(WebResourceRequest.class);
        final WebResourceError mockError = Mockito.mock(WebResourceError.class);
        Mockito.when(mockRequest.isForMainFrame()).thenReturn(false);

        client.onReceivedError(mMockWebView, mockRequest, mockError);

        Mockito.verify(mockTracker, never()).updateLatestUrlStatus(any(), any());
    }

    @Test
    public void testUrlTracker_onReceivedSslError_callsUpdateLatestUrlStatus() throws ClientException {
        final IFlightsManager mockFlightsManager = Mockito.mock(IFlightsManager.class);
        final IFlightsProvider mockFlightsProvider = Mockito.mock(IFlightsProvider.class);
        when(mockFlightsProvider.isFlightEnabled(eq(CommonFlight.SHOULD_PRESERVE_WEBVIEW_FLOW_ON_SSL_ERROR))).thenReturn(false);
        when(mockFlightsManager.getFlightsProvider(anyLong())).thenReturn(mockFlightsProvider);
        CommonFlightsManager.INSTANCE.initializeCommonFlightsManager(mockFlightsManager);

        final IUrlLoadTracker mockTracker = Mockito.mock(IUrlLoadTracker.class);
        final SslError mockError = Mockito.mock(SslError.class);
        final SslErrorHandler mockHandler = Mockito.mock(SslErrorHandler.class);
        Mockito.when(mockError.toString()).thenReturn("SslError(SSL_EXPIRED)");

        final AzureActiveDirectoryWebViewClient client = createWebViewClientWithTracker(mockTracker);

        client.onReceivedSslError(new WebView(mContext), mockHandler, mockError);

        Mockito.verify(mockTracker).updateLatestUrlStatus("SslError(SSL_EXPIRED)", null);
        CommonFlightsManager.INSTANCE.resetFlightsManager();
    }

    @Test
    public void testUrlTracker_onReceivedHttpError_mainFrame_callsUpdateLatestUrlStatus() throws ClientException {
        final IUrlLoadTracker mockTracker = Mockito.mock(IUrlLoadTracker.class);
        final AzureActiveDirectoryWebViewClient client = createWebViewClientWithTracker(mockTracker);

        final WebResourceRequest mockRequest = Mockito.mock(WebResourceRequest.class);
        final WebResourceResponse mockErrorResponse = Mockito.mock(WebResourceResponse.class);
        Mockito.when(mockRequest.isForMainFrame()).thenReturn(true);
        Mockito.when(mockErrorResponse.getStatusCode()).thenReturn(403);

        client.onReceivedHttpError(mMockWebView, mockRequest, mockErrorResponse);

        Mockito.verify(mockTracker).updateLatestUrlStatus("HTTP Error Code: 403", null);
    }

    @Test
    public void testUrlTracker_onReceivedHttpError_subResource_doesNotCallUpdateLatestUrlStatus() throws ClientException {
        final IUrlLoadTracker mockTracker = Mockito.mock(IUrlLoadTracker.class);
        final AzureActiveDirectoryWebViewClient client = createWebViewClientWithTracker(mockTracker);

        final WebResourceRequest mockRequest = Mockito.mock(WebResourceRequest.class);
        final WebResourceResponse mockErrorResponse = Mockito.mock(WebResourceResponse.class);
        Mockito.when(mockRequest.isForMainFrame()).thenReturn(false);

        client.onReceivedHttpError(mMockWebView, mockRequest, mockErrorResponse);

        Mockito.verify(mockTracker, never()).updateLatestUrlStatus(any(), any());
    }

    // ===== Authenticator activation app link tests =====

    @Test
    public void testIsAuthenticatorActivationAppLink_globalHost_shouldReturnTrue() {
        assertTrue(mWebViewClient.isAuthenticatorActivationAppLink(
                TEST_AUTHENTICATOR_ACTIVATION_GLOBAL.toLowerCase()));
    }

    @Test
    public void testIsAuthenticatorActivationAppLink_chinaHost_shouldReturnTrue() {
        assertTrue(mWebViewClient.isAuthenticatorActivationAppLink(
                TEST_AUTHENTICATOR_ACTIVATION_CHINA.toLowerCase()));
    }

    @Test
    public void testIsAuthenticatorActivationAppLink_usGovHost_shouldReturnTrue() {
        assertTrue(mWebViewClient.isAuthenticatorActivationAppLink(
                TEST_AUTHENTICATOR_ACTIVATION_US_GOV.toLowerCase()));
    }

    @Test
    public void testIsAuthenticatorActivationAppLink_invalidHost_shouldReturnFalse() {
        assertFalse(mWebViewClient.isAuthenticatorActivationAppLink(
                TEST_AUTHENTICATOR_ACTIVATION_INVALID_HOST.toLowerCase()));
    }

    @Test
    public void testIsAuthenticatorActivationAppLink_invalidPath_shouldReturnFalse() {
        assertFalse(mWebViewClient.isAuthenticatorActivationAppLink(
                TEST_AUTHENTICATOR_ACTIVATION_INVALID_PATH.toLowerCase()));
    }

    @Test
    public void testIsAuthenticatorActivationAppLink_nonHttpsScheme_shouldReturnFalse() {
        assertFalse(mWebViewClient.isAuthenticatorActivationAppLink(
                "http://login.microsoftonline.com/authenticatorapp/activateaccount"));
    }

    @Test
    public void testIsAuthenticatorActivationAppLink_legacyMfaScheme_shouldReturnFalse() {
        assertFalse(mWebViewClient.isAuthenticatorActivationAppLink(
                AUTHENTICATOR_MFA_LINKING_PREFIX.toLowerCase()));
    }

    /**
     * Registers a fake handler on the given activity's PackageManager so that
     * {@code intent.resolveActivity(...)} returns non-null for ACTION_VIEW + the given Uri.
     * This lets us exercise the "handler present" branch of
     * processAuthenticatorActivationAppLink in a Robolectric test.
     */
    private void registerActivationHandler(@NonNull final Activity activity,
                                           @NonNull final Uri uri,
                                           @NonNull final String packageName,
                                           @NonNull final String activityClass) {
        final ShadowPackageManager shadowPm = Shadows.shadowOf(activity.getPackageManager());
        final Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        final ResolveInfo info = new ResolveInfo();
        info.activityInfo = new ActivityInfo();
        info.activityInfo.packageName = packageName;
        info.activityInfo.name = activityClass;
        shadowPm.addResolveInfoForIntent(intent, info);
    }

    @Test
    public void testAuthenticatorActivationAppLink_launchesIntent_whenHandlerPresent() {
        // Arrange: register a handler so resolveActivity(...) returns non-null.
        registerActivationHandler(
                mActivity,
                Uri.parse(TEST_AUTHENTICATOR_ACTIVATION_GLOBAL),
                "com.azure.authenticator",
                "com.azure.authenticator.ui.MainActivity");

        final WebView mockWebView = Mockito.mock(WebView.class);

        // Act
        final boolean handled = mWebViewClient.shouldOverrideUrlLoading(
                mockWebView, TEST_AUTHENTICATOR_ACTIVATION_GLOBAL);

        // Assert: URL was intercepted, WebView stopped, and an ACTION_VIEW intent was launched.
        assertTrue(handled);
        Mockito.verify(mockWebView).stopLoading();

        final Intent launched = Shadows.shadowOf(mActivity).getNextStartedActivity();
        Assert.assertNotNull("Expected an intent to be launched for the activation link", launched);
        assertEquals(Intent.ACTION_VIEW, launched.getAction());
        assertEquals(TEST_AUTHENTICATOR_ACTIVATION_GLOBAL, launched.getData().toString());
        assertTrue("Expected FLAG_ACTIVITY_NEW_TASK on activation intent",
                (launched.getFlags() & Intent.FLAG_ACTIVITY_NEW_TASK) != 0);
    }

    @Test
    public void testAuthenticatorActivationAppLink_noHandler_stopsLoading_andDoesNotCrash() {
        // Arrange: no handler registered -> intent.resolveActivity() returns null in both
        // the Authenticator launch path and the openLinkInBrowser fallback path.
        final WebView mockWebView = Mockito.mock(WebView.class);

        // Act
        final boolean handled = mWebViewClient.shouldOverrideUrlLoading(
                mockWebView, TEST_AUTHENTICATOR_ACTIVATION_GLOBAL);

        // Assert: handled=true, WebView stopped, no activity started (no crash, no error callback).
        assertTrue(handled);
        Mockito.verify(mockWebView).stopLoading();
        Assert.assertNull("Expected NO intent to be launched when no handler is installed",
                Shadows.shadowOf(mActivity).getNextStartedActivity());
    }

    @Test
    public void testAuthenticatorActivationAppLink_preservesOriginalCasing() {
        // The activation link carries case-sensitive query values (e.g. base64 codes).
        // shouldOverrideUrlLoading lowercases the URL for matching but must dispatch the
        // *original* URL to the Authenticator.
        final String mixedCaseUrl =
                "https://login.microsoftonline.com/authenticatorApp/activateAccount"
                        + "?accountType=mfa&source=QrCode&code=AbCdEf123XYZ&url=https://Service";
        registerActivationHandler(
                mActivity,
                Uri.parse(mixedCaseUrl),
                "com.azure.authenticator",
                "com.azure.authenticator.ui.MainActivity");

        final WebView mockWebView = Mockito.mock(WebView.class);

        final boolean handled = mWebViewClient.shouldOverrideUrlLoading(mockWebView, mixedCaseUrl);

        assertTrue(handled);
        final Intent launched = Shadows.shadowOf(mActivity).getNextStartedActivity();
        Assert.assertNotNull(launched);
        assertEquals("Authenticator activation intent must carry the original-cased URL",
                mixedCaseUrl, launched.getData().toString());
    }

    @Test
    public void testProcessAuthAppMFAUrl_startsViewIntentWithNewTaskFlag() {
        // microsoft-authenticator://activatemfa/... is handed to processAuthAppMFAUrl,
        // which dispatches an ACTION_VIEW intent with FLAG_ACTIVITY_NEW_TASK.
        final String mfaUrl = AUTHENTICATOR_MFA_LINKING_PREFIX + "/?x=1";
        // Make the intent resolvable so the OS would accept the launch. (Robolectric
        // records the started activity regardless, but this documents the expectation.)
        registerActivationHandler(
                mActivity,
                Uri.parse(mfaUrl),
                "com.azure.authenticator",
                "com.azure.authenticator.ui.MainActivity");

        final boolean handled = mWebViewClient.shouldOverrideUrlLoading(mMockWebView, mfaUrl);

        assertTrue(handled);
        final Intent launched = Shadows.shadowOf(mActivity).getNextStartedActivity();
        Assert.assertNotNull(launched);
        assertEquals(Intent.ACTION_VIEW, launched.getAction());
        assertEquals(mfaUrl, launched.getDataString());
        assertTrue("MFA activation intent must carry FLAG_ACTIVITY_NEW_TASK",
                (launched.getFlags() & Intent.FLAG_ACTIVITY_NEW_TASK) != 0);
    }

    // -----------------------------------------------------------------------
    // Onboarding telemetry hooks
    // -----------------------------------------------------------------------

    /**
     * Verifies that when an OnboardingTelemetryRecorder is attached to the WebView client,
     * a broker install request URL produces a populated blob containing the
     * {@code BrokerInstallPrompted} step. We construct a recorder with a synthetic seed
     * containing a session correlation id and a blocking error so {@code finalizeBlob}
     * returns non-empty.
     */
    @Test
    public void testProcessInstallRequest_RecordsBrokerInstallPromptedStep() throws Exception {
        final String seedJson = "{\"schema_version\":\"1.0.0\","
                + "\"session_correlation_id\":\"abc-123\","
                + "\"onboarding_mode\":\"non-brokered\"}";
        final com.microsoft.identity.common.internal.telemetry.OnboardingTelemetryRecorder recorder =
                new com.microsoft.identity.common.internal.telemetry.OnboardingTelemetryRecorder(
                        seedJson, "client-id", "scope1", mContext);
        // Record a blocking error so finalizeBlob() emits a populated blob.
        recorder.addBlockingError(
                com.microsoft.identity.common.java.telemetry.OnboardingTelemetryConstants.BLOCKING_ERROR_BROKER_INSTALL);

        mWebViewClient.setOnboardingTelemetryRecorder(recorder);

        // Trigger a broker install URL through the WebView client (delegates to processInstallRequest).
        mWebViewClient.shouldOverrideUrlLoading(mMockWebView, TEST_INSTALL_REQUEST_URL);

        final org.json.JSONObject blob = new org.json.JSONObject(recorder.finalizeBlob());
        final org.json.JSONArray steps = blob.getJSONArray("steps_list");
        boolean foundStep = false;
        for (int i = 0; i < steps.length(); i++) {
            if (com.microsoft.identity.common.java.telemetry.OnboardingTelemetryConstants
                    .STEP_BROKER_INSTALL_PROMPTED.equals(steps.getJSONObject(i).getString("step_id"))) {
                foundStep = true;
                break;
            }
        }
        assertTrue("Expected BrokerInstallPrompted step in onboarding blob", foundStep);
    }

    /**
     * No recorder attached → no crash, hook is a no-op.
     */
    @Test
    public void testProcessInstallRequest_NoRecorder_IsNoOp() {
        // Default mWebViewClient has no recorder attached. This must not throw.
        assertTrue(mWebViewClient.shouldOverrideUrlLoading(mMockWebView, TEST_INSTALL_REQUEST_URL));
    }

    /**
     * Verifies that {@code onPageFinished} extracts the host from a real URL and stores it on
     * the recorder as {@code lastLoadedDomain}. Requires a blocking error to have been recorded
     * so the finalized blob is non-empty.
     */
    @Test
    public void testOnPageFinished_RecordsLastLoadedDomain() throws Exception {
        final String seedJson = "{\"schema_version\":\"1.0.0\","
                + "\"session_correlation_id\":\"abc-123\","
                + "\"onboarding_mode\":\"non-brokered\"}";
        final com.microsoft.identity.common.internal.telemetry.OnboardingTelemetryRecorder recorder =
                new com.microsoft.identity.common.internal.telemetry.OnboardingTelemetryRecorder(
                        seedJson, "client-id", "scope1", mContext);
        recorder.addBlockingError(
                com.microsoft.identity.common.java.telemetry.OnboardingTelemetryConstants.BLOCKING_ERROR_BROKER_INSTALL);

        mWebViewClient.setOnboardingTelemetryRecorder(recorder);
        mWebViewClient.onPageFinished(mMockWebView, "https://login.microsoftonline.com/common/oauth2/authorize");

        final org.json.JSONObject blob = new org.json.JSONObject(recorder.finalizeBlob());
        assertEquals("login.microsoftonline.com", blob.getString(
                com.microsoft.identity.common.java.telemetry.OnboardingTelemetryConstants.LAST_LOADED_DOMAIN));
    }

    /**
     * onPageFinished with a URL that has no host (e.g. about:blank) does not throw and
     * does not set lastLoadedDomain.
     */
    @Test
    public void testOnPageFinished_BlankUrl_DoesNotSetDomain() throws Exception {
        final String seedJson = "{\"schema_version\":\"1.0.0\","
                + "\"session_correlation_id\":\"abc-123\","
                + "\"onboarding_mode\":\"non-brokered\"}";
        final com.microsoft.identity.common.internal.telemetry.OnboardingTelemetryRecorder recorder =
                new com.microsoft.identity.common.internal.telemetry.OnboardingTelemetryRecorder(
                        seedJson, "client-id", "scope1", mContext);
        recorder.addBlockingError(
                com.microsoft.identity.common.java.telemetry.OnboardingTelemetryConstants.BLOCKING_ERROR_BROKER_INSTALL);

        mWebViewClient.setOnboardingTelemetryRecorder(recorder);
        mWebViewClient.onPageFinished(mMockWebView, TEST_BLANK_PAGE_REQUEST_URL);

        final org.json.JSONObject blob = new org.json.JSONObject(recorder.finalizeBlob());
        assertFalse("blank URL should not produce a last_loaded_domain entry",
                blob.has("last_loaded_domain"));
    }

    // -----------------------------------------------------------------------
    // MAM Conditional Access: install-referrer decoration on the broker install
    // -----------------------------------------------------------------------

    /**
     * @param mamCaInstallReferrerEnabled whether the host has opted this client in to MAM-CA
     *                                    install-referrer decoration.
     * @return a client configured exactly like {@link #mWebViewClient} apart from the opt-in.
     */
    private AzureActiveDirectoryWebViewClient mamCaWebViewClient(
            final boolean mamCaInstallReferrerEnabled) {
        final AzureActiveDirectoryWebViewClient client = new AzureActiveDirectoryWebViewClient(
                mActivity,
                new IAuthorizationCompletionCallback() {
                    @Override
                    public void onChallengeResponseReceived(@NonNull RawAuthorizationResult response) {
                    }

                    @Override
                    public void setPKeyAuthStatus(boolean status) {
                    }
                },
                new OnPageLoadedCallback() {
                    @Override
                    public void onPageLoaded(final String url) {
                    }
                },
                TEST_REDIRECT_URI,
                Mockito.mock(SwitchBrowserProtocolCoordinator.class),
                "homeTenantId",
                false,
                mamCaInstallReferrerEnabled,
                null);
        client.setRequestUrl(TEST_PUBLIC_CLOUD_REDIRECT_URL);
        return client;
    }

    /**
     * Drives a broker-install redirect and returns the install {@link Intent} that was launched.
     * <p>
     * The launch is deliberately posted a second out so the calling activity can register its
     * broker-result receiver first, so the main looper has to be advanced past that delay before
     * the intent exists. Robolectric pauses the looper by default, which is what makes this
     * observable at all.
     *
     * @param client             the WebView client to drive.
     * @param installRedirectUrl the {@code msauth://wpj} redirect to feed the WebView client.
     * @return the launched install intent, or null if none was launched.
     */
    private Intent launchInstallAndCaptureIntent(final AzureActiveDirectoryWebViewClient client,
                                                 final String installRedirectUrl) {
        assertTrue("A broker-install redirect must be handled by the WebView client",
                client.shouldOverrideUrlLoading(mMockWebView, installRedirectUrl));
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(2));
        return Shadows.shadowOf(mActivity).getNextStartedActivity();
    }

    /**
     * The feature itself: a server-marked MAM-CA install is launched with the calling app named as
     * the Play install referrer, which is what lets Company Portal skip its own sign-in UX and
     * redirect back here after install.
     */
    @Test
    public void testProcessInstallRequest_mamCaInstall_tagsCallingAppAsInstallReferrer() {
        final Intent launched = launchInstallAndCaptureIntent(
                mamCaWebViewClient(true), TEST_MAM_CA_INSTALL_REQUEST_URL);

        assertNotNull("Expected the Company Portal install link to be launched", launched);
        assertEquals(Intent.ACTION_VIEW, launched.getAction());
        assertTrue("A MAM-CA install must name the calling app as the install referrer, but was: "
                        + launched.getDataString(),
                launched.getDataString().contains(
                        MamInstallReferrerBuilder.REFERRER_QUERY_PARAM + "=" + mActivity.getPackageName()));
    }

    /**
     * The host opt-in is a complete kill switch: without it the install link is launched
     * byte-for-byte as the server sent it. This is also what every host that never sets it - MSAL,
     * and the broker process - gets.
     */
    @Test
    public void testProcessInstallRequest_mamCaInstall_hostDidNotOptIn_leavesLinkUnchanged() {
        final Intent launched = launchInstallAndCaptureIntent(
                mamCaWebViewClient(false), TEST_MAM_CA_INSTALL_REQUEST_URL);

        assertNotNull("The install must still be launched without the opt-in", launched);
        assertFalse("Without the opt-in nothing may be appended to the install link, but was: "
                        + launched.getDataString(),
                launched.getDataString().contains(MamInstallReferrerBuilder.REFERRER_QUERY_PARAM + "="));
    }

    /**
     * The same broker-install redirect also drives ordinary device-registration installs. Without
     * the server's MAM-CA marker those must keep their existing, undecorated behavior.
     */
    @Test
    public void testProcessInstallRequest_notMamCaInstall_leavesLinkUnchanged() {
        final Intent launched = launchInstallAndCaptureIntent(
                mamCaWebViewClient(true), TEST_PLAIN_INSTALL_REQUEST_URL);

        assertNotNull("A device-registration install must still be launched", launched);
        assertFalse("An unmarked install must not be tagged as MAM-CA, but was: "
                        + launched.getDataString(),
                launched.getDataString().contains(MamInstallReferrerBuilder.REFERRER_QUERY_PARAM + "="));
    }

    /**
     * The decoration is a fallback, not an override. The server names the calling app directly,
     * whereas the client can only ever name the process hosting the sign-in UI, so where the two
     * disagree the server wins.
     */
    @Test
    public void testProcessInstallRequest_serverSuppliedReferrer_isNotOverridden() {
        final Intent launched = launchInstallAndCaptureIntent(
                mamCaWebViewClient(true), TEST_MAM_CA_INSTALL_REQUEST_URL_SERVER_REFERRER);

        assertNotNull(launched);
        final String launchedLink = launched.getDataString();
        assertTrue("A referrer the server already set must be preserved, but was: " + launchedLink,
                launchedLink.contains(
                        MamInstallReferrerBuilder.REFERRER_QUERY_PARAM + "=" + SERVER_SUPPLIED_REFERRER));
        assertFalse("The client must not add a second referrer, but was: " + launchedLink,
                launchedLink.contains(
                        MamInstallReferrerBuilder.REFERRER_QUERY_PARAM + "=" + mActivity.getPackageName()));
    }

    /**
     * A {@code browser://}-prefixed app_link never reaches the install launch at all:
     * {@code BrokerInstallLinkValidator} requires the scheme to be exactly https, so the redirect is
     * not classified as a broker install and nothing is started. This pins the allowlist as the gate
     * in front of the install launch, and makes the {@code browser://} rewrite further down
     * demonstrably unreachable for app_links.
     */
    @Test
    public void testProcessInstallRequest_browserPrefixedAppLink_isRejectedByTheAllowlist() {
        final Intent launched = launchInstallAndCaptureIntent(
                mamCaWebViewClient(true), TEST_MAM_CA_INSTALL_REQUEST_URL_BROWSER_PREFIX);

        assertNull("An app_link that is not https must never be launched, but was: "
                        + (launched == null ? "" : launched.getDataString()),
                launched);
    }
}

