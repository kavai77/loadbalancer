package com.himadri

import kotlinx.serialization.Serializable

@Serializable
data class Config(
    val maxNumberOfProviders: Int,
    val requiredHealthyHeartbeatForEnablingProvider: Int,
    val heartbeatIntervalMs: Long,
    val providerTimeoutMs: Long,
    val maxConcurrentRequestsPerProvider: Int
)
