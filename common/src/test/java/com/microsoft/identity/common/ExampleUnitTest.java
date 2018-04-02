package com.microsoft.identity.common;

import com.microsoft.identity.common.internal.providers.keys.CertificateCredential;
import com.microsoft.identity.common.internal.providers.keys.ClientCertificateMetadata;
import com.microsoft.identity.common.internal.providers.keys.KeyStoreConfiguration;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.mockito.Mockito.*;

import org.mockito.Mock;
import org.mockito.MockSettings;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.listeners.InvocationListener;
import org.mockito.mock.SerializableMode;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import java.io.IOException;
import java.math.BigInteger;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.interfaces.RSAPrivateKey;



import static org.junit.Assert.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest({KeyStore.class})
public class ExampleUnitTest {

    public static final String WINDOWS_MY_KEYSTORE = "Windows-MY";
    public static final String WINDOWS_ROOT_KEYSTORE = "Windows-ROOT";
    public static final String CERTIFICATE_ALIAS = "AutomationCertificate";
    public static final String WINDOWS_KEYSTORE_PROVDER = "SunMSCAPI";
    public static final char[] NULL_CHAR_ARRAY = new char[]{'\u0000'};

    @Mock
    private KeyStore keyStoreMock;

    private Key privateKey;

    @Before
    public void setup() throws Exception, KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException, UnrecoverableKeyException  {
        //keyStoreMock = KeyStore.getInstance(KeyStore.getDefaultType());
        //keyStoreMock.load(null, null);
        //MockitoAnnotations.initMocks(this);
        PowerMockito.mockStatic(KeyStore.class);
        keyStoreMock = PowerMockito.mock(KeyStore.class);
        Whitebox.setInternalState(keyStoreMock, "initialized", true);
        privateKey = new RSAPrivateKey() {
            @Override
            public BigInteger getPrivateExponent() {
                return null;
            }

            @Override
            public String getAlgorithm() {
                return null;
            }

            @Override
            public String getFormat() {
                return null;
            }

            @Override
            public byte[] getEncoded() {
                return new byte[0];
            }

            @Override
            public BigInteger getModulus() {
                return null;
            }
        };

        PowerMockito.doReturn(privateKey).when(keyStoreMock, "getKey",CERTIFICATE_ALIAS, null);

    }

    @Test
    public void test_clientCredential_IsCorrect() throws
            KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException, NoSuchProviderException,
            IOException, CertificateException, Exception{


        PowerMockito.doReturn(keyStoreMock).when(KeyStore.class, "getInstance", WINDOWS_MY_KEYSTORE, WINDOWS_KEYSTORE_PROVDER);
        //when(KeyStore.getInstance(WINDOWS_MY_KEYSTORE, WINDOWS_KEYSTORE_PROVDER)).thenReturn(keyStoreMock);

        CertificateCredential.CertificateCredentialBuilder builder = new CertificateCredential.CertificateCredentialBuilder("asdf");
        CertificateCredential cred = builder.keyStoreConfiguration(new KeyStoreConfiguration(WINDOWS_MY_KEYSTORE, WINDOWS_KEYSTORE_PROVDER, null))
                .clientCertificateMetadata(new ClientCertificateMetadata(CERTIFICATE_ALIAS, null))
                .build();

        assertEquals(privateKey, cred.getPrivateKey());

    }
}