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
package com.microsoft.identity.common.java.challengehandlers;

import static com.microsoft.identity.common.java.challengehandlers.MockData.PKEYAUTH_AUTH_ENDPOINT_URL;
import static com.microsoft.identity.common.java.challengehandlers.MockData.PKEYAUTH_MOCK_VERSION;
import static com.microsoft.identity.common.java.challengehandlers.MockData.PKEYAUTH_CERT_AUTHORITIES;
import static com.microsoft.identity.common.java.challengehandlers.MockData.PKEYAUTH_AUTH_ENDPOINT_CONTEXT;
import static com.microsoft.identity.common.java.challengehandlers.MockData.PKEYAUTH_AUTH_ENDPOINT_NONCE;
import static com.microsoft.identity.common.java.challengehandlers.MockData.PKEYAUTH_AUTH_ENDPOINT_SUBMIT_URL;
import static com.microsoft.identity.common.java.challengehandlers.MockData.PKEYAUTH_TOKEN_ENDPOINT_AUTHORITY;
import static com.microsoft.identity.common.java.challengehandlers.MockData.PKEYAUTH_TOKEN_ENDPOINT_CHALLENGE_HEADER;
import static com.microsoft.identity.common.java.challengehandlers.MockData.PKEYAUTH_TOKEN_ENDPOINT_CONTEXT;
import static com.microsoft.identity.common.java.challengehandlers.MockData.PKEYAUTH_TOKEN_ENDPOINT_NONCE;
import static com.microsoft.identity.common.java.exception.ErrorStrings.DEVICE_CERTIFICATE_REQUEST_INVALID;

import com.microsoft.identity.common.java.exception.ClientException;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.UnsupportedEncodingException;

@RunWith(JUnit4.class)
public class PKeyAuthChallengeFactoryTest {

