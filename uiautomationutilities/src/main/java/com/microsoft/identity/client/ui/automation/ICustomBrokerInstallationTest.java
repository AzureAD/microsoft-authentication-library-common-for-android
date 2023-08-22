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
package com.microsoft.identity.client.ui.automation;

import com.microsoft.identity.client.ui.automation.broker.BrokerCompanyPortal;
import com.microsoft.identity.client.ui.automation.broker.BrokerHost;
import com.microsoft.identity.client.ui.automation.broker.BrokerLTW;
import com.microsoft.identity.client.ui.automation.broker.BrokerMicrosoftAuthenticator;

/**
 * An interface to facilitate testing a very specific app installation order with support for
 * old and current version of the app.
 */
public interface ICustomBrokerInstallationTest {

    /**
     * Install old/legacy BrokerHost.
     */
    default BrokerHost installOldBrokerHost(){
        final BrokerHost brokerHost = new BrokerHost(BrokerHost.OLD_BROKER_HOST_APK,
                BrokerHost.BROKER_HOST_APK);
        brokerHost.install();
        return brokerHost;
    }
    /**
     * Install updated BrokerHost.
     */
    default BrokerHost installBrokerHost(){
        final BrokerHost brokerHost = new BrokerHost();
        brokerHost.install();
        return brokerHost;
    }

    /**
     * Install old/legacy Authenticator.
     */
    default BrokerMicrosoftAuthenticator installOldAuthenticator(){
        final BrokerMicrosoftAuthenticator authenticator = new BrokerMicrosoftAuthenticator(BrokerMicrosoftAuthenticator.OLD_AUTHENTICATOR_APK,
                BrokerMicrosoftAuthenticator.AUTHENTICATOR_APK);
        authenticator.install();
        return authenticator;
    }
    /**
     * Install updated Authenticator.
     */
    default BrokerMicrosoftAuthenticator installAuthenticator(){
        final BrokerMicrosoftAuthenticator authenticator = new BrokerMicrosoftAuthenticator();
        authenticator.install();
        return authenticator;
    }

    /**
     * Install old/legacy Company Portal.
     */
    default BrokerCompanyPortal installOldCompanyPortal(){
        final BrokerCompanyPortal companyPortal = new BrokerCompanyPortal(BrokerCompanyPortal.OLD_COMPANY_PORTAL_APK,
                BrokerCompanyPortal.COMPANY_PORTAL_APK);
        companyPortal.install();
        return companyPortal;
    }
    /**
     * Install updated Company Portal.
     */
    default BrokerCompanyPortal installCompanyPortal(){
        final BrokerCompanyPortal companyPortal = new BrokerCompanyPortal();
        companyPortal.install();
        return companyPortal;
    }

    /**
     * Install old/legacy LTW.
     */
    default BrokerLTW installOldLtw(){
        final BrokerLTW ltw = new BrokerLTW(BrokerLTW.OLD_BROKER_LTW_APK,
                BrokerLTW.BROKER_LTW_APK);
        ltw.install();
        return ltw;
    }
    /**
     * Install updated LTW.
     */
    default BrokerLTW installLtw(){
        final BrokerLTW ltw = new BrokerLTW();
        ltw.install();
        return ltw;
    }

    // TODO: LTW folks, expand this to include msaltestapp, adaltestapp, oneuathtestapp
    
}
