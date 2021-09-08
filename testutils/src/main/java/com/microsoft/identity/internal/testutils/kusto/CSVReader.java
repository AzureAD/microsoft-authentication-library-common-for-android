//  Copyright (c) Microsoft Corporation.
//  All rights reserved.
//
//  This code is licensed under the MIT License.
//
//  Permission is hereby granted, free of charge, to any person obtaining a copy
//  of this software and associated documentation files(the "Software"), to deal
//  in the Software without restriction, including without limitation the rights
//  to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
//  copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions :
//
//  The above copyright notice and this permission notice shall be included in
//  all copies or substantial portions of the Software.
//
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//  THE SOFTWARE.
package com.microsoft.identity.internal.testutils.kusto;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.microsoft.identity.internal.testutils.kusto.CSVWriter.DEFAULT_SEPARATOR;


/**
 * A simple CSV file reader that takes care of cases when the CSV column data contains the separator.
 * E.g.
 * field1_value,field2_value,"field 3,value",field4
 * <p>
 * In this case "field 3, value" should be considered as a single column value.
 */
public class CSVReader {


    protected final Reader reader;
    protected final char separator;

    // Defines the regex for finding the CSV columns while taking care of the case where the
    // separator is part of the column data. In the REGEX below, the semi-colon (;)
    // is considered as the separator.
    public static final String REGEX = "(?<=^|;)\".*?\"(?=;|$)|(?<=^|;)[^;]*(?=;|$)";

    public CSVReader(@NonNull final Reader reader) {
        this(reader, DEFAULT_SEPARATOR);
    }

    public CSVReader(@NonNull final Reader reader, final char separator) {
        this.reader = reader;
        this.separator = separator;
    }

    /**
     * Reads a CSV file and returns it as a list of list of strings.
     *
     * @return a List containing the lines of the CSV file. Each line is represented as a list of strings
     * @throws IOException when a problem occurred closing the reader
     */
    public List<List<String>> read() throws IOException {
        final List<List<String>> content = new ArrayList<>();
        Scanner scanner = new Scanner(this.reader);

        while (scanner.hasNextLine()) {
            content.add(this.readLine(scanner.nextLine()));
        }

        scanner.close();
        reader.close();

        return content;
    }

    private List<String> readLine(final String line) {
        final List<String> content = new ArrayList<>();

        final Pattern pattern = Pattern.compile(REGEX.replaceAll(";", String.valueOf(this.separator)));

        final Matcher matcher = pattern.matcher(line);

        while (matcher.find()) {
            content.add(matcher.group());
        }

        return content;
    }

}
