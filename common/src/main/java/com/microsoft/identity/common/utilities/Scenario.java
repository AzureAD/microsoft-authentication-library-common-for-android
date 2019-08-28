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

package com.microsoft.identity.common.utilities;

import com.microsoft.identity.internal.test.labapi.model.TestConfiguration;

import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

public class Scenario {

    private TestConfiguration mTestConfiguration;
    private Credential mCredential;

    public TestConfiguration getTestConfiguration() {
        return mTestConfiguration;
    }

    public void setTestConfiguration(TestConfiguration testConfiguration) {
        this.mTestConfiguration = testConfiguration;
    }

    public Credential getCredential() {
        return mCredential;
    }

    public void setCredential(Credential credential) {
        this.mCredential = credential;
    }

    public static String getPasswordForUser(String upn) throws CertificateException, InterruptedException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, NoSuchProviderException, IOException {
        TestConfigurationQuery query = new TestConfigurationQuery();
        query.upn = upn;
        Scenario scenario = GetScenario(query);
        String password = scenario.getCredential().password;
        return password;
    }

    public static Scenario GetScenario(TestConfigurationQuery query) throws CertificateException, InterruptedException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, NoSuchProviderException, IOException {
        TestConfiguration tc = TestConfigurationHelper.GetTestConfiguration(query);
        String keyVaultLocation = tc.getUsers().getCredentialVaultKeyName();
        String secretName = keyVaultLocation.substring(keyVaultLocation.lastIndexOf('/') + 1);

        Credential credential = Secrets.GetCredential(tc.getUsers().getUpn(), secretName);

        Scenario scenario = new Scenario();
        scenario.setTestConfiguration(tc);
        scenario.setCredential(credential);

        return scenario;
    }
}
