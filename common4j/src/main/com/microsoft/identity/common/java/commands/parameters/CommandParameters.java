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
package com.microsoft.identity.common.java.commands.parameters;

import com.google.gson.annotations.Expose;
import com.microsoft.identity.common.java.WarningType;
import com.microsoft.identity.common.java.interfaces.IPlatformComponents;
import com.microsoft.identity.common.java.opentelemetry.SerializableSpanContext;
import com.microsoft.identity.common.java.providers.oauth2.OAuth2TokenCache;
import com.microsoft.identity.common.java.request.SdkType;

import java.util.Collections;
import java.util.Map;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

// Suppressing rawtype warnings due to the generic type OAuth2TokenCache
@SuppressWarnings(WarningType.rawtype_warning)
@Getter
@EqualsAndHashCode
@SuperBuilder(toBuilder = true)
public class CommandParameters {

    public static final String APPLICATION_IDENTIFIER_FORMAT = "%s/%s";

    @NonNull
    @EqualsAndHashCode.Exclude
    private transient IPlatformComponents platformComponents;

    @EqualsAndHashCode.Exclude
    private transient OAuth2TokenCache oAuth2TokenCache;

    @EqualsAndHashCode.Exclude
    private transient boolean isSharedDevice;

    @Expose()
    private String applicationName;

    @Expose()
    private String applicationVersion;

    @Expose()
    private String requiredBrokerProtocolVersion;

    @Expose()
    private SdkType sdkType;

    @Expose()
    private String sdkVersion;

    @Expose()
    private String clientId;

    @Expose()
    private String redirectUri;

    @Expose()
    private boolean powerOptCheckEnabled;

    @Expose()
    private String callerPackageName;

    @Expose()
    private String callerSignature;

    @Builder.Default
    private transient Map<String, String> flightInformation = Collections.emptyMap();

    @Setter
    @EqualsAndHashCode.Exclude
    @Expose()
    private String correlationId;

    @Expose()
    private SerializableSpanContext spanContext;
    
    //Overriding what Lombok would otherwise generate for me
    public String getApplicationIdentifier(){
        return String.format(APPLICATION_IDENTIFIER_FORMAT, this.callerPackageName, this.callerSignature);
    }
}


