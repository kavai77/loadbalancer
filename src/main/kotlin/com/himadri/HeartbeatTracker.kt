package com.himadri

import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.logging.Logger

class HeartbeatTracker(
    private val loadBalancer: LoadBalancer,
    private val config: Config
) {
    private val healthyHeartbeatCounter: ConcurrentMap<Provider, AtomicInteger> = ConcurrentHashMap()
    private val logger = Logger.getLogger(HeartbeatTracker::class.qualifiedName)

    fun scheduleHeartbeat() {
        GlobalScope.launch {
            while (isActive) {
                loadBalancer.providersIterator().forEach {
                    launch { checkProviderHeartbeat(it) }
                }
                delay(config.heartbeatIntervalMs)
            }
        }
    }

    suspend fun checkProviderHeartbeat(provider: Provider) {
        if (isProviderHealthy(provider)) {
            registerHealthyHeartbeat(provider)
        } else {
            registerFailedHeartbeat(provider)
        }
    }

    private suspend fun isProviderHealthy(provider: Provider): Boolean {
        try {
            withTimeout(config.providerTimeoutMs) { provider.check() }
            return true
        } catch (ex: Exception) {
            return false
        }
    }

    private fun registerHealthyHeartbeat(provider: Provider) {
        if (loadBalancer.isProviderExcluded(provider)) {
            val healthyHeartbeats = healthyHeartbeatCounter.getOrPut(provider) { AtomicInteger(0) }
            if (healthyHeartbeats.incrementAndGet() >= config.requiredHealthyHeartbeatForEnablingProvider) {
                loadBalancer.includeProvider(provider)
                healthyHeartbeatCounter.remove(provider)
                logger.info("${provider.get()} healthy again")
            }
        }
    }

    private fun registerFailedHeartbeat(provider: Provider) {
        loadBalancer.excludeProvider(provider)
        healthyHeartbeatCounter[provider] = AtomicInteger(0)
        logger.info("${provider.get()} is unhealthy")
    }
}