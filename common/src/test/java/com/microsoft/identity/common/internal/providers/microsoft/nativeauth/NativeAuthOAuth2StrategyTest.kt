package com.microsoft.identity.common.internal.providers.microsoft.nativeauth

import android.os.Build
import org.junit.Before
import org.robolectric.annotation.Config

@Config(sdk = [Build.VERSION_CODES.O_MR1])
class NativeAuthOAuth2StrategyTest {

    @Before
    fun setup() {
    }
}
