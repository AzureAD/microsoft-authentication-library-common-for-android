package com.microsoft.identity.common.internal.providers.azureactivedirectory;

import android.content.Context;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by shoatman on 11/17/2017.
 */

public class AzureActiveDirectoryInstanceClient {

    private static final String TAG = "Discovery";

    private static final String API_VERSION_KEY = "api-version";

    private static final String API_VERSION_VALUE = "1.1";

    private static final String AUTHORIZATION_ENDPOINT_KEY = "authorization_endpoint";

    private static final String INSTANCE_DISCOVERY_SUFFIX = "common/discovery/instance";

    private static final String AUTHORIZATION_COMMON_ENDPOINT = "/common/oauth2/authorize";

    /**
     * {@link ReentrantLock} for making sure there is only one instance discovery request sent out at a time.
     */
    private static volatile ReentrantLock sInstanceDiscoveryNetworkRequestLock;

    /**
     * Sync set of valid hosts to skip query to server if host was verified
     * before.
     */
    private static final Set<String> AAD_WHITELISTED_HOSTS = Collections
            .synchronizedSet(new HashSet<String>());

    /**
     * Sync map of validated AD FS authorities and domains. Skips query to server
     * if already verified
     */
    private static final Map<String, Set<URI>> ADFS_VALIDATED_AUTHORITIES =
            Collections.synchronizedMap(new HashMap<String, Set<URI>>());

    /**
     * Discovery query will go to the prod only for now.
     */
    private static final String TRUSTED_QUERY_INSTANCE = "login.microsoftonline.com";

    private UUID mCorrelationId;

    private Context mContext;

    /**
     * interface to use in testing.
     */
    //private final IWebRequestHandler mWebrequestHandler;
}
