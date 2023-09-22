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
package com.microsoft.identity.common.java.opentelemetry;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;

/**
 * Main implementation of IFidoTelemetryHelper.
 */
public class FidoTelemetryHelper implements IFidoTelemetryHelper {

    private final Span mSpan;

    public FidoTelemetryHelper() {
        mSpan = OTelUtility.createSpan(SpanName.CertBasedAuth.name());
    }
    @Override
    public void setFidoChallenge(String challengeName) {
        mSpan.setAttribute(
          AttributeName.fido_challenge.name(),
          challengeName
        );
    }

    @Override
    public void setFidoChallengeHandler(String challengeHandlerName) {
        mSpan.setAttribute(
                AttributeName.fido_challenge_handler.name(),
                challengeHandlerName
        );
    }
    //Suppressing warnings for RETURN_VALUE_IGNORED_NO_SIDE_EFFECT
    @SuppressFBWarnings
    @Override
    public void setResultSuccess() {
        mSpan.setStatus(StatusCode.OK);
        mSpan.end();
    }

    //Suppressing warnings for RETURN_VALUE_IGNORED_NO_SIDE_EFFECT
    @SuppressFBWarnings
    @Override
    public void setResultFailure(String message) {
        mSpan.setStatus(StatusCode.ERROR, message);
        mSpan.end();
    }

    //Suppressing warnings for RETURN_VALUE_IGNORED_NO_SIDE_EFFECT
    @SuppressFBWarnings
    @Override
    public void setResultFailure(Exception exception) {
        mSpan.recordException(exception);
        mSpan.setStatus(StatusCode.ERROR);
        mSpan.end();
    }
}
