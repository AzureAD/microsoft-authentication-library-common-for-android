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
package com.microsoft.identity.common.internal.ui.browser;

import androidx.annotation.NonNull;

import com.microsoft.identity.common.logging.Logger;

import java.util.Arrays;
import java.util.List;

/**
 * A blocked list of browsers. This will reject a match for any browser on the list, and permit
 * all others.
 */
public class BrowserBlocklist {
    private static final String TAG = BrowserBlocklist.class.getSimpleName();
    private List<Browser> mBrowsers;

    /**
     * Create a block list from given set of browsers.
     */
    public BrowserBlocklist(Browser... browsers) {
        mBrowsers = Arrays.asList(browsers);
    }

    /**
     * @return true if the browser is in the block list.
     */
    public boolean matches(@NonNull Browser targetBrowser) {
        final String methodTag = TAG + ":matches";
        for (Browser browser : mBrowsers) {
            if (browser.equals(targetBrowser)) {
                Logger.verbose(methodTag, "The target browser is in the block list.");
                return true;
            }
        }

        return false;
    }
}