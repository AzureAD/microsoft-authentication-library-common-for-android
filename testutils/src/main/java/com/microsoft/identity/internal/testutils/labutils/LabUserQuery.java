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
package com.microsoft.identity.internal.testutils.labutils;

public class LabUserQuery {
    public String userType;
    public String mfa;
    public String protectionPolicy;
    public String homeDomain;
    public String homeUpn;
    public String b2cProvider;
    public String federationProvider;
    public String azureEnvironment;
    public String signInAudience;
    public String guestHomedIn;

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;

        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }

        LabUserQuery query = (LabUserQuery) obj;

        return computeEquals(query.userType, this.userType) &&
                computeEquals(query.mfa, this.mfa) &&
                computeEquals(query.protectionPolicy, this.protectionPolicy) &&
                computeEquals(query.homeDomain, this.homeDomain) &&
                computeEquals(query.homeUpn, this.homeUpn) &&
                computeEquals(query.b2cProvider, this.b2cProvider) &&
                computeEquals(query.federationProvider, this.federationProvider) &&
                computeEquals(query.azureEnvironment, this.azureEnvironment) &&
                computeEquals(query.signInAudience, this.signInAudience) &&
                computeEquals(query.guestHomedIn, this.guestHomedIn);
    }

    @Override
    public int hashCode() {
        return computeHashCode(userType, 2) +
                computeHashCode(mfa, 3) +
                computeHashCode(protectionPolicy, 5) +
                computeHashCode(homeDomain, 7) +
                computeHashCode(homeUpn, 11) +
                computeHashCode(b2cProvider, 13) +
                computeHashCode(federationProvider, 17) +
                computeHashCode(azureEnvironment, 19) +
                computeHashCode(signInAudience, 23) +
                computeHashCode(guestHomedIn, 29);

    }

    private boolean computeEquals(String a, String b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.equals(b);
    }

    private int computeHashCode(String s, int n) {
        if (s == null) {
            return 0;
        }

        return n * s.hashCode();
    }
}
