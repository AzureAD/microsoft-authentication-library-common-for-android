package com.microsoft.identity.common.java.network;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class NetworkState {
    private long delay;
    private NetworkInterface networkInterface;
    private long duration;
}
