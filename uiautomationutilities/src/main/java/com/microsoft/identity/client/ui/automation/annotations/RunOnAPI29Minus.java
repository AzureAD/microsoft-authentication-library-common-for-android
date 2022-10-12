package com.microsoft.identity.client.ui.automation.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation indicating that an automated End-to-End test case should be ran on a test device with API 29 or lower.
 * Tests not marked with this annotation are assumed to be compatible with API 30+
 * Typical reasons for wanting to run on older API:
 *      - Pages that have elements with height 0 (these aren't visible to automation in API 30+) i.e. Keep me signed in page, consent page
 *      - Some apps not behaving correctly on API 30+ i.e. Azure Sample app
 *      - WebView seems to be easier to test on API 29-.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface RunOnAPI29Minus {
    String value() default "";
}
