package com.himadri

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock

internal class RoundRobinLoadBalancingStrategyTest {
    @BeforeEach
    internal fun setUp() {
        RoundRobinLoadBalancingStrategy.providerIndexMap.clear()
    }

    @Test
    internal fun `test round robin strategy with excluded providers`() {
        val provider1 = mock(Provider::class.java)
        val provider2 = mock(Provider::class.java)
        val provider3 = mock(Provider::class.java)
        val providers = listOf(provider1, provider2, provider3)

        assertEquals(provider1, RoundRobinLoadBalancingStrategy.selectProvider(providers, setOf(provider2)))
        assertEquals(provider3, RoundRobinLoadBalancingStrategy.selectProvider(providers, setOf(provider2)))
        assertEquals(provider3, RoundRobinLoadBalancingStrategy.selectProvider(providers, setOf(provider1, provider2)))
        assertEquals(provider1, RoundRobinLoadBalancingStrategy.selectProvider(providers, setOf()))
    }

    @Test
    internal fun `test round robin strategy with each provider excluded`() {
        val provider1 = mock(Provider::class.java)
        val providers = listOf(provider1)
        val excludedProviders = setOf(provider1)

        assertThrows(NoAvailableProviderException::class.java) {RoundRobinLoadBalancingStrategy.selectProvider(providers, excludedProviders)}
    }

    @Test
    internal fun `test round robin strategy with no provider`() {
        assertThrows(IndexOutOfBoundsException::class.java) {RoundRobinLoadBalancingStrategy.selectProvider(listOf(), setOf())}
    }
}