package com.microsoft.identity.common.adal.internal

enum class BatteryOptimizationStatus {
    OptOut,
    NotOptOut,
    CannotRetrievePowerManager,
    NullPointerException,
    SecurityException,
    IllegalArgumentException,
    UnknownException
}