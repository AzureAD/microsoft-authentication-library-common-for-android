package com.microsoft.identity.common.internal.providers.microsoft;

import com.microsoft.identity.common.internal.providers.oauth2.IDToken;

public class MicrosoftIdToken extends IDToken {

    /**
     * Identifies the intended recipient of the token. In ID tokens, the audience is your app's
     * Application ID, assigned to your app in the Microsoft Application Registration Portal.
     * Your app should validate this value, and reject the token if the value does not match.
     */
    public static final String AUDIENCE = "aud";

    /**
     * Identifies the security token service (STS) that constructs and returns the token, and the
     * Azure AD tenant in which the user was authenticated. Your app should validate the issuer
     * claim to ensure that the token came from the v2.0 endpoint. It also should use the GUID
     * portion of the claim to restrict the set of tenants that can sign in to the app. The GUID
     * that indicates that the user is a consumer user from a Microsoft account is
     * 9188040d-6c67-4c5b-b112-36a304b66dad
     */
    public static final String ISSUER = "iss";

    /**
     * The time at which the token was issued, represented in epoch time.
     */
    public static final String ISSUED_AT = "iat";

    /**
     * The time at which the token becomes valid, represented in epoch time. It is usually the same
     * as the issuance time. Your app should use this claim to verify the validity of the token
     * lifetime.
     */
    public static final String NOT_BEFORE = "nbf";

    /**
     * The immutable identifier for an object in the Microsoft identity system, in this case, a
     * user account. It can also be used to perform authorization checks safely and as a key in
     * database tables. This ID uniquely identifies the user across applications - two different
     * applications signing in the same user will receive the same value in the oid claim. This
     * means that it can be used when making queries to Microsoft online services, such as the
     * Microsoft Graph. The Microsoft Graph will return this ID as the id property for a given
     * user account. Because the oid allows multiple apps to correlate users, the profile scope is
     * required in order to receive this claim. Note that if a single user exists in multiple
     * tenants, the user will contain a different object ID in each tenant - they are considered
     * different accounts, even though the user logs into each account with the same credentials.
     */
    public static final String OJBECT_ID = "oid";

    /**
     * A GUID that represents the Azure AD tenant that the user is from. For work and school
     * accounts, the GUID is the immutable tenant ID of the organization that the user belongs to.
     * For personal accounts, the value is 9188040d-6c67-4c5b-b112-36a304b66dad. The profile scope
     * is required in order to receive this claim.
     */
    public static final String TENANT_ID = "tid";

    /**
     * The version of the ID token, as defined by Azure AD. For the v2.0 endpoint, the value is 2.0.
     */
    public static final String VERSION = "ver";

    public MicrosoftIdToken(String rawIdToken) {
        super(rawIdToken);
    }
}
