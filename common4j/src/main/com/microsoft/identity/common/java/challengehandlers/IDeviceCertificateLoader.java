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

import edu.umd.cs.findbugs.annotations.Nullable;

/**
 * An interface for loading an {@link IDeviceCertificate} object (WorkplaceJoin cert)
 * to perform cert-based device authentication in PKeyAuth flow.
 */
public interface IDeviceCertificateLoader {

    /**
     * Loads an {@link IDeviceCertificate} object matching the provided tenant Id.
     * If tenant ID is not provided, this will load the default certificate.
     * (For MultipleWorkplaceJoinDataStore, this means the entry in the legacy space.
     *  For (Legacy) WorkplaceJoinDataStore, this is the only entry it has.)
     *
     * @param tenantId an optional tenantID to perform look up.
     * @return an {@link IDeviceCertificate} object.
     * */
    @Nullable
    IDeviceCertificate loadCertificate (@Nullable final String tenantId);
}