    private final String[] CERT_AUTHORITIES = new String[]{
            "OU=82dbaca4-3e81-46ca-9c73-0950c1eaca97,CN=MS-Organization-Access,DC=windows,DC=net"
    };
    private final String PKEYAUTH_AUTH_ENDPOINT_CONTEXT = "rQIIAa2RvW_TQBjGfXGSNqGFlImpytBKFegcn78dCYmmKZB-JAqFfi3obN8lVzl24kubNjMCxIQ6MgESA5mgYkAMiKlDB5QRwYZgQQwdI4EEKVX_A57h9z7TM_zedAJJumRfFhVJzk9pjuw5lqtAmyoG1DRkQsvxCFSpTFRDN6hF5OhiOjP_-3Dy-9JIef94vPIiR7_0QKXB8Xa7ns_l3LAhNThtS8wjQZu19yTXZ8Mmcdxo-kTyQxf7OdQptdZXWoUtszOtFOoIbTLKg1antrcYbUyrxbcA9AHYjwn9GPgcu1CZHY4rJwgj1iVPxLKiWVZeUW0TIw1DKnsIajpSoYVtAyqGRXXbVi1H16FsI4u6JoIKdUyoWbYNMbJ0iC0FuzKl1FZRT5yST6PCE57CPWtnORDHw6iGA9bFbRYG_Eic2eYkkiKCvWw2bJKAedmQUp8F5C52XcJ5thmFlPnkkwi-ipPM87FzrcH_XU0KgwZzo5CHQ11Db_04-BEfGwWZxKVUVpiZkMEgDp4nhsIn3qNvv-69mTt48HTjI6fCUSK3q1d3iwurZVIv-o621aTImMdygemVBWPZWa0u8PWdkotcs1W9aubR4yQ4SMZHhQw4So4tr8wuSbOBF4XMO07GHo4I71L_-4ODlNBLX1nb4V7neql2e_fWjRbVUbdwZ66EFpVFo9Je3URRszVXM9aUctCtvkqD_jnwYVwYnH_26PD1_Zd_ft78Cw2.AQABAAEAAAD--DLA3VO7QrddgJg7WevrS-SEPcQWHvmlxD6gDZD4tzKpufY8FnfGkxc4ZD_1S0UqyCs1aOsjmFmxdz8JIxakFka_Q5aaf-RlurMM4UAp9qjBryW6lR2_GJCMvauswa2GVowfL7H099Oo2j1EBe0ddzAZvImhXh7Q-fIXWNfA20OTMPSdKHPWRlgJJrxqG2EW5XcXwrUiCQ3Zw3vbDtULiibI8DvWfH2AbYachYABPaBHQu64twJXtOIZRUPMd5cQvFtNzBHBE_x907m9bB4F_kPeYkMZTXr42CCGDToVnsPwcx_dd2Zm7yxeBrN6PspSq_FysaHy0yJ1dYh3kOT2AjhEwvm9vLh2uPq2wbeJbHZ-sWvaSAeJpON8Q-AxNCxB8dTiAv96kQfDkryshQabQsb86CjIHY26oeBuXFZ4pNy6jZEEXdJ7z2pD5iuZFH9zpyQTQGuq6esy1nUKRA_feNtqBqIVo6HwRgAzmz1COwRdY7B_8NEy8XbWLercIn-d9o9PCVARSSDGs4e8AhZMV1w-a6TSNlW7D12d6FzwhwG5lRbxjeq1Xb3RuZ67OQylNYT0nEmqhSeiG3xuRCtFnaffk15lFmUSUGV-Ki98CGslIP7IwXQKOOwR9RQlaZl0e-Q-_VnPtl-2E5vX0eh1odBRayxnuCufRTuDT5ra1cvU5YbuLWTGxW2oNaQGMZuhH0r84bpmiULMfzFdh1uYSoQXVNEW5jJrw5FwF_pAD54sLgqGGKGkb4NEkjKV-R8gAA";
    private final String PKEYAUTH_AUTH_ENDPOINT_NONCE = "MV50ggQuxKocqa9BV1Wzp8Gs16sfijXXlfpZJgYEpP4";
    private final String PKEYAUTH_AUTH_ENDPOINT_SUBMIT_URL = "https://login.microsoftonline.com/common/DeviceAuthPKeyAuth";
    private final String PKEYAUTH_AUTH_ENDPOINT_URL = "urn:http-auth:PKeyAuth?CertAuthorities=OU%3d82dbaca4-3e81-46ca-9c73-0950c1eaca97%2cCN%3dMS-Organization-Access%2cDC%3dwindows%2cDC%3dnet&Version=1.0&Context=rQIIAa2RvW_TQBjGfXGSNqGFlImpytBKFegcn78dCYmmKZB-JAqFfi3obN8lVzl24kubNjMCxIQ6MgESA5mgYkAMiKlDB5QRwYZgQQwdI4EEKVX_A57h9z7TM_zedAJJumRfFhVJzk9pjuw5lqtAmyoG1DRkQsvxCFSpTFRDN6hF5OhiOjP_-3Dy-9JIef94vPIiR7_0QKXB8Xa7ns_l3LAhNThtS8wjQZu19yTXZ8Mmcdxo-kTyQxf7OdQptdZXWoUtszOtFOoIbTLKg1antrcYbUyrxbcA9AHYjwn9GPgcu1CZHY4rJwgj1iVPxLKiWVZeUW0TIw1DKnsIajpSoYVtAyqGRXXbVi1H16FsI4u6JoIKdUyoWbYNMbJ0iC0FuzKl1FZRT5yST6PCE57CPWtnORDHw6iGA9bFbRYG_Eic2eYkkiKCvWw2bJKAedmQUp8F5C52XcJ5thmFlPnkkwi-ipPM87FzrcH_XU0KgwZzo5CHQ11Db_04-BEfGwWZxKVUVpiZkMEgDp4nhsIn3qNvv-69mTt48HTjI6fCUSK3q1d3iwurZVIv-o621aTImMdygemVBWPZWa0u8PWdkotcs1W9aubR4yQ4SMZHhQw4So4tr8wuSbOBF4XMO07GHo4I71L_-4ODlNBLX1nb4V7neql2e_fWjRbVUbdwZ66EFpVFo9Je3URRszVXM9aUctCtvkqD_jnwYVwYnH_26PD1_Zd_ft78Cw2.AQABAAEAAAD--DLA3VO7QrddgJg7WevrS-SEPcQWHvmlxD6gDZD4tzKpufY8FnfGkxc4ZD_1S0UqyCs1aOsjmFmxdz8JIxakFka_Q5aaf-RlurMM4UAp9qjBryW6lR2_GJCMvauswa2GVowfL7H099Oo2j1EBe0ddzAZvImhXh7Q-fIXWNfA20OTMPSdKHPWRlgJJrxqG2EW5XcXwrUiCQ3Zw3vbDtULiibI8DvWfH2AbYachYABPaBHQu64twJXtOIZRUPMd5cQvFtNzBHBE_x907m9bB4F_kPeYkMZTXr42CCGDToVnsPwcx_dd2Zm7yxeBrN6PspSq_FysaHy0yJ1dYh3kOT2AjhEwvm9vLh2uPq2wbeJbHZ-sWvaSAeJpON8Q-AxNCxB8dTiAv96kQfDkryshQabQsb86CjIHY26oeBuXFZ4pNy6jZEEXdJ7z2pD5iuZFH9zpyQTQGuq6esy1nUKRA_feNtqBqIVo6HwRgAzmz1COwRdY7B_8NEy8XbWLercIn-d9o9PCVARSSDGs4e8AhZMV1w-a6TSNlW7D12d6FzwhwG5lRbxjeq1Xb3RuZ67OQylNYT0nEmqhSeiG3xuRCtFnaffk15lFmUSUGV-Ki98CGslIP7IwXQKOOwR9RQlaZl0e-Q-_VnPtl-2E5vX0eh1odBRayxnuCufRTuDT5ra1cvU5YbuLWTGxW2oNaQGMZuhH0r84bpmiULMfzFdh1uYSoQXVNEW5jJrw5FwF_pAD54sLgqGGKGkb4NEkjKV-R8gAA&nonce=MV50ggQuxKocqa9BV1Wzp8Gs16sfijXXlfpZJgYEpP4&SubmitUrl=https%3a%2f%2flogin.microsoftonline.com%2fcommon%2fDeviceAuthPKeyAuth";

