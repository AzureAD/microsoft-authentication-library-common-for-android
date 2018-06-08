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
package com.microsoft.identity.common.internal.providers.microsoft.microsoftsts;

import android.os.Build;
import android.util.Base64;

import com.microsoft.identity.common.adal.internal.AuthenticationConstants;
import com.microsoft.identity.common.adal.internal.util.StringExtensions;
import com.microsoft.identity.common.exception.ClientException;
import com.microsoft.identity.common.exception.ErrorStrings;
import com.microsoft.identity.common.internal.logging.Logger;
import com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectory.AzureActiveDirectoryAuthorizationRequest;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationRequest;
import com.microsoft.identity.common.internal.ui.AuthorizationConfiguration;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static android.content.ContentValues.TAG;

public class MicrosoftStsAuthorizationRequest extends AuthorizationRequest {
    private String mAuthorizationEndpoint;
    private final Set<String> mScope = new HashSet<>();
    private final Set<String> mExtraScopesToConsent = new HashSet<>();
    private UUID mCorrelationId; //nullable
    private String mLoginHint;
    private String mUid;
    private String mUtid;
    private String mDisplayableId;

    private MicrosoftStsPromptBehavior mPromptBehavior;
    private PKCEChallengeFactory.PKCEChallenge mPKCEChallenge;
    private String mExtraQueryParam;
    private String mSliceParameters;
    private String mAuthority;

    public UUID getCorrelationId() {
        return mCorrelationId;
    }

    public void setCorrelationId(final UUID correlationId) {
        mCorrelationId = correlationId;
    }


    public String getAuthorizationStartUrl() throws UnsupportedEncodingException, ClientException{
        if(AuthorizationConfiguration.getInstance().isBrokerRequest()) {
            return getBrokerAuthorizationStartUrl();
        } else {
            return getLocalAuthorizationStartUrl();
        }
    }

    private String getBrokerAuthorizationStartUrl() {
        throw new UnsupportedOperationException("Not implemented.");

    }

    private String getLocalAuthorizationStartUrl() throws UnsupportedEncodingException, ClientException {
        String authorizationUrl = StringExtensions.appendQueryParameterToUrl(
                mAuthorizationEndpoint,
                createAuthorizationRequestParameters());
        Logger.infoPII(TAG, "", "Request uri to authorize endpoint is: " + authorizationUrl);
        return authorizationUrl;
    }

    public String getAuthorizationEndpoint() {
        return mAuthorizationEndpoint;
    }

    public void setAuthorizationEndpoint(final String authorizationEndpoint) {
        mAuthorizationEndpoint = authorizationEndpoint;
    }

    private Map<String, String> createAuthorizationRequestParameters() throws UnsupportedEncodingException, ClientException {
        final Map<String, String> requestParameters = new HashMap<>();

        final Set<String> scopes = new HashSet<>(mScope);
        scopes.addAll(mExtraScopesToConsent);
        final Set<String> requestedScopes = getDecoratedScope(scopes);
        requestParameters.put(AuthenticationConstants.OAuth2.SCOPE,
                StringExtensions.convertSetToString(requestedScopes, " "));
        requestParameters.put(AuthenticationConstants.OAuth2.CLIENT_ID, getClientId());
        requestParameters.put(AuthenticationConstants.OAuth2.REDIRECT_URI, getRedirectUri());
        requestParameters.put(AuthenticationConstants.OAuth2.RESPONSE_TYPE, AuthenticationConstants.OAuth2.CODE);
        requestParameters.put(AuthenticationConstants.AAD.CORRELATION_ID, mCorrelationId.toString());

        requestParameters.put(AuthenticationConstants.AAD.ADAL_ID_PLATFORM, AuthenticationConstants.MSSTS.PLATFORM_VALUE);
        requestParameters.put(AuthenticationConstants.AAD.ADAL_ID_VERSION, AuthenticationConstants.MSSTS.MSAL_ID_VERSION);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            requestParameters.put(AuthenticationConstants.AAD.ADAL_ID_CPU, Build.CPU_ABI);
        } else {
            final String[] supportedABIs = Build.SUPPORTED_ABIS;
            if (supportedABIs != null && supportedABIs.length > 0) {
                requestParameters.put(AuthenticationConstants.AAD.ADAL_ID_CPU, supportedABIs[0]);
            }
        }
        requestParameters.put(AuthenticationConstants.AAD.ADAL_ID_OS_VER, String.valueOf(Build.VERSION.SDK_INT));
        requestParameters.put(AuthenticationConstants.AAD.ADAL_ID_DM, Build.MODEL);

