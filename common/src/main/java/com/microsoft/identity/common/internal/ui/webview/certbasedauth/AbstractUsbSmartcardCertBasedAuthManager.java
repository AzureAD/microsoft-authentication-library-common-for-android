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
package com.microsoft.identity.common.internal.ui.webview.certbasedauth;

import androidx.annotation.Nullable;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * An abstract smartcard CertBasedAuth manager specifically for USB.
 */
public abstract class AbstractUsbSmartcardCertBasedAuthManager extends AbstractSmartcardCertBasedAuthManager {
    @Accessors(prefix = "m")
    protected IDisconnectionCallback mDisconnectionCallback;

    //Helps with deciding whether or not we want to show the user a prompt to unplug at the end of the CBA flow.
    @Getter @Accessors(prefix = "m")
    protected boolean mUsbDeviceInitiallyPluggedIn;

    /**
     * Sets callback to be run for when a smartcard connection is ended.
     * @param callback an implementation of IDisconnectionCallback.
     */
    public void setDisconnectionCallback(@Nullable final IDisconnectionCallback callback) {
        mDisconnectionCallback = callback;
    }

    /**
     * Sets disconnection callback to null.
     */
    public void clearDisconnectionCallback() {
        mDisconnectionCallback = null;
    }
}
