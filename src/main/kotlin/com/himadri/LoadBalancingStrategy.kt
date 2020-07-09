package com.himadri

import java.util.*
import kotlin.collections.HashMap

abstract class LoadBalancingStrategy {
    abstract fun selectProvider(providers: List<Provider>, excludedProviders: Set<Provider>): Provider

    fun findNextEnabledProvider(providers: List<Provider>, excludedProviders: Set<Provider>, startIndex: Int): Int {
        var index = startIndex
        while (excludedProviders.contains(providers[index])) {
            index = (index + 1) % providers.size
            if (index == startIndex) {
                throw NoAvailableProviderException()
            }
        }
        return index
    }
}

object RandomLoadBalancingStrategy: LoadBalancingStrategy() {
    var random = Random()

    override fun selectProvider(providers: List<Provider>, excludedProviders: Set<Provider>): Provider {
        val randomIndex = random.nextInt(providers.size)
        val selectedIndex = findNextEnabledProvider(providers, excludedProviders, randomIndex)
        return providers[selectedIndex]
    }
}

object RoundRobinLoadBalancingStrategy: LoadBalancingStrategy() {
    val providerIndexMap = HashMap<List<Provider>, Int>()

    override fun selectProvider(providers: List<Provider>, excludedProviders: Set<Provider>): Provider {
        val currentIndex = providerIndexMap.getOrDefault(providers, 0)
        val selectedIndex = findNextEnabledProvider(providers, excludedProviders, currentIndex)
        providerIndexMap.put(providers, (selectedIndex + 1) % providers.size)
        return providers[selectedIndex]
    }
}
