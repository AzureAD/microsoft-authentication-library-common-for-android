package com.microsoft.identity.common.adal.internal.util;

import com.microsoft.identity.common.adal.internal.AndroidTestHelper;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * StringExtensions class has helper methods and it is not public
 */
@RunWith(AndroidJUnit4.class)
public class StringExtensionTests extends AndroidTestHelper {
    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    public void testIsNullOrBlankNotEmpty() throws IllegalArgumentException,
            IllegalAccessException, InvocationTargetException, ClassNotFoundException,
            NoSuchMethodException, InstantiationException {
        assertFalse("not empty", StringExtensions.isNullOrBlank("non-Empty"));
    }

    @Test
    public void testIsNullOrBlankEmpty() throws IllegalArgumentException, IllegalAccessException,
            InvocationTargetException, ClassNotFoundException, NoSuchMethodException,
            InstantiationException {
        assertTrue("empty", StringExtensions.isNullOrBlank(""));

        assertTrue("empty", StringExtensions.isNullOrBlank("          "));
    }

    @Test
    public void testURLFormEncodeDecode() {

        try {
            assertEquals("https%3A%2F%2Flogin.windows.net%2Faaltests.onmicrosoft.com%2F",
                    StringExtensions.urlFormEncode("https://login.windows.net/aaltests.onmicrosoft.com/"));

            assertEquals("https://login.windows.net/aaltests.onmicrosoft.com/",
                    StringExtensions.urlFormDecode("https%3A%2F%2Flogin.windows.net%2Faaltests.onmicrosoft.com%2F"));

            assertEquals("abc+d1234567890-",
                    StringExtensions.urlFormEncode("abc d1234567890-"));

            assertEquals("abc d1234567890-",
                    StringExtensions.urlFormDecode("abc+d1234567890-"));

            String longString = "asdfk+j0a-=skjwe43;1l234 1#$!$#%345903485qrq@#$!@#$!(rekr341!#$%Ekfaآزمايشsdsdfsddfdgsfgjsglk==CVADS";
            String result = StringExtensions.urlFormEncode(longString);

            String decodeResult = StringExtensions.urlFormDecode(result);
            assertEquals(longString, decodeResult);

        } catch (UnsupportedEncodingException ueex) {
            Log.e(getClass().getName(), ueex.getMessage());
            Assert.fail("Did not expect exception");
        }
    }

    @Test
    public void testGetStringTokens() {
        assertEquals(0, StringExtensions.getStringTokens("", ";").size());
        assertEquals(0, StringExtensions.getStringTokens(";;;;", ";").size());

        List<String> tokens = StringExtensions.getStringTokens("one;;two", ";");
        assertEquals(2, tokens.size());
        assertEquals("one", tokens.get(0));
        assertEquals("two", tokens.get(1));

        tokens = StringExtensions.getStringTokens(";;one;;two;;;", ";");
        assertEquals(2, tokens.size());
        assertEquals("one", tokens.get(0));
        assertEquals("two", tokens.get(1));
    }

    @Test(expected = NullPointerException.class)
    public void testGetStringTokensWithNullItems() {
        StringExtensions.getStringTokens(null, ";");
    }

    @Test(expected = NullPointerException.class)
    public void testGetStringTokensWithNullDelimiters() {
        StringExtensions.getStringTokens("", null);
    }
}

