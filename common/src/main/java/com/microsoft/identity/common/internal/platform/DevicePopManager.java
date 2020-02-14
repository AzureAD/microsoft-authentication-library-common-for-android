//  Copyright (c) Microsoft Corporation.
//  All rights reserved.
//
//  This code is licensed under the MIT License.
//
//  Permission is hereby granted, free of charge, to any person obtaining a copy
//  of this software and associated documentation files(the "Software"), to deal
//  in the Software without restriction, including without limitation the rights
//  to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
//  copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions :
//
//  The above copyright notice and this permission notice shall be included in
//  all copies or substantial portions of the Software.
//
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//  THE SOFTWARE.
package com.microsoft.identity.common.internal.platform;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.security.KeyPairGeneratorSpec;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyInfo;
import android.security.keystore.KeyProperties;
import android.security.keystore.StrongBoxUnavailableException;
import android.text.TextUtils;
import android.util.Base64;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.microsoft.identity.common.exception.ClientException;
import com.microsoft.identity.common.internal.controllers.TaskCompletedCallbackWithError;
import com.microsoft.identity.common.internal.logging.Logger;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.impl.RSAKeyUtils;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.util.Base64URL;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.URL;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.RSAKeyGenParameterSpec;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.security.auth.x500.X500Principal;

import static com.microsoft.identity.common.exception.ClientException.ANDROID_KEYSTORE_UNAVAILABLE;
import static com.microsoft.identity.common.exception.ClientException.BAD_KEY_SIZE;
import static com.microsoft.identity.common.exception.ClientException.INTERRUPTED_OPERATION;
import static com.microsoft.identity.common.exception.ClientException.INVALID_ALG;
import static com.microsoft.identity.common.exception.ClientException.INVALID_PROTECTION_PARAMS;
import static com.microsoft.identity.common.exception.ClientException.JSON_CONSTRUCTION_FAILED;
import static com.microsoft.identity.common.exception.ClientException.JWT_SIGNING_FAILURE;
import static com.microsoft.identity.common.exception.ClientException.KEYSTORE_NOT_INITIALIZED;
import static com.microsoft.identity.common.exception.ClientException.NO_SUCH_ALGORITHM;
import static com.microsoft.identity.common.exception.ClientException.THUMBPRINT_COMPUTATION_FAILURE;
import static com.microsoft.identity.common.internal.net.ObjectMapper.ENCODING_SCHEME;

/**
 * Concrete class providing convenience functions around AndroidKeystore to support PoP.
 */
class DevicePopManager implements IDevicePopManager {

    private static final String TAG = DevicePopManager.class.getSimpleName();

    /**
     * The PoP alias in the designated KeyStore.
     */
    private static final String KEYSTORE_ENTRY_ALIAS = "microsoft-device-pop";

    /**
     * The NIST advised min keySize for RSA pairs.
     */
    private static final int RSA_KEY_SIZE = 2048;

    /**
     * The keystore backing this implementation.
     */
    private final KeyStore mKeyStore;

    /**
     * The name of the KeyStore to use.
     */
    private static final String ANDROID_KEYSTORE = "AndroidKeyStore";

    /**
     * A background worker to service async tasks.
     */
    private static final ExecutorService sThreadExecutor = Executors.newCachedThreadPool();

    /**
     * Properties used by the self-signed certificate.
     */
    private static final class CertificateProperties {

        /**
         * The certification validity duration.
         */
        static final int CERTIFICATE_VALIDITY_YEARS = 99;

        /**
         * The serial number of the certificate.
         */
        static final BigInteger SERIAL_NUMBER = BigInteger.ONE;

        /**
         * The Common Name of the certificate.
         */
        static final String COMMON_NAME = "CN=device-pop";
    }

    /**
     * Properties embedded in the SignedHttpRequest.
     * Roughly conforms to: https://tools.ietf.org/html/draft-ietf-oauth-signed-http-request-03
     */
    private static final class SignedHttpRequestJwtClaims {

        /**
         * The access_token.
         */
        private static final String ACCESS_TOKEN = "at";

        /**
         * The timestamp.
         */
        private static final String TIMESTAMP = "ts";

        /**
         * The HTTP method.
         */
        private static final String HTTP_METHOD = "m";

        /**
         * The host part of the resource server URL.
         */
        private static final String HTTP_HOST = "u";

