package com.microsoft.identity.client.ui.automation.annotations;

import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Retention(value = RUNTIME)
public @interface RetryOnFailure {

    int retryCount() default 1;
}