    private final String PKEYAUTH_TOKEN_ENDPOINT_CONTEXT = "rQIIAeNiNdQz1bPUYjbSM7BSMUkySEmySDbStUwzMtM1MTE017VISknVNU4zSDU2MzVLs0g1KBLiEjjb_F946uHJbnNXzk0IdlaqfMHIeIGJ8RYTt79jaUmGUUh-dmreKmYVAwgw1gWRECIZxoKBTcwqaWYmpokplka6qcYWKbomKYaJukmmhga6KYZJBpaJ5iaJFsmJp5jVS4tTi_SKUhNTFPILUvMygVRaWk5mXmp8YnJyanGxQkFRflpmTuoEFsYLLCyvWMQ4GAVYJTgVGDQEDRi9ODhYBBgkGBQYFrEC3X_ObG2x7Ul_31bX6eesLnYxTGCTmcDGMYGNbQKb8CY2Fg4GAcZTbDy-wY4-eo55KUX5mSkf2Bg72Bl-cDLM4GK8wM14gJfhB9-c81N61q_9_8YDAA2.AQABAAEAAAD--DLA3VO7QrddgJg7WevritOStff29rmP7pxoNaAajotJtvq_lSIqVLdNZgszjRcnTW3gQPrpYxT6hydXcUMEuASqhFvxXPBs0JVggjbyO9s9oDTZQqRsoz9w1cQPpcagNBBUhdyw89DU4XvM4HWIQ_pdn6FcUX0ixrkCyNR6e3U-1TNP3ctH_l5kq--9IWJ8MIs4rrchcCSu_A7b66fOwyVFTJnQhnpTLd9d5crH1lAlYOuATsSKAu7q2oR5UNEMMqJS8gemwdxl2NtyA0yh4cyaxWOeeEdEVoRAehKCe0xFVgsfHoUOdf6WLHwWnWCh1ptV1xdaYVyLFCd1TFbUoFrM_wNWzfFo9vOT1Vy_JSSkkLmn3tZEYw4tCPKnG04b-qJPciDSVg2m9DIfZCYreQxgPCDAwjFhnIOmPz_-TP4dpS2bP-HVev0C1-l3t00EyNQDs4ZdbWwyL_zY_zWglsVwoedl3eF2w0DhAys2NFXEj4D6Nf5EoEQES3yDeSpDdZHyMQNxJoVRiN1SSqqrjUr1pwbExDjzJ1p19xvcQRJDn2iNdWAlxvRWrQC3D-3JPY4cRKYaip-HQ-HrHRGIC2V_ry36yGS73e0pEoCSKMaebO-6Jyiqmiy1fMzePRwTZzDeyBPBw3eYfMW-6H_KIAA";
    private final String PKEYAUTH_TOKEN_ENDPOINT_NONCE = "ZS6jQlJQLnntAXLP2wMtDl9QvhnNyidmKuaC63rz_pA";
    private final String PKEYAUTH_TOKEN_ENDPOINT_AUTHORITY = "https://login.microsoftonline.com/f645ad92-e38d-4d1a-b510-d1b09a74a8ca/oAuth2/v2.0/token";
    private final String PKEYAUTH_TOKEN_ENDPOINT_CHALLENGE_HEADER = "PKeyAuth CertAuthorities=\"OU=82dbaca4-3e81-46ca-9c73-0950c1eaca97,CN=MS-Organization-Access,DC=windows,DC=net\", Version=\"1.0\", Context=\"rQIIAeNiNdQz1bPUYjbSM7BSMUkySEmySDbStUwzMtM1MTE017VISknVNU4zSDU2MzVLs0g1KBLiEjjb_F946uHJbnNXzk0IdlaqfMHIeIGJ8RYTt79jaUmGUUh-dmreKmYVAwgw1gWRECIZxoKBTcwqaWYmpokplka6qcYWKbomKYaJukmmhga6KYZJBpaJ5iaJFsmJp5jVS4tTi_SKUhNTFPILUvMygVRaWk5mXmp8YnJyanGxQkFRflpmTuoEFsYLLCyvWMQ4GAVYJTgVGDQEDRi9ODhYBBgkGBQYFrEC3X_ObG2x7Ul_31bX6eesLnYxTGCTmcDGMYGNbQKb8CY2Fg4GAcZTbDy-wY4-eo55KUX5mSkf2Bg72Bl-cDLM4GK8wM14gJfhB9-c81N61q_9_8YDAA2.AQABAAEAAAD--DLA3VO7QrddgJg7WevritOStff29rmP7pxoNaAajotJtvq_lSIqVLdNZgszjRcnTW3gQPrpYxT6hydXcUMEuASqhFvxXPBs0JVggjbyO9s9oDTZQqRsoz9w1cQPpcagNBBUhdyw89DU4XvM4HWIQ_pdn6FcUX0ixrkCyNR6e3U-1TNP3ctH_l5kq--9IWJ8MIs4rrchcCSu_A7b66fOwyVFTJnQhnpTLd9d5crH1lAlYOuATsSKAu7q2oR5UNEMMqJS8gemwdxl2NtyA0yh4cyaxWOeeEdEVoRAehKCe0xFVgsfHoUOdf6WLHwWnWCh1ptV1xdaYVyLFCd1TFbUoFrM_wNWzfFo9vOT1Vy_JSSkkLmn3tZEYw4tCPKnG04b-qJPciDSVg2m9DIfZCYreQxgPCDAwjFhnIOmPz_-TP4dpS2bP-HVev0C1-l3t00EyNQDs4ZdbWwyL_zY_zWglsVwoedl3eF2w0DhAys2NFXEj4D6Nf5EoEQES3yDeSpDdZHyMQNxJoVRiN1SSqqrjUr1pwbExDjzJ1p19xvcQRJDn2iNdWAlxvRWrQC3D-3JPY4cRKYaip-HQ-HrHRGIC2V_ry36yGS73e0pEoCSKMaebO-6Jyiqmiy1fMzePRwTZzDeyBPBw3eYfMW-6H_KIAA\", nonce=\"ZS6jQlJQLnntAXLP2wMtDl9QvhnNyidmKuaC63rz_pA\"";