        /**
         * The path part of the resource server URL.
         */
        private static final String HTTP_PATH = "p";

        /**
         * The JWK signed into this JWT.
         */
        private static final String CNF = "cnf";

        /**
         * A random value used for replay protection.
         */
        private static final String NONCE = "nonce";
    }

    /**
     * Algorithms supported by this KeyPairGenerator.
     */
    static final class KeyPairGeneratorAlgorithms {
        static final String RSA = "RSA";
    }

    DevicePopManager() throws KeyStoreException, CertificateException,
            NoSuchAlgorithmException, IOException {
        mKeyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
        mKeyStore.load(null);
    }

    @Override
    public boolean asymmetricKeyExists() {
        boolean exists = false;

        try {
            exists = mKeyStore.containsAlias(KEYSTORE_ENTRY_ALIAS);
        } catch (final KeyStoreException e) {
            Logger.error(
                    TAG,
                    "Error while querying KeyStore",
                    e
            );
        }

        return exists;
    }

    @Override
    public boolean asymmetricKeyExists(@NonNull final String thumbprint) {
        if (asymmetricKeyExists()) { // Test if keys exist at all...
            try {
                return getAsymmetricKeyThumbprint().equals(thumbprint);
            } catch (final ClientException e) {
                Logger.error(
                        TAG,
                        "Error while comparing thumbprints.",
                        e
                );
            }
        }

        return false;
    }

    @Override
    public String getAsymmetricKeyThumbprint() throws ClientException {
        final Exception exception;
        final String errCode;

        try {
            final KeyStore.Entry keyEntry = mKeyStore.getEntry(KEYSTORE_ENTRY_ALIAS, null);
            final KeyPair rsaKeyPair = getKeyPairForEntry(keyEntry);
            final RSAKey rsaKey = getRsaKeyForKeyPair(rsaKeyPair);
            return getThumbprintForRsaKey(rsaKey);
        } catch (final KeyStoreException e) {
            exception = e;
            errCode = KEYSTORE_NOT_INITIALIZED;
        } catch (final NoSuchAlgorithmException e) {
            exception = e;
            errCode = NO_SUCH_ALGORITHM;
        } catch (final UnrecoverableEntryException e) {
            exception = e;
            errCode = INVALID_PROTECTION_PARAMS;
        } catch (final JOSEException e) {
            exception = e;
            errCode = THUMBPRINT_COMPUTATION_FAILURE;
        }

        throw new ClientException(
                errCode,
                exception.getMessage(),
                exception
        );
    }

    @Override
    public void generateAsymmetricKey(@NonNull final Context context,
                                      @NonNull final TaskCompletedCallbackWithError<String, ClientException> callback) {
        sThreadExecutor.submit(
                new Runnable() {
                    @Override
                    public void run() {
                        try {
                            callback.onTaskCompleted(generateAsymmetricKey(context));
                        } catch (final ClientException e) {
                            callback.onError(e);
                        }
                    }
                }
        );
    }

    @Override
    public String generateAsymmetricKey(@NonNull final Context context) throws ClientException {
        final Exception exception;
        final String errCode;

        try {
            final KeyPair keyPair = generateNewRsaKeyPair(context, RSA_KEY_SIZE);
            final RSAKey rsaKey = getRsaKeyForKeyPair(keyPair);
            return getThumbprintForRsaKey(rsaKey);
        } catch (final UnsupportedOperationException e) {
            exception = e;
            errCode = BAD_KEY_SIZE;
        } catch (final NoSuchAlgorithmException e) {
            exception = e;
            errCode = NO_SUCH_ALGORITHM;
        } catch (final NoSuchProviderException e) {
            exception = e;
            errCode = ANDROID_KEYSTORE_UNAVAILABLE;
        } catch (final InvalidAlgorithmParameterException e) {
            exception = e;
            errCode = INVALID_ALG;
        } catch (final JOSEException e) {
            exception = e;
            errCode = THUMBPRINT_COMPUTATION_FAILURE;
        }

        final ClientException clientException = new ClientException(
                errCode,
                exception.getMessage(),
                exception
        );

        Logger.error(
                TAG,
                clientException.getMessage(),
                clientException
        );

        throw clientException;
    }

