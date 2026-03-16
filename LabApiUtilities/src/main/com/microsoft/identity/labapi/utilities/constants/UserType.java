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

import lombok.NonNull;

public enum UserType {
    BASIC(LabConstants.UserType.BASIC),
    MSA(LabConstants.UserType.MSA),
    MDM_CA(LabConstants.UserType.MDM_CA),
    MAM_CA(LabConstants.UserType.MAM_CA),
    MAM_ON_SPO(LabConstants.UserType.MAM_ON_SPO),
    TRUE_MAM_CA(LabConstants.UserType.TRUE_MAM_CA),
    WP(LabConstants.UserType.WP),
    FEDERATED(LabConstants.UserType.FEDERATED),
    DEVICE_ADMIN(LabConstants.UserType.DEVICE_ADMIN),
    USGOV(LabConstants.UserType.USGOV),
    USGOV_GUEST(LabConstants.UserType.USGOV_GUEST),
    CHINA(LabConstants.UserType.CHINA),
    CHINA_GUEST(LabConstants.UserType.CHINA_GUEST),
    QR_PIN(LabConstants.UserType.QR_PIN),
    TOKEN_BINDING(LabConstants.UserType.TOKEN_BINDING),
    CBA(LabConstants.UserType.CBA),
    RESOURCE_ACCOUNT_1(LabConstants.UserType.RESOURCE_ACCOUNT_1),
    RESOURCE_ACCOUNT_2(LabConstants.UserType.RESOURCE_ACCOUNT_2),
    DUNA_BASIC_1(LabConstants.UserType.DUNA_BASIC_1),
    DUNA_BASIC_2(LabConstants.UserType.DUNA_BASIC_2),
    DUNA_MAM_CA_1(LabConstants.UserType.DUNA_MAM_CA_1),
    DUNA_MAM_CA_2(LabConstants.UserType.DUNA_MAM_CA_2),
    DUNA_MDM_CA_1(LabConstants.UserType.DUNA_MDM_CA_1),
    DUNA_MDM_CA_2(LabConstants.UserType.DUNA_MDM_CA_2),
    DUNA_MFA_1(LabConstants.UserType.DUNA_MFA_1),
    DUNA_MFA_2(LabConstants.UserType.DUNA_MFA_2),
    TP_CA(LabConstants.UserType.TP_CA),
    CLOUD(LabConstants.UserType.CLOUD),
    B2C(LabConstants.UserType.B2C),
    CIAM(LabConstants.UserType.CIAM),
    GUEST(LabConstants.UserType.GUEST),
    ONPREM(LabConstants.UserType.ONPREM);

    final String value;

    UserType(final String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }

    public static UserType fromName(@NonNull final String name) {
        return valueOf(UserType.class, name.toUpperCase());
    }

}
