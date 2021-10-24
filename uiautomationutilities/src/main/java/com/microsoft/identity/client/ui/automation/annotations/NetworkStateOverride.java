package com.microsoft.identity.client.ui.automation.annotations;

import com.microsoft.identity.common.java.network.NetworkInterface;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@Repeatable(value = NetworkOverrides.class)
public @interface NetworkStateOverride {
    String marker();

    long delay() default 0;

    NetworkInterface networkInterface();

    long duration() default 0;
}
