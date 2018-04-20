package com.microsoft.identity.common.internal.cache;

import com.microsoft.identity.common.internal.dto.Account;
import com.microsoft.identity.common.internal.dto.Credential;
import com.microsoft.identity.common.internal.logging.Logger;
import com.microsoft.identity.common.internal.providers.microsoft.MicrosoftIdToken;
import com.microsoft.identity.common.internal.providers.oauth2.IDToken;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

/**
 * Utility class for performing common actions needed for the common cache schema.
 */
public class SchemaUtil {

    private static final String TAG = SchemaUtil.class.getSimpleName();

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
        String environment = null;

        if (null != idToken) {
            final Map<String, String> idTokenClaims = idToken.getTokenClaims();

            if (null != idTokenClaims) {
                environment = idTokenClaims.get(MicrosoftIdToken.ISSUER);
                Logger.verbosePII(TAG + ":" + methodName, null, "Issuer: " + environment);

                try {
                    environment = new URL(environment).getHost();
                } catch (MalformedURLException e) {
                    environment = null;
                    Logger.error(
                            TAG + ":" + methodName,
                            null,
                            "Failed to construct URL from issuer claim",
                            null // Do not supply the Exception, as it contains PII
                    );
                    Logger.errorPII(
                            TAG + ":" + methodName,
                            null,
                            "Failed with Exception",
                            e
                    );
                }

                Logger.verbosePII(
                        TAG + ":" + methodName,
                        null,
                        "Environment: " + environment
                );

                if (null == environment) {
                    Logger.warn(
                            TAG + ":" + methodName,
                            null,
                            "Environment was null or could not be parsed."
                    );
                }
            } else {
                Logger.warn(
                        TAG + ":" + methodName,
                        null,
                        "IDToken claims were null"
                );
            }
        } else {
            Logger.warn(
                    TAG + ":" + methodName,
                    null,
                    "IDToken was null"
            );
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

                Logger.verbosePII(
                        TAG + ":" + methodName,
                        null,
                        "Avatar URL: " + avatarUrl
                );

                if (null == avatarUrl) {
                    Logger.warn(
                            TAG + ":" + methodName,
                            null,
                            "Avatar URL was null."
                    );
                }
            } else {
                Logger.warn(
                        TAG + ":" + methodName,
                        null,
                        "IDToken claims were null."
                );
            }
        } else {
            Logger.warn(
                    TAG + ":" + methodName,
                    null,
                    "IDToken was null."
            );
        }

        return avatarUrl;
    }

    /**
     * Returns the 'guest_id' for the supplied IDToken.
     *
     * @param idToken The IDToken to parse.
     * @return The guestId or null if the IDToken cannot be parsed or the altsecid claim is empty.
     */
    public static String getGuestId(final IDToken idToken) {
        final String methodName = "getGuestId";
        String guestId = null;

        if (null != idToken) {
            final Map<String, String> idTokenClaims = idToken.getTokenClaims();

            if (null != idTokenClaims) {
                guestId = idTokenClaims.get("altsecid");

                Logger.verbosePII(
                        TAG + ":" + methodName,
                        null,
                        "Guest Id: " + guestId
                );

                if (null == guestId) {
                    Logger.warn(
                            TAG + ":" + methodName,
                            null,
                            "Guest Id was null."
                    );
                }
            } else {
                Logger.warn(
                        TAG + ":" + methodName,
                        null,
                        "IDToken claims were null."
                );
            }
        } else {
            Logger.warn(
                    TAG + ":" + methodName,
                    null,
                    "IDToken was null."
            );
        }

        return guestId;
    }
}
