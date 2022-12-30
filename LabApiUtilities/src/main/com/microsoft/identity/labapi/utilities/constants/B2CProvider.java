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

public enum B2CProvider {
    NONE(LabConstants.B2CProvider.NONE),
    AMAZON(LabConstants.B2CProvider.AMAZON),
    FACEBOOK(LabConstants.B2CProvider.FACEBOOK),
    GOOGLE(LabConstants.B2CProvider.GOOGLE),
    LOCAL(LabConstants.B2CProvider.LOCAL),
    MICROSOFT(LabConstants.B2CProvider.MICROSOFT),
    TWITTER(LabConstants.B2CProvider.TWITTER);

    final String value;

    B2CProvider(final String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }

    public static B2CProvider fromName(@NonNull final String name) {
        return valueOf(B2CProvider.class, name.toUpperCase());
    }
}
