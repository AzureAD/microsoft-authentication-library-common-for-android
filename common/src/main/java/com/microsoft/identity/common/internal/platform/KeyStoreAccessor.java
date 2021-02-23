package com.microsoft.identity.common.internal.platform;

import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;

import com.microsoft.identity.common.exception.ClientException;
import com.microsoft.identity.common.internal.util.Supplier;

import java.io.IOException;
import java.nio.charset.Charset;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.util.UUID;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

/**
 * This class is a static factory providing access to KeyStore objects.  Since all of the construction
 * in DevicePopManager is package private, and we're really interested in only a few operations, just
 * construct new instances here, and expose an interface that gives us the functionality that we need.
 */
public class KeyStoreAccessor {
    /**
     * The name of the KeyStore to use.
     */
    private static final String ANDROID_KEYSTORE = "AndroidKeyStore";
    public static final Charset UTF8 = Charset.forName("UTF-8");
    private static final int KEY_PURPOSES =  KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT;// |KeyProperties.PURPOSE_SIGN | KeyProperties.PURPOSE_VERIFY;
    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * For a given alias, construct an accessor for a KeyStore backed entry given that alias.
     * @param alias The key alias.
     * @param suite The cipher type of this key.
     * @return a key accessor for the use of that particular key.
     * @throws CertificateException
     * @throws NoSuchAlgorithmException
     * @throws KeyStoreException
     * @throws IOException
     */
    public static KeyAccessor forAlias(final String alias, final CryptoSuite suite) throws CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException {
        final IDevicePopManager popManager = new DevicePopManager(alias);
        if (suite instanceof IDevicePopManager.Cipher) {
            return getKeyAccessor((IDevicePopManager.Cipher) suite, popManager);
        }
        final KeyStore instance = KeyStore.getInstance(ANDROID_KEYSTORE);
        final DeviceKeyManager<KeyStore.SecretKeyEntry> keyManager = new DeviceKeyManager<>(instance, alias, new Supplier<byte[]>() {
            @Override
            public byte[] get() {
                try {
                    KeyStore.Entry entry = instance.getEntry(alias, null);
                    if (entry instanceof KeyStore.SecretKeyEntry) {
                        SecretKey key = ((KeyStore.SecretKeyEntry) entry).getSecretKey();
                        Cipher cipher = Cipher.getInstance(key.getAlgorithm());
                        return cipher.doFinal((key.getAlgorithm() + cipher.getBlockSize() + cipher.getParameters()).getBytes(UTF8));
                    } else {
                        return null;
                    }
                } catch (KeyStoreException | BadPaddingException | NoSuchAlgorithmException | IllegalBlockSizeException | UnrecoverableEntryException | NoSuchPaddingException e) {
                    //TODO: logging
                    return null;
                }
            }
        });
        return new SecretKeyAccessor(keyManager, suite);
    }

    private static final KeyAccessor getKeyAccessor(final IDevicePopManager.Cipher cipher, final IDevicePopManager popManager) {
        return new AsymmetricKeyAccessor() {

            @Override
            public String getPublicKey(IDevicePopManager.PublicKeyFormat format) throws ClientException {
                return popManager.getPublicKey(format);
            }

            @Override
            public PublicKey getPublicKey() throws UnrecoverableEntryException, NoSuchAlgorithmException, KeyStoreException {
                return popManager.getPublicKey();
            }

            @Override
            public byte[] encrypt(byte[] plaintext) throws ClientException {
                return popManager.encrypt(cipher, plaintext);
            }

            @Override
            public byte[] decrypt(byte[] ciphertext) throws ClientException {
                return popManager.decrypt(cipher, ciphertext);
            }

            @Override
            public byte[] sign(byte[] text, IDevicePopManager.SigningAlgorithm alg) throws ClientException {
                return popManager.sign(alg, text);
            }

            @Override
            public boolean verify(byte[] text, IDevicePopManager.SigningAlgorithm alg, byte[] signature) throws ClientException {
                return popManager.verify(alg, text, signature);
            }

            @Override
            public byte[] getThumprint() throws ClientException {
                return popManager.getAsymmetricKeyThumbprint().getBytes(UTF8);
            }
        };
    }

