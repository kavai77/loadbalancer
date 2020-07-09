package com.himadri

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import java.util.*

internal class RandomLoadBalancingStrategyTest {
    @Mock
    lateinit var randomMock: Random

    @BeforeEach
    internal fun setUp() {
        MockitoAnnotations.initMocks(this)
        `when`(randomMock.nextInt(ArgumentMatchers.anyInt())).thenReturn(0)
        RandomLoadBalancingStrategy.random = randomMock
    }

    @Test
    internal fun `test random strategy with excluded providers`() {
        val provider1 = mock(Provider::class.java)
        val provider2 = mock(Provider::class.java)
        val providers = listOf(provider1, provider2)
        val excludedProviders = setOf(provider1)


        val selectedProvider = RandomLoadBalancingStrategy.selectProvider(providers, excludedProviders)

        assertEquals(provider2, selectedProvider)
        verify(randomMock).nextInt(providers.size)
    }

    @Test
    internal fun `test random strategy with each provider excluded`() {
        val provider1 = mock(Provider::class.java)
        val providers = listOf(provider1)
        val excludedProviders = setOf(provider1)

        assertThrows(NoAvailableProviderException::class.java) {RandomLoadBalancingStrategy.selectProvider(providers, excludedProviders)}
        verify(randomMock).nextInt(providers.size)
    }

    @Test
    internal fun `test random strategy with no provider`() {
        RandomLoadBalancingStrategy.random = Random()
        assertThrows(IllegalArgumentException::class.java) {RandomLoadBalancingStrategy.selectProvider(listOf(), setOf())}
    }
}