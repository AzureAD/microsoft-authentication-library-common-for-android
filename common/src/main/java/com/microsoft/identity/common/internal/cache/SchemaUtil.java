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
package com.microsoft.identity.common.internal.cache;

import com.microsoft.identity.common.adal.internal.util.StringExtensions;
import com.microsoft.identity.common.internal.dto.Account;
import com.microsoft.identity.common.internal.dto.Credential;
import com.microsoft.identity.common.internal.logging.Logger;
import com.microsoft.identity.common.internal.providers.microsoft.MicrosoftIdToken;
import com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectory.ClientInfo;
import com.microsoft.identity.common.internal.providers.oauth2.IDToken;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

/**
 * Utility class for performing common actions needed for the common cache schema.
 */
public final class SchemaUtil {

    private static final String TAG = SchemaUtil.class.getSimpleName();

    private SchemaUtil() {
        // Utility class.
    }

    /**
     * Returns the authority (issuer) for the supplied IDToken.
     *
     * @param idToken The IDToken to parse.
     * @return The issuer or null if the IDToken cannot be parsed or the issuer claim is empty.
     */
    public static String getAuthority(final IDToken idToken) {
        final String methodName = "getAuthority";

        String issuer = null;

        if (null != idToken) {
            final Map<String, String> idTokenClaims = idToken.getTokenClaims();

            if (null != idTokenClaims) {
                issuer = idTokenClaims.get(MicrosoftIdToken.ISSUER);
                Logger.verbosePII(TAG + ":" + methodName, "Issuer: " + issuer);

                if (null == issuer) {
                    Logger.warn(TAG + ":" + methodName, "Environment was null or could not be parsed.");
                }
            } else {
                Logger.warn(TAG + ":" + methodName, "IDToken claims were null");
            }
        } else {
            Logger.warn(TAG + ":" + methodName, "IDToken was null");
        }

        return issuer;
    }

    /**
     * Returns the 'environment' for the supplied IDToken.
     * <p>
     * For a description of this field,
     * see {@link Account#getEnvironment()}, {@link Credential#getEnvironment()}
     *
     * @param idToken The IDToken to parse.
     * @return The environment or null if the IDToken cannot be parsed, the issuer claim is empty
     * or contains an invalid URL.
     */
    public static String getEnvironment(final IDToken idToken) {
        final String methodName = "getEnvironment";

        final String issuer = getAuthority(idToken);
        String environment = null;
        try {
            environment = new URL(issuer).getHost();
        } catch (MalformedURLException e) {
            environment = null;
            Logger.error(
                    TAG + ":" + methodName,
                    "Failed to construct URL from issuer claim",
                    null // Do not supply the Exception, as it contains PII
            );
            Logger.errorPII(TAG + ":" + methodName, "Failed with Exception", e);
        }

        return environment;
    }

    /**
     * Returns the 'avatar url' for the supplied IDToken.
     *
     * @param idToken The IDToken to parse.
     * @return The environment or null if the IDToken cannot be parsed or the picture claim is empty.
     */
    public static String getAvatarUrl(final IDToken idToken) {
        final String methodName = "getAvatarUrl";

        String avatarUrl = null;

        if (null != idToken) {
            final Map<String, String> idTokenClaims = idToken.getTokenClaims();

            if (null != idTokenClaims) {
                avatarUrl = idTokenClaims.get(IDToken.PICTURE);

                Logger.verbosePII(TAG + ":" + methodName, "Avatar URL: " + avatarUrl);

                if (null == avatarUrl) {
                    Logger.warn(TAG + ":" + methodName, "Avatar URL was null.");
                }
            } else {
                Logger.warn(TAG + ":" + methodName, "IDToken claims were null.");
            }
        } else {
            Logger.warn(TAG + ":" + methodName, "IDToken was null.");
        }

        return avatarUrl;
    }

    /**
     * Returns the 'alternative_account_id' for the supplied IDToken.
     *
     * @param idToken The IDToken to parse.
     * @return The alternative_account_id or null if the IDToken cannot be parsed or the altsecid
     * claim is empty.
     */
    public static String getAlternativeAccountId(final IDToken idToken) {
        final String methodName = "getAlternativeAccountId";

        String alternativeAccountId = null;

        if (null != idToken) {
            final Map<String, String> idTokenClaims = idToken.getTokenClaims();

            if (null != idTokenClaims) {
                alternativeAccountId = idTokenClaims.get("altsecid");

                Logger.verbosePII(TAG + ":" + methodName, "alternative_account_id: " + alternativeAccountId);

                if (null == alternativeAccountId) {
                    Logger.warn(TAG + ":" + methodName, "alternative_account_id was null.");
                }
            } else {
                Logger.warn(TAG + ":" + methodName, "IDToken claims were null.");
            }
        } else {
            Logger.warn(TAG + ":" + methodName, "IDToken was null.");
        }

        return alternativeAccountId;
    }

    /**
     * Get the home account id with the client info.
     *
     * @param clientInfo ClientInfo
     * @return String home account id
     */
    public static String getHomeAccountId(final ClientInfo clientInfo) {
        final String methodName = ":getHomeAccountId";

        String homeAccountId = null;

        if (null != clientInfo) {
            final String uid = clientInfo.getUid();
            final String utid = clientInfo.getUtid();

            if (StringExtensions.isNullOrBlank(uid)) {
                Logger.warn(TAG + ":" + methodName, "uid was null/blank");
            }

            if (StringExtensions.isNullOrBlank(utid)) {
                Logger.warn(TAG + ":" + methodName, "utid was null/blank");
            }

            if (!StringExtensions.isNullOrBlank(uid) && !StringExtensions.isNullOrBlank(utid)) {
                homeAccountId = uid + "." + utid;
            }

            Logger.verbosePII(TAG + ":" + methodName, "home_account_id: " + homeAccountId);

        } else {
            Logger.warn(TAG + ":" + methodName, "ClientInfo was null.");
        }

        return homeAccountId;
    }
}
