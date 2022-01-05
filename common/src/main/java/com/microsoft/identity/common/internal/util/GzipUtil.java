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

package com.microsoft.identity.common.internal.util;

import androidx.annotation.NonNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class GzipUtil {

    /**
     * Util method which compress the input String to bytes using gzip compression.
     */
    public static byte[] compressString(@NonNull final String inputString) throws IOException {
        byte[] bytes = inputString.getBytes("UTF-8");
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        GZIPOutputStream gzipOutputStream = new GZIPOutputStream(byteArrayOutputStream);
        gzipOutputStream.write(bytes, 0, bytes.length);
        gzipOutputStream.flush();
        gzipOutputStream.close();
        byte[] result = byteArrayOutputStream.toByteArray();
        byteArrayOutputStream.close();
        return result;
    }

    /**
     * Util method which converts the gzip compressed bytes to  String.
     */
    public static String decompressBytesToString(@NonNull final byte[] compressedBytes)
            throws IOException {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(compressedBytes);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        GZIPInputStream gzipInputStream = new GZIPInputStream(byteArrayInputStream);
        byte[] tempBuffer = new byte[256];
        while (true) {
            int bytesRead = gzipInputStream.read(tempBuffer);
            if (bytesRead < 0) {
                break;
            }
            byteArrayOutputStream.write(tempBuffer, 0, bytesRead);
        }
        gzipInputStream.close();

        byte[] deCompressedBytes = byteArrayOutputStream.toByteArray();
        byteArrayInputStream.close();
        return new String(deCompressedBytes, 0, deCompressedBytes.length, "UTF-8");
    }
}