        if (!StringExtensions.isNullOrBlank(mLoginHint)) {
            requestParameters.put(AuthenticationConstants.AAD.LOGIN_HINT, mLoginHint);
        }

        if (mPromptBehavior == MicrosoftStsPromptBehavior.FORCE_LOGIN) {
            requestParameters.put(AuthenticationConstants.AAD.QUERY_PROMPT, AuthenticationConstants.MSSTS.PromptValue.LOGIN);
        } else if (mPromptBehavior == MicrosoftStsPromptBehavior.SELECT_ACCOUNT) {
            requestParameters.put(AuthenticationConstants.AAD.QUERY_PROMPT,AuthenticationConstants.MSSTS.PromptValue.SELECT_ACCOUNT);
        } else if (mPromptBehavior == MicrosoftStsPromptBehavior.CONSENT) {
            requestParameters.put(AuthenticationConstants.AAD.QUERY_PROMPT, AuthenticationConstants.MSSTS.PromptValue.CONSENT);
        }

        // append state in the query parameters
        requestParameters.put(AuthenticationConstants.OAuth2.STATE, encodeProtocolState());

        // Add PKCE Challenge
        addPKCEChallengeToRequestParameters(requestParameters);

        // Enforce session continuation if user is provided in the API request
        //TODO can wrap the user info into User class object.
        addExtraQueryParameter(AuthenticationConstants.MSSTS.LOGIN_REQ, mUid, requestParameters);
        addExtraQueryParameter(AuthenticationConstants.MSSTS.DOMAIN_REQ, mUtid, requestParameters);
        addExtraQueryParameter(AuthenticationConstants.MSSTS.LOGIN_HINT, mDisplayableId, requestParameters);

        // adding extra qp
        if (!StringExtensions.isNullOrBlank(mExtraQueryParam)) {
            appendExtraQueryParameters(mExtraQueryParam, requestParameters);
        }

        if (!StringExtensions.isNullOrBlank(mSliceParameters)) {
            appendExtraQueryParameters(mExtraQueryParam, requestParameters);
        }