    private final String PKEYAUTH_AUTH_ENDPOINT_WITH_TENANT_ID_CONTEXT = "rQQIARAArVFNaxNRANzXfJjG1sYepAhKDi2K8jbv7fcGxKa0tR-mBRul7aW8fftesnGzm-wmTVM8-4GXInjQozdzKkVQetKDlxwkF0F6EbwpCEUQehHcWvoPnMPMXIZhmHQCi6poXotJIsqPKxayLYNK0OSSBhUF69CwbAZljpisqRo3GApG05kH8x97f27-ntxNfX578at80AXLtZC0mpV8Lkf9mlgLeVN0bOY1nWZHpK4TOTEktbrLRNenxM3h9nxjdaUxVdXbE9JUBeN1h4deo13uLAZrE_L0OwD6ABwC8GzgsmO7xJqshf9UEX2v5tDAD_2oIyrrD4CDgZHlQtQuHZMfONvsZayIMc4TilVV4TrkioqhotrRHs4wNKmFTaqqRJc41JGhYUOWoGHoElQIJdFogqCGmcU1HSHG1G5sHJ1Ahsd8QvTUnWIvNuwHZeI526Tp-F64H8UoNW2uWxBzk0JFYwiaKmeQSoaECKKIyKgXu9IKWSAGjNhZv848JxLOXcdjG4RSFobZeuBzx2VfYmAnDvpx8CN-IQUyibHBrHD1PAILqVQ8A8aErHAUB68S0UXfxkfuXnr9fe7ph-eP9iQg9BI55d7CZqu6Yvqrqt3obLU9ukRRgZP6bEWuamW56i2h1v3ZGX1LuWHm8U4S7CXjKSEDesmh4krhtljw7MB37MMkeHxG2B_8358fDQrd9PWl0ka9NG3OFUvS-qZ7p75oacvFaqu8rbc7q2tbM7c6i37JmbFxob2bBv2z4GAolY5VwtYowO-HhaNzT978-tR98fDn3F81.AQABAAEAAAD--DLA3VO7QrddgJg7WevrWV6Hwgh4oMO4ZUpENLjjVJ1oCEzmYx7ZjGiq9NIUziqpd4H0TyOqo5GEjUFYhFnr4YyDMe1ghOOdp6xR_wMXbWt8gVGN__lUXaqny1b-dDZoqgvzYj-YIVYM9T6d85FlyhB4WW59Fye_M8j7ISKHkDas7PizX6sOXwWEDQ-yZWPwEV-enSvW-PJAmrW2-e0bzyAtA3NwLeIPJgFkpIcxj0ZT2JeVEvxRweb9iz_wcfVZpsk7-l2JJLgzuE38w1eAzoUbI7PlKqAeUH0SHsGz0WkmDHxamc67-N35UfW5eZnS_triZ6FEYf4h6-9aE2rBio8RXQRJxSeJb2VKWRyJkeOxyS7SO9Xpk70wIMoKeWAgoMNIseCg5rET8XptsHmv_DyW6FWl0uAnxKng741BIp4RrgFMJCaZTBv2mZlhU08pp-0vp-qtKs66SDEiu_8wSpDTaZ7Pola1JvXvhKeQUUB3tlJzkvHKhs6uFLgGSofjsvOCFbBIQLHSx1nQyxcWjHV_9zakGaF1TotBSYN4WL3oYJHGURt2dRvk8DODb8_aDkFAz40SgahnVQUggy97tmmk4Wt6csfPsoFAexfV8qRc4YqgeqEVXXFHWbQKH-eqOQRJicx3SPdGUmQjOATFNwlTfVDQvBsiYTaAWPOtaiQnhS5RaYnCtlMKzL_q8D8bNc721FB7kOvn2s3xZ0Kdas8qNw6Ma3NntrEjRudxRIZqYg9VJCL_5F3LEScBqdl7fDss90dfO0c3IaHPuvi3IAA";
    private final String PKEYAUTH_AUTH_ENDPOINT_WITH_TENANT_ID_NONCE = "0CcoPi_2bAjzSxDe9rxzdcXtSxmE_z4jefhssBmW5O8";
    private final String PKEYAUTH_AUTH_ENDPOINT_WITH_TENANT_ID_SUBMIT_URL = "https://login.microsoftonline.com/common/DeviceAuthPKeyAuth?dc=ESTS-PUB-WUS2-AZ1-FD000-TEST1";
    private final String PKEYAUTH_AUTH_ENDPOINT_WITH_TENANT_ID_HOME_TENANT_ID = "f645ad92-e38d-4d1a-b510-d1b09a74a8ca";
    private final String PKEYAUTH_AUTH_ENDPOINT_WITH_TENANT_ID_URL = "urn:http-auth:PKeyAuth?CertAuthorities=OU%3d82dbaca4-3e81-46ca-9c73-0950c1eaca97%2cCN%3dMS-Organization-Access%2cDC%3dwindows%2cDC%3dnet&Version=1.0&Context=rQQIARAArVFNaxNRANzXfJjG1sYepAhKDi2K8jbv7fcGxKa0tR-mBRul7aW8fftesnGzm-wmTVM8-4GXInjQozdzKkVQetKDlxwkF0F6EbwpCEUQehHcWvoPnMPMXIZhmHQCi6poXotJIsqPKxayLYNK0OSSBhUF69CwbAZljpisqRo3GApG05kH8x97f27-ntxNfX578at80AXLtZC0mpV8Lkf9mlgLeVN0bOY1nWZHpK4TOTEktbrLRNenxM3h9nxjdaUxVdXbE9JUBeN1h4deo13uLAZrE_L0OwD6ABwC8GzgsmO7xJqshf9UEX2v5tDAD_2oIyrrD4CDgZHlQtQuHZMfONvsZayIMc4TilVV4TrkioqhotrRHs4wNKmFTaqqRJc41JGhYUOWoGHoElQIJdFogqCGmcU1HSHG1G5sHJ1Ahsd8QvTUnWIvNuwHZeI526Tp-F64H8UoNW2uWxBzk0JFYwiaKmeQSoaECKKIyKgXu9IKWSAGjNhZv848JxLOXcdjG4RSFobZeuBzx2VfYmAnDvpx8CN-IQUyibHBrHD1PAILqVQ8A8aErHAUB68S0UXfxkfuXnr9fe7ph-eP9iQg9BI55d7CZqu6Yvqrqt3obLU9ukRRgZP6bEWuamW56i2h1v3ZGX1LuWHm8U4S7CXjKSEDesmh4krhtljw7MB37MMkeHxG2B_8358fDQrd9PWl0ka9NG3OFUvS-qZ7p75oacvFaqu8rbc7q2tbM7c6i37JmbFxob2bBv2z4GAolY5VwtYowO-HhaNzT978-tR98fDn3F81.AQABAAEAAAD--DLA3VO7QrddgJg7WevrWV6Hwgh4oMO4ZUpENLjjVJ1oCEzmYx7ZjGiq9NIUziqpd4H0TyOqo5GEjUFYhFnr4YyDMe1ghOOdp6xR_wMXbWt8gVGN__lUXaqny1b-dDZoqgvzYj-YIVYM9T6d85FlyhB4WW59Fye_M8j7ISKHkDas7PizX6sOXwWEDQ-yZWPwEV-enSvW-PJAmrW2-e0bzyAtA3NwLeIPJgFkpIcxj0ZT2JeVEvxRweb9iz_wcfVZpsk7-l2JJLgzuE38w1eAzoUbI7PlKqAeUH0SHsGz0WkmDHxamc67-N35UfW5eZnS_triZ6FEYf4h6-9aE2rBio8RXQRJxSeJb2VKWRyJkeOxyS7SO9Xpk70wIMoKeWAgoMNIseCg5rET8XptsHmv_DyW6FWl0uAnxKng741BIp4RrgFMJCaZTBv2mZlhU08pp-0vp-qtKs66SDEiu_8wSpDTaZ7Pola1JvXvhKeQUUB3tlJzkvHKhs6uFLgGSofjsvOCFbBIQLHSx1nQyxcWjHV_9zakGaF1TotBSYN4WL3oYJHGURt2dRvk8DODb8_aDkFAz40SgahnVQUggy97tmmk4Wt6csfPsoFAexfV8qRc4YqgeqEVXXFHWbQKH-eqOQRJicx3SPdGUmQjOATFNwlTfVDQvBsiYTaAWPOtaiQnhS5RaYnCtlMKzL_q8D8bNc721FB7kOvn2s3xZ0Kdas8qNw6Ma3NntrEjRudxRIZqYg9VJCL_5F3LEScBqdl7fDss90dfO0c3IaHPuvi3IAA&nonce=0CcoPi_2bAjzSxDe9rxzdcXtSxmE_z4jefhssBmW5O8&SubmitUrl=https%3a%2f%2flogin.microsoftonline.com%2fcommon%2fDeviceAuthPKeyAuth%3fdc%3dESTS-PUB-WUS2-AZ1-FD000-TEST1&TenantId=f645ad92-e38d-4d1a-b510-d1b09a74a8ca";

