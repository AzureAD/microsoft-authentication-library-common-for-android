package com.microsoft.identity.common;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import com.microsoft.identity.common.internal.cache.MsalOtherCache;
import com.microsoft.identity.common.java.exception.ClientException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class MsalOtherCacheTest {
    private MsalOtherCache msalOtherCache;
    private Context mContext;

    @Before
    public void setUp() throws Exception {
        // Context and related init
        mContext = ApplicationProvider.getApplicationContext();
        msalOtherCache = new MsalOtherCache(mContext);
    }

    @After
    public void tearDown() {
        msalOtherCache.clearCache();
    }

    @Test
    public void saveAuthorityValidationMetadataSuccess() throws ClientException {
        String environment = "login.microsoftonline.com";
        String metadata = "{metadata}";
        String ReadedMetadata = msalOtherCache.getAuthorityValidationMetadata(environment);
        Assert.assertEquals(ReadedMetadata, null);
        msalOtherCache.saveAuthorityValidationMetadata(environment, metadata);
        ReadedMetadata = msalOtherCache.getAuthorityValidationMetadata(environment);
        Assert.assertEquals(ReadedMetadata, metadata);
    }
}
