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
import com.microsoft.identity.common.java.opentelemetry.AttributeName;
import com.microsoft.identity.common.java.opentelemetry.SpanExtension;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import io.opentelemetry.api.trace.Span;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
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
    // fromPipeDelimited() tests
    // -------------------------------------------------------------------------

    @Test
    public void fromPipeDelimited_validFiveSegments_allFieldsParsed() {
        // format: account_type|error|sub_error|caller_data_boundary|cloud_instance
        final String raw = "m|AADSTS50058|login_required|us|public";
        final ClientDataInfo info = ClientDataInfo.fromPipeDelimited(raw);

        assertNotNull(info);
        assertEquals("m", info.getAccountType());
        assertEquals("AADSTS50058", info.getError());
        assertEquals("login_required", info.getSubError());
        assertEquals("us", info.getCallerDataBoundary());
        assertEquals("public", info.getCloudInstance());
        assertEquals(raw, info.getRaw());
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
        // format: account_type|error|sub_error|caller_data_boundary|cloud_instance
        final ClientDataInfo info = ClientDataInfo.fromPipeDelimited("m|AADSTS50058|login_required|us|public");
        assertNotNull(info);

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
        // Only error populated; account_type and sub_error empty
        final ClientDataInfo info = ClientDataInfo.fromPipeDelimited("|AADSTS50058|");
        assertNotNull(info);

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
        // Only account_type populated (m for MSA)
        final ClientDataInfo info = ClientDataInfo.fromPipeDelimited("m||");
        assertNotNull(info);

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
        // Only account_type populated (e for AAD)
        final ClientDataInfo info = ClientDataInfo.fromPipeDelimited("e||");
        assertNotNull(info);

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

        // Construct via the only public entry point; long value goes in the error field
        final ClientDataInfo info = ClientDataInfo.fromPipeDelimited("|" + longValue + "|");
        assertNotNull(info);

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

    // -------------------------------------------------------------------------
    // Input bounds, bounded splitting, and segment sanitization tests
    // -------------------------------------------------------------------------

    @Test
    public void fromPipeDelimited_exceedsMaxRawLength2048_logsWarnAndReturnsNull() {
        final String oversizedSubError = repeatChar('A', 2035);
        final String oversizedPayload = "m|AADSTS50058|" + oversizedSubError;
        assertEquals(2049, oversizedPayload.length());

        final Span mockSpan = mock(Span.class);
        try (MockedStatic<Logger> mockedLogger = Mockito.mockStatic(Logger.class);
             MockedStatic<SpanExtension> mockedExtension = Mockito.mockStatic(SpanExtension.class)) {
            mockedExtension.when(SpanExtension::current).thenReturn(mockSpan);

            assertNull(ClientDataInfo.fromPipeDelimited(oversizedPayload));

            final ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
            mockedLogger.verify(() -> Logger.warn(Mockito.anyString(), messageCaptor.capture()));
            final String warnMessage = messageCaptor.getValue();
            assertTrue(warnMessage.contains("2048"));
            assertFalse(warnMessage.contains(oversizedSubError));
            assertFalse(warnMessage.contains("AADSTS50058"));
            verify(mockSpan, never()).setAttribute(Mockito.anyString(), Mockito.anyString());
        }

        final String oversizedWhitespace = repeatChar(' ', 2049);
        assertEquals(2049, oversizedWhitespace.length());
        try (MockedStatic<Logger> mockedLogger = Mockito.mockStatic(Logger.class)) {
            assertNull(ClientDataInfo.fromPipeDelimited(oversizedWhitespace));

            final ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
            mockedLogger.verify(() -> Logger.warn(Mockito.anyString(), messageCaptor.capture()));
            final String warnMessage = messageCaptor.getValue();
            assertTrue(warnMessage.contains("2048"));
            assertFalse(warnMessage.contains(oversizedWhitespace));
        }
    }

    @Test
    public void fromPipeDelimited_exactMaxRawLength2048_parsedAndRawPreserved() {
        final String longSubError = repeatChar('A', 2034);
        final String exactMaxPayload = "m|AADSTS50058|" + longSubError;
        assertEquals(2048, exactMaxPayload.length());

        final ClientDataInfo info = ClientDataInfo.fromPipeDelimited(exactMaxPayload);
        assertNotNull(info);
        assertEquals(exactMaxPayload, info.getRaw());
        assertEquals(2048, info.getRaw().length());
        assertEquals("m", info.getAccountType());
        assertEquals("AADSTS50058", info.getError());
        assertEquals(longSubError, info.getSubError());
        assertEquals(2034, info.getSubError().length());

        final Span mockSpan = mock(Span.class);
        when(mockSpan.setAttribute(Mockito.anyString(), Mockito.anyString())).thenReturn(mockSpan);
        try (MockedStatic<SpanExtension> mockedExtension = Mockito.mockStatic(SpanExtension.class)) {
            mockedExtension.when(SpanExtension::current).thenReturn(mockSpan);

            info.emitToSpan();

            verify(mockSpan).setAttribute(AttributeName.account_type.name(), "MSA");
            verify(mockSpan).setAttribute(AttributeName.server_error.name(), "AADSTS50058");
            verify(mockSpan).setAttribute(AttributeName.server_sub_error.name(), repeatChar('A', 256));
        }
    }

    @Test
    public void fromPipeDelimited_moreThanFiveSegments_boundsSplitAndPreservesFifthFieldAndRaw() {
        final String rawSix = "m|AADSTS50058|login_required|us|public|ext1";
        final ClientDataInfo infoSix = ClientDataInfo.fromPipeDelimited(rawSix);
        assertNotNull(infoSix);
        assertEquals("m", infoSix.getAccountType());
        assertEquals("AADSTS50058", infoSix.getError());
        assertEquals("login_required", infoSix.getSubError());
        assertEquals("us", infoSix.getCallerDataBoundary());
        assertEquals("public", infoSix.getCloudInstance());
        assertEquals(rawSix, infoSix.getRaw());

        final String rawTrailingEmptySixth = "m|AADSTS50058|login_required|us|public|";
        final ClientDataInfo infoTrailing = ClientDataInfo.fromPipeDelimited(rawTrailingEmptySixth);
        assertNotNull(infoTrailing);
        assertEquals("public", infoTrailing.getCloudInstance());
        assertEquals(rawTrailingEmptySixth, infoTrailing.getRaw());

        final String rawEight = "e|err|sub|eu| sovereign |ext1|ext2|ext3";
        final ClientDataInfo infoEight = ClientDataInfo.fromPipeDelimited(rawEight);
        assertNotNull(infoEight);
        assertEquals("e", infoEight.getAccountType());
        assertEquals("err", infoEight.getError());
        assertEquals("sub", infoEight.getSubError());
        assertEquals("eu", infoEight.getCallerDataBoundary());
        assertEquals("sovereign", infoEight.getCloudInstance());
        assertEquals(rawEight, infoEight.getRaw());
    }

    @Test
    public void fromPipeDelimited_fourSegments_callerDataBoundarySetAndCloudInstanceNull() {
        final String raw = "e|AADSTS50058|login_required| eu ";
        final ClientDataInfo info = ClientDataInfo.fromPipeDelimited(raw);

        assertNotNull(info);
        assertEquals("e", info.getAccountType());
        assertEquals("AADSTS50058", info.getError());
        assertEquals("login_required", info.getSubError());
        assertEquals("eu", info.getCallerDataBoundary());
        assertNull(info.getCloudInstance());
        assertEquals(raw, info.getRaw());
    }

    @Test
    public void fromPipeDelimited_paddedAndCrlfSegments_normalizedAndMappedWithoutUnknownWarn() {
        final String aadRaw = " e | AADSTS50058\r\n | login_required ";
        final ClientDataInfo aadInfo = ClientDataInfo.fromPipeDelimited(aadRaw);
        assertNotNull(aadInfo);
        assertEquals("e", aadInfo.getAccountType());
        assertEquals("AADSTS50058", aadInfo.getError());
        assertEquals("login_required", aadInfo.getSubError());
        assertNull(aadInfo.getCallerDataBoundary());
        assertNull(aadInfo.getCloudInstance());
        assertEquals(aadRaw, aadInfo.getRaw());

        final Span aadSpan = mock(Span.class);
        when(aadSpan.setAttribute(Mockito.anyString(), Mockito.anyString())).thenReturn(aadSpan);
        try (MockedStatic<SpanExtension> mockedExtension = Mockito.mockStatic(SpanExtension.class);
             MockedStatic<Logger> mockedLogger = Mockito.mockStatic(Logger.class)) {
            mockedExtension.when(SpanExtension::current).thenReturn(aadSpan);

            aadInfo.emitToSpan();

            verify(aadSpan).setAttribute(AttributeName.account_type.name(), "AAD");
            verify(aadSpan).setAttribute(AttributeName.server_error.name(), "AADSTS50058");
            verify(aadSpan).setAttribute(AttributeName.server_sub_error.name(), "login_required");
            mockedLogger.verify(() -> Logger.warn(Mockito.anyString(), Mockito.contains("UNKNOWN")), never());
        }

        final String msaRaw = " \tm\r\n | AADSTS50058 | sub ";
        final ClientDataInfo msaInfo = ClientDataInfo.fromPipeDelimited(msaRaw);
        assertNotNull(msaInfo);
        assertEquals("m", msaInfo.getAccountType());
        assertEquals("AADSTS50058", msaInfo.getError());
        assertEquals("sub", msaInfo.getSubError());
        assertEquals(msaRaw, msaInfo.getRaw());

        final Span msaSpan = mock(Span.class);
        when(msaSpan.setAttribute(Mockito.anyString(), Mockito.anyString())).thenReturn(msaSpan);
        try (MockedStatic<SpanExtension> mockedExtension = Mockito.mockStatic(SpanExtension.class);
             MockedStatic<Logger> mockedLogger = Mockito.mockStatic(Logger.class)) {
            mockedExtension.when(SpanExtension::current).thenReturn(msaSpan);

            msaInfo.emitToSpan();

            verify(msaSpan).setAttribute(AttributeName.account_type.name(), "MSA");
            verify(msaSpan).setAttribute(AttributeName.server_error.name(), "AADSTS50058");
            verify(msaSpan).setAttribute(AttributeName.server_sub_error.name(), "sub");
            mockedLogger.verify(() -> Logger.warn(Mockito.anyString(), Mockito.contains("UNKNOWN")), never());
        }
    }

    @Test
    public void fromPipeDelimited_asciiControlCharsAcrossRange_strippedCleanly() {
        final String raw = " \u0000e\u007f |"
                + "\u001bAAD\u0000STS\u001f50058\r\n|"
                + " \tsub\u000b\f \u007ferror\n |"
                + "\u0000 u\ts \u001b|"
                + " \u007fpub\rlic\u001f ";
        final ClientDataInfo info = ClientDataInfo.fromPipeDelimited(raw);

        assertNotNull(info);
        assertEquals("e", info.getAccountType());
        assertEquals("AADSTS50058", info.getError());
        assertEquals("sub error", info.getSubError());
        assertEquals("us", info.getCallerDataBoundary());
        assertEquals("public", info.getCloudInstance());
        assertEquals(raw, info.getRaw());
    }

    @Test
    public void emitToSpan_whitespaceOnlyAndControlCharOnlySegments_omittedFromSpan() {
        final String raw = "   | \t\r\n | \u0000\u001f | \u007f |  \u007f\r\n  ";
        final ClientDataInfo info = ClientDataInfo.fromPipeDelimited(raw);

        assertNotNull(info);
        assertNull(info.getAccountType());
        assertNull(info.getError());
        assertNull(info.getSubError());
        assertNull(info.getCallerDataBoundary());
        assertNull(info.getCloudInstance());
        assertEquals(raw, info.getRaw());

        final Span mockSpan = mock(Span.class);
        try (MockedStatic<SpanExtension> mockedExtension = Mockito.mockStatic(SpanExtension.class)) {
            mockedExtension.when(SpanExtension::current).thenReturn(mockSpan);

            info.emitToSpan();

            verify(mockSpan, never()).setAttribute(Mockito.anyString(), Mockito.anyString());
        }
    }

    @Test
    public void fromPipeDelimited_whitespaceOrControlOnlyRawWithoutThreeSegments_returnsNull() {
        assertNull(ClientDataInfo.fromPipeDelimited("   "));
        assertNull(ClientDataInfo.fromPipeDelimited("\t\r\n"));
        assertNull(ClientDataInfo.fromPipeDelimited("\u0000\u001f"));
        assertNull(ClientDataInfo.fromPipeDelimited("\u007f"));
        assertNull(ClientDataInfo.fromPipeDelimited(" \u007f "));
        assertNull(ClientDataInfo.fromPipeDelimited(" | \u007f "));
    }

    @Test
    public void fromPipeDelimited_twoPipesEmptySegments_preservesRawAndNullFields() {
        final ClientDataInfo info = ClientDataInfo.fromPipeDelimited("||");

        assertNotNull(info);
        assertNull(info.getAccountType());
        assertNull(info.getError());
        assertNull(info.getSubError());
        assertNull(info.getCallerDataBoundary());
        assertNull(info.getCloudInstance());
        assertEquals("||", info.getRaw());

        final Span mockSpan = mock(Span.class);
        try (MockedStatic<SpanExtension> mockedExtension = Mockito.mockStatic(SpanExtension.class)) {
            mockedExtension.when(SpanExtension::current).thenReturn(mockSpan);

            info.emitToSpan();

            verify(mockSpan, never()).setAttribute(Mockito.anyString(), Mockito.anyString());
        }
    }

    @Test
    public void emitToSpan_unknownAccountType_emitsUnknown() {
        final ClientDataInfo info = ClientDataInfo.fromPipeDelimited("x|AADSTS50058|login_required");
        assertNotNull(info);
        assertEquals("x", info.getAccountType());

        final Span mockSpan = mock(Span.class);
        when(mockSpan.setAttribute(Mockito.anyString(), Mockito.anyString())).thenReturn(mockSpan);
        try (MockedStatic<SpanExtension> mockedExtension = Mockito.mockStatic(SpanExtension.class);
             MockedStatic<Logger> mockedLogger = Mockito.mockStatic(Logger.class)) {
            mockedExtension.when(SpanExtension::current).thenReturn(mockSpan);

            info.emitToSpan();

            verify(mockSpan).setAttribute(AttributeName.account_type.name(), "UNKNOWN");
            verify(mockSpan).setAttribute(AttributeName.server_error.name(), "AADSTS50058");
            verify(mockSpan).setAttribute(AttributeName.server_sub_error.name(), "login_required");
            mockedLogger.verify(() -> Logger.warn(Mockito.anyString(), Mockito.contains("UNKNOWN")));
        }
    }

    private static String repeatChar(final char ch, final int count) {
        final StringBuilder sb = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            sb.append(ch);
        }
        return sb.toString();
    }
}
