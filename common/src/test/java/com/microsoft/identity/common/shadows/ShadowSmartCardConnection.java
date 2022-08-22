package com.microsoft.identity.common.shadows;

import com.yubico.yubikit.core.smartcard.SmartCardConnection;

import org.robolectric.annotation.Implements;

@Implements(SmartCardConnection.class)
public class ShadowSmartCardConnection {
}
