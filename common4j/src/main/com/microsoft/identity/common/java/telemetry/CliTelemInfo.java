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
package com.microsoft.identity.common.java.telemetry;

import com.microsoft.identity.common.java.logging.Logger;
import com.microsoft.identity.common.java.util.StringUtil;

import java.io.Serializable;
import java.util.regex.Pattern;

import edu.umd.cs.findbugs.annotations.Nullable;

@Deprecated
public class CliTelemInfo implements Serializable {

    private static final String TAG = CliTelemInfo.class.getSimpleName();
    private static final long serialVersionUID = -7200606162774338466L;
    private static final Pattern HEADER_FORMAT_REGULAR_EXPRESSION = Pattern.compile("^[1-9]+\\.?[0-9|\\.]*,[0-9|\\.]*,[0-9|\\.]*,[^,]*[0-9\\.]*,[^,]*$");

    private String mVersion;
    private String mServerErrorCode;
    private String mServerSubErrorCode;
    private String mRefreshTokenAge;
    private String mSpeRing;

    public CliTelemInfo() {
        // Default constructor for legacy support.
    }

    public CliTelemInfo(final CliTelemInfo copy) {
        if (null != copy) {
            mVersion = copy.mVersion;
            mServerErrorCode = copy.mServerErrorCode;
            mServerSubErrorCode = copy.mServerSubErrorCode;
            mRefreshTokenAge = copy.mRefreshTokenAge;
            mSpeRing = copy.mSpeRing;
        }
    }

    public String getVersion() {
        return mVersion;
    }

    protected void setVersion(String version) {
        this.mVersion = version;
    }

    public String getServerErrorCode() {
        return mServerErrorCode;
    }

    protected void setServerErrorCode(String serverErrorCode) {
        this.mServerErrorCode = serverErrorCode;
    }

    public String getServerSubErrorCode() {
        return mServerSubErrorCode;
    }

    protected void setServerSubErrorCode(String serverSubErrorCode) {
        this.mServerSubErrorCode = serverSubErrorCode;
    }

    public String getRefreshTokenAge() {
        return mRefreshTokenAge;
    }

    public void setRefreshTokenAge(String refreshTokenAge) {
        this.mRefreshTokenAge = refreshTokenAge;
    }

    public String getSpeRing() {
        return mSpeRing;
    }

    public void setSpeRing(String speRing) {
        this.mSpeRing = speRing;
    }

    @Nullable
    public static CliTelemInfo fromXMsCliTelemHeader(final String headerValue) {
        // if the header isn't present, do nothing
        if (StringUtil.isNullOrEmpty(headerValue)) {
            return null;
        }

        // split the header based on the delimiter
        String[] headerSegments = headerValue.split(",");

        // make sure the header isn't empty
        if (0 == headerSegments.length) {
            Logger.warn(
                    TAG,
                    "SPE Ring header missing version field."
            );

            return null;
        }

        // get the version of this header
        final String headerVersion = headerSegments[0];

        // The eventual result
        final CliTelemInfo cliTelemInfo = new CliTelemInfo();
        cliTelemInfo.setVersion(headerVersion);

        if (headerVersion.equals("1")) {
            // The expected delimiter count of the v1 header
            final int delimCount = 4;

            // Verify the expected format "<version>, <error_code>, <sub_error_code>, <token_age>, <ring>"
            if (!HEADER_FORMAT_REGULAR_EXPRESSION.matcher(headerValue).matches()) {
                Logger.warn(
                        TAG,
                        "Malformed x-ms-clitelem header"
                );

                return null;
            }

            headerSegments = headerValue.split(",", delimCount + 1);

            // Constants used to identify value position
            final int indexErrorCode = 1;
            final int indexSubErrorCode = 2;
            final int indexTokenAge = 3;
            final int indexSpeInfo = 4;

            cliTelemInfo.setServerErrorCode(headerSegments[indexErrorCode]);
            cliTelemInfo.setServerSubErrorCode(headerSegments[indexSubErrorCode]);
            cliTelemInfo.setRefreshTokenAge(headerSegments[indexTokenAge]);
            cliTelemInfo.setSpeRing(headerSegments[indexSpeInfo]);
        } else { // unrecognized version
            Logger.warn(
                    TAG,
                    "Unrecognized x-ms-clitelem header version"
            );

            return null;
        }

        return cliTelemInfo;
    }
}
