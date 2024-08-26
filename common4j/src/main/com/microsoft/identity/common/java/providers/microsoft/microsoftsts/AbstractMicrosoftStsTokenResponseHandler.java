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
package com.microsoft.identity.common.java.providers.microsoft.microsoftsts;

import static com.microsoft.identity.common.java.net.HttpConstants.HeaderField.XMS_CCS_REQUEST_ID;
import static com.microsoft.identity.common.java.net.HttpConstants.HeaderField.XMS_CCS_REQUEST_SEQUENCE;
import static com.microsoft.identity.common.java.net.HttpConstants.HeaderField.X_MS_CLITELEM;

import com.google.gson.JsonParseException;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.flighting.CommonFlight;
import com.microsoft.identity.common.java.flighting.CommonFlightsManager;
import com.microsoft.identity.common.java.net.HttpResponse;
import com.microsoft.identity.common.java.opentelemetry.AttributeName;
import com.microsoft.identity.common.java.opentelemetry.SpanExtension;
import com.microsoft.identity.common.java.providers.microsoft.MicrosoftTokenErrorResponse;
import com.microsoft.identity.common.java.providers.oauth2.TokenErrorResponse;
import com.microsoft.identity.common.java.providers.oauth2.ITokenResponseHandler;
import com.microsoft.identity.common.java.providers.oauth2.TokenResult;
import com.microsoft.identity.common.java.telemetry.CliTelemInfo;
import com.microsoft.identity.common.java.util.HeaderSerializationUtil;
import com.microsoft.identity.common.java.util.ObjectMapper;
import com.microsoft.identity.common.java.util.ResultUtil;

import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.NonNull;

/**
 * Abstract class for handling Microsoft STS token responses. The responses can from service can
 * encrypted or decrypted or either can be expected depends on contract between client and service.
 * Implementations of this class should handle the response accordingly. Standard response is
 * implemented in {@link MicrosoftStsTokenResponseHandler}. For PRT protocol contracts, different
 * implementation would be used that can handle encrypted and unencryted responses.
 */
public abstract class AbstractMicrosoftStsTokenResponseHandler implements ITokenResponseHandler<TokenResult> {

    private static final String TAG = AbstractMicrosoftStsTokenResponseHandler.class.getSimpleName();

    /**
     * Handle the token response from the service.
     * @param response An {@link HttpResponse} from the service}
     */
    @NonNull
    public TokenResult handleTokenResponse(@NonNull final HttpResponse response) throws ClientException {
        final String methodTag = TAG + ":handleTokenResponse";

        final MicrosoftStsTokenResponse tokenResponse;
        final TokenErrorResponse tokenErrorResponse;

        if (response.getStatusCode() >= HttpURLConnection.HTTP_BAD_REQUEST) {
            //An error occurred
            tokenResponse = null;
            tokenErrorResponse = getErrorResponse(response);
        } else {
            tokenResponse = getSuccessfulResponse(response);
            tokenErrorResponse = null;
        }

        final TokenResult result = new TokenResult(tokenResponse, tokenErrorResponse);

        ResultUtil.logResult(methodTag, result);

        if (null != response.getHeaders()) {
            final Map<String, List<String>> responseHeaders = response.getHeaders();

            final List<String> cliTelemValues;
            if (null != (cliTelemValues = responseHeaders.get(X_MS_CLITELEM))
                    && !cliTelemValues.isEmpty()) {
                // Element should only contain 1 value...
                final String cliTelemHeader = cliTelemValues.get(0);
                final CliTelemInfo cliTelemInfo = CliTelemInfo.fromXMsCliTelemHeader(
                        cliTelemHeader
                );
                // Parse and set the result...
                result.setCliTelemInfo(cliTelemInfo);

                if (null != tokenResponse && null != cliTelemInfo) {
                    tokenResponse.setSpeRing(cliTelemInfo.getSpeRing());
                    tokenResponse.setRefreshTokenAge(cliTelemInfo.getRefreshTokenAge());
                    tokenResponse.setCliTelemErrorCode(cliTelemInfo.getServerErrorCode());
                    tokenResponse.setCliTelemSubErrorCode(cliTelemInfo.getServerSubErrorCode());
                }
            }

            final Map<String, String> mapWithAdditionalEntry = new HashMap<String, String>();

            final String ccsRequestId = response.getHeaderValue(XMS_CCS_REQUEST_ID, 0);
            if (null != ccsRequestId) {
                SpanExtension.current().setAttribute(AttributeName.ccs_request_id.name(), ccsRequestId);
                if (CommonFlightsManager.INSTANCE.getFlightsProvider().isFlightEnabled(CommonFlight.EXPOSE_CCS_REQUEST_ID_IN_TOKENRESPONSE)){
                    mapWithAdditionalEntry.put(XMS_CCS_REQUEST_ID, ccsRequestId);
                }
            }

            final String ccsRequestSequence = response.getHeaderValue(XMS_CCS_REQUEST_SEQUENCE, 0);
            if (null != ccsRequestSequence) {
                SpanExtension.current().setAttribute(AttributeName.ccs_request_sequence.name(), ccsRequestSequence);
                if (CommonFlightsManager.INSTANCE.getFlightsProvider().isFlightEnabled(CommonFlight.EXPOSE_CCS_REQUEST_SEQUENCE_IN_TOKENRESPONSE)){
                    mapWithAdditionalEntry.put(XMS_CCS_REQUEST_SEQUENCE, ccsRequestSequence);
                }
            }

            if (null != tokenResponse) {
                if (null != tokenResponse.getExtraParameters()) {
                    for (final Map.Entry<String, String> entry : tokenResponse.getExtraParameters()) {
                        mapWithAdditionalEntry.put(entry.getKey(), entry.getValue());
                    }
                }

                tokenResponse.setExtraParameters(mapWithAdditionalEntry.entrySet());
            }
        }

        return result;
    }

    private MicrosoftTokenErrorResponse getErrorResponse(@NonNull final HttpResponse response) throws ClientException {
        MicrosoftTokenErrorResponse tokenErrorResponse;
        try {
            tokenErrorResponse = ObjectMapper.deserializeJsonStringToObject(
                    getBodyFromUnsuccessfulResponse(response.getBody()),
                    MicrosoftTokenErrorResponse.class
            );
        } catch (final JsonParseException ex) {
            tokenErrorResponse = new MicrosoftTokenErrorResponse();
            final String statusCode = String.valueOf(response.getStatusCode());
            tokenErrorResponse.setError(statusCode);
            tokenErrorResponse.setErrorDescription("Received " + statusCode + " status code from Server ");
        }
        tokenErrorResponse.setStatusCode(response.getStatusCode());

        if (null != response.getHeaders()) {
            tokenErrorResponse.setResponseHeadersJson(
                    HeaderSerializationUtil.toJson(response.getHeaders())
            );
        }
        tokenErrorResponse.setResponseBody(response.getBody());
        return tokenErrorResponse;
    }

    private String getBodyFromUnsuccessfulResponse(@NonNull final String responseBody) {
        final String EMPTY_JSON_OBJECT = "{}";
        return responseBody.isEmpty() ? EMPTY_JSON_OBJECT : responseBody;
    }

    /**
     * Error responses are handled in the class as they are not customized,
     * successful need to implemented by the subclass. Standard succesful response is handled
     * by {@link MicrosoftStsTokenResponseHandler}
     */
    abstract protected MicrosoftStsTokenResponse getSuccessfulResponse(@NonNull final HttpResponse httpResponse) throws ClientException;
}
