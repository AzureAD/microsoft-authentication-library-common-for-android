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
package com.microsoft.identity.common.java.broker;

public final class BrokerAccountDataName {

    private BrokerAccountDataName() {
    }

    /**
     * Key for the data used by the PRTv3 protocol.
     */
    public static final String PRT_V3 = "workplaceJoin.key.prt3";

    public static final String PRT = "workplaceJoin.key.prt";

    public static final String PRT_AUTHORITY = "workplaceJoin.key.prt.authority";

    public static final String BRT_AUTHORITY = "workplaceJoin.key.brt.authority";

    public static final String PRT_ACQUISITION_TIME = "workplaceJoin.key.prt.acquisition.time";

    public static final String PRT_ID_TOKEN = "workplaceJoin.key.prt.idtoken.key";

    public static final String EMAIL = "workplaceJoin.key.email";

    /**
     * String of key for account name.
     */
    public static final String ACCOUNT_HOME_ACCOUNT_ID = "account.home.account.id";

    /**
     * String of key for account id token.
     */
    public static final String ACCOUNT_IDTOKEN = "account.idtoken";

    /**
     * String of key for user id.
     */
    public static final String ACCOUNT_USERINFO_USERID = "account.userinfo.userid";

    /**
     * String of key for user id list.
     */
    public static final String ACCOUNT_USERINFO_USERID_LIST = "account.userinfo.userid.list";

    /**
     * String of key for given name.
     */
    public static final String ACCOUNT_USERINFO_GIVEN_NAME = "account.userinfo.given.name";

    /**
     * String of key for family name.
     */
    public static final String ACCOUNT_USERINFO_FAMILY_NAME = "account.userinfo.family.name";

    /**
     * String of key for identity provider.
     */
    public static final String ACCOUNT_USERINFO_IDENTITY_PROVIDER = "account.userinfo.identity.provider";

    /**
     * String of key for displayable id.
     */
    public static final String ACCOUNT_USERINFO_USERID_DISPLAYABLE = "account.userinfo.userid.displayable";

    /**
     * String of key for tenant id.
     */
    public static final String ACCOUNT_USERINFO_TENANTID = "account.userinfo.tenantid";

    /**
     * String of key for environment.
     */
    public static final String ACCOUNT_USERINFO_ENVIRONMENT = "account.userinfo.environment";

    /**
     * String of key for authority type.
     */
    public static final String ACCOUNT_USERINFO_AUTHORITY_TYPE = "account.userinfo.authority.type";

    /**
     * String of key for account id token record.
     */
    public static final String ACCOUNT_USERINFO_ID_TOKEN = "account.userinfo.id.token";

    /**
     * String of key for user data broker RT.
     */
    public static final String USERDATA_BROKER_RT = "userdata.broker.rt";


    public static final String DATA_IS_NGC = "com.microsoft.workaccount.isNGC";
}
