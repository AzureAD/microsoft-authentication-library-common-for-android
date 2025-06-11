// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// This code is licensed under the MIT License.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files(the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions :
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.
package com.microsoft.identity.common.crypto;

import android.content.Context;

import androidx.annotation.NonNull;

import com.microsoft.identity.common.internal.util.AndroidKeyStoreUtil;
import com.microsoft.identity.common.java.controllers.ExceptionAdapter;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.opentelemetry.AttributeName;
import com.microsoft.identity.common.java.opentelemetry.OTelUtility;
import com.microsoft.identity.common.java.opentelemetry.SpanExtension;
import com.microsoft.identity.common.java.opentelemetry.SpanName;
import com.microsoft.identity.common.java.util.StringUtil;
import com.microsoft.identity.common.logging.Logger;

import java.security.KeyPair;
import java.security.spec.AlgorithmParameterSpec;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.crypto.SecretKey;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Scope;


/**
 * Class responsible for generating key pairs used for wrapping secret keys.
 * Handles different strategies based on API levels and feature flags.
 */
public class AndroidKeyStoreRsaKekManager implements IKekManager {
    private static final String TAG = AndroidKeyStoreRsaKekManager.class.getSimpleName();


    /**
     * Algorithm used to generate wrapping key.
     */
    private static final String KEK_ALGORITHM = "RSA";


    private final String mKeyAlias;

    private final CryptoParameterSpecFactory mCryptoParameterSpecFactory;


    /**
     * Constructor for AndroidKeyStoreRsaKekManager.
     *
     * @param keyAlias The alias for the key to be generated
     * @param context  The context in which the key will be used, typically an Android Context
     */
    public AndroidKeyStoreRsaKekManager(@NonNull final String keyAlias,
                                        @NonNull final Context context
    ) {
        mKeyAlias = keyAlias;
        mCryptoParameterSpecFactory = new CryptoParameterSpecFactory(context, mKeyAlias);
    }

    @Override
    public boolean kekExists() throws ClientException {
        return AndroidKeyStoreUtil.readKey(mKeyAlias) != null;
    }


    @Override
    public SecretKey unwrapKey(byte[] wrappedSecretKey, final String SecretKeyAlgorithm) throws ClientException {
        final String methodTag = TAG + ":unwrapKey";
        final KeyPair keyPair = AndroidKeyStoreUtil.readKey(mKeyAlias);
        if (keyPair == null) {
            final ClientException clientException = new ClientException(
                    ClientException.KEY_LOAD_FAILURE,
                    "No existing keypair found for alias: " + mKeyAlias
            );
            Logger.error(methodTag, clientException.getMessage(), clientException);
            throw clientException;
        }
        final List<CryptoParameterSpecFactory.CipherSpec> specs = mCryptoParameterSpecFactory.getPrioritizedCipherParameterSpecs();
        final LinkedList<Throwable> exceptions = new LinkedList<>();
        for (CryptoParameterSpecFactory.CipherSpec spec : specs) {
            try {
                // Attempt to unwrap the key using the current spec
                return AndroidKeyStoreUtil.unwrap(
                        wrappedSecretKey,
                        SecretKeyAlgorithm,
                        keyPair,
                        spec.getTransformation(),
                        spec.getAlgorithmParameterSpecs()
                );
            } catch (final Throwable throwable) {
                Logger.warn(methodTag, "Failed to unwrap key with spec: " + spec.getTransformation());
                // Continue to the next spec if this one fails
                exceptions.add(throwable);
            }
        }
        for (final Throwable exception : exceptions) {
            Logger.error(methodTag, "Exception encountered during key pair generation: " + exception.getMessage(),exception);
        }

        // If we've tried all specs and failed, set span status and throw the last exception
        if (exceptions.isEmpty()) {
            exceptions.add(
                    new ClientException(
                            ClientException.UNKNOWN_CRYPTO_ERROR,
                            "Failed to unwrap key after trying all available specs.")
            );
        }
        throw ExceptionAdapter.clientExceptionFromException(exceptions.getLast());
    }