    private final String PKEYAUTH_TOKEN_ENDPOINT_WITH_TENANT_ID_CONTEXT = "rQQIARAA42I11DPVs9RiNtIzsFIxSTJISbJINtK1TDMy0zUxMTTXtUhKSdU1TjNINTYzNUuzSDUoEuIS-JwaPEfq6C2vud33LFV5tQVeMDJeYGK8xcTt71hakmEUkp-dmreKWcUAAox1QSSESIaxYGATs0qamYlpYoqlkW6qsUWKrkmKYaJukqmhgW6KYZKBZaK5SaJFcuIpZvXS4tQivaLUxBSF_ILUvEwglZaWk5mXGp-YnJxaXKxQUJSflpmTOoGF8QILyysWMQ5GAVYJTgUGDUEDRi8ODhYBRgkGBYZFrED3T6zcKvzuh5Nj15q5G68fYWSYwCYzgY1jAhvbBDbhTWwsHAwCjKfYeHyDHX30HPNSivIzUz6wMXawM_zgZJjBxXiBm_EAL8MPvtP_P61ZPKvtrQcA0.AQABAAEAAAD--DLA3VO7QrddgJg7WevrUGf8n76QJQ_vdgHTjJbamXSdAitK_CkmHH_QQCSgvvFv6o8YWf1MUZknQi2gJ4rwPu_6Bsg3VB_hthg_ADy3wi2VdOObFlhwcz9vvuOz8WOYnx4dhYqQceou_0JvML8TXh_r4dqq6gR-akiU5vfF4-0Rv5QiT6xWXwT3970rHXzJhmteE1dSYuICcK1ua3ATIVLVsYV5FFfUFqvtR1b-YKf51_u2rf2wrX8yy1n9QD_DjK1yUtS5b0TPzV8rzlsK7GeYJqzUPHo7iGEq9LIGqrZ-LWDFEqcw4W-kSgruyrkbi0szVU8E9E8UNrvIES091qyVHW36UXDwdWJa_yxJYOiIOkkKtYtdowkJkhPf6VK3mgwkD94YKXAv0c2Ee2tFhE8kkJpknWk61G-OQemYLGl-6C1uzwhwyrZlF8PaeJuSHzOK0OgqexvLUmoa60rSFF_jdyMPyan48e_K4xBXPPmfycyEAyoq28_TES_IhnPUUihq35tt2tkd3qFwk0wiltc_iYu8qTbGYbA1Pepn8y4pQ5oCQLqkrRdHlgHcuIcYx4UrIg-3dNgDwf8VJgwPHMBur2Ui0jd8Um1nX-WmZTIbbUhgEsVm8asX9awIgDTvav-bRB9vdBiedt14_dXa5x-Y3gtbk4-23DvaV8zcQ0rGor1uGMT2Ac7Z_2BqWYwgAA";
    private final String PKEYAUTH_TOKEN_ENDPOINT_WITH_TENANT_ID_NONCE = "F8ZXlph4oUK7YeIlWvLoQtKTlkQ2u9t9eovEmeMXUIM";
    private final String PKEYAUTH_TOKEN_ENDPOINT_WITH_TENANT_ID_AUTHORITY = "https://login.microsoftonline.com/f645ad92-e38d-4d1a-b510-d1b09a74a8ca/oAuth2/v2.0/token?dc=ESTS-PUB-WUS2-AZ1-FD000-TEST1";
    private final String PKEYAUTH_TOKEN_ENDPOINT_WITH_TENANT_ID_HOME_TENANT_ID = "f645ad92-e38d-4d1a-b510-d1b09a74a8ca";
    private final String PKEYAUTH_TOKEN_ENDPOINT_WITH_TENANT_ID_CHALLENGE_HEADER="PKeyAuth CertAuthorities=\"OU=82dbaca4-3e81-46ca-9c73-0950c1eaca97,CN=MS-Organization-Access,DC=windows,DC=net\", Version=\"1.0\", Context=\"rQQIARAA42I11DPVs9RiNtIzsFIxSTJISbJINtK1TDMy0zUxMTTXtUhKSdU1TjNINTYzNUuzSDUoEuIS-JwaPEfq6C2vud33LFV5tQVeMDJeYGK8xcTt71hakmEUkp-dmreKWcUAAox1QSSESIaxYGATs0qamYlpYoqlkW6qsUWKrkmKYaJukqmhgW6KYZKBZaK5SaJFcuIpZvXS4tQivaLUxBSF_ILUvEwglZaWk5mXGp-YnJxaXKxQUJSflpmTOoGF8QILyysWMQ5GAVYJTgUGDUEDRi8ODhYBRgkGBYZFrED3T6zcKvzuh5Nj15q5G68fYWSYwCYzgY1jAhvbBDbhTWwsHAwCjKfYeHyDHX30HPNSivIzUz6wMXawM_zgZJjBxXiBm_EAL8MPvtP_P61ZPKvtrQcA0.AQABAAEAAAD--DLA3VO7QrddgJg7WevrUGf8n76QJQ_vdgHTjJbamXSdAitK_CkmHH_QQCSgvvFv6o8YWf1MUZknQi2gJ4rwPu_6Bsg3VB_hthg_ADy3wi2VdOObFlhwcz9vvuOz8WOYnx4dhYqQceou_0JvML8TXh_r4dqq6gR-akiU5vfF4-0Rv5QiT6xWXwT3970rHXzJhmteE1dSYuICcK1ua3ATIVLVsYV5FFfUFqvtR1b-YKf51_u2rf2wrX8yy1n9QD_DjK1yUtS5b0TPzV8rzlsK7GeYJqzUPHo7iGEq9LIGqrZ-LWDFEqcw4W-kSgruyrkbi0szVU8E9E8UNrvIES091qyVHW36UXDwdWJa_yxJYOiIOkkKtYtdowkJkhPf6VK3mgwkD94YKXAv0c2Ee2tFhE8kkJpknWk61G-OQemYLGl-6C1uzwhwyrZlF8PaeJuSHzOK0OgqexvLUmoa60rSFF_jdyMPyan48e_K4xBXPPmfycyEAyoq28_TES_IhnPUUihq35tt2tkd3qFwk0wiltc_iYu8qTbGYbA1Pepn8y4pQ5oCQLqkrRdHlgHcuIcYx4UrIg-3dNgDwf8VJgwPHMBur2Ui0jd8Um1nX-WmZTIbbUhgEsVm8asX9awIgDTvav-bRB9vdBiedt14_dXa5x-Y3gtbk4-23DvaV8zcQ0rGor1uGMT2Ac7Z_2BqWYwgAA\", nonce=\"F8ZXlph4oUK7YeIlWvLoQtKTlkQ2u9t9eovEmeMXUIM\", TenantId=\"f645ad92-e38d-4d1a-b510-d1b09a74a8ca\"";

