package com.microsoft.identity.common.internal.ui.webview.certbasedauth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.Activity;

import androidx.annotation.Nullable;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

public class AbstractCertBasedAuthTest {
    protected Activity mActivity;
    protected TestDialogHolder mDialogHolder;

    @Before
    public void setUp() {
        mActivity = Robolectric.buildActivity(Activity.class).get();
        mDialogHolder = new TestDialogHolder();
    }

    protected void checkIfCorrectDialogIsShowing(@Nullable final TestDialog expectedDialog) {
        if (expectedDialog == null) {
            assertFalse(mDialogHolder.isDialogShowing());
            return;
        }
        assertNotNull(mDialogHolder.getMCurrentDialog());
        assertTrue(mDialogHolder.isDialogShowing());
        assertEquals(expectedDialog, mDialogHolder.getMCurrentDialog());
    }
}
