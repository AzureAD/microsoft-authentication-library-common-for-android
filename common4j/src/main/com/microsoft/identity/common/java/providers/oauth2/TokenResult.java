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

import com.microsoft.identity.common.java.telemetry.CliTelemInfo;

/**
 * Holds the request of a token request.  The request will either contain the success result or the error result.
 */
public class TokenResult implements IResult {

    private TokenResponse mTokenResponse;
    private TokenErrorResponse mTokenErrorResponse;
    private CliTelemInfo mCliTelemInfo;

    private boolean mIsNaaRequest = false;
    private boolean mSuccess = false;

    public TokenResult() {
        // Intentionally blank
    }

    /**
     * Constructor of TokenResult.
     *
     * @param response TokenResponse
     */
    public TokenResult(final TokenResponse response) {
        this(response, null);
    }


    /**
     * Constructor of TokenResult.
     *
     * @param errorResponse TokenErrorResponse
     */
    public TokenResult(final TokenErrorResponse errorResponse) {
        this(null, errorResponse);
    }

    /**
     * Constructor of TokenResult.
     *
     * @param response      TokenResponse
     * @param errorResponse TokenErrorResponse
     */
    public TokenResult(final TokenResponse response, final TokenErrorResponse errorResponse) {
        this.mTokenResponse = response;
        this.mTokenErrorResponse = errorResponse;

        if (response != null) {
            mSuccess = true;
        }
    }

    /**
     * Returns the TokenResponse (success) associated with the request.
     *
     * @return TokenResponse
     */
    public TokenResponse getTokenResponse() {
        return mTokenResponse;
    }

    public TokenResponse getSuccessResponse() { return mTokenResponse; }

    /**
     * Returns the TokenErrorResponse associated with the request.
     *
     * @return TokenErrorResponse
     */
    public TokenErrorResponse getErrorResponse() {
        return mTokenErrorResponse;
    }

    /**
     * Gets the CliTelemInfo associated with this TokenResult.
     *
     * @return The CliTelemInfo to get.
     */
    public CliTelemInfo getCliTelemInfo() {
        return mCliTelemInfo;
    }

    /**
     * Sets the CliTelemInfo associated with this TokenResult.
     *
     * @param cliTelemInfo The CliTelemInfo to set.
     */
    public void setCliTelemInfo(final CliTelemInfo cliTelemInfo) {
        mCliTelemInfo = cliTelemInfo;
    }

    public void setIsNaaRequest(final boolean isNaaRequest) {
        mIsNaaRequest = isNaaRequest;
    }

    /**
     * Returns whether the token request was successful or not.
     *
     * @return boolean
     */
    public boolean getSuccess() {
        return mSuccess;
    }

    /**
     * Set if the TokenResult is success or not
     *
     * @param success true if successful
     */
    public void setSuccess(boolean success) {
        mSuccess = success;
    }


    //CHECKSTYLE:OFF
    @Override
    public String toString() {
        return "TokenResult{" +
                "mTokenResponse=" + mTokenResponse +
                ", mTokenErrorResponse=" + mTokenErrorResponse +
                ", mSuccess=" + mSuccess +
                '}';
    }
    //CHECKSTYLE:ON

}
