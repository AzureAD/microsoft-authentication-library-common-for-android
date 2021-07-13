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
package com.microsoft.identity.common.java.crypto;

import static com.microsoft.identity.common.java.AuthenticationConstants.ENCODING_UTF8;

public class MockData {
    public MockData(){}

    // Value extracted from the legacy StorageHelper.
    // Data Set 1 - Predefined key.
    static final byte[] TEXT_TO_BE_ENCRYPTED_WITH_PREDEFINED_KEY = "TEST_TEXT_TO_ENCRYPT".getBytes(ENCODING_UTF8);
    static final byte[] PREDEFINED_KEY = new byte[]{22, 78, -69, -66, 84, -65, 119, -9, -34, -80, 60, 67, -12, -117, 86, -47, -84, -24, -18, 121, 70, 32, -110, 51, -93, -10, -93, -110, 124, -68, -42, -119};final byte[] encryptionKey_slightlyModified = new byte[]{23, 78, -69, -66, 84, -65, 119, -9, -34, -80, 60, 67, -12, -117, 86, -47, -84, -24, -18, 121, 70, 32, -110, 51, -93, -10, -93, -110, 124, -68, -42, -119};
    static final byte[] PREDEFINED_KEY_SLIGHTLY_MODIFIED = new byte[]{23, 78, -69, -66, 84, -65, 119, -9, -34, -80, 60, 67, -12, -117, 86, -47, -84, -24, -18, 121, 70, 32, -110, 51, -93, -10, -93, -110, 124, -68, -42, -119};
    static final byte[] PREDEFINED_KEY_MALFORMED = new byte[]{22, 78, -75, -66, 84, -65, 119, -9, -34, -80, 60, 67, -12, -117, 86, -47, -84, -24, -18, 121, 70, 32, -110, 51, -93, -10, -93, -110, 124, -68, -42, -119};
    static final byte[] PREDEFINED_KEY_IV = new byte[]{15, -63, 107, 116, -73, -68, 101, 37, -1, 21, -27, 53, 106, -106, 10, -78};
    static final byte[] TEXT_ENCRYPTED_BY_PREDEFINED_KEY = "cE1VTAwMeHz7BCCH/27kWvMYYMsGamVenQk6w+YJ14JnFBi6fJ1D8FrdLe8ZSX/FeU1apYKsj9d1fNoMD4kR62XfPMytA3P2XpXEQtkblP6F6A5R74F".getBytes(ENCODING_UTF8);
    static final String PREDEFINED_KEY_IDENTIFIER = "U001";
    static final byte[] EXPECTED_ENCRYPTED_TEXT_1_WITH_MALFORMED_ENCODE_VERSION = "cD1VTAwMeHz7BCCH/27kWvMYYMsGamVenQk6w+YJ14JnFBi6fJ1D8FrdLe8ZSX/FeU1apYKsj9d1fNoMD4kR62XfPMytA3P2XpXEQtkblP6F6A5R74F".getBytes(ENCODING_UTF8);

    // Data Set 2 - Android KeyStore-wrapped key.
    static final byte[] TEXT_TO_BE_ENCRYPTED_WITH_ANDROID_WRAPPED_KEY = "SECOND_TEXT_TO_ENCRYPT".getBytes(ENCODING_UTF8);
    static final byte[] ANDROID_WRAPPED_KEY = new byte[]{122, 75, 49, 112, 36, 126, 5, 35, 46, 45, -61, -61, 55, 105, 9, -123, 115, 27, 35, -54, -49, 14, -16, 49, -74, -88, -29, -15, -33, -13, 100, 118};
    static final byte[] ANDROID_WRAPPED_KEY_IV = new byte[]{63, 54, -115, 111, -46, 66, -40, -9, -53, -56, -8, -65, 112, -101, -116, -1};
    static final byte[] TEXT_ENCRYPTED_BY_ANDROID_WRAPPED_KEY = "cE1QTAwMTDvTopC+ds4Wgm7IbhnZl1pEVWU+vt7dp0h098822NjPzaNb9JC2PfLyPi/cJuM/wKGYN9YpvRP+BA+i0DlGdCb7nOQ/fmYfjpNq9aj26Kh".getBytes(ENCODING_UTF8);
    static final String ANDROID_WRAPPED_KEY_IDENTIFIER = "A001";
}
