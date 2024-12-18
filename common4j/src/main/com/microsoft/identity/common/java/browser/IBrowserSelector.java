package com.microsoft.identity.common.java.browser;

import com.microsoft.identity.common.java.ui.BrowserDescriptor;

import java.util.List;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;

/**
 * Interface for selecting a browser.
 */
public interface IBrowserSelector {

    /**
     * Selects a valid installed browser from the list of safe browsers.
     * If no browser is present in the list of safe browser, null is returned.
     *
     * @param browserSafeList             The list of browsers to choose from.
     * @param preferredBrowserDescriptor  The preferred browser descriptor.
     * @return The selected browser.
     */
    @Nullable
    Browser select(
            @NonNull List<BrowserDescriptor> browserSafeList,
            @Nullable BrowserDescriptor preferredBrowserDescriptor);
}
