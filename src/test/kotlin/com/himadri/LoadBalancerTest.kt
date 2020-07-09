package com.himadri

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock

internal class LoadBalancerTest {
    lateinit var loadBalancer: LoadBalancer
    private val config: Config = Config(
        maxConcurrentRequestsPerProvider = 3,
        maxNumberOfProviders = 1,
        requiredHealthyHeartbeatForEnablingProvider = 0,
        heartbeatIntervalMs = 0,
        providerTimeoutMs = 0
    )

    @BeforeEach
    internal fun setUp() {
        loadBalancer = LoadBalancerImpl(config)
    }

    @Test
    internal fun `test load balancer throttling`() {
        val slowProvider = object : Provider {
            override fun get(): String {
                runBlocking { delay(100) }
                return "slow provider"
            }

            override suspend fun check() {
            }
        }


        loadBalancer.registerProvider(slowProvider)
        runBlocking {
            for (i in 0 until config.maxConcurrentRequestsPerProvider) {
                launch {
                    loadBalancer.get()
                }
            }

            launch {
                assertThrows(RequestThrottlingException::class.java) { loadBalancer.get() }
            }
        }
    }

    @Test
    fun `test maximum number of providers`() {
        val provider1 = mock(Provider::class.java)
        val provider2 = mock(Provider::class.java)

        loadBalancer.registerProvider(provider1)

        assertThrows(MaximumNumberOfProviderException::class.java) { loadBalancer.registerProvider(provider2) }
    }

    @Test
    fun `test provider inclusion and exclusion`() {
        val provider = mock(Provider::class.java)

        loadBalancer.registerProvider(provider)
        assertFalse(loadBalancer.isProviderExcluded(provider))

        loadBalancer.excludeProvider(provider)
        assertTrue(loadBalancer.isProviderExcluded(provider))

        loadBalancer.includeProvider(provider)
        assertFalse(loadBalancer.isProviderExcluded(provider))
    }
}