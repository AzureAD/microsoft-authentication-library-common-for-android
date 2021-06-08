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
package com.microsoft.identity.common.java.providers.oauth2;

import org.junit.Assert;
import org.junit.Test;

public class PkceChallengeTest {

    @Test
    public void testGenerateCodeVerifier() {
        final byte[] verifierBytes = new byte[]{-121, -61, 79, 79, 38, 98, 25, 43, 105, -86,
                -122, -94, 40, 72, 57, 76, -68, -71, 28, 98, 47, -17, -101, -55, 63, 93, 53,
                -113, 78, 124, -52, -38};
        Assert.assertEquals("h8NPTyZiGStpqoaiKEg5TLy5HGIv75vJP101j058zNo",
                PkceChallenge.generateCodeVerifier(verifierBytes));
    }

    @Test
    public void testGenerateCodeVerifierChallenge(){
        final String verifier = "z86XHrKFENPT1U8dZt_Aa6UIybxaTKrqJkdTwsGfAv4";

        Assert.assertEquals("9zH10spxQ4ivCvet1EQdRQI82xZ7I8DKU2NFvoSg5mY",
                PkceChallenge.generateCodeVerifierChallenge(verifier));
    }
}
