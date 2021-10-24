package com.microsoft.identity.common.java.network;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NetworkState {
    private NetworkInterface networkInterface;
    private long duration;
}