    @Test
    public void testParsingChallengeUrl() throws ClientException {
        final PKeyAuthChallenge challenge = new PKeyAuthChallengeFactory().getPKeyAuthChallengeFromWebViewRedirect(PKEYAUTH_AUTH_ENDPOINT_URL);
        Assert.assertArrayEquals(PKEYAUTH_CERT_AUTHORITIES, challenge.getCertAuthorities().toArray());
        Assert.assertEquals(PKEYAUTH_MOCK_VERSION, challenge.getVersion());
        Assert.assertEquals(PKEYAUTH_AUTH_ENDPOINT_CONTEXT, challenge.getContext());
        Assert.assertEquals(PKEYAUTH_AUTH_ENDPOINT_NONCE, challenge.getNonce());
        Assert.assertEquals(PKEYAUTH_AUTH_ENDPOINT_SUBMIT_URL, challenge.getSubmitUrl());
        Assert.assertNull(challenge.getTenantId());
        Assert.assertNull(challenge.getThumbprint());
    }

    // eSTS will only return TenantID in PKeyAuth challenge when x-ms-brkr >= 4.1.0
    // (or, if the earlier is not provided, x-client-Ver >= 3.1.0)
    @Test
    public void testParsingChallengeUrl_WithTenantId() throws ClientException {
        final PKeyAuthChallenge challenge = new PKeyAuthChallengeFactory().getPKeyAuthChallengeFromWebViewRedirect(PKEYAUTH_AUTH_ENDPOINT_WITH_TENANT_ID_URL);
        Assert.assertArrayEquals(PKEYAUTH_CERT_AUTHORITIES, challenge.getCertAuthorities().toArray());
        Assert.assertEquals(PKEYAUTH_MOCK_VERSION, challenge.getVersion());
        Assert.assertEquals(PKEYAUTH_AUTH_ENDPOINT_WITH_TENANT_ID_CONTEXT, challenge.getContext());
        Assert.assertEquals(PKEYAUTH_AUTH_ENDPOINT_WITH_TENANT_ID_NONCE, challenge.getNonce());
        Assert.assertEquals(PKEYAUTH_AUTH_ENDPOINT_WITH_TENANT_ID_SUBMIT_URL, challenge.getSubmitUrl());
        Assert.assertEquals(PKEYAUTH_AUTH_ENDPOINT_WITH_TENANT_ID_HOME_TENANT_ID, challenge.getTenantId());
        Assert.assertNull(challenge.getThumbprint());
    }

