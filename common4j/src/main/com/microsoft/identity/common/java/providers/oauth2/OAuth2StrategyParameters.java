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
package com.microsoft.identity.common.java.providers.oauth2;

import com.microsoft.identity.common.java.authscheme.AbstractAuthenticationScheme;
import com.microsoft.identity.common.java.interfaces.IPlatformComponents;

import edu.umd.cs.findbugs.annotations.Nullable;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.Accessors;

/**
 * Base class for defining options relative to the construction of an {@link OAuth2Strategy}.
 */
@Builder
@Getter
@Accessors(prefix = "m")
public class OAuth2StrategyParameters {
    @Nullable
    private final transient IPlatformComponents mPlatformComponents;

    @Nullable
    private final transient AbstractAuthenticationScheme mAuthenticationScheme;

    // TODO: Consider moving this field into MicrosoftStsOAuth2Configuration and updating it's endpoint methods
    //  to use OpenId Configuration.
    private transient boolean mUsingOpenIdConfiguration;

    public void setUsingOpenIdConfiguration(final boolean isUsingOpenIdConfiguration){
        mUsingOpenIdConfiguration = isUsingOpenIdConfiguration;
    }
}
