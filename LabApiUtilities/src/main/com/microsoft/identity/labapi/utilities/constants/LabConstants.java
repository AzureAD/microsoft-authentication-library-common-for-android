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
package com.microsoft.identity.labapi.utilities.constants;

public class LabConstants {
    private static final String NONE = "none";

    static final class UserType {
        public static final String CLOUD = "cloud";
        public static final String FEDERATED = "federated";
        public static final String ON_PREM = "onprem";
        public static final String GUEST = "guest";
        public static final String MSA = "msa";
        public static final String B2C = "b2c";
    }

    static final class UserRole {
        public static final String NONE = LabConstants.NONE;
        public static final String CLOUD_DEVICE_ADMINISTRATOR = "clouddeviceadministrator";
    }

    static final class Mfa {
        public static final String NONE = LabConstants.NONE;
        public static final String MFA_ON_ALL = "mfaonall";
        public static final String AUTO_MFA_ON_ALL = "automfaonall";
    }

    static final class ProtectionPolicy {
        public static final String NONE = LabConstants.NONE;
        public static final String CA = "ca";
        public static final String CADJ = "cadj";
        public static final String MAM = "mam";
        public static final String MDM = "mdm";
        public static final String MDM_CA = "mdmca";
        public static final String MAM_CA = "mamca";
        public static final String TRUE_MAM_CA = "truemamca";
        public static final String MAM_SPO = "mamspo";
        public static final String BLOCKED = "blocked";
    }

    static final class HomeDomain {
        public static final String NONE = LabConstants.NONE;
        public static final String LAB_2 = "msidlab2.com";
        public static final String LAB_3 = "msidlab3.com";
        public static final String LAB_4 = "msidlab4.com";
    }

    static final class HomeUpn {
        public static final String NONE = LabConstants.NONE;
        public static final String LAB_2 = "gidlab@msidlab2.com";
        public static final String LAB_3 = "gidlab@msidlab3.com";
        public static final String LAB_4 = "gidlab@msidlab4.com";
    }

    static final class B2CProvider {
        public static final String NONE = LabConstants.NONE;
        public static final String AMAZON = "amazon";
        public static final String FACEBOOK = "facebook";
        public static final String GOOGLE = "google";
        public static final String LOCAL = "local";
        public static final String MICROSOFT = "microsoft";
        public static final String TWITTER = "twitter";
    }

    static final class FederationProvider {
        public static final String NONE = LabConstants.NONE;
        public static final String ADFS_V2 = "adfsv2";
        public static final String ADFS_V3 = "adfsv3";
        public static final String ADFS_V4 = "adfsv4";
        public static final String ADFS_V2019 = "adfsv2019";
        public static final String B2C = "b2c";
        public static final String PING = "ping";
        public static final String SHIBBOLETH = "shibboleth";
        public static final String CIAM = "ciam";
    }

    static final class AzureEnvironment {
        public static final String AZURE_B2C_CLOUD = "azureb2ccloud";
        public static final String AZURE_CHINA_CLOUD = "azurechinacloud";
        public static final String AZURE_CLOUD = "azurecloud";
        public static final String AZURE_GERMANY_CLOUD = "azuregermanycloud";
        public static final String AZURE_GERMANY_CLOUD_MIGRATED = "azuregermanycloudmigrated";
        public static final String AZURE_PPE = "azureppe";
        public static final String AZURE_US_GOVERNMENT = "azureusgovernment";
        public static final String AZURE_US_GOVERNMENT_MIGRATED = "azureusgovernmentmigrated";
    }

    static final class GuestHomeAzureEnvironment {
        public static final String AZURE_CHINA_CLOUD = AzureEnvironment.AZURE_CHINA_CLOUD;
        public static final String AZURE_CLOUD = AzureEnvironment.AZURE_CLOUD;
        public static final String AZURE_US_GOVERNMENT = AzureEnvironment.AZURE_US_GOVERNMENT;
    }

    static final class AppType {
        public static final String CLOUD = "cloud";
        public static final String ON_PREM = "on_prem";
    }

    static final class PublicClient {
        public static final String YES = "yes";
        public static final String NO = "no";
    }

    static final class SignInAudience {
        public static final String AZURE_AD_MY_ORG = "azureadmyorg";
        public static final String AZURE_AD_MULTIPLE_ORGS = "azureadmultipleorgs";
        public static final String AZURE_AD_AND_PERSONAL_MICROSOFT_ACCOUNT = "azureadandpersonalmicrosoftaccount";
    }

    static final class GuestHomedIn {
        public static final String NONE = LabConstants.NONE;
        public static final String ON_PREM = "onprem";
        public static final String HOST_AZURE_AD = "hostazuread";
    }

    static final class IsAdminConsented {
        public static final String YES = "yes";
        public static final String NO = "no";
    }

    static final class TempUserType {
        public static final String BASIC = "Basic";
        public static final String GLOBAL_MFA = "GlobalMFA";
        public static final String MFAONSPO = "MFAONSPO";
        public static final String MFAONEXO = "MFAONEXO";
        public static final String MAMCA = "MAMCA";
        public static final String MDMCA = "MDMCA";
    }

    static final class TempUserPolicy {
        public static final String GLOBAL_MFA = TempUserType.GLOBAL_MFA;
        public static final String MFAONSPO = TempUserType.MFAONSPO;
        public static final String MFAONEXO = TempUserType.MFAONEXO;
        public static final String MAMCA = TempUserType.MAMCA;
        public static final String MDMCA = TempUserType.MDMCA;
    }

    static final class ResetOperation {
        public static final String MFA = "MFA";
        public static final String PASSWORD = "Password";
    }

    static final class HasAltId {
        public static final String NO = "no";
        public static final String YES = "yes";
    }

    static final class AltIdSource {
        public static final String NONE = LabConstants.NONE;
        public static final String ON_PREM = "onprem";
    }

    static final class AltIdType {
        public static final String NONE = LabConstants.NONE;
        public static final String UPN = "upn";
    }

    static final class PasswordPolicyValidityPeriod {
        public static final String SIXTY = "60";
    }

    static final class PasswordPolicyNotificationDays {
        public static final String FOURTEEN = "14";
        public static final String SIXTY_ONE = "61";
    }

    static final class TokenLifetimePolicy {
        public static final String ORGANIZATION_DEFAULT = "OrganizationDefault";
        public static final String CAE = "CAE";
        public static final String CTL = "CTL";
    }

    static final class TokenType {
        public static final String ACCESS = "Access";
    }

    static final class TokenLifetime {
        public static final String DEFAULT = "default";
        public static final String SHORT = "short";
        public static final String LONG = "long";
    }
}
