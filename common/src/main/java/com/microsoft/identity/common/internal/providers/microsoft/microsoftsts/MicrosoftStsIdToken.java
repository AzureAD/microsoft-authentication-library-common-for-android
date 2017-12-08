package com.microsoft.identity.common.internal.providers.microsoft.microsoftsts;

import com.microsoft.identity.common.internal.providers.microsoft.MicrosoftIdToken;

/**
 * IdToken claims emitted by the Microsoft STS (V2).
 *
 * @see <a href="https://docs.microsoft.com/en-us/azure/active-directory/develop/active-directory-v2-tokens">Azure Active Directory v2.0 tokens reference</a>
 */
public class MicrosoftStsIdToken extends MicrosoftIdToken {
    /**
     * The time at which the token becomes invalid, represented in epoch time. Your app should use
     * this claim to verify the validity of the token lifetime.
     */
    public static final String EXPIRATION_TIME = "exp";

    // TODO Could not locate documentation for the following fields.
    public static final String AIO = "aio";
    public static final String UTI = "uti";

    public MicrosoftStsIdToken(String rawIdToken) {
        super(rawIdToken);
    }
}
