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

import com.microsoft.identity.common.java.opentelemetry.AttributeName;
import com.microsoft.identity.common.java.opentelemetry.SpanExtension;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.MockedStatic;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import io.opentelemetry.api.trace.Span;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ClientDataInfo} – covering all parsing paths,
 * edge cases, and span-attribute emission.
 */
@RunWith(JUnit4.class)
public class ClientDataInfoTest {

    // -------------------------------------------------------------------------
    // fromJson – success paths
    // -------------------------------------------------------------------------

    @Test
    public void fromJson_validUrlEncodedJson_allFiveFieldsParsed() throws Exception {
        final String json = "{\"Error\":\"50001\",\"SubError\":\"sub1\","
                + "\"AccountType\":\"e\",\"cloud_instance\":\"login.microsoftonline.de\","
                + "\"caller_data_boundary\":\"EU\"}";
        final String encoded = URLEncoder.encode(json, StandardCharsets.UTF_8.name());

        final ClientDataInfo info = ClientDataInfo.fromJson(encoded);

        assertNotNull(info);
        assertEquals("50001", info.getError());
        assertEquals("sub1", info.getSubError());
        assertEquals("e", info.getAccountType());
        assertEquals("login.microsoftonline.de", info.getCloudInstance());
        assertEquals("EU", info.getCallerDataBoundary());
    }

    @Test
    public void fromJson_partialJson_onlyPresentFieldsSet() throws Exception {
        final String json = "{\"Error\":\"65001\",\"AccountType\":\"m\"}";
        final String encoded = URLEncoder.encode(json, StandardCharsets.UTF_8.name());

        final ClientDataInfo info = ClientDataInfo.fromJson(encoded);

        assertNotNull(info);
        assertEquals("65001", info.getError());
        assertEquals("m", info.getAccountType());
        assertNull("SubError should be null", info.getSubError());
        assertNull("CloudInstance should be null", info.getCloudInstance());
        assertNull("CallerDataBoundary should be null", info.getCallerDataBoundary());
    }

    @Test
    public void fromJson_notUrlEncoded_isHandledGracefully() {
        // A plain JSON string (not URL-encoded) should still parse because
        // URLDecoder is a no-op for strings that contain no percent-encoding.
        final String json = "{\"Error\":\"50076\"}";

        final ClientDataInfo info = ClientDataInfo.fromJson(json);

        assertNotNull(info);
        assertEquals("50076", info.getError());
    }

    // -------------------------------------------------------------------------
    // fromJson – failure / edge-case paths
    // -------------------------------------------------------------------------

    @Test
    public void fromJson_malformedJson_returnsNull() {
        // Should NOT throw – just return null and log a warning.
        final ClientDataInfo info = ClientDataInfo.fromJson("not-valid-json{{{");
        assertNull(info);
    }

    @Test
    public void fromJson_nullInput_returnsNull() {
        assertNull(ClientDataInfo.fromJson(null));
    }

    @Test
    public void fromJson_emptyString_returnsNull() {
        assertNull(ClientDataInfo.fromJson(""));
    }

    // -------------------------------------------------------------------------
    // fromPipeDelimited – success paths
    // -------------------------------------------------------------------------

    @Test
    public void fromPipeDelimited_validFiveSegments_allFieldsParsed() throws Exception {
        // Format: account_type|error|sub_error|caller_data_boundary|cloud_instance
        final String value = "e|50001|sub1|EU|login.microsoftonline.de";
        final String encoded = URLEncoder.encode(value, StandardCharsets.UTF_8.name());

        final ClientDataInfo info = ClientDataInfo.fromPipeDelimited(encoded);

        assertNotNull(info);
        assertEquals("e", info.getAccountType());
        assertEquals("50001", info.getError());
        assertEquals("sub1", info.getSubError());
        assertEquals("EU", info.getCallerDataBoundary());
        assertEquals("login.microsoftonline.de", info.getCloudInstance());
    }

