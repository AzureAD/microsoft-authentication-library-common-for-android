package com.microsoft.identity.common.internal.providers.oauth2;

/**
 * The response type values defined by the
 * [OAuth 2.0](https://tools.ietf.org/html/rfc6749) and
 * [OpenID Connect Core 1.0](http://openid.net/specs/openid-connect-core-1_0.html)
 **/

public final class ResponseType {
    /**
     * For requesting an authorization code.
     *
     * @see "The OAuth 2.0 Authorization Framework (RFC 6749), Section 3.1.1
     * <https://tools.ietf.org/html/rfc6749#section-3.1.1>"
     */
    public static final String CODE = "code";

    /**
     * For requesting an access token via an implicit grant.
     *
     * @see "The OAuth 2.0 Authorization Framework (RFC 6749), Section 3.1.1
     * <https://tools.ietf.org/html/rfc6749#section-3.1.1>"
     */
    public static final String TOKEN = "token";

    /**
     * For requesting an OpenID Conenct ID Token.
     *
     * @see "The OAuth 2.0 Authorization Framework (RFC 6749), Section 3.1.1
     * <https://tools.ietf.org/html/rfc6749#section-3.1.1>"
     */
    public static final String ID_TOKEN = "id_token";
}
