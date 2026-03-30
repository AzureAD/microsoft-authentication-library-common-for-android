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
import org.mockito.Mockito;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import io.opentelemetry.api.trace.Span;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ClientDataInfo}.
 */
@RunWith(JUnit4.class)
public class ClientDataInfoTest {

    // -------------------------------------------------------------------------
    // fromJson() tests
    // -------------------------------------------------------------------------

    @Test
    public void fromJson_validInput_allFieldsParsed() throws Exception {
        final String raw = "{\"Error\":\"AADSTS50058\","
                + "\"SubError\":\"login_required\","
                + "\"AccountType\":\"m\","
                + "\"cloud_instance\":\"public\","
                + "\"caller_data_boundary\":\"us\"}";
        final String encoded = URLEncoder.encode(raw, StandardCharsets.UTF_8.name());

        final ClientDataInfo info = ClientDataInfo.fromJson(encoded);

        assertNotNull(info);
        assertEquals("AADSTS50058", info.getError());
        assertEquals("login_required", info.getSubError());
        assertEquals("m", info.getAccountType());
        assertEquals("public", info.getCloudInstance());
        assertEquals("us", info.getCallerDataBoundary());
    }

    @Test
    public void fromJson_partialInput_onlyPresentFieldsSet() throws Exception {
        final String raw = "{\"Error\":\"AADSTS65001\",\"AccountType\":\"e\"}";
        final String encoded = URLEncoder.encode(raw, StandardCharsets.UTF_8.name());

        final ClientDataInfo info = ClientDataInfo.fromJson(encoded);

        assertNotNull(info);
        assertEquals("AADSTS65001", info.getError());
        assertEquals("e", info.getAccountType());
        assertNull(info.getSubError());
        assertNull(info.getCloudInstance());
        assertNull(info.getCallerDataBoundary());
    }