    @Test
    public void fromPipeDelimited_threeSegments_onlyFirstThreeFieldsSet() throws Exception {
        final String value = "m|65001|sub2";
        final String encoded = URLEncoder.encode(value, StandardCharsets.UTF_8.name());

        final ClientDataInfo info = ClientDataInfo.fromPipeDelimited(encoded);

        assertNotNull(info);
        assertEquals("m", info.getAccountType());
        assertEquals("65001", info.getError());
        assertEquals("sub2", info.getSubError());
        assertNull("CallerDataBoundary should be null", info.getCallerDataBoundary());
        assertNull("CloudInstance should be null", info.getCloudInstance());
    }

    @Test
    public void fromPipeDelimited_notUrlEncoded_isHandledGracefully() {
        final String value = "e|50001|sub1|EU|login.microsoftonline.de";

        final ClientDataInfo info = ClientDataInfo.fromPipeDelimited(value);

        assertNotNull(info);
        assertEquals("50001", info.getError());
    }

    // -------------------------------------------------------------------------
    // fromPipeDelimited – failure / edge-case paths
    // -------------------------------------------------------------------------

    @Test
    public void fromPipeDelimited_lessThanThreeSegments_returnsNull() {
        assertNull(ClientDataInfo.fromPipeDelimited("m|50001"));
    }

    @Test
    public void fromPipeDelimited_nullInput_returnsNull() {
        assertNull(ClientDataInfo.fromPipeDelimited(null));
    }

    @Test
    public void fromPipeDelimited_emptyString_returnsNull() {
        assertNull(ClientDataInfo.fromPipeDelimited(""));
    }

    @Test
    public void fromPipeDelimited_emptySegments_fieldsAreNull() {
        // Three segments present, all empty strings -> fields should be null
        final ClientDataInfo info = ClientDataInfo.fromPipeDelimited("||");

        assertNotNull(info);
        assertNull("AccountType should be null", info.getAccountType());
        assertNull("Error should be null", info.getError());
        assertNull("SubError should be null", info.getSubError());
    }

    // -------------------------------------------------------------------------
    // emitToSpan – attribute emission
    // -------------------------------------------------------------------------

    @Test
    public void emitToSpan_allFieldsSet_allAttributesEmitted() {
        final Span mockSpan = mock(Span.class);
        when(mockSpan.setAttribute(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString())).thenReturn(mockSpan);

        try (MockedStatic<SpanExtension> spanExtensionMock = mockStatic(SpanExtension.class)) {
            spanExtensionMock.when(SpanExtension::current).thenReturn(mockSpan);

            final ClientDataInfo info = new ClientDataInfo();
            info.setError("50001");
            info.setSubError("sub1");
            info.setAccountType("e");       // maps to AAD
            info.setCloudInstance("login.microsoftonline.de");
            info.setCallerDataBoundary("EU");
            info.emitToSpan();

            verify(mockSpan).setAttribute(AttributeName.server_error.name(), "50001");
            verify(mockSpan).setAttribute(AttributeName.server_sub_error.name(), "sub1");
            verify(mockSpan).setAttribute(AttributeName.account_type.name(), "AAD");
            verify(mockSpan).setAttribute(AttributeName.server_cloud_instance.name(), "login.microsoftonline.de");
            verify(mockSpan).setAttribute(AttributeName.server_caller_data_boundary.name(), "EU");
        }
    }

    @Test
    public void emitToSpan_someFieldsNull_nullFieldsNotEmitted() {
        final Span mockSpan = mock(Span.class);
        when(mockSpan.setAttribute(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString())).thenReturn(mockSpan);

        try (MockedStatic<SpanExtension> spanExtensionMock = mockStatic(SpanExtension.class)) {
            spanExtensionMock.when(SpanExtension::current).thenReturn(mockSpan);

            final ClientDataInfo info = new ClientDataInfo();
            info.setError("50001");
            // mSubError, mAccountType, mCloudInstance, mCallerDataBoundary all null
            info.emitToSpan();

            verify(mockSpan).setAttribute(AttributeName.server_error.name(), "50001");
            verify(mockSpan, never()).setAttribute(
                    org.mockito.ArgumentMatchers.eq(AttributeName.server_sub_error.name()),
                    org.mockito.ArgumentMatchers.anyString());
            verify(mockSpan, never()).setAttribute(
                    org.mockito.ArgumentMatchers.eq(AttributeName.account_type.name()),
                    org.mockito.ArgumentMatchers.anyString());
            verify(mockSpan, never()).setAttribute(
                    org.mockito.ArgumentMatchers.eq(AttributeName.server_cloud_instance.name()),
                    org.mockito.ArgumentMatchers.anyString());
            verify(mockSpan, never()).setAttribute(
                    org.mockito.ArgumentMatchers.eq(AttributeName.server_caller_data_boundary.name()),
                    org.mockito.ArgumentMatchers.anyString());
        }
    }

