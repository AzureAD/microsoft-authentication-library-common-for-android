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
package com.microsoft.identity.labapi.utilities.client;

import com.microsoft.identity.labapi.utilities.constants.AltIdSource;
import com.microsoft.identity.labapi.utilities.constants.AltIdType;
import com.microsoft.identity.labapi.utilities.constants.AppType;
import com.microsoft.identity.labapi.utilities.constants.AzureEnvironment;
import com.microsoft.identity.labapi.utilities.constants.B2CProvider;
import com.microsoft.identity.labapi.utilities.constants.FederationProvider;
import com.microsoft.identity.labapi.utilities.constants.GuestHomeAzureEnvironment;
import com.microsoft.identity.labapi.utilities.constants.GuestHomedIn;
import com.microsoft.identity.labapi.utilities.constants.HasAltId;
import com.microsoft.identity.labapi.utilities.constants.HomeDomain;
import com.microsoft.identity.labapi.utilities.constants.HomeUpn;
import com.microsoft.identity.labapi.utilities.constants.IsAdminConsented;
import com.microsoft.identity.labapi.utilities.constants.Mfa;
import com.microsoft.identity.labapi.utilities.constants.PasswordPolicyNotificationDays;
import com.microsoft.identity.labapi.utilities.constants.PasswordPolicyValidityPeriod;
import com.microsoft.identity.labapi.utilities.constants.ProtectionPolicy;
import com.microsoft.identity.labapi.utilities.constants.PublicClient;
import com.microsoft.identity.labapi.utilities.constants.SignInAudience;
import com.microsoft.identity.labapi.utilities.constants.TokenLifetimePolicy;
import com.microsoft.identity.labapi.utilities.constants.TokenType;
import com.microsoft.identity.labapi.utilities.constants.UserRole;
import com.microsoft.identity.labapi.utilities.constants.UserType;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.lang.reflect.Field;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.Accessors;

/**
 * A query used for fetching accounts from the Lab Api.
 */
@Getter
@Accessors(prefix = "m")
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode
public class LabQuery {
    private final UserType mUserType;
    private final UserRole mUserRole;
    private final Mfa mMfa;
    private final ProtectionPolicy mProtectionPolicy;
    private final HomeDomain mHomeDomain;
    private final HomeUpn mHomeUpn;
    private final B2CProvider mB2cProvider;
    private final FederationProvider mFederationProvider;
    private final AzureEnvironment mAzureEnvironment;
    private final GuestHomeAzureEnvironment mGuestHomeAzureEnvironment;
    private final AppType mAppType;
    private final PublicClient mPublicClient;
    private final SignInAudience mSignInAudience;
    private final GuestHomedIn mGuestHomedIn;
    private final HasAltId mHasAltId;
    private final AltIdSource mAltIdSource;
    private final AltIdType mAltIdType;
    private final PasswordPolicyValidityPeriod mPasswordPolicyValidityPeriod;
    private final PasswordPolicyNotificationDays mPasswordPolicyNotificationDays;
    private final TokenLifetimePolicy mTokenLifetimePolicy;
    private final TokenType mTokenType;
    private final TokenLifetimePolicy mTokenLifetime;
    private final IsAdminConsented mIsAdminConsented;

    // Overriding this to only print NonNull values
    // The reason is that we're going to use toString as parameter name in Parameterized Tests
    // If complete String is used then parameter name becomes too long and that means that test name
    // becomes too long and that means that test output file name becomes too long and JUnit
    // cannot handle that.
    @Override
    public String toString() {
        final Object myself = this;
        final ReflectionToStringBuilder toStringBuilder = new ReflectionToStringBuilder(this, ToStringStyle.NO_CLASS_NAME_STYLE) {
            @Override
            protected boolean accept(@NonNull final Field field) {
                try {
                    // print in toString if field Not Null
                    return super.accept(field) && field.get(myself) != null;
                } catch (final IllegalAccessException e) {
                    System.out.println(e);

                    // we caught an error...let's print the field anyway
                    return super.accept(field);
                }
            }
        };

        return toStringBuilder.toString();
    }
}