    @Test
    public void testParsingChallengeUrl_Malformed() {
        try{
            new PKeyAuthChallengeFactory().getPKeyAuthChallengeFromWebViewRedirect(
                    "urn:http-auth:PKeyAuth?CertAuthorities=OU%3d82dbaca4"
            );
            Assert.fail("Exception is expected");
        } catch (final ClientException e) {
            Assert.assertEquals(DEVICE_CERTIFICATE_REQUEST_INVALID, e.getErrorCode());
        }
    }

    @Test
    public void testParsingChallengeHeader() throws UnsupportedEncodingException, ClientException {
        final PKeyAuthChallenge challenge = new PKeyAuthChallengeFactory().getPKeyAuthChallengeFromTokenEndpointResponse(
                PKEYAUTH_TOKEN_ENDPOINT_CHALLENGE_HEADER,
                PKEYAUTH_TOKEN_ENDPOINT_AUTHORITY);
        Assert.assertArrayEquals(PKEYAUTH_CERT_AUTHORITIES, challenge.getCertAuthorities().toArray());
        Assert.assertEquals(PKEYAUTH_MOCK_VERSION, challenge.getVersion());
        Assert.assertEquals(PKEYAUTH_TOKEN_ENDPOINT_CONTEXT, challenge.getContext());
        Assert.assertEquals(PKEYAUTH_TOKEN_ENDPOINT_NONCE, challenge.getNonce());
        Assert.assertEquals(PKEYAUTH_TOKEN_ENDPOINT_AUTHORITY, challenge.getSubmitUrl());
        Assert.assertNull(challenge.getTenantId());
        Assert.assertNull(challenge.getThumbprint());
    }

