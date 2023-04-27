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
package com.microsoft.identity.common.internal.ui.webview.certbasedauth;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.math.BigInteger;
import java.security.Principal;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

@RunWith(RobolectricTestRunner.class)
public abstract class AbstractSmartcardCertBasedAuthChallengeHandlerTest extends AbstractCertBasedAuthTest {

    protected TestClientCertRequest mRequest;

    @Before
    public void handlerSetUp() {
        mRequest = new TestClientCertRequest();
    }

    @Test
    public void testNoCertsOnSmartcard() {
        setAndProcessChallengeHandler(new ArrayList<>());
        checkIfCorrectDialogIsShowing(TestDialog.error);
        checkIfProceeded(false);
    }

    @Test
    public void testCancelAtPickerDialog() {
        setAndProcessChallengeHandler(getMockCertList());
        checkIfCorrectDialogIsShowing(TestDialog.cert_picker);
        final ICancelCbaCallback callback =  mDialogHolder.getCancelCbaCallback();
        assertNotNull(callback);
        callback.onCancel();
        checkIfCorrectDialogIsShowing(null);
        checkIfProceeded(false);
    }

    @Test
    public void testCancelAtPinDialog() {
        setAndProcessChallengeHandler(getMockCertList());
        checkIfCorrectDialogIsShowing(TestDialog.cert_picker);
        goToPinDialog();
        final ICancelCbaCallback callback = mDialogHolder.getCancelCbaCallback();
        assertNotNull(callback);
        callback.onCancel();
        checkIfCorrectDialogIsShowing(null);
        checkIfProceeded(false);
    }

    @Test
    public void testExceptionThrownWhenGettingCertDetailsList() {
        final List<X509Certificate> certList = new ArrayList<>();
        certList.add(getMockCertificate("Exception", "Exception"));
        setAndProcessChallengeHandler(certList);
        checkIfCorrectDialogIsShowing(TestDialog.error);
        checkIfProceeded(false);
    }

    @Test
    public abstract void testLockedOut();

    @Test
    public abstract void testExceptionThrownWhenVerifyingPin();

    @Test
    public abstract void testExceptionThrownWhenGettingKey();

    @Test
    public abstract void testProceed();

    protected abstract void setAndProcessChallengeHandler(@NonNull final List<X509Certificate> certList);

    protected void goToPinDialog() {
        final SmartcardCertPickerDialog.PositiveButtonListener listener = mDialogHolder.getCertPickerPositiveButtonListener();
        assertNotNull(listener);
        listener.onClick(mDialogHolder.getCertList().get(0));
        checkIfCorrectDialogIsShowing(TestDialog.pin);
    }

    protected void checkIfProceeded(final boolean isExpected) {
        if (isExpected) {
            assertTrue(mRequest.isProceeded());
            assertFalse(mRequest.isCancelled());
        } else {
            assertFalse(mRequest.isProceeded());
            assertTrue(mRequest.isCancelled());
        }
    }

    @NonNull
    protected List<X509Certificate> getMockCertList() {
        final X509Certificate cert1 = getMockCertificate("SomeIssuer1", "SomeSubject1");
        final X509Certificate cert2 = getMockCertificate("SomeIssuer2", "SomeSubject2");
        final List<X509Certificate> certList = new ArrayList<>();
        certList.add(cert1);
        certList.add(cert2);
        return certList;
    }

    @NonNull
    protected X509Certificate getMockCertificate(@Nullable final String issuerDNName, @Nullable final String subjectDNName) {
        return new X509Certificate() {
            @Override
            public void checkValidity() {

            }

            @Override
            public void checkValidity(Date date) {

            }

            @Override
            public int getVersion() {
                return 0;
            }

            @Override
            public BigInteger getSerialNumber() {
                return null;
            }

            @Override
            public Principal getIssuerDN() {
                return new Principal() {
                    @Override
                    public String getName() {
                        return issuerDNName;
                    }
                };
            }

            @Override
            public Principal getSubjectDN() {
                return new Principal() {
                    @Override
                    public String getName() {
                        return subjectDNName;
                    }
                };
            }

            @Override
            public Date getNotBefore() {
                return null;
            }

            @Override
            public Date getNotAfter() {
                return null;
            }

            @Override
            public byte[] getTBSCertificate() {
                return new byte[0];
            }

            @Override
            public byte[] getSignature() {
                return new byte[0];
            }

            @Override
            public String getSigAlgName() {
                return null;
            }

            @Override
            public String getSigAlgOID() {
                return null;
            }

            @Override
            public byte[] getSigAlgParams() {
                return new byte[0];
            }

            @Override
            public boolean[] getIssuerUniqueID() {
                return new boolean[0];
            }

            @Override
            public boolean[] getSubjectUniqueID() {
                return new boolean[0];
            }

            @Override
            public boolean[] getKeyUsage() {
                return new boolean[0];
            }

            @Override
            public int getBasicConstraints() {
                return 0;
            }

            @Override
            public byte[] getEncoded() {
                return new byte[0];
            }

            @Override
            public void verify(PublicKey publicKey) {

            }

            @Override
            public void verify(PublicKey publicKey, String s) {

            }

            @NonNull
            @Override
            public String toString() {
                return issuerDNName + " " + subjectDNName;
            }

            @Override
            public PublicKey getPublicKey() {
                return new PublicKey() {
                    @Override
                    public String getAlgorithm() {
                        return "N/A";
                    }

                    @Override
                    public String getFormat() {
                        return null;
                    }

                    @Override
                    public byte[] getEncoded() {
                        return new byte[0];
                    }
                };
            }

            @Override
            public boolean hasUnsupportedCriticalExtension() {
                return false;
            }

            @Override
            public Set<String> getCriticalExtensionOIDs() {
                return null;
            }

            @Override
            public Set<String> getNonCriticalExtensionOIDs() {
                return null;
            }

            @Override
            public byte[] getExtensionValue(String s) {
                return new byte[0];
            }
        };
    }
}
