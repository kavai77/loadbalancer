package com.himadri

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import java.util.logging.LogManager
import java.util.logging.Logger
import kotlin.random.Random


val logger = Logger.getLogger(object {}.javaClass.getName())

fun main() {
    val logManager = LogManager.getLogManager()
    logManager.readConfiguration(object {}.javaClass.getResourceAsStream("/logging.properties"))
    val json: String = object {}.javaClass.getResource("/config.json").readText()
    val config:Config = Json.parse(Config.serializer(), json)
    val loadBalancer = LoadBalancerImpl(config)
    val heartbeatTracker = HeartbeatTracker(loadBalancer, config)
    heartbeatTracker.scheduleHeartbeat()

    val provider1 = object : Provider {
        override fun get() = "Provider 1"

        override suspend fun check() {
            val randomSleep = Random.nextLong(config.providerTimeoutMs * 2)
            logger.info("[HeartBeat] ${get()}: unresponsive for ${randomSleep}ms")
            delay(randomSleep)
        }
    }

    val provider2 = object : Provider {
        override fun get() = "Provider 2"

        override suspend fun check() {
            logger.info("[HeartBeat] ${get()}: OK")
        }
    }

    loadBalancer.registerProvider(provider1)
    loadBalancer.registerProvider(provider2)
    runBlocking {
        testStartegy(loadBalancer, RoundRobinLoadBalancingStrategy)
        testStartegy(loadBalancer, RandomLoadBalancingStrategy)
    }
}

suspend fun testStartegy(loadBalancer: LoadBalancer, loadBalancingStrategy: LoadBalancingStrategy) {
    logger.info("""-
        -------------------------------------------
        ${loadBalancingStrategy::class.simpleName}
        -------------------------------------------
    """.trimIndent())
    loadBalancer.setLoadBalancerStrategy(loadBalancingStrategy)
    for (i in 0..20) {
        logger.info("Routing to ${loadBalancer.get()}")
        delay(1000)
    }
}