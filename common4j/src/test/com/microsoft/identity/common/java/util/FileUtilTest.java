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

import com.microsoft.identity.common.java.exception.ClientException;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;

public class FileUtilTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void createTempFile_returnsFileInGivenDirectory() throws ClientException, IOException {
        final File directory = temporaryFolder.newFolder("testDir");
        final File tempFile = FileUtil.createTempFile("ms_auth_", ".jpg", directory);

        Assert.assertNotNull(tempFile);
        Assert.assertTrue(tempFile.exists());
        Assert.assertEquals(directory.getCanonicalPath(), tempFile.getParentFile().getCanonicalPath());
    }

    @Test
    public void createTempFile_fileNameStartsWithPrefix() throws ClientException, IOException {
        final File directory = temporaryFolder.newFolder("testDir");
        final File tempFile = FileUtil.createTempFile("ms_auth_", ".jpg", directory);

        Assert.assertTrue(tempFile.getName().startsWith("ms_auth_"));
    }

    @Test
    public void createTempFile_fileNameEndsWithSuffix() throws ClientException, IOException {
        final File directory = temporaryFolder.newFolder("testDir");
        final File tempFile = FileUtil.createTempFile("ms_auth_", ".jpg", directory);

        Assert.assertTrue(tempFile.getName().endsWith(".jpg"));
    }

    @Test
    public void createTempFile_withNullSuffix_usesTmpSuffix() throws ClientException, IOException {
        final File directory = temporaryFolder.newFolder("testDir");
        final File tempFile = FileUtil.createTempFile("ms_auth_", null, directory);

        Assert.assertNotNull(tempFile);
        Assert.assertTrue(tempFile.exists());
        Assert.assertTrue(tempFile.getName().endsWith(".tmp"));
    }

    @Test(expected = ClientException.class)
    public void createTempFile_withNonExistentDirectory_throwsClientException() throws ClientException {
        final File nonExistentDir = new File("/nonexistent/path/that/does/not/exist");
        FileUtil.createTempFile("ms_auth_", ".jpg", nonExistentDir);
    }

    @Test
    public void createTempFile_multipleCalls_returnUniqueFiles() throws ClientException, IOException {
        final File directory = temporaryFolder.newFolder("testDir");
        final File tempFile1 = FileUtil.createTempFile("ms_auth_", ".jpg", directory);
        final File tempFile2 = FileUtil.createTempFile("ms_auth_", ".jpg", directory);

        Assert.assertNotEquals(tempFile1.getName(), tempFile2.getName());
    }
}