    /**
     * Construct an accessor for a KeyStore backed entry using a random alias.
     * @param cipher The cipher type of this key.
     * @return a key accessor for the use of that particular key.
     * @throws CertificateException
     * @throws NoSuchAlgorithmException
     * @throws KeyStoreException
     * @throws IOException
     */
    public static KeyAccessor newInstance(final IDevicePopManager.Cipher cipher) throws CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException, ClientException {
        String alias = UUID.randomUUID().toString();
        final IDevicePopManager popManager = new DevicePopManager(alias);
        popManager.generateAsymmetricKey(null);
        return getKeyAccessor(cipher, popManager);
    }
    /**
     * Construct an accessor for a KeyStore backed entry using a random alias.
     * @param cipher The cipher type of this key.
     * @return a key accessor for the use of that particular key.
     * @throws CertificateException
     * @throws NoSuchAlgorithmException
     * @throws KeyStoreException
     * @throws IOException
     */
    public static KeyAccessor newInstance(SymmetricCipher cipher) throws CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException, ClientException, NoSuchProviderException, InvalidAlgorithmParameterException {
        String alias = UUID.randomUUID().toString();
        //TODO: Everything here looks like it should work, but we get an exception when trying to
        //      initialize the resulting cipher, at least on my samsung device.
        //java.lang.NullPointerException: Attempt to get length of null array
        //	at com.android.org.bouncycastle.crypto.params.KeyParameter.<init>(KeyParameter.java:13)
        //	at com.android.org.bouncycastle.jcajce.provider.symmetric.util.BaseBlockCipher.engineInit(BaseBlockCipher.java:692)
        //	at com.android.org.bouncycastle.jcajce.provider.symmetric.util.BaseBlockCipher.engineInit(BaseBlockCipher.java:1076)
        //	at javax.crypto.Cipher.tryTransformWithProvider(Cipher.java:2984)
        //	at javax.crypto.Cipher.tryCombinations(Cipher.java:2891)
        //	at javax.crypto.Cipher$SpiAndProviderUpdater.updateAndGetSpiAndProvider(Cipher.java:2796)
        //	at javax.crypto.Cipher.chooseProvider(Cipher.java:773)
        //	at javax.crypto.Cipher.init(Cipher.java:1143)
        //	at javax.crypto.Cipher.init(Cipher.java:1084)
        if (android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.R) {
            final KeyStore instance = KeyStore.getInstance(ANDROID_KEYSTORE);
            instance.load(null);
            KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE);
            KeyGenParameterSpec spec = new KeyGenParameterSpec.Builder(alias, KEY_PURPOSES)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                    .build();
            generator.init(spec);
            generator.generateKey();

            final DeviceKeyManager<KeyStore.SecretKeyEntry> keyManager = new DeviceKeyManager<>(instance, alias, new Supplier<byte[]>() {
                @Override
                public byte[] get() {
                    try {
                        KeyStore.Entry entry = instance.getEntry(alias, null);
                        if (entry instanceof KeyStore.SecretKeyEntry) {
                            SecretKey key = ((KeyStore.SecretKeyEntry) entry).getSecretKey();
                            Cipher cipher = Cipher.getInstance(key.getAlgorithm());
                            return cipher.doFinal((key.getAlgorithm() + cipher.getBlockSize() + cipher.getParameters()).getBytes(UTF8));
                        } else {
                            return null;
                        }
                    } catch (KeyStoreException | BadPaddingException | NoSuchAlgorithmException | IllegalBlockSizeException | UnrecoverableEntryException | NoSuchPaddingException e) {
                        //TODO: logging
                        return null;
                    }
                }
            });
            return new SecretKeyAccessor(keyManager, cipher);
        } else {
            byte[] key = new byte[cipher.keySize()/8];
            RANDOM.nextBytes(key);
            return new RawKeyAccessor(cipher, key);
        }
    }

    //TODO: Fill this in.
    public static KeyAccessor importSymmetricKey(SymmetricCipher cipher, String key_jwe, KeyAccessor stk_accessor) {
        throw new UnsupportedOperationException("This operation is not yet supported");
    }
}