    @Test
    public void fromJson_malformedJson_returnsNull() throws Exception {
        final String malformed = URLEncoder.encode("{not valid json}", StandardCharsets.UTF_8.name());
        assertNull(ClientDataInfo.fromJson(malformed));
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
    // fromPipeDelimited() tests
    // -------------------------------------------------------------------------

    @Test
    public void fromPipeDelimited_validFiveSegments_allFieldsParsed() {
        // format: account_type|error|sub_error|caller_data_boundary|cloud_instance
        final ClientDataInfo info = ClientDataInfo.fromPipeDelimited("m|AADSTS50058|login_required|us|public");

        assertNotNull(info);
        assertEquals("m", info.getAccountType());
        assertEquals("AADSTS50058", info.getError());
        assertEquals("login_required", info.getSubError());
        assertEquals("us", info.getCallerDataBoundary());
        assertEquals("public", info.getCloudInstance());
    }

    @Test
    public void fromPipeDelimited_threeSegments_firstThreeFieldsSet() {
        final ClientDataInfo info = ClientDataInfo.fromPipeDelimited("e|AADSTS65001|consent_required");

        assertNotNull(info);
        assertEquals("e", info.getAccountType());
        assertEquals("AADSTS65001", info.getError());
        assertEquals("consent_required", info.getSubError());
        assertNull(info.getCallerDataBoundary());
        assertNull(info.getCloudInstance());
    }

    @Test
    public void fromPipeDelimited_fewerThanThreeSegments_returnsNull() {
        assertNull(ClientDataInfo.fromPipeDelimited("m|AADSTS50058"));
    }

    @Test
    public void fromPipeDelimited_emptySegments_fieldsAreNull() {
        // Three empty segments – meets minimum count but values are empty → null
        final ClientDataInfo info = ClientDataInfo.fromPipeDelimited("||");

        assertNotNull(info);
        assertNull(info.getAccountType());
        assertNull(info.getError());
        assertNull(info.getSubError());
    }

    @Test
    public void fromPipeDelimited_nullInput_returnsNull() {
        assertNull(ClientDataInfo.fromPipeDelimited(null));
    }

    @Test
    public void fromPipeDelimited_emptyString_returnsNull() {
        assertNull(ClientDataInfo.fromPipeDelimited(""));
    }

    // -------------------------------------------------------------------------
    // emitToSpan() tests
    // -------------------------------------------------------------------------

    @Test
    public void emitToSpan_allFieldsSet_allAttributesEmitted() {
        final ClientDataInfo info = new ClientDataInfo();
        info.setError("AADSTS50058");
        info.setSubError("login_required");
        info.setAccountType("m");
        info.setCloudInstance("public");
        info.setCallerDataBoundary("us");

        final Span mockSpan = mock(Span.class);
        when(mockSpan.setAttribute(Mockito.anyString(), Mockito.anyString())).thenReturn(mockSpan);

        try (MockedStatic<SpanExtension> mockedExtension = Mockito.mockStatic(SpanExtension.class)) {
            mockedExtension.when(SpanExtension::current).thenReturn(mockSpan);

            info.emitToSpan();

            verify(mockSpan).setAttribute(AttributeName.server_error.name(), "AADSTS50058");
            verify(mockSpan).setAttribute(AttributeName.server_sub_error.name(), "login_required");
            verify(mockSpan).setAttribute(AttributeName.account_type.name(), "MSA");
            verify(mockSpan).setAttribute(AttributeName.server_cloud_instance.name(), "public");
            verify(mockSpan).setAttribute(AttributeName.server_caller_data_boundary.name(), "us");
        }
    }

    @Test
    public void emitToSpan_someFieldsNull_nullFieldsNotEmitted() {
        final ClientDataInfo info = new ClientDataInfo();
        info.setError("AADSTS50058");
        // subError, accountType, cloudInstance, callerDataBoundary all null

        final Span mockSpan = mock(Span.class);
        when(mockSpan.setAttribute(Mockito.anyString(), Mockito.anyString())).thenReturn(mockSpan);

        try (MockedStatic<SpanExtension> mockedExtension = Mockito.mockStatic(SpanExtension.class)) {
            mockedExtension.when(SpanExtension::current).thenReturn(mockSpan);

            info.emitToSpan();

            verify(mockSpan).setAttribute(AttributeName.server_error.name(), "AADSTS50058");
            verify(mockSpan, never()).setAttribute(
                    Mockito.eq(AttributeName.server_sub_error.name()), Mockito.anyString());
            verify(mockSpan, never()).setAttribute(
                    Mockito.eq(AttributeName.account_type.name()), Mockito.anyString());
            verify(mockSpan, never()).setAttribute(
                    Mockito.eq(AttributeName.server_cloud_instance.name()), Mockito.anyString());
            verify(mockSpan, never()).setAttribute(
                    Mockito.eq(AttributeName.server_caller_data_boundary.name()), Mockito.anyString());
        }
    }

    @Test
    public void emitToSpan_accountTypeMsa_mappedToMSA() {
        final ClientDataInfo info = new ClientDataInfo();
        info.setAccountType("m");

        final Span mockSpan = mock(Span.class);
        when(mockSpan.setAttribute(Mockito.anyString(), Mockito.anyString())).thenReturn(mockSpan);

        try (MockedStatic<SpanExtension> mockedExtension = Mockito.mockStatic(SpanExtension.class)) {
            mockedExtension.when(SpanExtension::current).thenReturn(mockSpan);

            info.emitToSpan();

            verify(mockSpan).setAttribute(AttributeName.account_type.name(), "MSA");
        }
    }

    @Test
    public void emitToSpan_accountTypeAad_mappedToAAD() {
        final ClientDataInfo info = new ClientDataInfo();
        info.setAccountType("e");

        final Span mockSpan = mock(Span.class);
        when(mockSpan.setAttribute(Mockito.anyString(), Mockito.anyString())).thenReturn(mockSpan);

        try (MockedStatic<SpanExtension> mockedExtension = Mockito.mockStatic(SpanExtension.class)) {
            mockedExtension.when(SpanExtension::current).thenReturn(mockSpan);

            info.emitToSpan();

            verify(mockSpan).setAttribute(AttributeName.account_type.name(), "AAD");
        }
    }

    // -------------------------------------------------------------------------
    // Field truncation test
    // -------------------------------------------------------------------------

    @Test
    public void emitToSpan_fieldExceeds256Chars_truncatedTo256() {
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 300; i++) {
            sb.append('A');
        }
        final String longValue = sb.toString();
        final StringBuilder expected256 = new StringBuilder();
        for (int i = 0; i < 256; i++) {
            expected256.append('A');
        }

        final ClientDataInfo info = new ClientDataInfo();
        info.setError(longValue);

        final Span mockSpan = mock(Span.class);
        when(mockSpan.setAttribute(Mockito.anyString(), Mockito.anyString())).thenReturn(mockSpan);

        try (MockedStatic<SpanExtension> mockedExtension = Mockito.mockStatic(SpanExtension.class)) {
            mockedExtension.when(SpanExtension::current).thenReturn(mockSpan);

            info.emitToSpan();

            verify(mockSpan).setAttribute(
                    AttributeName.server_error.name(),
                    expected256.toString());
        }
    }
}
