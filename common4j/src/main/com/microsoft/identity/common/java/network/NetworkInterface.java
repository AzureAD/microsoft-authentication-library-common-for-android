package com.microsoft.identity.common.java.network;

/**
 * Defines the different network interfaces we can switch to during a test run.
 * The value of the interface holds bit information regarding whether WIFI and CELLULAR are active.
 * <p>
 * 0 - 0,0 [Both WIFI and CELLULAR are OFF]
 * 1 - 0,1 [WIFI is turned ON]
 * 2 - 1,0 [CELLULAR is turned ON]
 * 3 - 1,1 [Both WIFI and CELLULAR are ON]
 */
public enum NetworkInterface {
    NONE("NONE", 0),
    WIFI("WIFI", 1),
    CELLULAR("CELLULAR", 2),
    WIFI_AND_CELLULAR("WIFI_AND_CELLULAR", 3);


    private final String key;
    private final byte value;

    NetworkInterface(String key, int value) {
        this.key = key;
        this.value = (byte) value;
    }

    public String getKey() {
        return key;
    }

    /**
     * WIFI active state is stored in the first bit.
     *
     * @return a boolean representing whether WIFI is enabled in this InterfaceType
     */
    public boolean wifiActive() {
        return ((value) & 1) == 1;
    }

    /**
     * CELLULAR active state is stored in the second bit
     *
     * @return a boolean representing whether CELLULAR is enabled in this InterfaceType
     */
    public boolean cellularActive() {
        return ((value >> 1) & 1) == 1;
    }

    @Override
    public String toString() {
        return String.valueOf(key);
    }


    /**
     * Set the interfaceType from a string representation of {@link NetworkInterface}
     *
     * @param value a string representation of {@link NetworkInterface}
     */
    public static NetworkInterface fromValue(final String value) {
        for (NetworkInterface interfaceType : values()) {
            if (interfaceType.getKey().equals(value)) {
                return interfaceType;
            }
        }
        return null;
    }
}
