package com.microsoft.identity.common.internal.cache.registry;

import com.microsoft.identity.common.internal.cache.ISimpleCache;

public interface IBrokerApplicationRegistry extends ISimpleCache<BrokerApplicationRegistryData> {

    BrokerApplicationRegistryData getMetadata(String client, String environment, int processUid);
}