    @Override
    public boolean clearAsymmetricKey() {
        boolean deleted = false;

        try {
            mKeyStore.deleteEntry(KEYSTORE_ENTRY_ALIAS);
            deleted = true;
        } catch (final KeyStoreException e) {
            Logger.error(
                    TAG,
                    "Error while clearing KeyStore",
                    e
            );
        }

        return deleted;
    }

    @Override
    public String getRequestConfirmation() throws ClientException {
        // The sync API is a wrapper around the async API
        // This likely shouldn't be called on the UI thread to avoid ANR
        // Device perf may vary, however -- some devices this may be OK.
        // YMMV
        final CountDownLatch latch = new CountDownLatch(1);
        final String[] result = new String[1];
        final ClientException[] errorResult = new ClientException[1];

        getRequestConfirmation(new TaskCompletedCallbackWithError<String, ClientException>() {
            @Override
            public void onTaskCompleted(@NonNull final String reqCnf) {
                result[0] = reqCnf;
                latch.countDown();
            }

            @Override
            public void onError(@NonNull final ClientException error) {
                errorResult[0] = error;
                latch.countDown();
            }
        });

        // Wait for the async op to complete...
        try {
            latch.await();

            if (null != result[0]) {
                return result[0];
            } else {
                throw errorResult[0];
            }
        } catch (final InterruptedException e) {
            Logger.error(
                    TAG,
                    "Interrupted while waiting on callback.",
                    e
            );

            throw new ClientException(
                    INTERRUPTED_OPERATION,
                    e.getMessage(),
                    e
            );
        }
    }

    @Override
    public void getRequestConfirmation(@NonNull final TaskCompletedCallbackWithError<String, ClientException> callback) {
        sThreadExecutor.submit(new Runnable() {
            @Override
            public void run() {
                // Vars for error handling...
                final Exception exception;
                final String errCode;

                try {
                    final KeyStore.Entry keyEntry = mKeyStore.getEntry(KEYSTORE_ENTRY_ALIAS, null);
                    final KeyPair rsaKeyPair = getKeyPairForEntry(keyEntry);
                    final RSAKey rsaKey = getRsaKeyForKeyPair(rsaKeyPair);
                    final String base64UrlEncodedJwkJsonStr = getReqCnfForRsaKey(rsaKey);

                    callback.onTaskCompleted(base64UrlEncodedJwkJsonStr);

                    // We're done.
                    return;
                } catch (final KeyStoreException e) {
                    exception = e;
                    errCode = KEYSTORE_NOT_INITIALIZED;
                } catch (final NoSuchAlgorithmException e) {
                    exception = e;
                    errCode = NO_SUCH_ALGORITHM;
                } catch (final UnrecoverableEntryException e) {
                    exception = e;
                    errCode = INVALID_PROTECTION_PARAMS;
                } catch (final JOSEException e) {
                    exception = e;
                    errCode = THUMBPRINT_COMPUTATION_FAILURE;
                } catch (final JSONException e) {
                    exception = e;
                    errCode = JSON_CONSTRUCTION_FAILED;
                }

                final ClientException clientException = new ClientException(
                        errCode,
                        exception.getMessage(),
                        exception
                );

                Logger.error(
                        TAG,
                        clientException.getMessage(),
                        clientException
                );

                callback.onError(clientException);
            }
        });
    }

