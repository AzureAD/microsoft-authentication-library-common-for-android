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
package com.microsoft.identity.common.internal.util;

import android.content.Context;

import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.logging.Logger;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

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
     * @param data             a data blob to be written.
     * @param context          a {@link Context} object.
     * @param relativeFilePath a relative path (under the calling app's directory) to a file to write to.
     */
    public static void writeDataToFile(@NonNull final byte[] data,
                                       @NonNull final Context context,
                                       @NonNull final String relativeFilePath) throws ClientException {
        final String methodName = ":writeKeyData";

        Logger.verbose(TAG + methodName, "Writing data to a file");

        final Exception exception;
        final String errCode;
        try {
            final File keyFile = new File(
                    context.getDir(context.getPackageName(), Context.MODE_PRIVATE),
                    relativeFilePath);

            final OutputStream out = new FileOutputStream(keyFile);
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
     * @param context          a {@link Context} object.
     * @param relativeFilePath a relative path (under the calling app's directory) to a file to read from.
     * @param dataSize         expected size of the resulted data blob.
     * @return A data blob, if exists.
     */
    @Nullable
    public static byte[] readFromFile(@NonNull final Context context,
                                      @NonNull final String relativeFilePath,
                                      @NonNull final int dataSize) throws ClientException {
        final String methodName = ":readKeyData";

        final File keyFile = new File(
                context.getDir(context.getPackageName(), Context.MODE_PRIVATE),
                relativeFilePath);

        if (!keyFile.exists()) {
            return null;
        }

        final Exception exception;
        final String errCode;

        try {
            Logger.verbose(TAG + methodName, "Reading data from a file");
            final InputStream in = new FileInputStream(keyFile);

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
     * @param context          a {@link Context} object.
     * @param relativeFilePath a relative path (under the calling app's directory) to delete.
     */
    public static void deleteFile(@NonNull final Context context,
                                  @NonNull final String relativeFilePath) {
        final String methodName = ":deleteKeyFile";

        final File keyFile = new File(
                context.getDir(context.getPackageName(),
                        Context.MODE_PRIVATE), relativeFilePath);
        if (keyFile.exists()) {
            Logger.verbose(TAG + methodName, "Delete File");
            if (!keyFile.delete()) {
                Logger.verbose(TAG + methodName, "Failed to delete file.");
            }
        }
    }

}
