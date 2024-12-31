package com.microsoft.identity.common.java.providers.microsoft.azureactivedirectory;

public class AzureActiveDirectoryEnvironment {
    public static final String PRODUCTION_CLOUD_URL = "https://login.microsoftonline.com"; //Prod
    public static final String PREPRODUCTION_CLOUD_URL = "https://login.windows-ppe.net"; //PPE

    public static final String ONEBOX_AUTHORITY = "zurich.test.dnsdemo1.test:8478";
    public static final String ONEBOX_CLOUD_URL = "https://" + ONEBOX_AUTHORITY; // Local ESTS Deployment
}
