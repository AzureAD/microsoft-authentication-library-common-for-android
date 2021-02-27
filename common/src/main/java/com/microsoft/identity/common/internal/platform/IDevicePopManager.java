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

import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.microsoft.identity.common.exception.ClientException;
import com.microsoft.identity.common.internal.controllers.TaskCompletedCallbackWithError;

import java.net.URL;
import java.security.AlgorithmParameters;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.UnrecoverableEntryException;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.MGF1ParameterSpec;
import java.security.cert.Certificate;
import java.util.Date;

import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;

/**
 * Internal convenience class interface for PoP related functions.
 */
public interface IDevicePopManager {

    /**
     * The desired export format of our PoP public key.
     */
    enum PublicKeyFormat {
        /**
         * Base64 encoded SubjectPublicKeyInfo of the X.509 Certificate.
         * <p>
         * Conforms to the following ASN.1
         * <pre>
         * SubjectPublicKeyInfo  ::=  SEQUENCE  {
         *     algorithm            AlgorithmIdentifier,
         *     subjectPublicKey     BIT STRING
         * }
         * </pre>
         */
        X_509_SubjectPublicKeyInfo_ASN_1,

        /**
         * An RFC-7517 compliant public key as a minified JWK.
         * <p>
         * Sample value:
         * <pre>
         * {
         * 	"kty": "RSA",
         * 	"e": "AQAB",
         * 	"n": "tMqJ7Oxh3PdLaiEc28w....HwES9Q"
         * }
         * </pre>
         */
        JWK
    }

    /**
     * Signing algorithms supported by our underlying keystore. Not all algs available at all device
     * levels.
     */
    enum SigningAlgorithm {
        @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
        MD5_WITH_RSA("MD5withRSA"),

        @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
        NONE_WITH_RSA("NONEwithRSA"),

        @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
        SHA_256_WITH_RSA("SHA256withRSA"),

        @RequiresApi(Build.VERSION_CODES.M)
        SHA_256_WITH_RSA_PSS("SHA256withRSA/PSS"),

        @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
        SHA_384_WITH_RSA("SHA384withRSA"),

        @RequiresApi(Build.VERSION_CODES.M)
        SHA_384_WITH_RSA_PSS("SHA384withRSA/PSS"),

        @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
        SHA_512_WITH_RSA("SHA512withRSA"),

        @RequiresApi(Build.VERSION_CODES.M)
        SHA_512_WITH_RSA_PSS("SHA512withRSA/PSS");

        private final String mValue;

        SigningAlgorithm(@NonNull final String value) {
            mValue = value;
        }

        @Override
        @NonNull
        public String toString() {
            return mValue;
        }
    }

    /**
     * Ciphers supported by our underlying keystore. Asymmetric ciphers shown only.
     * <p>
     * Note: Some ciphers are [in]conspicuously absent. Any cipher that requires use of a SHA-1
     * digest or uses NO_PADDING will not be supported.
     */
    enum Cipher implements CryptoSuite {
        @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
        RSA_ECB_PKCS1_PADDING("RSA/ECB/PKCS1Padding"),

        @RequiresApi(Build.VERSION_CODES.M)
        RSA_ECB_OAEPWithSHA_256AndMGF1Padding("RSA/ECB/OAEPWithSHA-256AndMGF1Padding") {
            @Override
            public AlgorithmParameterSpec getParameters() {
                // We're going to be forcing defaults in this cipher to correct a deficiency in certain
                // android platform support.  See:
                // https://issuetracker.google.com/issues/37075898#comment7
                return new OAEPParameterSpec("SHA-256", "MGF1", new MGF1ParameterSpec("SHA-1"), PSource.PSpecified.DEFAULT);
            }
        },

        @RequiresApi(Build.VERSION_CODES.M)
        RSA_ECB_OAEPWithSHA_384AndMGF1Padding("RSA/ECB/OAEPWithSHA-384AndMGF1Padding") {
            @Override
            public AlgorithmParameterSpec getParameters() {
                // We're going to be forcing defaults in this cipher to correct a deficiency in certain
                // android platform support.  See:
                // https://issuetracker.google.com/issues/37075898#comment7
                return new OAEPParameterSpec("SHA-384", "MGF1", new MGF1ParameterSpec("SHA-1"), PSource.PSpecified.DEFAULT);
            }
        },

        @RequiresApi(Build.VERSION_CODES.M)
        RSA_ECB_OAEPWithSHA_512AndMGF1Padding("RSA/ECB/OAEPWithSHA-512AndMGF1Padding") {
            @Override
            public AlgorithmParameterSpec getParameters() {
                // We're going to be forcing defaults in this cipher to correct a deficiency in certain
                // android platform support.  See:
                // https://issuetracker.google.com/issues/37075898#comment7
                return new OAEPParameterSpec("SHA-512", "MGF1", new MGF1ParameterSpec("SHA-1"), PSource.PSpecified.DEFAULT);
            }
        };

