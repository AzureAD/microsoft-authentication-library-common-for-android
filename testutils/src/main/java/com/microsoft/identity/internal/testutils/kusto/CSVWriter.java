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
package com.microsoft.identity.internal.testutils.kusto;

import androidx.annotation.NonNull;

import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;
import java.io.Writer;

public class CSVWriter implements Closeable, Flushable {

    public static final int INITIAL_STRING_SIZE = 1024;

    /**
     * Default line terminator.
     */
    public static final String DEFAULT_LINE_END = "\n";

    /**
     * The default separator to use if none is supplied to the constructor.
     */
    public static final char DEFAULT_SEPARATOR = ',';

    protected final Writer writer;
    protected final char separator;
    protected String lineEnd;

    public CSVWriter(@NonNull final Writer writer, final char separator) {
        this.writer = writer;
        this.separator = separator;
        lineEnd = DEFAULT_LINE_END;
    }

    public CSVWriter(@NonNull final Writer writer) {
        this.writer = writer;
        this.separator = DEFAULT_SEPARATOR;
        lineEnd = DEFAULT_LINE_END;
    }

    /**
     * Writes the contents of the array to a file in a comma separated format.
     *
     * @param nextLine a string array with each comma-separated element as a separate
     *                 entry.
     * @throws IOException Exceptions thrown by the writer supplied to CSVWriter.
     */
    protected void writeNext(String[] nextLine) throws IOException {
        final Appendable appendable = new StringBuilder(INITIAL_STRING_SIZE);


        if (nextLine == null) {
            return;
        }

        for (int i = 0; i < nextLine.length; i++) {

            if (i != 0) {
                appendable.append(separator);
            }

            String nextElement = nextLine[i];

            if (nextElement == null) {
                continue;
            }

            appendable.append(nextElement);

        }

        appendable.append(lineEnd);
        writer.write(appendable.toString());
    }

    /**
     * Flush underlying stream to writer.
     *
     * @throws IOException If bad things happen
     */
    @Override
    public void flush() throws IOException {
        writer.flush();
    }

    /**
     * Close the underlying stream writer flushing any buffered content.
     *
     * @throws IOException If bad things happen
     */
    @Override
    public void close() throws IOException {
        flush();
        writer.close();
    }
}
