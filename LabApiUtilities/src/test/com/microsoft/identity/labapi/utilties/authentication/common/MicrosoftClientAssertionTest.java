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
package com.microsoft.identity.labapi.utilties.authentication.common;

import com.microsoft.identity.labapi.utilities.authentication.common.CertificateCredential;
import com.microsoft.identity.labapi.utilities.authentication.common.ClientCertificateMetadata;
import com.microsoft.identity.labapi.utilities.authentication.common.KeyStoreConfiguration;
import com.microsoft.identity.labapi.utilities.authentication.common.MicrosoftClientAssertion;

import org.junit.Assert;
import org.junit.Test;

public class MicrosoftClientAssertionTest {

    private final static String CLIENT_ID = "some_client_id";
    private final static String CERTIFICATE_ALIAS = "AutomationRunner";
    private final static String KEYSTORE_TYPE = "Windows-MY";
    private final static String KEYSTORE_PROVIDER = "SunMSCAPI";
    private final static String AUDIENCE = "some_audience";

    @Test
    public void testCanCreateMicrosoftClientAssertionWithValidCertificate() {
        final MicrosoftClientAssertion microsoftClientAssertion = MicrosoftClientAssertion
                .builder()
                .clientId(CLIENT_ID)
                .audience(AUDIENCE)
                .certificateCredential(CertificateCredential.create(
                        new KeyStoreConfiguration(
                                KEYSTORE_TYPE, KEYSTORE_PROVIDER, null
                        ),
                        new ClientCertificateMetadata(CERTIFICATE_ALIAS, null)
                ))
                .build();

        Assert.assertNotNull(microsoftClientAssertion);
        Assert.assertNotNull(microsoftClientAssertion.getClientAssertion());
        Assert.assertNotNull(microsoftClientAssertion.getClientAssertionType());
        Assert.assertEquals(MicrosoftClientAssertion.CLIENT_ASSERTION_TYPE, microsoftClientAssertion.getClientAssertionType());
        Assert.assertTrue(microsoftClientAssertion.getClientAssertion().length() > 0);
    }


}
