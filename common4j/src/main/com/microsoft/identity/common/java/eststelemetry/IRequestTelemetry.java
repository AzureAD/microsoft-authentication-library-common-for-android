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
package com.microsoft.identity.common.java.eststelemetry;

interface IRequestTelemetry {

    /**
     * Get partial header string pertaining to fields specific to this telemetry object
     *
     * @return Header string component for direct member fields
     */
    String getHeaderStringForFields();

    /**
     * Get schema version of the telemetry.
     *
     * @return schema version.
     */
    String getSchemaVersion();

    /**
     * Get a complete header string for all fields belonging to the schema for this telemetry object
     *
     * @return Complete header string for this telemetry object
     */
    String getCompleteHeaderString();

    /**
     * Returning a telemetry object that contains data included in the provided telemetry object
     *
     * @param requestTelemetry supplied telemetry object
     * @return telemetry object
     */
    IRequestTelemetry copySharedValues(IRequestTelemetry requestTelemetry);
}
