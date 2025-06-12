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
package com.microsoft.identity.common.java.util;

import com.microsoft.identity.common.java.controllers.ExceptionAdapter;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.logging.Logger;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import edu.umd.cs.findbugs.annotations.Nullable;
import lombok.NonNull;

import static com.microsoft.identity.common.java.exception.ClientException.IO_ERROR;

/**
 * Utility class for reading/writing file in Android.
 */
public class FileUtil {
    private static final String TAG = FileUtil.class.getSimpleName();

    private FileUtil() {
    }

    /**
     * Write a data blob to a file.
     *
     * @param data a data blob to be written.
     * @param file file to write to.
     */
    public static void writeDataToFile(@NonNull final byte[] data,
                                       @NonNull final File file) throws ClientException {
        final String methodName = ":writeKeyData";

        try {
            final OutputStream out = new FileOutputStream(file);
            try {
                out.write(data);
            } finally {
                out.close();
            }
        } catch (IOException e) {
            final ClientException clientException = new ClientException(
                    IO_ERROR,
                    e.getMessage(),
                    e
            );

            Logger.error(
                    TAG + methodName,
                    clientException.getErrorCode(),
                    e
            );

            throw clientException;
        }
    }

    /**
     * Read a data blob from a file.
     *
     * @param file     file to load.
     * @param dataSize expected size of the resulted data blob.
     * @return A data blob, if exists.
     */
    @Nullable
    public static byte[] readFromFile(@NonNull final File file,
                                      final int dataSize) throws ClientException {
        final String methodName = ":readKeyData";

        if (!file.exists()) {
            return null;
        }

        try {
            final InputStream in = new FileInputStream(file);

            try {
                final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                final byte[] buffer = new byte[dataSize];
                int count;
                while ((count = in.read(buffer)) != -1) {
                    bytes.write(buffer, 0, count);
                }
                return bytes.toByteArray();
            } finally {
                in.close();
            }
        } catch (IOException e) {
            final ClientException clientException = new ClientException(
                    IO_ERROR,
                    e.getMessage(),
                    e
            );

            Logger.error(
                    TAG + methodName,
                    clientException.getErrorCode(),
                    e
            );

            throw clientException;
        }
    }

    /**
     * Delete the wrapped key file (if exists).
     *
     * @param file file to delete.
     */
    public static void deleteFile(@NonNull final File file) {
        final String methodName = ":deleteKeyFile";

        if (file.exists()) {
            Logger.verbose(TAG + methodName, "Delete File");
            if (!file.delete()) {
                Logger.verbose(TAG + methodName, "Failed to delete file.");
            }
        }
    }

    /**
     * Read a string from a file.
     *
     * @param file the file to read from.
     * @return the content of the file as a String, or null if an error occurs.
     */
    public static String readStringFromFile(File file) {
        final String methodTag = TAG + ":readStringFromFile";

        try (FileInputStream fis = new FileInputStream(file);
             final ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            byte[] buffer = new byte[1024];
            int length;
            while ((length = fis.read(buffer)) != -1) {
                baos.write(buffer, 0, length);
            }

            return baos.toString("UTF-8");
        } catch (IOException e) {
            Logger.error(methodTag, e.getMessage(), e);
            return null; // or handle the exception as needed
        }
    }

    /**
     * Write a string to a file.
     *
     * @param content the string content to write.
     * @param file    the file to write to.
     * @throws ClientException if an error occurs during writing.
     */
    public static void writeStringToFile(String content, File file) throws ClientException {
        final String methodTag = TAG + ":writeStringToFile";

        try (FileOutputStream fos = new FileOutputStream(file)) {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            baos.write(content.getBytes(StandardCharsets.UTF_8));
            baos.writeTo(fos);
        } catch (final IOException e) {
            Logger.error(methodTag, e.getMessage(), e);
            throw ExceptionAdapter.clientExceptionFromException(e);
        }
    }
}
