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
package com.microsoft.identity.common.internal.authscheme;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.security.KeyPairGeneratorSpec;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.security.keystore.StrongBoxUnavailableException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.microsoft.identity.common.exception.ClientException;
import com.microsoft.identity.common.internal.controllers.TaskCompletedCallbackWithError;
import com.microsoft.identity.common.internal.logging.Logger;
import com.nimbusds.jose.crypto.impl.RSAKeyUtils;

import java.math.BigInteger;
import java.net.URL;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.RSAKeyGenParameterSpec;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.security.auth.x500.X500Principal;

import static com.microsoft.identity.common.exception.ClientException.BAD_KEY_SIZE;
import static com.microsoft.identity.common.exception.ClientException.INVALID_ALG;
import static com.microsoft.identity.common.exception.ClientException.NO_SUCH_ALGORITHM;
import static com.microsoft.identity.common.exception.ClientException.NO_SUCH_PROVIDER;

public class DevicePopManagerImpl implements IDevicePopManager {

    private static final String TAG = DevicePopManagerImpl.class.getSimpleName();

    /**
     * The PoP alias in the designated KeyStore.
     */
    private static final String KEYSTORE_ENTRY_ALIAS = "microsoft-device-pop";

    /**
     * The NIST advised min keySize for RSA pairs.
     */
    private static final int RSA_KEY_SIZE = 2048;

    /**
     * The current application Context.
     */
    private final Context mContext;

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
     * Algorithms supported by this KeyPairGenerator.
     */
    static final class KeyPairGeneratorAlgorithms {
        static final String RSA = "RSA";
    }