        return requestParameters;
    }

    private String encodeProtocolState() throws UnsupportedEncodingException {
        final String state = String.format("a=%s&r=%s", StringExtensions.urlFormEncode(
                mAuthority),
                StringExtensions.urlFormEncode(StringExtensions.convertSetToString(
                        mScope, " ")));
        return Base64.encodeToString(state.getBytes("UTF-8"), Base64.NO_PADDING | Base64.URL_SAFE);
    }

    private void addExtraQueryParameter(final String key, final String value, final Map<String, String> requestParams) {
        if (!StringExtensions.isNullOrBlank(key) && !StringExtensions.isNullOrBlank(value)) {
            requestParams.put(key, value);
        }
    }

    private void appendExtraQueryParameters(final String queryParams, final Map<String, String> requestParams) throws ClientException {
        final Map<String, String> extraQps = StringExtensions.decodeUrlToMap(queryParams, "&");
        final Set<Map.Entry<String, String>> extraQpEntries = extraQps.entrySet();
        for (final Map.Entry<String, String> extraQpEntry : extraQpEntries) {
            if (requestParams.containsKey(extraQpEntry.getKey())) {
                throw new ClientException(ErrorStrings.DUPLICATE_QUERY_PARAMETER,
                        "Extra query parameter " + extraQpEntry.getKey() + " is already sent by "
                        + "the SDK. ");
            }

            requestParams.put(extraQpEntry.getKey(), extraQpEntry.getValue());
        }
    }

    /**
     * Get the decorated scopes. Will combine the input scope and the reserved scope. If client id is provided as scope,
     * it will be removed from the combined scopes.
     *
     * @param inputScopes The input scopes to decorate.
     * @return The combined scopes.
     */
    Set<String> getDecoratedScope(final Set<String> inputScopes) {
        final Set<String> scopes = new HashSet<>(inputScopes);
        final Set<String> reservedScopes = getReservedScopesAsSet();
        scopes.addAll(reservedScopes);
        scopes.remove(getClientId());

        return scopes;
    }

    private Set<String> getReservedScopesAsSet() {
        return new HashSet<>(Arrays.asList(AuthenticationConstants.MSSTS.RESERVED_SCOPES));
    }

    private void addPKCEChallengeToRequestParameters(final Map<String, String> requestParameters) throws ClientException {
        // Create our Challenge
        mPKCEChallenge = PKCEChallengeFactory.newPKCEChallenge();

        // Add it to our Authorization request
        requestParameters.put(AuthenticationConstants.MSSTS.CODE_CHALLENGE, mPKCEChallenge.mCodeChallenge);
        requestParameters.put(AuthenticationConstants.MSSTS.CODE_CHALLENGE_METHOD, PKCEChallengeFactory.PKCEChallenge.ChallengeMethod.S256.name());
    }

    /**
     * Factory class for PKCE Challenges.
     */
    private static class PKCEChallengeFactory {
        private static final int CODE_VERIFIER_BYTE_SIZE = 32;
        private static final int ENCODE_MASK = Base64.URL_SAFE | Base64.NO_PADDING | Base64.NO_WRAP;
        private static final String DIGEST_ALGORITHM = "SHA-256";
        private static final String ISO_8859_1 = "ISO_8859_1";

        static class PKCEChallenge {

            /**
             * The client creates a code challenge derived from the code
             * verifier by using one of the following transformations.
             * <p>
             * Sophisticated attack scenarios allow the attacker to
             * observe requests (in addition to responses) to the
             * authorization endpoint.  The attacker is, however, not able to
             * act as a man in the middle. To mitigate this,
             * "code_challenge_method" value must be set either to "S256" or
             * a value defined by a cryptographically secure
             * "code_challenge_method" extension. In this implementation "S256" is used.
             * <p>
             * Example for the S256 code_challenge_method
             *
             * @see <a href="https://tools.ietf.org/html/rfc7636#page-17">RFC-7636</a>
             */
            enum ChallengeMethod {
                S256
            }

            /**
             * A cryptographically random string that is used to correlate the
             * authorization request to the token request.
             * <p>
             * code-verifier = 43*128unreserved
             * where...
             * unreserved = ALPHA / DIGIT / "-" / "." / "_" / "~"
             * ALPHA = %x41-5A / %x61-7A
             * DIGIT = %x30-39
             */
            private final String mCodeVerifier;

            /**
             * A challenge derived from the code verifier that is sent in the
             * authorization request, to be verified against later.
             */
            private final String mCodeChallenge;

            PKCEChallenge(String codeVerifier, String codeChallenge) {
                this.mCodeVerifier = codeVerifier;
                this.mCodeChallenge = codeChallenge;
            }
        }

        /**
         * Creates a new instance of {@link PKCEChallenge}.
         *
         * @return the newly created Challenge
         */
        static PKCEChallenge newPKCEChallenge() throws ClientException{
            // Generate the code_verifier as a high-entropy cryptographic random String
            final String codeVerifier = generateCodeVerifier();

            // Create a code_challenge derived from the code_verifier
            final String codeChallenge = generateCodeVerifierChallenge(codeVerifier);

            return new PKCEChallenge(codeVerifier, codeChallenge);
        }

        private static String generateCodeVerifier() {
            final byte[] verifierBytes = new byte[CODE_VERIFIER_BYTE_SIZE];
            new SecureRandom().nextBytes(verifierBytes);
            return Base64.encodeToString(verifierBytes, ENCODE_MASK);
        }

        private static String generateCodeVerifierChallenge(final String verifier) throws ClientException {
            try {
                MessageDigest digester = MessageDigest.getInstance(DIGEST_ALGORITHM);
                digester.update(verifier.getBytes(ISO_8859_1));
                byte[] digestBytes = digester.digest();
                return Base64.encodeToString(digestBytes, ENCODE_MASK);
            } catch (final NoSuchAlgorithmException e) {
                throw new ClientException(ErrorStrings.NO_SUCH_ALGORITHM, "Failed to generate the code verifier challenge", e);
            } catch (final UnsupportedEncodingException e) {
                throw new ClientException(ErrorStrings.UNSUPPORTED_ENCODING,
                        "Every implementation of the Java platform is required to support ISO-8859-1."
                                + "Consult the release documentation for your implementation.", e);
            }
        }
    }

}
