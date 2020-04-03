package com.microsoft.identity.common.internal.commands.parameters;

import android.accounts.Account;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder(toBuilder = true)
public class BrokerSilentTokenCommandParameters extends SilentTokenCommandParameters {

    private String callerPackageName;
    private int callerUid;
    private String callerAppVersion;
    private String brokerVersion;

    private Account accountManagerAccount;
    private String homeAccountId;
    private String localAccountId;
    private int sleepTimeBeforePrtAcquisition;
    private String loginHint;
}
