package com.microsoft.identity.internal.testutils.shadows;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.microsoft.identity.common.internal.broker.BrokerValidator;

import org.robolectric.annotation.Implements;

@Implements(BrokerValidator.class)
public class ShadowBrokerValidator {

    public static boolean isValidBrokerRedirect(@Nullable final String redirectUri,
                                                @NonNull final Context context,
                                                @NonNull final String packageName) {
        return true;
    }

}
