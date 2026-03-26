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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import io.opentelemetry.api.trace.Span;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link ClientDataInfo}.
 */
@RunWith(JUnit4.class)
public class ClientDataInfoTest {

    // -----------------------------------------------------------------------
    // fromJson() tests
    // -----------------------------------------------------------------------

    @Test
    public void testFromJson_validAllFields_allFieldsParsed() throws Exception {
        final String json = "{\"at\":\"m\",\"e\":\"50058\",\"se\":\"basic_action\",\"sr\":\"ring1\",\"ta\":\"100\"}";
        final String urlEncoded = URLEncoder.encode(json, StandardCharsets.UTF_8.name());

        final ClientDataInfo info = ClientDataInfo.fromJson(urlEncoded);

        assertNotNull(info);
        assertEquals("m", info.getAccountType());
        assertEquals("50058", info.getError());
        assertEquals("basic_action", info.getSubError());
        assertEquals("ring1", info.getSpeRing());
        assertEquals("100", info.getTokenAge());
    }

    @Test
    public void testFromJson_partialFields_onlySetFieldsPopulated() throws Exception {
        final String json = "{\"at\":\"e\",\"e\":\"90023\"}";
        final String urlEncoded = URLEncoder.encode(json, StandardCharsets.UTF_8.name());

        final ClientDataInfo info = ClientDataInfo.fromJson(urlEncoded);

        assertNotNull(info);
        assertEquals("e", info.getAccountType());
        assertEquals("90023", info.getError());
        assertNull(info.getSubError());
        assertNull(info.getSpeRing());
        assertNull(info.getTokenAge());
    }

    @Test
    public void testFromJson_malformedJson_returnsNull() {
        final ClientDataInfo info = ClientDataInfo.fromJson("{not valid json");
        assertNull(info);
    }

    @Test
    public void testFromJson_nullInput_returnsNull() {
        assertNull(ClientDataInfo.fromJson(null));
    }

    @Test
    public void testFromJson_emptyString_returnsNull() {
        assertNull(ClientDataInfo.fromJson(""));
    }

    // -----------------------------------------------------------------------
    // fromPipeDelimited() tests
    // -----------------------------------------------------------------------

    @Test
    public void testFromPipeDelimited_fiveSegments_allFieldsParsed() {
        final ClientDataInfo info = ClientDataInfo.fromPipeDelimited("m|50058|basic_action|ring1|100");

        assertNotNull(info);
        assertEquals("m", info.getAccountType());
        assertEquals("50058", info.getError());
        assertEquals("basic_action", info.getSubError());
        assertEquals("ring1", info.getSpeRing());
        assertEquals("100", info.getTokenAge());
    }

    @Test
    public void testFromPipeDelimited_threeSegments_onlyFirstThreeFields() {
        final ClientDataInfo info = ClientDataInfo.fromPipeDelimited("e|90023|basic_action");

        assertNotNull(info);
        assertEquals("e", info.getAccountType());
        assertEquals("90023", info.getError());
        assertEquals("basic_action", info.getSubError());
        assertNull(info.getSpeRing());
        assertNull(info.getTokenAge());
    }

    @Test
    public void testFromPipeDelimited_twoSegments_returnsNull() {
        assertNull(ClientDataInfo.fromPipeDelimited("m|50058"));
    }

    @Test
    public void testFromPipeDelimited_oneSegment_returnsNull() {
        assertNull(ClientDataInfo.fromPipeDelimited("m"));
    }

    @Test
    public void testFromPipeDelimited_nullInput_returnsNull() {
        assertNull(ClientDataInfo.fromPipeDelimited(null));
    }

    @Test
    public void testFromPipeDelimited_emptyString_returnsNull() {
        assertNull(ClientDataInfo.fromPipeDelimited(""));
    }

    @Test
    public void testFromPipeDelimited_emptySegments_fieldsAreNull() {
        final ClientDataInfo info = ClientDataInfo.fromPipeDelimited("||");

        assertNotNull(info);
        assertNull(info.getAccountType());
        assertNull(info.getError());
        assertNull(info.getSubError());
        assertNull(info.getSpeRing());
        assertNull(info.getTokenAge());
    }

    // -----------------------------------------------------------------------
    // emitToSpan() tests
    // -----------------------------------------------------------------------

