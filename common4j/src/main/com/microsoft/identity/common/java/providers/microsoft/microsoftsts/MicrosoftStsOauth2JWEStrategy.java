package com.microsoft.identity.common.java.providers.microsoft.microsoftsts;


import com.google.gson.Gson;
import com.microsoft.identity.common.java.AuthenticationConstants;
import com.microsoft.identity.common.java.crypto.IKeyAccessor;
import com.microsoft.identity.common.java.crypto.RawKeyAccessor;
import com.microsoft.identity.common.java.crypto.SymmetricCipher;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.logging.Logger;
import com.microsoft.identity.common.java.platform.JweResponse;
import com.microsoft.identity.common.java.providers.oauth2.OAuth2StrategyParameters;
import com.microsoft.identity.common.java.util.ObjectMapper;
import com.microsoft.identity.common.java.util.StringUtil;

import org.json.JSONException;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

import cz.msebera.android.httpclient.extras.Base64;
import lombok.NonNull;

/**
 * In addition to the normal requests that we handle, some of these need to use a specific
 * kind of strategy that enloses the token request in a JOSE object inside of a form post.
 */
public class MicrosoftStsOauth2JWEStrategy extends MicrosoftStsOAuth2Strategy {

    private final String TAG = MicrosoftStsOauth2JWEStrategy.class.getSimpleName();

    private final IKeyAccessor mSessionKey;
    private final byte[] mCtx;
    private static final Gson GSON = new Gson();
    private static final SecureRandom random;
    static {
        SecureRandom tmpRandom = new SecureRandom();
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
                                         @NonNull final IKeyAccessor sessionKey,
                                         @NonNull final byte[] ctx) throws ClientException {
        super(config, parameters);
        setTokenEndpoint(config.getTokenEndpoint().toString());
        mSessionKey = sessionKey;
        mCtx = Arrays.copyOfRange(ctx, 0, ctx.length);
    }

    /**
     * Helper method to decrypt the access token response using the derived Session key.
     */
    public byte[] decryptUsingDerivedSessionKey(@NonNull final byte[] ivBytes,
                                                @NonNull final byte[] ctx,
                                                @NonNull final byte[] encryptedBytes,
                                                @NonNull final byte[] authenticationTag,
                                                RawKeyAccessor skAccessor)
            throws ClientException {
        byte[] label = AuthenticationConstants.SP800_108_LABEL.getBytes(Charset.forName("ASCII"));
        IKeyAccessor derivedKey = skAccessor.generateDerivedKey(label, ctx, skAccessor.getSuite());

        byte[] cryptobuf = new byte[ivBytes.length + encryptedBytes.length + authenticationTag.length];
        System.arraycopy(ivBytes, 0, cryptobuf, 0, ivBytes.length);
        System.arraycopy(encryptedBytes, 0, cryptobuf, ivBytes.length, encryptedBytes.length);
        System.arraycopy(authenticationTag, 0, cryptobuf, ivBytes.length + encryptedBytes.length, authenticationTag.length);
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
    public String decryptTokenResponse(@NonNull final String jwe,
                                       @NonNull final IKeyAccessor skAccessor)
            throws JSONException, ClientException {

        final JweResponse jweResponse = JweResponse.parseJwe(jwe);

        if (!jweResponse.getJweHeader().getEncryptionAlgorithm().equalsIgnoreCase("A256GCM")
                && !jweResponse.getJweHeader().getEncryptionAlgorithm().equalsIgnoreCase("dir")) {
            throw new IllegalArgumentException("Invalid encryption algorithm");
        }

        final byte[] authenticationTag = Base64.decode(jweResponse.getAuthenticationTag(), Base64.NO_WRAP | Base64.URL_SAFE);

        // NOTE: EVOsts sends mIv and mPayload as Base64UrlEncoded
        final byte[] ivDecoded = Base64.decode(jweResponse.getIV(), Base64.NO_WRAP | Base64.URL_SAFE);
        final byte[] payloadCipherText = Base64.decode(jweResponse.getPayload(), Base64.NO_WRAP | Base64.URL_SAFE);

        // CTX is inside the mJweHeader and comes as Base64 not base64urlencode
        final byte[] derivedKeyCtx = Base64.decode(
                jweResponse.getJweHeader().getContext(),
                Base64.DEFAULT
        );

        Logger.verbose(TAG,
                "Decrypting the token response for using PRT. IV size:"
                        + ivDecoded.length
                        + " mPayload size:"
                        + payloadCipherText.length
                        + " ctx size:"
                        + derivedKeyCtx.length
                        + " auth tag length:"
                        + authenticationTag.length
        );

        final byte[] decryptedData = decryptUsingDerivedSessionKey(
                ivDecoded,
                derivedKeyCtx,
                payloadCipherText,
                authenticationTag,
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
        Map<String, String> bodyMap = ObjectMapper.constructMapFromObject(request);
        Map<String, String> headerMap = new LinkedHashMap<>();

        headerMap.put("alg", "HS256");
        headerMap.put("ctx", Base64.encodeToString(mCtx, Base64.NO_WRAP ));
        headerMap.put("typ", "JWT");

        String bodyString =
                StringUtil.encodeUrlSafeString(GSON.toJson(headerMap).getBytes(AuthenticationConstants.CHARSET_UTF8)) + "."
                + StringUtil.encodeUrlSafeString(GSON.toJson(bodyMap).getBytes(AuthenticationConstants.CHARSET_UTF8));

        String signature = Base64.encodeToString(mSessionKey.sign(bodyString.getBytes(AuthenticationConstants.CHARSET_UTF8)),
                Base64.NO_PADDING | Base64.NO_WRAP);

        String requestBody = bodyString + "." + signature;

        Map<String, String> requestMap = new TreeMap<>();

        requestMap.put(AuthenticationConstants.OAuth2.GRANT_TYPE, "urn:ietf:params:oauth:grant-type:jwt-bearer");
        requestMap.put("request", requestBody);
        requestMap.put("prt_protocol_version", "3.0");

        return ObjectMapper.serializeObjectToFormUrlEncoded(requestMap);
    }
}