    @Test
    public void emitToSpan_accountTypeMsaRaw_mapsToMSA() {
        final Span mockSpan = mock(Span.class);
        when(mockSpan.setAttribute(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString())).thenReturn(mockSpan);

        try (MockedStatic<SpanExtension> spanExtensionMock = mockStatic(SpanExtension.class)) {
            spanExtensionMock.when(SpanExtension::current).thenReturn(mockSpan);

            final ClientDataInfo info = new ClientDataInfo();
            info.setAccountType("m");
            info.emitToSpan();

            verify(mockSpan).setAttribute(AttributeName.account_type.name(), "MSA");
        }
    }

    @Test
    public void emitToSpan_accountTypeAadRaw_mapsToAAD() {
        final Span mockSpan = mock(Span.class);
        when(mockSpan.setAttribute(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString())).thenReturn(mockSpan);

        try (MockedStatic<SpanExtension> spanExtensionMock = mockStatic(SpanExtension.class)) {
            spanExtensionMock.when(SpanExtension::current).thenReturn(mockSpan);

            final ClientDataInfo info = new ClientDataInfo();
            info.setAccountType("e");
            info.emitToSpan();

            verify(mockSpan).setAttribute(AttributeName.account_type.name(), "AAD");
        }
    }

    @Test
    public void emitToSpan_unknownAccountType_passedThrough() {
        final Span mockSpan = mock(Span.class);
        when(mockSpan.setAttribute(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString())).thenReturn(mockSpan);

        try (MockedStatic<SpanExtension> spanExtensionMock = mockStatic(SpanExtension.class)) {
            spanExtensionMock.when(SpanExtension::current).thenReturn(mockSpan);

            final ClientDataInfo info = new ClientDataInfo();
            info.setAccountType("unknown");
            info.emitToSpan();

            verify(mockSpan).setAttribute(AttributeName.account_type.name(), "unknown");
        }
    }

    // -------------------------------------------------------------------------
    // Field truncation
    // -------------------------------------------------------------------------

    @Test
    public void emitToSpan_fieldExceeds256Chars_truncatedTo256() {
        final Span mockSpan = mock(Span.class);
        when(mockSpan.setAttribute(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString())).thenReturn(mockSpan);

        try (MockedStatic<SpanExtension> spanExtensionMock = mockStatic(SpanExtension.class)) {
            spanExtensionMock.when(SpanExtension::current).thenReturn(mockSpan);

            final String longValue = "A".repeat(300);
            final String expectedTruncated = "A".repeat(256);

            final ClientDataInfo info = new ClientDataInfo();
            info.setError(longValue);
            info.emitToSpan();

            verify(mockSpan).setAttribute(AttributeName.server_error.name(), expectedTruncated);
        }
    }

    @Test
    public void emitToSpan_fieldExactly256Chars_notTruncated() {
        final Span mockSpan = mock(Span.class);
        when(mockSpan.setAttribute(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString())).thenReturn(mockSpan);

        try (MockedStatic<SpanExtension> spanExtensionMock = mockStatic(SpanExtension.class)) {
            spanExtensionMock.when(SpanExtension::current).thenReturn(mockSpan);

            final String value256 = "B".repeat(256);

            final ClientDataInfo info = new ClientDataInfo();
            info.setError(value256);
            info.emitToSpan();

            verify(mockSpan).setAttribute(AttributeName.server_error.name(), value256);
        }
    }
}
