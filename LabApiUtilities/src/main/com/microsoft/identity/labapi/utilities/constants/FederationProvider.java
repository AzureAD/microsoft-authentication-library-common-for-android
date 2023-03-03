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

public enum FederationProvider {
    NONE(LabConstants.FederationProvider.NONE),
    ADFS_V2(LabConstants.FederationProvider.ADFS_V2),
    ADFS_V3(LabConstants.FederationProvider.ADFS_V3),
    ADFS_V4(LabConstants.FederationProvider.ADFS_V4),
    ADFS_V2019(LabConstants.FederationProvider.ADFS_V2019),
    B2C(LabConstants.FederationProvider.B2C),
    PING(LabConstants.FederationProvider.PING),
    SHIBBOLETH(LabConstants.FederationProvider.SHIBBOLETH),
    CIAM(LabConstants.FederationProvider.CIAM);

    final String value;

    FederationProvider(final String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}