    @Override
    public String mintSignedAccessToken(@NonNull final String httpMethod,
                                        @NonNull final URL requestUrl,
                                        @NonNull final String accessToken,
                                        @Nullable final String nonce) throws ClientException {
        final Exception exception;
        final String errCode;

        try {
            final JWTClaimsSet.Builder claimsBuilder = new JWTClaimsSet.Builder();
            claimsBuilder.claim(SignedHttpRequestJwtClaims.ACCESS_TOKEN, accessToken);
            claimsBuilder.claim(SignedHttpRequestJwtClaims.TIMESTAMP, System.currentTimeMillis() / 1000L);
            claimsBuilder.claim(SignedHttpRequestJwtClaims.HTTP_METHOD, httpMethod);
            claimsBuilder.claim(SignedHttpRequestJwtClaims.HTTP_HOST, requestUrl.getHost());
            claimsBuilder.claim(SignedHttpRequestJwtClaims.HTTP_PATH, requestUrl.getPath());
            claimsBuilder.claim(SignedHttpRequestJwtClaims.CNF, getDevicePopJwkMinifiedJson());

            if (!TextUtils.isEmpty(nonce)) {
                claimsBuilder.claim(SignedHttpRequestJwtClaims.NONCE, nonce);
            }

            final JWTClaimsSet claimsSet = claimsBuilder.build();

            final KeyStore.Entry entry = mKeyStore.getEntry(KEYSTORE_ENTRY_ALIAS, null);
            final PrivateKey privateKey = ((KeyStore.PrivateKeyEntry) entry).getPrivateKey();
            final RSASSASigner signer = new RSASSASigner(privateKey);

            final SignedJWT signedJWT = new SignedJWT(
                    new JWSHeader.Builder(JWSAlgorithm.RS256)
                            .build(),
                    claimsSet
            );

            signedJWT.sign(signer);

            return signedJWT.serialize();
        } catch (final NoSuchAlgorithmException e) {
            exception = e;
            errCode = NO_SUCH_ALGORITHM;
        } catch (final KeyStoreException e) {
            exception = e;
            errCode = KEYSTORE_NOT_INITIALIZED;
        } catch (final JOSEException e) {
            exception = e;
            errCode = JWT_SIGNING_FAILURE;
        } catch (final UnrecoverableEntryException e) {
            exception = e;
            errCode = INVALID_PROTECTION_PARAMS;
        }

        final ClientException clientException = new ClientException(
                errCode,
                exception.getMessage(),
                exception
        );

        Logger.error(
                TAG,
                clientException.getMessage(),
                clientException
        );

        throw clientException;
    }

    //region Internal Functions

    /**
     * Generates a new RSA KeyPair of the specified lenth.
     *
     * @param context    The current application Context.
     * @param minKeySize The minimum keysize to use.
     * @return The newly generated RSA KeyPair.
     * @throws UnsupportedOperationException
     */
    @SuppressLint("NewApi")
    private KeyPair generateNewRsaKeyPair(@NonNull final Context context,
                                          final int minKeySize)
            throws UnsupportedOperationException, InvalidAlgorithmParameterException,
            NoSuchAlgorithmException, NoSuchProviderException {
        final int MAX_RETRIES = 4;

        for (int ii = 0; ii < MAX_RETRIES; ii++) {
            KeyPair kp;

            try {
                kp = generateNewKeyPair(context, true);
            } catch (final StrongBoxUnavailableException e) {
                Logger.error(
                        TAG,
                        "StrongBox unsupported - skipping hardware flags.",
                        e
                );

                // Retry, but don't request StrongBox
                kp = generateNewKeyPair(context, false);
            }

            // Due to a bug in some versions of Android, keySizes may not be exactly as specified
            // To generate a 2048-bit key, two primes of length 1024 are multiplied -- this product
            // may be 2047 in length in some cases which causes Nimbus to throw an IllegalArgumentException.
            // To avoid this, check the keysize prior to returning the generated KeyPair.

            // Since this seems to be nondeterministic in nature, attempt this a maximum of 4 times.
            final int length = RSAKeyUtils.keyBitLength(kp.getPrivate());

            // If the key material is hidden (HSM or otherwise) the length is -1
            if (length >= minKeySize || length < 0) {
                logSecureHardwareState(kp);

                return kp;
            }
        }

        // Clean up... we generated a cert, but it cannot be used.
        clearAsymmetricKey();

        throw new UnsupportedOperationException(
                "Failed to generate valid KeyPair. Attempted " + MAX_RETRIES + " times."
        );
    }

