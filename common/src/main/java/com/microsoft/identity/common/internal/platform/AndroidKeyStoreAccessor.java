package com.microsoft.identity.common.internal.platform;

import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;

import androidx.annotation.NonNull;

import com.microsoft.identity.common.java.crypto.CryptoSuite;
import com.microsoft.identity.common.java.crypto.IKeyAccessor;
import com.microsoft.identity.common.java.crypto.KeyStoreAccessor;
import com.microsoft.identity.common.java.crypto.RawKeyAccessor;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.interfaces.IPlatformComponents;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.ProviderException;
import java.security.cert.CertificateException;
import java.util.UUID;

import javax.crypto.KeyGenerator;

public class AndroidKeyStoreAccessor extends KeyStoreAccessor {
    protected static final int KEY_PURPOSES =  KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT | KeyProperties.PURPOSE_SIGN ;

    public AndroidKeyStoreAccessor(IPlatformComponents components) {
        super("AndroidKeyStore", components);
    }

    @Override
    public IKeyAccessor newInstance(@NonNull final CryptoSuite cipher, @NonNull final boolean needRawAccess)
            throws CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException, ClientException,
            NoSuchProviderException, InvalidAlgorithmParameterException {
        final String alias = UUID.randomUUID().toString();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !needRawAccess) {
            final KeyStore instance = KeyStore.getInstance(getKeyStoreName());
            instance.load(null);
            final String[] params = cipher.cipher().name().split("/");
            final KeyGenerator generator = KeyGenerator.getInstance(params[0], getKeyStoreName());
            KeyGenParameterSpec spec = null;
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    spec = new KeyGenParameterSpec.Builder(alias, KEY_PURPOSES)
                            .setIsStrongBoxBacked(true)
                            .setKeySize(cipher.keySize())
                            .setBlockModes(params[1])
                            .setEncryptionPaddings(params[2])
                            .setKeySize(cipher.keySize())
                            .build();
                } else {
                    spec = new KeyGenParameterSpec.Builder(alias, KEY_PURPOSES)
                            .setKeySize(cipher.keySize())
                            .setBlockModes(params[1])
                            .setEncryptionPaddings(params[2])
                            .setKeySize(cipher.keySize())
                            .build();
                }
                generator.init(spec);
                generator.generateKey();
            } catch (final ProviderException e) {
                if (e.getClass().getSimpleName().equals("StrongBoxUnavailableException")) {
                    spec = null;
                } else {
                    throw e;
                }
            }
            if (spec == null) {
                spec = new KeyGenParameterSpec.Builder(alias, KEY_PURPOSES)
                        .setKeySize(cipher.keySize())
                        .setBlockModes(params[1])
                        .setEncryptionPaddings(params[2])
                        .setKeySize(cipher.keySize())
                        .build();
                generator.init(spec);
                generator.generateKey();
            }

            final DeviceKeyManager<KeyStore.SecretKeyEntry> keyManager = new DeviceKeyManager<>(instance, alias, symmetricThumbprint(alias, instance));
            return new AndroidSecretKeyAccessor(keyManager, cipher) {
                @Override
                public byte[] sign(byte[] text) throws ClientException {
                    throw new UnsupportedOperationException("This key instance does not support signing");
                }

                @Override
                public boolean verify(byte[] text, byte[] signature) throws ClientException {
                    throw new UnsupportedOperationException("This key instance does not support verification");
                }
            };
        } else {
            KeyGenerator generator = KeyGenerator.getInstance(cipher.cipher().name().split("/")[0]);
            generator.init(cipher.keySize());
            byte[] key = generator.generateKey().getEncoded();
            return RawKeyAccessor.builder()
                    .suite(cipher)
                    .key(key)
                    .alias(alias).build();
        }
    }

}
