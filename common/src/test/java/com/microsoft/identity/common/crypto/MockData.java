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
package com.microsoft.identity.common.crypto;

import static com.microsoft.identity.common.java.AuthenticationConstants.ENCODING_UTF8;

public class MockData {
    private MockData(){}

    // Value extracted from the legacy StorageHelper.
    // Data Set 1 - Predefined key.
    static final byte[] PREDEFINED_KEY = new byte[]{22, 78, -69, -66, 84, -65, 119, -9, -34, -80, 60, 67, -12, -117, 86, -47, -84, -24, -18, 121, 70, 32, -110, 51, -93, -10, -93, -110, 124, -68, -42, -119};
    static final byte[] TEXT_ENCRYPTED_BY_PREDEFINED_KEY = "cE1VTAwMeHz7BCCH/27kWvMYYMsGamVenQk6w+YJ14JnFBi6fJ1D8FrdLe8ZSX/FeU1apYKsj9d1fNoMD4kR62XfPMytA3P2XpXEQtkblP6F6A5R74F".getBytes(ENCODING_UTF8);

    // Data Set 2 - Another predefined key.
    static final byte[] ANOTHER_PREDEFINED_KEY = new byte[]{122, 75, 49, 112, 36, 126, 5, 35, 46, 45, -61, -61, 55, 105, 9, -123, 115, 27, 35, -54, -49, 14, -16, 49, -74, -88, -29, -15, -33, -13, 100, 118};

    // Data Set 3 - Android KeyStore-wrapped key.
    static final byte[] ANDROID_WRAPPED_KEY = new byte[]{122, 75, 49, 112, 36, 126, 5, 35, 46, 45, -61, -61, 55, 105, 9, -123, 115, 27, 35, -54, -49, 14, -16, 49, -74, -88, -29, -15, -33, -13, 100, 118};
    static final byte[] TEXT_ENCRYPTED_BY_ANDROID_WRAPPED_KEY = "cE1QTAwMTDvTopC+ds4Wgm7IbhnZl1pEVWU+vt7dp0h098822NjPzaNb9JC2PfLyPi/cJuM/wKGYN9YpvRP+BA+i0DlGdCb7nOQ/fmYfjpNq9aj26Kh".getBytes(ENCODING_UTF8);
}
