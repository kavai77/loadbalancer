package com.himadri

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

interface LoadBalancer {
    fun registerProvider(provider: Provider)
    fun includeProvider(provider: Provider)
    fun excludeProvider(provider: Provider)
    fun isProviderExcluded(provider: Provider): Boolean
    fun providersIterator(): Iterator<Provider>
    fun setLoadBalancerStrategy(loadBalancingStrategy: LoadBalancingStrategy)
    fun get(): String
}

class LoadBalancerImpl(
    private val config: Config
) : LoadBalancer {
    private val providers: MutableList<Provider> = mutableListOf()
    private val excludedProviders: MutableSet<Provider> = ConcurrentHashMap.newKeySet()
    private val requestCounter = AtomicInteger()
    private var loadBalancingStrategy: LoadBalancingStrategy = RoundRobinLoadBalancingStrategy

    override fun registerProvider(provider: Provider) {
        if (providers.size >= config.maxNumberOfProviders) {
            throw MaximumNumberOfProviderException()
        }
        providers.add(provider)
    }

    override fun includeProvider(provider: Provider) { excludedProviders.remove(provider) }
    override fun excludeProvider(provider: Provider) { excludedProviders.add(provider) }
    override fun isProviderExcluded(provider: Provider) = excludedProviders.contains(provider)
    override fun providersIterator() = providers.iterator()
    override fun setLoadBalancerStrategy(loadBalancingStrategy: LoadBalancingStrategy) {
        this.loadBalancingStrategy = loadBalancingStrategy
    }

    override fun get(): String {
        val availableProviders = providers.size - excludedProviders.size
        if (availableProviders == 0) {
            throw NoAvailableProviderException()
        }
        try {
            if (requestCounter.incrementAndGet() > config.maxConcurrentRequestsPerProvider * availableProviders) {
                throw RequestThrottlingException()
            }

            val provider = loadBalancingStrategy.selectProvider(providers, excludedProviders)
            return provider.get()
        } finally {
            requestCounter.decrementAndGet()
        }
    }
}