    private void logSecureHardwareState(@NonNull final KeyPair kp) {
        String msg;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                final PrivateKey privateKey = kp.getPrivate();
                final KeyFactory factory = KeyFactory.getInstance(
                        privateKey.getAlgorithm(), ANDROID_KEYSTORE
                );
                final KeyInfo info = factory.getKeySpec(privateKey, KeyInfo.class);
                final boolean isInsideSecureHardware = info.isInsideSecureHardware();
                msg = "SecretKey is secure hardware backed? " + isInsideSecureHardware;
            } catch (final Exception e) {
                msg = "Failed to query secure hardware state.";
            }
        } else {
            msg = "Cannot query secure hardware state (API unavailable <23)";
        }

        Logger.info(TAG, msg);
    }

    /**
     * Generates a new {@link KeyPair}.
     *
     * @param context      The application Context.
     * @param useStrongbox True if StrongBox should be used, false otherwise.
     * @return The newly generated KeyPair.
     * @throws InvalidAlgorithmParameterException If the designated crypto algorithm is not
     *                                            supported for the designated parameters.
     * @throws NoSuchAlgorithmException           If the designated crypto algorithm is not supported.
     * @throws NoSuchProviderException            If the designated crypto provider cannot be found.
     * @throws StrongBoxUnavailableException      If StrongBox is unavailable.
     */
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private KeyPair generateNewKeyPair(@NonNull final Context context, final boolean useStrongbox)
            throws InvalidAlgorithmParameterException, NoSuchAlgorithmException,
            NoSuchProviderException, StrongBoxUnavailableException {
        final KeyPairGenerator kpg = getInitializedRsaKeyPairGenerator(
                context,
                RSA_KEY_SIZE,
                useStrongbox
        );
        return kpg.generateKeyPair();
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private KeyPairGenerator getInitializedRsaKeyPairGenerator(@NonNull final Context context,
                                                               final int keySize,
                                                               final boolean useStrongbox)
            throws InvalidAlgorithmParameterException, NoSuchProviderException, NoSuchAlgorithmException {
        // Create the KeyPairGenerator
        final KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(
                KeyPairGeneratorAlgorithms.RSA,
                ANDROID_KEYSTORE
        );

        // Initialize it!
        initialize(context, keyPairGenerator, keySize, useStrongbox);

        return keyPairGenerator;
    }

    /**
     * Initialize the provided {@link KeyPairGenerator}.
     *
     * @param context          The current application Context.
     * @param keyPairGenerator The KeyPairGenerator to initialize.
     * @param keySize          The RSA keysize.
     * @param useStrongbox     True if StrongBox should be used, false otherwise. Please note that
     *                         StrongBox may not be supported on all devices.
     * @throws InvalidAlgorithmParameterException
     */
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private static void initialize(@NonNull final Context context,
                                   @NonNull final KeyPairGenerator keyPairGenerator,
                                   final int keySize,
                                   final boolean useStrongbox) throws InvalidAlgorithmParameterException {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            initializePre23(context, keyPairGenerator, keySize);
        } else {
            initialize23(keyPairGenerator, keySize, useStrongbox);
        }
    }

    @SuppressLint("InlinedApi")
    @RequiresApi(api = Build.VERSION_CODES.M)
    private static void initialize23(@NonNull final KeyPairGenerator keyPairGenerator,
                                     final int keySize,
                                     final boolean useStrongbox) throws InvalidAlgorithmParameterException {
        KeyGenParameterSpec.Builder builder = new KeyGenParameterSpec.Builder(
                KEYSTORE_ENTRY_ALIAS,
                KeyProperties.PURPOSE_SIGN
                        | KeyProperties.PURPOSE_VERIFY
                        | KeyProperties.PURPOSE_ENCRYPT
                        | KeyProperties.PURPOSE_DECRYPT
        )
                .setKeySize(keySize)
                .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1)
                .setDigests(KeyProperties.DIGEST_SHA256);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && useStrongbox) {
            Logger.verbose(
                    TAG,
                    "Attempting to apply StrongBox isolation."
            );
            builder = applyHardwareIsolation(builder);
        }

        final AlgorithmParameterSpec spec = builder.build();
        keyPairGenerator.initialize(spec);
    }

    /**
     * Applies hardware backed security to the supplied {@link KeyGenParameterSpec.Builder}.
     *
     * @param builder The builder.
     * @return A reference to the supplied builder instance.
     */
    @SuppressLint("NewApi")
    @RequiresApi(Build.VERSION_CODES.P)
    @NonNull
    private static KeyGenParameterSpec.Builder applyHardwareIsolation(
            @NonNull final KeyGenParameterSpec.Builder builder) {
        return builder.setIsStrongBoxBacked(true);
    }

    @SuppressLint("NewApi")
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private static void initializePre23(@NonNull final Context context,
                                        @NonNull final KeyPairGenerator keyPairGenerator,
                                        final int keySize) throws InvalidAlgorithmParameterException {
        final Calendar calendar = Calendar.getInstance();
        final Date start = getNow(calendar);
        calendar.add(Calendar.YEAR, CertificateProperties.CERTIFICATE_VALIDITY_YEARS);
        final Date end = calendar.getTime();

        final KeyPairGeneratorSpec.Builder specBuilder = new KeyPairGeneratorSpec.Builder(context)
                .setAlias(KEYSTORE_ENTRY_ALIAS)
                .setStartDate(start)
                .setEndDate(end)
                .setSerialNumber(CertificateProperties.SERIAL_NUMBER)
                .setSubject(new X500Principal(CertificateProperties.COMMON_NAME));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            specBuilder.setAlgorithmParameterSpec(
                    new RSAKeyGenParameterSpec(keySize, RSAKeyGenParameterSpec.F4)
            );
        }

        final KeyPairGeneratorSpec spec = specBuilder.build();
        keyPairGenerator.initialize(spec);
    }

    /**
     * Gets the current time as a {@link Date}.
     *
     * @param calendar The {@link Calendar} implementation to use.
     * @return The current time.
     */
    private static Date getNow(@NonNull final Calendar calendar) {
        return calendar.getTime();
    }

    /**
     * For the supplied {@link KeyStore.Entry}, get a corresponding {@link KeyPair} instance.
     *
     * @param entry The Keystore.Entry to use.
     * @return The resulting KeyPair.
     */
    private static KeyPair getKeyPairForEntry(@NonNull final KeyStore.Entry entry) {
        final PrivateKey privateKey = ((KeyStore.PrivateKeyEntry) entry).getPrivateKey();
        final PublicKey publicKey = ((KeyStore.PrivateKeyEntry) entry).getCertificate().getPublicKey();
        return new KeyPair(publicKey, privateKey);
    }

    /**
     * Gets the corresponding {@link RSAKey} for the supplied {@link KeyPair}.
     *
     * @param keyPair The KeyPair to use.
     * @return The resulting RSAKey.
     */
    private static RSAKey getRsaKeyForKeyPair(@NonNull final KeyPair keyPair) {
        return new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
                .privateKey(keyPair.getPrivate())
                .keyUse(null)
                .build();
    }

    /**
     * Gets the base64url encoded public jwk for the supplied RSAKey.
     *
     * @param rsaKey The input key material.
     * @return The base64url encoded jwk.
     */
    private static String getReqCnfForRsaKey(@NonNull final RSAKey rsaKey)
            throws JOSEException, JSONException {
        final String thumbprintStr = getThumbprintForRsaKey(rsaKey);
        final String thumbprintMinifiedJson =
                new JSONObject()
                        .put("kid", thumbprintStr)
                        .toString();

        return base64UrlEncode(thumbprintMinifiedJson);
    }

    private static String getThumbprintForRsaKey(@NonNull RSAKey rsaKey) throws JOSEException {
        final Base64URL thumbprint = rsaKey.computeThumbprint();
        return thumbprint.toString();
    }

    /**
     * Encodes a String with Base64Url and no padding.
     *
     * @param input String to be encoded.
     * @return Encoded result from input.
     */
    private static String base64UrlEncode(@NonNull final String input) {
        String result = null;

        try {
            byte[] encodeBytes = input.getBytes(ENCODING_SCHEME);
            result = Base64.encodeToString(
                    encodeBytes,
                    Base64.NO_PADDING | Base64.NO_WRAP | Base64.URL_SAFE
            );
        } catch (final UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return result;
    }

    /**
     * Returns the cnf claim used in SHRs (Signed HTTP Requests); format is JSON.
     *
     * @return The cnf claim value.
     * @throws UnrecoverableEntryException If the queried key cannot be found.
     * @throws NoSuchAlgorithmException    If the KeyStore is unable to use the designated alg.
     * @throws KeyStoreException           If the KeyStore experiences an error during read.
     */
    private net.minidev.json.JSONObject getDevicePopJwkMinifiedJson()
            throws UnrecoverableEntryException, NoSuchAlgorithmException, KeyStoreException {
        final KeyStore.Entry keyEntry = mKeyStore.getEntry(KEYSTORE_ENTRY_ALIAS, null);
        final KeyPair rsaKeyPair = getKeyPairForEntry(keyEntry);
        final RSAKey rsaKey = getRsaKeyForKeyPair(rsaKeyPair);
        final RSAKey publicRsaKey = rsaKey.toPublicJWK();
        final net.minidev.json.JSONObject jwkContents = publicRsaKey.toJSONObject();
        final net.minidev.json.JSONObject wrappedJwk = new net.minidev.json.JSONObject();
        wrappedJwk.appendField("jwk", jwkContents);

        return wrappedJwk;
    }
    //endregion
}