    public byte[] wrapKey(final SecretKey keyToWrap) throws ClientException {
        final String methodTag = TAG + ":wrapKey";
        KeyPair keyPair = AndroidKeyStoreUtil.readKey(mKeyAlias);
        if (keyPair == null) {
            Logger.info(methodTag, "No existing keypair found for alias. Generating a new keypair.");
            keyPair = generateKeyPair();
        }
        final CryptoParameterSpecFactory.CipherSpec cipherSpecs = mCryptoParameterSpecFactory.getPrimaryCipherParameterSpec();
        return AndroidKeyStoreUtil.wrap(
                keyToWrap,
                keyPair,
                cipherSpecs.getTransformation(),
                cipherSpecs.getAlgorithmParameterSpecs()
        );
    }

    @NonNull
    private KeyPair generateKeyPair() throws ClientException {
        final String methodTag = TAG + ":generateKeyPair";
        final Span span = OTelUtility.createSpanFromParent(SpanName.KeyPairGeneration.name(), SpanExtension.current().getSpanContext());
        final List<CryptoParameterSpecFactory.KeyGenSpec> specs = mCryptoParameterSpecFactory.getPrioritizedKeyGenParameterSpecs();
        // Track the last exception encountered to throw if all attempts fail
        final LinkedList<Throwable> exceptions = new LinkedList<>();

        try (final Scope ignored = SpanExtension.makeCurrentSpan(span)) {
            // Try each spec in order of priority
            for (CryptoParameterSpecFactory.KeyGenSpec spec : specs) {
                try {
                    final KeyPair keyPair = attemptKeyPairGeneration(spec.getKeyGenParameterSpec());

                    // Log the success with a descriptive name for telemetry
                    span.setAttribute(AttributeName.key_pair_gen_successful_method.name(), spec.getDescription());
                    Logger.info(methodTag, "Successfully generated key pair using: " + spec.getDescription());

                    // Return successful key pair
                    span.setStatus(StatusCode.OK);
                    return keyPair;
                } catch (final Throwable throwable) {
                    // Log the failure but continue to the next spec
                    Logger.warn(methodTag, "Failed to generate keypair with spec: " + spec.getDescription());
                    if (!StringUtil.isNullOrEmpty(throwable.getMessage())) {
                        span.setAttribute(AttributeName.keypair_gen_exception.name(), throwable.getMessage());
                    }
                    exceptions.add(throwable);
                }
            }
            for (final Throwable exception : exceptions) {
                Logger.error(methodTag, "Exception encountered during key pair generation: " + exception.getMessage(),exception);
            }

            // If we've tried all specs and failed, set span status and throw the last exception
            if (exceptions.isEmpty()) {
                exceptions.add(
                        new ClientException(
                                ClientException.UNKNOWN_CRYPTO_ERROR,
                                "Failed to generate key pair after trying all available specs.")
                );
            }
            span.setStatus(StatusCode.ERROR);
            span.recordException(exceptions.getLast());
            Logger.error(methodTag, "Failed to generate key pair with all available specs", exceptions.getLast());
            throw ExceptionAdapter.clientExceptionFromException(exceptions.getLast());
        } finally {
            span.end(); // Span is ended only once, after all attempts
        }
    }

    private KeyPair attemptKeyPairGeneration(@lombok.NonNull final AlgorithmParameterSpec keyPairGenSpec) throws ClientException {
        final long keypairGenStartTime = System.currentTimeMillis();
        final KeyPair keyPair = AndroidKeyStoreUtil.generateKeyPair(KEK_ALGORITHM, keyPairGenSpec);
        recordKeyGenerationTime(keypairGenStartTime);
        return keyPair;
    }

    private void recordKeyGenerationTime(long keypairGenStartTime) {
        long elapsedTime = System.currentTimeMillis() - keypairGenStartTime;
        SpanExtension.current().setAttribute(AttributeName.elapsed_time_keypair_generation.name(), elapsedTime);
    }

    @NonNull
    @Override
    public String getCipherTransformation() {
        return mCryptoParameterSpecFactory.getPrimaryCipherParameterSpec().getTransformation();
    }
}