        private final String mValue;

        Cipher(@NonNull final String value) {
            mValue = value;
        }

        @Override
        @NonNull
        public String toString() {
            return mValue;
        }

        @Override
        public String cipherName() {
            return mValue;
        }

        @Override
        public String macName() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                return SigningAlgorithm.SHA_256_WITH_RSA.name();
            } else {
                return "HmacSHA256";
            }
        }

        @Override
        public boolean isAsymmetric() {
            return true;
        }

        @Override
        public Class<? extends KeyStore.Entry> keyClass() {
            return KeyStore.PrivateKeyEntry.class;
        }

        @Override
        public int keySize() {
            return 2048;
        }

        /**
         * @return parameters to configure this cipher with, or null if none.
         */
        public AlgorithmParameterSpec getParameters() {
            return null;
        }
    }

    /**
     * Tests if keys exist.
     *
     * @return True if keys exist, false otherwise.
     */
    boolean asymmetricKeyExists();

    /**
     * Tests for existence of keys AND that they match the match the supplied thumbprint.
     *
     * @param thumbprint The thumbprint to match.
     * @return True if keys exist and they match the supplied thumbprint. False if keys do not match
     * or if keys cannot be loaded due to KeyStore errors.
     */
    boolean asymmetricKeyExists(String thumbprint);

    /**
     * Gets the thumbprint of the current KeyPair.
     *
     * @return The thumbprint.
     */
    String getAsymmetricKeyThumbprint() throws ClientException;

    /**
     * Generates asymmetric keys used by pop.
     *
     * @param callback Async callback with thumbprint/exception info.
     */
    void generateAsymmetricKey(Context context, TaskCompletedCallbackWithError<String, ClientException> callback);

    /**
     * Generates asymmetric keys used by pop.
     *
     * @return The generated RSA KeyPair's thumbprint.
     */
    String generateAsymmetricKey(Context context) throws ClientException;

    /**
     * Returns the creation date of the asymmetric key entry backing this instance.
     *
     * @return The asymmetric key creation date.
     * @throws ClientException If no asymmetric key exists.
     */
    Date getAsymmetricKeyCreationDate() throws ClientException;

    /**
     * Clears keys, if present.
     */
    boolean clearAsymmetricKey();

    /**
     * API to generate the req_cnf used for auth code redemptions.
     *
     * @return The req_cnf value.
     */
    String getRequestConfirmation() throws ClientException;

    /**
     * Async API to generate the req_cnf used for auth code redemptions.
     *
     * @return The req_cnf value.
     */
    void getRequestConfirmation(TaskCompletedCallbackWithError<String, ClientException> callback);

    /**
     * Signs an arbitrary piece of String data.
     *
     * @param alg   The RSA signing algorithm to use.
     * @param input The input to sign.
     * @return The input data, signed by our private key.
     * @see com.microsoft.identity.common.internal.platform.DevicePopManager.SigningAlgorithm
     */
    String sign(SigningAlgorithm alg, String input) throws ClientException;

    /**
     * Signs an arbitrary piece of byte data.
     *
     * @param alg   The RSA signing algorithm to use.
     * @param input The input to sign.
     * @return The input data, signed by our private key.
     * @see com.microsoft.identity.common.internal.platform.DevicePopManager.SigningAlgorithm
     */
    byte[] sign(@NonNull SigningAlgorithm alg, byte[] input) throws ClientException;

    /**
     * Verify a signature previously made by our Private Key.
     *
     * @param alg          The RSA signing algorithm to use.
     * @param plainText    The input to verify.
     * @param signatureStr The signature against which the plainText should be evaluated.
     * @return True if the input was signed by our private key. False otherwise.
     * @see com.microsoft.identity.common.internal.platform.DevicePopManager.SigningAlgorithm
     */
    boolean verify(SigningAlgorithm alg, String plainText, String signatureStr);

    /**
     * Verify a signature previously made by our Private Key.
     *
     * @param alg            The RSA signing algorithm to use.
     * @param plainText      The input to verify.
     * @param signatureBytes The signature against which the plainText should be evaluated.
     * @return True if the input was signed by our private key. False otherwise.
     * @see com.microsoft.identity.common.internal.platform.DevicePopManager.SigningAlgorithm
     */
    boolean verify(@NonNull SigningAlgorithm alg, byte[] plainText, byte[] signatureBytes);

    /**
     * Encrypts the supplied String with the provided cipher.
     *
     * @param cipher    The cipher to use.
     * @param plaintext The data to encrypt.
     * @return The encrypted plaintext.
     * @throws ClientException If encryption fails.
     */
    String encrypt(Cipher cipher, String plaintext) throws ClientException;

    /**
     * Encrypts the supplied byte array with the provided cipher.
     *
     * @param cipher    The cipher to use.
     * @param plaintext The data to encrypt.
     * @return The encrypted plaintext.
     * @throws ClientException If encryption fails.
     */
    byte[] encrypt(@NonNull final Cipher cipher, @NonNull final byte[] plaintext) throws ClientException;

    /**
     * Decrypts the supplied String with the provided cipher.
     *
     * @param cipher     The cipher used to derive the provided ciphertext.
     * @param ciphertext The text to decrypt.
     * @return The decrypted text.
     * @throws ClientException If decryption fails.
     */
    String decrypt(Cipher cipher, String ciphertext) throws ClientException;

    /**
     * Decrypts the supplied String with the provided cipher.
     *
     * @param cipher     The cipher used to derive the provided ciphertext.
     * @param ciphertext The text to decrypt.
     * @return The decrypted text.
     * @throws ClientException If decryption fails.
     */
    byte[] decrypt(@NonNull Cipher cipher, byte[] ciphertext) throws ClientException;

    /**
     * Gets the {@link SecureHardwareState} of this DevicePopManager.
     *
     * @return The SecureHardwareState.
     * @throws ClientException If the underlying key material cannot be inspected.
     */
    SecureHardwareState getSecureHardwareState() throws ClientException;

    /**
     * Gets the public key associated with this DevicePoPManager formatted per the supplied
     * export param.
     *
     * @param format The export format of the public key.
     * @return A String of the public key.
     */
    String getPublicKey(PublicKeyFormat format) throws ClientException;

    /**
     * Gets the public key associated with this underlying key in the pop manager..
     *
     * @return A PublicKey instance.
     */
    PublicKey getPublicKey() throws UnrecoverableEntryException, NoSuchAlgorithmException, KeyStoreException;
    
    /*
     * Returns the certificate chain associated with the underlying key material.
     *
     * @return The certificate chain (with the device pop key certificate first, following by zero
     * or more certificate authorities), or null if the current key does not contain a certificate
     * chain.
     * @throws ClientException If the underlying key material cannot be inspected.
     */
    Certificate[] getCertificateChain() throws ClientException;

    /**
     * Api to create the signed PoP access token.
     *
     * @param httpMethod  (Optional) The HTTP method that will be used with this outbound request.
     * @param timestamp   Seconds since January 1st, 1970 (UTC).
     * @param requestUrl  The recipient URL of the outbound request.
     * @param accessToken The access_token from which to derive the signed JWT.
     * @param nonce       (Optional) Arbitrary value used for replay protection by middleware.
     * @return The signed PoP access token.
     */
    String mintSignedAccessToken(String httpMethod,
                                 long timestamp,
                                 URL requestUrl,
                                 String accessToken,
                                 String nonce
    ) throws ClientException;

    /**
     * Api to create the signed PoP access token.
     *
     * @param httpMethod   (Optional) The HTTP method that will be used with this outbound request.
     * @param timestamp    Seconds since January 1st, 1970 (UTC).
     * @param requestUrl   The recipient URL of the outbound request.
     * @param accessToken  The access_token from which to derive the signed JWT.
     * @param nonce        (Optional) Arbitrary value used for replay protection by middleware.
     * @param clientClaims (Optional) Arbitrary String data provided by the caller. Used as the
     *                     client_claims value.
     * @return The signed PoP access token.
     */
    String mintSignedAccessToken(String httpMethod,
                                 long timestamp,
                                 URL requestUrl,
                                 String accessToken,
                                 String nonce,
                                 String clientClaims
    ) throws ClientException;

    /**
     * Api to create the signed HTTP requests (SHRs) without embedding a PoP-AT.
     *
     * @param httpMethod   (Optional) The HTTP method that will be used with this outbound request.
     * @param timestamp    Seconds since January 1st, 1970 (UTC).
     * @param requestUrl   The recipient URL of the outbound request.
     * @param nonce        (Optional) Arbitrary value used for replay protection by middleware.
     * @param clientClaims (Optional) Arbitrary String data provided by the caller. Used as the
     *                     client_claims value.
     * @return The signed PoP access token.
     */
    String mintSignedHttpRequest(String httpMethod,
                                 long timestamp,
                                 URL requestUrl,
                                 String nonce,
                                 String clientClaims
    ) throws ClientException;
}
