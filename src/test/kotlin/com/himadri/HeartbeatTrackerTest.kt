package com.himadri

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations

internal class HeartbeatTrackerTest {
    @Mock
    lateinit var loadBalancer: LoadBalancer

    lateinit var heartbeatTracker: HeartbeatTracker
    private val config: Config = Config(
        maxNumberOfProviders = 1,
        requiredHealthyHeartbeatForEnablingProvider = 2,
        providerTimeoutMs = 100,
        heartbeatIntervalMs = 0,
        maxConcurrentRequestsPerProvider = 0
    )

    @BeforeEach
    internal fun setUp() {
        MockitoAnnotations.initMocks(this)
        heartbeatTracker = HeartbeatTracker(loadBalancer, config)
    }

    @Test
    fun `provider becomes unhealthy`() {
        val provider = mock(Provider::class.java)
        runBlocking {
            // provider is healthy
            `when`(provider.check()).thenReturn(Unit)
            `when`(loadBalancer.isProviderExcluded(provider)).thenReturn(false)
            heartbeatTracker.checkProviderHeartbeat(provider)
            verify(loadBalancer, never()).excludeProvider(provider)
            verify(loadBalancer, never()).includeProvider(provider)

            // provider becomes unhealthy
            `when`(provider.check()).thenThrow(RuntimeException())
            heartbeatTracker.checkProviderHeartbeat(provider)
            verify(loadBalancer).excludeProvider(provider)
        }
    }

    @Test
    fun `provider becomes healthy after being excluded`() {
        val provider = mock(Provider::class.java)
        runBlocking {
            `when`(loadBalancer.isProviderExcluded(provider)).thenReturn(true)

            // provider is healthy again, first time
            `when`(provider.check()).thenReturn(Unit)
            heartbeatTracker.checkProviderHeartbeat(provider)
            verify(loadBalancer, never()).includeProvider(provider)

            // provider is healthy again, second time
            `when`(provider.check()).thenReturn(Unit)
            heartbeatTracker.checkProviderHeartbeat(provider)
            verify(loadBalancer).includeProvider(provider)
        }
    }

    @Test
    fun `provider fluctuating healthyness resets the counter`() {
        val provider = mock(Provider::class.java)
        runBlocking {
            `when`(loadBalancer.isProviderExcluded(provider)).thenReturn(true)

            // provider is healthy again, first time
            `when`(provider.check()).thenReturn(Unit)
            heartbeatTracker.checkProviderHeartbeat(provider)
            verify(loadBalancer, never()).includeProvider(provider)

            // provider is unhealthy
            `when`(provider.check()).thenThrow(RuntimeException())
            heartbeatTracker.checkProviderHeartbeat(provider)
            verify(loadBalancer, never()).includeProvider(provider)

            // provider is healthy again, first time, after being unhealthy
            doReturn(Unit).`when`(provider).check()
            heartbeatTracker.checkProviderHeartbeat(provider)
            verify(loadBalancer, never()).includeProvider(provider)

            // provider is healthy again, second time
            `when`(provider.check()).thenReturn(Unit)
            heartbeatTracker.checkProviderHeartbeat(provider)
            verify(loadBalancer).includeProvider(provider)
        }
    }
}