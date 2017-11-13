package com.microsoft.identity.common.adal.error;

import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@RunWith(AndroidJUnit4.class)
public class AuthenticationExceptionTests {

    @Test
    public void testException() {
        AuthenticationException exception = new AuthenticationException();
        assertNull("Code is null", exception.getCode());
        assertNull("Details is null", exception.getMessage());
        assertNull("Cause is null", exception.getCause());

        exception = new AuthenticationException(ADALError.AUTH_FAILED);
        assertEquals("Code is same", ADALError.AUTH_FAILED, exception.getCode());
        assertNotNull("Details is not null", exception.getMessage());
        assertNull("Cause is null", exception.getCause());

        exception = new AuthenticationException(ADALError.AUTH_FAILED, "Some details");
        assertEquals("Code is same", ADALError.AUTH_FAILED, exception.getCode());
        assertEquals("Details is not null", "Some details", exception.getMessage());
        assertNull("Cause is null", exception.getCause());

        exception = new AuthenticationException(ADALError.AUTH_FAILED, "Some details", new Throwable("test"));
        assertEquals("Code is same", ADALError.AUTH_FAILED, exception.getCode());
        assertEquals("Details is not null", "Some details", exception.getMessage());
        assertNotNull("Cause is not null", exception.getCause());
        assertEquals("Message from Cause is not null", "test", exception.getCause().getMessage());
    }
}

