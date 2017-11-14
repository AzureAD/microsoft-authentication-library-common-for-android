package com.microsoft.identity.common.adal.error;


import com.microsoft.identity.common.adal.internal.AuthenticationConstants;
import com.microsoft.identity.common.adal.internal.AuthenticationSettings;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.res.Configuration;
import android.os.Build;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Locale;

import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class ADALErrorTest {

    private static final String TAG = "ADALErrorTests";

    @Before
    @SuppressLint("PackageManagerGetSignatures")
    public void setUp() throws Exception {
        System.setProperty("dexmaker.dexcache", getInstrumentation().getTargetContext().getCacheDir().getPath());

        AuthenticationSettings.INSTANCE
                .setBrokerPackageName(AuthenticationConstants.Broker.COMPANY_PORTAL_APP_PACKAGE_NAME);
        Log.d(TAG, "mTestSignature is set");
    }

    @Test
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    public void testResourceOverwrite() {
        ADALError err = ADALError.DEVELOPER_AUTHORITY_CAN_NOT_BE_VALIDED;
        String msg = err.getDescription();
        Log.v(TAG, "Test context packagename:" + getInstrumentation().getTargetContext().getPackageName());
        Locale locale2 = new Locale("de");
        Locale.setDefault(locale2);
        Configuration config = new Configuration();
        config.setLocale(locale2);
        getInstrumentation().getContext().getResources().updateConfiguration(config,
                getInstrumentation().getContext().getResources().getDisplayMetrics());
        String localizedMsg = err.getLocalizedDescription(getInstrumentation().getContext());

        assertFalse("Error description is different in resource", msg.equalsIgnoreCase(localizedMsg));

        Locale localefr = new Locale("fr");
        Locale.setDefault(localefr);
        config.setLocale(localefr);
        getInstrumentation().getContext().getResources().updateConfiguration(config,
                getInstrumentation().getContext().getResources().getDisplayMetrics());
        localizedMsg = err.getLocalizedDescription(getInstrumentation().getContext());

        assertFalse("Same as english", msg.equalsIgnoreCase(localizedMsg));
        assertTrue("in default", localizedMsg.contains("Authority validation returned an error"));
    }
}
