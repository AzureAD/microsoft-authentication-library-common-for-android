package com.microsoft.identity.common.internal.providers.oauth2;

import android.util.Base64;

import org.json.JSONException;

import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Map;

import com.microsoft.identity.common.adal.internal.util.JsonExtensions;
import com.microsoft.identity.common.adal.internal.util.StringExtensions;

/**
 * Represents the ID Token returned from the oAuth Authorization Server / OpenID Connect Provider
 */
public class IDToken {


    private final Map<String, String> mTokenClaims = null;
    private final String mRawIdToken;

    public IDToken(final String rawIdToken) {

        if (StringExtensions.isNullOrBlank(rawIdToken)) {
            throw new IllegalArgumentException("null or empty raw idtoken");
        }

        mRawIdToken = rawIdToken;

        // set all the instance variables.
        final Map<String, String> mTokenClaims = parseJWT(rawIdToken);
    }


    public Map<String, String> getTokenClaims(){
        return Collections.unmodifiableMap(mTokenClaims);
    }

    private Map<String, String> parseJWT(final String idToken) {
        final String idTokenBody = extractJWTBody(idToken);
        final byte[] data = Base64.decode(idTokenBody, Base64.URL_SAFE);

        try {
            final String decodedBody = new String(data, Charset.forName(StringExtensions.ENCODING_UTF8));
            return JsonExtensions.extractJsonObjectIntoMap(decodedBody);
        } catch (final JSONException e) {
            throw new RuntimeException(e);
        }
    }

    private String extractJWTBody(final String idToken) {
        final int firstDot = idToken.indexOf(".");
        final int secondDot = idToken.indexOf(".", firstDot + 1);
        final int invalidDot = idToken.indexOf(".", secondDot + 1);

        if (invalidDot == -1 && firstDot > 0 && secondDot > 0) {
            return idToken.substring(firstDot + 1, secondDot);
        } else {
            throw new IllegalArgumentException("Invalid ID token format.");
        }
    }
}