    // eSTS will only return TenantID in PKeyAuth challenge when x-ms-brkr >= 4.1.0
    // (or, if the earlier is not provided, x-client-Ver >= 3.1.0)
    @Test
    public void testParsingChallengeHeader_WithTenantID() throws UnsupportedEncodingException, ClientException {
        final PKeyAuthChallenge challenge = new PKeyAuthChallengeFactory().getPKeyAuthChallengeFromTokenEndpointResponse(
                PKEYAUTH_TOKEN_ENDPOINT_WITH_TENANT_ID_CHALLENGE_HEADER,
                PKEYAUTH_TOKEN_ENDPOINT_WITH_TENANT_ID_AUTHORITY);
        Assert.assertArrayEquals(PKEYAUTH_CERT_AUTHORITIES, challenge.getCertAuthorities().toArray());
        Assert.assertEquals(PKEYAUTH_MOCK_VERSION, challenge.getVersion());
        Assert.assertEquals(PKEYAUTH_TOKEN_ENDPOINT_WITH_TENANT_ID_CONTEXT, challenge.getContext());
        Assert.assertEquals(PKEYAUTH_TOKEN_ENDPOINT_WITH_TENANT_ID_NONCE, challenge.getNonce());
        Assert.assertEquals(PKEYAUTH_TOKEN_ENDPOINT_WITH_TENANT_ID_AUTHORITY, challenge.getSubmitUrl());
        Assert.assertEquals(PKEYAUTH_TOKEN_ENDPOINT_WITH_TENANT_ID_HOME_TENANT_ID, challenge.getTenantId());
        Assert.assertNull(challenge.getThumbprint());
    }

    @Test
    public void testParsingChallengeHeader_Malformed() throws UnsupportedEncodingException {
        try{
            new PKeyAuthChallengeFactory().getPKeyAuthChallengeFromTokenEndpointResponse(
                    "urn:http-auth:PKeyAuth?CertAuthorities=OU%3d82dbaca4",
                    PKEYAUTH_TOKEN_ENDPOINT_AUTHORITY
            );
            Assert.fail("Exception is expected");
        } catch (final ClientException e) {
            Assert.assertEquals(DEVICE_CERTIFICATE_REQUEST_INVALID, e.getErrorCode());
        }
    }
}