    @Test
    public void testEmitToSpan_allFieldsSet_allAttributesEmitted() throws Exception {
        final String json = "{\"at\":\"m\",\"e\":\"50058\",\"se\":\"basic_action\",\"sr\":\"ring1\",\"ta\":\"100\"}";
        final ClientDataInfo info = ClientDataInfo.fromJson(URLEncoder.encode(json, StandardCharsets.UTF_8.name()));
        assertNotNull(info);

        final Span mockSpan = Mockito.mock(Span.class);
        Mockito.when(mockSpan.setAttribute(Mockito.anyString(), Mockito.anyString())).thenReturn(mockSpan);

        info.emitToSpan(mockSpan);

        verify(mockSpan, times(1)).setAttribute(eq(AttributeName.account_type.name()), eq("MSA"));
        verify(mockSpan, times(1)).setAttribute(eq(AttributeName.server_client_data_error.name()), eq("50058"));
        verify(mockSpan, times(1)).setAttribute(eq(AttributeName.server_client_data_sub_error.name()), eq("basic_action"));
        verify(mockSpan, times(1)).setAttribute(eq(AttributeName.server_client_data_spe_ring.name()), eq("ring1"));
        verify(mockSpan, times(1)).setAttribute(eq(AttributeName.server_client_data_token_age.name()), eq("100"));
    }

    @Test
    public void testEmitToSpan_someFieldsNull_nullFieldsNotEmitted() {
        final ClientDataInfo info = ClientDataInfo.fromPipeDelimited("e|90023|basic_action");
        assertNotNull(info);

        final Span mockSpan = Mockito.mock(Span.class);
        Mockito.when(mockSpan.setAttribute(Mockito.anyString(), Mockito.anyString())).thenReturn(mockSpan);

        info.emitToSpan(mockSpan);

        verify(mockSpan, times(1)).setAttribute(eq(AttributeName.account_type.name()), eq("AAD"));
        verify(mockSpan, times(1)).setAttribute(eq(AttributeName.server_client_data_error.name()), eq("90023"));
        verify(mockSpan, times(1)).setAttribute(eq(AttributeName.server_client_data_sub_error.name()), eq("basic_action"));
        verify(mockSpan, never()).setAttribute(eq(AttributeName.server_client_data_spe_ring.name()), Mockito.anyString());
        verify(mockSpan, never()).setAttribute(eq(AttributeName.server_client_data_token_age.name()), Mockito.anyString());
    }

    @Test
    public void testEmitToSpan_accountTypeMsa_emitsAccountTypeMSA() {
        final ClientDataInfo info = ClientDataInfo.fromPipeDelimited("m|0||ring1|");
        assertNotNull(info);

        final Span mockSpan = Mockito.mock(Span.class);
        Mockito.when(mockSpan.setAttribute(Mockito.anyString(), Mockito.anyString())).thenReturn(mockSpan);

        info.emitToSpan(mockSpan);

        verify(mockSpan).setAttribute(eq(AttributeName.account_type.name()), eq("MSA"));
    }

    @Test
    public void testEmitToSpan_accountTypeAad_emitsAccountTypeAAD() {
        final ClientDataInfo info = ClientDataInfo.fromPipeDelimited("e|0||ring1|");
        assertNotNull(info);

        final Span mockSpan = Mockito.mock(Span.class);
        Mockito.when(mockSpan.setAttribute(Mockito.anyString(), Mockito.anyString())).thenReturn(mockSpan);

        info.emitToSpan(mockSpan);

        verify(mockSpan).setAttribute(eq(AttributeName.account_type.name()), eq("AAD"));
    }

    @Test
    public void testEmitToSpan_nullSpan_doesNotThrow() {
        final ClientDataInfo info = ClientDataInfo.fromPipeDelimited("m|0|0|ring1|100");
        assertNotNull(info);
        // Should not throw
        info.emitToSpan(null);
    }

    // -----------------------------------------------------------------------
    // Field truncation test
    // -----------------------------------------------------------------------

    @Test
    public void testFieldTruncation_fieldExceedsMaxLength_truncatedTo256() throws Exception {
        final String longValue = new String(new char[300]).replace('\0', 'x');
        final String json = "{\"e\":\"" + longValue + "\"}";
        final ClientDataInfo info = ClientDataInfo.fromJson(URLEncoder.encode(json, StandardCharsets.UTF_8.name()));

        assertNotNull(info);
        assertNotNull(info.getError());
        assertEquals(ClientDataInfo.MAX_FIELD_LENGTH, info.getError().length());
    }

    @Test
    public void testFieldTruncation_pipeDelimitedFieldExceedsMaxLength_truncatedTo256() {
        final String longValue = new String(new char[300]).replace('\0', 'y');
        final ClientDataInfo info = ClientDataInfo.fromPipeDelimited("m|" + longValue + "|suberr");

        assertNotNull(info);
        assertNotNull(info.getError());
        assertEquals(ClientDataInfo.MAX_FIELD_LENGTH, info.getError().length());
    }
}
