package com.microsoft.identity.common.internal.providers.microsoft.microsoftsts;

import android.os.Build;
import android.text.TextUtils;
import android.util.Base64;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.microsoft.identity.common.adal.internal.AuthenticationConstants;
import com.microsoft.identity.common.adal.internal.util.StringExtensions;
import com.microsoft.identity.common.exception.ClientException;
import com.microsoft.identity.common.internal.logging.Logger;
import com.microsoft.identity.common.internal.net.ObjectMapper;
import com.microsoft.identity.common.internal.platform.JweResponse;
import com.microsoft.identity.common.internal.platform.KeyAccessor;
import com.microsoft.identity.common.internal.platform.RawKeyAccessor;
import com.microsoft.identity.common.internal.platform.SymmetricCipher;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2StrategyParameters;
import com.microsoft.identity.common.internal.providers.oauth2.TokenRequest;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.SignedJWT;

import org.json.JSONException;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

/**
 * In addition to the normal requests that we handle, some of these need to use a specific
 * kind of strategy that encoses the token request in a JOSE object inside of a form post.
 */
public class MicrosoftStsOauth2JWEStrategy extends MicrosoftStsOAuth2Strategy {

    private final String TAG = MicrosoftStsOauth2JWEStrategy.class.getSimpleName();

    private final KeyAccessor mSessionKey;
    public static final Gson GSON = new Gson();
    private static final SecureRandom random;
    static {
        SecureRandom tmpRandom;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                tmpRandom = SecureRandom.getInstanceStrong();
            } catch (final NoSuchAlgorithmException e) {
                tmpRandom = new SecureRandom();
            }
        } else {
            tmpRandom = new SecureRandom();
        }
        random = tmpRandom;
    }


    /**
     * Constructor of MicrosoftStsOAuth2Strategy.
     *
     * @param config     MicrosoftStsOAuth2Configuration
     * @param parameters OAuth2StrategyParameters
     */
    public MicrosoftStsOauth2JWEStrategy(@NonNull final MicrosoftStsOAuth2Configuration config,
                                         @NonNull final OAuth2StrategyParameters parameters,
                                         @NonNull final KeyAccessor sessionKey) {
        super(config, parameters);
        setTokenEndpoint(config.getTokenEndpoint().toString());
        mSessionKey = sessionKey;
    }

    /**
     * Helper method to decrypt the access token response using the derived Session key.
     */
    public byte[] decryptUsingDerivedSessionKey(@androidx.annotation.NonNull final byte[] ivBytes,
                                                @androidx.annotation.NonNull final byte[] ctx,
                                                @androidx.annotation.NonNull final byte[] encryptedBytes,
                                                RawKeyAccessor skAccessor)
            throws ClientException {
        byte[] label = AuthenticationConstants.SP800_108_LABEL.getBytes(Charset.forName("ASCII"));
        KeyAccessor derivedKey = skAccessor.generateDerivedKey(label, ctx, SymmetricCipher.AES_GCM_NONE_HMACSHA256);

        byte[] cryptobuf = new byte[ivBytes.length + encryptedBytes.length];
        System.arraycopy(ivBytes, 0, encryptedBytes, 0, ivBytes.length);
        System.arraycopy(encryptedBytes, 0, encryptedBytes, ivBytes.length, encryptedBytes.length);
        return derivedKey.decrypt(cryptobuf);
    }


    /**
     * Helper method to decrypt JWE token response
     *
     * @param jwe : response body in JWE format
     * @return
     * @throws JSONException
     * @throws UnsupportedEncodingException
     * @throws ClientException
     */
    public String decryptTokenResponse(@androidx.annotation.NonNull final String jwe,
                                       @lombok.NonNull final KeyAccessor skAccessor)
            throws JSONException, ClientException {

        final JweResponse jweResponse = JweResponse.parseJwe(jwe);
        if (skAccessor instanceof RawKeyAccessor) {
            throw new UnsupportedOperationException("This accessor is not currently supported.");
        }

        if (!jweResponse.getJweHeader().mHeaderEncryptionAlgorithm.equalsIgnoreCase("A256GCM")
                && !jweResponse.getJweHeader().mHeaderEncryptionAlgorithm.equalsIgnoreCase("dir")) {
            throw new IllegalArgumentException("Invalid encryption algorithm");
        }

        // NOTE: EVOsts sends mIv and mPayload as Base64UrlEncoded
        final byte[] ivDecoded = Base64.decode(jweResponse.getIV(), Base64.URL_SAFE);
        final byte[] payloadCipherText = Base64.decode(jweResponse.getPayload(), Base64.URL_SAFE);

        // CTX is inside the mJweHeader and comes as Base64 not base64urlencode
        final byte[] derivedKeyCtx = Base64.decode(
                jweResponse.getJweHeader().mHeaderContext,
                Base64.DEFAULT
        );

        com.microsoft.identity.common.internal.logging.Logger.verbose(TAG,
                "Decrypting the token response for using PRT. IV size:"
                        + ivDecoded.length
                        + " mPayload size:"
                        + payloadCipherText.length
                        + " ctx size:"
                        + derivedKeyCtx.length
        );

        final byte[] decryptedData = decryptUsingDerivedSessionKey(
                ivDecoded,
                derivedKeyCtx,
                payloadCipherText,
                ((RawKeyAccessor) skAccessor)
        );

        return new String(decryptedData, AuthenticationConstants.CHARSET_UTF8);
    }

    @NonNull
    @Override
    protected String getBodyFromSuccessfulResponse(@NonNull String response) throws ClientException {
        try {
            String rawBody = super.getBodyFromSuccessfulResponse(decryptTokenResponse(response, mSessionKey));
            return decryptTokenResponse(rawBody, mSessionKey);
        } catch (JSONException e) {
            throw new ClientException(ClientException.JSON_PARSE_FAILURE, "Unable to parse message", e);
        }
    }

    @Override
    protected String getRequestBody(MicrosoftStsTokenRequest request) throws ClientException, UnsupportedEncodingException {
        Map<String, String> bodyMap = ObjectMapper.constuctMapFromObject(request);
        Map<String, String> headerMap = new LinkedHashMap<>();
        byte[] ctx = new byte[256];
        random.nextBytes(ctx);
        headerMap.put("ctx", Base64.encodeToString(ctx, Base64.NO_PADDING | Base64.NO_WRAP | Base64.URL_SAFE));
        headerMap.put("alg", "HS256");

        RawKeyAccessor derivedKey = (RawKeyAccessor) ((RawKeyAccessor) mSessionKey)
                .generateDerivedKey(AuthenticationConstants.SP800_108_LABEL.getBytes(AuthenticationConstants.CHARSET_UTF8),
                                    ctx, SymmetricCipher.AES_GCM_NONE_HMACSHA256);

        String bodyString = GSON.toJson(headerMap) + "." + GSON.toJson(bodyMap);

        String signature = Base64.encodeToString(derivedKey.sign(bodyString.getBytes(AuthenticationConstants.CHARSET_UTF8)),
                Base64.NO_PADDING | Base64.NO_WRAP | Base64.URL_SAFE);

        String requestBody = bodyString + "." + signature;

        Map<String, String> requestMap = new TreeMap<>();

        requestMap.put(AuthenticationConstants.OAuth2.GRANT_TYPE, "urn:ietf:params:oauth:grant-type:jwt-bearer");
        requestMap.put("request", requestBody);
        requestMap.put("prt_protocol_version", "3.0");

        return ObjectMapper.serializeObjectToFormUrlEncoded(requestMap);
    }
}
