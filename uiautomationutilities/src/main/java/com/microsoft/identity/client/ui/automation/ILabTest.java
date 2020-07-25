package com.microsoft.identity.client.ui.automation;

import com.microsoft.identity.internal.testutils.labutils.LabUserQuery;

/**
 * An interface describing a test that can leverage the Lab Api to fetch accounts
 */
public interface ILabTest {

    /**
     * Get the query that can be used to pull a user from the LAB API
     *
     * @return A {@link LabUserQuery} object that can be used to pull user via LAB API
     */
    LabUserQuery getLabUserQuery();

    /**
     * Get the type of temp user that can be used to create a new temp user via LAB API
     *
     * @return The type of temp user as denoted in {@link com.microsoft.identity.internal.testutils.labutils.LabConstants.TempUserType}
     */
    String getTempUserType();

}
