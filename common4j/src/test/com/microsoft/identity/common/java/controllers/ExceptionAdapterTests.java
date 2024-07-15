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

package com.microsoft.identity.common.java.controllers;

import com.microsoft.identity.common.java.exception.BaseException;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.exception.TerminalException;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.concurrent.TimeoutException;

@RunWith(JUnit4.class)
public class ExceptionAdapterTests {

    @Test
    public void testBaseExceptionFromException_TerminalException() throws Exception{
        TerminalException t = new TerminalException("errorMsg", ClientException.KEY_RING_WRITE_FAILURE);
        BaseException e = ExceptionAdapter.baseExceptionFromException(t);
        Assert.assertEquals(e.getErrorCode(), t.getErrorCode());
        Assert.assertEquals(e.getCause(), t);
    }

    @Test
    public void testClientExceptionFromException_TimeoutException() {
        final TimeoutException t = new TimeoutException();
        Assert.assertEquals(ClientException.TIMED_OUT, ExceptionAdapter.clientExceptionFromException(t).getErrorCode());
    }
}