    public DevicePopManagerImpl(@NonNull final Context context,
                                @NonNull final KeyStore keyStore) {
        mContext = context;
        mKeyStore = keyStore;
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
    public void generateAsymmetricKey(@NonNull final TaskCompletedCallbackWithError<KeyPair, ClientException> callback) {
        sThreadExecutor.submit(new Runnable() {
            @Override
            public void run() {
                final Exception exception;
                final String errCode;

                try {
                    final KeyPair keyPair = generateNewRsaKeyPair(mContext, RSA_KEY_SIZE);
                    callback.onTaskCompleted(keyPair);

                    return;
                } catch (final UnsupportedOperationException e) {
                    exception = e;
                    errCode = BAD_KEY_SIZE;
                } catch (final NoSuchAlgorithmException e) {
                    exception = e;
                    errCode = NO_SUCH_ALGORITHM;
                } catch (final NoSuchProviderException e) {
                    exception = e;
                    errCode = NO_SUCH_PROVIDER;
                } catch (final InvalidAlgorithmParameterException e) {
                    exception = e;
                    errCode = INVALID_ALG;
                }

                callback.onError(
                        new ClientException(
                                errCode,
                                exception.getMessage(),
                                exception
                        )
                );
            }
        });
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
    public boolean getRequestConfirmation() {
        // TODO
        return false;
    }

    @Override
    public void getRequestConfirmation(@NonNull final TaskCompletedCallbackWithError<String, ClientException> callback) {
        // TODO
    }

    @Override
    public String getAuthorizationHeaderValue(@NonNull final String httpMethod,
                                              @NonNull final URL requestUrl,
                                              @NonNull final String accessToken,
                                              @Nullable final String nonce) {
        // TODO
        return null;
    }

    //region Internal Functions

    /**
     * Generates a new RSA KeyPair of the specified lenth.
     *
     * @param ctx        The current application Context.
     * @param minKeySize The minimum keysize to use.
     * @return The newly generated RSA KeyPair.
     * @throws UnsupportedOperationException
     */
    @SuppressLint("NewApi")
    private KeyPair generateNewRsaKeyPair(@NonNull final Context ctx,
                                          final int minKeySize)
            throws UnsupportedOperationException, InvalidAlgorithmParameterException,
            NoSuchAlgorithmException, NoSuchProviderException {
        final int MAX_RETRIES = 4;

        for (int ii = 0; ii < MAX_RETRIES; ii++) {
            KeyPair kp;

            try {
                kp = generateNewKeyPair(ctx, true);
            } catch (final StrongBoxUnavailableException e) {
                // TODO log an error/warning
                kp = generateNewKeyPair(ctx, false);
            }

            // Due to a bug in some versions of Android, keySizes may not be exactly as specified
            // To generate a 2048-bit key, two primes of length 1024 are multiplied -- this product
            // may be 2047 in length in some cases which causes Nimbus to crash. To avoid this,
            // check the keysize prior to returning the generated KeyPair.

            // Since this seems to be nondeterministic in nature, attempt this a maximum of 4 times.
            final int length = RSAKeyUtils.keyBitLength(kp.getPrivate());

            // If the key material is hidden (HSM or otherwise) the length is -1
            if (length >= minKeySize || length < 0) {
                return kp;
            }
        }

        // Clean up... we generated a cert, but it cannot be used.
        clearAsymmetricKey();

        throw new UnsupportedOperationException(
                "Failed to generate valid KeyPair. Attempted " + MAX_RETRIES + " times."
        );
    }

    /**
     * Generates a new {@link KeyPair}.
     *
     * @param ctx          The application Context.
     * @param useStrongbox True if StrongBox should be used, false otherwise.
     * @return The newly generated KeyPair.
     * @throws InvalidAlgorithmParameterException If the designated crypto algorithm is not
     *                                            supported for the designated parameters.
     * @throws NoSuchAlgorithmException           If the designated crypto algorithm is not supported.
     * @throws NoSuchProviderException            If the designated crypto provider cannot be found.
     * @throws StrongBoxUnavailableException      If StrongBox is unavailable.
     */
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private KeyPair generateNewKeyPair(@NonNull final Context ctx, final boolean useStrongbox)
            throws InvalidAlgorithmParameterException, NoSuchAlgorithmException,
            NoSuchProviderException, StrongBoxUnavailableException {
        final KeyPairGenerator kpg = getInitializedRsaKeyPairGenerator(
                ctx,
                RSA_KEY_SIZE,
                useStrongbox
        );
        return kpg.generateKeyPair();
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private KeyPairGenerator getInitializedRsaKeyPairGenerator(@NonNull final Context ctx,
                                                               final int keySize,
                                                               final boolean useStrongbox)
            throws InvalidAlgorithmParameterException, NoSuchProviderException, NoSuchAlgorithmException {
        // Create the KeyPairGenerator
        final KeyPairGenerator keyPairGenerator = getKeyPairGenerator(
                KeyPairGeneratorAlgorithms.RSA,
                ANDROID_KEYSTORE
        );

        // Initialize it!
        initialize(ctx, keyPairGenerator, keySize, useStrongbox);

        return keyPairGenerator;
    }

    /**
     * For the provided algorithm and keystore type, return a KeyPairGenerator.
     *
     * @param alg      The algorithm suite that must be supported by this generator.
     * @param provider The String name of the provider to use.
     * @return The new KeyPairGenerator object.
     * @throws NoSuchProviderException  If the specified Provider is not registered in the security
     *                                  provider list.
     * @throws NoSuchAlgorithmException If a KeyPairGeneratorSpi implementation for the specified
     *                                  algorithm is not available for the specified Provider.
     */
    private static KeyPairGenerator getKeyPairGenerator(@NonNull final String alg,
                                                        @NonNull final String provider)
            throws NoSuchProviderException, NoSuchAlgorithmException {
        return KeyPairGenerator.getInstance(alg, provider);
    }

    /**
     * Initialize the provided {@link KeyPairGenerator}.
     *
     * @param ctx              The current application Context.
     * @param keyPairGenerator The KeyPairGenerator to initialize.
     * @param keySize          The RSA keysize.
     * @param useStrongbox     True if StrongBox should be used, false otherwise. Please note that
     *                         StrongBox may not be supported on all devices.
     * @throws InvalidAlgorithmParameterException
     */
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private static void initialize(@NonNull final Context ctx,
                                   @NonNull final KeyPairGenerator keyPairGenerator,
                                   final int keySize,
                                   final boolean useStrongbox) throws InvalidAlgorithmParameterException {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            initializePre23(ctx, keyPairGenerator, keySize);
        } else {
            initialize23(keyPairGenerator, keySize, useStrongbox);
        }
    }

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
    @RequiresApi(Build.VERSION_CODES.P)
    @NonNull
    private static KeyGenParameterSpec.Builder applyHardwareIsolation(
            @NonNull final KeyGenParameterSpec.Builder builder) {
        return builder.setIsStrongBoxBacked(true);
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private static void initializePre23(@NonNull final Context ctx,
                                        @NonNull final KeyPairGenerator keyPairGenerator,
                                        final int keySize) throws InvalidAlgorithmParameterException {
        final Calendar calendar = Calendar.getInstance();
        final Date start = getNow(calendar);
        calendar.add(Calendar.YEAR, CertificateProperties.CERTIFICATE_VALIDITY_YEARS);
        final Date end = calendar.getTime();

        final KeyPairGeneratorSpec.Builder specBuilder = new KeyPairGeneratorSpec.Builder(ctx)
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
    //endregion
}
